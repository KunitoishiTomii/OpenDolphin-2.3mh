package open.dolphin.impl.rezept;

import open.dolphin.impl.rezept.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.TensuMaster;

/**
 * UkeLoader
 * 
 * @author masuda, Masuda Naika
 */
public class UkeLoader {
    
    private static final String UNCODED_DIAG_SRYCD = "0000999";
    private static final String MODIFIER_PREFIX = "ZZZ";
    //private static final String ENCODING = "SJIS";
    private Set<String> itemSrycdSet;
    private int reModelCount;
    
    public Set<String> getItemSrycdSet() {
        return itemSrycdSet;
    }
    
    public int getReModelCount() {
        return reModelCount;
    }

    public List<IR_Model> loadFromOrca(String ym) {

        List<IR_Model> irModelList = new ArrayList<>();

        // 1:社保, 2:国保, 6:後期高齢者
        final int[] teisyutusakiArray = {1, 2, 6};
        final String[] nyugaikbnArray = {"1", "2"};

        for (String nyugaikbn : nyugaikbnArray) {
            for (int teisyutusaki : teisyutusakiArray) {

                // tbl_recedenを参照する
                SqlMiscDao dao = SqlMiscDao.getInstance();
                List<String> list = dao.getRecedenCsv(ym, nyugaikbn, teisyutusaki);

                if (!list.isEmpty()) {
                    // tbl_recedenにIRは記録されていないので作成する
                    IR_Model irModel = new IR_Model();
                    irModel.setShinsaKikan(teisyutusaki);
                    irModel.setTenTable(1);     // 医科
                    
                    LineParser parser = new LineParser();
                    parser.setCurrentModel(irModel);
                    
                    for (String line : list) {
                        try {
                            parser.parseLine(line);
                        } catch (Exception ex) {
                            ex.printStackTrace(System.err);
                        }
                    }
                    
                    // tbl_recedenにGOは記録されていないので作成する
                    GO_Model goModel = new GO_Model();
                    goModel.setTotalTen(parser.getTotalTen());
                    goModel.setTotalCount(irModel.getReModelList().size());
                    irModel.setGOModel(goModel);

                    irModelList.add(irModel);
                    
                    reModelCount += parser.getReModelCount();
                }
            }
        }
        if (irModelList.isEmpty()) {
            return null;
        }

        // patient id順にソート
        sortByPatientId(irModelList);

        // 傷病名・点数マスタを参照して名称をセットする
        processSYModel(irModelList);
        processIRezeItem(irModelList);

        return irModelList;
    }

    private static class LineParser {

        private int totalTen;
        private boolean hasHO;
        private IR_Model currentModel;
        private int reModelCount;

        private void setCurrentModel(IR_Model irModel) {
            currentModel = irModel;
        }
        private IR_Model getCurrentModel() {
            return currentModel;
        }
        private int getTotalTen() {
            return totalTen;
        }
        private int getReModelCount() {
            return reModelCount;
        }

        private void parseLine(String line) {

            String id = line.substring(0, 2);

            switch (id) {
                case "IR":
                    currentModel = new IR_Model();
                    currentModel.parseLine(line);
                    totalTen = 0;
                case "RE":
                    RE_Model reModel = new RE_Model();
                    reModel.parseLine(line);
                    currentModel.addReModel(reModel);
                    hasHO = false;
                    reModelCount++;
                    break;
                case "HO":
                    HO_Model hoModel = new HO_Model();
                    hoModel.parseLine(line);
                    currentModel.getCurrentREModel().setHOModel(hoModel);
                    totalTen += hoModel.getTen();
                    hasHO = true;
                    break;
                case "KO":
                    KO_Model koModel = new KO_Model();
                    koModel.parseLine(line);
                    currentModel.getCurrentREModel().addKOModel(koModel);
                    if (!hasHO) {   //  TODO
                        totalTen += koModel.getTen();
                    }
                    break;
                case "KH":
                    KH_Model khModel = new KH_Model();
                    khModel.parseLine(line);
                    currentModel.getCurrentREModel().setKHModel(khModel);
                    break;
                case "SY":
                    SY_Model syModel = new SY_Model();
                    syModel.parseLine(line);
                    currentModel.getCurrentREModel().addSYModel(syModel);
                    break;
                case "SI":
                    SI_Model siModel = new SI_Model();
                    siModel.parseLine(line);
                    currentModel.getCurrentREModel().addItem(siModel);
                    break;
                case "IY":
                    IY_Model iyModel = new IY_Model();
                    iyModel.parseLine(line);
                    currentModel.getCurrentREModel().addItem(iyModel);
                    break;
                case "TO":
                    TO_Model toModel = new TO_Model();
                    toModel.parseLine(line);
                    currentModel.getCurrentREModel().addItem(toModel);
                    break;
                case "CO":
                    CO_Model coModel = new CO_Model();
                    coModel.parseLine(line);
                    currentModel.getCurrentREModel().addItem(coModel);
                    break;
                case "SJ":
                    SJ_Model sjModel = new SJ_Model();
                    sjModel.parseLine(line);
                    currentModel.getCurrentREModel().addSJModel(sjModel);
                    break;
                case "GO":
                    GO_Model goModel = new GO_Model();
                    goModel.parseLine(line);
                    currentModel.setGOModel(goModel);
                    break;
                default:
                    break;
            }
        }
    }
    
