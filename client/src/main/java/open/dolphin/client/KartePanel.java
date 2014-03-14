package open.dolphin.client;

import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.IInfoModel;

/**
 * KartePanelの抽象クラス
 * 改行文字を表示するEditorKitもここにある
 *
 * @author masuda, Masuda Naika
 */
public abstract class KartePanel extends Panel2 {

    public static enum MODE {SINGLE_VIEWER, DOUBLE_VIEWER, SINGLE_EDITOR, DOUBLE_EDITOR};
    
    private static enum DOC_TYPE {OUT_PATIENT, ADMISSION, SELF_INSURANCE, TEMP_KARTE};
    
    private static final Color OUT_PATIENT_COLOR = null;    // new Color(0, 0, 0, 0);
    private static final Color SELF_INSURANCE_COLOR = new Color(255, 236, 103);
    private static final Color ADMISSION_COLOR = new Color(253, 202, 138);
    private static final Color TEMP_KARTE_COLOR = new Color(239, 156, 153);

    // タイムスタンプの foreground カラー
    private static final Color TIMESTAMP_FORE = Color.BLUE;
    // タイムスタンプのフォントサイズ
    private static final int TIMESTAMP_FONT_SIZE = 14;
    // タイムスタンプフォント
    private static final Font TIMESTAMP_FONT = new Font("Dialog", Font.PLAIN, TIMESTAMP_FONT_SIZE);
    private static final int tsHgap = 0;
    private static final int tsVgap = 3;
    // TextPaneの余白
    private static final int topMargin = 5;
    private static final int bottomMargin = 0;
    private static final int leftMargin = 5;
    private static final int rightMargin = 5;
    private static final Insets TEXT_PANE_MARGIN = new Insets(topMargin, leftMargin, bottomMargin, rightMargin);

    //private static final Dimension INITIAL_SIZE = new Dimension(1, 1);

    protected static final int hgap = 2;
    protected static final int vgap = 0;
    protected static final int rows = 1;
    protected static final int cols = 2;

    private JPanel timeStampPanel;
    private JLabel timeStampLabel;
    
    // KarteViewerでは遅延レンダリングする
    private  KarteViewer karteViewer;
    // コンポーネントレンダリング済みフラグ
    private boolean rendered;
    
    private void setKarteViewer(KarteViewer karteViewer) {
        this.karteViewer = karteViewer;
    }
    private void setRendered(boolean rendered) {
        this.rendered = rendered;
    }
    public boolean isRendered() {
        return rendered;
    }
    
    @Override
    protected void paintComponent(Graphics g) {

        if (!rendered && karteViewer != null) {
            karteViewer.renderComponentsOnViewer();
            rendered = true;
            //revalidate();
        }
        super.paintComponent(g);
    }

    // ファクトリー
    public static KartePanel createKartePanel(MODE mode, boolean verticalLayout) {
        
        return createKartePanel(mode, verticalLayout, null);
    }
    
    public static KartePanel createKartePanel(MODE mode, boolean verticalLayout, KarteViewer karteViewer) {

        KartePanel kartePanel;

        switch (mode) {
            case SINGLE_VIEWER:
                kartePanel = new KartePanel1(false);
                break;
            case DOUBLE_VIEWER:
                if (verticalLayout) {
                    kartePanel = new KartePanel2V(false);
                    break;
                } else {
                    kartePanel = new KartePanel2(false);
                    break;
                }
            case SINGLE_EDITOR:
                kartePanel = new KartePanel1(true);
                break;
            case DOUBLE_EDITOR:
                kartePanel = new KartePanel2(true);
                break;
            default:
                return null;
        }
        
        if (karteViewer != null) {
            kartePanel.setKarteViewer(karteViewer);
            kartePanel.setRendered(false);
        } else {
            // KarteViewerでなければlazy renderingしない
            kartePanel.setRendered(true);
        }

        return kartePanel;
    }

    // 抽象メソッド
    protected abstract void initComponents(boolean editor);

    public abstract JTextPane getPTextPane();

    public abstract JTextPane getSoaTextPane();
    
    public abstract boolean isSinglePane();

    public final JLabel getTimeStampLabel() {
        return timeStampLabel;
    }

    protected KartePanel() {
        initCommonComponents();
    }

    private void initCommonComponents() {

        //setPreferredSize(INITIAL_SIZE); // KartePanelが広がりすぎないように
        timeStampLabel = new JLabel();
        timeStampPanel = new JPanel();
        timeStampPanel.setLayout(new FlowLayout(FlowLayout.CENTER, tsHgap, tsVgap));
        timeStampLabel.setForeground(TIMESTAMP_FORE);
        timeStampLabel.setFont(TIMESTAMP_FONT);
        timeStampPanel.add(timeStampLabel);
        timeStampPanel.setOpaque(true);
        setLayout(new BorderLayout());
        add(timeStampPanel, BorderLayout.NORTH);
    }

    // 継承クラスから呼ばれる
    protected final JTextPane createTextPane() {
        JTextPane textPane = new JTextPane();
        textPane.setMargin(TEXT_PANE_MARGIN);
        textPane.setEditorKit(new KartePanelEditorKit());
        // これをセットしないと，勝手に cut copy paste のポップアップがセットされてしまう。 thx to Dr. pns
        textPane.putClientProperty("Quaqua.TextComponent.showPopup", false);
        return textPane;
    }

    protected final int getContainerWidth() {

        Container grandParent = getParent().getParent();
        int width = grandParent instanceof JViewport
                ? grandParent.getWidth()
                : getParent().getWidth();

        return width;
    }

    protected final JPanel getTimeStampPanel() {
        return timeStampPanel;
    }
    
    public void setTitleColor(DocInfoModel docInfo) {
        
        DOC_TYPE docType = DOC_TYPE.OUT_PATIENT;
        AdmissionModel admission = docInfo.getAdmissionModel();
        if (IInfoModel.STATUS_TMP.equals(docInfo.getStatus())) {
            docType = DOC_TYPE.TEMP_KARTE;
        } else if (admission != null) {
            docType = DOC_TYPE.ADMISSION;
        } else if (docInfo.getHealthInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
            docType = DOC_TYPE.SELF_INSURANCE;
        }

        switch (docType) {
            case ADMISSION:
                timeStampPanel.setBackground(ADMISSION_COLOR);
                break;
            case SELF_INSURANCE:
                timeStampPanel.setBackground(SELF_INSURANCE_COLOR);
                break;
            case TEMP_KARTE:
                timeStampPanel.setBackground(TEMP_KARTE_COLOR);
                break;
            case OUT_PATIENT:
            default:
                timeStampPanel.setBackground(OUT_PATIENT_COLOR);
                break;
        }
    }
}
