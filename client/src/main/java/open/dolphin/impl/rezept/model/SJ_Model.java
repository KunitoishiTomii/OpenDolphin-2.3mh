package open.dolphin.impl.rezept.model;

/**
 * 症状詳記レコード
 * 
 * @author masuda, Masuda Naika
 */
public class SJ_Model implements IRezeModel {

    public static final String ID = "SJ";
    
    private String kbn;      // 症状詳記区分
    private String data;     // 症状詳記データ
    
    public String getKbn() {
        return kbn;
    }
    public String getData() {
        return data;
    }

    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        kbn = tokens[1].trim();
        data = tokens[2].trim();
    }
}
