package open.dolphin.stampbox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import open.dolphin.client.*;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.helper.WindowSupport;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.IStampTreeModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.PublishedTreeModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.infomodel.StampTreeModel;
import open.dolphin.infomodel.TextStampModel;
import open.dolphin.order.EditorSetPanel;
import open.dolphin.order.OldNewValuePair;
import open.dolphin.project.Project;
import org.apache.log4j.Level;

/**
 * StampBox クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampBoxPlugin extends AbstractMainTool {
    
    private static final String NAME = "スタンプ箱";
    
    // frameのデフォルトの大きさ及びタイトル
    private static final int IMPORT_TREE_OFFSET     = 1;
    private static final int DEFAULT_EDITOR_WIDTH   = 800;
    private static final int DEFAULT_EDITOR_HEIGHT  = 400;

    private ComponentMemory cm;             // スタンプ箱のcomponent memory
    private ComponentMemory cmEditor;       // スタンプエディタのcomponent memory
    private JFrame editorFrame;             // スタンプエディタのJFrame
    private JFrame frame;                   // StampBox の JFrame
    private JTabbedPane parentBox;          // StampBox
    private AbstractStampBox userBox;       //ユーザ個人用の StampBox
    private AbstractStampBox curBox;        // 現在選択されている StampBox
    private List<Long> importedTreeList;    // インポートしている StampTree のリスト
    private JLabel curBoxInfo;              // 現在選択されている StampBox の情報を表示するラベル
    private JToggleButton toolBtn;          // Stampmaker ボタン
    private JButton publishBtn;             // 公開ボタン
    private JButton importBtn;              // インポートボタン
    private EditorSetPanel editors;         // StampMaker のエディタセット
    private EditorValueListener editorValueListener; // Editorの編集値リスナ
    private boolean editing;                // StampMaker モードのフラグ
    private BlockGlass glass;               // Block Glass Pane
    private JPanel stampBoxPanel;           // Container Panel
    private List<IStampTreeModel> stampTreeModels;  // このスタンプボックスの StmpTreeModel
    
    // Logger
    private final boolean DEBUG;

//masuda^   排他処理
    private boolean isEditable;
    
    public boolean isEditable() {
        return isEditable;
    }
//masuda$
    
//pns^  ツリー入れ替えロックボタン
    private JToggleButton lockBtn;
    private boolean isLocked = true;
    public boolean isLocked() {
        return isLocked;
    }
    private void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }
//pns$
    
    /**
     * Creates new StampBoxPlugin
     */
    public StampBoxPlugin() {
        setName(NAME);
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
    }
    
    /**
     * StampTreeModel を返す。
     * @return StampTreeModelのリスト
     */
    public List<IStampTreeModel> getStampTreeModels() {
        return stampTreeModels;
    }
    
    /**
     * StampTreeModel を設定する。
     * @param stampTreeModels StampTreeModelのリスト
     */
    public void setStampTreeModels(List<IStampTreeModel> stampTreeModels) {
        this.stampTreeModels = stampTreeModels;
    }
    
    /**
     * 現在のStampBoxを返す。
     * @return 現在選択されているStampBox
     */
    public AbstractStampBox getCurrentBox() {
        return curBox;
    }
    
    /**
     * 現在のStampBoxを設定する。
     * @param curBox 選択されたStampBox
     */
    public void setCurrentBox(AbstractStampBox curBox) {
        this.curBox = curBox;
    }
    
    /**
     * User(個人用)のStampBoxを返す。
     * @return User(個人用)のStampBox
     */
    public AbstractStampBox getUserStampBox() {
        return userBox;
    }
    
    /**
     * User(個人用)のStampBoxを設定する。
     * @param userBox User(個人用)のStampBox
     */
    public void setUserStampBox(AbstractStampBox userBox) {
        this.userBox = userBox;
    }
    
    /**
     * StampBox の JFrame を返す。
     * @return StampBox の JFrame
     */
    public JFrame getFrame() {
//masuda^
        if (editing) {
            return editorFrame;
        }
//masuda$
        return frame;
    }
    
    /**
     * インポートしているStampTreeのリストを返す。
     * @return インポートしているStampTreeのリスト
     */
    public List<Long> getImportedTreeList() {
        return importedTreeList;
    }
    
    /**
     * Block用GlassPaneを返す。
     * @return Block用GlassPane
     */
    public BlockGlass getBlockGlass() {
        return glass;
    }
    
    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {
        
        if (stampTreeModels == null) {
            ClientContext.getBootLogger().fatal("StampTreeModel is null");
            throw new RuntimeException("Fatal error: StampTreeModel is null at start.");
        }
        
        //
        // 全体のボックスを生成する
        //
        parentBox = new JTabbedPane();
        parentBox.setTabPlacement(JTabbedPane.BOTTOM);
        
        //
        // 読み込んだStampTreeをTabbedPaneに格納し、さらにそれをparentBoxに追加する
        //
        for (IStampTreeModel model : stampTreeModels) {
            
            if (model != null) {

                if (DEBUG) {
                    ClientContext.getBootLogger().debug("id = " + model.getId());
                    ClientContext.getBootLogger().debug("name = " + model.getName());
                    ClientContext.getBootLogger().debug("publishType = " + model.getPublishType());
                    ClientContext.getBootLogger().debug("category = " + model.getCategory());
                    ClientContext.getBootLogger().debug("partyName = " + model.getPartyName());
                    ClientContext.getBootLogger().debug("url = " + model.getUrl());
                    ClientContext.getBootLogger().debug("description = " + model.getDescription());
                    ClientContext.getBootLogger().debug("publishedDate = " + model.getPublishedDate());
                    ClientContext.getBootLogger().debug("lastUpdated = " + model.getLastUpdated());
                    ClientContext.getBootLogger().debug("userId = " + model.getUserModel());
                }
                
                // ユーザ個人用StampTreeの場合
                if (model.getUserModel().getId() == Project.getUserModel().getId() && model instanceof StampTreeModel) {
                    
                    //------------------------------------------
                    // 個人用のスタンプボックス(JTabbedPane)を生成する
                    //------------------------------------------
                    userBox = new UserStampBox();
                    userBox.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
                    userBox.setContext(this);
                    userBox.setStampTreeModel(model);
                    userBox.buildStampBox();
                    
                    //
                    // ParentBox に追加する
                    //
                    parentBox.addTab(ClientContext.getString("stampTree.personal.box.name"), userBox);
                    
                } else if (model instanceof PublishedTreeModel) {
                    //
                    // インポートしているTreeの場合
                    //
                    importPublishedTree(model);
                }
                model.setTreeXml(null);
            }
        }
        
        //
        // StampTreeModel を clear する
        //
        stampTreeModels.clear();
        
        // ParentBox のTab に tooltips を設定する
        for (int i = 0; i < parentBox.getTabCount(); i++) {
            AbstractStampBox box = (AbstractStampBox) parentBox.getComponentAt(i);
            parentBox.setToolTipTextAt(i, box.getInfo());
        }
        
        //
        // ParentBoxにChangeListenerを登録しスタンプメーカの制御を行う
        //
        parentBox.addChangeListener(new BoxChangeListener());
        setCurrentBox(userBox);
        
        //
        // ユーザBox用にChangeListenerを設定する
        //
        userBox.addChangeListener(new TabChangeListener());
        
        //
        // スタンプメーカを起動するためのボタンを生成する
        //
        toolBtn = new JToggleButton(ClientContext.getImageIconAlias("icon_stamp_maker"));
        toolBtn.setToolTipText("スタンプメーカを起動します");
        toolBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!editing) {
                    startStampMake();
                    editing = true;
//pns
                    if (isLocked) {
                        lockBtn.doClick();
                    }
                } else {
                    stopStampMake();
                    editing = false;
//pns
                    if (!isLocked) {
                        lockBtn.doClick();
                    }
                }
            }
        });
        
        //
        // スタンプ公開ボタンを生成する
        //
        publishBtn = new JButton(ClientContext.getImageIconAlias("icon_stamp_publish"));
        publishBtn.setToolTipText("スタンプの公開を管理をします");
