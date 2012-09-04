package open.dolphin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import open.dolphin.project.Project;

/**
 * tbl_syskanriのカオスな情報を扱う
 * @author masuda, Masuda Naika
 */
public class SyskanriInfo extends SqlDaoBean {

    private static final SyskanriInfo instance;
    
    private static final String[] KANRICDS 
            = new String[]{"1001", "5000", "5001", "5002", "5013"};
    
    public static final String ORCA46 = "orca46";
    public static final String ORCA45 = "orca45";
    
    private String orcaVer;     // "orca46" or "orca45"
    
    private int hospNum;
    
    private List<Integer> syskanri1006;
    private Map<String, String> kanriTblMap;
    
    private static boolean initialized = false;

    static {
        instance = new SyskanriInfo();
    }
    
    public SyskanriInfo() {
    }
    
    public static SyskanriInfo getInstance() {
        if (!initialized) {
            instance.initialize();
        }
        return instance;
    }
    
    private void initialize() {
        syskanri1006 = new ArrayList<Integer>();
        kanriTblMap = new HashMap<String, String>();
        initialized = setHospNum();
        initialized &= setSyskanri1006();

        for (String kanricd : KANRICDS) {
            initialized &= setKanriTbl(kanricd);
        }
    }

    
    public String getOrcaVer() {
        return orcaVer;
    }
    
    public boolean isOrca46() {
        return ORCA46.equals(orcaVer);
    }
    
    public int getHospNum() {
        return hospNum;
    }

    public String getOrcaStaffCode(String userName) {
        
        String orcaStaffCode = "";
        
        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = '1010'";
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()) {
                String kbncd = rs.getString(1);
                String orcaUserName = rs.getString(2).substring(0, 16).trim();
                if (userName.equals(orcaUserName)) {
                    orcaStaffCode = kbncd.trim();
                    break;
                }
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
        } finally {
            closeStatement(st);
            closeConnection(con);
        }
        
        return orcaStaffCode;
    }
    
    // 有床か無床か
    public boolean hasBed() {
        try {
            String value = kanriTblMap.get("SYS-1001-BEDSU");
            if (Integer.valueOf(value) > 0) {
                return true;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public boolean getSyskanriFlag(int code) {
        return syskanri1006.contains(code);
    }

    
//============================================================================//
    
    // ORCAのデータベースバージョンとhospNumを取得する
    private boolean setHospNum() {
        
        Connection con = null;
        Statement st = null;

        boolean success = true;
        hospNum = 1;
        String jmari = Project.getString(Project.JMARI_CODE);

        StringBuilder sb = new StringBuilder();
        sb.append("select hospnum, kanritbl from tbl_syskanri where kanricd='1001' and kanritbl like '%");
        sb.append(jmari);
        sb.append("%'");
        String sql = sb.toString();
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                hospNum = rs.getInt(1);
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);
        }

        String dbVersion = null;
        sql = "select version from tbl_dbkanri where kanricd='ORCADB00'";
        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                dbVersion = rs.getString(1);
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);
        }
        
        if (ORCA_DB_VER46.equals(dbVersion)) {
            orcaVer = ORCA46;
        } else if (ORCA_DB_VER45.equals(dbVersion)) {
            orcaVer = ORCA45;
        }
        return success;
    }
    
    // 施設情報フラグ情報を取得する
    private boolean setSyskanri1006() {

        final String sql = "select kbncd, kanritbl from tbl_syskanri where kanricd = '1006' order by kbncd";
        boolean success = true;
        
        Connection con = null;
        Statement st = null;

        try {
            con = getConnection();
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            while (rs.next()) {
                int kbncd = rs.getInt(1);
                String kanritbl = rs.getString(2);
                for (int i = 0; i < kanritbl.length(); ++i) {
                    int index = (kbncd - 1) * 500 + i + 1;
                    char c = kanritbl.charAt(i);
                    if (c == '1') {
                        syskanri1006.add(index);
                    }
                }
            }
            rs.close();
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closeStatement(st);
            closeConnection(con);            
        }
        
        return success;
    }
    
    // 医療機関情報を取得する
    private boolean setKanriTbl(String kanricd) {

        final String sql = "select kanritbl from tbl_syskanri where kanricd = ?";
        boolean success = true;
        Connection con = null;
        PreparedStatement ps = null;

        try {
            String data = null;
            con = getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, kanricd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data = rs.getString(1);
            }
            rs.close();
            
            // CPSKxxxx.csvからカラム名とデータ位置・データ長のマップを取得する
            IncReader reader = new IncReader(kanricd, orcaVer);
            Map<String, String> map = reader.getMap(data);

            // 取得できたらマップに登録する
            if (map != null && !map.isEmpty()) {
                kanriTblMap.putAll(map);
            }
        } catch (Exception e) {
            processError(e);
            success = false;
        } finally {
            closePreparedStatement(ps);
            closeConnection(con);
        }
        return success;
    }
}