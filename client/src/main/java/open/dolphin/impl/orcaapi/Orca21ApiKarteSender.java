package open.dolphin.impl.orcaapi;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import open.dolphin.client.Chart;
import open.dolphin.client.IKarteSender;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.project.Project;
import open.dolphin.common.util.ZenkakuUtils;
import open.dolphin.impl.orcaapi.model.MedicalModModel;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.MMLTable;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.PVTHealthInsuranceModel;

/**
 * Orca21ApiKarteSender
 * 
 * @author masuda, Masuda Naika
 */
public class Orca21ApiKarteSender implements IKarteSender {

    // Context
    private Chart context;
    private DocumentModel sendModel;
    private PropertyChangeSupport boundSupport;

    // DG UUID の変わりに保険情報モジュールを送信する
    //private PVTHealthInsuranceModel insuranceToApply;
    
//masuda^    ClaimItemの最大数
    private static final int maxClaimItemCount = 40;
    private static final String ORCA_API = "ORCA API";
//masuda$
    
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
    public void removeListeners() {
        if (boundSupport != null) {
            for (PropertyChangeListener listener : boundSupport.getPropertyChangeListeners()) {
                boundSupport.removePropertyChangeListener(KarteSenderResult.PROP_KARTE_SENDER_RESULT, listener);
            }
        }
    }

    @Override
    public void fireResult(KarteSenderResult result) {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteSenderResult.PROP_KARTE_SENDER_RESULT, null, result);
        }
    }
    
    @Override
    public void send() {
        
        if (sendModel == null 
                || !sendModel.getDocInfoModel().isSendClaim() 
                || context == null) {
            fireResult(new KarteSenderResult(ORCA_API, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        // ORCA API使用しない場合はリターン
        if (!Project.getBoolean(Project.USE_ORCA_API)) {
            fireResult(new KarteSenderResult(ORCA_API, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        PVTHealthInsuranceModel insuranceToApply
                = context.getHealthInsuranceToApply(sendModel.getDocInfoModel().getHealthInsuranceGUID());
        
        if (insuranceToApply == null) {
            fireResult(new KarteSenderResult(ORCA_API, KarteSenderResult.SKIPPED, null, this));
            return;
        }

        DocInfoModel docInfo = sendModel.getDocInfoModel();
        // 入院カルテの場合はadmitFlagを立てる
        AdmissionModel admission = sendModel.getDocInfoModel().getAdmissionModel();
        boolean admissionFlg = (admission != null);
        List<ClaimBundle> cbList = getClaimBundleList(sendModel.getModules(), admissionFlg);
        
        MedicalModModel modModel = new MedicalModModel();
        modModel.setContext(context);
        modModel.setDepartmentCode(docInfo.getDepartmentCode());
        modModel.setPhysicianCode(Project.getString(Project.ORCA_STAFF_CODE));
        modModel.setPerformDate(docInfo.getFirstConfirmDate());
        modModel.setInsuranceModel(insuranceToApply);
        modModel.setClaimBundleList(cbList);
        modModel.setAdmissionFlg(admissionFlg);
        
        KarteSenderResult result = OrcaApiDelegater.getInstance().sendMedicalModModel(modModel);
        result.setKarteSender(this);
        fireResult(result);
    }

    private List<ClaimBundle> getClaimBundleList(List<ModuleModel> modules_src, boolean admission){
        
        // 保存する KarteModel の全モジュールをチェックしClaimBundleならヘルパーに登録
        // Orcaで受信できないような大きなClaimBundleを分割する
        // 処方のコメント項目は分離して、別に".980"として送信する
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
        
        return bundleList;
    }

    // 包括対象検査区分分ごとに分類する
    private List<ClaimBundle> divideBundleByHokatsuKbn(ClaimBundle cb) {

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
}
