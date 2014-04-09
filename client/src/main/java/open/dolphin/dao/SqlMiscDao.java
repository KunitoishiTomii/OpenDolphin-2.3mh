package open.dolphin.dao;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.DrugInteractionModel;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.order.MasterItem;
import open.dolphin.project.Project;
import open.dolphin.util.MMLDate;

/**
 * ORCAに問い合わせる諸々
 *
 * @author masuda, Masuda Naika
 */
public final class SqlMiscDao extends SqlDaoBean {

    private static final SqlMiscDao instance;

    // srycdと検査等実施判断グループ区分のマップ
    private final Map<String, Integer> hokatsuKbnMap;

    static {
        instance = new SqlMiscDao();
    }

    public static SqlMiscDao getInstance() {
        return instance;
    }

    private SqlMiscDao() {
        hokatsuKbnMap = new HashMap<>();
    }

    // 検査等実施判断グループ区分を調べる
    public Map<String, Integer> getHokatsuKbnMap(List<String> srycds) throws DaoException {

        List<String> srycdsToGet = new ArrayList<>();
        for (String srycd : srycds) {
            if (!hokatsuKbnMap.containsKey(srycd)) {
                srycdsToGet.add(srycd);
            }
        }
        if (srycdsToGet.isEmpty()) {
            return hokatsuKbnMap;
        }

        int hospNum = getHospNum();
        StringBuilder sb = new StringBuilder();
        sb.append("select srycd, houksnkbn from tbl_tensu");
        sb.append(" where yukoedymd = '99999999'");
        sb.append(" and srycd in (").append(getCodes(srycdsToGet)).append(")");
        sb.append(" and hospnum = ").append(String.valueOf(hospNum));

        final String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        for (String[] values : valuesList) {
            String srycd = values[0];
            String kbnStr = values[1];
            int kbn = (kbnStr != null) ? Integer.parseInt(kbnStr) : 0;
            hokatsuKbnMap.put(srycd, kbn);
        }

        return hokatsuKbnMap;
    }

    // 入院中の患者を検索し入院モデルを作成する
    public List<AdmissionModel> getInHospitalPatients(Date date) throws DaoException {

        final String sql = "select TP.ptnum, TN.brmnum, TN.nyuinka, TN.nyuinymd , TN.drcd1 "
                + "from tbl_ptnyuinrrk TN inner join tbl_ptnum TP on TP.ptid = TN.ptid "
                + "where TN.tennyuymd <= ? and ? <= TN.tenstuymd and TN.hospnum = ?";

        SimpleDateFormat frmt = new SimpleDateFormat("yyyyMMdd");
        String dateStr = frmt.format(date);
        int hospNum = getHospNum();

        List<AdmissionModel> ret = new ArrayList<>();

        Object[] params = {dateStr, dateStr, hospNum};

        List<String[]> valuesList = executePreparedStatement(sql, params);

        for (String[] values : valuesList) {
            String patientId = values[0].trim();
            String room = getRoomNumber(values[1].trim());
            String dept = getDepartmentDesc(values[2].trim());
            Date aDate = null;
            try {
                aDate = frmt.parse(values[3].trim());
            } catch (ParseException ex) {
            }
            String doctor = getOrcaStaffName(values[4].trim());

            // 新たに作成
            AdmissionModel model = new AdmissionModel();
            model.setId(0L);
            model.setPatientId(patientId);
            model.setRoom(room);
            model.setDepartment(dept);
            model.setStarted(aDate);
            model.setDoctorName(doctor);
            ret.add(model);
        }

        return ret;
    }

    private String getOrcaStaffName(String code) {
        String staffName = SyskanriInfo.getInstance().getOrcaStaffName(code);
        return staffName;
    }

