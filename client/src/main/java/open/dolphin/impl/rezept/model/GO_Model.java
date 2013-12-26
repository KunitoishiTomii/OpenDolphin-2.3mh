package open.dolphin.impl.rezept.model;

/**
 * 診療報酬請求書レコード
 * 
 * @author masuda, Masuda Naika
 */
public class GO_Model implements IRezeModel {
    
    public static final String ID = "GO";
    
    private int totalCount;
    private int totalTen;
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public int getTotalTen() {
        return totalTen;
    }
    
    public void setTotalCount(int count) {
        totalCount = count;
    }
    public void setTotalTen(int ten) {
        totalTen = ten;
    }

    @Override
    public void parseLine(String csv) {
       String[] tokens = csv.split(CAMMA);
       totalCount = Integer.parseInt(tokens[1]);
       totalTen = Integer.parseInt(tokens[2]);
    }
    
}
