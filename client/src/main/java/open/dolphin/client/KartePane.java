package open.dolphin.client;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledEditorKit;
import open.dolphin.client.ChartMediator.CompState;
import open.dolphin.dao.SqlOrcaSetDao;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.helper.ImageHelper;
import open.dolphin.infomodel.*;
import open.dolphin.order.AbstractStampEditor;
import open.dolphin.order.StampEditor;
import open.dolphin.plugin.PluginLoader;
import open.dolphin.project.Project;
import open.dolphin.tr.*;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.util.DicomImageEntry;
import open.dolphin.util.ImageTool;
import open.dolphin.util.NonHidePopupMenu;
import org.apache.log4j.Logger;

/**
 * Karte Pane
 *
 * @author Kazushi Minagawa, Digital Globe, inc.
 */
public class KartePane implements MouseListener, CaretListener, PropertyChangeListener {

    // 文書に付けるタイトルを自動で取得する時の長さ
    private static final int TITLE_LENGTH = 15;

    // 編集不可時の背景色
    private static final Color UNEDITABLE_COLOR = new Color(227, 250, 207);

    // Schema/Image 設定用定数
    private static final String MEDIA_TYPE_IMAGE_JPEG = "image/jpeg";
    private static final String DEFAULT_IMAGE_TITLE = "Schema Image";
    private static final String JPEG_EXT = ".jpg";

    // JTextPane
    private JTextPane textPane;

    // SOA または P のロール
    private String myRole;

    // この KartePaneのオーナ
    private ChartDocument parent;

    private int stampId;

    // Selection Flag
    private boolean hasSelection;

    private CompState curState;

    // ChartMediator(MenuSupport)
    private ChartMediator mediator;

    // このオブジェクトで生成する文書DocumentModelの文書ID
    private String docId;

    // 保存後及びブラウズ時の編集不可を表すカラー
    private Color uneditableColor = UNEDITABLE_COLOR;

    private final Logger logger;

    /** 
     * Creates new KartePane2 
     */
    public KartePane() {
        logger = ClientContext.getBootLogger();
    }

    public void setMargin(Insets margin) {
        textPane.setMargin(margin);
    }

    public void setPreferredSize(Dimension size) {
        textPane.setPreferredSize(size);
    }

    public void setSize(Dimension size) {
        textPane.setMinimumSize(size);
        textPane.setMaximumSize(size);
    }

    /**
     * このPaneのオーナを設定する。
     * @param parent KarteEditorオーナ
     */
    public void setParent(ChartDocument parent) {
        this.parent = parent;
    }

    /**
     * このPaneのオーナを返す。
     * @return KarteEditorオーナ
     */
    public ChartDocument getParent() {
        return parent;
    }

    /**
     * 編集不可を表すカラーを設定する。
     * @param uneditableColor 編集不可を表すカラー
     */
    public void setUneditableColor(Color uneditableColor) {
        this.uneditableColor = uneditableColor;
    }

    /**
     * 編集不可を表すカラーを返す。
     * @return 編集不可を表すカラー
     */
    public Color getUneditableColor() {
        return uneditableColor;
    }

    /**
     * このPaneで生成するDocumentModelの文書IDを設定する。
     * @param docId 文書ID
     */
    protected void setDocId(String docId) {
        this.docId = docId;
    }

    /**
     * このPaneで生成するDocumentModelの文書IDを返す。
     * @return 文書ID
     */
    protected String getDocId() {
        return docId;
    }

