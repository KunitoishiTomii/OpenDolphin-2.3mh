package open.dolphin.impl.rezept;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.BlockGlass;
import open.dolphin.client.ChartEventListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Dolphin;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.helper.WindowSupport;
import open.dolphin.impl.rezept.filter.*;
import open.dolphin.impl.rezept.model.*;
import open.dolphin.infomodel.DrPatientIdModel;
import open.dolphin.infomodel.IndicationModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.project.Project;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * レセ電ビューアーもどき
 * 
 * @author masuda, Masuda Naika
 */
public class RezeptViewer {
    
    private static final String EMPTY = "";
    
    private static final String RE_TBL_SPEC_NAME = "reze.retable.column.spec";
    private static final String[] RE_TBL_COLUMN_NAMES = {"", "ID", "氏名", "性別", "年齢"};
    private static final String[] RE_TBL_PROPERTY_NAMES = {"getCheckFlag", "getPatientId", "getName", "getSex", "getAge"};
    private static final Class[] RE_TBL_CLASSES = {Integer.class, String.class, String.class, String.class, Integer.class};
    private static final int[] RE_TBL_COLUMN_WIDTH = {5, 20, 40, 10, 5, 10};
    
    private static final String DIAG_TBL_SPEC_NAME = "reze.diagtable.column.spec";
    private static final String[] DIAG_TBL_COLUMN_NAMES = {"傷病名", "開始日", "転帰", "特定疾患"};
    private static final String[] DIAG_TBL_PROPERTY_NAMES = {"getDiagName", "getStartDateStr", "getOutcomeStr", "getByoKanrenKbnStr"};
    private static final Class[] DIAG_TBL_CLASSES = {String.class, String.class, String.class, String.class};
    private static final int[] DIAG_TBL_COLUMN_WIDTH = {100, 20, 10, 20};
    
    private static final String ITEM_TBL_SPEC_NAME = "reze.itemtable.column.spec";
    private static final String[] ITEM_TBL_COLUMN_NAMES = {"区分", "項目", "数量", "点数", "回数"};
    private static final String[] ITEM_TBL_PROPERTY_NAMES = {"getClassCode", "getDescription", "getNumber", "getTen", "getCount"};
    private static final Class[] ITEM_TBL_CLASSES = {String.class, String.class, Float.class, Float.class, Integer.class};
    private static final int[] ITEM_TBL_COLUMN_WIDTH = {10, 100, 10, 10, 10};
    
    private static final String INFO_TBL_SPEC_NAME = "reze.infotable.column.spec";
    private static final String[] INFO_TBL_COLUMN_NAMES = {"区分", "項目", "内容"};
    private static final String[] INFO_TBL_PROPERTY_NAMES = {"getResult", "getFilterName", "getMsg"};
    private static final Class[] INFO_TBL_CLASSES = {Integer.class, String.class, String.class};
    private static final int[] INFO_TBL_COLUMN_WIDTH = {10, 100, 200};
    
    private static final ImageIcon INFO_ICON = ClientContext.getImageIconAlias("icon_info_small");
    private static final ImageIcon WARN_ICON = ClientContext.getImageIconAlias("icon_warn_small");
    private static final ImageIcon ERROR_ICON = ClientContext.getImageIconAlias("icon_error_small");
    
    private final Class[] FILTER_CLASSES= {BasicFilter.class, DiagnosisFilter.class};
    
    private final RezeptView view;
    private final BlockGlass blockGlass;
    private final Map<String, IndicationModel> indicationMap;
    
    private ColumnSpecHelper reTblHelper;
    private ColumnSpecHelper diagTblHelper;
    private ColumnSpecHelper itemTblHelper;
    private ColumnSpecHelper infoTblHelper;
    
    private ListTableModel<SY_Model> diagTableModel;
    private ListTableModel<IRezeItem> itemTableModel;
    private ListTableModel<CheckResult> infoTableModel;
    private List<IR_Model> irList;
    private List<DrPatientIdModel> drPatientIdList;
    
    private int reModelCount;
    private String facilityName;

    private Set<String> itemSrycdSet;
    

    public RezeptViewer() {
        indicationMap = new HashMap<>();
        view = new RezeptView();
        blockGlass = new BlockGlass();
    }
    
