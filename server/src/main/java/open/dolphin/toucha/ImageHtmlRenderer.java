package open.dolphin.toucha;

import java.awt.Dimension;
import open.dolphin.common.util.SimpleXmlWriter;
import org.apache.commons.codec.binary.Base64;

/**
 * ImageHtmlRenderer
 * 
 * @author masuda, Masuda Naika
 */
public class ImageHtmlRenderer {
    
    public String getImageHtml(byte[] jpegByte, Dimension imageSize) {
        
        SimpleXmlWriter writer = new SimpleXmlWriter();
        byte[] bytes = ImageTool.getScaledBytes(jpegByte, imageSize, "jpeg");
        
        if (bytes != null) {
            String base64 = "data:image/jpeg;base64," + Base64.encodeBase64String(bytes);
            writer.writeEmptyElement("img");
            writer.writeAttribute("src", base64).writeAttribute("alt", "img");
            writer.writeBR();
        }
        return writer.getProduct();
    }

}
