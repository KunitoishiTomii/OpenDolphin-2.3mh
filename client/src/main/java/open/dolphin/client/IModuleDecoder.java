package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * IModuleDecoder
 * MethodHandle使いたくて。苦労した割には…
 *
 * @author masuda, Masuda Naika
 */
public class IModuleDecoder {

    private static enum OBJECT_TYPE {

        unknown, bundleMed, bundleDolphin, progressCourse, claimItem, claimItemArray
    };

    private static final MethodType MT_VOID_WITH_STRING_ARG
            = MethodType.methodType(void.class, String.class);
    
    private static final MethodType MT_VOID_WITH_CLAIM_ARRAY_ARG
            = MethodType.methodType(void.class, ClaimItem[].class);

    private static final IModuleDecoder instance;
    
    private final Map<Class, Map<String, MethodHandle>> methodHandleMap;
    
    static {
        instance = new IModuleDecoder();
    }

    private IModuleDecoder() {
        methodHandleMap = new ConcurrentHashMap<>();
    }

    public static IModuleDecoder getInstance() {
        return instance;
    }
    

    public IModuleModel decode(final byte[] beanBytes) {

        Object obj =  new IModuleDecoderImpl().decode(beanBytes);
        
        return (IModuleModel) obj;
    }

    private class IModuleDecoderImpl {

        private Object lastObject;

        private String fieldName;
        private int arrayIndex;

        private final Deque<ObjectClassTypeModel> objStack;

        private IModuleDecoderImpl() {
            arrayIndex = -1;
            objStack = new ArrayDeque();
        }

        private Object decode(final byte[] beanBytes) {

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

            } catch (Throwable ex) {
                ex.printStackTrace(System.err);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }

            return lastObject;
        }

        private void endElement(final XMLStreamReader reader) {

            final String eName = reader.getLocalName();
            switch (eName) {
                case "object":
                    lastObject = objStack.removeFirst().getObject();
                    break;
                case "array":
                    arrayIndex = -1;
                    objStack.removeFirst();
                    break;
            }
        }

        private void startElement(final XMLStreamReader reader) throws Throwable {

            final String eName = reader.getLocalName();

            switch (eName) {
                case "object":
                    createObject(reader.getAttributeValue(0));
                    break;
                case "void":
                    String attrName = reader.getAttributeLocalName(0);
                    String attrValue = reader.getAttributeValue(0);
                    switch (attrName) {
                        case "property":
                            fieldName = attrValue;
                            break;
                        case "index":
                            arrayIndex = Integer.valueOf(attrValue);
                            break;
                    }
                    break;
                case "string":
                    String value = reader.getElementText();
                    writeObjectField(value);
                    break;
                case "array":
                    String className = reader.getAttributeValue(0);
                    String strLen = reader.getAttributeValue(1);
                    writeArray(className, strLen);
                    break;
            }
        }

        private void writeArray(String className, String strLen) throws Throwable {
            
            int len = Integer.parseInt(strLen);
            String methodName = getCamelCaseSetMethodName(fieldName);
            
            switch (className) {
                case "open.dolphin.infomodel.ClaimItem":
                    ClaimItem[] claimItemArray = new ClaimItem[len];
                    ObjectClassTypeModel objModel = objStack.getFirst();
                    
                    switch (objModel.getObjectType()) {
                        case bundleDolphin: {
                            BundleDolphin bundleDolphin = (BundleDolphin) objModel.getObject();
                            MethodHandle mh = getMethodHandle(BundleDolphin.class, 
                                    methodName, MT_VOID_WITH_CLAIM_ARRAY_ARG);
                            mh.invokeExact(bundleDolphin, claimItemArray);
                            break;
                        }
                        case bundleMed: {
                            BundleMed bundleMed = (BundleMed) objModel.getObject();
                            MethodHandle mh = getMethodHandle(BundleMed.class, 
                                    methodName, MT_VOID_WITH_CLAIM_ARRAY_ARG);
                            mh.invokeExact(bundleMed, claimItemArray);
                            break;
                        }
                    }
                    
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.claimItemArray, claimItemArray));
                    break;
            }
        }

        // モデルを作成する
        private void createObject(final String className) {

            switch (className) {
                case "open.dolphin.infomodel.ClaimItem":
                    ClaimItem ci = new ClaimItem();
                    // うまくない
                    ObjectClassTypeModel objModel = objStack.getFirst();
                    if (objModel.getObjectType() == OBJECT_TYPE.claimItemArray) {
                        ((ClaimItem[]) objModel.getObject())[arrayIndex] = ci;
                    }
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.claimItem, ci));
                    break;
                case "open.dolphin.infomodel.BundleDolphin":
                    BundleDolphin bundleDolphin = new BundleDolphin();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.bundleDolphin, bundleDolphin));
                    break;
                case "open.dolphin.infomodel.BundleMed":
                    BundleMed bundleMed = new BundleMed();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.bundleMed, bundleMed));
                    break;
                case "open.dolphin.infomodel.ProgressCourse":
                    ProgressCourse pc = new ProgressCourse();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.progressCourse, pc));
                    break;
                default:
                    System.out.println("Unknown class : " + className);
                    break;
            }
        }
        
        // モデルに値を設定する
        private void writeObjectField(final String value) throws Throwable {

            String methodName = getCamelCaseSetMethodName(fieldName);
            
            ObjectClassTypeModel objModel = objStack.getFirst();
            
            switch (objModel.getObjectType()) {
                case bundleMed: {
                    BundleMed bundleMed = (BundleMed) objModel.getObject();
                    MethodHandle mh = getMethodHandle(BundleMed.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(bundleMed, value);
                    break;
                }
                case bundleDolphin: {
                    BundleDolphin bundleDolphin = (BundleDolphin) objModel.getObject();
                    MethodHandle mh = getMethodHandle(BundleDolphin.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(bundleDolphin, value);
                    break;
                }
                case progressCourse: {
                    ProgressCourse pc = (ProgressCourse) objModel.getObject();
                    MethodHandle mh = getMethodHandle(ProgressCourse.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(pc, value);
                    break;
                }
                case claimItem: {
                    ClaimItem ci = (ClaimItem) objModel.getObject();
                    MethodHandle mh = getMethodHandle(ClaimItem.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(ci, value);
                    break;
                }
            }
        }
    }

    // 毎回instanceofするよりは速いかな…？
    private static class ObjectClassTypeModel {

        private final OBJECT_TYPE type;
        private final Object object;

        private ObjectClassTypeModel(OBJECT_TYPE type, Object object) {
            this.type = type;
            this.object = object;
        }

        private OBJECT_TYPE getObjectType() {
            return type;
        }

        private Object getObject() {
            return object;
        }
    }

    // setter method名を作成する
    private String getCamelCaseSetMethodName(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("set").append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
        return sb.toString();
    }

    // MethodHandleを作る
    private MethodHandle getMethodHandle(Class clazz, String methodName, MethodType mt) throws Throwable {

        Map<String, MethodHandle> mtMap = methodHandleMap.get(clazz);
        if (mtMap == null) {
            mtMap = new ConcurrentHashMap<>();
            methodHandleMap.put(clazz, mtMap);
        }
        MethodHandle mh = mtMap.get(methodName);
        if (mh == null) {
            mh = MethodHandles.lookup().findVirtual(clazz, methodName, mt);
            mtMap.put(methodName, mh);
        }

        return mh;
    }
}
