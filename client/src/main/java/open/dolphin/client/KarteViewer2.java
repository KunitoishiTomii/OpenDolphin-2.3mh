package open.dolphin.client;

import java.awt.Color;
import java.awt.event.MouseListener;
import javax.swing.ActionMap;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;

/**
 * KarteViwer2
 *
 * @author masuda, Masuda Naika
 */
public class KarteViewer2 extends KarteViewer {

    // SOA Pane
    private final KartePane soaPane;
    // P Pane
    private final KartePane pPane;
    
    public KarteViewer2(DocumentModel model, boolean vLayout) {
        super(model);
        soaPane = new KartePane();
        pPane = new KartePane();
        KartePanel kp = vLayout ? new KartePanel2V(false) : new KartePanel2(false);
        setKartePanel(kp);
        setUI(kp);
    }

    private void initialize() {

        KartePanel kartePanel = getKartePanel();
        //kartePanel.initComponents(false);
        kartePanel.setBorder(NOT_SELECTED_BORDER);

        // SOA Pane を生成する
        soaPane.setRole(IInfoModel.ROLE_SOA);
        soaPane.setTextPane(kartePanel.getSoaTextPane());

        // P Pane を生成する
        pPane.setRole(IInfoModel.ROLE_P);
        pPane.setTextPane(kartePanel.getPTextPane());

        // Schema 画像にファイル名を付けるのために必要
        String docId = getModel().getDocInfoModel().getDocId();
        soaPane.setDocId(docId);
        
        // DocumentModelのstatusをKartePaneに保存しておく
        // KarteViewerのpopup制御に利用
        String status = getModel().getDocInfoModel().getStatus();
        soaPane.getTextPane().putClientProperty(GUIConst.PROP_DOC_STATUS, status);
        pPane.getTextPane().putClientProperty(GUIConst.PROP_DOC_STATUS, status);
        
        // KarteViewerをJTextPaneに保存しておく
        soaPane.getTextPane().putClientProperty(GUIConst.PROP_KARTE_VIEWER, this);
        pPane.getTextPane().putClientProperty(GUIConst.PROP_KARTE_VIEWER, this);
    }

    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {
        
        // Creates GUI
        initialize();

        // タイトルを設定する
        setTitle();

        // レンダリングする
        KarteRenderer_2.getInstance().render(getModel(), soaPane, pPane);
        
        // モデル表示後にリスナ等を設定する
        ChartMediator mediator = getContext().getChartMediator();
        soaPane.init(false, mediator);
        pPane.init(false, mediator);
        
    }

    @Override
    public void stop() {
        soaPane.clear();
        pPane.clear();
        // memory leak?
        dispose();
    }

    /**
     * SOA Pane を返す。
     * @return soaPane
     */
    @Override
    public KartePane getSOAPane() {
        return soaPane;
    }

    /**
     * P Pane を返す。
     * @return pPane
     */
    @Override
    public KartePane getPPane() {
        return pPane;
    }

    @Override
    public void addMouseListener(MouseListener ml) {
        soaPane.getTextPane().addMouseListener(ml);
        pPane.getTextPane().addMouseListener(ml);
    }

    @Override
    public void setBackground(Color c) {
        soaPane.getTextPane().setBackground(c);
        pPane.getTextPane().setBackground(c);
    }

    @Override
    public void setParentActionMap(ActionMap amap) {
        soaPane.getTextPane().getActionMap().setParent(amap);
        pPane.getTextPane().getActionMap().setParent(amap);
    }
}