//masuda^   reflectionはキライ
        //publishBtn.addActionListener(new ReflectActionListener(this, "publishStamp"));
        publishBtn.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                publishStamp();
            }
        });
//masuda$
        //
        // インポートボタンを生成する
        //
        importBtn = new JButton(ClientContext.getImageIconAlias("icon_stamp_import"));
        importBtn.setToolTipText("スタンプのインポートを管理をします");
//masuda^   reflectionはキライ
        //importBtn.addActionListener(new ReflectActionListener(this, "importStamp"));
        importBtn.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                importStamp();
            }
        });
//masuda$
//pns^  ロックボタンを生成する
        lockBtn = new JToggleButton();
        lockBtn.setIcon(ClientContext.getImageIconAlias("icon_stamp_lock_on"));
        lockBtn.setSelectedIcon(ClientContext.getImageIconAlias("icon_stamp_lock_off"));
        lockBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        lockBtn.setToolTipText("ツリー内での入れ替えのロック／解除");
        lockBtn.setPreferredSize(new java.awt.Dimension(40,40));
        
        lockBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 選択されていたらロック解除
                if (lockBtn.isSelected()) {
                    setLocked(false);
                } else {
                    setLocked(true);
                }
            }

        });
        // 特別メニューボタンを生成する
        JLabel extraBtn = new JLabel();
        extraBtn.setIcon(ClientContext.getImageIconAlias("icon_stamp_memu"));
        extraBtn.setToolTipText("+SHIFTで特別メニューを開きます");
        extraBtn.setFocusable(false);
