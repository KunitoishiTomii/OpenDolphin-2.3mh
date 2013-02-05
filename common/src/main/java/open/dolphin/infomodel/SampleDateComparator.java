package open.dolphin.infomodel;

import java.text.SimpleDateFormat;
/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class SampleDateComparator implements java.util.Comparator {

    private String ConvertDateString(NLaboModule m1){
        String dateRet;
        SimpleDateFormat df12 = new SimpleDateFormat("yyyyMMddhhmm");
        SimpleDateFormat dfDef= new SimpleDateFormat("yyyy-MM-dd hh:mm");
        
        if(m1.getSampleDate().length()==12){
            // FALCOの日付データはyyyymmddhhmm形式でDBに登録されているので、
            // DBに登録されたデータを他データの形式に併せて出力する (苦肉の策)
            // 2013/1/19 FALCOのlabocentercodeは"FALCO"でないことがあるため、
            // labocentercodeを判断条件から除外
            try{
                dateRet = dfDef.format(df12.parse(m1.getSampleDate()));
            }
            catch(Exception e){
                dateRet = m1.getSampleDate();
            }
        }
        else{
            dateRet = m1.getSampleDate();
        }
        return dateRet;
    }
    @Override
    public int compare(Object o1, Object o2) {
        String date1;
        String date2;
       
        NLaboModule m1 = (NLaboModule) o1;
        NLaboModule m2 = (NLaboModule) o2;
        
        date1 = ConvertDateString(m1);
        date2 = ConvertDateString(m2);
        
        int result = date1.compareTo(date2);

        if (result == 0) {
            String key1 = m1.getModuleKey();
            String key2 = m2.getModuleKey();
            if (key1 != null && key2 != null) {
                return key1.compareTo(key2);
            }
        }

        return date1.compareTo(date2);
    }
    
}
