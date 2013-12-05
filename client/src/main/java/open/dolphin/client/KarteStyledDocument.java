package open.dolphin.client;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.*;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.project.Project;

/**
 * KartePane の StyledDocument class。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class KarteStyledDocument extends DefaultStyledDocument {

    private static final String COMPONENT_NAME = StyleConstants.ComponentElementName;   // "component";
    private static final String NAME_NAME = StyleConstants.NameAttribute.toString();
    private static final String CR = "\n";
    private static final String SPC = " ";
    private static final String DEFAULT_STYLE_NAME = StyleContext.DEFAULT_STYLE;

    // KartePane
    private final KartePane kartePane;

    public KarteStyledDocument(KartePane kartePane) {

        this.kartePane = kartePane;
        // コンストラクタでdefalt styleを設定しておく
        setDefaultStyle();
    }

    // KartePaneを返す。SOA/PTransferHandlerでインポート先を、
    // JTextPane->KarteStyledDocument->KartePaneとたぐることができる
    public KartePane getKartePane() {
        return kartePane;
    }

    public final Style setDefaultStyle() {
        Style style = getStyle(DEFAULT_STYLE_NAME);
        setLogicalStyle(getLength(), style);
        return style;
    }

    public void clearLogicalStyle() {
        this.setLogicalStyle(getLength(), null);
    }

    // ComponentHolderのAttributeSetを作成する
    public MutableAttributeSet createComponentAttribute(ComponentHolder ch) {
        MutableAttributeSet atts = new SimpleAttributeSet();
        StyleConstants.setComponent(atts, (Component) ch);
        atts.addAttribute(NAME_NAME, ch.getAttributeName());
        return atts;
    }

    /**
     * CompohentHolderをスタンプする
     *
     * @param ch スタンプするComponentHolder
     */
    public void stampComponent(ComponentHolder ch) {

        try {
            // キャレット位置を取得する
            int start = kartePane.getTextPane().getCaretPosition();

            // ComponentHolderを挿入する
            if (Project.getBoolean("stampSpace")) {
                insertCR(start++);
            }
            insertComponentHolder(start++, ch);
            insertCR(start);

        } catch (BadLocationException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * ComponentHolderを流し込む KarteRenderer_2から
     *
     * @param ch 流し込むComponentHolder
     */
    public void flowComponent(ComponentHolder ch) {

        try {
            // 挿入位置を取得する
            int start = getLength();
            // ComponentHolderを挿入する
            insertComponentHolder(start, ch);

        } catch (BadLocationException ex) {
            ex.printStackTrace(System.err);
        }
    }

    // 改行する
    private void insertCR(int pos) throws BadLocationException {
        insertString(pos, CR, null);
    }

    // ComponentHolderを挿入する
    private void insertComponentHolder(int pos, ComponentHolder ch) throws BadLocationException {
        
        // ComponentHolderのAttributeを設定する
        MutableAttributeSet atts = createComponentAttribute(ch);
        insertString(pos, SPC, atts);
        // ComponentHolderのPositionを設定する
        ch.setStartPosition(createPosition(pos));
    }
    
    /**
     * Stampを削除する。
     *
     * @param pos 削除開始のオフセット位置
     */
    public void removeComponent(int pos) {

        // Stamp/Schemaをremoveするときは直後の改行も削除する
        // Stamp は一文字で表されている
        try {
            if (pos < getLength() && CR.equals(getText(pos + 1, 1))) {
                remove(pos, 2);
            } else {
                remove(pos, 1);
            }
        } catch (BadLocationException be) {
            be.printStackTrace(System.err);
        }
    }

    public void insertTextStamp(String text) {

        try {
            setDefaultStyle();
            int pos = kartePane.getTextPane().getCaretPosition();
            insertString(pos, text, null);
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }
    }

    public void insertFreeString(String text, AttributeSet a) {
        try {
            insertString(getLength(), text, a);
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }
    }


    // KarteStyledDocument内のStampHolderを取得する。
    public List<StampHolder> getStampHolders() {

        List<StampHolder> list = new ArrayList<>();
        List<Component> components = getAllComponents();
        for (Component c : components) {
            if (c instanceof StampHolder) {
                list.add((StampHolder) c);
            }
        }
        return list;
    }

    public List<SchemaHolder> getSchemaHolders() {

        List<SchemaHolder> list = new ArrayList<>();
        List<Component> components = getAllComponents();
        for (Component c : components) {
            if (c instanceof SchemaHolder) {
                list.add((SchemaHolder) c);
            }
        }
        return list;
    }

    private List<Component> getAllComponents() {

        List<Component> list = new ArrayList<>();
        ElementIterator itr = new ElementIterator(this);

        for (Element elem = itr.first(); elem != null; elem = itr.next()) {
            if (COMPONENT_NAME.equals(elem.getName())) {
                list.add(StyleConstants.getComponent(elem.getAttributes()));
            }
        }

        return list;
    }

    // StampHolder内のModuleModelだけ返す
    public List<ModuleModel> getStamps() {

        List<StampHolder> shList = getStampHolders();
        List<ModuleModel> list = new ArrayList<>();
        for (StampHolder sh : shList) {
            list.add(sh.getStamp());
        }
        return list;
    }

    // 文書末の余分な改行文字を削除する
    public void removeExtraCR() {

        int len = getLength();
        int pos;
        try {
            // 改行文字以外が出てくるまで文書末からスキャン
            for (pos = len - 1; pos >= 0; --pos) {
                if (!CR.equals(getText(pos, 1))) {
                    break;
                }
            }
            // 一文字戻す
            ++pos;
            if (len - pos > 0) {
                remove(pos, len - pos);
            }

        } catch (BadLocationException ex) {
        }
    }

    // 文頭に挿入する場合、現在の文頭がComponentHolderならばそのEntryを更新する。
    // position=0は特殊で移動しても変わらないため、スタンプホルダの開始位置がずれてしまうことへの対応 11/06/07
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

        if (offs == 0) {
            ComponentHolder ch = (ComponentHolder) StyleConstants.getComponent(getCharacterElement(offs).getAttributes());
            if (ch != null) {
                super.insertString(offs, str, a);
                int pos = offs + str.length();
                ch.setStartPosition(createPosition(pos));
                return;
            }
        }
        super.insertString(offs, str, a);
    }

    public void createDocument(List<ElementSpec> specList) {
        ElementSpec[] specs = new ElementSpec[specList.size()];
        specList.toArray(specs);
        super.create(specs);
    }

    // ComponentHolderのStart/End Positionを設定する
    public void setComponentPositions() {

        ElementIterator itr = new ElementIterator(this);

        for (Element elem = itr.first(); elem != null; elem = itr.next()) {

            if (COMPONENT_NAME.equals(elem.getName())) {
                ComponentHolder ch = (ComponentHolder) StyleConstants.getComponent(elem.getAttributes());
                if (ch != null) {
                    try {
                        Position startP = createPosition(elem.getStartOffset());
                        ch.setStartPosition(startP);
                    } catch (BadLocationException ex) {
                    }
                }
            }

        }
    }
}
