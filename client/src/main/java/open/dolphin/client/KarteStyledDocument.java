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

    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String COMPONENT_NAME = StyleConstants.ComponentElementName;   // "component";
    private static final String NAME_NAME = StyleConstants.NameAttribute.toString();
    private static final String CR = "\n";
    private static final String SPC = " ";
    private static final String DEFAULT_STYLE_NAME = StyleContext.DEFAULT_STYLE;

    // KartePane
    private KartePane kartePane;

    public KarteStyledDocument() {
        // コンストラクタでdefalt styleを設定しておく
        setLogicalStyle(DEFAULT_STYLE_NAME);
    }

    public void setParent(KartePane kartePane) {
        this.kartePane = kartePane;
    }

    public final void setLogicalStyle(String str) {
        Style style = this.getStyle(str);
        this.setLogicalStyle(this.getLength(), style);
    }

    public void clearLogicalStyle() {
        this.setLogicalStyle(this.getLength(), null);
    }

    public void makeParagraph() {
        try {
            insertString(getLength(), CR, null);
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }
    }

//masuda^   コンポーネントのAttributeを作成する
    public MutableAttributeSet createStampAttribute() {
        MutableAttributeSet atts = new SimpleAttributeSet();
        atts.addAttribute(NAME_NAME, STAMP_HOLDER);
        return atts;
    }

    public MutableAttributeSet createSchemaAttribute() {
        MutableAttributeSet atts = new SimpleAttributeSet();
        atts.addAttribute(NAME_NAME, SCHEMA_HOLDER);
        return atts;
    }
//masuda$

    /**
     * Stamp を挿入する。
     *
     * @param sh 挿入するスタンプホルダ
     */
    public void stamp(final StampHolder sh) {

        try {
            // StampHolderの属性をセットする
            MutableAttributeSet atts = createStampAttribute();
            StyleConstants.setComponent(atts, sh);

            // キャレット位置を取得する
            int start = kartePane.getTextPane().getCaretPosition();

            // Stamp を挿入する
            if (Project.getBoolean("stampSpace")) {
                insertString(start, CR, null);
                insertString(start + 1, SPC, atts);
                // 改行をつけないとテキスト入力制御がやりにくくなる
                insertString(start + 2, CR, null);
                // スタンプの開始と終了位置を生成して保存する
                sh.setEntry(createPosition(start + 1), createPosition(start + 2));
            } else {
                insertString(start, SPC, atts);
                // 改行をつけないとテキスト入力制御がやりにくくなる
                insertString(start + 1, CR, null);
                // スタンプの開始と終了位置を生成して保存する
                sh.setEntry(createPosition(start), createPosition(start + 1));
            }

        } catch (BadLocationException | NullPointerException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Stamp を挿入する。
     *
     * @param sh 挿入するスタンプホルダ
     */
    public void flowStamp(final StampHolder sh) {

        try {
            // StampHolderの属性をセットする
            MutableAttributeSet atts = createStampAttribute();
            StyleConstants.setComponent(atts, sh);

            // 挿入位置を取得する
            int start = this.getLength();

            // Stamp を挿入する
            insertString(start, SPC, atts);

            // スタンプの開始と終了位置を生成して保存する
            Position stPos = createPosition(start);
            Position endPos = createPosition(start + 1);
            sh.setEntry(stPos, endPos);

        } catch (BadLocationException | NullPointerException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Stampを削除する。
     *
     * @param start 削除開始のオフセット位置
     * @param len
     */
    public void removeStamp(int start, int len) {

        try {
//masuda^   Stamp/Schemaをremoveするときは直後の改行も削除する
            // Stamp は一文字で表されている
            //remove(start, 1);
            if (start < getLength() && CR.equals(getText(start + 1, 1))) {
                remove(start, 2);
            } else {
                remove(start, 1);
            }
//masuda$
        } catch (BadLocationException be) {
            be.printStackTrace(System.err);
        }
    }

    /**
     * Stampを指定されたポジションに挿入する。
     *
     * @param inPos　挿入ポジション
     * @param sh　挿入する StampHolder
     */
    public void insertStamp(Position inPos, StampHolder sh) {

        try {
            // StampHolderの属性をセットする
            MutableAttributeSet atts = createStampAttribute();
            StyleConstants.setComponent(atts, sh);

            // 挿入位置
            int start = inPos.getOffset();
            // Stamp を挿入する
            insertString(start, SPC, atts);
            // スタンプの開始と終了位置を生成して保存する
            sh.setEntry(createPosition(start), createPosition(start + 1));
            
        } catch (BadLocationException be) {
            be.printStackTrace(System.err);
        }
    }

    public void stampSchema(SchemaHolder sh) {

        try {
            // SchemaHolderの属性をセットする
            MutableAttributeSet atts = createSchemaAttribute();
            StyleConstants.setComponent(atts, sh);

            // 挿入位置
            int start = kartePane.getTextPane().getCaretPosition();
            // シェーマを挿入する
            insertString(start, SPC, atts);
            insertString(start + 1, CR, null);

            // シェーマの開始と終了位置を生成して保存する
            sh.setEntry(createPosition(start), createPosition(start + 1));

        } catch (BadLocationException be) {
            be.printStackTrace(System.err);
        }
    }

    public void flowSchema(final SchemaHolder sh) {

        try {
            // SchemaHolderの属性をセットする
            MutableAttributeSet atts = createSchemaAttribute();
            StyleConstants.setComponent(atts, sh);

            // 挿入位置を取得する
            int start = this.getLength();

            // シェーマを挿入する
            insertString(start, SPC, atts);

            // シェーマの開始と終了位置を生成して保存する
            sh.setEntry(createPosition(start), createPosition(start + 1));

        } catch (BadLocationException | NullPointerException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void insertTextStamp(String text) {

        try {
            //System.out.println("insertTextStamp");
            clearLogicalStyle();
            setLogicalStyle(DEFAULT_STYLE_NAME); // mac 2207-03-31
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

//masuda^   KarteStyledDocument内のStampHolderを取得する。
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
                ch.setEntry(createPosition(pos), createPosition(pos + 1));
                return;
            }
        }
        super.insertString(offs, str, a);
    }

    // KartePaneを返す。SOA/PTransferHandlerでインポート先を、
    // JTextPane->KarteStyledDocument->KartePaneとたぐることができる
    public KartePane getKartePane() {
        return kartePane;
    }

    public void processBatch(List<ElementSpec> specList) throws BadLocationException {
        ElementSpec[] specs = new ElementSpec[specList.size()];
        specList.toArray(specs);
        super.insert(0, specs);
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
                        Position endP = createPosition(elem.getEndOffset());
                        ch.setEntry(startP, endP);
                    } catch (BadLocationException ex) {
                    }
                }
            }

        }
    }
//masuda$

}
