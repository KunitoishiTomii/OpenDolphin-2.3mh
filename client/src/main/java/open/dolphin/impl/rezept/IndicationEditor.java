package open.dolphin.impl.rezept;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.client.ClientContext;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.impl.rezept.model.IRezeItem;
import open.dolphin.impl.rezept.model.SY_Model;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * 適応症えぢた
 * 
 * @author masuda, Masuda Naika
 */
public class IndicationEditor {
    
    private static final String PMDA_URL = "http://www.info.pmda.go.jp/psearch/html/menu_tenpu_base.html";
    
    private static final String DIAG_TBL_SPEC_NAME = "indication.retable.column.spec";
    private static final String[] DIAG_TBL_COLUMN_NAMES = {"傷病名", "特定疾患"};
    private static final String[] DIAG_TBL_PROPERTY_NAMES = {"getDiagName", "getByoKanrenKbnStr"};
    private static final Class[] DIAG_TBL_CLASSES = {String.class, String.class};
    private static final int[] DIAG_TBL_COLUMN_WIDTH = {100, 20};
    
    private static final String INDICATION_TBL_SPEC_NAME = "indication.indicationtable.column.spec";
    private static final String[] INDICATION_TBL_COLUMN_NAMES = {"無効", "NOT", "キーワード"};
    private static final String[] INDICATION_TBL_PROPERTY_NAMES = {"isDisabled", "isNotCondition", "getKeyword"};
    private static final Class[] INDICATION_TBL_CLASSES = {Boolean.class, Boolean.class, String.class};
    private static final int[] INDICATION_TBL_COLUMN_WIDTH = {10, 10, 100};
    
    private JDialog dialog;
    private final IndicationView view;
    private final RezeptViewer rezeptViewer;
    
    private ColumnSpecHelper diagTblHelper;
    private ColumnSpecHelper indicationTblHelper;
    
    private ListTableModel<SY_Model> diagTableModel;
    private ListTableModel<IndicationItem> indicationTableModel;
    private List<SY_Model> diagList;
    
    private IRezeItem rezeItem;
    private IndicationModel indication;
    
    private boolean editable;
    
    
    public IndicationEditor(RezeptViewer rezeptViewer) {
        view = new IndicationView();
        this.rezeptViewer = rezeptViewer;
    }
    
    public void start(IRezeItem rezeItem, List<SY_Model> diagList) {
        
        this.rezeItem = rezeItem;
        this.diagList = diagList;
        
        initComponents();
        modelToView();
        
        if (!editable) {
            String msg = String.format("%sは現在編集できません", rezeItem.getDescription());
            String title = "適応病名エディタ";
                    
            JOptionPane.showMessageDialog(null, msg, title, JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        
        // do not remove copyright!
        String title = ClientContext.getFrameTitle("適応症エディタ") + ", Masuda Naika";
        dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setModal(true);
        
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                stop();

            }
        });
        
        dialog.setContentPane(view);
        dialog.pack();

