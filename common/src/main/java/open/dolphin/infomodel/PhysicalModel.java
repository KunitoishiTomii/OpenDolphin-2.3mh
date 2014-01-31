package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * PhysicalModel
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class PhysicalModel implements Serializable, Comparable {
    
    private long heightId;
    private long weightId;
    
    // 身長
    private String height;
    
    // 体重
    private String weight;
    
    // BMI
    @JsonIgnore     // no use
    private int bmi;
    
    // 同定日
    private String identifiedDate;
    
    // メモ
    private String memo;
    
    /**
     * デフォルトコンストラクタ
     */
    public PhysicalModel() {
    }
    
    public long getHeightId() {
        return heightId;
    }
    
    public void setHeightId(long heightId) {
        this.heightId = heightId;
    }
    
    public long getWeightId() {
        return weightId;
    }
    
    public void setWeightId(long weightId) {
        this.weightId = weightId;
    }
    
    // factor
    public String getHeight() {
        return height;
    }
    public void setHeight(String value) {
        height = value;
    }
    
    // identifiedDate
    public String getIdentifiedDate() {
        return identifiedDate;
    }
    
    public void setIdentifiedDate(String value) {
        identifiedDate = value;
    }
    
    // memo
    public String getMemo() {
        return memo;
    }
    
    public void setMemo(String value) {
        memo = value;
    }
    
    public void setWeight(String severity) {
        this.weight = severity;
    }
    
    public String getWeight() {
        return weight;
    }
    
    public String getBmi() {
//        if (bmi == null) {
//            bmi = calcBmi();
//        }
//        return bmi;
        return calcBmi();
    }
    
    /**
     * @return Returns the bmi.
     */
/*
    public String calcBmi() {
        if (height != null && weight != null) {
            float fw = new Float(weight).floatValue();
            float fh = new Float(height).floatValue();
            float bmif = (10000f*fw) / (fh*fh);
            String bmiS = String.valueOf(bmif);
            int index = bmiS.indexOf('.');
            int len = bmiS.length();
            if (index >0 && (index + 2 < len)) {
                bmiS = bmiS.substring(0,index+2);
            }
            return bmiS;
        }
        return null;
    }
*/
    public String calcBmi() {

        try {
            float fw = Float.parseFloat(weight);
            float fh = Float.parseFloat(height);
            BigDecimal bi = new BigDecimal(fw * 10000 / fh / fh);
            float bmif = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
            DecimalFormat frmt = new DecimalFormat("##.0");
            return frmt.format(bmif);
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }
    
    public String getStandardWeight() {
        if (getHeight() == null) {
            return  null;
        }
        try {
            float h = Float.parseFloat(getHeight());
            h /= 100.0f;
            float stW = 22.0f * (h * h);
            String stWS = String.valueOf(stW);
            int index = stWS.indexOf('.');
            if (index > 0) {
                stWS = stWS.substring(0, index +2);
            }
            return stWS;

        } catch (Exception e) {
        }
        return null;
    }
    
    @Override
    public int compareTo(Object other) {
        if (other != null && getClass() == other.getClass()) {
            String val1 = getIdentifiedDate();
            String val2 = ((PhysicalModel)other).getIdentifiedDate();
            return val1.compareTo(val2);
        }
        return 1;
    }
}