package open.dolphin.dao;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import open.dolphin.delegater.OrcaDelegater;
import open.dolphin.infomodel.DiseaseEntry;
import open.dolphin.infomodel.OrcaSqlModel;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.project.Project;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * SqlDaoBean
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class SqlDaoBean extends DaoBean {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final int PORT = 5432;
    private static final String DATABASE = "orca";
    private static final String USER = "orca";
    private static final String PASSWD = "";
    
    private String dataBase;
    private String driver;
    private boolean trace = true;

    protected static final String ORCA_DB_VER45 = "040500-1";
    protected static final String ORCA_DB_VER46 = "040600-1";
    protected static final String ORCA_DB_VER47 = "040700-1";

    protected static final String SELECT_TBL_TENSU =
            "select srycd,name,kananame,taniname,tensikibetu,"
            + "ten,nyugaitekkbn,routekkbn,srysyukbn,hospsrykbn,"
            + "ykzkbn,yakkakjncd,yukostymd,yukoedymd,datakbn "
            + "from tbl_tensu ";
    
    protected static final String SELECT_TBL_TENSU2 =
            "select t.srycd,t.name,t.kananame,t.taniname,t.tensikibetu,"
            + "t.ten,t.nyugaitekkbn,t.routekkbn,t.srysyukbn,t.hospsrykbn,"
            + "t.ykzkbn,t.yakkakjncd,t.yukostymd,t.yukoedymd,t.datakbn "
            + "from tbl_tensu t ";
    
    protected static final String SELECT_TBL_BYOMEI =
            "select byomeicd,byomei,byomeikana,icd10_1,haisiymd,tokskncd "
            + "from tbl_byomei ";

    protected static final String HOSPNUM_SRYCD = " and hospnum = ? order by srycd";
    
    protected static final DecimalFormat srycdFrmt = new DecimalFormat("000000000");
    
    private static DataSource dataSource;
    
    
    /**
     * Creates a new instance of SqlDaoBean
     */
    public SqlDaoBean() {
        // DataSourceを設定
        setDriver(DRIVER);
        setHost(Project.getString(Project.CLAIM_ADDRESS));
        setPort(PORT);
        setDatabase(DATABASE);
        setUser(USER);
        setPasswd(PASSWD);
    }

    protected DiseaseEntry getDiseaseEntry(List<String> values) {

        DiseaseEntry de = new DiseaseEntry();
        de.setCode(values.get(0));          // Code
        de.setName(values.get(1));          // Name
        de.setKana(values.get(2));          // Kana
        de.setIcdTen(values.get(3));        // IcdTen
        de.setDisUseDate(values.get(4));    // DisUseDate
        de.setByoKanrenKbn(Integer.parseInt(values.get(5)));
        return de;
    }
    
    protected TensuMaster getTensuMaster(List<String> values) {

        TensuMaster tm = new TensuMaster();
        tm.setSrycd(values.get(0));
        tm.setName(values.get(1));
        tm.setKananame(values.get(2));
        tm.setTaniname(values.get(3));
        tm.setTensikibetu(values.get(4));
        tm.setTen(values.get(5));
        tm.setNyugaitekkbn(values.get(6));
        tm.setRoutekkbn(values.get(7));
        tm.setSrysyukbn(values.get(8));
        tm.setHospsrykbn(values.get(9));
        tm.setYkzkbn(values.get(10));
        tm.setYakkakjncd(values.get(11));
        tm.setYukostymd(values.get(12));
        tm.setYukoedymd(values.get(13));
        tm.setDataKbn(values.get(14));
        return tm;
    }
    
    private boolean isClient() {
        String str = Project.getString(Project.CLAIM_SENDER);
        boolean client = (str == null || Project.CLAIM_CLIENT.equals(str));
        return client;
    }
    
    protected List<List<String>> executeStatement(String sql) {
        
        if (isClient()) {
            return executeStatement1(sql);
        }
        return executeStatement2(sql);
    }
    
    private List<List<String>> executeStatement1(String sql) {

        List<List<String>> valuesList = new ArrayList<>();

        try (Connection con = getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> values = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; ++i) {
                    values.add(String.valueOf(rs.getObject(i)));
                }
                valuesList.add(values);
            }

        } catch (Exception ex) {
            //ex.printStackTrace(System.err);
            processError(ex);
        }

        return valuesList;
    }
    
    private List<List<String>> executeStatement2(String sql) {
        
        OrcaSqlModel sqlModel = new OrcaSqlModel();
        sqlModel.setUrl(getURL());
        sqlModel.setSql(sql);

        try {
            OrcaSqlModel result = OrcaDelegater.getInstance().executeQuery(sqlModel);
            if (result != null) {
                String errMsg = result.getErrorMessage();
                if (errMsg != null) {
                    processError(new SQLException(result.getErrorMessage()));
                } else {
                    return result.getValuesList();
                }
            }
        } catch (Exception ex) {
        }

        return Collections.emptyList();
    }

    protected List<List<String>> executePreparedStatement(String sql, Object[] params) {
        
        try {
            String sql2 = createSql(sql, params);
            if (isClient()) {
                return executeStatement1(sql2);
            }
            return executeStatement2(sql2);
        } catch (SQLException ex) {
        }
        
        return Collections.emptyList();
    }
    
    private String createSql(String sql, Object[] params) throws SQLException {
        
        int index = 0;
        StringBuilder sb = new StringBuilder();

        try {
            int len = sql.length();
            for (int i = 0; i < len; ++i) {
                char c = sql.charAt(i);
                if (c == '?') {
                    Object param = params[index];
                    if (param instanceof Number) {
                        sb.append(String.valueOf(param));
                    } else {
                        sb.append('\'').append(String.valueOf(param)).append('\'');
                    }
                    index++;
                } else {
                    sb.append(c);
                }
            }
        } catch (Exception ex) {
            throw new SQLException(ex.getMessage());
        }

        if (index != params.length) {
            throw new SQLException("Illegal parameter count.");
        }

        return sb.toString();
    }

    
    protected final int getHospNum() {
        return SyskanriInfo.getInstance().getHospNumFromSysKanriInfo();
    }
    
    // srycdのListからカンマ区切りの文字列を作る
    protected String getCodes(Collection<String> srycdList){

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String srycd : srycdList){
            if (!first){
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(addSingleQuote(srycd));
        }
        return sb.toString();
    }

    // ORCAのptidを取得する
    protected final long getOrcaPtID(String patientId){

        long ptid = 0;
        int hospNum = getHospNum();
        
        final String sql = "select ptid from tbl_ptnum where hospnum = ? and ptnum = ?";
        
        Object[] params = {hospNum, patientId};
        
        List<List<String>> valuesList = executePreparedStatement(sql, params);
        
        if (!valuesList.isEmpty()) {
            List<String> values = valuesList.get(0);
            ptid = Long.valueOf(values.get(0));
        }

        return ptid;
    }

    private Connection getConnection() throws Exception {
        Connection con;
        try {
            if (false) {
                con = DriverManager.getConnection(getURL(), getUser(), getPasswd());
            } else {
                con = getConnectionFromPool();
            }
        } catch (Exception ex) {
            processError(ex);
            String msg = getErrorMessage();
            String title = "ORCA接続";
            JOptionPane.showMessageDialog(null, msg, title, JOptionPane.WARNING_MESSAGE);
            throw ex;
        }
        return con;
    }
    
    private Connection getConnectionFromPool() throws Exception {
        
        if (dataSource == null) {
            setupDataSource();
        }
        // 呼び出し前にエラーをクリア。シングルトン化の影響ｗ
        errorCode = TT_NO_ERROR;
        errorMessage = null;
        
        return dataSource.getConnection();
    }
    
    private void setupDataSource() {
        PoolProperties p = new PoolProperties();
        p.setDriverClassName(getDriver());
        p.setUrl(getURL());
        p.setUsername(getUser());
        p.setPassword(getPasswd());
        p.setDefaultReadOnly(true);
        p.setMaxActive(2);
        p.setMaxIdle(2);
        p.setMinIdle(1);
        p.setInitialSize(1);
        p.setMaxWait(5000);
        p.setRemoveAbandonedTimeout(30);
        p.setRemoveAbandoned(true);
        dataSource = new DataSource();
        dataSource.setPoolProperties(p);
    }

    public String getDriver() {
        return driver;
    }

    public final void setDriver(String driver) {
        this.driver = driver;
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
        }
    }

    public final String getDatabase() {
        return dataBase;
    }

    public final void setDatabase(String base) {
        dataBase = base;
    }

    protected final String getURL() {
        StringBuilder buf = new StringBuilder();
        buf.append("jdbc:postgresql://");
        buf.append(host);
        buf.append(":");
        buf.append(port);
        buf.append("/");
        buf.append(dataBase);
        return buf.toString();
    }
     
    public boolean getTrace() {
        return trace;
    }
    
    public void setTrace(boolean b) {
        trace = b;
    }

    public String addSingleQuote(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("'");
        buf.append(s);
        buf.append("'");
        return buf.toString();
    }

    /**
     * To make sql statement ('xxxx',)
     */
    public String addSingleQuoteComa(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("'");
        buf.append(s);
        buf.append("',");
        return buf.toString();
    }

    protected void debug(String msg) {
        logger.debug(msg);
    }
    
    protected void printTrace(String msg) {
        if (trace) {
            logger.debug(msg);
        }
    }
    
    public static void closeDao() {
        if (dataSource != null) {
            dataSource.close(true);
        }
    }
    
    protected void rollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }
}
