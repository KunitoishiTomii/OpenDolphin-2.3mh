package open.dolphin.impl.claim;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import open.dolphin.client.*;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.message.ClaimHelper;
import open.dolphin.project.Project;
import open.dolphin.common.util.ZenkakuUtils;
import open.dolphin.dao.DaoException;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.MMLTable;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.message.ClaimMessageBuilder;
import org.apache.log4j.Level;

/**
 * Karte と Diagnosis の CLAIM を送る KarteEditor の sendClaim を独立させた
 * DiagnosisDocument の CLAIM 送信部分もここにまとめた
 *
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class ClaimSender implements IKarteSender {

    private static final String CLAIM = "CLAIM";

    // Context
    private Chart context;
    private DocumentModel sendModel;
    private PropertyChangeSupport boundSupport;

    private final boolean DEBUG;

//masuda^    ClaimItemの最大数
    private static final int maxClaimItemCount = 20;
//masuda$

    public ClaimSender() {
        DEBUG = (ClientContext.getBootLogger().getLevel() == Level.DEBUG);
    }

    @Override
    public Chart getContext() {
        return context;
    }

    @Override
    public void setContext(Chart context) {
        this.context = context;
    }

    @Override
    public void setModel(DocumentModel sendModel) {
        this.sendModel = sendModel;
    }

    @Override
    public void addListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
    }

    @Override
    public void removeListener(PropertyChangeListener listener) {
        if (boundSupport != null) {
            boundSupport.removePropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
        }
    }

    @Override
    public void fireResult(KarteSenderResult result) {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteSenderResult.PROP_KARTE_SENDER_RESULT, null, result);
        }
    }

    /**
     * DocumentModel の CLAIM 送信を行う。
     */
    @Override
    public void send() {

        if (sendModel == null
                || !sendModel.getDocInfoModel().isSendClaim()
                || context == null) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        // ORCA API使用時はCLAIM送信しない
        if (Project.getBoolean(Project.USE_ORCA_API)) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        // CLAIM 送信リスナ
        ClaimMessageListener claimListener = context.getCLAIMListener();

        // DG UUID の変わりに保険情報モジュールを送信する
        PVTHealthInsuranceModel insuranceToApply
                = context.getHealthInsuranceToApply(sendModel.getDocInfoModel().getHealthInsuranceGUID());

        if (claimListener == null || insuranceToApply == null) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        // ヘルパークラスを生成しVelocityが使用するためのパラメータを設定する
        ClaimHelper helper = new ClaimHelper();

//masuda^   入院カルテの場合はadmitFlagを立てる
        AdmissionModel admission = sendModel.getDocInfoModel().getAdmissionModel();
        if (admission != null) {
            helper.setAdmitFlag(true);
        }
        boolean b = Project.getBoolean(Project.CLAIM_01);
        helper.setUseDefalutDept(b);
//masuda$

        //DG ------
        DocInfoModel docInfo = sendModel.getDocInfoModel();
        List<ModuleModel> modules = sendModel.getModules();
        //--------DG

        //DG ------------------------------------------
        // 過去日で送信するために firstConfirmDate へ変更
        String confirmedStr = ModelUtils.getDateTimeAsString(docInfo.getFirstConfirmDate());
        //--------------------------------------------- DG
        helper.setConfirmDate(confirmedStr);
        debug(confirmedStr);

        String deptName = docInfo.getDepartmentName();
        String deptCode = docInfo.getDepartmentCode();
        String doctorName = docInfo.getAssignedDoctorName();
        if (doctorName == null) {
            doctorName = Project.getUserModel().getCommonName();
        }
        String doctorId = docInfo.getAssignedDoctorId();
        if (doctorId == null) {
            doctorId = Project.getUserModel().getOrcaId() != null
                    ? Project.getUserModel().getOrcaId()
                    : Project.getUserModel().getUserId();
        }
        String jamriCode = docInfo.getJMARICode();
        if (jamriCode == null) {
            jamriCode = Project.getString(Project.JMARI_CODE);
        }
        if (DEBUG) {
            debug(deptName);
            debug(deptCode);
            debug(doctorName);
            debug(doctorId);
            debug(jamriCode);
        }
        helper.setCreatorDeptDesc(deptName);
        helper.setCreatorDept(deptCode);
        helper.setCreatorName(doctorName);
        helper.setCreatorId(doctorId);
        helper.setCreatorLicense(Project.getUserModel().getLicenseModel().getLicense());
        helper.setJmariCode(jamriCode);
        helper.setFacilityName(Project.getUserModel().getFacilityModel().getFacilityName());

        //DG -------------------------------------------
        helper.setPatientId(context.getPatient().getPatientId());
        //--------------------------------------------- DG
        helper.setGenerationPurpose(docInfo.getPurpose());
        helper.setDocId(docInfo.getDocId());
        helper.setHealthInsuranceGUID(docInfo.getHealthInsuranceGUID());
        helper.setHealthInsuranceClassCode(docInfo.getHealthInsurance());
        helper.setHealthInsuranceDesc(docInfo.getHealthInsuranceDesc());

        //DG -----------------------------------------------
        // 2010-11-10 UUIDの変わりに保険情報モジュールを送信する
        helper.setSelectedInsurance(insuranceToApply);
        //-------------------------------------------------- DG
        if (DEBUG) {
            debug(helper.getHealthInsuranceGUID());
            debug(helper.getHealthInsuranceClassCode());
            debug(helper.getHealthInsuranceDesc());
        }
        try {
            // ヘルパー登録を分離 masuda
            registToHelper(helper, modules);
        } catch (DaoException ex) {
            fireResult(new KarteSenderResult(CLAIM, KarteSenderResult.ERROR, "ORCA接続", this));
            return;
        }

        ClaimMessageBuilder mb = ClaimMessageBuilder.getInstance();
        String claimMessage = mb.build(helper);
        ClaimMessageEvent cvt = new ClaimMessageEvent(this);
        cvt.setClaimInstance(claimMessage);
        //DG ----------------------------------------------
        cvt.setPatientId(context.getPatient().getPatientId());
        cvt.setPatientName(context.getPatient().getFullName());
        cvt.setPatientSex(context.getPatient().getGender());
        cvt.setTitle(sendModel.getDocInfoModel().getTitle());
        //---------------------------------------------- DG
        cvt.setConfirmDate(confirmedStr);

        // debug 出力を行う
        if (ClientContext.getClaimLogger() != null) {
            ClientContext.getClaimLogger().debug(cvt.getClaimInsutance());
        }

        claimListener.claimMessageEvent(cvt);
    }

    private void debug(String msg) {
        if (DEBUG) {
            ClientContext.getBootLogger().debug(msg);
        }
    }

