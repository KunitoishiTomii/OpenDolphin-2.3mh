package open.dolphin.impl.rezept;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.BlockGlass;
import open.dolphin.client.ChartEventListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.Dolphin;
import open.dolphin.client.IChartEventListener;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.helper.SimpleWorker;
import open.dolphin.helper.WindowSupport;
import open.dolphin.impl.rezept.filter.CheckResult;
import open.dolphin.impl.rezept.model.*;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * レセ電ビューアーもどき
 * 
 * @author masuda, Masuda Naika
 */
public class RezeptViewer implements IChartEventListener {
    
    private static final String EMPTY = "";
    
    private static final String RE_TBL_SPEC_NAME = "reze.retable.column.spec";
    private static final String[] RE_TBL_COLUMN_NAMES = {"ID", "氏名", "性別", "状態"};
    private static final String[] RE_TBL_PROPERTY_NAMES = {"getPatientId", "getName", "getSex", "isOpened"};
    private static final Class[] RE_TBL_CLASSES = {String.class, String.class, String.class, Object.class};
    private static final int[] RE_TBL_COLUMN_WIDTH = {20, 40, 10, 5};
    
    private static final String DIAG_TBL_SPEC_NAME = "reze.diagtable.column.spec";
    private static final String[] DIAG_TBL_COLUMN_NAMES = {"傷病名", "開始日", "転帰", "特定疾患"};
    private static final String[] DIAG_TBL_PROPERTY_NAMES = {"getDiagName", "getStartDate", "getOutcomeStr", "getByoKanrenKbnStr"};
    private static final Class[] DIAG_TBL_CLASSES = {String.class, String.class, String.class, String.class};
    private static final int[] DIAG_TBL_COLUMN_WIDTH = {100, 20, 10, 20};
    
    private static final String ITEM_TBL_SPEC_NAME = "reze.itemtable.column.spec";
    private static final String[] ITEM_TBL_COLUMN_NAMES = {"区分", "項目", "数量", "点数", "回数"};
    private static final String[] ITEM_TBL_PROPERTY_NAMES = {"getClassCode", "getDescription", "getNumber", "getTen", "getCount"};
    private static final Class[] ITEM_TBL_CLASSES = {String.class, String.class, Float.class, Float.class, Integer.class};
    private static final int[] ITEM_TBL_COLUMN_WIDTH = {10, 100, 10, 10, 10};
    
    private static final String INFO_TBL_SPEC_NAME = "reze.infotable.column.spec";
    private static final String[] INFO_TBL_COLUMN_NAMES = {"区分", "項目", "内容"};
    private static final String[] INFO_TBL_PROPERTY_NAMES = {"getClassCode", "getDescription", "getNumber", "getTen", "getCount"};
    private static final Class[] INFO_TBL_CLASSES = {Integer.class, String.class, String.class};
    private static final int[] INFO_TBL_COLUMN_WIDTH = {10, 100, 200};
    
    private RezeptView view;
    private BlockGlass blockGlass;
    private ColumnSpecHelper reTblHelper;
    private ColumnSpecHelper diagTblHelper;
    private ColumnSpecHelper itemTblHelper;
    private ColumnSpecHelper infoTblHelper;
    
    private ListTableModel<SY_Model> diagTableModel;
    private ListTableModel<IRezeItem> itemTableModel;
    private ListTableModel<CheckResult> infoTableModel;
    
    private static final String clientUUID;
    private final ChartEventListener cel;
    
    static {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }
    
    public RezeptViewer() {
        cel = ChartEventListener.getInstance();
    }
    
    public void enter() {
        
        initComponents();
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
        
        blockGlass = new BlockGlass();
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
        RE_Panel rePanel = getSelectedRePanel();
        if (rePanel != null) {
            JTable reTable = rePanel.getReTable();
            reTblHelper.saveProperty(reTable);
        }
        
        // ChartStateListenerから除去する
        cel.removeListener(this);
    }
    