    private String getRoomNumber(String str) {

        // 病棟番号を除去
        str = str.substring(2);
        // 先頭のゼロを除去
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (found) {
                sb.append(c);
            } else if (c != '0') {
                found = true;
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getDepartmentDesc(String code) {
        return SyskanriInfo.getInstance().getOrcaDeptDesc(code.trim());
    }

    public List<DrugInteractionModel> checkInteraction(Collection<String> src1, 
            Collection<String> src2) throws DaoException {
        // 引数はdrugcdの配列ｘ２

        if (src1 == null || src1.isEmpty() || src2 == null || src2.isEmpty()) {
            return Collections.emptyList();
        }

        // コードが後発品ならばその先発品のコードを追加する
        Collection<String> allDrug = new HashSet<>();
        allDrug.addAll(src1);
        allDrug.addAll(src2);

        // ゾロと先発の対応リストを作成する one-to-many
        List<ZoroBrandPair> zoroBrandList = getZoroBrandPair(allDrug);

        Collection<String> drug1 = new ArrayList<>(src1);
        for (String srycd : src1) {
            List<ZoroBrandPair> pairs = getBrands(zoroBrandList, srycd);
            for (ZoroBrandPair pair : pairs) {
                drug1.add(pair.brandSrycd);
            }
        }

        Collection<String> drug2 = new ArrayList<>(src2);
        for (String srycd : src2) {
            List<ZoroBrandPair> pairs = getBrands(zoroBrandList, srycd);
            for (ZoroBrandPair pair : pairs) {
                drug2.add(pair.brandSrycd);
            }
        }

        StringBuilder sb = new StringBuilder();
        Map<String, DrugInteractionModel> map = new HashMap<>();

        // SQL文を作成
        sb.append("select drugcd, drugcd2, TI.syojyoucd, syojyou ");
        sb.append("from tbl_interact TI, tbl_sskijyo TS where TI.syojyoucd = TS.syojyoucd ");
        sb.append("and (drugcd in (");
        sb.append(getCodes(drug1));
        sb.append(") and drugcd2 in (");
        sb.append(getCodes(drug2));
        sb.append("))");
        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        for (String[] values : valuesList) {
            String srycd1 = values[0];
            String srycd2 = values[1];
            String sskijo = values[2];
            String syojoucd = values[3];
            String brandName1 = null;
            String brandName2 = null;

            ZoroBrandPair zoro = getZoro(zoroBrandList, srycd1);
            // ゾロのエイリアスとしての先発薬の場合
            if (zoro != null) {
                List<ZoroBrandPair> brands = getBrands(zoroBrandList, zoro.zoroSrycd);
                if (!brands.isEmpty()) {
                    srycd1 = zoro.zoroSrycd;
                    boolean first = true;
                    sb = new StringBuilder();
                    for (ZoroBrandPair pair : brands) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(pair.brandName);
                    }
                    brandName1 = sb.toString();
                }
            }

            zoro = getZoro(zoroBrandList, srycd2);
            if (zoro != null) {
                List<ZoroBrandPair> brands = getBrands(zoroBrandList, zoro.zoroSrycd);
                if (!brands.isEmpty()) {
                    srycd2 = zoro.zoroSrycd;
                    boolean first = true;
                    sb = new StringBuilder();
                    for (ZoroBrandPair pair : brands) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(pair.brandName);
                    }
                    brandName2 = sb.toString();
                }
            }
            map.put(srycd1, new DrugInteractionModel(
                    srycd1, srycd2, sskijo, syojoucd, brandName1, brandName2));
        }

        List<DrugInteractionModel> ret = new ArrayList<>(map.values());
        map.clear();

        return ret;
    }

    private static class ZoroBrandPair {

        String zoroSrycd;
        String zoroName;
        String brandSrycd;
        String brandName;
    }

    private ZoroBrandPair getZoro(List<ZoroBrandPair> list, String brandSrycd) {
        for (ZoroBrandPair pair : list) {
            if (brandSrycd.equals(pair.brandSrycd)) {
                return pair;
            }
        }
        return null;
    }

    private List<ZoroBrandPair> getBrands(List<ZoroBrandPair> list, String zoroSrycd) {
        List<ZoroBrandPair> ret = new ArrayList<>();
        for (ZoroBrandPair pair : list) {
            if (zoroSrycd.equals(pair.zoroSrycd)) {
                ret.add(pair);
            }
        }
        return ret;
    }