//pns$        
//masuda^   スタンプ箱排他処理　プライマリユーザーでないなら編集不可
        String clientUUID = Dolphin.getInstance().getClientUUID();
        String userUUID = Project.getUserModel().getClientUUID();
        isEditable = clientUUID.equals(userUUID);
        if (isEditable) {
            extraBtn.addMouseListener(new StampBoxPluginExtraMenu(this));
        } else {
            lockBtn.setEnabled(false);
            toolBtn.setEnabled(false);
            publishBtn.setEnabled(false);
            importBtn.setEnabled(false);
        }
//masuda$

        //
        // curBoxInfoラベルを生成する
        //
        curBoxInfo = new JLabel("");
        curBoxInfo.setFont(GUIFactory.createSmallFont());
        
        //
        // レイアウトする
        //
        stampBoxPanel = new JPanel(new BorderLayout());
        stampBoxPanel.add(parentBox, BorderLayout.CENTER);
        JPanel cmdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cmdPanel.add(toolBtn);
        cmdPanel.add(publishBtn);
        cmdPanel.add(importBtn);
        cmdPanel.add(curBoxInfo);
//pns^
        JPanel cmdPanel2 = new JPanel(new BorderLayout());
        JPanel utilPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        utilPanel.add(extraBtn);
        utilPanel.add(lockBtn);
        cmdPanel2.setPreferredSize(new java.awt.Dimension(1,40));
        cmdPanel2.add(cmdPanel, BorderLayout.WEST);
        cmdPanel2.add(utilPanel, BorderLayout.EAST);
        stampBoxPanel.add(cmdPanel2, BorderLayout.NORTH);
//pns$

        //
        // 前回終了時のタブを選択する
        //
        String name = this.getClass().getName();
        int index = Project.getInt(name + "_parentBox", 0);
        index = ( index >= 0 && index <= (parentBox.getTabCount() -1) ) ? index : 0;
        parentBox.setSelectedIndex(index);
        index = Project.getInt(name + "_stampBox", 0);
        index = ( index >= 0 && index <= (userBox.getTabCount() -1) ) ? index : 0;
        
        //
        // ORCA タブが選択されていて ORCA に接続がない場合を避ける
        //
        index = index == IInfoModel.TAB_INDEX_ORCA ? 0 : index;
        userBox.setSelectedIndex(index);
        
        //
        // ボタンをコントロールする
        //
        boxChanged();
        
        // StampBoxPluginのJFrameを生成する
        String title = ClientContext.getFrameTitle(getName());
//masuda^
        WindowSupport ws = WindowSupport.create(title);
        frame = ws.getFrame();
        ws.getMenuBar().setVisible(false);
