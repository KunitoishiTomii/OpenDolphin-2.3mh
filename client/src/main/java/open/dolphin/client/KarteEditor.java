package open.dolphin.client;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.List;
import java.util.TooManyListenersException;
import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.tr.PTransferHandler;
import open.dolphin.tr.SOATransferHandler;
import open.dolphin.util.MMLDate;

/**
 * 2号カルテクラス。
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class KarteEditor extends AbstractChartDocument implements IInfoModel,
        NChartDocument, UndoableEditListener {

    // シングルモード
    public static final int SINGLE_MODE = 1;
    // ２号カルテモード
    public static final int DOUBLE_MODE = 2;

    // TimeStamp のカラー
    private static final Color TIMESTAMP_FORE = Color.BLUE;
    private static final int TIMESTAMP_FONT_SIZE = 14;
    private static final Font TIMESTAMP_FONT = new Font("Dialog", Font.PLAIN, TIMESTAMP_FONT_SIZE);
    private static final String DEFAULT_TITLE = "経過記録";
    private static final String UPDATE_TAB_TITLE = "更新";
    private static final String TEMP_KARTE_TITLE = "仮保存中";

    // このエディタのモード
    private int mode = 2;
    // このエディタのモデル
    private DocumentModel model;
    // このエディタを構成するコンポーネント
    private JLabel timeStampLabel;
    // Timestamp
    private String timeStamp;
    // 開始時間（カルテオープン）
    private Date started;
    // 健康保険Box
    private boolean insuranceVisible;
    // SOA Pane
    private KartePane soaPane;
    // P Pane
    private KartePane pPane;
    // 2号カルテ JPanel
    private KartePanel kartePanel;

    // タイムスタンプの foreground
    private final Color timeStampFore = TIMESTAMP_FORE;
    // タイムスタンプフォント
    private final Font timeStampFont = TIMESTAMP_FONT;

    // 編集可能かどうかのフラグ。このフラグで KartePane を初期化する
    private boolean editable;
    // 修正時に true
    private boolean modify;

    // CLAIM 送信リスナ
    private ClaimMessageListener claimListener;
    // MML送信リスナ
    private MmlMessageListener mmlListener;

    // State Manager
    private final StateMgr stateMgr;

    // Undo and Redo, ChartMediatorから引っ越し
    private final UndoManager undoManager;
    private Action undoAction;
    private Action redoAction;

    // EditorFrame に save 完了を知らせる
    public static final String SAVE_DONE = "saveDoneProp";
    private PropertyChangeSupport boundSupport;

    // カルテ記載最低文字数
    private static final int MinimalKarteLength = 5;

    /**
     * Creates new KarteEditor
     */
    public KarteEditor() {
        setTitle(DEFAULT_TITLE);
        stateMgr = new StateMgr();
        undoManager = new UndoManager();
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * DocumentModelを返す。
     *
     * @return DocumentModel
     */
    public DocumentModel getModel() {
        return model;
    }

    /**
     * DocumentModelを設定する。
     *
     * @param model DocumentModel
     */
    public void setModel(DocumentModel model) {
        this.model = model;
    }

    /**
     * SOAPaneを返す。
     *
     * @return SOAPane
     */
    public KartePane getSOAPane() {
        return soaPane;
    }

    /**
     * PPaneを返す。
     *
     * @return PPane
     */
    public KartePane getPPane() {
        return pPane;
    }

    /**
     * 編集可能属性を設定する。
     *
     * @param b 編集可能な時true
     */
    public void setEditable(boolean b) {
        editable = b;
    }

    /**
     * 修正属性を設定する。
     *
     * @param b 修正する時true
     */
    public void setModify(boolean b) {
        modify = b;
    }
    
    /**
     * MMLリスナを追加する。
     *
     * @param listener MMLリスナリスナ
     */
    public void addMMLListner(MmlMessageListener listener) throws TooManyListenersException {
        if (mmlListener != null) {
            throw new TooManyListenersException();
        }
        mmlListener = listener;
    }

    /**
     * MMLリスナを削除する。
     *
     * @param listener MMLリスナリスナ
     */
    public void removeMMLListener(MmlMessageListener listener) {
        if (mmlListener != null && mmlListener == listener) {
            mmlListener = null;
        }
    }

    /**
     * CLAIMリスナを追加する。
     *
     * @param listener CLAIMリスナ
     * @throws TooManyListenersException
     */
    public void addCLAIMListner(ClaimMessageListener listener)
            throws TooManyListenersException {
        if (claimListener != null) {
            throw new TooManyListenersException();
        }
        claimListener = listener;
    }

    /**
     * CLAIMリスナを削除する。
     *
     * @param listener 削除するCLAIMリスナ
     */
    public void removeCLAIMListener(ClaimMessageListener listener) {
        if (claimListener != null && claimListener == listener) {
            claimListener = null;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(SAVE_DONE, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport != null) {
            boundSupport.removePropertyChangeListener(SAVE_DONE, listener);
        }
    }

    // save が終了したことを EditorFrame に知らせる(KarteDocumentSaverから)
    public void fireFinishSave() {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteEditor.SAVE_DONE, false, true);
        }
    }

    public void printPanel2(final PageFormat format) {
        String name = getContext().getPatient().getFullName();
        boolean printName = true;
        if (mode == SINGLE_MODE) {
            printName = printName && Project.getBoolean("plain.print.patinet.name");
        }
        // 印刷前にダイアログを表示
        kartePanel.printPanel(format, 1, true, name, kartePanel.getPreferredSize().height + 60, printName);
    }

    public void printPanel2(final PageFormat format, final int copies, final boolean useDialog) {
        String name = getContext().getPatient().getFullName();
        boolean printName = true;
        if (mode == SINGLE_MODE) {
            printName = printName && Project.getBoolean("plain.print.patinet.name");
        }
        kartePanel.printPanel(format, copies, useDialog, name, kartePanel.getPreferredSize().height + 60, printName);
    }

    @Override
    public void print() {
        PageFormat pageFormat = getContext().getContext().getPageFormat();
        this.printPanel2(pageFormat);
    }

    @Override
    public void enter() {
        super.enter();
        stateMgr.controlMenu();
    }

    @Override
    public void setDirty(boolean dirty) {
        // 外部からのdirty設定はforceDirtyとする
        stateMgr.setForceDirty(dirty);
    }

    @Override
    public boolean isDirty() {
        return stateMgr.isDirty();
    }

    @Override
    public void start() {

        if (getMode() == SINGLE_MODE) {
            start1();
        } else if (getMode() == DOUBLE_MODE) {
            start2();
        }
    }

    @Override
    public void stop() {
    }