    private RE_Panel getSelectedRePanel() {
        RE_Panel rePanel = (RE_Panel) view.getTabbedPane().getSelectedComponent();
        return rePanel;
    }
    
    private void initComponents() {
        
        // ChartEventListenerに登録する
        cel.addListener(this);
        
        view = new RezeptView();
        
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
        JTable itemTable = view.getItemTable();
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
        JTable infoTable = view.getInfoTable();
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
                showImportPopup();
            }
        });
        
        view.getPrevBtn().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                RE_Panel rePanel = getSelectedRePanel();
                if (rePanel != null) {
                    JTable reTable = rePanel.getReTable();
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
                RE_Panel rePanel = getSelectedRePanel();
                if (rePanel != null) {
                    JTable reTable = rePanel.getReTable();
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
                // not yet!
            }
        });
    }
    
    // ボタンクリックでポップアップ表示
    private void showImportPopup() {
        
        GregorianCalendar gc = new GregorianCalendar();
        SimpleDateFormat frmt = new SimpleDateFormat("yyyyMM");
        String ymd2 = frmt.format(gc.getTime());
        gc.add(GregorianCalendar.MONTH, -1);
        String ymd1 = frmt.format(gc.getTime());

        JPopupMenu pMenu = new JPopupMenu();
        JMenuItem item1 = new JMenuItem(ymd1);
        JMenuItem item2 = new JMenuItem(ymd2);
        pMenu.add(item1);
        pMenu.add(item2);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String ym = e.getActionCommand();
                //ym = "201303";  // development
                loadFromOrca(ym);
            }
        };
        
        item1.addActionListener(listener);
        item2.addActionListener(listener);

        pMenu.show(view.getImportBtn(), 0, 0);
    }
    
    // ORCAからレセ電データを取得し、表示する
    private void loadFromOrca(final String ym) {

        view.getTabbedPane().removeAll();
        
        SimpleWorker worker = new SimpleWorker<List<IR_Model>, Void>(){

            @Override
            protected List<IR_Model> doInBackground() throws Exception {
                blockGlass.setText("処理中です...");
                blockGlass.block();
                UkeLoader loader = new UkeLoader();
                List<IR_Model> list = loader.loadFromOrca(ym);
                return list;
            }

            @Override
            protected void succeeded(List<IR_Model> result) {
                
                blockGlass.setText("");
                blockGlass.unblock();

                if (result != null) {
                    showRezeData(result);
                } else {
                    JOptionPane.showMessageDialog(view, "指定月のレセ電データがありません。",
                            "レセ点", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }

    private void showRezeData(List<IR_Model> list) {

        String[] columnNames = reTblHelper.getTableModelColumnNames();
        String[] methods = reTblHelper.getTableModelColumnMethods();
        Class[] cls = reTblHelper.getTableModelColumnClasses();

        for (IR_Model irModel : list) {
            RE_Panel panel = new RE_Panel();
            final JTable table = panel.getReTable();
            final ListTableModel<RE_Model> tableModel = new ListTableModel<>(columnNames, 1, methods, cls);
            List<RE_Model> reList = new ArrayList<>(irModel.getReModelList());
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
                        PatientModel pm = tableModel.getObject(row).getPatientModel();
                        openKarte(pm);
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
            view.getTabbedPane().add(kikanName, panel);

            // １件目を表示する
            RE_Panel rePanel = getSelectedRePanel();
            final JTable reTable = rePanel.getReTable();
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    reTable.getSelectionModel().setSelectionInterval(0, 0);
                }
            });
        }
    }

    private void openKarte(PatientModel pm) {
        PatientVisitModel pvt = ChartEventListener.getInstance().createFakePvt(pm);
        Dolphin.getInstance().openKarte(pvt);
    }
    
    private void reModelToView(RE_Model reModel, int kikanNum) {
        
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
        view.getInsTypeArea().setText(reModel.getRezeType(kikanNum));
        HO_Model hoModel = reModel.getHOModel();
        if (hoModel != null) {
            view.getInsField().setText(hoModel.getInsuranceNum());
            view.getInsSymbolField().setText(hoModel.getInsuranceSymbol());
            view.getInsNumberField().setText(hoModel.getCertificateNum());
            ten += hoModel.getTen();
            numDays += hoModel.getDays();
        } else {
            view.getInsField().setText(EMPTY);
            view.getInsSymbolField().setText(EMPTY);
            view.getInsNumberField().setText(EMPTY);
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
        } else {
            view.getPubIns1Field().setText(EMPTY);
            view.getPubIns1NumField().setText(EMPTY);
            view.getPubIns2Field().setText(EMPTY);
            view.getPubIns2NumField().setText(EMPTY);
            view.getPubIns3Field().setText(EMPTY);
            view.getPubIns3NumField().setText(EMPTY);
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
        } else {
            view.getCommentArea().setText("");
        }

    }

    @Override
    public void onEvent(ChartEventModel evt) throws Exception {
        
        // JTabbedPaneからListTableModelを取得
        List<ListTableModel<RE_Model>> tableModelList = new ArrayList<>();
        for (Component comp : view.getTabbedPane().getComponents()) {
            RE_Panel panel = (RE_Panel) comp;
            JTable reTable = panel.getReTable();
            ListTableSorter<RE_Model> sorter = (ListTableSorter<RE_Model>) reTable.getModel();
            tableModelList.add(sorter.getListTableModel());
        }

        // 各tabbed paneに配置されたテーブルを更新　めんどくちゃ
        for (ListTableModel<RE_Model> tableModel : tableModelList) {
            
            List<PatientModel> list = new ArrayList<>();
            for (RE_Model reModel : tableModel.getDataProvider()) {
                list.add(reModel.getPatientModel());
            }
            int sRow = -1;
            long ptPk = evt.getPtPk();

            ChartEventModel.EVENT eventType = evt.getEventType();

            switch (eventType) {
                case PVT_STATE:
                    for (int row = 0; row < list.size(); ++row) {
                        PatientModel pm = list.get(row);
                        if (ptPk == pm.getId()) {
                            sRow = row;
                            pm.setOwnerUUID(evt.getOwnerUUID());
                            break;
                        }
                    }
                    break;
                case PM_MERGE:
                    for (int row = 0; row < list.size(); ++row) {
                        PatientModel pm = list.get(row);
                        if (ptPk == pm.getId()) {
                            sRow = row;
                            list.set(row, evt.getPatientModel());
                            break;
                        }
                    }
                    break;
                case PVT_MERGE:
                    for (int row = 0; row < list.size(); ++row) {
                        PatientModel pm = list.get(row);
                        if (ptPk == pm.getId()) {
                            sRow = row;
                            list.set(row, evt.getPatientVisitModel().getPatientModel());
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }

            if (sRow != -1) {
                tableModel.fireTableRowsUpdated(sRow, sRow);
            }
        }
    }
    
    private static class RE_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int col) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            
            ListTableSorter<RE_Model> sorter = (ListTableSorter<RE_Model>) table.getModel();
            PatientModel pm = sorter.getObject(row).getPatientModel();
            if (pm == null) {
                return this;
            }
            
            if (col == 3) {
                setHorizontalAlignment(CENTER);
                setBorder(null);
                if (pm.isOpened()) {
                    if (clientUUID.equals(pm.getOwnerUUID())) {
                        setIcon(OPEN_ICON);
                    } else {
                        setIcon(NETWORK_ICON);
                    }
                } else {
                    setIcon(null);
                }
                setText("");
            } else {
                setIcon(null);
                setText(value == null ? "" : value.toString());
            }
            
            return this;
        }
    }
    
    private static class SY_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
    private static class ITEM_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
    private class INFO_TableRenderer extends StripeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
