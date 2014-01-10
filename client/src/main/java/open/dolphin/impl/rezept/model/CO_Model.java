package open.dolphin.impl.rezept.model;

/**
 * コメントレコード
 * 
 * @author masuda, Masuda Naika
 */
public class CO_Model implements IRezeItem {

    public static final String ID = "CO";
    
    private String classCode;       // 診療識別
    private String srycd;           // コメントコード
    private String comment;         // 文字データ
    
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
        return null;
    }
    
    @Override
    public Float getTen() {
        return null;
    }

    @Override
    public Integer getCount() {
        return null;
    }
    
    public String getComment() {
        return comment;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = csv.split(CAMMA);
        classCode = tokens[1].trim();
        srycd = tokens[3].trim();
        comment = tokens[4].trim();
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