    private List<ZoroBrandPair> getZoroBrandPair(Collection codes) throws DaoException {

        List<ZoroBrandPair> ret = new ArrayList<>();

        // 後発薬の薬価基準コードを取得する。先頭９ケタ
        StringBuilder sb = new StringBuilder();
        sb.append("select distinct srycd,name,yakkakjncd from tbl_tensu where srycd in (");
        sb.append(getCodes(codes));
        sb.append(") and kouhatukbn='1' and yukoedymd='99999999'");
        String sql = sb.toString();

        Map<String, ZoroBrandPair> yakkakjncdMap = new HashMap<>();
        List<String[]> valuesList = executeStatement(sql);
        for (String[] values : valuesList) {
            ZoroBrandPair pair = new ZoroBrandPair();
            pair.zoroSrycd = values[0];
            pair.zoroName = values[1];
            // javaのsubstringはyakkakjncd.substring(0,9)である
            String yakkakjncd = values[2].substring(0, 9);
            yakkakjncdMap.put(yakkakjncd, pair);
        }

        if (yakkakjncdMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 先発薬を調べる
        sb = new StringBuilder();
        // sqlのsubstringはsubstring(yakkakjncd,0,10)である
        sb.append("select distinct srycd,name,yakkakjncd from tbl_tensu where substring(yakkakjncd,0,10) in (");
        sb.append(getCodes(yakkakjncdMap.keySet()));
        sb.append(") and kouhatukbn<>'1' and yukoedymd='99999999'");
        sql = sb.toString();
        valuesList = executeStatement(sql);
        for (String[] values : valuesList) {
            String brandSrycd = values[0];
            String brandName = values[1];
            String yakkakjncd = values[2].substring(0, 9);
            ZoroBrandPair mapPair = yakkakjncdMap.get(yakkakjncd);
            if (mapPair != null) {
                ZoroBrandPair pair = new ZoroBrandPair();
                pair.brandSrycd = brandSrycd;
                pair.brandName = brandName;
                pair.zoroSrycd = mapPair.zoroSrycd;
                pair.zoroName = mapPair.zoroName;
                ret.add(pair);
            }
        }

        yakkakjncdMap.clear();

        return ret;
    }

    // srycdからgairaiKanriKbnの有無をチェックする。算定チェックに利用
    public boolean hasGairaiKanriKbn(Collection<String> srycdList) throws DaoException {

        if (srycdList == null || srycdList.isEmpty()) {
            return false;
        }
        int hospNum = getHospNum();

        StringBuilder sb = new StringBuilder();
        sb.append("select gaikanrikbn from tbl_tensu");
        sb.append(" where yukoedymd = '99999999'");
        sb.append(" and gaikanrikbn = 1");      //１：外来管理加算が算定できない診療行為
        sb.append(" and hospnum = ").append(String.valueOf(hospNum));
        sb.append(" and srycd in (").append(getCodes(srycdList)).append(")");

        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        boolean ret = !valuesList.isEmpty();

        return ret;
    }

    // srycdから腫瘍マーカー検査の有無をチェックする。算定チェックに利用
    public boolean hasTumorMarkers(Collection<String> srycdList) throws DaoException {

        if (srycdList == null || srycdList.isEmpty()) {
            return false;
        }

        int hospNum = getHospNum();

        StringBuilder sb = new StringBuilder();
        sb.append("select houksnkbn from tbl_tensu");
        sb.append(" where yukoedymd = '99999999'");
        sb.append(" and houksnkbn = 5");                //５：腫瘍マーカー
        sb.append(" and hospnum = ").append(String.valueOf(hospNum));
        sb.append(" and srycd in (").append(getCodes(srycdList)).append(")");

        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        boolean ret = !valuesList.isEmpty();

        return ret;
    }

    /*  もっさり。。。
     // srycdからTensuMasterをまとめて取得。LaboTestPanel, BaseEditor, RpEditor, ImportOrcaMedicine, CheckSanteiで利用
     public List<TensuMaster> getTensuMasterList(List<String> srycdList) {

     if (srycdList == null || srycdList.isEmpty()) {
     return null;
     }

     // 結果を格納するリスト
     List<TensuMaster> ret = new ArrayList<TensuMaster>();
        
     StringBuilder sb = new StringBuilder();
     sb.append(SELECT_TBL_TENSU2);
     sb.append("where t.hospnum = ");
     sb.append(String.valueOf(getHospNum()));
     sb.append(" and ");
     sb.append("t.srycd in (");
     sb.append(getCodes(srycdList));
     sb.append(")");
     sb.append(" and ");
     sb.append(" t.yukoedymd = (select max(t2.yukoedymd) from tbl_tensu t2 where t.srycd = t2.srycd group by t2.srycd)");
     String sql = sb.toString();

     Connection con = null;
     Statement st = null;

     try {
     con = getConnection();
     st = con.createStatement();
     ResultSet rs = st.executeQuery(sql);

     while (rs.next()) {
     TensuMaster tm = getTensuMaster(rs);
     ret.add(tm);
     }
     rs.close();
     closeStatement(st);
     closeConnection(con);
     return ret;

     } catch (Exception e) {
     processError(e);
     closeStatement(st);
     closeConnection(con);
     }
     return null;
     }
     */
    // srycdからTensuMasterをまとめて取得。LaboTestPanel, BaseEditor, RpEditor, ImportOrcaMedicine, CheckSanteiで利用
    public List<TensuMaster> getTensuMasterList(Collection<String> srycdList) throws DaoException {

        if (srycdList == null || srycdList.isEmpty()) {
            return Collections.emptyList();
        }

        int hospNum = getHospNum();
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_TBL_TENSU);
        sb.append("where hospnum = ").append(String.valueOf(hospNum));
        sb.append(" and srycd in (").append(getCodes(srycdList)).append(")");
        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        List<TensuMaster> tmList = new ArrayList();
        for (String[] values : valuesList) {
            TensuMaster tm = getTensuMaster(values);
            tmList.add(tm);
        }

        return filterTensuMaster(tmList);
    }

