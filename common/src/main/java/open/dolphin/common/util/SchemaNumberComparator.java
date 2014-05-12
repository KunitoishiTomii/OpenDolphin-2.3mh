package open.dolphin.common.util;

import java.util.Comparator;
import open.dolphin.infomodel.SchemaModel;

/**
 * ImageNumber順のコンパレーター
 * 
 * SchemaModel.imageIndexはTransientじゃぁねぇーか！
 * 
 * @author masuda, Masuda Naika
 */
public class SchemaNumberComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {

        int imgNum1 = getImageIndex((SchemaModel) o1);
        int imgNum2 = getImageIndex((SchemaModel) o2);

        return imgNum1 - imgNum2;
    }

    private int getImageIndex(SchemaModel model) {
        String href = model.getExtRefModel().getHref();
        int hyphenPos = href.indexOf('-');
        int dotPos = href.indexOf('.');
        return Integer.parseInt(href.substring(hyphenPos + 1, dotPos));
    }

}
