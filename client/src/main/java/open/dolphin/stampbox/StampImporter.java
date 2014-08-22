package open.dolphin.stampbox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import open.dolphin.client.BlockGlass;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIFactory;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.helper.ProgressMonitorWorker;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.PublishedTreeModel;
import open.dolphin.infomodel.SubscribedTreeModel;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * StampImporter
 *
 * @author Minagawa,Kazushi
 */
public class StampImporter {
    
    private static final String[] COLUMN_NAMES = {
        "名  称", "カテゴリ", "公開者", "説  明", "公開先", "インポート"
    };
    private static final String[] METHOD_NAMES = {
        "name", "category", "partyName", "description", "publishType", "isImported"
    };
    private static final Class[] CLASSES = {
        String.class, String.class, String.class, String.class, String.class, Boolean.class
    };
    private static final int[] COLUMN_WIDTH = {
        120, 90, 170, 270, 40, 40
    };
    private static final Color ODD_COLOR = ClientContext.getColor("color.odd");
    //private static final Color EVEN_COLOR = ClientContext.getColor("color.even");
    private static final Color EVEN_COLOR = ClientContext.getZebraColor();
    private static final ImageIcon WEB_ICON = ClientContext.getImageIconAlias("icon_world_small");
    private static final ImageIcon HOME_ICON = ClientContext.getImageIconAlias("icon_hospital_small");
    private static final ImageIcon FLAG_ICON = ClientContext.getImageIconAlias("icon_flag_blue_small");
    
    private final String title = "スタンプインポート";
    private JFrame frame;
    private BlockGlass blockGlass;
    private JTable browseTable;
    private ListTableModel<PublishedTreeModel> tableModel;
    private JButton importBtn;
    private JButton deleteBtn;
    private JButton cancelBtn;
    private JLabel publicLabel;
    private JLabel localLabel;
    private JLabel importedLabel;
    
    private final StampBoxPlugin stampBox;
    private final List<Long> importedTreeList;

    
    public StampImporter(StampBoxPlugin stampBox) {
        this.stampBox = stampBox;
        importedTreeList = stampBox.getImportedTreeList();
    }

    /**
     * 公開されているTreeのリストを取得しテーブルへ表示する。
     */
    public void start() {
        
        String message = "スタンプインポート";
        String note = "公開スタンプを取得しています...";
        Component c = frame;
        
        ProgressMonitorWorker worker = new ProgressMonitorWorker<List<PublishedTreeModel>, Void>(c, message, note) {

            @Override
            protected List<PublishedTreeModel> doInBackground() throws Exception {

                StampDelegater sdl = StampDelegater.getInstance();
                List<PublishedTreeModel> result = sdl.getPublishedTrees();
                return result;
            }

            @Override
            protected void succeeded(List<PublishedTreeModel> result) {
                // DBから取得が成功したらGUIコンポーネントを生成する
                initComponent();
                if (importedTreeList != null && importedTreeList.size() > 0) {
                    for (PublishedTreeModel model : result) {
                        for (Long id : importedTreeList) {
                            if (id.longValue() == model.getId()) {
                                model.setImported(true);
                                break;
                            }
                        }
                    }
                }
                tableModel.setDataProvider(result);
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                JOptionPane.showMessageDialog(frame,
                            cause.getMessage(),
                            ClientContext.getFrameTitle(title),
                            JOptionPane.WARNING_MESSAGE);
                ClientContext.getBootLogger().warn(cause.getMessage());
            }

        };

        worker.execute();
    }
    
    /**
     * GUIコンポーネントを初期化する。
     */
    public void initComponent() {
        
        frame = new JFrame(ClientContext.getFrameTitle(title));
//masuda^    アイコン設定
        ClientContext.setDolphinIcon(frame);
//masuda$
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
        
        JPanel contentPane = createBrowsePane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
        
        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        frame.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int n = ClientContext.isMac() ? 3 : 2;
        int x = (screen.width - frame.getPreferredSize().width) / 2;
        int y = (screen.height - frame.getPreferredSize().height) / n;
        frame.setLocation(x, y);

        blockGlass = new BlockGlass();
        frame.setGlassPane(blockGlass);

        frame.setVisible(true);
    }
    