    // 古いTensuMasterを振るい落とす
    public List<TensuMaster> filterTensuMaster(List<TensuMaster> list) {

        int todayYmd = MMLDate.getTodayInt();
        Map<String, TensuMaster> map = new HashMap<>();

        for (TensuMaster test : list) {
            String srycd = test.getSrycd();
            TensuMaster exist = map.get(srycd);
            if (exist == null) {
                map.put(srycd, test);
            } else {
                int existEdYmd = Integer.parseInt(exist.getYukoedymd());
                int testStYmd = Integer.parseInt(test.getYukostymd());
                int testEdYmd = Integer.parseInt(test.getYukoedymd());
                if (testStYmd <= todayYmd && testEdYmd > existEdYmd) {
                    map.put(srycd, test);
                }
            }
        }

        List<TensuMaster> ret = new ArrayList<>(map.values());
        map.clear();

        return ret;
    }

    // 傷病名コードからまとめてDiseaseEntryを取得
    public List<DiseaseEntry> getDiseaseEntries(Collection<String> srycdList) throws DaoException {

        if (srycdList == null || srycdList.isEmpty()) {
            return Collections.emptyList();
        }
        String selectSql = SyskanriInfo.getInstance().isOrca45()
                ? SELECT_TBL_BYOMEI.replace("icd10_1", "icd10")
                : SELECT_TBL_BYOMEI;

        StringBuilder sb = new StringBuilder();
        sb.append(selectSql);
        sb.append("where byomeicd in (").append(getCodes(srycdList)).append(")");
        String sql = sb.toString();

        List<DiseaseEntry> collection = new ArrayList<>();
        List<DiseaseEntry> outUse = new ArrayList<>();

        List<String[]> valuesList = executeStatement(sql);
        for (String[] values : valuesList) {
            DiseaseEntry de = getDiseaseEntry(values);
            if (de.isInUse()) {
                collection.add(de);
            } else {
                outUse.add(de);
            }
        }
        collection.addAll(outUse);

        return collection;
    }

    // 患者の処方をORCAから取り出してMasterItemのリストとして返す
    public List<MasterItem> getMedMasterItemFromOrca(String patientId, String visitYMD) throws DaoException {
        
        final String ADMIN_MARK = "[用法] ";

        List<MasterItem> list = new ArrayList<>();
        Set<String> srycdSet = new HashSet<>();

        String sryYM = visitYMD.substring(0, 6);
        String sryD = String.valueOf(Integer.parseInt(visitYMD.substring(6)));
        long ptid = getOrcaPtID(patientId);

        // まずは調べたい日の処方のコード・数量、用法のコード・日数をピックアップ
        String dayColumn = "day_" + sryD;

        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("act.srycd1,act.srysuryo1,act.srykaisu1,inputnum1,");
        sb.append("act.srycd2,act.srysuryo2,act.srykaisu2,inputnum2,");
        sb.append("act.srycd3,act.srysuryo3,act.srykaisu3,inputnum3,");
        sb.append("act.srycd4,act.srysuryo4,act.srykaisu4,inputnum4,");
        sb.append("act.srycd5,act.srysuryo5,act.srykaisu5,inputnum5,");
        sb.append("main.").append(dayColumn).append(",");
        sb.append("main.zaikaisu, act.zainum");
        sb.append(" from tbl_sryact act, tbl_sryacct_main main");
        sb.append(" where act.zainum=main.zainum and act.ptid=main.ptid");
        sb.append(" and act.srysyukbn~'2[1239]'");
        sb.append(" and main.ptid=").append(String.valueOf(ptid));
        sb.append(" and main.sryym=").append(addSingleQuote(sryYM));
        sb.append(" and main.").append(dayColumn).append("<>0");
        sb.append(" order by act.zainum, act.rennum");
        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);
        DecimalFormat frmt = new DecimalFormat("0.###");

