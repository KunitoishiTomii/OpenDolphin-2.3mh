package open.dolphin.impl.pacsviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.ClientContext;
import open.dolphin.client.ImageEntryJList;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.DicomImageEntry;
import open.dolphin.util.ModifiedFlowLayout;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * DicomViewer.java
 *
 * PACSから取得した画像を閲覧してみる
 *
 * @author masuda, Masuda Naika
 */
public class DicomViewer {

    private static final ImageIcon RESET_ICON
            = ClientContext.getClientContextStub().getImageIcon("edit-undo-4_24.png");
    private static final ImageIcon DRAG_ICON
            = ClientContext.getClientContextStub().getImageIcon("transform-move.png");
    private static final ImageIcon MEASURE_ICON
            = ClientContext.getClientContextStub().getImageIcon("measure.png");
    private static final ImageIcon SELECT_ICON
            = ClientContext.getClientContextStub().getImageIcon("select.png");
    private static final ImageIcon ZOOM_ICON
            = ClientContext.getClientContextStub().getImageIcon("zoom-5.png");
    private static final ImageIcon MOVE_ICON
            = ClientContext.getClientContextStub().getImageIcon("media-playback-start-5.png");
    private static final ImageIcon COPY_ICON
            = ClientContext.getClientContextStub().getImageIcon("edit-copy-2_24.png");
    private static final ImageIcon GAMMA_ICON
            = ClientContext.getClientContextStub().getImageIcon("gamma-24.png");
    private static final ImageIcon BLACK_ICON
            = ClientContext.getClientContextStub().getImageIcon("black-24.png");
    private static final ImageIcon WHITE_ICON
            = ClientContext.getClientContextStub().getImageIcon("white-24.png");

    private static final int MAX_IMAGE_SIZE = 120;

    private JFrame frame;
    private DicomViewerRootPane viewerPane;
    private JScrollPane thumbnailScrollPane;
    private JToggleButton invertBtn;
    private JButton resetBtn;
    private JButton copyBtn;
    private JCheckBox showInfoCb;
    private JToggleButton moveBtn;
    private JToggleButton zoomBtn;
    private JToggleButton dragBtn;
    private JToggleButton measureBtn;
    private JToggleButton selectBtn;
    private JToggleButton gammaBtn;
    private JLabel studyInfoLbl;
    private JLabel statusLbl;
    private JSlider slider;
    private JTextField gammaField;
    private JPanel sliderPanel;

    private ImageEntryJList<DicomImageEntry> thumbnailList;
    private DefaultListModel<DicomImageEntry> thumbnailListModel;

    private int index = 0;
    private final static DecimalFormat frmt = new DecimalFormat("0.0");
    private final static DecimalFormat frmt1 = new DecimalFormat("0.00");
    private final static double gammaStep = 0.01;
    private final static double gammaMin = 0.5;
    private final static double gammaMax = 2.0;
    private final static double gammaDefault = 1.0;

    public DicomViewer() {
        initComponents();
    }

    public JToggleButton getZoomBtn() {
        return zoomBtn;
    }

    public JToggleButton getMoveBtn() {
        return moveBtn;
    }

    public JToggleButton getMeasureBtn() {
        return measureBtn;
    }

    public JToggleButton getSelectBtn() {
        return selectBtn;
    }

    public JToggleButton getDragBtn() {
        return dragBtn;
    }
    
    public JToggleButton getInvertBtn() {
        return invertBtn;
    }

    private void exit() {

        // Frameを閉じるときにPreferrenceに保存する
        Project.setDouble(MiscSettingPanel.PACS_VIEWER_GAMMA, getSliderGamma());
        boolean b = showInfoCb.isSelected();
        Project.setBoolean(MiscSettingPanel.PACS_SHOW_IMAGEINFO, b);

        // memory leak?
        thumbnailListModel.clear();

    }