    /**
     * ChartMediatorを設定する。
     * @param mediator ChartMediator
     */
    protected void setMediator(ChartMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * ChartMediatorを返す。
     * @return ChartMediator
     */
    protected ChartMediator getMediator() {
        return mediator;
    }

    /**
     * このPaneのロールを設定する。
     * @param myRole SOAまたはPのロール
     */
    public void setMyRole(String myRole) {
        this.myRole = myRole;
    }

    /**
     *  このPaneのロールを返す。
     * @return SOAまたはPのロール
     */
    public String getMyRole() {
        return myRole;
    }

    /**
     * JTextPaneを設定する。
     * @param textPane JTextPane
     */
    public void setTextPane(JTextPane textPane) {
        this.textPane = textPane;
//masuda^   off screen updates trickのため、KarteStyledDocumentの設定は後回し
/*
        if (this.textPane != null) {
            KarteStyledDocument doc = new KarteStyledDocument();
            this.textPane.setDocument(doc);
            doc.setParent(this);
        }
*/
    }
    
    // あらたにKarteStyledDocumentを設定する
    public void initKarteStyledDocument() {
        KarteStyledDocument doc = new KarteStyledDocument(KartePane.this);
        this.textPane.setDocument(doc);
    }
//masuda$    

    /**
     * JTextPaneを返す。
     * @return JTextPane
     */
    public JTextPane getTextPane() {
        return textPane;
    }

    /**
     * JTextPaneのStyledDocumentを返す。
     * @return JTextPaneのStyledDocument
     */
//masuda    protected -> public
    public KarteStyledDocument getDocument() {
        return (KarteStyledDocument) getTextPane().getDocument();
    }

    /**
     * 初期化する。
     * @param editable 編集可能かどうかのフラグ
     * @param mediator チャートメディエータ（実際にはメニューサポート）
     */
    public void init(boolean editable, ChartMediator mediator) {

        // Mediatorを保存する
        setMediator(mediator);

        // Drag は editable に関係なく可能
        getTextPane().setDragEnabled(true);

        // リスナを登録する
        getTextPane().addMouseListener(this);
        getTextPane().addCaretListener(this);

        // Editable Property を設定する
        setEditableProp(editable);
    }
    
//masuda^   UndoableEditListenerはChartMediatorではなくKarteEditorに設定する
    /**
     * 編集可否を設定する。それに応じてリスナの登録または取り除きを行う。
     * @param editable 編集可の時 true
     */
    public void setEditableProp(boolean editable) {
        getTextPane().setEditable(editable);
        if (editable) {
            getTextPane().addFocusListener(AutoKanjiListener.getInstance());
            if (parent instanceof KarteEditor) {
                KarteEditor editor = (KarteEditor) parent;
                getTextPane().getDocument().addUndoableEditListener(editor);
            }
            if (IInfoModel.ROLE_SOA.equals(myRole)) {
                SOACodeHelper helper = new SOACodeHelper(this, getMediator());
            } else if (IInfoModel.ROLE_P.equals(myRole)) {
                PCodeHelper helper = new PCodeHelper(this, getMediator());
            }
            getTextPane().setBackground(Color.WHITE);
            getTextPane().setOpaque(true);
        } else {
            getTextPane().removeFocusListener(AutoKanjiListener.getInstance());
            if (parent instanceof KarteEditor) {
                KarteEditor editor = (KarteEditor) parent;
                getTextPane().getDocument().removeUndoableEditListener(editor);
            }
            setBackgroundUneditable();
        }
    }
//masuda$

    @Override
    public void caretUpdate(CaretEvent e) {
        boolean newSelection = (e.getDot() != e.getMark());
        if (newSelection != hasSelection) {
            hasSelection = newSelection;

            // テキスト選択の状態へ遷移する
            if (hasSelection) {
                curState = getMyRole().equals(IInfoModel.ROLE_SOA) ? CompState.SOA_TEXT : CompState.P_TEXT;
            } else {
                curState = getMyRole().equals(IInfoModel.ROLE_SOA) ? CompState.SOA : CompState.P;
            }
            controlMenus(mediator.getActions());
        }
    }

    /**
     * リソースをclearする。
     */
    public void clear() {
        getTextPane().removeMouseListener(this);
        getTextPane().removeFocusListener(AutoKanjiListener.getInstance());
        getTextPane().removeCaretListener(this);

        try {
            KarteStyledDocument doc = getDocument();
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }

        setTextPane(null);
    }

    /**
     * メニューを制御する。
     *
     */
    private void controlMenus(ActionMap map) {

        // 各Stateはenableになる条件だけを管理する
        switch (curState) {

            case NONE:
                break;

            case SOA:
                // SOAPaneにFocusがありテキスト選択がない状態
                if (getTextPane().isEditable()) {
                    map.get(GUIConst.ACTION_PASTE).setEnabled(canPaste());
                    map.get(GUIConst.ACTION_INSERT_TEXT).setEnabled(true);
                    map.get(GUIConst.ACTION_INSERT_SCHEMA).setEnabled(true);
                }
                break;

            case SOA_TEXT:
                // SOAPaneにFocusがありテキスト選択がある状態
                map.get(GUIConst.ACTION_CUT).setEnabled(getTextPane().isEditable());
                map.get(GUIConst.ACTION_COPY).setEnabled(true);
                boolean pasteOk = (getTextPane().isEditable() && canPaste());
                map.get(GUIConst.ACTION_PASTE).setEnabled(pasteOk);
                break;

            case P:
                // PPaneにFocusがありテキスト選択がない状態
                if (getTextPane().isEditable()) {
                    map.get(GUIConst.ACTION_PASTE).setEnabled(canPaste());
                    map.get(GUIConst.ACTION_INSERT_TEXT).setEnabled(true);
                    map.get(GUIConst.ACTION_INSERT_STAMP).setEnabled(true);
                }
                break;

            case P_TEXT:
                // PPaneにFocusがありテキスト選択がある状態
                map.get(GUIConst.ACTION_CUT).setEnabled(getTextPane().isEditable());
                map.get(GUIConst.ACTION_COPY).setEnabled(true);
                pasteOk = (getTextPane().isEditable() && canPaste());
                map.get(GUIConst.ACTION_PASTE).setEnabled(pasteOk);
                break;
        }
    }

    // ペイン内の右クリックメニューを生成する
    protected JPopupMenu createMenus() {

        //final JPopupMenu contextMenu = new JPopupMenu();
        final JPopupMenu contextMenu = new NonHidePopupMenu();
        
        // cut, copy, paste メニューを追加する
        contextMenu.add(mediator.getAction(GUIConst.ACTION_CUT));
        contextMenu.add(mediator.getAction(GUIConst.ACTION_COPY));
        contextMenu.add(mediator.getAction(GUIConst.ACTION_PASTE));

        // テキストカラーメニューを追加する
        if (getTextPane().isEditable()) {
            ColorChooserComp ccl = new ColorChooserComp();
            ccl.addPropertyChangeListener(ColorChooserComp.SELECTED_COLOR, new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    Color selected = (Color) e.getNewValue();
                    Action action = new StyledEditorKit.ForegroundAction("selected", selected);
                    action.actionPerformed(new ActionEvent(getTextPane(), ActionEvent.ACTION_PERFORMED, "foreground"));
                    contextMenu.setVisible(false);
                }
            });
            JLabel l = new JLabel("  カラー:");
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            p.add(l);
            p.add(ccl);
            contextMenu.add(p);
        } else {
            contextMenu.addSeparator();
        }