        for (String[] values : valuesList) {

            boolean hasAdmin = false;
            int dayColumnValue = Integer.valueOf(values[20]);
            int zaiKaisu = Integer.valueOf(values[21]);
            int zainum = Integer.valueOf(values[22]);

            for (int i = 0; i < 20; i += 4) {

                // 外用薬ならsrykaisu=1、内服ならsrykaisu = 0
                // 「２つ目の数量（分画数）がある場合それを表す」の意味は理解できなかったｗ
                String srycd = values[i].trim();
                // srycdが空白なら剤の終了
                if (srycd.isEmpty()) {
                    break;
                }
                srycdSet.add(srycd);

                // 数量はfloatにしないと、0.5錠とかNGになる
                float srysuryo = Float.parseFloat(values[i + 1]);
                int srykaisu = Integer.parseInt(values[i + 2]);
                int inputnum = Integer.parseInt(values[i + 3]);

                if (srycd.startsWith("001")) {
                    // srycdが001から始まっていたら用法コード
                    // 内服なら'day_DD'のカラムから日数を、外用ならsrykaisuを返す
                    // 同日に同じ処方があると、その処方の日数は合計になる。
                    // tbl_sryacct_subを参照すれば分離できるが、めんどくさいｗ
                    srysuryo = (srykaisu == 0) ? dayColumnValue : srykaisu;
                    MasterItem mi = new MasterItem();
                    mi.setCode(srycd);
                    mi.setBundleNumber(frmt.format(srysuryo));
                    list.add(mi);
                    hasAdmin = true;
                } else {
                    // 普通の薬とコメント
                    MasterItem mi = new MasterItem();
                    mi.setCode(srycd);
                    mi.setNumber(frmt.format(srysuryo));
                    // コメントの入力値がある場合
                    if (inputnum != 0) {
                        mi.setInputnum(inputnum);
                        mi.setZainum(zainum);
                    }
                    list.add(mi);
                }
            }

            // 「薬剤コード」△「数量」＊「回数」の対応
            // 取り敢えず　医師の指示通りにしておく。ただしこれは屯用になるが。
            if (!hasAdmin) {
                final String srycd = "001000101";
                MasterItem mi = new MasterItem();
                mi.setCode(srycd);
                mi.setNumber(frmt.format(zaiKaisu));
                list.add(mi);
                srycdSet.add(srycd);
            }
        }

        // 調べたsrycdに応じたTensuMasterを取得して、MasterItemのリストに追加する。
        // まずはTensuMasterをまとめて取得しHashMapに登録する
        int len = list.size();
        if (len == 0) {
            return Collections.emptyList();
        }

        // ORCAに問い合わせ
        List<TensuMaster> tmList = getTensuMasterList(srycdSet);
        // HashMapに登録
        HashMap<String, TensuMaster> tensuMasterMap = new HashMap<>();
        for (TensuMaster tm : tmList) {
            tensuMasterMap.put(tm.getSrycd(), tm);
        }

