package open.dolphin.client;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import javax.swing.text.*;
import open.dolphin.infomodel.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


/**
 * KarteRenderer_2 改
 *
 * @author Kazushi Minagawa, Digital Globe, Inc. 
 * @author modified by masuda, Masuda Naika
 */
public class KarteRenderer_2 {

    private static final String COMPONENT_ELEMENT_NAME = "component";
    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final int TT_SECTION = 0;
    private static final int TT_PARAGRAPH = 1;
    private static final int TT_CONTENT = 2;
    private static final int TT_ICON = 3;
    private static final int TT_COMPONENT = 4;
    private static final int TT_PROGRESS_COURSE = 5;
    private static final String SECTION_NAME = "section";
    private static final String PARAGRAPH_NAME = "paragraph";
    private static final String CONTENT_NAME = "content";
    private static final String COMPONENT_NAME = "component";
    private static final String ICON_NAME = "icon";
    private static final String ALIGNMENT_NAME = "Alignment";
    private static final String FOREGROUND_NAME = "foreground";
    private static final String SIZE_NAME = "size";
    private static final String BOLD_NAME = "bold";
    private static final String ITALIC_NAME = "italic";
    private static final String UNDERLINE_NAME = "underline";
    private static final String TEXT_NAME = "text";
    private static final String NAME_NAME = "name";
    private static final String LOGICAL_STYLE_NAME = "logicalStyle";
    private static final String PROGRESS_COURSE_NAME = "kartePane";
    private static final String[] REPLACES = new String[]{"<", ">", "&", "'", "\""};
    private static final String[] MATCHES = new String[]{"&lt;", "&gt;", "&amp;", "&apos;", "&quot;"};
    
    private static final String NAME_STAMP_HOLDER = "name=\"stampHolder\"";
    
    private static KarteRenderer_2 instance;
    
    static {
        instance = new KarteRenderer_2();
    }
    
    private KarteRenderer_2() {
    }
    
    public static KarteRenderer_2 getInstance() {
        return instance;
    }
    
/*
    private KartePane soaPane;
    private KartePane pPane;

    public KarteRenderer_2(KartePane soaPane, KartePane pPane) {
        this.soaPane = soaPane;
        this.pPane = pPane;
    }
*/
    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    @SuppressWarnings("unchecked")
    public void render(DocumentModel model, KartePane soaPane, KartePane pPane) {

        List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        List<ModuleModel> soaModules = new ArrayList<ModuleModel>();
        List<ModuleModel> pModules = new ArrayList<ModuleModel>();
        List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {

            String role = bean.getModuleInfoBean().getStampRole();

            if (role.equals(IInfoModel.ROLE_SOA)) {
                soaModules.add(bean);
            } else if (role.equals(IInfoModel.ROLE_SOA_SPEC)) {
                soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
            } else if (role.equals(IInfoModel.ROLE_P)) {
                pModules.add(bean);
            } else if (role.equals(IInfoModel.ROLE_P_SPEC)) {
                pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
            }
        }

        // 念のためソート
        Collections.sort(soaModules);
        Collections.sort(pModules);

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
        if (soaSpec == null || soaSpec.equals("")) {
            // soaにModuleModelはないはずだよね… あ、モディファイ版にはあるかもしれない…
            for (ModuleModel mm : soaModules) {
                soaPane.stamp(mm);
            }

        } else {
            debug("Render SOA Pane");
            debug("Module count = " + soaModules.size());
            new KartePaneRenderer().renderPane(soaSpec, soaModules, schemas, soaPane);
        }

        // P Pane をレンダリングする
        if (pSpec == null || pSpec.equals("")) {
            // 前回処方など適用
            for (ModuleModel mm : pModules) {
                pPane.stamp(mm);
            }
        } else {
            debug("Render P Pane");
            debug("Module count = " + pModules.size());
            new KartePaneRenderer().renderPane(pSpec, pModules, schemas, pPane);
            // StampHolder直後の改行がない場合は補う
            pPane.getDocument().fixCrAfterStamp();
        }
    }

    // クラスを分離した
    private class KartePaneRenderer {

