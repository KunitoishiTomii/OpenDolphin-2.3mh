package open.dolphin.impl.rezept.model;

/**
 *
 * @author masuda, Masuda Naika
 */
public interface IRezeModel {
    
    public static final String CAMMA = ",";
    
    public void parseLine(String csv);
}