    /**
     * 終了する。
     */
    public void stop() {
        frame.setVisible(false);
        frame.dispose();
    }
    
    /**
     * 公開スタンプブラウズペインを生成する。
     */
    private JPanel createBrowsePane() {
        
        JPanel browsePane = new JPanel();

        tableModel = new ListTableModel<>(COLUMN_NAMES, 10, METHOD_NAMES, CLASSES);
        browseTable = new JTable(tableModel);
        for (int i = 0; i < COLUMN_WIDTH.length; i++) {
            browseTable.getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTH[i]);
        }
//masuda^   ストライプテーブル
        //browseTable.setDefaultRenderer(Object.class, new OddEvenRowRenderer());
        StripeTableCellRenderer renderer = new StripeTableCellRenderer(browseTable);
        renderer.setDefaultRenderer();
 //masuda$
        
        importBtn = new JButton("インポート");
        importBtn.setEnabled(false);
        cancelBtn = new JButton("閉じる");
        deleteBtn = new JButton("削除");
        deleteBtn.setEnabled(false);
        publicLabel = new JLabel("グローバル", WEB_ICON, SwingConstants.CENTER);
        localLabel = new JLabel("院内", HOME_ICON, SwingConstants.CENTER);
        importedLabel = new JLabel("インポート済", FLAG_ICON, SwingConstants.CENTER);

        JScrollPane tableScroller = new JScrollPane(browseTable);
        tableScroller.getViewport().setPreferredSize(new Dimension(730, 380));
        
