package open.dolphin.common.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * SimpleXmlWriter ただやってみたかっただけｗ
 * 
 * @author masuda, Masuda Naika
 */
public class SimpleXmlWriter {
    
    private static final String CR = "\n";
    private static final String BR = "<br>";
    private static final String HR = "<hr>";
    
    private final StringBuilder buffer;
    private boolean replaceXmlChar;
    private boolean replaceZenkaku;
    private final Deque<String> stack;
    
    private boolean isEmptyElement;
    private boolean isWithinStartTag;
    
    public SimpleXmlWriter() {
        buffer = new StringBuilder(256);
        stack = new ArrayDeque<>();
        replaceXmlChar = true;
        replaceZenkaku = false;
    }
    
    public void setRepcaceXmlChar(boolean replaceXmlChar) {
        this.replaceXmlChar = replaceXmlChar;
    }
    
    public void setReplaceZenkaku(boolean replaceZenkaku) {
        this.replaceZenkaku = replaceZenkaku;
    }
    
    public SimpleXmlWriter writeStartElement(String elemName) {
        
        closeOpenedTag();
        
        buffer.append('<').append(elemName);
        
        isEmptyElement = false;
        isWithinStartTag = true;
        
        stack.push(elemName);
        
        return this;
    }
    
    public SimpleXmlWriter writeEmptyElement(String elemName) {
        
        closeOpenedTag();
        
        buffer.append('<').append(elemName);
        
        isEmptyElement = true;
        isWithinStartTag = true;
        
        return this;
    }
    
    public SimpleXmlWriter writeEndElement() {
        
        closeOpenedTag();
        
        buffer.append(addEndTag(stack.pop()));
        
        return this;
    }
    
    public SimpleXmlWriter writeAttribute(String attrName, int attrValue) {
        
        writeAttribute(attrName, String.valueOf(attrValue));
        
        return this;
    }
    
    public SimpleXmlWriter writeAttribute(String attrName, String attrValue) {
        
        if (isWithinStartTag) {
            buffer.append(' ').append(attrName);
            if (attrValue != null) {
                attrValue = replaceString(attrValue);
                buffer.append("=\"").append(attrValue).append('"');
            }
        }
        return this;
    }
    
    public SimpleXmlWriter writeBR() {
        writeRawCharacters(BR);
        return this;
    }
    
    public SimpleXmlWriter writeHR() {
        writeRawCharacters(HR);
        return this;
    }
    
    public SimpleXmlWriter writeCharacters(String str) {
        
        str = replaceString(str);
        writeRawCharacters(str);
        
        return this;
    }
    
    public SimpleXmlWriter writeCrReplaceCharacters(String str) {
        
        str = replaceString(str);
        str = str.replace(CR, BR);
        writeRawCharacters(str);
        
        return this;
    }
    
    public SimpleXmlWriter writeRawCharacters(String str) {
        
        closeOpenedTag();
        
        buffer.append(str);
        
        return this;
    }
    
    public SimpleXmlWriter writeEndDocument() {
        
        closeOpenedTag();
        
        for (String elemName : stack) {
            buffer.append(addEndTag(elemName));
        }
        stack.clear();
        
        return this;
    }
    
    public SimpleXmlWriter writeStartDocument(String encoding, String version) {
        
        buffer.append("<?xml version=\"").append(version).append('"');
        buffer.append(" encoding=\"").append(encoding).append("\"?>\n");
        
        return this;
    }
    
    public SimpleXmlWriter writeComment(String text) {
        
        text = replaceString(text);
        buffer.append("<!--").append(text).append("-->\n");
        
        return this;
    }
    
    public String getProduct() {
        return buffer.toString();
    }
    
    private String addEndTag(String elemName) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("</").append(elemName).append('>');
        
        return sb.toString();
    }
    
    private void closeOpenedTag() {

        if (!isWithinStartTag) {
            return;
        }
        
        if (isEmptyElement) {
            buffer.append("/>");
        } else {
            buffer.append('>');
        }
        
        isWithinStartTag = false;
    }
    
    private String replaceString(String text) {
        
        if (text == null) {
            return "";
        }
        if (replaceXmlChar) {
            text = XmlUtils.toXml(text);
        }
        if (replaceZenkaku) {
            text = ZenkakuUtils.toHalfNumber(text);
        }
        return text;
    }
}