// ↓ reflections?
    public void insertImage() {
        JFileChooser chooser = new JFileChooser();
        int selected = chooser.showOpenDialog(getContext().getFrame());
        if (selected == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getPath();
            this.getSOAPane().insertImage(path);

        } else if (selected == JFileChooser.CANCEL_OPTION) {
        }
    }

    /**
     * 処方日数を一括変更する。
     */
    public void changeNumOfDatesAll() {

        if (getPPane() == null || !editable || !getPPane().hasRP()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        PropertyChangeListener pcl = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                int number = ((Integer) pce.getNewValue()).intValue();
                if (number > 0) {
                    getPPane().changeAllRPNumDates(number);
                }
            }
        };

        ChangeNumDatesDialog dialog = new ChangeNumDatesDialog(getContext().getFrame(), pcl);
        dialog.show();
    }

    /**
     * Chart画面で保険選択が行われた時にコールされる。
     *
     * @param hm 選択された保険情報
     */
    public void applyInsurance(PVTHealthInsuranceModel hm) {

        getModel().getDocInfoModel().setHealthInsurance(hm.getInsuranceClassCode());
        getModel().getDocInfoModel().setHealthInsuranceDesc(hm.toString());
        getModel().getDocInfoModel().setHealthInsuranceGUID(hm.getGUID());  // GUID
        this.setDirty(true);

        setInsuranceVisible(true);
    }
