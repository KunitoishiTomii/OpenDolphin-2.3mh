package open.dolphin.infomodel;

import open.dolphin.common.util.BeanUtils;
import org.hibernate.search.bridge.StringBridge;

/**
 * ModuleModelのbeanBytesからテキストを取り出すブリッジ
 *
 * @author masuda, Masuda Naika
 */

public class ModuleModelBridge implements StringBridge {

    @Override
    public String objectToString(Object object) {

        byte[] beanBytes = (byte[]) object;
        InfoModel im = (InfoModel) BeanUtils.xmlDecode(beanBytes);
        
        if (im instanceof ProgressCourse) {
            String xml = ((ProgressCourse) im).getFreeText();
            return ModelUtils.extractText(xml);
        } else {
            return im.toString();
        }
    }
}
