package open.dolphin.impl.pacsviewer;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.table.ColumnSpecHelper;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;
import open.dolphin.tr.ImageEntryTransferHandler;
import open.dolphin.util.DicomImageEntry;
import open.dolphin.util.ImageTool;
import open.dolphin.util.NamedThreadFactory;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * PACSサーバーから画像を取得するChartDocument
 *
 * @author masuda, Masuda Naika
 */

public class PacsDicomDocImpl extends AbstractChartDocument implements PropertyChangeListener {

    private static final String TITLE = "PACS";
    private static final ImageIcon ICON_WEASIS_S = ClientContext.getImageIcon("weasis_s.png");
    private static final ImageIcon ICON_WEASIS_P = ClientContext.getImageIcon("weasis_p.png");

    private JPanel panel;
    private JButton retrieveBtn;
    private JButton viewBtn;
    private JButton searchBtn;
    private JButton weasisStudyBtn;
    private JButton weasisPatientBtn;
    private JTable listTable;
    private JLabel statusLabel;

    private DicomObject currentDicomObject;
    private boolean useSuffixSearch;

    private ListTableModel<ListDicomObject> listTableModel;
    
    // カラム仕様ヘルパー
    private static final String COLUMN_SPEC_NAME = "pacsTable.column.spec";
    private final String[] COLUMN_NAMES = new String[]{"患者ID","検査日","氏名","性別","生年月日","Modality","Images","Description"};
    private static final String[] PROPERTY_NAMES = new String[]{
        "getPtId","getStudyDate","getPtName","getPtSex","getPtBirthDate","getModalities", "getNumberOfImage","getDescription"};
    private static final Class[] COLUMN_CLASSES = new Class[]{
        String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class
    };
    private static final int[] COLUMN_WIDTH = new int[]{30,30,80,10,30,10,10,50};
    private static final int START_NUM_ROWS = 1;
    private ColumnSpecHelper columnHelper;
    
    private ListTableSorter sorter;

    private static final int MAX_ICON_WIDTH = ImageTool.MAX_ICON_SIZE.width;
    private DefaultListModel<DicomImageEntry> listModel;

    private PacsService pacsService;
    private ExecutorService executor;
    
    private String weasisAddr;
    

    public PacsDicomDocImpl() {
        
        setTitle(TITLE);
        
        // Weasisの設定
        String addr = Project.getString(MiscSettingPanel.PACS_WEASIS_ADDRESS, MiscSettingPanel.DEFAULT_PACS_WEASIS_ADDRESS);
        if (addr != null && !addr.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(addr);
            if (!addr.endsWith("/")) {
                sb.append("/");
            }
            sb.append("weasis-pacs-connector/viewer.jnlp?");
            weasisAddr = sb.toString();
        }
    }

    @Override
    public void start() {
        initComponents();
        NamedThreadFactory factory = new NamedThreadFactory(getClass().getSimpleName());
        executor = Executors.newSingleThreadExecutor(factory);
        pacsService = (PacsService) ((ChartImpl) getContext()).getContext().getPlugin("pacsService");
        pacsService.addPropertyChangeListener(this);
        enter();
    }

    @Override
    public void stop() {
        
        // ColumnSpecsを保存する
        if (columnHelper != null) {
            columnHelper.saveProperty();
        }
        
        if (listTableModel != null) {
            listTableModel.clear();
        }

        if (pacsService != null) {
            pacsService.removePropertyChangeListener(this);
        }
        
        if (listModel != null) {
            listModel.clear();
        }
        
        shutdownExecutor();
    }
    
