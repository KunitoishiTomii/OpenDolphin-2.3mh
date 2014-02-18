package open.dolphin.impl.rezept.filter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import open.dolphin.impl.rezept.LexicalAnalyzer;
import open.dolphin.impl.rezept.model.IRezeItem;
import open.dolphin.impl.rezept.model.RE_Model;
import open.dolphin.impl.rezept.model.SI_Model;
import open.dolphin.impl.rezept.model.SY_Model;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;

/**
 * 適応病名チェックフィルタ
 *
 * @author masuda, Masuda Naika
 */
public class DiagnosisFilter extends AbstractCheckFilter {

    private static final String AND_OPERATOR = "&";
    private static final String OR_OPERATOR = "|";

    private static final String FILTER_NAME = "適応病名";

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    @Override
    public List<CheckResult> doCheck(RE_Model reModel) {

        boolean isAdmission = "1".equals(reModel.getNyugaikbn());
        List<CheckResult> results = new ArrayList<>();

        List<SY_Model> diagList = reModel.getSYModelList();

        // 薬剤・診療行為と病名対応チェック
        for (IRezeItem item : reModel.getItemList()) {
            List<CheckResult> list = checkDiag(diagList, item, isAdmission);
            results.addAll(list);
        }

        // 余剰病名チェック
        for (SY_Model syModel : diagList) {
            CheckResult result = checkSurplusDiag(syModel);
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    // 病名と適応症マスタでチェック
    private List<CheckResult> checkDiag(List<SY_Model> diagList, IRezeItem rezeItem, boolean isAdmission) {

        Map<String, IndicationModel> indicationMap = viewer.getIndicationMap();
        IndicationModel indication = indicationMap.get(rezeItem.getSrycd());

        // 適応症データがない場合はエラー
        if (indication == null) {
            rezeItem.setPass(false);
            String msg = String.format("%sの適応症データがありません", rezeItem.getDescription());
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
            result.setSrycd(rezeItem.getSrycd());
            return Collections.singletonList(result);
        }

        // 審査対象でないならばスキップ
        if (!(indication.isAdmission() && isAdmission || indication.isOutPatient() && !isAdmission)) {
            rezeItem.setPass(true);
            return Collections.emptyList();
        }
        
        // 検査１０項目包括審査対象外ならばスキップ
        if (rezeItem instanceof SI_Model) {
            SI_Model siModel = (SI_Model) rezeItem;
            if (siModel.isInclusive() && indication.isInclusive()) {
                return Collections.emptyList();
            }
        }
        
        // 810000001の場合はdescriptionが合致するものを対象とする
        // XPなどの部位以外で810000001コメントがあるとうっとうしいが、病名漏れよりはよかろう
        List<IndicationItem> indicationItems = new ArrayList<>();
        if ("810000001".equals(rezeItem.getSrycd())) {
            for (IndicationItem item : indication.getIndicationItems()) {
                if (rezeItem.getDescription().equals(item.getDescription())) {
                    indicationItems.add(item);
                }
            }
        } else {
            indicationItems.addAll(indication.getIndicationItems());
        }

        // or条件とnot条件を分離する
        List<IndicationItem> orItems = new ArrayList();
        List<IndicationItem> notItems = new ArrayList();
        for (IndicationItem item : indicationItems) {
            if (item.isNotCondition()) {
                notItems.add(item);
            } else {
                orItems.add(item);
            }
        }

        List<CheckResult> results = new ArrayList<>();
        String description = rezeItem.getDescription();

        // or条件
        for (IndicationItem item : orItems) {
            try {
                String keyword = item.getKeyword();
                boolean b = checkIndicatedDiag(diagList, keyword, false);
                if (b) {
                    rezeItem.incrementHitCount();
                }
            } catch (Exception ex) {
            }
        }
        // 病名ヒットしなかったrezeItemは不合格
        if (rezeItem.getHitCount() == 0) {
            rezeItem.setPass(false);
            String msg = String.format("%sに対応する病名がありません", description);
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
            result.setSrycd(rezeItem.getSrycd());
            results.add(result);
        }

        // not条件
        for (IndicationItem item : notItems) {
            try {
                String keyword = item.getKeyword();
                boolean b = checkIndicatedDiag(diagList, keyword, true);
                if (b) {
                    // ドボンの場合
                    rezeItem.setPass(false);
                    String msg = String.format("%sの禁止句「%s」が病名に存在します", description, keyword);
                    CheckResult result = createCheckResult(msg, CheckResult.CHECK_ERROR);
                    result.setSrycd(rezeItem.getSrycd());
                    results.add(result);
                }
            } catch (Exception ex) {
            }
        }

        return results;
    }

    // 傷病名リストにキーワードが含まれるかチェックする
    public boolean checkIndicatedDiag(List<SY_Model> diagList,
            String keyword, boolean notCondition) throws Exception {

        Deque<Boolean> stack = new ArrayDeque();
        List<String> tokens = LexicalAnalyzer.toPostFixNotation(keyword);

        for (String token : tokens) {
            switch (token) {
                case AND_OPERATOR:
                    boolean b1 = stack.pop() & stack.pop(); // &&はダメ
                    stack.push(b1);
                    break;
                case OR_OPERATOR:
                    boolean b2 = stack.pop() | stack.pop(); // ||はダメ
                    stack.push(b2);
                    break;
                default:
                    boolean hit = false;
                    for (SY_Model syModel : diagList) {
                        boolean b3 = syModel.getDiagName().contains(token);
                        hit |= b3;
                        if (notCondition) {
                            if (b3) {
                                // ドボン
                                syModel.setPass(false);
                            }
                        } else {
                            if (b3) {
                                syModel.incrementHitCount();
                            }
                        }
                    }
                    stack.push(hit);
                    break;
            }
        }

        boolean pass = stack.pop();
        if (!stack.isEmpty()) {
            System.out.println("DiagnosisFilter lexicalAnalyze: stack is not empty");
        }

        return pass;
    }

    // 余剰病名チェック
    private CheckResult checkSurplusDiag(SY_Model syModel) {
        if (syModel.getHitCount() == 0) {
            String msg = String.format("%sは余剰病名かもしれません", syModel.getDiagName());
            CheckResult result = createCheckResult(msg, CheckResult.CHECK_INFO);
            return result;
        }
        return null;
    }
}
