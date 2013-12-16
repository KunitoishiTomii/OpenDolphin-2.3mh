package open.dolphin.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * 速度優先、汎用性なしｗ
 *
 * @author masuda, Masuda Naika
 */
public class IModuleDecoder {
    
    private ProgressCourse progressCourse;
    private BundleDolphin bundle;
    
    private ClaimItem claimItem;
    private String fieldName;
    private int arrayIndex = -1;

    
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

        } catch (XMLStreamException | IOException ex) {
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (XMLStreamException ex) {
            }
        }

        if (progressCourse != null) {
            return progressCourse;
        } else {
            return bundle;
        }
    }

    private void endElement(final XMLStreamReader reader) {

        final String eName = reader.getLocalName();

        switch (eName) {
            case "array":
                arrayIndex = -1;
                break;
        }
    }

    private void startElement(final XMLStreamReader reader) throws XMLStreamException {

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
                if (progressCourse != null) {
                    writeProgressCourse(value);
                } else if (arrayIndex != -1) {
                    writeClaimItemField(value);
                } else {
                    writeBundleField(value);
                }
                break;
            case "array":
                final int len = Integer.parseInt(reader.getAttributeValue(null, "length"));
                bundle.setClaimItem(new ClaimItem[len]);
        }

    }

    private void createObject(final String className) {

        switch (className) {
            case "open.dolphin.infomodel.ClaimItem":
                claimItem = new ClaimItem();
                bundle.getClaimItem()[++arrayIndex] = claimItem;
                break;
            case "open.dolphin.infomodel.BundleDolphin":
                bundle = new BundleDolphin();
                break;
            case "open.dolphin.infomodel.BundleMed":
                bundle = new BundleMed();
                break;
            case "open.dolphin.infomodel.ProgressCourse":
                progressCourse = new ProgressCourse();
                break;
        }
    }

    private void writeProgressCourse(final String value) {
        progressCourse.setFreeText(value);
    }

    private void writeBundleField(final String value) {

        switch (fieldName) {
            case "className":
                bundle.setClassName(value);
                break;
            case "classCode":
                bundle.setClassCode(value);
                break;
            case "classCodeSystem":
                bundle.setClassCodeSystem(value);
                break;
            case "admin":
                bundle.setAdmin(value);
                break;
            case "adminCode":
                bundle.setAdminCode(value);
                break;
            case "adminCodeSystem":
                bundle.setAdminCodeSystem(value);
                break;
            case "adminMemo":
                bundle.setAdminMemo(value);
                break;
            case "bundleNumber":
                bundle.setBundleNumber(value);
                break;
            case "memo":
                bundle.setMemo(value);
                break;
            case "insurance":
                bundle.setInsurance(value);
                break;
            case "orderName":
                bundle.setOrderName(value);
                break;
        }
    }

    private void writeClaimItemField(final String value) {

        switch (fieldName) {
            case "name":
                claimItem.setName(value);
                break;
            case "code":
                claimItem.setCode(value);
                break;
            case "codeSystem":
                claimItem.setCodeSystem(value);
                break;
            case "classCode":
                claimItem.setClassCode(value);
                break;
            case "classCodeSystem":
                claimItem.setClassCodeSystem(value);
                break;
            case "number":
                claimItem.setNumber(value);
                break;
            case "unit":
                claimItem.setUnit(value);
                break;
            case "numberCode":
                claimItem.setNumberCode(value);
                break;
            case "numberCodeSystem":
                claimItem.setNumberCodeSystem(value);
                break;
            case "memo":
                claimItem.setMemo(value);
                break;
            case "ykzKbn":
                claimItem.setYkzKbn(value);
                break;
        }
    }
}