        ComponentMemory cm = new ComponentMemory(dialog, new Point(100, 100), dialog.getPreferredSize(), view);
        cm.setToPreferenceBounds();
        dialog.setVisible(true);
    }
    
    private void initComponents() {
        
        // ColumnSpecHelperを準備する
        diagTblHelper = new ColumnSpecHelper(DIAG_TBL_SPEC_NAME, DIAG_TBL_COLUMN_NAMES,
                DIAG_TBL_PROPERTY_NAMES, DIAG_TBL_CLASSES, DIAG_TBL_COLUMN_WIDTH);
        diagTblHelper.loadProperty();

        indicationTblHelper = new ColumnSpecHelper(INDICATION_TBL_SPEC_NAME, INDICATION_TBL_COLUMN_NAMES,
                INDICATION_TBL_PROPERTY_NAMES, INDICATION_TBL_CLASSES, INDICATION_TBL_COLUMN_WIDTH);
        indicationTblHelper.loadProperty();
        
        // 傷病名テーブル
        JTable diagTable = view.getDiagTable();
        StripeTableCellRenderer syRen = new StripeTableCellRenderer();
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
        
        // 適応テーブル
        JTable indicationTable = view.getIndicationTable();
        StripeTableCellRenderer indRen = new StripeTableCellRenderer();
        indRen.setTable(indicationTable);
        indRen.setDefaultRenderer();
        indicationTblHelper.setTable(indicationTable);
        // カラム設定、モデル設定
        String[] indColumnNames = indicationTblHelper.getTableModelColumnNames();
        String[] indMethods = indicationTblHelper.getTableModelColumnMethods();
        Class[] indCls = indicationTblHelper.getTableModelColumnClasses();
        indicationTableModel = new ListTableModel<>(indColumnNames, 1, indMethods, indCls);
        indicationTable.setModel(indicationTableModel);
        indicationTblHelper.updateColumnWidth();
        
        // connect
        // 傷病名テーブルダブルクリックでkeyword fieldに入力
        view.getDiagTable().addMouseListener(new MouseAdapter(){
        
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyDiagToKeywordFld();
                }
            }
        });
        
        // open PMDA site
        view.getPmdaButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    copyToClipboard();
                    URI uri = new URI(PMDA_URL);
                    Desktop.getDesktop().browse(uri);
                } catch (IOException | URISyntaxException ex) {
                }
            }
        });
        // copy item name
        view.getCopyButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipboard();
            }
        });
        // import from orca
        view.getOrcaButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                referOrca();
            }
        });
        // delete
        view.getDeleteButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteIndicationItem();
            }
        });
        // clear
        view.getClearButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                clearKeyword();
            }
        });
        // add
        view.getAddButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                addKeyword();
            }
        });
        // close
        view.getCloseButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
                dialog.setVisible(false);
            }
        });
        // keywordField
        JTextField keywordFld = view.getKeywordFld();
        keywordFld.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                view.getAddButton().doClick();
                
            }
        });
        keywordFld.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkValidation();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkValidation();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkValidation();
            }
        });
        checkValidation();
    }
    
    // 傷病名をキーワードフィールドにコピーする
    private void copyDiagToKeywordFld() {
        
        int row = view.getDiagTable().getSelectedRow();
        
        if (row != -1) {
            SY_Model syModel = diagTableModel.getObject(row);
            String keyword = syModel.getDiagName().trim();
            view.getKeywordFld().setText(keyword);
            view.getNotCheck().setSelected(false);
        }
    }
    
    // ORCAからインポート
    private void referOrca() {
        
        // ORCAを参照
        SqlMiscDao dao = SqlMiscDao.getInstance();
        IndicationModel iModel = dao.getTekiouByomei(rezeItem.getSrycd());
        List<IndicationItem> items = iModel.getIndicationItems();
        
        // 新しいものがあれば追加する
        for (IndicationItem item : items) {
            boolean found = false;
            for (IndicationItem exist : indicationTableModel.getDataProvider()) {
                if (isSameIndicationItem(exist, item)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                indicationTableModel.addObject(item);
            }
        }
    }
    
    private boolean isSameIndicationItem(IndicationItem item1, IndicationItem item2) {
        
        boolean same = true;
        
        if (item1 == null || item2 == null) {
            return false;
        }
        if (item1.getKeyword() == null) {
            return false;
        }
        same &= item1.getKeyword().equals(item2.getKeyword());
        same &= item1.isNotCondition() == item2.isNotCondition();
        
        return same;
    }
    
    private void deleteIndicationItem() {
        int row = view.getIndicationTable().getSelectedRow();
        if (row != -1) {
            indicationTableModel.deleteAt(row);
        }
    }
    
    private void clearKeyword() {
        view.getKeywordFld().setText("");
        view.getNotCheck().setSelected(false);
    }
    
    private void addKeyword() {
        
        // IndicationItemを作成する
        String keyword = view.getKeywordFld().getText().trim();
        IndicationItem item = new IndicationItem();
        item.setIndicationModel(indication);
        item.setKeyword(keyword);
        item.setNotCondition(view.getNotCheck().isSelected());
        
        indicationTableModel.addObject(item);
        
        clearKeyword();
    }
    
    private void checkValidation() {
        
        JTextField tf = view.getKeywordFld();
        String data = tf.getText().trim();
        if (data.isEmpty()) {
            tf.setForeground(Color.BLACK);
            view.getAddButton().setEnabled(false);
            return;
        }
        try {
            LexicalAnalyzer.getTokens(data);
            tf.setForeground(Color.BLACK);
            view.getAddButton().setEnabled(true);
        } catch (Exception ex) {
            // 構文誤りならば赤色にする
            tf.setForeground(Color.RED);
            view.getAddButton().setEnabled(false);
        }
    }
    
    private void copyToClipboard() {
        
        if (rezeItem != null) {
            String str = rezeItem.getDescription();
            Toolkit kit = Toolkit.getDefaultToolkit();
            Clipboard clip = kit.getSystemClipboard();
            StringSelection ss = new StringSelection(str);
            clip.setContents(ss, ss);
        }
    }

    private void modelToView() {

        String srycd = rezeItem.getSrycd();
        try {
            MasudaDelegater del = MasudaDelegater.getInstance();
            indication = del.getIndicationModel(srycd);
            editable = !indication.isLock();
            // 排他処理
            indication.setLock(true);
            del.updateIndicationModel(indication);
        } catch (Exception ex) {
        }

        view.getSrycdFld().setText(srycd);
        view.getNameFld().setText(rezeItem.getDescription());
        
        view.getAdmissionChk().setSelected(indication.isAdmission());
        view.getOutPatientChk().setSelected(indication.isOutPatient());
        view.getInclusiveChk().setSelected(indication.isInclusive());
        
        diagTableModel.setDataProvider(diagList);
        indicationTableModel.setDataProvider(indication.getIndicationItems());

    }
    
    private void viewToModel() {
        
        indication.setAdmission(view.getAdmissionChk().isSelected());
        indication.setOutPatient(view.getOutPatientChk().isSelected());
        indication.setInclusive(view.getInclusiveChk().isSelected());
        indication.setIndicationItems(indicationTableModel.getDataProvider());
        indication.setLock(false);
    }
    
    private void stop() {

        if (editable && indication != null) {
            // ColumnSpecを保存する
            diagTblHelper.saveProperty();
            indicationTblHelper.saveProperty();
            viewToModel();
            try {
                MasudaDelegater del = MasudaDelegater.getInstance();
                del.updateIndicationModel(indication);
            } catch (Exception ex) {
            }
            // indicationMapも更新する
            rezeptViewer.getIndicationMap().put(indication.getSrycd(), indication);
        }
    }
}
