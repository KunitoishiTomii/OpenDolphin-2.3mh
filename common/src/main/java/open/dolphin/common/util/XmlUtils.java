package open.dolphin.common.util;

/**
 * XML文字置換をまとめた
 * 
 * @author masuda, Masuda Naika
 */
public class XmlUtils {
    
    /** XML文書で置換が必要な文字 */
    private static final String[] REPLACES = new String[] { "<", ">", "&", "'" ,"\""};
    
    /** 置換文字 */
    private static final String[] XML_EXPRESSIONS = new String[] { "&lt;", "&gt;", "&amp;", "&apos;", "&quot;" };
    
    public static String toXml(String text) {
        if (text == null) {
            return null;
        }
        for (int i = 0; i < REPLACES.length; i++) {
            text = text.replace(REPLACES[i], XML_EXPRESSIONS[i]);
        }
        return text;
    }
    
    public static String fromXml(String xml) {
        if (xml == null) {
            return null;
        }
        for (int i = 0; i < REPLACES.length; i++) {
            xml = xml.replace(XML_EXPRESSIONS[i], REPLACES[i]);
        }
        return xml;
    }
}
