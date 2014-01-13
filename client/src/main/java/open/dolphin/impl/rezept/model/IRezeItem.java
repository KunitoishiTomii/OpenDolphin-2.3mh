package open.dolphin.impl.rezept.model;

/**
 *
 * @author masuda, Masuda Naika
 */
public interface IRezeItem extends IRezeModel {
    
    public static final String CR = "\n";
    
    public String getClassCode();
    public String getSrycd();
    public Float getNumber();
    public Float getTen();
    public Integer getCount();
    public String getDescription();
    public void setDescription(String name);
    public void setHitCount(int hitCount);
    public int getHitCount();
    public void incrementHitCount();
    public void setPass(boolean pass);
    public boolean isPass();
}
