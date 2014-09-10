package open.dolphin.infomodel;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * DiseaseEntry
 * 
 * @author  Minagawa, Kazushi
 */
public final class DiseaseEntry extends MasterEntry {
	
    private String icdTen;

    /** Creates a new instance of DeseaseEntry */
    public DiseaseEntry() {
    }

    public String getIcdTen() {
        return icdTen;
    }

    public void setIcdTen(String val) {
        icdTen = val;
    }
    
    @Override
    public boolean isInUse() {
        if (getDisUseDate() != null) {
            GregorianCalendar gc = new GregorianCalendar();
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
            String refDate = f.format(gc.getTime());
            return refDate.compareTo(getDisUseDate()) <= 0;
        }
        return false;
    }
    
//masuda^   特定疾患関連
    private int byoKanrenKbn;
    public void setByoKanrenKbn(int i){
        byoKanrenKbn = i;
    }
    public int getByoKanrenKbn(){
        return byoKanrenKbn;
    }
    public String getByoKanrenKbnStr() {
        return ModelUtils.getByoKanrenKbnStr(byoKanrenKbn);
    }
//masuda$
}