        // reconstruct
        for (MasterItem mi : list) {
            String srycd = mi.getCode();
            TensuMaster tm = tensuMasterMap.get(srycd);
            mi.setDataKbn(tm.getDataKbn());

            if (srycd.matches(ClaimConst.REGEXP_COMMENT_MED)) {
                // コメントコード
                mi.setClassCode(ClaimConst.OTHER);
                mi.setUnit(tm.getTaniname());
                mi.setName(tm.getName());
                mi.setYkzKbn(tm.getYkzkbn());
                mi.setNumber(null);
                mi.setBundleNumber(null);
                processComment(ptid, mi);
            } else if (srycd.startsWith("001")) {
                // 用法コードのTensuMasterを作成
                mi.setClassCode(ClaimConst.ADMIN);
                mi.setName(ADMIN_MARK + tm.getName());
                mi.setDummy("X");
                mi.setNumber(null);
            } else {
                // 薬剤コードのTensuMasterを作成
                mi.setClassCode(ClaimConst.YAKUZAI);
                mi.setUnit(tm.getTaniname());
                mi.setName(tm.getName());
                mi.setYkzKbn(tm.getYkzkbn());
            }
        }

        srycdSet.clear();
        tensuMasterMap.clear();

        return list;
    }
    
    private void processComment(long ptid, MasterItem mi) throws DaoException {

        int zainum = mi.getZainum();
        int inputnum = mi.getInputnum();    // inputnum = rennum
        if (inputnum == 0) {
            return;
        }

        final String sql = "select inputcoment, inputchi1, inputchi2, inputchi3, inputchi4, inputchi5 "
                + " from tbl_ptcom where ptid = ? and zainum = ? and rennum = ?";        
        Object[] params = {ptid, zainum, inputnum};

        List<String[]> valuesList = executePreparedStatement(sql, params);

        String srycd = mi.getCode();

        for (String[] values : valuesList) {

            if ("810000001".equals(srycd) || srycd.matches("008[356].*")
                    || srycd.matches("8[356].*")) {
                // 編集可能なコメントコードの場合は編集したものに置き換える
                mi.setName(values[0]);

            } else if (srycd.startsWith("0084") || srycd.startsWith("84")) {
                // 84コメントの場合はnameはマスターのままで数量を1-1-1のように設定する
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (int i = 1; i < values.length; ++i) {
                    String inputchi = values[i];
                    if (inputchi != null && !inputchi.isEmpty()) {
                        if (!first) {
                            sb.append("-");
                        } else {
                            first = false;
                        }
                        sb.append(inputchi);
                    }
                }
                String num = sb.toString();
                if (!num.isEmpty()) {
                    mi.setNumber(num);
                }
            }
        }
    }

    // 期間内の受診日を取得する。返り値は"YYYYMMDD"形式の文字列リスト
    public List<String> getOrcaVisit(String patientId, String startDate, String endDate, 
            boolean desc, String search) throws DaoException {

        List<String> orcaVisit = new ArrayList<>();
        long ptid = getOrcaPtID(patientId);

        StringBuilder sb = new StringBuilder();
        sb.append("select sryymd");
        for (int i = 1; i <= 15; ++i) {
            sb.append(",zainum");
            sb.append(String.valueOf(i));
        }
        sb.append(" from tbl_jyurrk where to_date(sryymd,'YYYYMMDD')");
        sb.append(" between to_date(").append(addSingleQuote(startDate)).append(",'YYYYMMDD')");
        sb.append(" and to_date(").append(addSingleQuote(endDate)).append(",'YYYYMMDD')");
        sb.append(" and ptid=").append(String.valueOf(ptid));
        if ("medOrder".equals(search)) {
            sb.append(" and (srykbn2 = '01' or srykbn3 = '01' or srykbn4 = '01')");
        }
        sb.append(" order by rennum asc, to_date(sryymd,'YYYYMMDD')");
        if (desc) {
            sb.append(" desc");
        }
        String sql = sb.toString();

        List<String[]> valuesList = executeStatement(sql);

        for (String[] values : valuesList) {
            StringBuilder sb1 = new StringBuilder();
            sb1.append(values[0].substring(0, 4)).append("-");
            sb1.append(values[0].substring(4, 6)).append("-");
            sb1.append(values[0].substring(6, 8));
            orcaVisit.add(sb1.toString());
        }

        return orcaVisit;
    }

    public long getTableRowCount(String tableName) throws DaoException {

        long count = 0;

        String sql = "select count(*) from " + tableName;

        List<String[]> valuesList = executeStatement(sql);
        if (!valuesList.isEmpty()) {
            String[] values = valuesList.get(0);
            count = Long.parseLong(values[0]);
        }

        return count;
    }

    public List<String[]> getRecedenCsv(String ymStr, String nyugaikbn, int teisyutusaki) throws DaoException {

        final String sql = "select recedata, totalten from tbl_receden"
                + " where sryym = ? and nyugaikbn = ? and teisyutusaki = ? and hospnum = ?"
                + " order by nyugaikbn, ptid, rennum";

        int hospNum = getHospNum();
        int ym = Integer.parseInt(ymStr);
        Object[] params = {ym, nyugaikbn, teisyutusaki, hospNum};
        List<String[]> valuesList = executePreparedStatement(sql, params);

        for (String[] values : valuesList) {
            values[0] = values[0].trim();
            values[1] = values[1].trim();
        }

        return valuesList;
    }

    public IndicationModel getTekiouByomei(String srycd) throws DaoException {

        final String sql1 = "select byomei from tbl_tekioubyomei"
                + " where srycd = ? order by rennum";
        // chkkbn 1:医薬品と病名 2:診療行為と病名 6:投与禁忌医薬品と病名
        final String sql2 = "select byomei from tbl_chksnd"
                + " where srycd = ? and (chkkbn = '1' or chkkbn = '2') order by rennum";
        final String sql3 = "select srycd, byomei from tbl_chksnd"
                + " where srycd = ? and chkkbn = '6' order by rennum";
        // tbl_chk005
        final String sql4 = "select byomei from tbl_chk005"
                + " where srycd = ? order by rennum";

        IndicationModel model = new IndicationModel();
        model.setSrycd(srycd);
        model.setOutPatient(true);
        model.setAdmission(true);
        model.setInclusive(false);
        model.setFacilityId(Project.getFacilityId());
        model.setIndicationItems(new ArrayList<IndicationItem>());

        Object[] params = {srycd};

        List<String[]> valuesList1 = executePreparedStatement(sql1, params);
        for (String[] values : valuesList1) {
            IndicationItem item = new IndicationItem();
            item.setKeyword(values[0].trim());
            item.setIndicationModel(model);
            addNewIndication(model, item);
        }

        List<String[]> valuesList2 = executePreparedStatement(sql2, params);
        for (String[] values : valuesList2) {
            IndicationItem item = new IndicationItem();
            item.setKeyword(values[0].trim());
            item.setIndicationModel(model);
            addNewIndication(model, item);
        }

        List<String[]> valuesList3 = executePreparedStatement(sql3, params);
        for (String[] values : valuesList3) {
            IndicationItem item = new IndicationItem();
            item.setKeyword(values[0].trim());
            item.setNotCondition(true);
            item.setIndicationModel(model);
            addNewIndication(model, item);
        }

        List<String[]> valuesList4 = executePreparedStatement(sql4, params);
        for (String[] values : valuesList4) {
            IndicationItem item = new IndicationItem();
            item.setKeyword(values[0].trim());
            item.setIndicationModel(model);
            addNewIndication(model, item);
        }

        return model;
    }

    private void addNewIndication(IndicationModel model, IndicationItem newItem) {

        boolean found = false;
        for (IndicationItem item : model.getIndicationItems()) {
            if (isSameIndicationItem(item, newItem)) {
                found = true;
                break;
            }
        }
        if (!found) {
            model.getIndicationItems().add(newItem);
        }
    }

    private boolean isSameIndicationItem(IndicationItem item1, IndicationItem item2) {

        boolean same = true;

        if (item1 == null || item2 == null) {
            return false;
        }
        if (item1.getKeyword() == null) {
            return false;
        }
        same &= item1.getKeyword().equals(item2.getKeyword());
        same &= item1.isNotCondition() == item2.isNotCondition();

        return same;
    }
    
    // もっと自由にDolphinを！
    public Map<String, String> getPtKanaName(Collection<String> ptIds) throws DaoException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("select num.ptnum, inf.kananame from tbl_ptnum num, tbl_ptinf inf ");
        sb.append("where num.ptid = inf.ptid and num.ptnum in (");
        sb.append(getCodes(ptIds)).append(") ");
        sb.append("and num.hospnum =").append(String.valueOf(getHospNum()));
        final String sql = sb.toString();
        
        List<String[]> valuesList = executeStatement(sql);
        Map<String, String> ptIdKanaMap = new HashMap<>();
        
        for (String[] values : valuesList) {
            ptIdKanaMap.put(values[0].trim(), values[1].trim());
        }
        
        return ptIdKanaMap;
    }
}
