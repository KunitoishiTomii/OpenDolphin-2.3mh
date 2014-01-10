package open.dolphin.impl.rezept.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import open.dolphin.impl.rezept.LexicalAnalyzer;
import open.dolphin.impl.rezept.model.IRezeItem;
import open.dolphin.impl.rezept.model.RE_Model;
import open.dolphin.impl.rezept.model.SY_Model;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;

/**
 * 適応病名チェックフィルタ
 *
 * @author masuda, Masuda Naika
 */
public class DiagnosisFilter extends AbstractCheckFilter {
    
    private static final String FILTER_NAME = "適応病名";
    private static final int MAX_DIAG_COUNT = 10;
    
    @Override
    public List<CheckResult> doCheck(RE_Model reModel) {
        
        boolean isAdmission = "1".equals(reModel.getNyugaikbn());
        List<CheckResult> results = new ArrayList<>();

        // hitCountをクリアする
        List<SY_Model> diagList = reModel.getSYModelList();
        for (SY_Model syModel : diagList) {
            syModel.setHitCount(0);
        }
        
        // 薬剤・診療行為と病名対応チェック
        for (IRezeItem item : reModel.getItemList()) {
            CheckResult result = checkDiag(diagList, item, isAdmission);
            if (result != null) {
                results.add(result);
            }
        }
        
        // 余剰病名チェック
        for (SY_Model syModel : diagList) {
            CheckResult result = checkSurplusDiag(syModel);
            if (result != null) {
                results.add(result);
            }
        }
        
        // 病名数チェック
        CheckResult result = checkDiagCount(diagList.size());
        if (result != null) {
            results.add(result);
        }
        
        return results;
    }

    // 病名と適応症マスタでチェック
    private CheckResult checkDiag(List<SY_Model> diagList, IRezeItem rezeItem, boolean isAdmission) {
        
        CheckResult result = new CheckResult();
        result.setFilterName(FILTER_NAME);
        result.setResult(CheckResult.CHECK_ERROR);
        
        Map<String, IndicationModel> indicationMap = viewer.getIndicationMap();
        IndicationModel indication = indicationMap.get(rezeItem.getSrycd());

        if (indication == null) {
            String msg = String.format("%sの適応症データがありません", rezeItem.getDescription());
            result.setMsg(msg);
            return result;
        }
        if (!(indication.isAdmission() && isAdmission || indication.isOutPatient() && !isAdmission)) {
            rezeItem.setHitCount(1);
            return null;
        }
        
        boolean checkOk = false;
        
        for (SY_Model syModel : diagList) {
            int hitCount = 0;
            String diagName = syModel.getDiagName();
            
            for (IndicationItem item : indication.getIndicationItems()) {
                try {
                    boolean b = LexicalAnalyzer.check(diagName, item.getKeyword());
                    if (b) {
                        hitCount++;
                        rezeItem.setHitCount(hitCount);
                    }
                    if (item.isNotCondition() && b) {
                        // ドボンの場合
                        String msg = String.format("%sに%sは禁止です", diagName, rezeItem.getDescription());
                        result.setMsg(msg);
                        rezeItem.setHitCount(0);
                        return result;
                    } else {
                        // どれかhitすればOK
                        checkOk |= b;
                    }
                } catch (Exception ex) {
                }
            }
            int oldSyHitCount = syModel.getHitCount();
            syModel.setHitCount(oldSyHitCount + hitCount);
        }
        
        if (checkOk) {
            return null;
        } else {
            String msg = String.format("%sに対応する病名がありません", rezeItem.getDescription());
            result.setMsg(msg);
        }
        
        return result;
    }
    
    // 余剰病名チェック
    private CheckResult checkSurplusDiag(SY_Model syModel) {
        if (syModel.getHitCount() == 0) {
            CheckResult result = new CheckResult();
            result.setFilterName(FILTER_NAME);
            result.setResult(CheckResult.CHECK_INFO);
            String msg = String.format("%sは余剰病名かもしれません", syModel.getDiagName());
            result.setMsg(msg);
            return result;
        }
        return null;
    }
    
    // 病名数チェック
    private CheckResult checkDiagCount(int count) {
        if (count > MAX_DIAG_COUNT) {
            CheckResult result = new CheckResult();
            result.setFilterName(FILTER_NAME);
            result.setResult(CheckResult.CHECK_WARNING);
            result.setMsg(String.format("病名数が%dを超えています", MAX_DIAG_COUNT));
            return result;
        }
        return null;
    }
}
