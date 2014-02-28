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

        Unknown, BundleMed, BundleDolphin, ProgressCourse, ClaimItem, ClaimItemArray
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

        IModuleModel model =  new IModuleDecoderImpl().decode(beanBytes);
        
        return model;
    }

    private class IModuleDecoderImpl {

        private ObjectClassTypeModel lastObject;

        private String fieldName;
        private int arrayIndex;
        private int depth;
        private int voidIndexDepth;
        
        private final StringBuilder sb;

        private final Deque<ObjectClassTypeModel> objStack;

        private IModuleDecoderImpl() {
            arrayIndex = -1;
            voidIndexDepth = -1;
            objStack = new ArrayDeque();
            sb = new StringBuilder();
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

            return (IModuleModel) lastObject.getObject();
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
                    // うまくない
                    if (depth == voidIndexDepth) {
                        ObjectClassTypeModel objModel = objStack.getFirst();
                        if (objModel.getObjectType() == OBJECT_TYPE.ClaimItemArray) {
                            ((ClaimItem[]) objModel.getObject())[arrayIndex] = (ClaimItem) lastObject.getObject();
                        }
                    }
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
                            voidIndexDepth = depth;
                            arrayIndex = Integer.valueOf(attrValue);
                            break;
                    }
                    break;
                case "string":
                    String value = reader.getElementText();
                    // getElementTextはreaderをEndElementまで進めるのでdepthを戻す
                    depth--;
                    writeObjectField(fieldName, value);
                    break;
                case "array":
                    String className = reader.getAttributeValue(0);
                    String strLen = reader.getAttributeValue(1);
                    writeArray(fieldName, className, strLen);
                    break;
            }
        }

        private void writeArray(String fldName, String className, String strLen) throws Throwable {
            
            int len = Integer.parseInt(strLen);
            String methodName = getCamelCaseSetMethodName(fldName);
            
            switch (className) {
                case "open.dolphin.infomodel.ClaimItem":
                    ClaimItem[] claimItemArray = new ClaimItem[len];
                    ObjectClassTypeModel objModel = objStack.getFirst();
                    
                    switch (objModel.getObjectType()) {
                        case BundleDolphin: {
                            BundleDolphin bundleDolphin = (BundleDolphin) objModel.getObject();
                            MethodHandle mh = getMethodHandle(BundleDolphin.class, 
                                    methodName, MT_VOID_WITH_CLAIM_ARRAY_ARG);
                            mh.invokeExact(bundleDolphin, claimItemArray);
                            break;
                        }
                        case BundleMed: {
                            BundleMed bundleMed = (BundleMed) objModel.getObject();
                            MethodHandle mh = getMethodHandle(BundleMed.class, 
                                    methodName, MT_VOID_WITH_CLAIM_ARRAY_ARG);
                            mh.invokeExact(bundleMed, claimItemArray);
                            break;
                        }
                    }
                    
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.ClaimItemArray, claimItemArray));
                    break;
                default:
                    System.out.println("Unknown class : " + className);
                    break;
            }
        }

        // モデルを作成する
        private void createObject(final String className) {

            switch (className) {
                case "open.dolphin.infomodel.ClaimItem":
                    ClaimItem ci = new ClaimItem();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.ClaimItem, ci));
                    break;
                case "open.dolphin.infomodel.BundleDolphin":
                    BundleDolphin bundleDolphin = new BundleDolphin();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.BundleDolphin, bundleDolphin));
                    break;
                case "open.dolphin.infomodel.BundleMed":
                    BundleMed bundleMed = new BundleMed();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.BundleMed, bundleMed));
                    break;
                case "open.dolphin.infomodel.ProgressCourse":
                    ProgressCourse pc = new ProgressCourse();
                    objStack.addFirst(new ObjectClassTypeModel(OBJECT_TYPE.ProgressCourse, pc));
                    break;
                default:
                    System.out.println("Unknown class : " + className);
                    break;
            }
        }
        
        // モデルに値を設定する
        private void writeObjectField(String fldName, String value) throws Throwable {

            String methodName = getCamelCaseSetMethodName(fldName);
            
            ObjectClassTypeModel objModel = objStack.getFirst();
            
            switch (objModel.getObjectType()) {
                case BundleMed: {
                    BundleMed bundleMed = (BundleMed) objModel.getObject();
                    MethodHandle mh = getMethodHandle(BundleMed.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(bundleMed, value);
                    break;
                }
                case BundleDolphin: {
                    BundleDolphin bundleDolphin = (BundleDolphin) objModel.getObject();
                    MethodHandle mh = getMethodHandle(BundleDolphin.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(bundleDolphin, value);
                    break;
                }
                case ProgressCourse: {
                    ProgressCourse pc = (ProgressCourse) objModel.getObject();
                    MethodHandle mh = getMethodHandle(ProgressCourse.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(pc, value);
                    break;
                }
                case ClaimItem: {
                    ClaimItem ci = (ClaimItem) objModel.getObject();
                    MethodHandle mh = getMethodHandle(ClaimItem.class,
                            methodName, MT_VOID_WITH_STRING_ARG);
                    mh.invokeExact(ci, value);
                    break;
                }
                default:
                    break;
            }
        }

        // setter method名を作成する
        private String getCamelCaseSetMethodName(String name) {
            sb.setLength(0);
            sb.append("set").append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
            return sb.toString();
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
