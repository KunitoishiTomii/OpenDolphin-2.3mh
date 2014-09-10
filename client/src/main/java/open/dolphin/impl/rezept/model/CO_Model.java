package open.dolphin.impl.rezept.model;

import java.util.Collections;
import java.util.List;

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
    
    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
    public void setSrycd(String srycd) {
        this.srycd = srycd;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
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

    @Override
    public List<CO_Model> getCommentList() {
        return Collections.EMPTY_LIST;
    }
}
