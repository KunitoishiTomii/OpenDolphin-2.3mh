package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.BundleMed;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.ProgressCourse;

/**
 * ModuleBeanDecoder 結局reflectionにｗ
 *
 * @author masuda, Masuda Naika
 */
public class ModuleBeanDecoder {

    private static final Class[] KNOWN_CLASSES = {
        ClaimItem.class, BundleDolphin.class, BundleMed.class, ProgressCourse.class
    };

    private static final ModuleBeanDecoder instance;

    private final Map<Class, Map<String, Field>> reflectFieldMap;

    private final Map<String, Class> classMap;

    static {
        instance = new ModuleBeanDecoder();
    }

    private ModuleBeanDecoder() {
        reflectFieldMap = new ConcurrentHashMap<>();
        classMap = new ConcurrentHashMap<>();
    }

    public static ModuleBeanDecoder getInstance() {
        return instance;
    }

    public IModuleModel decode(final byte[] beanBytes) {

        IModuleModel model = new ModuleDecoder().decode(beanBytes);

        return model;
    }

    private class ModuleDecoder {

        private Object lastObject;

        private final Deque<Object> objStack;
        private final Deque<VoidCommand> cmdStack;

        private ModuleDecoder() {
            objStack = new ArrayDeque();
            cmdStack = new ArrayDeque();
        }

        private IModuleModel decode(final byte[] beanBytes) {

            // いつものStAX
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = null;

            try (InputStream is = new ByteArrayInputStream(beanBytes)) {

                reader = factory.createXMLStreamReader(is);

                while (reader.hasNext()) {
                    final int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                        case XMLStreamReader.END_ELEMENT:
                            endElement(reader);
                            break;
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }

            return (IModuleModel) lastObject;
        }

        private void processVoidCommand(final Object obj) throws Exception {

            VoidCommand voidCmd = cmdStack.pollFirst();
            if (voidCmd == null) {
                return;
            }

            final Object target = objStack.getFirst();
            final String cmd = voidCmd.getCommand();
            final String value = voidCmd.getValue();

            switch (cmd) {
                case "property":
                    Field field = getReflectField(target.getClass(), value);
                    field.set(target, obj);
                    break;
                case "index":
                    int index = Integer.valueOf(value);
                    ((Object[]) target)[index] = obj;
                    break;
            }
        }

        private void startElement(final XMLStreamReader reader) throws Exception {

            final String eName = reader.getLocalName();

            switch (eName) {
                case "void":
                    String cmd = reader.getAttributeLocalName(0);
                    String value = reader.getAttributeValue(0);
                    cmdStack.add(new VoidCommand(cmd, value));
                    break;
                case "object":
                    String objClsName = reader.getAttributeValue(0);
                    Object obj = getClassForName(objClsName).newInstance();
                    processVoidCommand(obj);
                    objStack.addFirst(obj);
                    break;
                case "array":
                    String arrayClsName = reader.getAttributeValue(0);
                    int len = Integer.parseInt(reader.getAttributeValue(1));
                    Object array = Array.newInstance(getClassForName(arrayClsName), len);
                    processVoidCommand(array);
                    objStack.addFirst(array);
                    break;
                case "string":
                    String str = reader.getElementText();
                    processVoidCommand(str);
                    break;
            }
        }

        private void endElement(final XMLStreamReader reader) {

            final String eName = reader.getLocalName();

            switch (eName) {
                case "object":
                    lastObject = objStack.removeFirst();
                    break;
                case "array":
                    objStack.removeFirst();
                    break;
            }
        }
    }

    private static class VoidCommand {

        private final String cmd;
        private final String value;

        private VoidCommand(String cmd, String value) {
            this.cmd = cmd;
            this.value = value;
        }

        private String getCommand() {
            return cmd;
        }

        private String getValue() {
            return value;
        }
    }

    // 前もって関連クラス・フィールドを登録しておく
    public void init() {

        for (Class clazz : KNOWN_CLASSES) {
            // クラスを登録する
            classMap.put(clazz.getName(), clazz);
            // フィールドを登録する
            Map<String, Field> fieldMap = new ConcurrentHashMap<>();
            reflectFieldMap.put(clazz, fieldMap);
            for (Class cls = clazz; cls != null; cls = cls.getSuperclass()) {
                for (Field field : cls.getDeclaredFields()) {
                    int modifier = field.getModifiers();
                    // static final定数とtransientは除外する
                    if ((Modifier.isStatic(modifier) && Modifier.isFinal(modifier))
                            || Modifier.isTransient(modifier)) {
                        continue;
                    }
                    field.setAccessible(true);
                    fieldMap.put(field.getName(), field);
                }
            }
        }
    }

    // クラス名に対応したクラスを作る
    private Class getClassForName(final String className) throws Exception {

        Class clazz = classMap.get(className);
        if (clazz == null) {
            clazz = Class.forName(className);
            classMap.put(className, clazz);
        }
        return clazz;
    }

    // java.lang.reflect.Fieldを作る
    private Field getReflectField(final Class clazz, final String fieldName) throws Exception {

        Map<String, Field> fieldMap = reflectFieldMap.get(clazz);
        if (fieldMap == null) {
            fieldMap = new ConcurrentHashMap<>();
            reflectFieldMap.put(clazz, fieldMap);
        }
        Field field = fieldMap.get(fieldName);

        if (field == null) {
            Exception ex = null;
            for (Class cls = clazz; cls != null; cls = cls.getSuperclass()) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    fieldMap.put(fieldName, field);
                    break;
                } catch (NoSuchFieldException nsfex) {
                    if (ex != null) {
                        ex = nsfex;
                    }
                }
            }
            if (field == null && ex != null) {
                throw ex;
            }
        }

        return field;
    }
}