        // PPane の場合は処方日数変更とStampMenuを追加する
        if (getMyRole().equals(IInfoModel.ROLE_P)) {
            contextMenu.addSeparator();
            contextMenu.add(mediator.getAction(GUIConst.ACTION_CHANGE_NUM_OF_DATES_ALL));
            contextMenu.addSeparator();
            mediator.addStampMenu(contextMenu, this);
        } else {
            // TextMenuを追加する
            mediator.addTextMenu(contextMenu);
        }

        return contextMenu;
    }

    private void mabeShowPopup(final MouseEvent e) {
        if (e.isPopupTrigger()) {
//masuda^   textPaneにフォーカスを当ててからにする
            textPane.requestFocusInWindow();
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    JPopupMenu contextMenu = createMenus();
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });
//masuda$
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mabeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mabeShowPopup(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * 背景を編集不可カラーに設定する。
     */
    protected void setBackgroundUneditable() {
        getTextPane().setBackground(getUneditableColor());
        getTextPane().setOpaque(true);
    }

    /**
     * ロールとパートナを設定する。
     * @param role このペインのロール
     * @param partner パートナ
     */
    public void setRole(String role) {
        setMyRole(role);
    }
    
    public void setDirty(boolean dirty) {
        parent.setDirty(dirty);
    }
    
    /**
     * 保存時につけるドキュメントのタイトルをDocument Objectから抽出する。
     * @return 先頭から指定された長さを切り出した文字列
     */
    protected String getTitle() {
        try {
            KarteStyledDocument doc = getDocument();
            int len = doc.getLength();
            int freeTop = 0; // doc.getFreeTop();
            int freeLen = len - freeTop;
            freeLen = freeLen < TITLE_LENGTH ? freeLen : TITLE_LENGTH;
            return getTextPane().getText(freeTop, freeLen).trim();
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * Documentに文字列を挿入する。
     * @param str 挿入する文字列
     * @param atts 属性
     */
    public void insertFreeString(String str, AttributeSet atts) {
        getDocument().insertFreeString(str, atts);
    }

    /**
     * このペインに Stamp を挿入する。
     */
    public void stamp(final ModuleModel stamp) {
        
        if (stamp != null) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    StampHolder sh = new StampHolder(KartePane.this, stamp);
                    sh.setTransferHandler(StampHolderTransferHandler.getInstance());
                    KarteStyledDocument doc = getDocument();
                    doc.stampComponent(sh);
                }
            });
        }
    }

    /**
     * このペインに Stamp を挿入する。
     */
    public void flowStamp(ModuleModel stamp) {
        
        if (stamp != null) {
            StampHolder sh = new StampHolder(KartePane.this, stamp);
            sh.setTransferHandler(StampHolderTransferHandler.getInstance());
            KarteStyledDocument doc = getDocument();
            doc.flowComponent(sh);
        }
    }

    /**
     * このペインにシェーマを挿入する。
     * @param schema シェーマ
     */
    public void stampSchema(final SchemaModel schema) {
        
        if (schema != null) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    SchemaHolder sh = new SchemaHolder(KartePane.this, schema);
                    sh.setTransferHandler(SchemaHolderTransferHandler.getInstance());
                    KarteStyledDocument doc = getDocument();
                    doc.stampComponent(sh);
                }
            });
        }
    }

    /**
     * このペインにシェーマを挿入する。
     * @param schema  シェーマ
     */
    public void flowSchema(SchemaModel schema) {
        
        if (schema != null) {
            SchemaHolder sh = new SchemaHolder(KartePane.this, schema);
            sh.setTransferHandler(SchemaHolderTransferHandler.getInstance());
            KarteStyledDocument doc = (KarteStyledDocument) getTextPane().getDocument();
            doc.flowComponent(sh);
        }
    }

    /**
     * このペインに TextStamp を挿入する。
     */
    public void insertTextStamp(final String s) {
        
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                KarteStyledDocument doc = getDocument();
                doc.insertTextStamp(s);
            }
        });
    }

    /**
     * StampInfoがDropされた時、そのデータをペインに挿入する。
     * @param stampInfo ドロップされたスタンプ情報
     */
    public void stampInfoDropped(ModuleInfoBean stampInfo) {

        // Drop された StampInfo の属性に応じて処理を振分ける
        String entity = stampInfo.getEntity();
        String role = stampInfo.getStampRole();

        //------------------------------------
        // 病名の場合は２号カルテペインには展開しない
        //------------------------------------
        if (entity.equals(IInfoModel.ENTITY_DIAGNOSIS)) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        //------------------------------------
        // Text スタンプを挿入する
        //------------------------------------
        if (entity.equals(IInfoModel.ENTITY_TEXT)) {
            applyTextStamp(stampInfo);
            return;
        }

        //------------------------------------
        // ORCA 入力セットの場合
        //------------------------------------
        if (role.equals(IInfoModel.ROLE_ORCA_SET)) {
            applyOrcaSet(stampInfo);
            return;
        }

        //------------------------------------
        // データベースに保存されているスタンプを挿入する
        //------------------------------------
        if (stampInfo.isSerialized()) {
            applySerializedStamp(stampInfo);
            return;
        }

        //------------------------------------
        // Stamp エディタを起動する
        //------------------------------------
        ModuleModel stamp = new ModuleModel();
        stamp.setModuleInfoBean(stampInfo);
//masuda^   複数スタンプ対応
        //StampEditor se = new StampEditor(stamp, this);
        StampEditor se = new StampEditor(new ModuleModel[]{stamp}, this, parent.getContext());
//masuda$
    }

    /**
     * StampInfoがDropされた時、そのデータをペインに挿入する。
     * @param addList スタンプ情報のリスト
     */
    public void stampInfoDropped(final ArrayList<ModuleInfoBean> addList) {
        
        DBTask task = new DBTask<List<StampModel>, Void>(parent.getContext()) {

            @Override
            protected List<StampModel> doInBackground() throws Exception {
//masuda^   シングルトン化       
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                List<StampModel> list = sdl.getStamps(addList);
                return list;
            }
            
            @Override
            public void succeeded(List<StampModel> list) {
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        ModuleInfoBean stampInfo = addList.get(i);
                        StampModel theModel = list.get(i);
                        IInfoModel model = (IInfoModel) BeanUtils.xmlDecode(theModel.getStampBytes());
                        if (model != null) {
                            ModuleModel stamp = new ModuleModel();
                            stamp.setModel(model);
                            stamp.setModuleInfoBean(stampInfo);
                            stamp(stamp);
                        }
                    }
                }
            }
        };
        
        task.execute();
    }

    /**
     * TextStampInfo が Drop された時の処理を行なう。
     */
    public void textStampInfoDropped(final ArrayList<ModuleInfoBean> addList) {
        
        DBTask task = new DBTask<List<StampModel>, Void>(parent.getContext()) {

            @Override
            protected List<StampModel> doInBackground() throws Exception {
//masuda^   シングルトン化       
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                List<StampModel> list = sdl.getStamps(addList);
                return list;
            }
            
            @Override
            public void succeeded(List<StampModel> list) {
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        StampModel theModel = list.get(i);
                        IInfoModel model = (IInfoModel) BeanUtils.xmlDecode(theModel.getStampBytes());
                        if (model != null) {
                            insertTextStamp(model.toString() + "\n");
                        }
                    }
                }
            }
        };
        
        task.execute();
    }

    /**
     * TextStamp をこのペインに挿入する。
     */
    private void applyTextStamp(final ModuleInfoBean stampInfo) {
        
        DBTask task = new DBTask<StampModel, Void>(parent.getContext()) {

            @Override
            protected StampModel doInBackground() throws Exception {
//masuda^   シングルトン化       
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                StampModel getStamp = sdl.getStamp(stampInfo.getStampId());
                return getStamp;
            }
            
            @Override
            public void succeeded(StampModel result) {
                if (result != null) {
                    try {
//masuda^
                        byte[] bytes = result.getStampBytes();
                        //XMLDecoder d = new XMLDecoder(new BufferedInputStream(new ByteArrayInputStream(bytes)));
                        //IInfoModel model = (IInfoModel) d.readObject();
                        //d.close();
                        IInfoModel model = (IInfoModel) BeanUtils.xmlDecode(bytes);
//masuda$

                        if (model != null) {
                            insertTextStamp(model.toString());
                        }

                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        };
        
        task.execute();
    }

    /**
     * 永続化されているスタンプを取得してこのペインに展開する。
     */
    private void applySerializedStamp(final ModuleInfoBean stampInfo) {
        
        DBTask task = new DBTask<StampModel, Void>(parent.getContext()) {

            @Override
            protected StampModel doInBackground() throws Exception {
//masuda^   シングルトン化       
                //StampDelegater sdl = new StampDelegater();
                StampDelegater sdl = StampDelegater.getInstance();
//masuda$
                StampModel getStamp = sdl.getStamp(stampInfo.getStampId());
                return getStamp;
            }
            
            @Override
            public void succeeded(StampModel result) {
                if (result != null) {
                    IInfoModel model = (IInfoModel) BeanUtils.xmlDecode(result.getStampBytes());
                    ModuleModel stamp = new ModuleModel();
                    stamp.setModel(model);
                    stamp.setModuleInfoBean(stampInfo);
                    stamp(stamp);
                }
            }
        };

        task.execute();
    }

    /**
     * ORCA の入力セットを取得してこのペインに展開する。
     */
    private void applyOrcaSet(final ModuleInfoBean stampInfo) {
        
        DBTask task = new DBTask<List<ModuleModel>, Void>(parent.getContext()) {

            @Override
            protected List<ModuleModel> doInBackground() throws Exception {
                SqlOrcaSetDao sdl = SqlOrcaSetDao.getInstance();
                List<ModuleModel> models = sdl.getStamp(stampInfo);
                return models;
            }
            
            @Override
            public void succeeded(List<ModuleModel> models) {
                if (models != null) {
                    for (ModuleModel stamp : models) {
                        stamp(stamp);
                    }
                }
            }
        };

        task.execute();
    }
    
    private void showMetaDataMessage() {
        
        Window w = SwingUtilities.getWindowAncestor(getTextPane());  
        JOptionPane.showMessageDialog(w,
                                      "画像のメタデータが取得できず、読み込むことができません。",
                                      ClientContext.getFrameTitle("画像インポート"),
                                      JOptionPane.WARNING_MESSAGE);
    }
    
    private boolean showMaxSizeMessage() {
        
        int maxImageWidth = ClientContext.getInt("image.max.width");
        int maxImageHeight = ClientContext.getInt("image.max.height");
        
        String title = ClientContext.getFrameTitle("画像サイズについて");
        JLabel msg1 = new JLabel("カルテに挿入する画像は、最大で " + maxImageWidth + " x " + maxImageHeight + " pixcel に制限しています。");
        JLabel msg2 = new JLabel("画像を縮小しカルテに展開しますか?");
        final JCheckBox cb = new JCheckBox("今後このメッセージを表示しない");
        cb.setFont(new Font("Dialog", Font.PLAIN, 10));
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Project.setBoolean("showImageSizeMessage", !cb.isSelected());
            }
        });
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        p1.add(msg1);
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        p2.add(msg2);
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        p3.add(cb);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(p1);
        box.add(p2);
        box.add(p3);
        box.setBorder(BorderFactory.createEmptyBorder(0, 0, 11, 11));
        Window w = SwingUtilities.getWindowAncestor(getTextPane());        

        int option = JOptionPane.showOptionDialog(w,
                            new Object[]{box},
                            ClientContext.getFrameTitle(title),
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            ClientContext.getImageIconAlias("icon_info"),
                            new String[]{"縮小する", "取消す"}, "縮小する");
        return option == 0;
    }
    
    private void showNoReaderMessage() {
        Window w = SwingUtilities.getWindowAncestor(getTextPane());  
        JOptionPane.showMessageDialog(w,
                                      "選択した画像を読むことができるリーダが存在しません。",
                                      ClientContext.getFrameTitle("画像インポート"),
                                      JOptionPane.WARNING_MESSAGE);
    }

    /**
     * ImageTable から ImageEntry が drop された時の処理を行う。
     * entry の URL からイメージをロードし、SchemaEditorへ表示する。
     * @param entry カルテに展開する　 ImageEntry Object
     */
    public void imageEntryDropped(final ImageEntry entry) {
        
        DBTask task = new DBTask<BufferedImage, Void>(parent.getContext()) {

            @Override
            protected BufferedImage doInBackground() throws Exception {
//masuda^   DicomImageEntryの場合はjpegBytesを使う
                if (entry instanceof DicomImageEntry) {
                    byte[] bytes = ((DicomImageEntry) entry).getResizedJpegBytes();
                    return ImageTool.getBufferedImage(bytes);
                }
//masuda$
                URL url = new URL(entry.getUrl());
                BufferedImage importImage = ImageIO.read(url);
                return importImage;
            }
            
            @Override
            public void succeeded(BufferedImage importImage) {
                
                if (importImage != null) {

                    int maxImageWidth = ClientContext.getInt("image.max.width");
                    int maxImageHeight = ClientContext.getInt("image.max.height");

                    if (importImage.getWidth() > maxImageWidth || importImage.getHeight()> maxImageHeight) {
                        boolean ok =  true;
                        if (Project.getBoolean("showImageSizeMessage", true)) {
                            ok = showMaxSizeMessage();
                        }
                        if (ok) {
                            importImage = ImageHelper.getFirstScaledInstance(importImage, maxImageWidth);
                        } else {
                            return;
                        }
                    }

                    ImageIcon icon = new ImageIcon(importImage);
                    SchemaModel schema = new SchemaModel();
                    schema.setIcon(icon);

                    // IInfoModel として ExtRef を保持している
                    ExtRefModel ref = new ExtRefModel();
                    schema.setExtRefModel(ref);

                    ref.setContentType(MEDIA_TYPE_IMAGE_JPEG);   // MIME
                    ref.setTitle(DEFAULT_IMAGE_TITLE);           //
                    ref.setUrl(entry.getUrl());                  // 元画像のURL

                    // href=docID-stampId.jpg
                    stampId++;
                    StringBuilder sb = new StringBuilder();
                    sb.append(getDocId());
                    sb.append("-");
                    sb.append(stampId);
                    sb.append(JPEG_EXT);
                    String fileName = sb.toString();
                    schema.setFileName(fileName);       // href
                    ref.setHref(fileName);              // href
                    
                    PluginLoader<SchemaEditor> loader 
                        = PluginLoader.load(SchemaEditor.class);
                    Iterator<SchemaEditor> iter = loader.iterator();
                    if (iter.hasNext()) {
                        final SchemaEditor editor = iter.next();
                        editor.setSchema(schema);
                        editor.setEditable(true);
                        editor.addPropertyChangeListener(KartePane.this);
                        Runnable awt = new Runnable() {

                            @Override
                            public void run() {
                                editor.start();
                            }
                        };
                        EventQueue.invokeLater(awt);
                    }
                }
            }
        };
        
        task.execute();
    }

    /**
     * ファイルのDropを受け、イメージをカルテに挿入する。
     * @param file Drop されたファイル
     */
    public void imageFileDropped(final File file) {

        DBTask task = new DBTask<BufferedImage, Void>(parent.getContext()) {

            @Override
            protected BufferedImage doInBackground() throws Exception {
                URL url = file.toURI().toURL();
                BufferedImage importImage = ImageIO.read(url);
                return importImage;
            }

            @Override
            public void succeeded(BufferedImage importImage) {

                if (importImage != null) {

                    int maxImageWidth = ClientContext.getInt("image.max.width");
                    int maxImageHeight = ClientContext.getInt("image.max.height");

                    if (importImage.getWidth() > maxImageWidth || importImage.getHeight()> maxImageHeight) {
                        boolean ok =  true;
                        if (Project.getBoolean("showImageSizeMessage", true)) {
                            ok = showMaxSizeMessage();
                        }
                        if (ok) {
                            importImage = ImageHelper.getFirstScaledInstance(importImage, maxImageWidth);
                        } else {
                            return;
                        }
                    }

                    ImageIcon icon = new ImageIcon(importImage);
                    SchemaModel schema = new SchemaModel();
                    schema.setIcon(icon);

                    // IInfoModel として ExtRef を保持している
                    ExtRefModel ref = new ExtRefModel();
                    schema.setExtRefModel(ref);

                    ref.setContentType(MEDIA_TYPE_IMAGE_JPEG);   // MIME
                    ref.setTitle(DEFAULT_IMAGE_TITLE);           //
                    try {
                        ref.setUrl(file.toURI().toURL().toString()); // 元画像のURL
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace(System.err);
                    }

                    // href=docID-stampId.jpg
                    stampId++;
                    StringBuilder sb = new StringBuilder();
                    sb.append(getDocId());
                    sb.append("-");
                    sb.append(stampId);
                    sb.append(JPEG_EXT);
                    String fileName = sb.toString();
                    schema.setFileName(fileName);       // href
                    ref.setHref(fileName);              // href

                    PluginLoader<SchemaEditor> loader
                        = PluginLoader.load(SchemaEditor.class);
                    Iterator<SchemaEditor> iter = loader.iterator();
                    if (iter.hasNext()) {
                        final SchemaEditor editor = iter.next();
                        editor.setSchema(schema);
                        editor.setEditable(true);
                        editor.addPropertyChangeListener(KartePane.this);
                        Runnable awt = new Runnable() {

                            @Override
                            public void run() {
                                editor.start();
                            }
                        };
                        EventQueue.invokeLater(awt);
                    }
                }
            }

            @Override
            public void failed(Throwable e) {
            }
        };

        task.execute();
    }


    /**
     * Schema が DnD された場合、シェーマエディタを開いて編集する。
     */
    public void insertImage(String path) {
        
        if (path == null) {
            return;
        }
        
        String suffix = path.toLowerCase();
        int index = suffix.lastIndexOf('.');
        if (index == 0) {
            showNoReaderMessage();
            return;
        }
        suffix = suffix.substring(index+1);
            
        Iterator readers = ImageIO.getImageReadersBySuffix(suffix);

        if (!readers.hasNext()) {
            showNoReaderMessage();
            return;
        }

        ImageReader reader = (ImageReader) readers.next();
        int width;
        int height;
        String name;
        try {
            File file = new File(path);
            name = file.getName();
            reader.setInput(new FileImageInputStream(file), true);
            width = reader.getWidth(0);
            height = reader.getHeight(0);
            
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return;
        }
        ImageEntry entry = new ImageEntry();
        entry.setPath(path);
        entry.setFileName(name);
        entry.setNumImages(1);
        entry.setWidth(width);
        entry.setHeight(height);
        imageEntryDropped(entry);
    }

    /**
     * StampEditor の編集が終了するとここへ通知される。
     * 通知されたスタンプをペインに挿入する。
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {

        String prop = e.getPropertyName();

        if ("imageProp".equals(prop)) {

            SchemaModel schema = (SchemaModel) e.getNewValue();

            if (schema != null) {
                // 編集されたシェーマをこのペインに挿入する
                stampSchema(schema);
            }

        } else if (AbstractStampEditor.VALUE_PROP.equals(prop)) {

            Object o = e.getNewValue();

            if (o != null) {
//masuda    複数スタンプ
                ModuleModel[] stamps = (ModuleModel[]) o;
                for (ModuleModel stamp : stamps) {
                    stamp(stamp);
                }
            }
        }
    }

    /**
     * メニュー制御のため、ペースト可能かどうかを返す。
     * @return ペースト可能な時 true
     */
    protected boolean canPaste() {

        boolean ret = false;
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null) {
            return false;
        }

        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return true;
        }
        
        if (getMyRole().equals(IInfoModel.ROLE_P)) {
            if (t.isDataFlavorSupported(OrderListTransferable.orderListFlavor) ||
                t.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor)) {
                ret = true;
            }
        } else {
            if (t.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor) ||
                t.isDataFlavorSupported(LocalStampTreeNodeTransferable.localStampTreeNodeFlavor) || 
                t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * このペインからスタンプを削除する。
     * @param sh 削除するスタンプのホルダ
     */
    public void removeStamp(StampHolder sh) {
        // editableの場合のみ削除
        if (getTextPane().isEditable()) {
            getDocument().removeComponent(sh.getStartOffset());
        }
    }

    /**
     * このペインからスタンプを削除する。
     * @param stamps 削除するスタンプのホルダリスト
     */
    public void removeStamp(StampHolder[] stamps) {
        if (stamps != null && stamps.length > 0) {
            for (StampHolder sh : stamps) {
                removeStamp(sh);
            }
        }
    }

    /**
     * このペインからシェーマを削除する。
     * @param sh 削除するシェーマのホルダ
     */
    public void removeSchema(SchemaHolder sh) {
        // editableの場合のみ削除
        if (getTextPane().isEditable()) {
            getDocument().removeComponent(sh.getStartOffset());
        }
    }

    /**
     * このペインからシェーマを削除する。
     * @param schemas 削除するシェーマのホルダリスト
     */
    public void removeSchema(SchemaHolder[] schemas) {
        if (schemas != null && schemas.length > 0) {
            for (SchemaHolder schema : schemas) {
                removeSchema(schema);
            }
        }
    }

    /**
     * 処方日数を一括変更する。
     * @param number　日数
     */
    public void changeAllRPNumDates(int number) {

        final List<StampHolder> list = getRPStamps();

        if (list.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        final String numStr = String.valueOf(number);
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                for (StampHolder sh : list) {
                    ModuleModel module = sh.getStamp();
                    BundleMed med = (BundleMed) module.getModel();
                    med.setBundleNumber(numStr);
                    sh.setStamp(module);
                }
                setDirty(true);
            }
        });
    }

    private List<StampHolder> getRPStamps() {
        return getStamps(IInfoModel.ENTITY_MED_ORDER);
    }

    public boolean hasRP() {
        if (!getTextPane().isEditable()) {
            return false;
        }
        List<StampHolder> list = getStamps(IInfoModel.ENTITY_MED_ORDER);
        return !list.isEmpty();
    }
    
    public boolean hasLabtest() {
        if (!getTextPane().isEditable()) {
            return false;
        }
        List<StampHolder> list = getStamps(IInfoModel.ENTITY_LABO_TEST);
        return !list.isEmpty();
    }

    private List<StampHolder> getStamps(String entity) {
        
        if (entity == null) {
            return Collections.emptyList();
        }

        KarteStyledDocument doc = getDocument();
        List<StampHolder> list = doc.getStampHolders();
        for (Iterator<StampHolder> itr = list.iterator(); itr.hasNext();) {
            StampHolder sh = itr.next();
            if (!entity.equals(sh.getStamp().getModuleInfoBean().getEntity())) {
                itr.remove();
            }
        }
        return list;
    }

    public boolean hasSelection() {
        return hasSelection;
    }
}
