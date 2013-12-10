package open.dolphin.impl.pacsviewer;

import java.io.UnsupportedEncodingException;
import open.dolphin.util.CharsetDetector;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * listTableに保存するobject
 *
 * @author masuda, Masuda Naika
 */
public class ListDicomObject implements Comparable {
    
    private final DicomObject object;
    private final String ptId;
    private final String ptName;
    private final String ptSex;
    private final String ptBirthDate;
    private final String modalities;
    private final String description;
    private final String studyDate;
    private final String numberOfImage;


    public ListDicomObject(DicomObject obj) {
        object = obj;
        ptId = getString(Tag.PatientID);
        ptName = getNameString().replace("^", " ");
        ptSex = getString(Tag.PatientSex);
        ptBirthDate = getString(Tag.PatientBirthDate);
        modalities = getString(Tag.ModalitiesInStudy);
        description = getString2(Tag.StudyDescription);
        studyDate = getString(Tag.StudyDate);
        numberOfImage = getString(Tag.NumberOfStudyRelatedInstances);
    }
    
    // 橋本医院　加藤さま
    // 【1. Dicomサーバに患者名=""の画像があっても画像を表示出来るように修正】からパクリ
    private String getString(int tag) {
        if (object == null) {
            return "";
        }
        String str = object.getString(tag);
        return (str == null) ? "" : str;
    }
    
    // Descriptionの文字化け対策
    private String getString2(int tag) {
        
        if (object == null) {
            return "";
        }
        
        byte[] bytes = object.getBytes(tag);
        String encoding = CharsetDetector.getStringEncoding(bytes);
        String str;

        if (encoding != null) {
            try {
                str = new String(bytes, encoding);
            } catch (UnsupportedEncodingException ex) {
                str = object.getString(tag);
            }
        } else {
            str = object.getString(tag);
        }
        
        return (str == null) ? "" : str;
    }

    
    // Nameの文字化け対策 by katou
    private String getNameString() {
        String  strAll;
        String  strPart;
        String  strTemp;
        String  strHat;
        String  strReturn = "";
        int     iReadEnd;
        
        if (object == null) {
            return "";
        }
        
        strAll = object.getString(Tag.PatientName);
        if (strAll == null){
            return "";
        }
        iReadEnd    = 0;
        while(iReadEnd != -1){
            iReadEnd = strAll.indexOf("=");
            if(iReadEnd != -1){
                strPart = strAll.substring(0, iReadEnd);
                strHat  = "=";
            }
            else{
                strPart = strAll;
                strHat  = "";
            }
            String encoding = CharsetDetector.getStringEncoding(strPart.getBytes());
            if (encoding != null){
                try {
                    strTemp = new String(strPart.getBytes(), encoding);
                } catch (UnsupportedEncodingException ex) {
                    strTemp = strPart;
                }
            }
            else{
                strTemp = strPart;
            }
            strReturn = strReturn + strTemp + strHat;
            if(iReadEnd != -1){
                strAll = strAll.substring(iReadEnd+1);
            }
        }
        return strReturn;
    }

    public DicomObject getDicomObject() {
        return object;
    }

    public String getPtId() {
        return ptId;
    }

    public String getPtName() {
        return ptName;
    }

    public String getPtSex() {
        return ptSex;
    }

    public String getPtBirthDate() {
        return ptBirthDate;
    }

    public String getModalities() {
        return modalities;
    }

    public String getDescription() {
        return description;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getNumberOfImage() {
        return numberOfImage;
    }

    @Override
    public int compareTo(Object o) {
        try {
            int sDate = Integer.parseInt(studyDate);
            ListDicomObject test = (ListDicomObject) o;
            int tDate = Integer.parseInt(test.getStudyDate());
            if (sDate == tDate) {
                return 0;
            } else if (sDate < tDate) {
                return -1;
            }
        } catch (Exception ex) {
        }
        return 1;
    }
}
