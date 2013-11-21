package open.dolphin.client;

import java.awt.Color;
import java.io.StringReader;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.infomodel.*;
import open.dolphin.common.util.XmlUtils;
import open.dolphin.tr.SchemaHolderTransferHandler;
import open.dolphin.tr.StampHolderTransferHandler;


/**
 * KarteRenderer_2 改  batch insert版
 *
 * @author Kazushi Minagawa, Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika
 */
public class KarteRenderer_3 {

    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String COMPONENT_NAME = "component";
    //private static final String SECTION_NAME = AbstractDocument.SectionElementName;
    private static final String CONTENT_NAME = AbstractDocument.ContentElementName;
    private static final String PARAGRAPH_NAME = AbstractDocument.ParagraphElementName;
    private static final String TEXT_NAME ="text";
    
    private static final String DEFAULT_STYLE_NAME = "default";
    //private static final String ALIGNMENT_STYLE_NAME = "alignment";
    
    private static final String NAME_NAME = StyleConstants.NameAttribute.toString();
    private static final String ALIGNMENT_NAME = StyleConstants.Alignment.toString();   // "Alignment" not "alignment"
    private static final String FOREGROUND_NAME = StyleConstants.Foreground.toString();
    private static final String SIZE_NAME = StyleConstants.Size.toString();
    private static final String BOLD_NAME = StyleConstants.Bold.toString();
    private static final String ITALIC_NAME = StyleConstants.Italic.toString();
    private static final String UNDERLINE_NAME = StyleConstants.Underline.toString();
    
    private static final String NAME_STAMP_HOLDER = "name=\"stampHolder\"";
    
    private static final String CR = "\n";
    
    private final static KarteRenderer_3 instance;
    
    static {
        instance = new KarteRenderer_3();
    }
    
    private KarteRenderer_3() {
    }
    
    public static KarteRenderer_3 getInstance() {
        return instance;
    }

    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    public void render(DocumentModel model, KartePane soaPane, KartePane pPane) {

        List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        List<ModuleModel> soaModules = new ArrayList<>();
        List<ModuleModel> pModules = new ArrayList<>();
        List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {

            String role = bean.getModuleInfoBean().getStampRole();
            switch (role) {
                case IInfoModel.ROLE_SOA:
                    soaModules.add(bean);
                    break;
                case IInfoModel.ROLE_SOA_SPEC:
                    soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                    break;
                case IInfoModel.ROLE_P:
                    pModules.add(bean);
                    break;
                case IInfoModel.ROLE_P_SPEC:
                    pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                    break;
            }
        }

        // 念のためソート
        Collections.sort(soaModules);
        Collections.sort(pModules);
        if (schemas != null) {
            Collections.sort(schemas);
        }
        
        // この処理はなんだろう？ soaPaneにスタンプホルダ―？？？
        if (soaSpec != null && pSpec != null) {
            if (soaSpec.contains(NAME_STAMP_HOLDER)) {
                String sTmp = soaSpec;
                String pTmp = pSpec;
                soaSpec = pTmp;
                pSpec = sTmp;
            }
        }

        // SOA Pane をレンダリングする
        if (soaSpec == null || soaSpec.isEmpty()) {
            // soaにModuleModelはないはずだよね… あ、モディファイ版にはあるかもしれない…
            for (ModuleModel mm : soaModules) {
                soaPane.stamp(mm);
            }

        } else {
            new KartePaneRenderer().renderPane(soaSpec, soaModules, schemas, soaPane);
        }

        // P Pane をレンダリングする
        if (pSpec == null || pSpec.isEmpty()) {
            // 前回処方など適用
            for (ModuleModel mm : pModules) {
                pPane.stamp(mm);
            }
        } else {
            new KartePaneRenderer().renderPane(pSpec, pModules, schemas, pPane);
        }
    }

    
    // StAX版
    private class KartePaneRenderer {

        private KartePane kartePane;
        private KarteStyledDocument doc;
        private Style defaultStyle;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;
        
        private String foreground;
        private String size;
        private String bold;
        private String italic;
        private String underline;
        private boolean componentFlg;
        private boolean isFirstParagraph;
        
        private List<ElementSpec> batch;
        
        // http://javatechniques.com/blog/faster-jtextpane-text-insertion-part-i/
        private static final boolean USE_TRICK = true;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KartePane kartePane) {
            
            this.modules = modules;
            this.schemas = schemas;
            this.kartePane = kartePane;
            this.doc = kartePane.getDocument();
            batch = new ArrayList<>();
            isFirstParagraph = true;
            
            // Offscreen updates trick
            if (USE_TRICK) {
                doc = new KarteStyledDocument();
                doc.setParent(kartePane);
                DefaultStyledDocument dummy = new DefaultStyledDocument();
                kartePane.getTextPane().setDocument(dummy);
            } else {
                doc = kartePane.getDocument();
            }
            
            defaultStyle = doc.getStyle(DEFAULT_STYLE_NAME);
            //doc.setLogicalStyle(DEFAULT_STYLE_NAME);
            
            try {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                StringReader stream = new StringReader(xml);
                XMLStreamReader reader = factory.createXMLStreamReader(stream);

                while (reader.hasNext()) {
                    int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                        case XMLStreamReader.END_ELEMENT:
                            endElement(reader);
                            break;
                    }
                }
                doc.processBatch(batch);
            } catch (XMLStreamException | BadLocationException ex) {
            }
            
            // レンダリング後はdefault styleに戻す
            doc.setLogicalStyle(DEFAULT_STYLE_NAME);
            
            if (USE_TRICK) {
                kartePane.getTextPane().setDocument(doc);
            }
            