    private void initComponents() {

        frame = new JFrame();
        ClientContext.setDolphinIcon(frame);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                exit();
            }
        });
        
        // アクション定義
        AbstractAction resetImageAction = new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent e) {
                viewerPane.resetImage();
            }
        };
        AbstractAction copyImageAction = new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent e) {
                copyImage();
            }
        };

        // do not remove copyright!
        String title = ClientContext.getFrameTitle("Dicom Viewer, Masuda Naika");
        frame.setTitle(title);
        frame.setPreferredSize(new Dimension(640, 480));
        viewerPane = new DicomViewerRootPane(this);
        frame.setLayout(new BorderLayout());

        dragBtn = new JToggleButton(DRAG_ICON, true);
        dragBtn.setToolTipText("画像をドラッグして移動させます");
        measureBtn = new JToggleButton(MEASURE_ICON);
        measureBtn.setToolTipText("計測します");
        selectBtn = new JToggleButton(SELECT_ICON);
        selectBtn.setToolTipText("コピーする領域を選択します");
        resetBtn = new JButton();
        resetBtn.setAction(resetImageAction);
        resetBtn.setIcon(RESET_ICON);
        resetBtn.setToolTipText("画像を初期状態に戻します(CTRL+R)");
        copyBtn = new JButton();
        copyBtn.setAction(copyImageAction);
        copyBtn.setIcon(COPY_ICON);
        copyBtn.setToolTipText("選択領域をコピーします(CTRL+C)");
        invertBtn = new JToggleButton(BLACK_ICON);
        invertBtn.setSelectedIcon(WHITE_ICON);
        invertBtn.setToolTipText("色反転します");
        moveBtn = new JToggleButton(MOVE_ICON, true);
        moveBtn.setToolTipText("マウスホイールで前後画像に移動します");
        zoomBtn = new JToggleButton(ZOOM_ICON);
        zoomBtn.setToolTipText("マウスホイールで画像拡大縮小します");
        showInfoCb = new JCheckBox("画像情報");
        statusLbl = new JLabel("OpenDolphin-m");
        studyInfoLbl = new JLabel("Study Info.");
        gammaBtn = new JToggleButton(GAMMA_ICON);
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        gammaBtn.setFont(f);
        gammaBtn.setBorderPainted(true);
        // ガンマ係数スライダの設定
        double d = Project.getDouble(MiscSettingPanel.PACS_VIEWER_GAMMA, MiscSettingPanel.DEFAULT_PACS_GAMMA);
        int sliderMax = (int) ((gammaMax - gammaMin) / gammaStep);
        slider = new JSlider(0, sliderMax);
        JLabel lblSliderLeft = new JLabel(frmt.format(gammaMin));
        JLabel lblSliderRight = new JLabel(frmt.format(gammaMax));
        gammaField = new JTextField(frmt1.format(d));
        gammaField.setEditable(false);
        gammaField.setFocusable(false);
        gammaField.setToolTipText("γボタン有効時クリックで値を変更できます");
        int pos = (int) ((d - gammaMin) / gammaStep);
        slider.setValue(pos);
        viewerPane.setGamma(d);
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
        sliderPanel.add(lblSliderLeft);
        sliderPanel.add(slider);
        sliderPanel.add(lblSliderRight);

        // ボタンのパネル
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new ModifiedFlowLayout(FlowLayout.LEFT));
        JToolBar gammaBar = new JToolBar();
        gammaBar.add(gammaBtn);
        gammaBar.add(gammaField);
        toolPanel.add(gammaBar);
        JToolBar actionBar = new JToolBar();
        actionBar.add(invertBtn);
        actionBar.add(copyBtn);
        actionBar.add(resetBtn);
        actionBar.add(new JToolBar.Separator());
        actionBar.add(showInfoCb);
        toolPanel.add(actionBar);
        JToolBar mouseBar = new JToolBar();
        mouseBar.add(new JLabel("マウス左："));
        mouseBar.add(dragBtn);
        mouseBar.add(measureBtn);
        mouseBar.add(selectBtn);
        mouseBar.add(new JToolBar.Separator());
        mouseBar.add(new JLabel("ホイール："));
        mouseBar.add(moveBtn);
        mouseBar.add(zoomBtn);
        mouseBar.add(new JToolBar.Separator());
        JLabel rtLbl = new JLabel("右：WW / WL");
        rtLbl.setToolTipText("右ドラッグでWindow Width / Levelを変更します");
        mouseBar.add(rtLbl);
        toolPanel.add(mouseBar);

        // ボタングループを設定
        ButtonGroup group = new ButtonGroup();
        group.add(moveBtn);
        group.add(zoomBtn);
        ButtonGroup group2 = new ButtonGroup();
        group2.add(dragBtn);
        group2.add(measureBtn);
        group2.add(selectBtn);

        frame.add(toolPanel, BorderLayout.NORTH);
        
        // ボタン類の設定
        showInfoCb.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = showInfoCb.isSelected();
                viewerPane.setShowInfo(b);
            }
        });
        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                double d = getSliderGamma();
                gammaField.setText(frmt1.format(d));
                viewerPane.setGamma(d);
            }
        });
        gammaBtn.setSelected(true);
        gammaBtn.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (gammaBtn.isSelected()) {
                    double d = getSliderGamma();
                    gammaField.setText(frmt1.format(d));
                    viewerPane.setGamma(d);
                } else {
                    gammaField.setText(frmt1.format(gammaDefault));
                    viewerPane.setGamma(gammaDefault);
                }
            }
        });
        gammaField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (gammaBtn.isSelected()) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(sliderPanel);
                    popup.show(gammaField, 0, 32);
                }
            }
        });
        invertBtn.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                viewerPane.setInverted(invertBtn.isSelected());
            }
        });

        // イメージ表示パネル
        frame.add(viewerPane, BorderLayout.CENTER);

        // サムネイルパネル
        thumbnailListModel = new DefaultListModel();
        thumbnailList = new ImageEntryJList<>(thumbnailListModel, JList.VERTICAL);
        thumbnailList.setMaxIconTextWidth(MAX_IMAGE_SIZE);
        thumbnailList.setFocusable(false);
        thumbnailList.setDragEnabled(false);
        // サムネイル選択で画像表示
        thumbnailList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int idx = thumbnailList.getSelectedIndex();
                    if (idx >= 0) {
                        DicomImageEntry entry = thumbnailListModel.get(idx);
                        showSelectedImage(entry);
                    }
                }
            }
        });
        thumbnailList.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int cnt = e.getWheelRotation();
                if (cnt > 0) {
                    nextImage();
                } else {
                    prevImage();
                }
            }
        });
        // サムネイルはScrollPaneに入れる
        thumbnailScrollPane = new JScrollPane(thumbnailList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        frame.add(thumbnailScrollPane, BorderLayout.WEST);

        // 情報パネル
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(studyInfoLbl);
        panel.add(statusLbl);
        frame.add(panel, BorderLayout.SOUTH);
        
        // ショートカット　CTRL+C でコピー
        panel.getActionMap();
        String optionMapKey = "copyImage";
        KeyStroke ksc = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksc, optionMapKey);
        panel.getActionMap().put(optionMapKey, copyImageAction);
        
        // CTRL+R でリセット
        optionMapKey = "resetImage";
        ksc = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK);
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksc, optionMapKey);
        panel.getActionMap().put(optionMapKey, resetImageAction);

        // preferrence設定
        boolean b = Project.getBoolean(MiscSettingPanel.PACS_SHOW_IMAGEINFO, MiscSettingPanel.DEFAULT_PACS_SHOW_IMAGEINFO);
        viewerPane.setShowInfo(b);
        showInfoCb.setSelected(b);
        ComponentMemory cm = new ComponentMemory(frame, new Point(100, 100), frame.getPreferredSize(), DicomViewer.this);
        cm.setToPreferenceBounds();
    }

    private double getSliderGamma() {
        int pos = slider.getValue();
        return gammaStep * pos + gammaMin;
    }

    // クリップボードにコピーする
    private void copyImage() {
        SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                viewerPane.copyImage();
                return null;
            }
        };
        worker.execute();
    }

    // 次の画像を表示
    public void nextImage() {
        if (index < thumbnailListModel.size() - 1) {
            ++index;
            setSelectedIndex();
        }
    }

    // 前の画像を表示
    public void prevImage() {
        if (index > 0) {
            --index;
            setSelectedIndex();
        }
    }

    // サムネイルで選択した画像を表示させる
    private void setSelectedIndex() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                thumbnailList.setSelectedIndex(index);
                Rectangle r = thumbnailList.getCellBounds(index, index);
                thumbnailList.scrollRectToVisible(r);
            }
        });
    }

    // 入口
    public void enter(List<DicomImageEntry> list) {

        for (DicomImageEntry entry : list) {
            thumbnailListModel.addElement(entry);
        }

        index = 0;
        setSelectedIndex();
        frame.setVisible(true);
    }

    // 選択中の画像を設定する
    private void showSelectedImage(DicomImageEntry entry) {

        if (entry == null) {
            return;
        }
        try {

            DicomObject object = entry.getDicomObject();
            boolean isCR = "CR".equals(object.getString(Tag.Modality));
            gammaBtn.setSelected(isCR);
            setStudyInfoLabel(object);
            viewerPane.setDicomObject(object);
        } catch (IOException ex) {
        }
    }

    // study informationを表示する
    private void setStudyInfoLabel(DicomObject object) {
        StringBuilder sb = new StringBuilder();
        sb.append(object.getString(Tag.PatientName));
        sb.append(" / ");
        sb.append(object.getString(Tag.StudyDate));
        sb.append(" / ");
        sb.append(object.getString(Tag.SOPInstanceUID));
        statusLbl.setText(sb.toString());
    }

}
