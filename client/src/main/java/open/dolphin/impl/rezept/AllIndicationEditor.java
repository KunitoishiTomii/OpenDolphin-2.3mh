package open.dolphin.impl.rezept;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.ClientContext;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * AllIndicationEditor
 * 
 * @author masuda, Masuda Naika
 */
public class AllIndicationEditor {
    
    private static final String PMDA_URL = "http://www.info.pmda.go.jp/psearch/html/menu_tenpu_base.html";
    private static final String EMPTY = "";
    
    private static final String MODEL_TBL_SPEC_NAME = "indication.allindicationtable.column.spec";
    private static final String[] MODEL_TBL_COLUMN_NAMES = {"コード", "名称"};
    private static final String[] MODEL_TBL_PROPERTY_NAMES = {"getSrycd", "getDescription"};
    private static final Class[] MODEL_TBL_CLASSES = { String.class, String.class};
    private static final int[] MODEL_TBL_COLUMN_WIDTH = {50, 100};
    
    private static final String INDICATION_TBL_SPEC_NAME = "indication.indicationitemtable.column.spec";
    private static final String[] INDICATION_TBL_COLUMN_NAMES = {"無効", "禁忌", "81コメント", "キーワード"};
    private static final String[] INDICATION_TBL_PROPERTY_NAMES = {"isDisabled", "isNotCondition", "getDescription", "getKeyword"};
    private static final Class[] INDICATION_TBL_CLASSES = {Boolean.class, Boolean.class, String.class, String.class};
    private static final int[] INDICATION_TBL_COLUMN_WIDTH = {10, 10, 50, 100};
    
    private JDialog dialog;
    private final RezeptViewer rezeptViewer;
    private final AllIndicationView view;
    
    private ColumnSpecHelper modelTblHelper;
    private ColumnSpecHelper indicationTblHelper;
    
    private ListTableModel<IndicationModel> modelTableModel;
    private ListTableSorter<IndicationModel> sorter;
    private ListTableModel<IndicationItem> indicationTableModel;
    
    // 編集中のIndicationModel
    private IndicationModel currentModel;
    private boolean locked;

    
    public AllIndicationEditor(RezeptViewer viewer) {
        rezeptViewer = viewer;
        view = new AllIndicationView();
    }
    