            // キャレット位置を設定する
            //kartePane.getTextPane().setCaretPosition(doc.getLength() - 1);
        }

        private void startElement(XMLStreamReader reader) throws XMLStreamException {
            
            String eName = reader.getName().getLocalPart();

            switch (eName) {
                case PARAGRAPH_NAME:
                    String alignStr = reader.getAttributeValue(null, ALIGNMENT_NAME);
                    startParagraph(alignStr);
                    break;
                case CONTENT_NAME:
                    foreground = reader.getAttributeValue(null, FOREGROUND_NAME);
                    size = reader.getAttributeValue(null, SIZE_NAME);
                    bold = reader.getAttributeValue(null, BOLD_NAME);
                    italic = reader.getAttributeValue(null, ITALIC_NAME);
                    underline = reader.getAttributeValue(null, UNDERLINE_NAME);
                    break;
                case TEXT_NAME:
                    String text = reader.getElementText();
                    // StampHolder直後に改行を補う
                    if (componentFlg && !text.startsWith(CR)) {
                        insertString(CR, null);
                    }
                    componentFlg = false;

                    startContent(text);
                    break;
                case COMPONENT_NAME:
                    // StampHolderが連続している場合、間に改行を補う
                    if (componentFlg) {
                        insertString(CR, null);
                    }
                    componentFlg = true;
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_NAME);
                    int start = Integer.valueOf(reader.getAttributeValue(null, "start"));
                    int end = Integer.valueOf(reader.getAttributeValue(null, "end"));
                    try {
                        startComponent(name, number, start, end);
                    } catch (BadLocationException ex) {

                    }
                    break;
                default:
                    break;
            }
        }
        
        private void endElement(XMLStreamReader reader) {

            String eName = reader.getName().getLocalPart();
            
            switch (eName) {
                case PARAGRAPH_NAME:
                    endParagraph();
                    break;
                //case CONTENT_NAME:
                //    break;
                //case COMPONENT_NAME:
                //    break;
                //case SECTION_NAME:
                default:
                    break;
            }
        }
        
        private void endParagraph() {
            batch.add(new ElementSpec(null, ElementSpec.EndTagType));
        }
        
        private void startParagraph(String alignStr) {
            
            MutableAttributeSet attrs = new SimpleAttributeSet();
            attrs.setResolveParent(defaultStyle);
            
            if (alignStr != null) {
                switch (alignStr) {
                    case "0":
                        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                        break;
                    case "1":
                        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_CENTER);
                        break;
                    case "2":
                        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                        break;
                }
            }
            
            if (isFirstParagraph) {
                // 最初のParagraphにAlignmentを設定する
                doc.setParagraphAttributes(doc.getLength(), 0, attrs, true);
                isFirstParagraph = false;
            } else {
                batch.add(new ElementSpec(attrs, ElementSpec.StartTagType));
            }
        }
        
        private void startContent(String text) {

            // 特殊文字を戻す
            text = XmlUtils.fromXml(text);
            
            // このコンテントに設定する AttributeSet
            MutableAttributeSet atts = new SimpleAttributeSet();
            
            // foreground 属性を設定する
            if (foreground != null) {
                String[] tokens = foreground.split(",");
                int r = Integer.parseInt(tokens[0]);
                int g = Integer.parseInt(tokens[1]);
                int b = Integer.parseInt(tokens[2]);
                StyleConstants.setForeground(atts, new Color(r, g, b));
            }

            // size 属性を設定する
            if (size != null) {
                StyleConstants.setFontSize(atts, Integer.parseInt(size));
            }
            // bold 属性を設定する
            if (bold != null) {
                StyleConstants.setBold(atts, Boolean.valueOf(bold));
            }
            // italic 属性を設定する
            if (italic != null) {
                StyleConstants.setItalic(atts, Boolean.valueOf(italic));
            }
            // underline 属性を設定する
            if (underline != null) {
                StyleConstants.setUnderline(atts, Boolean.valueOf(underline));
            }

            // テキストを挿入する
            insertString(text, atts);
        }
        
        private void startComponent(String name, String number, int start, int end) throws BadLocationException {

            if (name != null) {
                int index = Integer.valueOf(number);
                switch (name) {
                    case STAMP_HOLDER: {
                        StampHolder sth = new StampHolder(kartePane, modules.get(index));
                        sth.setEntry(doc.createPosition(start), doc.createPosition(end));
                        sth.setTransferHandler(StampHolderTransferHandler.getInstance());
                        // このスタンプ用のスタイルを生成する
                        SimpleAttributeSet runStyle = new SimpleAttributeSet();
                        StyleConstants.setComponent(runStyle, sth);
                        runStyle.addAttribute(NAME_NAME, STAMP_HOLDER);
                        insertString(" ", runStyle);
                        break;
                    }
                    case SCHEMA_HOLDER: {
                        SchemaHolder sch = new SchemaHolder(kartePane, schemas.get(index));
                        sch.setEntry(doc.createPosition(start), doc.createPosition(end));
                        sch.setTransferHandler(SchemaHolderTransferHandler.getInstance());
                        // このスタンプ用のスタイルを生成する
                        SimpleAttributeSet runStyle = new SimpleAttributeSet();
                        runStyle.addAttribute(NAME_NAME, SCHEMA_HOLDER);
                        insertString(" ", runStyle);
                        break;
                    }
                }
            }
        }
        
        private void insertString(String text, AttributeSet attrs) {
            char[] chars = text.toCharArray();
            ElementSpec spec = new ElementSpec(attrs, ElementSpec.ContentType, chars, 0, chars.length);
            batch.add(spec);
        }

    }
}