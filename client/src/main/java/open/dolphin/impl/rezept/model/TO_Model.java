package open.dolphin.impl.rezept.model;

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
    public String dayData;          // 算定日情報
    
    private String description;
    private int hitCount;

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
    public String getDayData() {
        return dayData;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = csv.split(CAMMA);
        classCode = tokens[1].trim();
        srycd = tokens[2].trim();
        number = tokens[4].isEmpty() ? 1 : Float.parseFloat(tokens[4]);
        ten = tokens[5].isEmpty() ? null : Float.parseFloat(tokens[5]);
        count = Integer.parseInt(tokens[6]);
        unitCode = tokens[7].isEmpty() ? 0 : Integer.parseInt(tokens[7]);
        unitPrice = tokens[8].isEmpty() ? 0 : Float.parseFloat(tokens[8]);
        name = tokens[9].trim();
        size = tokens[10].trim();

        int len = tokens.length;
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (int i = daysStart; i < len; ++i) {
            if (!tokens[i].isEmpty()) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                int day = i - daysStart + 1;
                sb.append(day).append("(").append(tokens[i]).append(")");
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
}
