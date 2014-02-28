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
 * KarteRenderer_2 大改造
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class KarteRenderer_2 {

    private static final String STAMP_HOLDER = StampHolder.ATTRIBUTE_NAME;
    private static final String SCHEMA_HOLDER = SchemaHolder.ATTRIBUTE_NAME;
    private static final String COMPONENT_NAME = StyleConstants.ComponentElementName;   // "component";
    private static final String SECTION_NAME = AbstractDocument.SectionElementName;
    private static final String CONTENT_NAME = AbstractDocument.ContentElementName;
    private static final String PARAGRAPH_NAME = AbstractDocument.ParagraphElementName;
    private static final String TEXT_NAME = "text";

    private static final String DEFAULT_STYLE_NAME = StyleContext.DEFAULT_STYLE;
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

    private final static KarteRenderer_2 instance;

    static {
        instance = new KarteRenderer_2();
    }

    private KarteRenderer_2() {
    }

    public static KarteRenderer_2 getInstance() {
        return instance;
    }

    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    public final void render(final DocumentModel model, final KartePane soaPane, final KartePane pPane) {

        final List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        final List<ModuleModel> soaModules = new ArrayList<>();
        final List<ModuleModel> pModules = new ArrayList<>();
        final List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {

            final String role = bean.getModuleInfoBean().getStampRole();
            if (role == null) {
                continue;
            }
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
            soaPane.initKarteStyledDocument();  // 忘れてたｗ
            for (ModuleModel mm : soaModules) {
                soaPane.stamp(mm);
            }

        } else {
            new KartePaneRenderer_ElementSpec().renderPane(soaSpec, soaModules, schemas, soaPane);
        }

        // P Pane をレンダリングする
        if (pPane != null) {
            if (pSpec == null || pSpec.isEmpty()) {
                // 前回処方など適用
                pPane.initKarteStyledDocument();    // 忘れてたｗ
                for (ModuleModel mm : pModules) {
                    pPane.stamp(mm);
                }
            } else {
                new KartePaneRenderer_ElementSpec().renderPane(pSpec, pModules, schemas, pPane);
            }
        }
    }
    
    // StAX版
    private static class KartePaneRenderer {

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
        
        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KartePane kartePane) {

            this.modules = modules;
            this.schemas = schemas;
            this.kartePane = kartePane;

            // Offscreen updates trick
            doc = new KarteStyledDocument(kartePane);
            
            defaultStyle = doc.setDefaultStyle();
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = null;
            
            try (StringReader stream = new StringReader(xml)) {

                reader = factory.createXMLStreamReader(stream);

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

            } catch (XMLStreamException ex) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }

            // レンダリング後はdefault styleに戻す
            doc.setDefaultStyle();
            
            // JTextPaneにKarteStyledDocumentを設定する
            kartePane.getTextPane().setDocument(doc);
        }

        private void startElement(XMLStreamReader reader) throws XMLStreamException {

            String eName = reader.getLocalName();
            
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
                        doc.insertFreeString(CR, null);
                    }
                    componentFlg = false;
                    startContent(text);
                    break;
                case COMPONENT_NAME:
                    // StampHolderが連続している場合、間に改行を補う
                    if (componentFlg) {
                        doc.insertFreeString(CR, null);
                    }
                    componentFlg = true;
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_NAME);
                    startComponent(name, number);
                    break;
                //case SECTION_NAME:
                //    break;
                default:
                    break;
            }
        }

        private void endElement(XMLStreamReader reader) {

            String eName = reader.getLocalName();
            
            switch (eName) {
                case PARAGRAPH_NAME:
                    endParagraph();
                    break;
                //case CONTENT_NAME:
                //case COMPONENT_NAME:
                //case SECTION_NAME:
                default:
                    break;
            }
        }

        private void startParagraph(String alignStr) {

            MutableAttributeSet atts = new SimpleAttributeSet();
            atts.setResolveParent(defaultStyle);

            if (alignStr != null) {
                switch (alignStr) {
                    case "0":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_LEFT);
                        break;
                    case "1":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_CENTER);
                        break;
                    case "2":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_RIGHT);
                        break;
                }
            }
            // ParagraphにAlignmentを設定する
            doc.setParagraphAttributes(doc.getLength(), 0, atts, true);
        }

        private void endParagraph() {
            doc.clearLogicalStyle();
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
            doc.insertFreeString(text, atts);
        }

        private void startComponent(String name, String number) {

            if (name == null) {
                return;
            }

            int index = Integer.parseInt(number);
            switch (name) {
                case STAMP_HOLDER:
                    // StampHolderを作成する。JTextPaneにDocumentは未設定なのでKartePane.flowStampは使えない
                    StampHolder stamp = new StampHolder(kartePane, modules.get(index));
                    stamp.setTransferHandler(StampHolderTransferHandler.getInstance());
                    doc.flowComponent(stamp);
                    break;
                case SCHEMA_HOLDER:
                    // SchemaHolderを作成する
                    SchemaHolder schema = new SchemaHolder(kartePane, schemas.get(index));
                    schema.setTransferHandler(SchemaHolderTransferHandler.getInstance());
                    doc.flowComponent(schema);
                    break;
            }
        }
    }

    // ElementSpec版
    private static class KartePaneRenderer_ElementSpec {

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
        private int baseFontSize;

        private List<ElementSpec> specList;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KartePane kartePane) {

            this.modules = modules;
            this.schemas = schemas;
            this.kartePane = kartePane;
            baseFontSize = kartePane.getTextPane().getFont().getSize();
            specList = new ArrayList<>();
                        
            doc = new KarteStyledDocument(kartePane);
            
            defaultStyle = doc.getStyle(DEFAULT_STYLE_NAME);
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = null;
            
            try (StringReader stream = new StringReader(xml)) {

                reader = factory.createXMLStreamReader(stream);

                while (reader.hasNext()) {
                    final int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                        case XMLStreamReader.END_ELEMENT:
                            endElement(reader);
                            break;
                    }
                }

            } catch (XMLStreamException ex) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }
            
            // DocumentをElementSpecListで一括作成する
            doc.createDocument(specList);
            // ComponentHolderのPositionを設定する。ダサイが止む無し
            doc.setComponentPositions();
            
            // レンダリング後はdefault styleに戻す
            doc.setDefaultStyle();
            
            // JTextPaneにKarteStyledDocumentを設定する
            kartePane.getTextPane().setDocument(doc);
        }

        private void startElement(XMLStreamReader reader) throws XMLStreamException {

            final String eName = reader.getLocalName();
            
            switch (eName) {
                case SECTION_NAME:
                    startSection();
                    break;
                case PARAGRAPH_NAME:
                    final String alignStr = reader.getAttributeValue(null, ALIGNMENT_NAME);
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
                    final String text = reader.getElementText();
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
                    final String name = reader.getAttributeValue(null, NAME_NAME);
                    final String number = reader.getAttributeValue(null, COMPONENT_NAME);
                    startComponent(name, number);
                    break;
                default:
                    break;
            }
        }

        private void startSection() {
            specList.add(new ElementSpec(null, ElementSpec.StartTagType));
        }

        private void endElement(XMLStreamReader reader) {

            final String eName = reader.getLocalName();

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
            specList.add(new ElementSpec(null, ElementSpec.EndTagType));
        }

        private void startParagraph(final String alignStr) {

            final MutableAttributeSet atts = new SimpleAttributeSet();
            atts.setResolveParent(defaultStyle);

            if (alignStr != null) {
                switch (alignStr) {
                    case "0":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_LEFT);
                        break;
                    case "1":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_CENTER);
                        break;
                    case "2":
                        StyleConstants.setAlignment(atts, StyleConstants.ALIGN_RIGHT);
                        break;
                }
            }

            specList.add(new ElementSpec(atts, ElementSpec.StartTagType));
        }

        private void startContent(String text) {

            // 特殊文字を戻す
            text = XmlUtils.fromXml(text);

            // このコンテントに設定する AttributeSet
            final MutableAttributeSet atts = new SimpleAttributeSet();

            // foreground 属性を設定する
            if (foreground != null) {
                final String[] tokens = foreground.split(",");
                int r = Integer.parseInt(tokens[0]);
                int g = Integer.parseInt(tokens[1]);
                int b = Integer.parseInt(tokens[2]);
                StyleConstants.setForeground(atts, new Color(r, g, b));
            }

            // size 属性を設定する
            if (size != null) {
                int modelFontSize = Integer.parseInt(size);
                int viewFontSize = FontManager.toViewFontSize(modelFontSize, baseFontSize);
                StyleConstants.setFontSize(atts, viewFontSize);
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

        private void startComponent(final String name, final String number) {
            
            if (name == null) {
                return;
            }

            final int index = Integer.parseInt(number);
            switch (name) {
                case STAMP_HOLDER: {
                    // StampHolderを作成する
                    final StampHolder sh = new StampHolder(kartePane, modules.get(index));
                    sh.setTransferHandler(StampHolderTransferHandler.getInstance());
                    // このスタンプ用のスタイルを生成する
                    final MutableAttributeSet atts = doc.createComponentAttribute(sh);
                    insertString(" ", atts);
                    break;
                }
                case SCHEMA_HOLDER: {
                    // SchemaHolderを作成する
                    final SchemaHolder sh = new SchemaHolder(kartePane, schemas.get(index));
                    sh.setTransferHandler(SchemaHolderTransferHandler.getInstance());
                    // このスタンプ用のスタイルを生成する
                    final MutableAttributeSet atts = doc.createComponentAttribute(sh);
                    insertString(" ", atts);
                    break;
                }
            }
        }

        private void insertString(final String text, final AttributeSet attrs) {
            final char[] chars = text.toCharArray();
            final ElementSpec spec = new ElementSpec(attrs, ElementSpec.ContentType, chars, 0, chars.length);
            specList.add(spec);
        }

    }
}