        // レイアウトする
        browsePane.setLayout(new BorderLayout(0, 17));
        JPanel flagPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 5));
        flagPanel.add(localLabel);
        flagPanel.add(publicLabel);
        flagPanel.add(importedLabel);
        JPanel cmdPanel = GUIFactory.createCommandButtonPanel(new JButton[]{cancelBtn, deleteBtn, importBtn});
        browsePane.add(flagPanel, BorderLayout.NORTH);
        browsePane.add(tableScroller, BorderLayout.CENTER);
        browsePane.add(cmdPanel, BorderLayout.SOUTH);
        
        // レンダラを設定する
        PublishTypeRenderer pubTypeRenderer = new PublishTypeRenderer();
        browseTable.getColumnModel().getColumn(4).setCellRenderer(pubTypeRenderer);
        ImportedRenderer importedRenderer = new ImportedRenderer();
        browseTable.getColumnModel().getColumn(5).setCellRenderer(importedRenderer);
        
        // BrowseTableをシングルセレクションにする
        browseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel sleModel = browseTable.getSelectionModel();
        sleModel.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    int row = browseTable.getSelectedRow();
                    PublishedTreeModel model = tableModel.getObject(row);
                    if (model != null) {
                        if (model.isImported()) {
                            importBtn.setEnabled(false);
                            deleteBtn.setEnabled(true);
                        } else {
                            importBtn.setEnabled(true);
                            deleteBtn.setEnabled(false);
                        }
                    } else {
                        importBtn.setEnabled(false);
                        deleteBtn.setEnabled(false);
                    }
                }
            }
        });

        // import
        importBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                importPublishedTree();
            }
        });

        // remove
        deleteBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeImportedTree();
            }
        });

        // キャンセル
        cancelBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        
        return browsePane;
    }
    
    /**
     * ブラウザテーブルで選択した公開Treeをインポートする。
     */
    public void importPublishedTree() {

        // テーブルはシングルセレクションである
        int row = browseTable.getSelectedRow();
        final PublishedTreeModel importTree = tableModel.getObject(row);

        if (importTree == null) {
            return;
        }

        // Import 済みの場合
        if (importTree.isImported()) {
            return;
        }

        importTree.setTreeXml(new String(importTree.getTreeBytes(), StandardCharsets.UTF_8));

        // サブスクライブリストに追加する
        SubscribedTreeModel sm = new SubscribedTreeModel();
        sm.setUserModel(Project.getUserModel());
        sm.setTreeId(importTree.getId());
        final List<SubscribedTreeModel> subscribeList = new ArrayList<>(1);
        subscribeList.add(sm);

        String message = "スタンプインポート";
        String note = "インポートしています...";
        Component c = frame;
        
        ProgressMonitorWorker worker = new ProgressMonitorWorker<Void, Void>(c, message, note) {

            @Override
            protected Void doInBackground() throws Exception {

                StampDelegater sdl = StampDelegater.getInstance();
                sdl.subscribeTrees(subscribeList);
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                // スタンプボックスへインポートする
                stampBox.importPublishedTree(importTree);
                // Browser表示をインポート済みにする
                importTree.setImported(true);
                tableModel.fireTableDataChanged();
            }

            @Override
            protected void cancelled() {
                ClientContext.getBootLogger().debug("Task cancelled");
            }

            @Override
            protected void failed(java.lang.Throwable cause) {
                JOptionPane.showMessageDialog(frame,
                        cause.getMessage(),
                        ClientContext.getFrameTitle(title),
                        JOptionPane.WARNING_MESSAGE);
                ClientContext.getBootLogger().warn(cause.getMessage());
            }
        };
        worker.execute();
    }
    
    /**
     * インポートしているスタンプを削除する。
     */
    public void removeImportedTree() {

        // 削除するTreeを取得する
        int row = browseTable.getSelectedRow();
        final PublishedTreeModel removeTree = tableModel.getObject(row);
        
        if (removeTree == null) {
            return;
        }

        SubscribedTreeModel sm = new SubscribedTreeModel();
        sm.setTreeId(removeTree.getId());
        sm.setUserModel(Project.getUserModel());
        final List<SubscribedTreeModel> list = new ArrayList<>(1);
        list.add(sm);
        
        // Unsubscribeタスクを実行する
        String message = "スタンプインポート";
        String note = "インポート済みスタンプを削除しています...";
        Component c = frame;
        
        ProgressMonitorWorker worker = new ProgressMonitorWorker<Void, Void>(c, message, note) {
  
            @Override
            protected Void doInBackground() throws Exception {
                
                StampDelegater sdl = StampDelegater.getInstance();
                sdl.unsubscribeTrees(list);
                return null;
            }
            
            @Override
            protected void succeeded(Void result) {
                // スタンプボックスから削除する
                stampBox.removeImportedTree(removeTree.getId());
                // ブラウザ表示を変更する
                removeTree.setImported(false);
                tableModel.fireTableDataChanged();
            }
            
            @Override
            protected void cancelled() {
                ClientContext.getBootLogger().debug("Task cancelled");
            }
            
            @Override
            protected void failed(java.lang.Throwable cause) {
                JOptionPane.showMessageDialog(frame,
                            cause.getMessage(),
                            ClientContext.getFrameTitle(title),
                            JOptionPane.WARNING_MESSAGE);
                ClientContext.getBootLogger().warn(cause.getMessage());
            }
        };

        worker.execute();
    }
        
    class PublishTypeRenderer extends DefaultTableCellRenderer {
        
        /** Creates new IconRenderer */
        public PublishTypeRenderer() {
            super();
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setForeground(table.getForeground());
                if (row % 2 == 0) {
                    setBackground(EVEN_COLOR);
                } else {
                    setBackground(ODD_COLOR);
                }
            }
            
            if (value != null && value instanceof String) {
                
                String pubType = (String) value;
                
                if (pubType.equals(IInfoModel.PUBLISHED_TYPE_GLOBAL)) {
                    setIcon(WEB_ICON);
                } else {
                    setIcon(HOME_ICON);
                } 
                this.setText("");
                
            } else {
                setIcon(null);
                this.setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }
    
    class ImportedRenderer extends DefaultTableCellRenderer {
        
        /** Creates new IconRenderer */
        public ImportedRenderer() {
            super();
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {           
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setForeground(table.getForeground());
                if (row % 2 == 0) {
                    setBackground(EVEN_COLOR);
                } else {
                    setBackground(ODD_COLOR);
                }
            }
            
            if (value != null && value instanceof Boolean) {
                
                Boolean imported = (Boolean) value;
                
                if (imported.booleanValue()) {
                    this.setIcon(FLAG_ICON);
                } else {
                    this.setIcon(null);
                }
                this.setText("");
                
            } else {
                setIcon(null);
                this.setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }
}