        private KartePane kartePane;
        private boolean logicalStyle;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KartePane kartePane) {

            debug(xml);
            this.modules = modules;
            this.schemas = schemas;
            this.kartePane = kartePane;

            SAXBuilder docBuilder = new SAXBuilder();

            try {
                StringReader sr = new StringReader(xml);
                Document doc = docBuilder.build(new BufferedReader(sr));
                Element root = doc.getRootElement();
                writeChildren(root);

            } // indicates a well-formedness error
            catch (JDOMException e) {
                e.printStackTrace(System.err);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        /**
         * 子要素をパースする。
         *
         * @param current 要素
         */
        private void writeChildren(Element current) {

            int eType = -1;
            String eName = current.getName();

            if (eName.equals(PARAGRAPH_NAME)) {
                eType = TT_PARAGRAPH;
                startParagraph(current.getAttributeValue(LOGICAL_STYLE_NAME),
                        current.getAttributeValue(ALIGNMENT_NAME));

            } else if (eName.equals(CONTENT_NAME) && (current.getChild(TEXT_NAME) != null)) {
                eType = TT_CONTENT;
                startContent(current.getAttributeValue(FOREGROUND_NAME),
                        current.getAttributeValue(SIZE_NAME),
                        current.getAttributeValue(BOLD_NAME),
                        current.getAttributeValue(ITALIC_NAME),
                        current.getAttributeValue(UNDERLINE_NAME),
                        current.getChildText(TEXT_NAME));

            } else if (eName.equals(COMPONENT_NAME)) {
                eType = TT_COMPONENT;
                startComponent(current.getAttributeValue(NAME_NAME), // compoenet=number
                        current.getAttributeValue(COMPONENT_ELEMENT_NAME));

            } else if (eName.equals(ICON_NAME)) {
                eType = TT_ICON;
                startIcon(current);

            } else if (eName.equals(PROGRESS_COURSE_NAME)) {
                eType = TT_PROGRESS_COURSE;
                startProgressCourse();

            } else if (eName.equals(SECTION_NAME)) {
                eType = TT_SECTION;
                startSection();

            } else {
                debug("Other element:" + eName);
            }

            // 子を探索するのはパラグフとトップ要素のみ
            if (eType == TT_PARAGRAPH || eType == TT_PROGRESS_COURSE || eType == TT_SECTION) {

                List children = current.getChildren();
                Iterator iterator = children.iterator();

                while (iterator.hasNext()) {
                    Element child = (Element) iterator.next();
                    writeChildren(child);
                }
            }

            switch (eType) {
                case TT_PARAGRAPH:
                    endParagraph();
                    break;
                case TT_CONTENT:
                    endContent();
                    break;
                case TT_ICON:
                    endIcon();
                    break;
                case TT_COMPONENT:
                    endComponent();
                    break;
                case TT_PROGRESS_COURSE:
                    endProgressCourse();
                    break;
                case TT_SECTION:
                    endSection();
                    break;
            }
        }

        private void startSection() {
        }

        private void endSection() {
        }

        private void startProgressCourse() {
        }

        private void endProgressCourse() {
        }

        private void startParagraph(String lStyle, String alignStr) {

            // if (lStyle != null) {
            kartePane.setLogicalStyle("default");
            logicalStyle = true;
            // }

            if (alignStr != null) {
                DefaultStyledDocument doc = (DefaultStyledDocument) kartePane.getTextPane().getDocument();
                Style style0 = doc.getStyle("default");
                Style style = doc.addStyle("alignment", style0);
                if (alignStr.equals("0")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                } else if (alignStr.equals("1")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
                } else if (alignStr.equals("2")) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                }
                kartePane.setLogicalStyle("alignment");
                logicalStyle = true;
            }
        }

        private void endParagraph() {
            //thePane.makeParagraph(); // trim() の廃止で廃止
            if (logicalStyle) {
                kartePane.clearLogicalStyle();
                logicalStyle = false;
            }
        }

        private void startContent(
                String foreground,
                String size,
                String bold,
                String italic,
                String underline,
                String text) {

            // 特殊文字を戻す
            for (int i = 0; i < REPLACES.length; i++) {
                text = text.replaceAll(MATCHES[i], REPLACES[i]);
            }

            // このコンテントに設定する AttributeSet
            MutableAttributeSet atts = new SimpleAttributeSet();

            // foreground 属性を設定する
            if (foreground != null) {
                StringTokenizer stk = new StringTokenizer(foreground, ",");
                if (stk.hasMoreTokens()) {
                    int r = Integer.parseInt(stk.nextToken());
                    int g = Integer.parseInt(stk.nextToken());
                    int b = Integer.parseInt(stk.nextToken());
                    StyleConstants.setForeground(atts, new Color(r, g, b));
                }
            }

            // size 属性を設定する
            if (size != null) {
                StyleConstants.setFontSize(atts, Integer.parseInt(size));
            }
            // bold 属性を設定する
            if (bold != null) {
                StyleConstants.setBold(atts, Boolean.valueOf(bold).booleanValue());
            }
            // italic 属性を設定する
            if (italic != null) {
                StyleConstants.setItalic(atts, Boolean.valueOf(italic).booleanValue());
            }
            // underline 属性を設定する
            if (underline != null) {
                StyleConstants.setUnderline(atts, Boolean.valueOf(underline).booleanValue());
            }

            // テキストを挿入する
            kartePane.insertFreeString(text, atts);
        }

        private void endContent() {
        }

        private void startComponent(String name, String number) {

            debug("Entering startComponent");
            debug("Name = " + name);
            debug("Number = " + number);

            int index = Integer.valueOf(number);
            try {
                if (name != null && name.equals(STAMP_HOLDER)) {
                    ModuleModel stamp = modules.get(index);
                    kartePane.flowStamp(stamp);
                } else if (name != null && name.equals(SCHEMA_HOLDER)) {
                    kartePane.flowSchema(schemas.get(index));
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        private void endComponent() {
        }

        private void startIcon(Element current) {

            String name = current.getChildTextTrim("name");

            if (name != null) {
                debug(name);
            }
        }

        private void endIcon() {
        }
    }

    private void debug(String msg) {
        //ClientContext.getBootLogger().debug(msg);
    }
}