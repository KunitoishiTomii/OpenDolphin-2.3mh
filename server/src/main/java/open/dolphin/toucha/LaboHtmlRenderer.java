package open.dolphin.toucha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import open.dolphin.common.util.SimpleXmlWriter;
import open.dolphin.infomodel.NLaboItem;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.SampleDateComparator;

/**
 * LaboHtmlRenderer
 *
 * @author masuda, Masuda Naika
 */
public class LaboHtmlRenderer {
    
    private static final String TAG_FONT = "FONT";
    private static final String TAG_TABLE = "TABLE";
    private static final String TAG_TR = "TR";
    private static final String TAG_TD = "TD";
    private static final String TAG_TH = "TH";
    private static final String ATTR_COLOR = "COLOR";
    private static final String ATTR_BGCOLOR = "BGCOLOR";
    
    private static final LaboHtmlRenderer instance;

    static {
        instance = new LaboHtmlRenderer();
    }

    private LaboHtmlRenderer() {
    }

    public static LaboHtmlRenderer getInstance() {
        return instance;
    }

    public String render(List<NLaboModule> modules) {
        
        if (modules.isEmpty()) {
            return "No labo data.";
        }
        // 検体採取日の降順なので昇順にソートする
        Collections.sort(modules, new SampleDateComparator());
        
        List<String> header = getHeader(modules);
        List<LabTestRowObject> rowList = getRowList(modules);
        
        SimpleXmlWriter writer = new SimpleXmlWriter();
        writer.setRepcaceXmlChar(true);
        writer.setReplaceZenkaku(false);
        
        writer.writeStartElement(TAG_TABLE);
        // ヘッダ
        writer.writeStartElement(TAG_TR)
                .writeAttribute(ATTR_BGCOLOR, "lightgrey");
        for (String value : header) {
            writer.writeStartElement(TAG_TH)
                    .writeCharacters(value)
                    .writeEndElement();
        }
        writer.writeEndElement();
        
        for (int row = 0; row < rowList.size(); ++row) {
            LabTestRowObject rowObj = rowList.get(row);

            // 項目名
            String specimenName = rowObj.getSpecimenName();
            if (specimenName != null) {
                writer.writeStartElement(TAG_TR)
                        .writeAttribute(ATTR_BGCOLOR, "gold");
                for (int i = 0; i < modules.size() + 1; ++i) {
                    // 項目グループ
                    writer.writeStartElement(TAG_TD)
                            .writeCharacters(specimenName)
                            .writeEndElement();
                }
            } else {
                writer.writeStartElement(TAG_TR);
                if ((row & 1) == 1) {
                    writer.writeAttribute(ATTR_BGCOLOR, "aliceblue");
                }
                writer.writeStartElement(TAG_TD)
                        .writeCharacters(rowObj.getItemName())
                        .writeEndElement();

                List<LabTestValueObject> values = rowObj.getValues();
                if (values != null) {
                    for (LabTestValueObject value : values) {
                        // 項目
                        writer.writeStartElement(TAG_TD);
                        if (value != null && value.getOut() != null) {
                            switch (value.getOut()) {
                                case "H":
                                    writer.writeStartElement(TAG_FONT)
                                            .writeAttribute(ATTR_COLOR, "red")
                                            .writeCharacters(value.getValue())
                                            .writeEndElement();
                                    break;
                                case "L":
                                    writer.writeStartElement(TAG_FONT)
                                            .writeAttribute(ATTR_COLOR, "blue")
                                            .writeCharacters(value.getValue())
                                            .writeEndElement();
                                    break;
                                default:
                                    writer.writeCharacters(value.getValue());
                                    break;
                            }
                        }
                        writer.writeEndElement();
                    }
                }
            }
            writer.writeEndElement();
        }

        writer.writeEndDocument();
        
        return writer.getProduct();
    }
    
    private List<String> getHeader(List<NLaboModule> modules) {
        List<String> header = new ArrayList<>();
        header.add("項目");
        for (NLaboModule module : modules) {
            header.add(module.getSampleDate().substring(2));
        }
        return header;
    }

    private List<LabTestRowObject> getRowList(List<NLaboModule> modules) {

        List<LabTestRowObject> bloodExams = new ArrayList<>();
        List<LabTestRowObject> urineExams = new ArrayList<>();
        List<LabTestRowObject> otherExams = new ArrayList<>();

        int moduleIndex = 0;

        for (NLaboModule module : modules) {

            for (NLaboItem item : module.getItems()) {

                // 検体名を取得する
                String specimenName = item.getSpecimenName();
                // 検体で分類してリストを選択する
                List<LabTestRowObject> rowObjectList;
                if (specimenName != null) {     // null check 橋本先生のご指摘
                    if (specimenName.contains("血")) {
                        rowObjectList = bloodExams;
                    } else if (specimenName.contains("尿") || specimenName.contains("便")) {
                        rowObjectList = urineExams;
                    } else {
                        rowObjectList = otherExams;
                    }
                } else {
                    rowObjectList = otherExams;
                }

                boolean found = false;

                for (LabTestRowObject rowObject : rowObjectList) {
                    if (item.getItemCode().equals(rowObject.getItemCode())) {
                        found = true;
                        LabTestValueObject value = new LabTestValueObject();
                        value.setSampleDate(module.getSampleDate());
                        value.setValue(item.getValue());
                        value.setOut(item.getAbnormalFlg());
                        value.setComment1(item.getComment1());
                        value.setComment2(item.getComment2());
                        rowObject.addLabTestValueObjectAt(moduleIndex, value);
                        rowObject.setNormalValue(item.getNormalValue());    // 基準値記録漏れ対策
                        break;
                    }
                }

                if (!found) {
                    LabTestRowObject row = new LabTestRowObject();
                    row.setLabCode(item.getLaboCode());
                    row.setGroupCode(item.getGroupCode());
                    row.setParentCode(item.getParentCode());
                    row.setItemCode(item.getItemCode());
                    row.setItemName(item.getItemName());
                    row.setUnit(item.getUnit());
                    row.setNormalValue(item.getNormalValue());
                    //
                    LabTestValueObject value = new LabTestValueObject();
                    value.setSampleDate(module.getSampleDate());
                    value.setValue(item.getValue());
                    value.setOut(item.getAbnormalFlg());
                    value.setComment1(item.getComment1());
                    value.setComment2(item.getComment2());
                    row.addLabTestValueObjectAt(moduleIndex, value);
                    //
                    rowObjectList.add(row);
                }
            }

            moduleIndex++;
        }

        List<LabTestRowObject> ret = new ArrayList<>();

        if (!bloodExams.isEmpty()) {
            Collections.sort(bloodExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("血液検査");
            bloodExams.add(0, specimen);
            ret.addAll(bloodExams);
        }
        if (!urineExams.isEmpty()) {
            Collections.sort(urineExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("尿・便");
            urineExams.add(0, specimen);
            ret.addAll(urineExams);
        }
        if (!otherExams.isEmpty()) {
            Collections.sort(otherExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("その他");
            otherExams.add(0, specimen);
            ret.addAll(otherExams);
        }
        
        return ret;
    }
}
