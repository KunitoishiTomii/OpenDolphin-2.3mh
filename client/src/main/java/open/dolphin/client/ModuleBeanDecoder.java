package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javassist.Modifier;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.BundleMed;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.ProgressCourse;

/**
 * ModuleBeanDecoder 
 * 結局reflectionにｗ
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

    public IModuleModel decode(final byte[] beanBytes) {

        IModuleModel model = new ModuleDecoder().decode(beanBytes);

        return model;
    }

    private class ModuleDecoder {

        private Object lastObject;

        private String fieldName;
        private int arrayIndex;
        private int depth;
        private int voidIndexDepth;

        private final Deque<Object> objStack;

        private ModuleDecoder() {
            arrayIndex = -1;
            voidIndexDepth = -1;
            objStack = new ArrayDeque();
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
                            depth++;
                            startElement(reader);
                            break;
                        case XMLStreamReader.END_ELEMENT:
                            endElement(reader);
                            depth--;
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

        private void endElement(final XMLStreamReader reader) {

            final String eName = reader.getLocalName();

            switch (eName) {
                case "object":
                    lastObject = objStack.removeFirst();
                    break;
                case "array":
                    arrayIndex = -1;
                    voidIndexDepth = -1;
                    objStack.removeFirst();
                    break;
                case "void":
                    // <void index="?"> のdepthまで戻った時点でarray項目を設定する
                    if (depth == voidIndexDepth) {
                        Object obj = objStack.getFirst();
                        if (obj instanceof Object[]) {
                            ((Object[]) obj)[arrayIndex] = lastObject;
                        }
                    }
                    break;
            }
        }

        private void startElement(final XMLStreamReader reader) throws Exception {

            final String eName = reader.getLocalName();

            switch (eName) {
                case "object":
                    String objClassName = reader.getAttributeValue(0);
                    objStack.addFirst(getClassForName(objClassName).newInstance());
                    break;
                case "void":
                    String attrName = reader.getAttributeLocalName(0);
                    String attrValue = reader.getAttributeValue(0);
                    switch (attrName) {
                        case "property":
                            fieldName = attrValue;
                            break;
                        case "index":
                            voidIndexDepth = depth;
                            arrayIndex = Integer.valueOf(attrValue);
                            break;
                    }
                    break;
                case "string":
                    String value = reader.getElementText();
                    // getElementTextはreaderをEndElementまで進めるのでdepthを戻す
                    depth--;
                    // モデルに値を設定する
                    Object obj = objStack.getFirst();
                    Field field = getReflectField(obj.getClass(), fieldName);
                    field.set(obj, value);
                    break;
                case "array":
                    String className = reader.getAttributeValue(0);
                    int len = Integer.parseInt(reader.getAttributeValue(1));
                    // arrayを設定する
                    Object array = Array.newInstance(getClassForName(className), len);
                    Object object = objStack.getFirst();
                    Field arrayFld = getReflectField(object.getClass(), fieldName);
                    arrayFld.set(object, array);
                    objStack.addFirst(array);
                    break;
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
