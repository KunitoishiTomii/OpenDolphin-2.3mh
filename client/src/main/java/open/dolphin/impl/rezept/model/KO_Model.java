package open.dolphin.impl.rezept.model;

/**
 * 公費レコード
 * 
 * @author masuda, Masuda Naika
 */
public class KO_Model implements IRezeModel {
    
    public static final String ID = "KO";
    
    private String insuranceNum;
    private String certificateNum;
    private String kyufuKbn;
    private int days;
    private int ten;
    
    public String getInsuranceNum() {
        return insuranceNum;
    }
    public String getCertificateNum() {
        return certificateNum;
    }
    public String getKyufuKbn() {
        return kyufuKbn;
    }
    public int getDays() {
        return days;
    }
    public int getTen() {
        return ten;
    }

    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        insuranceNum = tokens[1].trim();
        certificateNum = tokens[2].trim();
        kyufuKbn = tokens[3].trim();
        days = Integer.parseInt(tokens[4]);
        ten = Integer.parseInt(tokens[5]);
    }
    
}
