package open.dolphin.client;

import java.awt.Color;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.SchemaModel;
import org.apache.log4j.Logger;

/**
 * KartePane の dumper
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class KartePaneDumper_2 {
    
    private static final char CAMMA = ',';
    private static final String FOREGROUND_NAME = StyleConstants.Foreground.toString();
    private static final String NAME_NAME = StyleConstants.NameAttribute.toString();
    private static final String CONTENT_NAME = AbstractDocument.ContentElementName;
    private static final String TEXT_NAME = "text";
    private static final String ATTR_START = "start";
    private static final String ATTR_END = "end";
    //private static final String ELEMENT_NAME= AbstractDocument.ElementNameAttribute;    // "$ename"
    
    private final List<ModuleModel> moduleList;
    private final List<SchemaModel> schemaList;
    private final LinkedList<Element> stack;
    private final Logger logger;
    private XMLStreamWriter writer;
    
    private String spec;
    
    
    public KartePaneDumper_2() {
        logger = ClientContext.getBootLogger();
        moduleList = new ArrayList<>();
        schemaList = new ArrayList<>();
        stack = new LinkedList<>();
    }
    
    /**
     * ダンプした Document の XML 定義を返す。
     *
     * @return Documentの内容を XML で表したもの
     */
    public String getSpec() {
        logger.debug(spec);
        return spec;
    }
    
    /**
     * ダンプした Documentに含まれている ModuleModelを返す。
     *
     * @return
     */
    public ModuleModel[] getModule() {

        if (!moduleList.isEmpty()) {
            return moduleList.toArray(new ModuleModel[moduleList.size()]);
        }
        return null;
    }
    
    /**
     * ダンプした Documentに含まれている SchemaModel を返す。
     *
     * @return
     */
    public SchemaModel[] getSchema() {

        if (!schemaList.isEmpty()) {
            return schemaList.toArray(new SchemaModel[schemaList.size()]);
        }
        return null;
    }

    /**
     * 引数の Document をダンプする。ElementIterator版
     *
     * @param doc ダンプするドキュメント
     */
    public void dump(DefaultStyledDocument doc) {
        
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        StringWriter stringWriter = new StringWriter();

        try {
            writer = factory.createXMLStreamWriter(stringWriter);
            
            ElementIterator itr = new ElementIterator(doc);

            for (Element elem = itr.first(); elem != null; elem = itr.next()){
                endElementsIfParentElementChanged(elem);
                startElement(elem);
            }
            
            endStackedElements();
            
        } catch (BadLocationException | XMLStreamException ex) {
            ex.printStackTrace(System.err);
        } finally {
            try {
                if (writer != null) {
                    // ドキュメントを閉じる
                    writer.flush();
                    spec = stringWriter.toString();
                    writer.close();
                }
            } catch (XMLStreamException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
    
    private void startElement(Element element) throws BadLocationException, XMLStreamException {
        
        // 要素の開始及び終了のオフセット値を保存する
        int start = element.getStartOffset();
        int end = element.getEndOffset();
        logger.debug("start = " + start);
        logger.debug("end = " + end);

        String elmName = element.getName();
        boolean isContent = CONTENT_NAME.equals(elmName);
        
        // Elementを開始する
        writer.writeStartElement(elmName);
        writer.writeAttribute(ATTR_START, String.valueOf(start));
        writer.writeAttribute(ATTR_END, String.valueOf(end));
   
        // このエレメントの属性セットを得る
        AttributeSet atts = element.getAttributes();
        
        // 属性を書き出す
        if (atts != null) {
            writeAttributes(atts, isContent);
        }
        
        // content要素の場合はテキストを抽出する
        if (isContent) {
            int len = end - start;
            String text = element.getDocument().getText(start, len);
            logger.debug("text = " + text);
            // 文字列を出力する
            writer.writeStartElement(TEXT_NAME);
            writer.writeCharacters(text);
            writer.writeEndElement();
        }
        
        stack.addFirst(element);
    }
    
    private void writeAttributes(AttributeSet atts, boolean isContent) throws XMLStreamException {
        
        // 全ての属性を列挙する
        Enumeration names = atts.getAttributeNames();

        while (names.hasMoreElements()) {

            // 属性の名前を得る
            Object nextName = names.nextElement();
            String attrName = nextName.toString();

            if (nextName != StyleConstants.ResolveAttribute) {

                logger.debug("attribute name = " + attrName);

                // $enameは除外する
                if (attrName.startsWith("$")) {
                    continue;
                }

                // foreground 属性の場合は再構築の際に利用しやすい形に分解する
                if (FOREGROUND_NAME.equals(attrName)) {
                    Color c = (Color) atts.getAttribute(StyleConstants.Foreground);
                    logger.debug("color = " + c.toString());
                    writer.writeAttribute(attrName, getColorStr(c));

                } else {
                    // 属性セットから名前をキーにして属性オブジェクトを取得する
                    Object attObject = atts.getAttribute(nextName);
                    logger.debug("attribute object = " + attObject.toString());

                    if (attObject instanceof StampHolder) {
                        // スタンプの場合
                        StampHolder sh = (StampHolder) attObject;
                        moduleList.add(sh.getStamp());
                        String value = String.valueOf(moduleList.size() - 1); // ペインに出現する順番をこの属性の値とする
                        writer.writeAttribute(attrName, value);

                    } else if (attObject instanceof SchemaHolder) {
                        // シュェーマの場合
                        SchemaHolder ch = (SchemaHolder) attObject;
                        schemaList.add(ch.getSchema());
                        String value = String.valueOf(schemaList.size() - 1); // ペインに出現する順番をこの属性の値とする
                        writer.writeAttribute(attrName, value);

                    } else {
                        // それ以外の属性についてはそのまま記録する
                        // <content start="1" end="2" name="stampHolder"><text>hoge</text></content>となるのを防ぐ
                        if (!(isContent && NAME_NAME.equals(attrName))) {
                            writer.writeAttribute(attrName, attObject.toString());
                        }
                    }
                }
            }
        }
    }
    
    private void endElementsIfParentElementChanged(Element element) throws XMLStreamException {

        // 親Elementが変更になる場合はwriteEndElementする
        if (stack.isEmpty()) {
            return;
        }

        Element parent = element.getParentElement();
        Element current = getCurrentElement();

        if (parent != current) {
            int index = stack.indexOf(parent);
            for (int i = 0; i < index; ++i) {
                writer.writeEndElement();
                stack.removeFirst();
            }
        }
    }
    
    private Element getCurrentElement() {
        return stack.getFirst();
    }
    
    private void endStackedElements() throws XMLStreamException {
        for (int i = 0; i < stack.size(); ++i) {
            writer.writeEndElement();
        }
        stack.clear();
    }
    
    private String getColorStr(Color c) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(c.getRed())).append(CAMMA);
        sb.append(String.valueOf(c.getGreen())).append(CAMMA);
        sb.append(String.valueOf(c.getBlue()));
        return sb.toString();
    }
}