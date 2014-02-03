package open.dolphin.impl.rezept.model;

/**
 * 国保連固有情報レコード
 * 
 * @author masuda, Masuda Naika
 */
public class KH_Model implements IRezeModel {
    
    public static final String ID = "KH";
    
    private String info;
    
    public String getInfo() {
        return info;
    }

    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        info = tokens[1].trim();
    }
}
