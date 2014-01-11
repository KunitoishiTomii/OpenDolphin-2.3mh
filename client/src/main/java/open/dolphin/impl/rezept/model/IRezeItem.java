package open.dolphin.impl.rezept.model;

/**
 *
 * @author masuda, Masuda Naika
 */
public interface IRezeItem extends IRezeModel {
    
    public String getClassCode();
    public String getSrycd();
    public Float getNumber();
    public Float getTen();
    public Integer getCount();
    public String getDescription();
    public void setDescription(String name);
    public void setHitCount(int hitCount);
    public int getHitCount();
}
