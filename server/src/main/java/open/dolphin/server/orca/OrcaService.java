package open.dolphin.server.orca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import open.dolphin.infomodel.ClaimMessageModel;
import open.dolphin.infomodel.OrcaSqlModel;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * OrcaService
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaService {
    
    private static final Logger logger = Logger.getLogger(OrcaService.class.getSimpleName());
    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final String user = "orca";
    private static final String passwd = "";
    
    private SendClaimImpl sendClaim;
    private Map<String, DataSource> dataSourceMap;
    
    private static final OrcaService instance;
    
    static {
        instance = new OrcaService();
    }
    
    public static OrcaService getInstance() {
        return instance;
    }
    
    private OrcaService() {
    }
    
    public void start() {
        
        dataSourceMap = new ConcurrentHashMap<>();
        sendClaim = new SendClaimImpl();
        logger.info("Server ORCA service started.");
    }
    
    public void dispose() {

        for (DataSource ds : dataSourceMap.values()) {
            ds.close(true);
        }
        dataSourceMap.clear();
        logger.info("Server ORCA service stopped.");
    }

    public ClaimMessageModel sendClaim(ClaimMessageModel model) {
        return sendClaim.sendClaim(model);
    }
    
    public OrcaSqlModel executeSql(OrcaSqlModel sqlModel) {

        executeStatement(sqlModel);

        return sqlModel;
    }
    
    private void executeStatement(OrcaSqlModel sqlModel) {
        
        List<List<String>> valuesList = new ArrayList<>();

        try (Connection con = getConnection(sqlModel.getUrl());
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sqlModel.getSql());){

            while (rs.next()) {
                List<String> values = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                for (int i = 1; i <= columnCount; ++i) {
                    int type = meta.getColumnType(i);
                    switch (type) {
                        case Types.SMALLINT:
                        case Types.NUMERIC:
                        case Types.INTEGER:
                            values.add(String.valueOf(rs.getInt(i)));
                            break;
                        default:
                            values.add(rs.getString(i));
                            break;
                    }
                }
                valuesList.add(values);
            }

        } catch (SQLException | ClassNotFoundException | NullPointerException ex) {
            sqlModel.setErrorMessage(ex.getMessage());
        }

        sqlModel.setValuesList(valuesList);
    }

    private Connection getConnection(String url) 
            throws ClassNotFoundException, SQLException, NullPointerException {
        DataSource ds = getDataSource(url, user, passwd);
        return ds.getConnection();
    }
    
    private DataSource getDataSource(String url, String user, String pass) {

        DataSource ds = dataSourceMap.get(url);
        if (ds == null) {
            try {
                ds = setupDataSource(url, user, pass);
                dataSourceMap.put(url, ds);
            } catch (ClassNotFoundException ex) {
            }
        }
        return ds;
    }
    
    private DataSource setupDataSource(String url, String user, String pass) throws ClassNotFoundException {

        PoolProperties p = new PoolProperties();
        p.setDriverClassName(POSTGRES_DRIVER);
        p.setUrl(url);
        p.setUsername(user);
        p.setPassword(pass);
        p.setDefaultReadOnly(true);
        p.setMaxActive(5);
        p.setMaxIdle(5);
        p.setMinIdle(1);
        p.setInitialSize(1);
        p.setMaxWait(5000);
        p.setRemoveAbandonedTimeout(30);
        p.setRemoveAbandoned(true);
        DataSource ds = new DataSource();
        ds.setPoolProperties(p);

        return ds;
    }
    
}