//masuda^
    private void registToHelper(ClaimHelper helper, List<ModuleModel> modules_src) throws DaoException {
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する

        boolean admission = helper.getAdmitFlag();
        List<ClaimItem> commentItem = new ArrayList<>();
        List<ClaimBundle> bundleList = new ArrayList<>();

        // ClaimBundleを抽出する
        for (ModuleModel mm : modules_src) {

            // ClaimBundleのみを処理する
            String entity = mm.getModuleInfoBean().getEntity();
            IModuleModel im = mm.getModel();
            if (entity == null || !(im instanceof ClaimBundle)) {
                continue;
            }

            // 気持ちが悪いので複製をつかう
            ClaimBundle bundle = cloneClaimBundle((ClaimBundle) im, true);

            switch (entity) {
                case IInfoModel.ENTITY_MED_ORDER:
                    List<ClaimItem> nonCommentItem = new ArrayList<>();
                    for (ClaimItem ci : bundle.getClaimItem()) {
                        // それぞれの処方bundleをしらべる
                        boolean comment = ci.getCode().matches(ClaimConst.REGEXP_PRESCRIPTION_COMMENT);
                        // 先頭がアスタリスクならば.980に分離しない
                        comment &= ci.getName() != null
                                && !ci.getName().startsWith("*")
                                && !ci.getName().startsWith("＊");
                        if (comment) {
                            commentItem.add(ci);    // コメントコード
                        } else {
                            nonCommentItem.add(ci); // コメントじゃない
                        }
                    }   // コメントコードを抜き取った残りをbundleに登録しなおす
                    if (!commentItem.isEmpty()) {
                        ClaimItem[] newItems = new ClaimItem[nonCommentItem.size()];
                        bundle.setClaimItem(nonCommentItem.toArray(newItems));
                    }
                    break;
                case IInfoModel.ENTITY_INJECTION_ORDER:
                    String clsCode = bundle.getClassCode();
                    if (clsCode != null && clsCode.startsWith("3") && clsCode.endsWith("1")) {
                        List<ClaimItem> ciList = new ArrayList<>();
                        for (ClaimItem ci : bundle.getClaimItem()) {
                            // int ClaimConst.SYUGI = 0
                            if (!"0".equals(ci.getClassCode())) {
                                ciList.add(ci);
                            }
                        }
                        ClaimItem[] newItems = new ClaimItem[ciList.size()];
                        bundle.setClaimItem(ciList.toArray(newItems));
                    }
                    break;
            }

            // 文字置換
            for (ClaimItem ci : bundle.getClaimItem()) {
                String replaced = ZenkakuUtils.utf8Replace(ci.getName());
                ci.setName(replaced);
            }

            List<ClaimBundle> cbList = new ArrayList();
            // 入院の検体検査の場合は包括対象検査区分ごとに分類する
            // そうしないと項目によってはbundleNumberが不正になってしまう。
            // ORCAの「仕様」とのこと…
            if (admission && ClaimConst.RECEIPT_CODE_LABO.equals(bundle.getClassCode())) {
                cbList.addAll(divideBundleByHokatsuKbn(bundle));
            } else {
                cbList.add(bundle);
            }

            // ClaimItem数が20を超えないように分割する
            for (ClaimBundle cb : cbList) {
                int count = cb.getClaimItem().length;
                if (count > maxClaimItemCount) {
                    bundleList.addAll(divideClaimBundle(cb));
                } else {
                    // 20以下なら今までどおり
                    bundleList.add(cb);
                }
            }
        }

        // 抜き出したコメント項目は.980で別に送る。コメントが20超えることはないだろう。
        // レセには印刷されなくなる？
        if (!commentItem.isEmpty()) {
            ClaimBundle cb = new ClaimBundle();
            cb.setClassName(MMLTable.getClaimClassCodeName("980"));
            cb.setClassCode("980");                             // 処方箋備考のclass code
            cb.setClassCodeSystem(ClaimConst.CLASS_CODE_ID);    // "Claim007"
            ClaimItem[] newItems = new ClaimItem[commentItem.size()];
            cb.setClaimItem(commentItem.toArray(newItems));
            bundleList.add(cb);
        }
        
        // ヘルパーに登録
        ClaimBundle[] cbArray = new ClaimBundle[bundleList.size()];
        helper.setClaimBundle(bundleList.toArray(cbArray));
    }

    // 包括対象検査区分分ごとに分類する
    private List<ClaimBundle> divideBundleByHokatsuKbn(ClaimBundle cb) throws DaoException {

        // srycdを列挙する
        List<String> srycds = new ArrayList<>();
        for (ClaimItem ci : cb.getClaimItem()) {
            srycds.add(ci.getCode());
        }

        // 包括対象検査区分とのマップを取得する
        Map<String, Integer> kbnMap = SqlMiscDao.getInstance().getHokatsuKbnMap(srycds);

        // 各項目をグループ分けする
        Map<Integer, List<ClaimItem>> ciMap = new HashMap<>();
        for (ClaimItem ci : cb.getClaimItem()) {
            Integer kbn = kbnMap.get(ci.getCode());
            List<ClaimItem> list = ciMap.get(kbn);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(ci);
            ciMap.put(kbn, list);
        }

        // ClaimBundleに戻す
        List<ClaimBundle> ret = new ArrayList<>();
        for (Map.Entry<Integer, List<ClaimItem>> entry : ciMap.entrySet()) {
            int houksnkbn = entry.getKey();
            List<ClaimItem> ciList = entry.getValue();
            // ＯＳＣに問い合わせたところ、下記の返答 2012/09/26
            // 「包括対象検査の対象でない検査は、検査毎に剤を分けていただくしか方法はありません」
            if (houksnkbn != 0) {
                ClaimBundle bundle = cloneClaimBundle(cb, false);
                ClaimItem[] newItems = new ClaimItem[ciList.size()];
                bundle.setClaimItem(ciList.toArray(newItems));
                ret.add(bundle);
            } else {
                for (ClaimItem ci : ciList) {
                    ClaimBundle bundle = cloneClaimBundle(cb, false);
                    bundle.setClaimItem(new ClaimItem[]{ci});
                    ret.add(bundle);
                }
            }
        }

        return ret;
    }

    private List<ClaimBundle> divideClaimBundle(ClaimBundle cb) {
        // Orcaで同時に受信できるClaimItem数が20に限られているので
        // 20を超えていたらClaimBundleを分割する masuda
        List<ClaimBundle> ret = new ArrayList<>();

        ClaimItem[] array = cb.getClaimItem();
        int size = array.length;
        int index = 0;

        while (index < size) {
            ClaimBundle bundle = cloneClaimBundle(cb, false);
            int indexTo = Math.min(index + maxClaimItemCount, size);
            ClaimItem[] ciArray = Arrays.copyOfRange(array, index, indexTo);
            bundle.setClaimItem(ciArray);
            ret.add(bundle);
            index = index + maxClaimItemCount;
        }

        return ret;
    }

    // 自前deep copy
    private ClaimBundle cloneClaimBundle(ClaimBundle src, boolean copyClaimItem) {
        ClaimBundle ret = new ClaimBundle();
        ret.setAdmin(src.getAdmin());
        ret.setAdminCode(src.getAdminCode());
        ret.setAdminCodeSystem(src.getAdminCodeSystem());
        ret.setAdminMemo(src.getAdminMemo());
        ret.setBundleNumber(src.getBundleNumber());
        ret.setClassCode(src.getClassCode());
        ret.setClassCodeSystem(src.getClassCodeSystem());
        ret.setClassName(src.getClassName());
        ret.setInsurance(src.getInsurance());
        ret.setMemo(src.getMemo());

        if (copyClaimItem && src.getClaimItem() != null) {
            int len = src.getClaimItem().length;
            ClaimItem[] items = new ClaimItem[len];
            for (int i = 0; i < len; ++i) {
                items[i] = cloneClaimItem(src.getClaimItem()[i]);
            }
            ret.setClaimItem(items);
        }
        return ret;
    }

    private ClaimItem cloneClaimItem(ClaimItem src) {
        ClaimItem ret = new ClaimItem();
        ret.setClassCode(src.getClassCode());
        ret.setClassCodeSystem(src.getClassCodeSystem());
        ret.setCode(src.getCode());
        ret.setCodeSystem(src.getCodeSystem());
        ret.setMemo(src.getMemo());
        ret.setName(src.getName());
        ret.setNumber(src.getNumber());
        ret.setNumberCode(src.getNumberCode());
        ret.setNumberCodeSystem(src.getNumberCodeSystem());
        ret.setUnit(src.getUnit());
        ret.setYkzKbn(src.getYkzKbn());
        return ret;
    }
//masuda$
}
