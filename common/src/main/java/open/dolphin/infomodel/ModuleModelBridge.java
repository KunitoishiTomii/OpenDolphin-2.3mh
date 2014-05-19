package open.dolphin.infomodel;

import open.dolphin.common.util.ModuleBeanDecoder;
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
        //IModuleModel im = (IModuleModel) BeanUtils.xmlDecode(beanBytes);
        IModuleModel im = ModuleBeanDecoder.getInstance().decode(beanBytes);
        
        if (im instanceof ProgressCourse) {
            String xml = ((ProgressCourse) im).getFreeText();
            return ModelUtils.extractText(xml);
        } else {
            return im.toString();
        }
    }
}
