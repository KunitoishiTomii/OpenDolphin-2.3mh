package open.dolphin.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import open.dolphin.infomodel.ETensuModel1;
import open.dolphin.infomodel.ETensuModel2;
import open.dolphin.infomodel.ETensuModel3;
import open.dolphin.infomodel.ETensuModel4;
import open.dolphin.infomodel.ETensuModel5;

/**
 * 電子点数表を参照するDAO
 * 
 * @author masuda, Masuda Naika
 */
public class SqlETensuDao extends SqlDaoBean {
    
    private static final String TBL_ETENSU_1 = "tbl_etensu_1";
    
    private static final String SELECT_ETENSU_1
            = "select srycd, yukostymd, yukoedymd, "
            + "h_tani1, h_group1, h_tani2, h_group2, h_tani3, h_group3, "
            + "r_day, r_month, r_same, r_week, n_group, c_kaisu, chgymd "
            + "from tbl_etensu_1";
    private static final String SELECT_ETENSU_2
            = "select h_group, srycd, yukostymd, yukoedymd, chgymd "
            + "from tbl_etensu_2";
    private static final String SELECT_ETENSU_2_JMA
            = "select h_group, srycd, yukostymd, yukoedymd, chgymd "
            + "from tbl_etensu_2_jma";
    private static final String SELECT_ETENSU_2_OFF
            = "select hospnum, h_group, srycd, yukostymd, yukoedymd, termid, opid, chgymd, upymd, uphms "
            + "from tbl_etensu_2_off ";
    private static final String SELECT_ETENSU_2_SAMPLE
            = "select h_group, srycd, yukostymd, yukoedymd, rennum, samplecd, chgymd "
            + "from tbl_etensu_2_sample";
    private static final String SELECT_ETENSU_3_1
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_1";
    private static final String SELECT_ETENSU_3_2
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_2";
    private static final String SELECT_ETENSU_3_3
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
            + "from tbl_etensu_3_3";
    private static final String SELECT_ETENSU_3_4
            = "select srycd1, srycd2, yukostymd, yukoedymd, haihan, tokurei, chgymd "
           + "from tbl_etensu_3_4";
    private static final String SELECT_ETENSU_4
            = "select n_group, srycd, yukostymd, yukoedymd, kasan, chgymd "
            + "from tbl_etensu_4";
    private static final String SELECT_ETENSU_5
            = "select srycd, yukostymd, yukoedymd, tanicd, taniname, kaisu, tokurei, chgymd "
            + "from tbl_etensu_5";
    
    private static final String WHERE_ETENSU_RELATED
            = " where not (h_tani1 = 0 and h_tani2 = 0 and h_tani3 = 0 "
            + "and r_day = 0 and r_month = 0 and r_same = 0 and r_week = 0 "
            + "and n_group = 0 and c_kaisu = 0)";

    
    private static final SqlETensuDao instance;

    static {
        instance = new SqlETensuDao();
    }

    public static SqlETensuDao getInstance() {
        return instance;
    }

    private SqlETensuDao() {
    }
    
    public List<ETensuModel1> getETensu1List(long offset, int limit) throws DaoException {
        
        // 結果を格納するリスト
        List<ETensuModel1> ret = new ArrayList<>();
        
        String sql = SELECT_ETENSU_1 + WHERE_ETENSU_RELATED + " limit ? offset ?";
        
        Object[] params = {limit, offset};

        List<String[]> valuesList = executePreparedStatement(sql, params);

        for (String[] values : valuesList) {
            ETensuModel1 model = createETensuModel1(values);
            ret.add(model);
        }
 
        return ret;
    }
    
