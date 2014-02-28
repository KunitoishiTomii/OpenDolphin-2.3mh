package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.IModuleModel;

/**
 * ModuleBeanDecoder 
 * 結局reflectionにｗ
 *
 * @author masuda, Masuda Naika
 */
public class ModuleBeanDecoder {

    private static final ModuleBeanDecoder instance;

    private final Map<String, Map<String, Field>> reflectFieldMap;

    static {
        instance = new ModuleBeanDecoder();
    }

    private ModuleBeanDecoder() {
        reflectFieldMap = new ConcurrentHashMap<>();
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
                    objStack.addFirst(createObject(reader.getAttributeValue(0)));
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
                    Object array = createArray(className, len);
                    Object object = objStack.getFirst();
                    Field arrayFld = getReflectField(object.getClass(), fieldName);
                    arrayFld.set(object, array);
                    objStack.addFirst(array);
                    break;
            }
        }
    }

    // モデルを作成する
    private Object createObject(final String className) throws Exception {
        Class clazz = Class.forName(className);
        return clazz.newInstance();
    }

    private Object createArray(final String className, final int len) throws Exception {
        Class clazz = Class.forName(className);
        return Array.newInstance(clazz, len);
    }

    // java.lang.reflect.Fieldを作る
    private Field getReflectField(final Class clazz, final String fieldName) throws Exception {

        final String className = clazz.getName();
        Map<String, Field> fieldMap = reflectFieldMap.get(className);
        if (fieldMap == null) {
            fieldMap = new ConcurrentHashMap<>();
            reflectFieldMap.put(className, fieldMap);
        }
        Field field = fieldMap.get(fieldName);

        if (field == null) {
            
            try {
                field = clazz.getDeclaredField(fieldName);
                
            } catch (NoSuchFieldException ex1) {
                // 親クラスを検索する
                Class parent = clazz.getSuperclass();
                while (parent != null) {
                    try {
                        field = parent.getDeclaredField(fieldName);
                        break;
                    } catch (NoSuchFieldException ex2) {
                        parent = parent.getSuperclass();
                    }
                }
                if (field == null) {
                    throw ex1;
                }
            }

            field.setAccessible(true);
            fieldMap.put(fieldName, field);
        }

        return field;
    }
}
