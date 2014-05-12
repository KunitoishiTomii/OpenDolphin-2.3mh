package open.dolphin.common.util;

import java.util.Comparator;
import open.dolphin.infomodel.SchemaModel;

/**
 * ImageNumber順のコンパレーター
 * 
 * @author masuda, Masuda Naika
 */
public class SchemaNumberComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        
        int imageNumber1 = ((SchemaModel) o1).getImageNumber();
        int imageNumber2 = ((SchemaModel) o2).getImageNumber();
        
        if (imageNumber1 > imageNumber2) {
            return 1;
        } else if (imageNumber1 < imageNumber2) {
            return -1;
        } else {
            return 0;
        }
    }
    
}