//masuda$
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (editing) {
                    toolBtn.doClick();
//pns
                    if (!isLocked) {
                        lockBtn.doClick();
                    }

                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // コンテントパネルを生成する
        JPanel content = createContentPanel();
        // Frame に加える
        frame.setContentPane(content);
        glass = new BlockGlass();
        frame.setGlassPane(glass);

        // スタンプ箱の位置をきめる。最初はメインウィンドウの隣に
        Dolphin dolphin = (Dolphin) getContext();
        Rectangle r = dolphin.getFrame().getBounds();
        int locX = r.x + r.width;
        int locY = r.y;
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        // はみ出そうなら中心付近に表示
        if (locX + 50 > d.width || locY + 100 > d.height) {
            locX = (d.width - r.width) / 2;
            locY = (d.height - r.height) / 2;
        }

        cm = new ComponentMemory(frame, new Point(locX, locY), new Dimension(r.width, r.height), StampBoxPlugin.this);
        cm.setToPreferenceBounds();
    }
    
    /**
     * ContentPanelを作成する    masuda
     */
    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(stampBoxPanel, BorderLayout.CENTER);
        panel.setOpaque(true);
        return panel;
    }
    
    /**
     * 選択されているIndexでボタンを制御する。
     */
    private void boxChanged() {
        
        int index = parentBox.getSelectedIndex();
        setCurrentBox((AbstractStampBox) parentBox.getComponentAt(index));
        String info = getCurrentBox().getInfo();
        curBoxInfo.setText(info);
        
        if (getCurrentBox() == userBox) {
            publishBtn.setEnabled(isEditable);
            int index2 = userBox.getSelectedIndex();
            boolean enabled = userBox.isHasEditor(index2);
            toolBtn.setEnabled(enabled && isEditable);
            
        } else {
            toolBtn.setEnabled(false);
            publishBtn.setEnabled(false);
        }
    }
    
    /**
     * ImportしたStampBoxの選択可能を制御する。
     * @param enabled 選択可能な時 true
     */
    private void enabledImportBox(boolean enabled) {
        int cnt = parentBox.getTabCount();
        for (int i = 0 ; i < cnt; i++) {
            if ((JTabbedPane) parentBox.getComponentAt(i) != userBox) {
                parentBox.setEnabledAt(i, enabled);
            }
        }
    }
    
    /**
     * TabChangeListener
     * User用StampBoxのTab切り替えリスナクラス。
     */
    class TabChangeListener implements ChangeListener {
        
        @Override
        public void stateChanged(ChangeEvent e) {
            
            if (!editing) {
                // スタンプメーカ起動中でない時
                // テキストスタンプタブが選択されたらスタンプメーカボタンを disabledにする
                // ORCA セットタブの場合を処理する
                int index = userBox.getSelectedIndex();
                StampTree tree = userBox.getStampTree(index);
                tree.enter();
                boolean enabled = userBox.isHasEditor(index);
                toolBtn.setEnabled(enabled && isEditable);
                
            } else {
                // スタンプメーカ起動中の時
                // 選択されたタブに対応するエディタを表示する
                int index = userBox.getSelectedIndex();
                StampTree tree = userBox.getStampTree(index);
//pns^
                //if (editors != null && (!tree.getEntity().equals(IInfoModel.ENTITY_TEXT)) ) {
                if (editors != null) {
                    editors.show(tree.getEntity());

                    javax.swing.tree.TreePath tp = tree.getSelectionPath();
                    tree.clearSelection();
                    tree.setSelectionPath(tp);
//pns$
                }
            }
        }
    }
    
    /**
     * ParentBox の TabChangeListenerクラス。
     */
    class BoxChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            boxChanged();
        }
    }

    /**
     * スタンプメーカを起動する。
     */
    public void startStampMake() {

        if (editing) {
            return;
        }

        Runnable awt = new Runnable() {

            @Override
            public void run() {
                createAndShowEditorSet();
            }
        };

        SwingUtilities.invokeLater(awt);
    }
    
    /**
     * スタンプメーカを起動する。
     */
    private void createAndShowEditorSet() {

        // インポートボックスを選択不可にする
        enabledImportBox(false);
        
        // 現在のタブからtreeのEntityを得る
        int index = userBox.getSelectedIndex();
        StampTree tree = userBox.getStampTree(index);
        String entity = tree.getEntity();
        
        // エディタセットを生成する
        editors = new EditorSetPanel();

        // text タブを選択不可にする
        userBox.setHasNoEditorEnabled(false);

        //----------------------------------------------------------
        // 全 Tree に edirorSet を treeSelectionListener として登録する
        //----------------------------------------------------------
        List<StampTree> allTrees = userBox.getAllTrees();
        for (StampTree st : allTrees) {
            st.addTreeSelectionListener(editors);
        }

        // Editorへ編集値を受けとるためのリスナを登録する
        editorValueListener = new EditorValueListener();
        editors.addPropertyChangeListener(EditorSetPanel.EDITOR_VALUE_PROP, editorValueListener);

        // EditorSet へ現在のentity(Tree)に対応するエディタを表示させる
        editors.show(entity);
//pns^
        javax.swing.tree.TreePath tp = tree.getSelectionPath();
        tree.clearSelection();
        tree.setSelectionPath(tp);
//pns$
        // StampBox の Frame を再構築する
        frame.setVisible(false);
        JPanel content = createContentPanel();
        content.add(editors, BorderLayout.CENTER);
        content.add(stampBoxPanel, BorderLayout.EAST);

        if (editorFrame == null) {
            String title = ClientContext.getFrameTitle(getName());
//masuda^
            WindowSupport ws = WindowSupport.create(title);
            editorFrame = ws.getFrame();
            ws.getMenuBar().setVisible(false);
//masuda$
        }
        editorFrame.setContentPane(content);
        editorFrame.setGlassPane(glass);
        editors.setPreferredSize(new Dimension(DEFAULT_EDITOR_WIDTH, DEFAULT_EDITOR_HEIGHT));

        editing = true;
        toolBtn.setToolTipText("スタンプメーカを終了します");
        publishBtn.setEnabled(false);
        importBtn.setEnabled(false);
        editorFrame.pack();

        // 前回終了時の位置とサイズを取得する
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = editorFrame.getPreferredSize().width;
        int height = editorFrame.getPreferredSize().height;
        int locX = (screenSize.width - width) / 2;
        int locY = (screenSize.height - height) / 2;
        if (cmEditor == null) {
            cmEditor = new ComponentMemory(editorFrame, new Point(locX, locY), new Dimension(width, height), editorFrame);
        }

        cmEditor.setToPreferenceBounds();
        editorFrame.setVisible(true);
    }


    public void stopStampMake() {

        if (!editing) {
            return;
        }

        Runnable awt = new Runnable() {

            @Override
            public void run() {
                disposeEditorSet();
            }
        };

        SwingUtilities.invokeLater(awt);
    }

    
    /**
     * スタンプメーカを終了する。
     */
    private void disposeEditorSet() {
        
        editorFrame.setVisible(false);
        
        editors.close();
        editors.removePropertyChangeListener(EditorSetPanel.EDITOR_VALUE_PROP, editorValueListener);
        List<StampTree> allTrees = userBox.getAllTrees();
        for (StampTree st : allTrees) {
            st.removeTreeSelectionListener(editors);
        }

        editors = null;
        editorValueListener = null;
        userBox.setHasNoEditorEnabled(true);
        
        editing = false;
        toolBtn.setToolTipText("スタンプメーカを起動します");
        publishBtn.setEnabled(true);
        importBtn.setEnabled(true);
        
        //
        // ASP ボックスを選択可にする
        //
        enabledImportBox(true);
        
        // frameを再作成する
        JPanel content = createContentPanel();
        content.add(stampBoxPanel, BorderLayout.CENTER);
        frame.setContentPane(content);
        frame.setGlassPane(glass);
        cm.setToPreferenceBounds();
        frame.setVisible(true);
    }
    
    /**
     * EditorValueListener
     * エディタ(EditorSetPanel)で作成したスタンプをStampTreeに加える。
     */
    private class EditorValueListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            
            String prop = e.getPropertyName();
            if (!EditorSetPanel.EDITOR_VALUE_PROP.equals(prop)) {
                return;
            }
            Object obj = e.getNewValue();
            if (!(obj instanceof OldNewValuePair)) {
                return;
            }
            OldNewValuePair valuePair = (OldNewValuePair) obj;

            Object oldValue = valuePair.getOldValue();
            Object newValue = valuePair.getNewValue();

            if (newValue == null) {
                return;
            }
            
            if (newValue instanceof ModuleModel[]) {
                //--------------------
                // 編集したスタンプ
                //--------------------
                ModuleModel[] newStamps = (ModuleModel[]) newValue;
                ModuleModel[] oldStamps = (ModuleModel[]) oldValue;
                
                // いったんstampIdをクリアする
                for (ModuleModel mm : newStamps) {
                    mm.getModuleInfoBean().setStampId(null);
                }
                // 編集元があれば一つ目はstampIdを継承する
                if (oldStamps != null && oldStamps.length > 0) {
                    ModuleInfoBean newInfo = newStamps[0].getModuleInfoBean();
                    ModuleInfoBean oldInfo = oldStamps[0].getModuleInfoBean();
                    if (newInfo != null && oldInfo != null) {
                        newInfo.setStampId(oldInfo.getStampId());
                    }
                }

                String entity = newStamps[0].getModuleInfoBean().getEntity();
                StampTree tree = getStampTree(entity);
                tree.addOrReplaceStamp(newStamps);

                
            } else if (newValue instanceof RegisteredDiagnosisModel) {
                //-------------------
                // 傷病名
                //-------------------
                RegisteredDiagnosisModel newRd = (RegisteredDiagnosisModel) newValue;
                RegisteredDiagnosisModel oldRd = (RegisteredDiagnosisModel) oldValue;
                
                // 編集元があればはstampIdを継承する
                if (oldRd != null) {
                    newRd.setStampId(oldRd.getStampId());
                } else {
                    newRd.setStampId(null);     //  新規
                }
                
                StampTree tree = getStampTree(IInfoModel.ENTITY_DIAGNOSIS);
                tree.addOrReplaceDiagnosis(newRd);
                
            } else if (newValue instanceof TextStampModel) {
                // TextStamp
                TextStampModel newText = (TextStampModel) newValue;
                TextStampModel oldText = (TextStampModel) oldValue;
                // 編集元があればはstampIdを継承する
                if (oldText != null) {
                    newText.setStampId(oldText.getStampId());
                } else {
                    newText.setStampId(null);   //  新規
                }
                StampTree tree = getStampTree(IInfoModel.ENTITY_TEXT);
                tree.addOrReplaceTextStamp(newText);
            }
        }
    }
    
    /**
     * スタンプパブリッシャーを起動する。
     */
    private void publishStamp() {
        StampPublisher publisher = new StampPublisher(this);
        publisher.start();
    }
    
    /**
     * スタンプインポーターを起動する。
     */
    private void importStamp() {
        StampImporter importer = new StampImporter(this);
        importer.start();
    }
    
    /**
     * 公開されているスタンプTreeをインポートする。
     * @param importTree インポートする公開Tree
     */
    public void importPublishedTree(IStampTreeModel importTree) {
        
        //
        // Asp StampBox を生成し parentBox に加える
        //
        AbstractStampBox aspBox = new AspStampBox();
        aspBox.setContext(this);
        aspBox.setStampTreeModel(importTree);
        aspBox.buildStampBox();
        parentBox.addTab(importTree.getName(), aspBox);
        
        //
        // インポートリストに追加する
        //
        if (importedTreeList == null) {
            importedTreeList = new ArrayList<>();
        }
        importedTreeList.add(new Long(importTree.getId()));
    }
    
    /**
     * インポートしている公開Treeを削除する。
     * @param removeId 削除する公開TreeのId
     */
    public void removeImportedTree(long removeId) {
        
        if (importedTreeList != null) {
            for (int i = 0; i < importedTreeList.size(); i++) {
                Long id = importedTreeList.get(i);
                if (id.longValue() == removeId) {
                    parentBox.removeTabAt(i+IMPORT_TREE_OFFSET);
                    importedTreeList.remove(i);
                    break;
                }
            }
        }
    }
    
    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {
        frame.setVisible(false);
        frame.dispose();
//masuda^
        if (editorFrame != null) {
            editorFrame.setVisible(false);
            editorFrame.dispose();
        }
//masuda$
    }
    
    /**
     * フレームを前面に出す。
     */
    @Override
    public void enter() {
        if (frame != null) {
            frame.toFront();
        }
    }

    /**
     * アプリケーションの終了時にスタンプツリーを返し保存する。
     * @return StamPtreeMode; 
     */
    public IStampTreeModel getUsersTreeTosave() {

        preSave();

        //
        // User Tree のみを保存する
        //
        List<StampTree> list = userBox.getAllTrees();
        if (list == null || list.isEmpty()) {
            // never
            return null;
        }

        //
        // ORCA セットは除く
        //
        for (StampTree tree : list) {
            if (tree.getTreeInfo().getEntity().equals(IInfoModel.ENTITY_ORCA)) {
                list.remove(tree);
                if (DEBUG) {
                    ClientContext.getBootLogger().debug("ORCAセットを除きました");
                }
                break;
            }
        }

        // StampTree を表す XML データを生成する
//masuda^
        StampTreeXmlBuilder builder = new StampTreeXmlBuilder(StampTreeXmlBuilder.MODE.DEFAULT);
        String treeXml = builder.build(list);
//masuda$
        // 個人用のStampTreeModelにXMLをセットする
        IStampTreeModel treeTosave = userBox.getStampTreeModel();
        treeTosave.setTreeXml(treeXml);
        
        return treeTosave;
    }

    /**
     * 位置大きさを保存する。
     * @throws Exception
     */
    private void preSave() {

        String name = (StampBoxPlugin.this).getClass().getName();

        // 終了時のタブ選択インデックスを保存する
        Project.setInt(name + "_parentBox", parentBox.getSelectedIndex());
        Project.setInt(name + "_stampBox", userBox.getSelectedIndex());
    }

    
    /**
     * 引数のカテゴリに対応するTreeを返す。
     * @param category Treeのカテゴリ
     * @return カテゴリにマッチするStampTree
     */
    public StampTree getStampTree(String entity) {
        return getCurrentBox().getStampTree(entity);
    }
    
    public StampTree getStampTreeFromUserBox(String entity) {
        return getUserStampBox().getStampTree(entity);
    }
    
    /**
     * スタンプボックスに含まれる全treeのTreeInfoリストを返す。
     * @return TreeInfoのリスト
     */
    public List<TreeInfo> getAllTress() {
        return getCurrentBox().getAllTreeInfos();
    }
    
    /**
     * スタンプボックスに含まれる全treeを返す。
     * @return StampTreeのリスト
     */
    public List<StampTree> getAllTrees() {
        return getCurrentBox().getAllTrees();
    }
    
    /**
     * スタンプボックスに含まれる全treeを返す。
     * @return StampTreeのリスト
     */
    public List<StampTree> getAllAllPTrees() {
        
        int cnt = parentBox.getTabCount();
        ArrayList<StampTree> ret = new ArrayList<>();
        
        for (int i = 0; i < cnt; i++) {
            AbstractStampBox stb = (AbstractStampBox) parentBox.getComponentAt(i);
            ret.addAll(stb.getAllPTrees());
        }
        
        return ret;
    }
    
    /**
     * Currentボックスの P 関連Staptreeを返す。
     * @return StampTreeのリスト
     */
    public List<StampTree> getAllPTrees() {
        
        AbstractStampBox stb = getCurrentBox();
        return stb.getAllPTrees();
    }
    
    /**
     * 引数のエンティティ配下にある全てのスタンプを返す。
     * これはメニュー等で使用する。
     * @param entity Treeのエンティティ
     * @return 全てのスタンプのリスト
     */
    public List<ModuleInfoBean> getAllStamps(String entity) {
        return getCurrentBox().getAllStamps(entity);
    }
}