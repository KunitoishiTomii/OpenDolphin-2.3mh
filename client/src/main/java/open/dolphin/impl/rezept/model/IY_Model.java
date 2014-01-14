package open.dolphin.impl.rezept.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 医薬品レコード
 * 
 * @author masuda, Masuda Naika
 */
public class IY_Model implements IRezeItem {

    public static final String ID = "IY";
    private static final int daysStart = 13;
    
    private String classCode;       // 診療識別　21等
    private String srycd;           // 医薬品コード
    private Float number;           // 使用量
    private Float ten;              // 点数
    private int count;              // 回数
    private List<DayNumberPair> dayData;    // 算定日情報
        
    private String description;
    
    private int hitCount;
    private boolean pass;
    
    @Override
    public String getClassCode() {
        return classCode;
    }
    @Override
    public String getSrycd() {
        return srycd;
    }
    @Override
    public Float getNumber() {
        return number;
    }
    @Override
    public Float getTen() {
        return ten;
    }
    @Override
    public Integer getCount() {
        return count;
    }
    public List<DayNumberPair> getDayData() {
        return dayData;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = csv.split(CAMMA);
        classCode = tokens[1].trim();
        srycd = tokens[3].trim();
        number = tokens[4].isEmpty() ? 1 : Float.parseFloat(tokens[4]);
        ten = tokens[5].isEmpty() ? null : Float.parseFloat(tokens[5]);
        count = Integer.parseInt(tokens[6]);
        
        dayData = new ArrayList<>();
        int len = tokens.length;
        for (int i = daysStart; i < len; ++i) {
            String token = tokens[i];
            if (!token.isEmpty() && !CR.equals(token)) {
                int day = i - daysStart + 1;
                int num = Integer.parseInt(tokens[i]);
                dayData.add(new DayNumberPair(day, num));
            }
        }
    }
    
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String desc) {
        description = desc;
    }
    
    @Override
    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    @Override
    public int getHitCount() {
        return hitCount;
    }

    @Override
    public void incrementHitCount() {
        hitCount++;
    }
    
    @Override
    public void setPass(boolean pass) {
        this.pass = pass;
    }
    @Override
    public boolean isPass() {
        return pass;
    }
}