    private void shutdownExecutor() {

        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
        } catch (NullPointerException ex) {
        }
        executor = null;
    }

    @Override
    public void enter() {
    }

    private void initComponents() {

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // listTableの設定
        listTable = new JTable();
        JScrollPane listScroll = new JScrollPane(listTable);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(new JLabel("Study"), BorderLayout.NORTH);
        panel1.add(listScroll, BorderLayout.CENTER);
        // listTableの右にボタンたち
        searchBtn = new JButton("検索");
        retrieveBtn = new JButton("取得");
        viewBtn = new JButton("閲覧");
        weasisStudyBtn = new JButton();
        weasisStudyBtn.setIcon(ICON_WEASIS_S);
        weasisStudyBtn.setToolTipText("選択中のstudyをWEASISで開きます");
        weasisPatientBtn = new JButton();
        weasisPatientBtn.setIcon(ICON_WEASIS_P);
        weasisPatientBtn.setToolTipText("この患者をWEASISで開きます");
        
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        panel2.add(searchBtn);
        panel2.add(retrieveBtn);
        panel2.add(viewBtn);
        panel2.add(Box.createVerticalStrut(10));
        panel2.add(weasisStudyBtn);
        panel2.add(weasisPatientBtn);
        panel1.add(panel2, BorderLayout.EAST);
        panel1.setPreferredSize(new Dimension(0, 300));
        panel.add(panel1);

        // ImageList を生成する
        listModel = new DefaultListModel();
        ImageEntryJList<ImageEntry> imageList = new ImageEntryJList(listModel);
        imageList.setMaxIconTextWidth(MAX_ICON_WIDTH);
        
        // transferHandler
        imageList.setTransferHandler(ImageEntryTransferHandler.getInstance());

        JScrollPane imageScroll = new JScrollPane(imageList);
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(new JLabel("Image"), BorderLayout.NORTH);
        panel1.add(imageScroll, BorderLayout.CENTER);
        // status label
        statusLabel = new JLabel("OpenDolhin");
        panel1.add(statusLabel, BorderLayout.SOUTH);
        panel1.setPreferredSize(new Dimension(0, 800));
        panel.add(panel1);

        // 検索ボタンの動作
        searchBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                find();
            }
        });
        // 取得ボタンの動作
        retrieveBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                retrieve();
            }
        });
        // 開くボタンの動作
        viewBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openViewer();
            }
        });
        // Weasisボタン
        weasisStudyBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openWeasisByStudyUID();
            }
        });
        weasisPatientBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openWeasisByPatientId();
            }
        });
        if (weasisAddr == null) {
            weasisStudyBtn.setEnabled(false);
            weasisPatientBtn.setEnabled(false);
        }
        
        //列の入れ替えを禁止
        listTable.getTableHeader().setReorderingAllowed(false);
        
        // ColumnSpecHelperを準備する
        columnHelper = new ColumnSpecHelper(COLUMN_SPEC_NAME,
                COLUMN_NAMES, PROPERTY_NAMES, COLUMN_CLASSES, COLUMN_WIDTH);
        columnHelper.loadProperty();
        
        // ColumnSpecHelperにテーブルを設定する
        columnHelper.setTable(listTable);

        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        String[] columnNames = columnHelper.getTableModelColumnNames();
        String[] methods = columnHelper.getTableModelColumnMethods();
        Class[] cls = columnHelper.getTableModelColumnClasses();
        
        // listTableの設定
        listTableModel = new ListTableModel<>(columnNames, 1, methods, cls);
        sorter = new ListTableSorter(listTableModel);
        listTable.setModel(sorter);
        sorter.setTableHeader(listTable.getTableHeader());
        
        // カラム幅更新
        columnHelper.updateColumnWidth();
        // ストライプテーブル
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(listTable);
        renderer.setDefaultRenderer();
        // ダブルクリックで取得動作
        listTable.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e){
                int count = e.getClickCount();
                if (count == 2) {
                    retrieve();
                }
            }

        });
        // 選択中のstudyを記録するためにlist selection listenerを設定
        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 選択モード
        listTable.setRowSelectionAllowed(true);
        ListSelectionModel m = listTable.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    setCurrentDicomObject();
                }
            }
        });

        // UIに登録
        setUI(panel);

    }

    // PACSに患者のstudyを問い合わせる
    private void find() {

        // 問合わせ
        statusLabel.setText("Start querying.");
        listTableModel.clear();
        useSuffixSearch = Project.getBoolean(MiscSettingPanel.PACS_USE_SUFFIXSEARCH, MiscSettingPanel.DEFAULT_PACS_SUFFIX_SEARCH);

        SwingWorker worker = new SwingWorker<List<DicomObject>, Void>() {

            @Override
            protected List<DicomObject> doInBackground() throws Exception {

                String patientId = getContext().getPatient().getPatientId();
                if (useSuffixSearch) {
                    patientId = "*" + patientId;
                }
                String[] matchingKeys = new String[]{"PatientID", patientId};
                try {
                    List<DicomObject> result = pacsService.findStudy(matchingKeys);
                    return result;
                } catch (Exception e) {
                    processException(e);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    // listTableに結果を設定する
                    List<DicomObject> result = get();
                    if (result != null && !result.isEmpty()) {
                        List<ListDicomObject> newList = new ArrayList<ListDicomObject>();
                        for (DicomObject obj : result) {
                            newList.add(new ListDicomObject(obj));
                        }
                        // 新しいものがトップに来るようにソート
                        Collections.sort(newList, Collections.reverseOrder());
                        listTableModel.setDataProvider(newList);
                    }
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
            }
        };
        worker.execute();
    }

    // PACSから選択しているstudyを取得する
    private void retrieve() {

        setCurrentDicomObject();

        if (currentDicomObject != null) {
            statusLabel.setText("Start retrieving.");
            listModel.clear();
            try {
                pacsService.retrieveDicomObject(currentDicomObject);
            } catch (Exception e) {
                processException(e);
            }
        }
    }

    // 通信障害などのException処理
    private void processException(Exception e) {

        statusLabel.setText("Some Exception occured. :-(");
        String title = ClientContext.getFrameTitle("PacsDicom");
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage());
        sb.append("\n");
        sb.append(e.getCause().toString());
        String msg = sb.toString();
        JOptionPane.showMessageDialog(getContext().getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
    }

    // 取得したDICOM画像を閲覧する
    private void openViewer() {

        if (listModel.isEmpty()) {
            return;
        }
        DicomViewer viewer = new DicomViewer();
        List<DicomImageEntry> list = new ArrayList<>();
        Enumeration<DicomImageEntry> enu = listModel.elements();
        while (enu.hasMoreElements()) {
            list.add(enu.nextElement());
        }
        Collections.sort(list);
        viewer.enter(list);
    }
    
    // Weasisで開く
    private void openWeasisByStudyUID() {
        if (currentDicomObject == null) {
            return;
        }
        String studyUID = currentDicomObject.getString(Tag.StudyInstanceUID);
        StringBuilder sb = new StringBuilder();
        sb.append("studyUID=");
        sb.append(studyUID);
        String param = sb.toString();
        openWeasis(param);
    }
    
    private void openWeasisByPatientId() {
        String patientId = getContext().getPatient().getPatientId();
        StringBuilder sb = new StringBuilder();
        sb.append("patientID=");
        sb.append(patientId);
        String param = sb.toString();
        openWeasis(param);
    }
    
    private void openWeasis(String param) {
        
        if (weasisAddr == null) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(weasisAddr);
        sb.append(param);
        String url = sb.toString();

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                } catch (IOException ex) {
                    //ex.printStackTrace(System.err);
                } catch (URISyntaxException ex) {
                    //ex.printStackTrace(System.err);
                }
            }
        }
    }

    // listTableで現在選択されているstudyを記録する
    private void setCurrentDicomObject() {
        int row = listTable.getSelectedRow();
        ListDicomObject selected = (ListDicomObject) sorter.getObject(row);
        if (selected == null) {
            currentDicomObject = null;
        } else {
            currentDicomObject = selected.getDicomObject();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (executor != null && !executor.isShutdown()) {
            DicomObject obj = (DicomObject) evt.getNewValue();
            executor.execute(new RegisterDicomObject(obj));
        }
    }

    private class RegisterDicomObject implements Runnable {

        private final DicomObject object;

        private RegisterDicomObject(DicomObject object) {
            this.object = object;
        }

        @Override
        public void run() {
            if (currentDicomObject != null) {
                String currentStudyUID = currentDicomObject.getString(Tag.StudyInstanceUID);
                String receivedStudyUID = object.getString(Tag.StudyInstanceUID);
                // studyUIDが違えば何もせず破棄
                if (!currentStudyUID.equals(receivedStudyUID)) {
                    setStatusLabel(new String[]{"Received DicomObject has different studyUID, discarded :", receivedStudyUID});
                    return;
                } else {
                    // studyが一致しても、同じものが既に存在すれば破棄
                    String sopInstanceUID = object.getString(Tag.SOPInstanceUID);
                    Enumeration<DicomImageEntry> enu = listModel.elements();
                    while (enu.hasMoreElements()) {
                        DicomImageEntry entry = enu.nextElement();
                        String test = entry.getTitle();
                        if (test != null && test.equals(sopInstanceUID)) {
                            setStatusLabel(new String[]{"Another image has same sopInstanceUID, discarded :", sopInstanceUID});
                            return;
                        }
                    }
                }
            }
            try {
                addDicomImageEntry(object);
            } catch (IOException ex) {
            }
        }
    }
    
    private void addDicomImageEntry(DicomObject object) throws IOException {
        
        setStatusLabel(new String[]{"Received DicomObeject :", object.getString(Tag.SOPInstanceUID)});
        // ImageEntryを作成する
        DicomImageEntry entry = ImageTool.getImageEntryFromDicom(object);
        entry.setDicomObject(object);
        
        int size = listModel.getSize();
        boolean added = false;
        for (int i = 0; i < size; ++i) {
            DicomImageEntry test = listModel.getElementAt(i);
            if (entry.compareTo(test) < 0) {
                listModel.add(i, entry);
                added = true;
                break;
            }
        }

        if (!added) {
            listModel.addElement(entry);
        }
    }

    // Status Labelに表示
    private void setStatusLabel(String[] msgs){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String str : msgs){
            if (!first){
                sb.append(" ");
            } else {
                first = false;
            }
            sb.append(str);
        }
        statusLabel.setText(sb.toString());
    }

}
