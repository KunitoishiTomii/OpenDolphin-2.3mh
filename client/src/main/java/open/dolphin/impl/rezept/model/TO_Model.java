package open.dolphin.impl.rezept.model;

import java.util.ArrayList;
import java.util.List;
import open.dolphin.impl.rezept.RezeUtil;

/**
 * 特定機材レコード
 * 
 * @author masuda, Masuda Naika
 */
public class TO_Model implements IRezeItem {

    public static final String ID = "TO";
    private static final int daysStart = 17;
    
    public String classCode;        // 診療識別
    public String srycd;            // 特定機材コード
    public Float number;            // 使用量
    public Float ten;               // 点数
    public int count;               // 回数
    public int unitCode;            // 単位コード
    public Float unitPrice;         // 単価
    public String name;             // 特定機材名称
    public String size;             // 商品名及び規格又はサイズ
    public List<DayNumberPair> dayData; // 算定日情報
    
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
    public String getUint() {
        return RezeUtil.getInstance().getTOUnit(unitCode);
    }
    public float getUnitPrice() {
        return unitPrice;
    }
    public String getName() {
        return name;
    }
    public String getSize() {
        return size;
    }
    public List<DayNumberPair> getDayData() {
        return dayData;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        classCode = tokens[1].trim();
        srycd = tokens[3].trim();
        number = tokens[4].isEmpty() ? 1 : Float.parseFloat(tokens[4]);
        ten = tokens[5].isEmpty() ? null : Float.parseFloat(tokens[5]);
        count = Integer.parseInt(tokens[6]);
        unitCode = tokens[7].isEmpty() ? 0 : Integer.parseInt(tokens[7]);
        unitPrice = tokens[8].isEmpty() ? 0 : Float.parseFloat(tokens[8]);
        name = tokens[9].trim();
        size = tokens[10].trim();

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
