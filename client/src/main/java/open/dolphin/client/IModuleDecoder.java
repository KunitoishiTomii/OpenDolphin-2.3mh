package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * MethodHandle使いたくて、汎用性なしｗ
 *
 * @author masuda, Masuda Naika
 */
public class IModuleDecoder {

    private static final MethodType MT_VOID_STRING = MethodType.methodType(void.class, String.class);
    
    private static final Map<String, MethodHandle> METHOD_HANDLE_MAP;

    private IModuleModel moduleModel;
    
    private final StringBuilder sb;

    private ClaimItem claimItem;
    private String fieldName;
    private int arrayIndex = -1;

    static {
        METHOD_HANDLE_MAP = new ConcurrentHashMap<>();
    }

    public IModuleDecoder() {
        sb = new StringBuilder();
    }

    public final IModuleModel decode(final byte[] beanBytes) {

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

        return moduleModel;
    }

    private void endElement(final XMLStreamReader reader) {

        final String eName = reader.getLocalName();

        switch (eName) {
            case "array":
                arrayIndex = -1;
                break;
        }
    }

    private void startElement(final XMLStreamReader reader) throws Throwable {

        final String eName = reader.getLocalName();

        switch (eName) {
            case "object":
                createObject(reader.getAttributeValue(null, "class"));
                break;
            case "void":
                final String prop = reader.getAttributeValue(null, "property");
                if (prop != null) {
                    fieldName = prop;
                }
                break;
            case "string":
                final String value = reader.getElementText();
                if (moduleModel instanceof ProgressCourse) {
                    writeProgressCourse(value);
                } else if (arrayIndex != -1) {
                    writeClaimItemField(value);
                } else {
                    writeBundleField(value);
                }
                break;
            case "array":
                final int len = Integer.parseInt(reader.getAttributeValue(null, "length"));
                if (moduleModel instanceof BundleDolphin) {
                    BundleDolphin bundle = (BundleDolphin) moduleModel;
                    bundle.setClaimItem(new ClaimItem[len]);
                }
        }
    }

    private void createObject(final String className) {

        switch (className) {
            case "open.dolphin.infomodel.ClaimItem":
                claimItem = new ClaimItem();
                BundleDolphin bundle = (BundleDolphin) moduleModel;
                bundle.getClaimItem()[++arrayIndex] = claimItem;
                break;
            case "open.dolphin.infomodel.BundleDolphin":
                moduleModel = new BundleDolphin();
                break;
            case "open.dolphin.infomodel.BundleMed":
                moduleModel = new BundleMed();
                break;
            case "open.dolphin.infomodel.ProgressCourse":
                moduleModel = new ProgressCourse();
                break;
            default:
                System.out.println("Unknown class : " + className);
                break;
        }
    }

    private void writeProgressCourse(final String value) {
        ProgressCourse pc = (ProgressCourse) moduleModel;
        pc.setFreeText(value);
    }

    private void writeBundleField(final String value) throws Throwable {

        String methodName = getCamelCaseSetMethodName(fieldName);
        MethodHandle mh = getMethodHandle(moduleModel.getClass(), methodName);
        if (moduleModel instanceof BundleMed) {
            mh.invokeExact((BundleMed) moduleModel, value);
        } else if (moduleModel instanceof BundleDolphin) {
            mh.invokeExact((BundleDolphin) moduleModel, value);
        }
    }

    private void writeClaimItemField(final String value) throws Throwable {

        String methodName = getCamelCaseSetMethodName(fieldName);
        MethodHandle mh = getMethodHandle(ClaimItem.class, methodName);
        mh.invokeExact(claimItem, value);
    }

    private String getCamelCaseSetMethodName(String name) {
        sb.setLength(0);
        sb.append("set").append(name.substring(0, 1).toUpperCase()).append(name.substring(1));
        return sb.toString();
    }

    private MethodHandle getMethodHandle(Class clazz, String methodName) throws Throwable {

        sb.setLength(0);
        sb.append(clazz.getSimpleName()).append(':').append(methodName);
        String key = sb.toString();

        MethodHandle mt = METHOD_HANDLE_MAP.get(key);
        if (mt == null) {
            mt = MethodHandles.lookup().findVirtual(clazz, methodName, MT_VOID_STRING);
            METHOD_HANDLE_MAP.put(key, mt);
        }

        return mt;
    }
}