// ↑ reflections?

    /**
     * 初期化する。
     */
    public void initialize() {

        if (getMode() == SINGLE_MODE) {
            initialize1();
        } else if (getMode() == DOUBLE_MODE) {
            initialize2();
        }

        // setup undo and redo actions
        undoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_UNDO);
        undoAction.setEnabled(false);
        redoAction = getContext().getChartMediator().getAction(GUIConst.ACTION_REDO);
        redoAction.setEnabled(false);
    }

    /**
     * シングルモードで初期化する。
     */
    private void initialize1() {

        kartePanel = KartePanel.createKartePanel(KartePanel.MODE.SINGLE_EDITOR, false);

        // TimeStampLabel を生成する
        timeStampLabel = kartePanel.getTimeStampLabel();
        timeStampLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeStampLabel.setForeground(timeStampFore);
        timeStampLabel.setFont(timeStampFont);

        // SOA Pane を生成する
        soaPane = new KartePane();
        soaPane.setTextPane(kartePanel.getSoaTextPane());
        soaPane.setParent(this);
        soaPane.setRole(ROLE_SOA);
        soaPane.getTextPane().setTransferHandler(SOATransferHandler.getInstance());

        if (model != null) {
            // Schema 画像にファイル名を付けるのために必要
            String docId = model.getDocInfoModel().getDocId();
            soaPane.setDocId(docId);
        }

        JScrollPane scroller = new JScrollPane(kartePanel);
        getUI().setLayout(new BorderLayout());
        getUI().add(scroller, BorderLayout.CENTER);

        // Model を表示する
        displayModel();
    }

    /**
     * 2号カルテモードで初期化する。
     */
    private void initialize2() {

        kartePanel = KartePanel.createKartePanel(KartePanel.MODE.DOUBLE_EDITOR, false);

        // TimeStampLabel を生成する
        timeStampLabel = kartePanel.getTimeStampLabel();
        timeStampLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeStampLabel.setForeground(timeStampFore);
        timeStampLabel.setFont(timeStampFont);

        // SOA Pane を生成する
        soaPane = new KartePane();
        soaPane.setTextPane(kartePanel.getSoaTextPane());
        soaPane.setParent(this);
        soaPane.setRole(ROLE_SOA);

        soaPane.getTextPane().setTransferHandler(SOATransferHandler.getInstance());
        if (model != null) {
            // Schema 画像にファイル名を付けるのために必要
            String docId = model.getDocInfoModel().getDocId();
            soaPane.setDocId(docId);
        }

        // P Pane を生成する
        pPane = new KartePane();
        pPane.setTextPane(kartePanel.getPTextPane());
        pPane.setParent(this);
        pPane.setRole(ROLE_P);
        pPane.getTextPane().setTransferHandler(PTransferHandler.getInstance());

        setUI(kartePanel);

        // Model を表示する
        displayModel();
    }

    /**
     * シングルモードを開始する。初期化の後コールされる。
     */
    private void start1() {
        // モデル表示後にリスナ等を設定する
        ChartMediator mediator = getContext().getChartMediator();
        soaPane.init(editable, mediator);
        enter();
    }

    /**
     * ２号カルテモードを開始する。初期化の後コールされる。
     */
    private void start2() {
        // モデル表示後にリスナ等を設定する
        ChartMediator mediator = getContext().getChartMediator();
        soaPane.init(editable, mediator);
        pPane.init(editable, mediator);
        enter();
    }

    /**
     * DocumentModelを表示する。
     */
    private void displayModel() {

        // Timestamp を表示する
        started = new Date();

        DocInfoModel docInfo = model.getDocInfoModel();

        StringBuilder sb = new StringBuilder();
        sb.append(ModelUtils.getDateAsFormatString(started, IInfoModel.KARTE_DATE_FORMAT));

        // 入院の場合は病室・入院科を表示する
        AdmissionModel admission = docInfo.getAdmissionModel();
        if (admission != null) {
            sb.append("<");
            sb.append(admission.getRoom()).append("号室:");
            sb.append(admission.getDepartment());
            sb.append(">");
        }
        timeStamp = sb.toString();

        // 修正の場合
        if (modify) {
            // 更新: YYYY-MM-DDTHH:MM:SS (firstConfirmDate)
            sb = new StringBuilder();
            // 仮保存の場合はタイトル変更
            if (IInfoModel.STATUS_TMP.equals(docInfo.getStatus())) {
                sb.append(TEMP_KARTE_TITLE);
            } else {
                sb.append(UPDATE_TAB_TITLE);
            }

            sb.append(": ");
            sb.append(timeStamp);
            sb.append(" [");
            sb.append(ModelUtils.getDateAsFormatString(docInfo.getFirstConfirmDate(), IInfoModel.KARTE_DATE_FORMAT));
            sb.append(" ]");
            timeStamp = sb.toString();
        }

        // 内容を表示する
        if (model.getModules() != null) {
            KarteRenderer_2.getInstance().render(model, soaPane, pPane);
            soaPane.setCaretPositionLast();
            if (pPane != null) {
                pPane.setCaretPositionLast();
            }
        } else {
            // 新規の場合ここでKarteStyledDocumentを設定する。
            // off screen renderingのため
            soaPane.initKarteStyledDocument();
            if (pPane != null) {
                pPane.initKarteStyledDocument();
            }
        }

        // 健康保険を表示する
        PVTHealthInsuranceModel[] ins = null;

        // 患者が保有する全ての保険情報を配列へ格納する
        // コンテキストが EditotFrame の場合と Chart の場合がある
        if (getContext() instanceof ChartImpl) {
            ins = ((ChartImpl) getContext()).getHealthInsurances();
        } else if (getContext() instanceof EditorFrame) {
            EditorFrame ef = (EditorFrame) getContext();
            ChartImpl chart = (ChartImpl) ef.getChart();
            ins = chart.getHealthInsurances();
        }

        // Model に設定してある健康保険を選択する
        // (カルテを作成する場合にダイアログで保険を選択している）
        // 選択した保険のGUIDと一致するものを配列から見つけ、表示する
        String selecteIns = null;
        String insGUID = docInfo.getHealthInsuranceGUID();
        if (insGUID != null) {
            ClientContext.getBootLogger().debug("insGUID = " + insGUID);
            for (PVTHealthInsuranceModel insModel : ins) {
                String GUID = insModel.getGUID();
                if (GUID != null && GUID.equals(insGUID)) {
                    selecteIns = insModel.toString();
                    ClientContext.getBootLogger().debug("found ins = " + selecteIns);
                    break;
                }
            }
        } else {
            ClientContext.getBootLogger().debug("insGUID is null");
        }

        sb = new StringBuilder();
        sb.append(timeStamp);
        if ((getMode() == DOUBLE_MODE) && (selecteIns != null)) {
            sb.append(" (").append(selecteIns).append(")");
        }

        timeStampLabel.setText(sb.toString());
        timeStampLabel.addMouseListener(new InsVisualizationListener());

        // タイトルを文書種別によって色分けする
        kartePanel.setTitleColor(docInfo);
    }

    // 保険を表示するか否か
    private final class InsVisualizationListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {

            if (e.getClickCount() == 1) {
                setInsuranceVisible(!insuranceVisible);
            }
            e.consume();
        }
    }

    private void setInsuranceVisible(boolean visible) {

        StringBuilder sb = new StringBuilder();
        sb.append(timeStamp);

        if (visible) {
            sb.append(" (");
            sb.append(getModel().getDocInfoModel().getHealthInsuranceDesc());
            sb.append(")");
        }

        timeStampLabel.setText(sb.toString());
        timeStampLabel.repaint();
        insuranceVisible = visible;
    }

    @Override
    public void save() {

        // 何も変更がないときはリターンする
        if (!stateMgr.isDirty()) {
            return;
        }

        // 薬剤相互作用チェックなど
        if (!isKarteCheckOK()) {
            return;
        }
        
        // この段階での CLAIM 送信 = 診療行為送信かつclaimListener!=null
        boolean sendClaim = getContext().isSendClaim();
        boolean sendLabtest = getContext().isSendLabtest();
        
        // MML送信用のマスタIDを取得する
        // ケース１ HANIWA 方式 facilityID + patientID
        // ケース２ HIGO 方式 地域ID を使用
        ID masterID = Project.getMasterId(getContext().getPatient().getPatientId());
        // 地域連携に参加する場合のみに変更する
        boolean sendMml = Project.getBoolean(Project.SEND_MML)
                && Project.getBoolean(Project.JOIN_AREA_NETWORK)
                && masterID != null
                && mmlListener != null;

        // 保存ダイアログを表示し、パラメータを得る
        // 地域連携に参加もしくはMML送信を行う場合は患者及び診療歴のある施設への参照許可
        // パラメータが設定できるようにする
        SaveParams params = getSaveParams(sendClaim, sendLabtest, sendMml);

        // キャンセルの場合はリターンする
        if (params != null) {
            // 次のステージを実行する
            KarteDocumentSaver saver = new KarteDocumentSaver(this);
            saver.saveAndSend(params);
        }
    }

    /**
     * 保存ダイアログを表示し保存時のパラメータを取得する。
     *
     * @params sendMML MML送信フラグ 送信するとき true
     */
    private SaveParams getSaveParams(boolean sendClaim, boolean sendLabtest, boolean sendMML) {

        // カルテ編集の場合は新しいtop15と編集元のタイトルを選べるようにする
        final boolean useTop15 = Project.getBoolean("useTop15AsTitle", true);
        final String defaultTitle = Project.getString("defaultKarteTitle", DEFAULT_TITLE);

        // 編集元のタイトルを取得
        String oldTitle = model.getDocInfoModel().getTitle();

        // 新しいタイトルを設定する
        String text = useTop15 ? soaPane.getTitle() : defaultTitle;     // newTitle
        if (text == null || "".equals(text)) {
            text = DEFAULT_TITLE;
        }

        //-------------------------------
        // 新規カルテで保存の場合
        // 仮保存から修正がかかっている場合
        // 修正の場合
        //-------------------------------
        DocInfoModel docInfo = getModel().getDocInfoModel();

        if (!modify && IInfoModel.STATUS_NONE.equals(docInfo.getStatus())) {
            ClientContext.getBootLogger().debug("saveFromNew");
            sendClaim &= Project.getBoolean(Project.SEND_CLAIM_SAVE);

        } else if (modify && IInfoModel.STATUS_TMP.equals(docInfo.getStatus())) {
            ClientContext.getBootLogger().debug("saveFromTmp");
            sendClaim &= Project.getBoolean(Project.SEND_CLAIM_TMP);

        } else if (modify) {
            ClientContext.getBootLogger().debug("saveFromModify");
            sendClaim &= Project.getBoolean(Project.SEND_CLAIM_MODIFY);
            // 修正保存の場合
            sendLabtest = false;
        }

        SaveParams params = new SaveParams();
        // 保存時に確認ダイアログを表示するかどうか
        if (Project.getBoolean(Project.KARTE_SHOW_CONFIRM_AT_SAVE)) {

            params.setTitle(text);
            params.setDepartment(model.getDocInfoModel().getDepartmentDesc());

            // 旧タイトルを設定
            params.setOldTitle(oldTitle);
            // 確定日変更可能かどうか　新規カルテかベースが仮カルテ
            String status = docInfo.getStatus();
            String ver = docInfo.getVersionNumber();
            boolean newKarte = !modify && IInfoModel.STATUS_NONE.equals(status);
            boolean editTemp = modify
                    && IInfoModel.STATUS_TMP.equals(status)
                    && "2.0".compareTo(ver) >= 0;   // うまくない
            params.setDateEditable(newKarte || editTemp);
            // FirstConfirmDateを設定する
            //Date firstConfirmed = model.getDocInfoModel().getFirstConfirmDate();
            //if (firstConfirmed != null) {
            //    params.setConfirmed(firstConfirmed);
            //}
            // 入院中か
            AdmissionModel admission = model.getDocInfoModel().getAdmissionModel();
            params.setInHospital(admission != null);

            // 印刷枚数をPreferenceから取得する
            int numPrint = Project.getInt("karte.print.count", 0);
            params.setPrintCount(numPrint);

            //-----------------------------
            // Single Mode の時は送信なし
            //-----------------------------
            params.setSendEnabled(getMode() != SINGLE_MODE);

            //-----------------------------
            // CLAIM 送信
            // 保存ダイアログで変更する事が可能
            //-----------------------------
            params.setSendClaim(sendClaim);

            //-----------------------------
            // Labtest 送信
            //-----------------------------
            params.setSendLabtest(sendLabtest);
            if (getMode() == DOUBLE_MODE && pPane != null) {
                params.setHasLabtest(pPane.hasLabtest());
            }
            // MML
            params.setSendMML(sendMML);

            // 保存ダイアログを表示する
            Window parent = SwingUtilities.getWindowAncestor(this.getUI());
            SaveDialog sd = new SaveDialog(parent);
            params.setAllowPatientRef(false);    // 患者の参照
            params.setAllowClinicRef(false);     // 診療履歴のある医療機関
            sd.setValue(params);
            sd.start();                          // showDaialog
            params = sd.getValue();

            // 印刷枚数を保存する
            if (params != null) {
                Project.setInt("karte.print.count", params.getPrintCount());
            }

        } else {
            //-----------------------------
            // 確認ダイアログを表示しない
            //-----------------------------
            params.setTitle(text);
            params.setDepartment(model.getDocInfoModel().getDepartmentDesc());
            params.setPrintCount(Project.getInt(Project.KARTE_PRINT_COUNT, 0));
            // 旧タイトルを設定
            params.setOldTitle(oldTitle);

            // 仮保存が指定されている端末の場合
            int sMode = Project.getInt(Project.KARTE_SAVE_ACTION);
            boolean tmpSave = (sMode == 1);
            params.setTmpSave(tmpSave);
            if (tmpSave) {
                params.setSendClaim(false);
                params.setSendLabtest(false);
            } else {
                // 保存が実行される端末の場合
                params.setSendClaim(sendClaim);
                params.setSendLabtest(sendLabtest);
            }

            // 患者参照、施設参照不可
            params.setAllowClinicRef(false);
            params.setAllowPatientRef(false);

            // MML
            params.setSendMML(sendMML);
        }

        return params;
    }

    // カルテ内容をチェックする
    private boolean isKarteCheckOK() {

        if (getMode() == DOUBLE_MODE) {

            // 薬剤相互作用チェック
            DocInfoModel docInfo = model.getDocInfoModel();
            AdmissionModel admission = docInfo.getAdmissionModel();
            boolean inHospital = admission != null;
            Chart context = getContext();

            // KartePaneからModuleModelを取得する
            List<ModuleModel> stamps = pPane.getDocument().getStamps();

            // 禁忌がないか、禁忌あるが無視のときはfalseが帰ってくる
            CheckMedication ci = new CheckMedication();
            if (ci.checkStart(context, stamps)) {
                return false;
            }
            
            // 診療行為重複チェック
            CheckDuplication cd = new CheckDuplication();
            if (cd.checkStart(context, stamps)) {
                return false;
            }

            // 入院の場合の追加チェック
            if (inHospital) {
                CheckAdmission ca = new CheckAdmission();
                if (ca.checkStart(context, stamps)) {
                    return false;
                }
            }

            // 算定チェック　自費・入院の場合は算定チェックしない
            boolean check = Project.getBoolean(MiscSettingPanel.SANTEI_CHECK, true);
            boolean selfIns = docInfo.getHealthInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX);
            if (check && !inHospital && !selfIns) {
                try {
                    String text = soaPane.getTextPane().getText();
                    CheckSantei cs = new CheckSantei();
                    cs.init(context, stamps, docInfo.getFirstConfirmDate(), text);
                    if (cs.checkOnSave()) {
                        // 算定チェックが問題なければfalseで返ってくる masuda
                        return false;
                    }
                } catch (Exception ex) {
                }
            }
        }

        return true;
    }

    // KarteDocumentSaverが終了すると呼ばれる
    public void saveSingleDone(SaveParams params, DocInfoModel savedInfo) {

        // 編集不可に設定する
        soaPane.setEditableProp(false);

        // 状態遷移する
        stateMgr.setSaved(true);

        // 印刷
        Chart chart = getContext();
        int copies = params.getPrintCount();
        if (copies > 0) {
            printPanel2(chart.getContext().getPageFormat(), copies, false);
        }

        // 文書履歴の更新を通知する
        chart.getDocumentHistory().getDocumentHistory();

        // save が終了したことを EditorFrame に知らせる
        fireFinishSave();
    }

    public void saveDoubleDone(SaveParams params, DocInfoModel savedInfo) {

        // 編集不可に設定する
        soaPane.setEditableProp(false);
        pPane.setEditableProp(false);

        // 状態遷移する
        stateMgr.setSaved(true);

        // 外来待合リスト以外から開いた場合はpvt.id = 0である
        Chart chart = getContext();
        PatientVisitModel pvt = chart.getPatientVisit();
        if (params.isSendClaim() && pvt.getId() != 0) {
            // CLAIMビットをセット
            if (modify) {
                pvt.setStateBit(PatientVisitModel.BIT_MODIFY_CLAIM, true);
            } else {
                pvt.setStateBit(PatientVisitModel.BIT_SAVE_CLAIM, true);
            }
        }

        // 今日のカルテをセーブした場合のみ chartState を変更する
        // 今日受診していて，過去のカルテを修正しただけなのに診療完了になってしまうのを防ぐ
        if (MMLDate.getDate().equals(savedInfo.getFirstConfirmDateTrimTime())) {
            int len = soaPane.getTextPane().getText().length();
            boolean empty = len < MinimalKarteLength;
            // 仮保存の場合もUNFINISHED flagを立てる
            empty |= STATUS_TMP.equals(savedInfo.getStatus());
            pvt.setStateBit(PatientVisitModel.BIT_UNFINISHED, empty);
        }

        // 印刷
        int copies = params.getPrintCount();
        if (copies > 0) {
            printPanel2(chart.getContext().getPageFormat(), copies, false);
        }

        // save が終了したことを EditorFrame に知らせる
        fireFinishSave();

        // 文書履歴の更新を通知する
        chart.getDocumentHistory().getDocumentHistory();
    }

    /**
     * 状態マネージャ
     */
    private class StateMgr {

        private final EditorState noDirtyState = new NoDirtyState();
        private final EditorState dirtyState = new DirtyState();
        private final EditorState savedState = new SavedState();
        private EditorState currentState;
        private boolean forceDirty;

        public StateMgr() {
            currentState = noDirtyState;
        }

        public boolean isDirty() {
            return currentState.isDirty();
        }

        public void setForceDirty(boolean dirty) {
            forceDirty = dirty;
            setDirty(dirty);
        }

        public void setDirty(boolean dirty) {
            currentState = (dirty || forceDirty)
                    ? dirtyState : noDirtyState;
            currentState.controlMenu();
        }

        public void setSaved(boolean saved) {
            if (saved) {
                currentState = savedState;
                forceDirty = false;
                undoManager.discardAllEdits();
                currentState.controlMenu();
            }
        }

        public void controlMenu() {
            currentState.controlMenu();
        }
    }

    /**
     * このエディタの抽象状態クラス
     */
    private abstract class EditorState {

        public EditorState() {
        }

        public abstract boolean isDirty();

        public abstract void controlMenu();
    }

    /**
     * No dirty 状態クラス
     */
    private final class NoDirtyState extends EditorState {

        public NoDirtyState() {
        }

        @Override
        public void controlMenu() {
            Chart chart = getContext();
            chart.enabledAction(GUIConst.ACTION_SAVE, false);   // 保存
            chart.enabledAction(GUIConst.ACTION_PRINT, false);  // 印刷
            chart.enabledAction(GUIConst.ACTION_CUT, false);
            chart.enabledAction(GUIConst.ACTION_COPY, false);
            chart.enabledAction(GUIConst.ACTION_PASTE, false);
            chart.enabledAction(GUIConst.ACTION_UNDO, false);
            chart.enabledAction(GUIConst.ACTION_REDO, false);
            // 元町皮ふ科
            chart.enabledAction(GUIConst.ACTION_SEND_CLAIM, false);
            chart.enabledAction(GUIConst.ACTION_INSERT_TEXT, false);
            chart.enabledAction(GUIConst.ACTION_INSERT_SCHEMA, false);
            chart.enabledAction(GUIConst.ACTION_INSERT_STAMP, false);
            chart.enabledAction(GUIConst.ACTION_CHANGE_NUM_OF_DATES_ALL, (getMode() == DOUBLE_MODE)); //true
            chart.enabledAction(GUIConst.ACTION_SELECT_INSURANCE, (getMode() == DOUBLE_MODE)); //true

//pns^
            chart.enabledAction(GUIConst.ACTION_FIND_FIRST, false);
            chart.enabledAction(GUIConst.ACTION_FIND_NEXT, false);
            chart.enabledAction(GUIConst.ACTION_FIND_PREVIOUS, false);
//pns$
        }

        @Override
        public boolean isDirty() {
            return false;
        }
    }

    /**
     * Dirty 状態クラス
     */
    private final class DirtyState extends EditorState {

        public DirtyState() {
        }

        @Override
        public void controlMenu() {
            Chart chart = getContext();
            chart.enabledAction(GUIConst.ACTION_SAVE, true);
            chart.enabledAction(GUIConst.ACTION_PRINT, true);
            chart.enabledAction(GUIConst.ACTION_CHANGE_NUM_OF_DATES_ALL, (getMode() == DOUBLE_MODE)); //true
            chart.enabledAction(GUIConst.ACTION_SELECT_INSURANCE, (getMode() == DOUBLE_MODE));    //true
            chart.enabledAction(GUIConst.ACTION_UNDO, undoManager.canUndo());
            chart.enabledAction(GUIConst.ACTION_REDO, undoManager.canRedo());
//pns^
            getContext().enabledAction(GUIConst.ACTION_FIND_FIRST, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_NEXT, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_PREVIOUS, false);
            getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, false);
//pns$
        }

        @Override
        public boolean isDirty() {
            return true;
        }
    }

    /**
     * EmptyNew 状態クラス
     */
    private final class SavedState extends EditorState {

        public SavedState() {
        }

        @Override
        public void controlMenu() {
            Chart chart = getContext();
            chart.enabledAction(GUIConst.ACTION_SAVE, false);
            chart.enabledAction(GUIConst.ACTION_PRINT, true);
            chart.enabledAction(GUIConst.ACTION_CUT, false);
            chart.enabledAction(GUIConst.ACTION_COPY, false);
            chart.enabledAction(GUIConst.ACTION_PASTE, false);
            chart.enabledAction(GUIConst.ACTION_UNDO, false);
            chart.enabledAction(GUIConst.ACTION_REDO, false);

            // 元町皮ふ科
            chart.enabledAction(GUIConst.ACTION_SEND_CLAIM, false);

            chart.enabledAction(GUIConst.ACTION_INSERT_TEXT, false);
            chart.enabledAction(GUIConst.ACTION_INSERT_SCHEMA, false);
            chart.enabledAction(GUIConst.ACTION_INSERT_STAMP, false);
            chart.enabledAction(GUIConst.ACTION_CHANGE_NUM_OF_DATES_ALL, false);
            chart.enabledAction(GUIConst.ACTION_SELECT_INSURANCE, false);

//pns^
            getContext().enabledAction(GUIConst.ACTION_FIND_FIRST, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_NEXT, false);
            getContext().enabledAction(GUIConst.ACTION_FIND_PREVIOUS, false);
            getContext().enabledAction(GUIConst.ACTION_SEND_CLAIM, false);
//pns$
        }

        @Override
        public boolean isDirty() {
            return false;
        }
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        undoManager.addEdit(e.getEdit());
        updateUndoRedoAction();
    }

    public void undo() {
        try {
            undoManager.undo();
        } catch (CannotUndoException ex) {
            ex.printStackTrace(System.err);
        }
        updateUndoRedoAction();
    }

    public void redo() {
        try {
            undoManager.redo();
        } catch (CannotRedoException ex) {
            ex.printStackTrace(System.err);
        }
        updateUndoRedoAction();
    }

    private void updateUndoRedoAction() {

        boolean canUndo = undoManager.canUndo();
        undoAction.setEnabled(canUndo);
        redoAction.setEnabled(undoManager.canRedo());

        // KarteEditorにdirtyを設定する。dirtyなのはundo可能な時（のはず）
        stateMgr.setDirty(canUndo);
    }

}