    public void enter() {

        initComponents();
        facilityName = Project.getUserModel().getFacilityModel().getFacilityName();
        
        // do not remove copyright!
        String title = ClientContext.getFrameTitle("レセ点") + ", Masuda Naika";
        WindowSupport ws = WindowSupport.create(title);
        ws.getMenuBar().setVisible(false);
        
        JFrame frame = ws.getFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                stop();

            }
        });
        
        frame.setContentPane(view);
        frame.pack();
        
        frame.setGlassPane(blockGlass);
        blockGlass.setSize(frame.getSize());

        ComponentMemory cm = new ComponentMemory(frame, new Point(100, 100), frame.getPreferredSize(), view);
        cm.setToPreferenceBounds();
        frame.setVisible(true);
    }
    
    private void stop() {
        
        // カラム幅を保存する
        diagTblHelper.saveProperty();
        itemTblHelper.saveProperty();
        infoTblHelper.saveProperty();
        JTable reTable = getSelectedReTable();
        if (reTable != null) {
            reTblHelper.saveProperty(reTable);
        }
        
        if (itemSrycdSet != null) {
            itemSrycdSet.clear();
        }
        indicationMap.clear();
        
        // JSplitPane幅を記録する
        view.saveDimension();
    }
    
    private JTable getSelectedReTable() {
        
        JTabbedPane tabbedPane = (JTabbedPane) view.getTabbedPane().getSelectedComponent();
        
        if (tabbedPane != null) {
            RE_Panel rePanel = (RE_Panel) tabbedPane.getSelectedComponent();
            return rePanel.getReTable();
        }
        
        return null;
    }
    
    private List<ListTableModel<RE_Model>> getAllReListTableModel() {
        
        List<ListTableModel<RE_Model>> list = new ArrayList<>();
        for (Component comp : view.getTabbedPane().getComponents()) {
            JTabbedPane tabbedPane = (JTabbedPane) comp;
            for (Component comp2 : tabbedPane.getComponents()) {
                RE_Panel rePanel = (RE_Panel) comp2;
                ListTableSorter<RE_Model> sorter = (ListTableSorter<RE_Model>) rePanel.getReTable().getModel();
                list.add(sorter.getListTableModel());
            }
        }

        return list;
    }
    
    private void initComponents() {
        
        // ColumnSpecHelperを準備する
        reTblHelper = new ColumnSpecHelper(RE_TBL_SPEC_NAME, RE_TBL_COLUMN_NAMES, 
                RE_TBL_PROPERTY_NAMES, RE_TBL_CLASSES, RE_TBL_COLUMN_WIDTH);
        reTblHelper.loadProperty();
        
        diagTblHelper = new ColumnSpecHelper(DIAG_TBL_SPEC_NAME, DIAG_TBL_COLUMN_NAMES, 
                DIAG_TBL_PROPERTY_NAMES, DIAG_TBL_CLASSES, DIAG_TBL_COLUMN_WIDTH);
        diagTblHelper.loadProperty();
        
        itemTblHelper = new ColumnSpecHelper(ITEM_TBL_SPEC_NAME, ITEM_TBL_COLUMN_NAMES, 
                ITEM_TBL_PROPERTY_NAMES, ITEM_TBL_CLASSES, ITEM_TBL_COLUMN_WIDTH);
        itemTblHelper.loadProperty();
        
        infoTblHelper = new ColumnSpecHelper(INFO_TBL_SPEC_NAME, INFO_TBL_COLUMN_NAMES, 
                INFO_TBL_PROPERTY_NAMES, INFO_TBL_CLASSES, INFO_TBL_COLUMN_WIDTH);
        infoTblHelper.loadProperty();
        
        // 傷病名テーブル
        JTable diagTable = view.getDiagTable();
        SY_TableRenderer syRen = new SY_TableRenderer();
        syRen.setTable(diagTable);
        syRen.setDefaultRenderer();
        diagTblHelper.setTable(diagTable);
        // カラム設定、モデル設定
        String[] diagColumnNames = diagTblHelper.getTableModelColumnNames();
        String[] diagMethods = diagTblHelper.getTableModelColumnMethods();
        Class[] diagCls = diagTblHelper.getTableModelColumnClasses();
        diagTableModel = new ListTableModel<>(diagColumnNames, 1, diagMethods, diagCls);
        diagTable.setModel(diagTableModel);
        diagTblHelper.updateColumnWidth();
        
        // 診療行為テーブル
        final JTable itemTable = view.getItemTable();
        ITEM_TableRenderer itemRen = new ITEM_TableRenderer();
        itemRen.setTable(itemTable);
        itemRen.setDefaultRenderer();
        // カラム設定、モデル設定
        itemTblHelper.setTable(itemTable);
        String[] itemColumnNames = itemTblHelper.getTableModelColumnNames();
        String[] itemMethods = itemTblHelper.getTableModelColumnMethods();
        Class[] itemCls = itemTblHelper.getTableModelColumnClasses();
        itemTableModel = new ListTableModel<>(itemColumnNames, 1, itemMethods, itemCls);
        itemTable.setModel(itemTableModel);
        itemTblHelper.updateColumnWidth();
        
        // インフォテーブル
        final JTable infoTable = view.getInfoTable();
        INFO_TableRenderer infoRen = new INFO_TableRenderer();
        infoRen.setTable(infoTable);
        infoRen.setDefaultRenderer();
        // カラム設定、モデル設定
        infoTblHelper.setTable(infoTable);
        String[] infoColumnNames = infoTblHelper.getTableModelColumnNames();
        String[] infoMethods = infoTblHelper.getTableModelColumnMethods();
        Class[] infoCls = infoTblHelper.getTableModelColumnClasses();
        infoTableModel = new ListTableModel<>(infoColumnNames, 1, infoMethods, infoCls);
        infoTable.setModel(infoTableModel);
        infoTblHelper.updateColumnWidth();
        
        // connect
        view.getImportBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int modifiers = e.getModifiers();
                boolean shift = (modifiers & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK;
                showImportPopup(shift);
            }
        });
        
        view.getPrevBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                JTable reTable = getSelectedReTable();
                if (reTable != null) {
                    int row = reTable.getSelectedRow();
                    row = Math.max(0, row - 1);
                    reTable.getSelectionModel().setSelectionInterval(row, row);
                    // 選択行が表示されるようにスクロールする
                    Rectangle r = reTable.getCellRect(row, 0, true);
                    reTable.scrollRectToVisible(r);
                }
            }
        });
        
        view.getNextBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                JTable reTable = getSelectedReTable();
                if (reTable != null) {
                    int row = reTable.getSelectedRow();
                    row = Math.min(reTable.getRowCount() - 1, row + 1);
                    reTable.getSelectionModel().setSelectionInterval(row, row);
                    // 選択行が表示されるようにスクロールする
                    Rectangle r = reTable.getCellRect(row, 0, true);
                    reTable.scrollRectToVisible(r);
                }
            }
        });
        
        view.getCheckBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                checkAllReze();
            }
        });
        
        view.getRefreshBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                checkSingleReze();
            }
        });
        
        itemTable.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = itemTable.getSelectedRow();
                    IRezeItem item = itemTableModel.getObject(row);
                    if (item != null) {
                        openIndicationEditor(item);
                    }
                }
            }
        });
        
        JComboBox combo = view.getDrCombo();
        combo.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent e) {
                Object o = e.getItem();
                if (o instanceof DrPatientIdModel) {
                    showRezeData((DrPatientIdModel) o);
                } else {
                    showRezeData(null);
                }
            }
        });
        
        view.getPrintBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                print();
            }
        });
        view.getPrintBtn().setEnabled(false);
    }
    
    private void print() {
        
        final String[] commands = {"エラー", "警告", "INFO", "全部"};
        JPopupMenu pMenu = new JPopupMenu();
        JMenuItem item0 = new JMenuItem(commands[0]);
        JMenuItem item1 = new JMenuItem(commands[1]);
        JMenuItem item2 = new JMenuItem(commands[2]);
        JMenuItem item3 = new JMenuItem(commands[3]);
        pMenu.add(item0);
        pMenu.add(item1);
        pMenu.add(item2);
        pMenu.add(item3);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String cmd = e.getActionCommand();
                if (commands[0].equals(cmd)) {
                    makePdf(CheckResult.CHECK_ERROR);
                } else if (commands[1].equals(cmd)) {
                    makePdf(CheckResult.CHECK_WARNING);
                } else if (commands[2].equals(cmd)) {
                    makePdf(CheckResult.CHECK_INFO);
                } else if (commands[3].equals(cmd)) {
                    makePdf(CheckResult.CHECK_NO_ERROR);
                }
            }
        };
        
        item0.addActionListener(listener);
        item1.addActionListener(listener);
        item2.addActionListener(listener);
        item3.addActionListener(listener);

        pMenu.show(view.getPrintBtn(), 0, 0);
    }
    
    
    private void makePdf(final int level) {
        
        final List<ListTableModel<RE_Model>> list = getAllReListTableModel();
        if (list.isEmpty()) {
            return;
        }
        
        List<RE_Model> reModelList = new ArrayList<>();
        for (ListTableModel<RE_Model> tableModel : list) {
            for (RE_Model reModel : tableModel.getDataProvider()) {
                if (reModel.getCheckFlag() >= level) {
                    reModelList.add(reModel);
                }
            }
        }
        
        RezeCheckPdfMaker pdfMaker = new RezeCheckPdfMaker();
        pdfMaker.setParent(view);
        pdfMaker.setReModelList(reModelList);
        JComboBox combo = view.getDrCombo();
        String drName = combo.getSelectedItem().toString();
        pdfMaker.setDrName(drName);
        pdfMaker.create();
    }
    
    public Map<String, IndicationModel> getIndicationMap() {
        return indicationMap;
    }
    
    public RezeptView getRezeptView() {
        return view;
    }
    
    public BlockGlass getBlockGlass() {
        return blockGlass;
    }
    
    private void checkSingleReze() {
        JTable reTable = getSelectedReTable();
        if (reTable == null) {
            return;
        }
        int row = reTable.getSelectedRow();
        if (row != -1) {
            ListTableSorter<RE_Model> sorter = (ListTableSorter<RE_Model>) reTable.getModel();
            RE_Model reModel = sorter.getObject(row);
            doCheck(reModel);
            // 再表示する
            reTable.getSelectionModel().clearSelection();
            reTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }
    
    private void checkAllReze() {
        
        final List<ListTableModel<RE_Model>> list = getAllReListTableModel();
        if (list.isEmpty()) {
            return;
        }
        
        final JTable reTable = getSelectedReTable();

        blockGlass.setText("処理中です...");
        blockGlass.block();
        
        final String noteFrmt = "%d / %d 件";
        String msg = "レセプトチェック中...";
        String note = String.format(noteFrmt, 0, reModelCount);
        final ProgressMonitor monitor = new ProgressMonitor(view, msg, note, 0, reModelCount);
        monitor.setMillisToDecideToPopup(0);
        monitor.setProgress(0);
        
        SwingWorker worker = new SwingWorker<Void, Integer>() {

            @Override
            protected Void doInBackground() throws Exception {
                int cnt = 0;
                createIndicationMap();
                for (ListTableModel<RE_Model> tableModel : list) {
                    for (RE_Model reModel : tableModel.getDataProvider()) {
                        doCheck(reModel);
                        publish(cnt++);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                Integer cnt = chunks.get(chunks.size() - 1);
                monitor.setNote(String.format(noteFrmt, cnt, reModelCount));
                monitor.setProgress(cnt);
            }

            @Override
            protected void done() {
                monitor.close();
                if (reTable != null) {
                    int row = reTable.getSelectedRow();
                    reTable.getSelectionModel().clearSelection();
                    if (row == -1) {
                        reTable.getSelectionModel().setSelectionInterval(0, 0);
                    } else {
                        reTable.getSelectionModel().setSelectionInterval(row, row);
                    }
                }
                view.getPrintBtn().setEnabled(true);
                blockGlass.setText("");
                blockGlass.unblock();
            }

        };

        worker.execute();
    }
    
    private void doCheck(RE_Model reModel) {

        // RE_Modelの算定フラグ類を初期化
        reModel.initCheckResult();

        // check filterでチェックする
        for (Class clazz : FILTER_CLASSES) {
            try {
                AbstractCheckFilter filter = (AbstractCheckFilter) clazz.newInstance();
                filter.setRezeptViewer(this);
                List<CheckResult> results = filter.doCheck(reModel);
                reModel.addCheckResults(results);
            } catch (Exception ex) {
            }
        }
    }
    
    private void createIndicationMap() throws Exception {
        
        // まずデータベースからIndicationModelを取得する
        List<String> srycds = new ArrayList<>();
        for (String srycd : itemSrycdSet) {
            if (!indicationMap.containsKey(srycd)) {
                srycds.add(srycd);
            }
        }
        if (srycds.isEmpty()) {
            return;
        }
        
        MasudaDelegater del = MasudaDelegater.getInstance();
        List<IndicationModel> indications = del.getIndicationList(srycds);

        for (IndicationModel model : indications) {
            String srycd = model.getSrycd();
            indicationMap.put(srycd, model);
            srycds.remove(srycd);
        }
        
        if (srycds.isEmpty()) {
            return;
        }
        
        // データベースに未登録のものはORCAを参照する
        SqlMiscDao dao = SqlMiscDao.getInstance();
        List<IndicationModel> toAddList = new ArrayList<>();
        for (String srycd : srycds) {
            IndicationModel model = dao.getTekiouByomei(srycd);
            toAddList.add(model);
        }
        
        // ORCAから取得したものをデータベースに登録する
        del.addIndicationModels(toAddList);
        
        // 登録したものをデータベースから取得してMapに登録する
        indications = del.getIndicationList(srycds);
        for (IndicationModel model : indications) {
            String srycd = model.getSrycd();
            indicationMap.put(srycd, model);
        }
    }
    
    private void openIndicationEditor(IRezeItem item) {
        IndicationEditor editor = new IndicationEditor(this);
        List<SY_Model> diagList = new ArrayList<>(diagTableModel.getDataProvider());
        editor.start(item, diagList);
    }
    
    // ボタンクリックでポップアップ表示、今月と先月と先々月を選択可能とする
    private void showImportPopup(boolean shiftPressed) {
        
        JPopupMenu pMenu = new JPopupMenu();
        
        if (!shiftPressed) {
            GregorianCalendar gc = new GregorianCalendar();
            SimpleDateFormat frmt = new SimpleDateFormat("yyyyMM");
            String ymd0 = frmt.format(gc.getTime());
            gc.add(GregorianCalendar.MONTH, -1);
            String ymd1 = frmt.format(gc.getTime());
            gc.add(GregorianCalendar.MONTH, -1);
            String ymd2 = frmt.format(gc.getTime());

            JMenuItem item0 = new JMenuItem(ymd0);
            JMenuItem item1 = new JMenuItem(ymd1);
            JMenuItem item2 = new JMenuItem(ymd2);
            pMenu.add(item0);
            pMenu.add(item1);
            pMenu.add(item2);

            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String ym = e.getActionCommand();
                    loadFromOrca(ym);
                }
            };

            item0.addActionListener(listener);
            item1.addActionListener(listener);
            item2.addActionListener(listener);
            
        } else {
            JMenuItem item0 = new JMenuItem("適応症XML出力");
            item0.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    IndicationExporter exporter = new IndicationExporter(RezeptViewer.this);
                    exporter.exportToFile();
                }
            });
            pMenu.add(item0);
            JMenuItem item1 = new JMenuItem("適応症XML読込");
            item1.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    IndicationExporter exporter = new IndicationExporter(RezeptViewer.this);
                    exporter.importFromFile();
                }
            });
            pMenu.add(item1);
        }

        pMenu.show(view.getImportBtn(), 0, 0);
    }
    
    // ORCAからレセ電データを取得し、表示する
    private void loadFromOrca(final String ym) {

        blockGlass.setText("処理中です...");
        blockGlass.block();
        view.getPrintBtn().setEnabled(false);
        
        SimpleWorker worker = new SimpleWorker<List<IR_Model>, Void>(){

            @Override
            protected List<IR_Model> doInBackground() throws Exception {
 
                UkeLoader loader = new UkeLoader();
                List<IR_Model> list = loader.loadFromOrca(ym);
                itemSrycdSet = loader.getItemSrycdSet();
                reModelCount = loader.getReModelCount();
                // 医師ごとの担当患者を取得する
                drPatientIdList = MasudaDelegater.getInstance().getDrPatientIdList(ym);
                return list;
            }

            @Override
            protected void succeeded(List<IR_Model> result) {
                
                irList = result;
                blockGlass.setText("");
                blockGlass.unblock();
                
                JComboBox combo = view.getDrCombo();
                combo.removeAllItems();

                if (result != null) {
                    // 担当医コンボを設定する
                    combo.addItem("全て");
                    if (drPatientIdList != null) {
                        for (DrPatientIdModel model : drPatientIdList) {
                            combo.addItem(model);
                        }
                    }
                    view.getPrintBtn().setEnabled(true);
                    //showRezeData(null);
                } else {
                    JOptionPane.showMessageDialog(view, "指定月のレセ電データがありません。",
                            "レセ点", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }

    // レセ電データを表示する
    private void showRezeData(DrPatientIdModel model) {
        
        view.getTabbedPane().removeAll();
        if (irList == null || irList.isEmpty()) {
            clearReModelView();
            return;
        }
        
        List<IR_Model> nyuin = new ArrayList<>(3);
        List<IR_Model> gairai = new ArrayList<>(3);
        
        for (IR_Model irModel : irList) {
            if ("1".equals(irModel.getNyugaikbn())) {
                nyuin.add(irModel);
            } else {
                gairai.add(irModel);
            }
        }
        
        if (!nyuin.isEmpty()) {
            JTabbedPane tabbedPane = createTabbedPane(nyuin, model);
            view.getTabbedPane().add("入院", tabbedPane);
        }
        if (!gairai.isEmpty()) {
            JTabbedPane tabbedPane = createTabbedPane(gairai, model);
            view.getTabbedPane().add("入院外", tabbedPane);
        }
        
        // １件目を表示する
        JTable reTable = getSelectedReTable();
        if (reTable != null && reTable.getRowCount() > 0) {
            reTable.getSelectionModel().clearSelection();
            reTable.getSelectionModel().setSelectionInterval(0, 0);
        } else {
            clearReModelView();
        }
    }
    
    // 担当患者か否かでフィルタリングする
    private void filterReModel(List<RE_Model> reList, DrPatientIdModel model) {
        
        if (model == null) {
            return;
        }
        
        List<String> ptIdList = model.getPatientIdList();
        for (Iterator<RE_Model> itr = reList.iterator(); itr.hasNext();) {
            RE_Model reModel = itr.next();
            if (!ptIdList.contains(reModel.getPatientId())) {
                itr.remove();
            }
        }
    }

    // 審査機関別のJTabbedPaneを作成する
    private JTabbedPane createTabbedPane(List<IR_Model> list, DrPatientIdModel model) {

        String[] columnNames = reTblHelper.getTableModelColumnNames();
        String[] methods = reTblHelper.getTableModelColumnMethods();
        Class[] cls = reTblHelper.getTableModelColumnClasses();

        JTabbedPane tabbedPane = new JTabbedPane();

        for (IR_Model irModel : list) {
            RE_Panel panel = new RE_Panel();
            final JTable table = panel.getReTable();
            final ListTableModel<RE_Model> tableModel = new ListTableModel<>(columnNames, 1, methods, cls);
            List<RE_Model> reList = new ArrayList<>(irModel.getReModelList());
            filterReModel(reList, model);
            tableModel.setDataProvider(reList);
            final ListTableSorter<RE_Model> sorter = new ListTableSorter<>(tableModel);
            table.setModel(sorter);
            sorter.setTableHeader(table.getTableHeader());
            reTblHelper.updateColumnWidth(table);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ListSelectionModel lm = table.getSelectionModel();
            final int kikanNum = irModel.getShinsaKikanNumber();

            lm.addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int row = table.getSelectedRow();
                    if (e.getValueIsAdjusting() == false && row != -1) {
                            RE_Model reModel = sorter.getObject(row);
                            reModelToView(reModel, kikanNum);
                        }
                    }
            });

            table.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = table.getSelectedRow();
                    if (e.getClickCount() == 2 && row != -1) {
                        String patientId = sorter.getObject(row).getPatientId();
                        openKarte(patientId);
                    }
                }
            });

            RE_TableRenderer ren = new RE_TableRenderer();
            ren.setTable(table);
            ren.setDefaultRenderer();

            String kikanName;
            switch (kikanNum) {
                case 1:
                    kikanName = "社保";
                    break;
                case 2:
                    kikanName = "国保";
                    break;
                case 6:
                    kikanName = "後期高齢";
                    break;
                default:
                    kikanName = "Unknown";
                    break;
            }

            panel.getCountField().setText(String.valueOf(irModel.getGOModel().getTotalCount()));
            NumberFormat frmt = NumberFormat.getNumberInstance();
            panel.getTenField().setText(frmt.format(irModel.getGOModel().getTotalTen()));
            tabbedPane.add(kikanName, panel);
        }

        return tabbedPane;
    }

    private void openKarte(String patientId) {
        PatientVisitModel pvt = ChartEventListener.getInstance().createFakePvt(patientId);
        if (pvt != null) {
            Dolphin.getInstance().openKarte(pvt);
        }
    }
    
    private void clearReModelView() {
        
        view.getBillYmField().setText(EMPTY);
        view.getPtIdField().setText(EMPTY);
        view.getPtNameField().setText(EMPTY);
        view.getPtSexField().setText(EMPTY);
        view.getPtBirthdayField().setText(EMPTY);
        view.getPtAgeField().setText(EMPTY);
        view.getInsTypeField().setText(EMPTY);
        view.getInsField().setText(EMPTY);
        view.getInsSymbolField().setText(EMPTY);
        view.getInsNumberField().setText(EMPTY);
        view.getPubIns1Field().setText(EMPTY);
        view.getPubIns1NumField().setText(EMPTY);
        view.getPubIns2Field().setText(EMPTY);
        view.getPubIns2NumField().setText(EMPTY);
        view.getPubIns3Field().setText(EMPTY);
        view.getPubIns3NumField().setText(EMPTY);
        view.getTenField().setText(EMPTY);
        view.getNumDayField().setText(EMPTY);
        view.getDiagCountField().setText(EMPTY);
        view.getCommentArea().setText(EMPTY);
        view.getFacilityField().setText(EMPTY);

        diagTableModel.setDataProvider(null);
        itemTableModel.setDataProvider(null);
        infoTableModel.setDataProvider(null);
    }
    
    private void reModelToView(RE_Model reModel, int kikanNum) {
        
        clearReModelView();
        view.getFacilityField().setText(facilityName);
        
        // 患者
        String ym = RezeUtil.getInstance().getYMStr(reModel.getBillDate());
        view.getBillYmField().setText(ym);
        view.getPtIdField().setText(reModel.getPatientId());
        view.getPtNameField().setText(reModel.getName());
        view.getPtSexField().setText(reModel.getSex());
        String birthday = RezeUtil.getInstance().getDateStr(reModel.getBirthday());
        view.getPtBirthdayField().setText(birthday);
        int age = RezeUtil.getInstance().getAge(reModel.getBirthday());
        view.getPtAgeField().setText(String.valueOf(age));
        
        int ten = 0;
        int numDays = 0;
        // 保険
        view.getInsTypeField().setText(reModel.getRezeType(kikanNum));
        HO_Model hoModel = reModel.getHOModel();
        if (hoModel != null) {
            view.getInsField().setText(hoModel.getInsuranceNum());
            view.getInsSymbolField().setText(hoModel.getInsuranceSymbol());
            view.getInsNumberField().setText(hoModel.getCertificateNum());
            ten += hoModel.getTen();
            numDays += hoModel.getDays();
        }
        // 公費
        if (reModel.getKOModelList() != null && !reModel.getKOModelList().isEmpty()) {
            List<KO_Model> list = reModel.getKOModelList();
            int cnt = list.size();
            ten = 0;    // 点計算がよくわからない
            if (cnt > 0) {
                KO_Model koModel = list.get(0);
                view.getPubIns1Field().setText(koModel.getInsuranceNum());
                view.getPubIns1NumField().setText(koModel.getCertificateNum());
                ten += koModel.getTen();
                numDays += koModel.getDays();
            }
            if (cnt > 1) {
                KO_Model koModel = list.get(1);
                view.getPubIns2Field().setText(koModel.getInsuranceNum());
                view.getPubIns2NumField().setText(koModel.getCertificateNum());
                ten += koModel.getTen();
                numDays += koModel.getDays();
            }
            if (cnt > 2) {
                KO_Model koModel = list.get(2);
                view.getPubIns3Field().setText(koModel.getInsuranceNum());
                view.getPubIns3NumField().setText(koModel.getCertificateNum());
                ten += koModel.getTen();
                numDays += koModel.getDays();
            }
            if (cnt > 3) {
                KO_Model koModel = list.get(3);
                view.getPubIns4Field().setText(koModel.getInsuranceNum());
                view.getPubIns4NumField().setText(koModel.getCertificateNum());
                ten += koModel.getTen();
                numDays += koModel.getDays();
            }
        }
        
        // 点数、診療日数
        NumberFormat frmt = NumberFormat.getNumberInstance();
        view.getTenField().setText(frmt.format(ten));
        view.getNumDayField().setText(String.valueOf(numDays));
        
        // 病名
        // そのままsetDataProviderしてはいけない
        List<SY_Model> syModelList = new ArrayList<>(reModel.getSYModelList());
        diagTableModel.setDataProvider(syModelList);
        view.getDiagCountField().setText(String.valueOf(syModelList.size()));
        
        // 診療行為
        List<IRezeItem> itemList = new ArrayList<>(reModel.getItemList());
        itemTableModel.setDataProvider(itemList);
        
        // 症状詳記
        if (reModel.getSJModelList() != null && !reModel.getSJModelList().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (SJ_Model sjModel : reModel.getSJModelList()) {
                sb.append("<").append(sjModel.getKbn()).append(">\n");
                sb.append(sjModel.getData()).append("\n");
            }
            view.getCommentArea().setText(sb.toString());
        }
        
        // Info
        if (reModel.getCheckResults() != null) {
            List<CheckResult> results = new ArrayList<>(reModel.getCheckResults());
            infoTableModel.setDataProvider(results);
        }
    }
    
    // 連ドラ
    private static class RE_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int col) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            
            ListTableSorter<RE_Model> sorter = (ListTableSorter<RE_Model>) table.getModel();
            RE_Model reModel = sorter.getObject(row);
            
            if (col == 0) {
                setText("");
                setHorizontalAlignment(CENTER);
                switch(reModel.getCheckFlag()) {
                    case CheckResult.CHECK_INFO:
                        setIcon(INFO_ICON);
                        break;
                    case CheckResult.CHECK_WARNING:
                        setIcon(WARN_ICON);
                        break;
                    case CheckResult.CHECK_ERROR:
                        setIcon(ERROR_ICON);
                        break;
                    default:
                        setIcon(null);
                }
            } else {
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }
            
            if (!isSelected) {
                switch (reModel.getCheckFlag()) {
                    case CheckResult.CHECK_INFO:
                        setForeground(Color.BLUE);
                        break;
                    case CheckResult.CHECK_WARNING:
                        setForeground(Color.MAGENTA);
                        break;
                    case CheckResult.CHECK_ERROR:
                        setForeground(Color.RED);
                        break;
                    default:
                        setForeground(Color.BLACK);
                        break;
                }
            }
            
            return this;
        }
    }
    
    private static class SY_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {

                ListTableModel<SY_Model> tableModel = (ListTableModel<SY_Model>) table.getModel();
                SY_Model syModel = tableModel.getObject(row);

                if (!syModel.isPass()) {
                    // ドボン
                    setForeground(Color.RED);
                } else if (syModel.getHitCount() == 0) {
                    // 余剰病名
                    setForeground(Color.BLUE);

                } else {
                    setForeground(Color.BLACK);
                }
            }
            
            return this;
        }
    }
    
    private static class ITEM_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                
                ListTableModel<IRezeItem> tableModel = (ListTableModel<IRezeItem>) table.getModel();
                IRezeItem rezeItem = tableModel.getObject(row);

                if (rezeItem.isPass()) {
                    setForeground(Color.BLACK);
                } else {
                    setForeground(Color.RED);
                }
            }

            return this;
        }
    }
    
    private static class INFO_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (column == 0) {
                setText("");
                setHorizontalAlignment(CENTER);
                int i = (int) value;
                switch(i) {
                    case CheckResult.CHECK_INFO:
                        setIcon(INFO_ICON);
                        break;
                    case CheckResult.CHECK_WARNING:
                        setIcon(WARN_ICON);
                        break;
                    case CheckResult.CHECK_ERROR:
                        setIcon(ERROR_ICON);
                        break;
                    default:
                        setIcon(null);
                }
            } else {
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }
}
