package open.dolphin.client;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.print.PageFormat;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.text.StyleConstants;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.project.Project;
import open.dolphin.util.AgeCalculator;

/**
 * ドキュメントの抽象ビュワークラス
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public abstract class KarteViewer extends AbstractChartDocument {

    // 選択されている時のボーダ色、1.3の赤
    private static final Color SELECTED_COLOR = new Color(255, 0, 153);
    // 選択された状態のボーダ
    private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(SELECTED_COLOR);
    // 選択されていない時のボーダ色
    private static final Color NOT_SELECTED_COLOR = new Color(0, 0, 0, 0);  // 透明
    // 選択されていない状態のボーダ
    protected static final Border NOT_SELECTED_BORDER = BorderFactory.createLineBorder(NOT_SELECTED_COLOR);
    // 仮保存中のドキュメントを表す文字
    protected static final String UNDER_TMP_SAVE = " - 仮保存中";
    
    private static final String COMPONENT_NAME = StyleConstants.ComponentElementName;

    // この view のモデル
    private DocumentModel model;

    // 2号カルテパネル
    private KartePanel kartePanel;

    // 選択されているかどうかのフラグ
    private boolean selected;
    
    // １号用紙か２号用紙
    public static enum MODE {SINGLE, DOUBLE};

    // 抽象メソッド
    public abstract KartePane getSOAPane();

    public abstract KartePane getPPane();

    public abstract void addMouseListener(MouseListener ml);

    public abstract void setBackground(Color c);
    
    // KarteViewerのJTextPaneにKarteScrollerPanelのActionMapを設定する
    // これをしないとJTextPaneにフォーカスがあるとキーでスクロールできない
    public abstract void setParentActionMap(ActionMap amap);

    
    // ファクトリー
    public static KarteViewer createKarteViewer(MODE mode) {

        switch(mode) {
            case SINGLE:
                return new KarteViewer1();
            case DOUBLE:
                return new KarteViewer2();
        }
        return null;
    }
    
    protected final void setTitle() {

        DocInfoModel docInfo = model.getDocInfoModel();
        String status = docInfo.getStatus();
        StringBuilder sb = new StringBuilder();
        
        if (status != null) {
            switch (status) {
                case IInfoModel.STATUS_DELETE:
                    sb.append("削除済／");
                    break;
                case IInfoModel.STATUS_MODIFIED:
                    sb.append("修正:");
                    sb.append(docInfo.getVersionNumber().replace(".0", ""));
                    sb.append("／");
                    break;
            }
        }

        // 確定日を分かりやすい表現に変える
        sb.append(ModelUtils.getDateAsFormatString(
                docInfo.getFirstConfirmDate(), IInfoModel.KARTE_DATE_FORMAT));

        // 当時の年齢を表示する
        String mmlBirthday = getContext().getPatient().getBirthday();
        String mmlDate = ModelUtils.getDateAsString(docInfo.getFirstConfirmDate());
        if (mmlBirthday != null) {
            sb.append("[").append(AgeCalculator.getAge2(mmlBirthday, mmlDate)).append("歳]");
        }
        
        if (docInfo.getStatus().equals(IInfoModel.STATUS_TMP)) {
            sb.append(UNDER_TMP_SAVE);
        }
        
        // 入院の場合は病室・入院科を表示する
        AdmissionModel admission = docInfo.getAdmissionModel();
        if (admission != null) {
            sb.append("<");
            sb.append(admission.getRoom()).append("号室:");
            sb.append(admission.getDepartment());
            sb.append(">");
        }
        
        // 保険　公費が見えるのは気分良くないだろうから、表示しない
        // コロン区切りの保険者名称・公費のフォーマットである 
        // 旧カルテはSPC区切りの保険者番号・SPC・保険者名称・公費のフォーマット
        String ins = docInfo.getHealthInsuranceDesc().trim();
        if (ins != null && !ins.isEmpty()) {
            if (ins.contains(":")) {
                String items[] = docInfo.getHealthInsuranceDesc().split(":");
                sb.append("／");
                sb.append(items[0]);
            } else if (ins.contains(" ")) {
                String items[] = docInfo.getHealthInsuranceDesc().split(" ");
                if (items.length > 2) {
                    sb.append("／");
                    sb.append(items[2]);
                } else {
                    sb.append("／");
                    sb.append(ins);
                }
            } else {
                sb.append("／");
                sb.append(ins);
            }
        }
        
        // KarteViewerで日付の右Dr名を表示する
        sb.append("／");
        sb.append(model.getUserModel().getCommonName());
        
//katoh^
        // a little modification by masuda
        // 初版作成者と最新版作成者が違う場合はその旨表示する
        String rootUser = model.getDocInfoModel().getRootUser();
        if (rootUser != null && !rootUser.equals(model.getUserModel().getCommonName())) {
            sb.append("(初版：");
            sb.append(rootUser);
            sb.append(")");
        }
//katoh$
        
        kartePanel.getTimeStampLabel().setText(sb.toString());
        
//masuda^   タイトルを文書種別によって色分けする
        kartePanel.setTitleColor(docInfo);
//masuda$
    }
    
    // StampHolder, SchemaHolder内容設定を後回しにしてレンダリング
    protected final void renderKarteLazily() {
        KarteRenderer_2.getInstance().renderLazily(getModel(), getSOAPane(), getPPane());
    }

    protected final void setKartePanel(KartePanel kartePanel) {
        this.kartePanel = kartePanel;
    }
    
    // memory leak?
    protected final void dispose() {
        kartePanel.removeAll();
    }

    public final String getDocType() {

        if (model != null) {
            String docType = model.getDocInfoModel().getDocType();
            return docType;
        }
        return null;
    }

    // Junzo SATO
    public void printPanel2(final PageFormat format) {
        String name = getContext().getPatient().getFullName();
        boolean printName = true;
        if (kartePanel.isSinglePane()) {
            printName = printName && Project.getBoolean("plain.print.patinet.name");
        }
        kartePanel.printPanel(format, 1, false, name, kartePanel.getPreferredSize().height +60, printName);
    }

    public void printPanel2(final PageFormat format, final int copies,
            final boolean useDialog) {
        String name = getContext().getPatient().getFullName();
        boolean printName = true;
        if (kartePanel.isSinglePane()) {
            printName = printName && Project.getBoolean("plain.print.patinet.name");
        }
        kartePanel.printPanel(format, copies, useDialog, name, kartePanel.getPreferredSize().height +60, printName);
    }

    @Override
    public void print() {
        PageFormat pageFormat = getContext().getContext().getPageFormat();
        this.printPanel2(pageFormat);
    }
    /**
     * 表示するモデルを設定する。
     * @param model 表示するDocumentModel
     */
    public final void setModel(DocumentModel model) {
        this.model = model;
    }

    /**
     * 表示するモデルを返す。
     * @return 表示するDocumentModel
     */
    public final DocumentModel getModel() {
        return model;
    }

    /**
     * 選択状態を設定する。
     * 選択状態によりViewのボーダの色を変える。
     * @param selected 選択された時 true
     */
    public final void setSelected(boolean selected) {

        this.selected = selected;
        if (selected) {
            getUI().setBorder(SELECTED_BORDER);
            enter();
        } else {
            getUI().setBorder(NOT_SELECTED_BORDER);
        }
    }

    /**
     * 選択されているかどうかを返す。
     * @return 選択されている時 true
     */
    public final boolean isSelected() {
        return selected;
    }

    /**
     * コンテナからコールされる enter() メソッドで
     * メニューを制御する。
     */
    @Override
    public final void enter() {
        
        // コレしちゃうとchainがKarteDocumentViewerからKarteViewerに変わってしまい
        // カルテ編集できなくなる。DON'T!!
        //super.enter();    

        // ReadOnly 属性
        boolean canEdit = !getContext().isReadOnly();
        // 仮保存かどうか
        boolean tmp = IInfoModel.STATUS_TMP.equals(model.getDocInfoModel().getStatus());
        // 新規カルテ作成が可能な条件
        boolean newOk = (canEdit && !tmp);

        ChartMediator mediator = getContext().getChartMediator();
        mediator.getAction(GUIConst.ACTION_NEW_KARTE).setEnabled(newOk);        // 新規カルテ
        mediator.getAction(GUIConst.ACTION_PRINT).setEnabled(true);             // 印刷
        mediator.getAction(GUIConst.ACTION_MODIFY_KARTE).setEnabled(canEdit);   // 修正
    }
}