    public List<ETensuModel1> getETensuModel1(Date date, Collection<String> srycds) throws DaoException {
        
        if (srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        sb.append(SELECT_ETENSU_1);
        sb.append(" where srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        List<ETensuModel1> list = new ArrayList<>();
        
        List<String[]> valuesList = executeStatement(sql);
        
        for (String[] values : valuesList) {
            ETensuModel1 model = createETensuModel1(values);
            list.add(model);
        }

        return list;
    }
    
    public List<ETensuModel2> getETensuModel2(Date date, Collection<String> h_groups, 
            Collection<String> srycds) throws DaoException {
        
        if (h_groups == null || h_groups.isEmpty() || srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_ETENSU_2);
        sb.append(" where h_group = (").append(getCodes(h_groups)).append(")");
        sb.append(" and srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        List<ETensuModel2> list = new ArrayList<>();
        
        List<String[]> valuesList = executeStatement(sql);
        
        for (String[] values : valuesList) {
            ETensuModel2 model = createETensuModel2(values);
            list.add(model);
        }

        return list;
    }
    
    public List<ETensuModel3> getETensuModel3(int tableNumber, Date date, 
            Collection<String> srycds1, Collection<String> srycds2) throws DaoException {
        
        if (srycds1 == null || srycds1.isEmpty() || srycds2 == null || srycds2.isEmpty()) {
            return Collections.emptyList();
        }
        
        SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        StringBuilder sb = new StringBuilder();
        switch(tableNumber) {
            case 1:     // １日につき
                sb.append(SELECT_ETENSU_3_1);
                break;
            case 2:     // 同一月内
                sb.append(SELECT_ETENSU_3_2);
                break;
            case 3:     // 同時
                sb.append(SELECT_ETENSU_3_3);
                break;
            case 4:     // １週間につき
                sb.append(SELECT_ETENSU_3_4);
                break;
            default:
                return Collections.emptyList();
        }
        sb.append(" where srycd1 in (").append(getCodes(srycds1)).append(")");
        sb.append(" and srycd2 in (").append(getCodes(srycds2)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        List<ETensuModel3> list = new ArrayList<>();
        
        List<String[]> valuesList = executeStatement(sql);
        
        for (String[] values : valuesList) {
            ETensuModel3 model = createETensuModel3(values);
            list.add(model);
        }

        return list;
    }
    
    public List<ETensuModel5> getETensuModel5(Date date, Collection<String> srycds) throws DaoException {
        
        if (srycds == null || srycds.isEmpty()) {
            return Collections.emptyList();
        }
        
        SimpleDateFormat yyyyMMddFrmt = new SimpleDateFormat("yyyyMMdd");
        StringBuilder sb = new StringBuilder();
        String ymd = addSingleQuote(yyyyMMddFrmt.format(date));
        sb.append(SELECT_ETENSU_5);
        sb.append(" where srycd in (").append(getCodes(srycds)).append(")");
        sb.append(" and yukostymd <= ").append(ymd);
        sb.append(" and yukoedymd >= ").append(ymd);
        
        String sql = sb.toString();
        List<ETensuModel5> list = new ArrayList<>();
        
        List<String[]> valuesList = executeStatement(sql);
        
        for (String[] values : valuesList) {
            ETensuModel5 model = createETensuModel5(values);
            list.add(model);
        }

        return list;
    }
    
    private ETensuModel1 createETensuModel1(String[] values) {
        int i = 0;
        ETensuModel1 model = new ETensuModel1();
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setH_tani1(Integer.valueOf(values[i++]));
        model.setH_group1(values[i++]);
        model.setH_tani2(Integer.valueOf(values[i++]));
        model.setH_group2(values[i++]);
        model.setH_tani3(Integer.valueOf(values[i++]));
        model.setH_group3(values[i++]);
        model.setR_day(Integer.valueOf(values[i++]));
        model.setR_month(Integer.valueOf(values[i++]));
        model.setR_same(Integer.valueOf(values[i++]));
        model.setR_week(Integer.valueOf(values[i++]));
        model.setN_group(Integer.valueOf(values[i++]));
        model.setC_kaisu(Integer.valueOf(values[i++]));
        model.setChgymd(values[i++]);
        return model;
    }
    
    private ETensuModel2 createETensuModel2(String[] values) {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setH_group(values[i++]);
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setChgymd(values[i++]);
        return model;
    }

    private ETensuModel2 createETensuModel2_OFF(String[] values) {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setHospnum(values[i++]);
        model.setH_group(values[i++]);
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setTermid(values[i++]);
        model.setOpid(values[i++]);
        model.setChgymd(values[i++]);
        model.setUpymd(values[i++]);
        model.setUphms(values[i++]);
        return model;
    }

    private ETensuModel2 createETensuModel2_SAMPLE(String[] values) {
        int i = 0;
        ETensuModel2 model = new ETensuModel2();
        model.setH_group(values[i++]);
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setRennum(Integer.valueOf(values[i++]));
        model.setSamplecd(values[i++]);
        model.setChgymd(values[i++]);
        return model;
    }
    
    private ETensuModel3 createETensuModel3(String[] values) {
        int i = 0;
        ETensuModel3 model = new ETensuModel3();
        model.setSrycd1(values[i++]);
        model.setSrycd2(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setHaihan(Integer.valueOf(values[i++]));
        model.setTokurei(Integer.valueOf(values[i++]));
        model.setChgymd(values[i++]);
        return model;
    }

    private ETensuModel4 createETensuModel4(String[] values) {
        int i = 0;
        ETensuModel4 model = new ETensuModel4();
        model.setN_group(values[i++]);
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setKasan(Integer.valueOf(values[i++]));
        model.setChgymd(values[i++]);
        return model;
    }
    
    private ETensuModel5 createETensuModel5(String[] values) {
        int i = 0;
        ETensuModel5 model = new ETensuModel5();
        model.setSrycd(values[i++]);
        model.setYukostymd(values[i++]);
        model.setYukoedymd(values[i++]);
        model.setTanicd(Integer.valueOf(values[i++]));
        model.setTaniname(values[i++]);
        model.setKaisu(Integer.valueOf(values[i++]));
        model.setTokurei(Integer.valueOf(values[i++]));
        model.setChgymd(values[i++]);
        return model;
    }

    public long getETensu1RowCount() throws DaoException {
        String condition = "et" + WHERE_ETENSU_RELATED;
        return getTableRowCount(TBL_ETENSU_1, condition);
    }
            
    private long getTableRowCount(String tableName, String condition) throws DaoException {

        long count = 0;

        String sql = "select count(*) from " + tableName + " " + condition;
        
        List<String[]> valuesList = executeStatement(sql);
        if (!valuesList.isEmpty()) {
            String[] values = valuesList.get(0);
            count = Long.valueOf(values[0]);
        }

        return count;
    }
}