    public void start() {
        
        initComponents();
        loadIndicationModels();

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
        modelTblHelper = new ColumnSpecHelper(MODEL_TBL_SPEC_NAME, MODEL_TBL_COLUMN_NAMES,
                MODEL_TBL_PROPERTY_NAMES, MODEL_TBL_CLASSES, MODEL_TBL_COLUMN_WIDTH);
        modelTblHelper.loadProperty();

        indicationTblHelper = new ColumnSpecHelper(INDICATION_TBL_SPEC_NAME, INDICATION_TBL_COLUMN_NAMES,
                INDICATION_TBL_PROPERTY_NAMES, INDICATION_TBL_CLASSES, INDICATION_TBL_COLUMN_WIDTH);
        indicationTblHelper.loadProperty();
        
        // 適応テーブル
        JTable modelTable = view.getModelTable();
        StripeTableCellRenderer syRen = new StripeTableCellRenderer();
        syRen.setTable(modelTable);
        syRen.setDefaultRenderer();
        modelTblHelper.setTable(modelTable);
        // カラム設定、モデル設定
        String[] modelColumnNames = modelTblHelper.getTableModelColumnNames();
        String[] modelMethods = modelTblHelper.getTableModelColumnMethods();
        Class[] modelCls = modelTblHelper.getTableModelColumnClasses();
        modelTableModel = new ListTableModel<>(modelColumnNames, 1, modelMethods, modelCls);
        sorter = new ListTableSorter<>(modelTableModel);
        modelTable.setModel(sorter);
        sorter.setTableHeader(modelTable.getTableHeader());
        modelTblHelper.updateColumnWidth();
        
        // キーワードテーブル
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
        view.getModelTable().getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = view.getModelTable().getSelectedRow();
                if (e.getValueIsAdjusting() == false && row != -1) {
                    IndicationModel model = sorter.getObject(row);
                    if (model != null) {
                        setCurrentModel(model.getSrycd(), model.getDescription());
                        modelToView();
                    }
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
        // save
        view.getSaveButton().addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                save();
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
        setButtonsEnable(false);
        checkValidation();
    }
    
    // ORCAからインポート
    private void referOrca() {

        if (currentModel == null) {
            return;
        }
        
        // ORCAを参照
        SqlMiscDao dao = SqlMiscDao.getInstance();
        IndicationModel iModel = dao.getTekiouByomei(currentModel.getSrycd());
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
        item.setIndicationModel(currentModel);
        item.setKeyword(keyword);
        item.setNotCondition(view.getNotCheck().isSelected());
        if (is81Comment(currentModel)) {
            String description = view.getCommentFld().getText().trim();
            item.setDescription(description);
        }
        
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
        
        clearKeyword();
    }
    
    private boolean is81Comment(IndicationModel model) {
        return model != null && "810000001".equals(model.getSrycd());
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
        
        if (currentModel != null) {
            String str = currentModel.getDescription();
            Toolkit kit = Toolkit.getDefaultToolkit();
            Clipboard clip = kit.getSystemClipboard();
            StringSelection ss = new StringSelection(str);
            clip.setContents(ss, ss);
        }
    }
    
    private void setCurrentModel(String srycd, String description) {
        try {
            MasudaDelegater del = MasudaDelegater.getInstance();
            IndicationModel model = del.getIndicationModel(srycd);
            locked = model.isLock();
            if (locked) {
                model.setDescription("現在編集できません");
                setButtonsEnable(false);
            } else {
                // 排他処理
                model.setLock(true);
                del.updateIndicationModel(model);
                model.setDescription(description);
                setButtonsEnable(true);
            }
            currentModel = model;
        } catch (Exception ex) {
            currentModel = null;
            setButtonsEnable(false);
        }
    }
    
    private void loadIndicationModels() {
        try {
            List<String> srycds = Collections.emptyList();
            List<IndicationModel> list = MasudaDelegater.getInstance().getIndicationList(srycds);
            
            if (!list.isEmpty()) {
                Collections.sort(list, new IndicationModelComparator());
                setMasterName(list);
                modelTableModel.setDataProvider(list);
            } else {
                modelTableModel.setDataProvider(null);
            }
        } catch (Exception ex) {
            modelTableModel.setDataProvider(null);
        }
    }

    private void setMasterName(List<IndicationModel> list) {
        
        Map<String, IndicationModel> map = new HashMap<>();
        for (IndicationModel model : list) {
            map.put(model.getSrycd(), model);
        }
        
        List<TensuMaster> tmList = SqlMiscDao.getInstance().getTensuMasterList(map.keySet());
        
        for (TensuMaster tm : tmList) {
            String srycd = tm.getSrycd();
            IndicationModel model = map.get(srycd);
            if (is81Comment(model)) {
                model.setDescription("81コメント");
            } else {
                model.setDescription(tm.getName());
            }
        }
        
        map.clear();
    }
    
    private void save() {
        
        if (currentModel == null) {
            return;
        }
        setButtonsEnable(false);
        viewToModel();
        try {
            MasudaDelegater.getInstance().updateIndicationModel(currentModel);
            rezeptViewer.getIndicationMap().put(currentModel.getSrycd(), currentModel);
        } catch (Exception ex) {
        }
        
    }

    private void setButtonsEnable(boolean enable) {
        
        view.getDeleteButton().setEnabled(enable);
        view.getClearButton().setEnabled(enable);
        view.getAddButton().setEnabled(enable);
        view.getSaveButton().setEnabled(enable);
        view.getAdmissionChk().setEnabled(enable);
        view.getOutPatientChk().setEnabled(enable);
        view.getInclusiveChk().setEnabled(enable);
        view.getNotCheck().setEnabled(enable);
        view.getKeywordFld().setEnabled(enable);
        view.getCommentFld().setEnabled(enable);
        
    }

    private void modelToView() {
        
        initView();
        
        if (currentModel == null) {
            indicationTableModel.setDataProvider(null);
            return;
        }

        view.getSrycdFld().setText(currentModel.getSrycd());
        view.getNameFld().setText(currentModel.getDescription());
        view.getAdmissionChk().setSelected(currentModel.isAdmission());
        view.getOutPatientChk().setSelected(currentModel.isOutPatient());
        view.getInclusiveChk().setSelected(currentModel.isInclusive());
        if (is81Comment(currentModel)) {
            view.getCommentFld().setEnabled(true);
        } else {
            view.getCommentFld().setEnabled(false);
        }
        
        if (!locked) {
            indicationTableModel.setDataProvider(currentModel.getIndicationItems());
        }
    }
    
    private void initView() {
        
        view.getSrycdFld().setText(EMPTY);
        view.getNameFld().setText(EMPTY);
        view.getAdmissionChk().setSelected(false);
        view.getOutPatientChk().setSelected(false);
        view.getInclusiveChk().setSelected(false);
        view.getCommentFld().setText(EMPTY);
    }
    
    private void viewToModel() {
        
        if (currentModel == null) {
            return;
        }
        
        currentModel.setAdmission(view.getAdmissionChk().isSelected());
        currentModel.setOutPatient(view.getOutPatientChk().isSelected());
        currentModel.setInclusive(view.getInclusiveChk().isSelected());
        currentModel.setIndicationItems(indicationTableModel.getDataProvider());
        currentModel.setLock(false);
    }
    
    private void stop() {
        // ColumnSpecを保存する
        modelTblHelper.saveProperty();
        indicationTblHelper.saveProperty();
    }
    
    private static class IndicationModelComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            String srycd1 = ((IndicationModel) o1).getSrycd();
            String srycd2 = ((IndicationModel) o2).getSrycd();
            return srycd1.compareTo(srycd2);
        }
    }
}
