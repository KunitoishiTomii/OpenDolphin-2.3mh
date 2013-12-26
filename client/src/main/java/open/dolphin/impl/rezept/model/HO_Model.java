package open.dolphin.impl.rezept.model;

/**
 * 保険者レコード
 * 
 * @author masuda, Masuda Naika
 */
public class HO_Model implements IRezeModel {

    public static final String ID = "HO";
    
    private String insuranceNum;
    private String insuranceSymbol;
    private String certificateNum;
    private int days;
    private int ten;
    
    public String getInsuranceNum() {
        return insuranceNum;
    }
    public String getInsuranceSymbol() {
        return insuranceSymbol;
    }
    public String getCertificateNum() {
        return certificateNum;
    }
    public int getDays() {
        return days;
    }
    public int getTen() {
        return ten;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = csv.split(CAMMA);
        insuranceNum = tokens[1].trim();
        insuranceSymbol = tokens[2].trim();
        certificateNum = tokens[3].trim();
        days = Integer.parseInt(tokens[4]);
        ten = Integer.parseInt(tokens[5]);

    }
    
}