    private void sortByPatientId(List<IR_Model> irModelList) {
        for (IR_Model irModel : irModelList) {
            List<RE_Model> reModels = irModel.getReModelList();
            Collections.sort(reModels, new RE_ModelComparator());
        }
    }
    
    // マスターを参照して項目名を設定する
    private void processSYModel(List<IR_Model> irModelList) {
        
        SqlMiscDao dao = SqlMiscDao.getInstance();
        
        // 病名コードを列挙しORCAから取得
        Set<String> srycds = new HashSet<>();
        for (IR_Model irModel : irModelList) {
            for (RE_Model reModel : irModel.getReModelList()) {
                for (SY_Model syModel : reModel.getSYModelList()) {
                    String srycd = String.valueOf(syModel.getSrycd());
                    // 未コード化病名の場合はスキップ
                    if (UNCODED_DIAG_SRYCD.equals(srycd)) {
                        continue;
                    }
                    srycds.add(srycd);
                    // 修飾語を処理する
                    String modifier = syModel.getModifier();
                    if (modifier != null && !modifier.isEmpty()) {
                        // modifierは４ケタ数字の連続
                        for (int i = 0; i < modifier.length(); i += 4) {
                            String str = modifier.substring(i, i + 4);
                            // ORCAではZZZxxxxと記録されている
                            srycds.add(MODIFIER_PREFIX + str);
                        }
                    }
                }
            }
        }
        
        // いったんHashMapに登録する
        List<DiseaseEntry> list = dao.getDiseaseEntries(srycds);
        Map<String, DiseaseEntry> map = new HashMap<>();
        for (DiseaseEntry de : list) {
            map.put(de.getCode(), de);
        }
        list.clear();
        srycds.clear();
        
        // 傷病名を構築する
        for (IR_Model irModel : irModelList) {
            for (RE_Model reModel : irModel.getReModelList()) {
                for (SY_Model syModel : reModel.getSYModelList()) {
                    String srycd = String.valueOf(syModel.getSrycd());
                    // 未コード化病名はスキップ
                    if (UNCODED_DIAG_SRYCD.equals(srycd)) {
                        continue;
                    }
                    DiseaseEntry de = map.get(srycd);
                    // 修飾語がある場合に再構築する
                    String modifier = syModel.getModifier();
                    if (modifier != null && !modifier.isEmpty()) {
                        boolean pre = true;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < modifier.length(); i += 4) {
                            String str = modifier.substring(i, i + 4);
                            // 後置修飾語は8から始まる
                            if (pre && str.startsWith("8")) {
                                sb.append(de.getName());
                                pre = false;
                            }
                            DiseaseEntry dem = map.get("ZZZ" + str);
                            sb.append(dem.getName());
                        }
                        if (pre) {
                            sb.append(de.getName());
                        }
                        syModel.setDiagName(sb.toString());
                    } else {
                        syModel.setDiagName(de.getName());
                    }
                    syModel.setByoKanrenKbn(de.getByoKanrenKbn());
                }
            }
        }
    }

    // マスターを参照して項目名を設定する
    private void processIRezeItem(List<IR_Model> irModelList) {

        // 診療行為・薬剤など
        SqlMiscDao dao = SqlMiscDao.getInstance();
        itemSrycdSet = new HashSet<>();
        Set<String> diagSrycds = new HashSet<>();

        // 診療行為コードを列挙しORCAから取得
        for (IR_Model irModel : irModelList) {
            for (RE_Model reModel : irModel.getReModelList()) {
                for (IRezeItem item : reModel.getItemList()) {
                    // レセ電は、ほんま、クソ仕様だわ
                    String srycd = String.valueOf(item.getSrycd());
                    if ("890000001".equals(srycd)) {
                        CO_Model coModel = (CO_Model) item;
                        // modifierは４ケタ数字の連続
                        String modifier = coModel.getComment();
                        for (int i = 0; i < modifier.length(); i += 4) {
                            String str = modifier.substring(i, i + 4);
                            // ORCAではZZZxxxxと記録されている
                            diagSrycds.add(MODIFIER_PREFIX + str);
                        }
                    } else {
                        itemSrycdSet.add(srycd);
                    }
                }
            }
        }
        
        // いったんHashMapに登録する
        List<TensuMaster> tmList = dao.getTensuMasterList(itemSrycdSet);
        Map<String, TensuMaster> map = new HashMap<>();
        for (TensuMaster tm : tmList) {
            map.put(tm.getSrycd(), tm);
        }
        tmList.clear();
        
        // 部位コードを取得
        List<DiseaseEntry> list = dao.getDiseaseEntries(diagSrycds);
        Map<String, DiseaseEntry> deMap = new HashMap<>();
        for (DiseaseEntry de : list) {
            deMap.put(de.getCode(), de);
        }
        list.clear();
        diagSrycds.clear();
        
        // 診療行為名をセットする
        for (IR_Model irModel : irModelList) {
            for (RE_Model reModel : irModel.getReModelList()) {
                for (IRezeItem item : reModel.getItemList()) {
                    String srycd = item.getSrycd();
                    TensuMaster tm = map.get(srycd);
                    // コメントコード
                    if (item instanceof CO_Model) {
                        CO_Model coModel = (CO_Model) item;
                        reconstructComment(tm, coModel, deMap);
                    } else {
                        item.setDescription(tm.getName());
                    }
                }
            }
        }
        
        deMap.clear();
    }
    
    // コメントコード内容を再構築
    private void reconstructComment(TensuMaster tm, CO_Model coModel, Map<String, DiseaseEntry> deMap) {

        String srycd = coModel.getSrycd();

        if (srycd.startsWith("81")) {
            coModel.setDescription(coModel.getComment());
        } else if (srycd.startsWith("82")) {
            coModel.setDescription(tm.getName());
        } else if (srycd.startsWith("83")) {
            StringBuilder sb = new StringBuilder();
            sb.append(tm.getName());
            sb.append(coModel.getComment());
            coModel.setDescription(sb.toString());
        } else if (srycd.startsWith("84")) {
            reconstruct84Comment(tm, coModel);
        } else if (srycd.startsWith("89")) {
            reconstruct89Comment(coModel, deMap);
        }
    }
    
    private void reconstruct84Comment(TensuMaster tm, CO_Model coModel) {
        
        String tmName = tm.getName();
        String comment = coModel.getComment();
        
        if (tmName.isEmpty()) {
            coModel.setDescription(comment);
        }
        if (comment.isEmpty()) {
            coModel.setDescription(tmName);
        }
        
        // 逆順に空白をコメントで置換する
        StringBuilder sb = new StringBuilder();
        int index = comment.length() - 1;

        for (int i = tmName.length() - 1; i >= 0; --i) {
            char c = tmName.charAt(i);
            if (c == '　' && index >= 0) {
                sb.append(comment.charAt(index--));
            } else {
                sb.append(c);
            }
        }
        String str = sb.reverse().toString();
        coModel.setDescription(str);
    }
    
    
    private void reconstruct89Comment(CO_Model coModel, Map<String, DiseaseEntry> deMap) {

        String modifier = coModel.getComment();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modifier.length(); i += 4) {
            String str = modifier.substring(i, i + 4);
            DiseaseEntry dem = deMap.get("ZZZ" + str);
            sb.append(dem.getName());
        }
        coModel.setDescription(sb.toString());
    }
    
    // patient id順のcomparator
    private static class RE_ModelComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            String pId1 = ((RE_Model) o1).getPatientId();
            String pId2 = ((RE_Model) o2).getPatientId();
            return pId1.compareTo(pId2);
        }
    }

}
