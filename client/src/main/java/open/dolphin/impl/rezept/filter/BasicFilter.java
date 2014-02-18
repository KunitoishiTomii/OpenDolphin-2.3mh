package open.dolphin.impl.rezept.filter;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import open.dolphin.impl.rezept.model.HO_Model;
import open.dolphin.impl.rezept.model.IRezeItem;
import open.dolphin.impl.rezept.model.KO_Model;
import open.dolphin.impl.rezept.model.RE_Model;
import open.dolphin.impl.rezept.model.SI_Model;
import open.dolphin.impl.rezept.model.SJ_Model;
import open.dolphin.impl.rezept.model.SY_Model;

/**
 * 基本フィルタ
 *
 * @author masuda, Masuda Naika
 */
public class BasicFilter extends AbstractCheckFilter {

    private static final String FILTER_NAME = "基本";
    private static final int MAX_DIAG_COUNT = 10;
    private static final String UNCODED_DIAG_SRYCD = "0000999";

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    @Override
    public List<CheckResult> doCheck(RE_Model reModel) {
        
        List<CheckResult> results = new ArrayList<>();
        
        // 保険チェック
        List<CheckResult> results1 = checkInsurance(reModel);
        results.addAll(results1);
        
        // 病名数チェック、病名重複チェック
        List<CheckResult> list = checkDiag(reModel);
        results.addAll(list);
        
        // コメント有無チェック
        CheckResult result1 = checkComment(reModel);
        if (result1 != null) {
            results.add(result1);
        }

        return results;
    }
    
    // コメント有無チェック
    private CheckResult checkComment(RE_Model reModel) {
        List<SJ_Model> sjList = reModel.getSJModelList();
        if (sjList != null && !sjList.isEmpty()) {
            CheckResult result = createCheckResult("症状詳記があります", CheckResult.CHECK_WARNING);
            return result;
        }
        return null;
    }
    
    // 病名重複、病名数チェック
    private List<CheckResult> checkDiag(RE_Model reModel) {
        
        List<SY_Model> diagList = reModel.getSYModelList();

        List<CheckResult> results = new ArrayList<>();
        
        // 病名数
        int len = diagList.size();
        if (len > MAX_DIAG_COUNT) {
            String msg = String.format("病名数が%dを超えています", MAX_DIAG_COUNT);
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_WARNING);
            results.add(result);
        }
        if (len == 0) {
            String msg = String.format("病名が全くありません", MAX_DIAG_COUNT);
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
            results.add(result);
        }
        
        // 重複
        for (int i = 0; i < len; ++i) {
            String diag1 = diagList.get(i).getDiagName();
            for (int j = i; j < len; ++j) {
                if (i != j) {
                    String diag2 = diagList.get(j).getDiagName();
                    if (diag1.equals(diag2)) {
                        String msg = String.format("病名 %s は重複しています", diag1);
                        CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
                        results.add(result);
                    }
                }
            }
        }
        
        // 未コード化病名
        for (SY_Model syModel : diagList) {
            if (UNCODED_DIAG_SRYCD.equals(syModel.getSrycd())) {
                String diag = syModel.getDiagName();
                String msg = String.format("病名 %s は未コード化病名です", diag);
                CheckResult result = createCheckResult(msg, CheckResult.CHECK_WARNING);
                results.add(result);
            }
        }
        
        // 疑い・急性チェック
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(reModel.getBillDate());
        gc.add(GregorianCalendar.MONTH, -2);    // ２か月前
        
        for (SY_Model syModel : diagList) {
            String diag = syModel.getDiagName();
            if ("1".equals(syModel.getOutcome())    // 1:転帰なし
                    && (diag.contains("急性") || diag.contains("の疑い"))
                    && syModel.getStartDate().before(gc.getTime())) {
                
                String msg = String.format("急性・疑い病名 %s は消し忘れかも", diag);
                CheckResult result = createCheckResult(msg, CheckResult.CHECK_WARNING);
                results.add(result);
            }
        }
        
        // 初診算定日前の開始病名
        Date shoshinDate = null;
        for (IRezeItem item : reModel.getItemList()) {
            if (item instanceof SI_Model && "11".equals(item.getClassCode())) {
                SI_Model siModel = (SI_Model) item;
                int day = siModel.getDayData().get(0).getDay();
                gc.setTime(reModel.getBillDate());
                gc.set(GregorianCalendar.DATE, day);
                shoshinDate = gc.getTime();
                break;
            }
        }
        if (shoshinDate != null) {
            for (SY_Model syModel : diagList) {
                if (syModel.isActive() && syModel.getStartDate().before(shoshinDate)) {
                    String diag = syModel.getDiagName();
                    String msg = String.format("初診算定日より前の病名 %s", diag);
                    CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
                    results.add(result);
                }
            }
        }

        return results;
    }
    
    // 保険チェック
    private List<CheckResult> checkInsurance(RE_Model reModel) {

        List<CheckResult> ret = new ArrayList<>();

        // 無保険？
        HO_Model hoModel = reModel.getHOModel();
        List<KO_Model> koList = reModel.getKOModelList();
        boolean hasKO = koList != null && !koList.isEmpty();
        if (hoModel == null && !hasKO) {
            String msg = "保険も公費もありません";
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
            ret.add(result);
        }
        // 受給者番号チェック
        if (hasKO) {
            for (KO_Model koModel : koList) {
                String certNum = koModel.getCertificateNum();
                if (certNum == null || certNum.isEmpty()) {
                    String msg = String.format("公費(%s)の受給者番号がありません", koModel.getInsuranceNum());
                    CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
                    ret.add(result);
                }
            }
        }

        return ret;
    }
}
