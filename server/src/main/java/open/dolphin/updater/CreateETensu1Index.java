package open.dolphin.updater;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import open.dolphin.infomodel.MsdUpdaterModel;
import org.hibernate.Session;

/**
 * ETensu1のsrycdカラムにインデックスを付ける
 * 
 * @author masuda, Masuda Naika
 */
public class CreateETensu1Index extends AbstractUpdaterModule {

    private static final String VERSION_DATE = "2014-03-05";
    private static final String UPDATE_MEMO = "Column srycd index created in tbl_etensu_1.";
    private static final String NO_UPDATE_MEMO = "Column srycd index not created in tbl_etensu_1.";
    
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String[] TABLES = {"tbl_etensu_1", "msd_santeiHistory"};
    private static final String TARGET_COLUMN_NAME = "srycd";
    
    boolean updated = false;
    
    @Override
    public String getVersionDateStr() {
        return VERSION_DATE;
    }

    @Override
    public String getModuleName() {
        return getClass().getSimpleName();
    }

    @Override
    public MsdUpdaterModel start() {

        Session hibernateSession = em.unwrap(Session.class);
        hibernateSession.doWork(new UpdateWork());

        return updated
                ? getResult(UPDATE_MEMO)
                : getResult(NO_UPDATE_MEMO);
    }

    private class UpdateWork implements org.hibernate.jdbc.Work {

        @Override
        public void execute(Connection con) throws SQLException {
            
            List<String> tables = new ArrayList<>();
            
            DatabaseMetaData dmd = con.getMetaData();
            
            for (String table : TABLES) {
                boolean found = false;
                try (ResultSet rs = dmd.getIndexInfo(null, null, table, false, false)) {
                    while (rs.next()) {
                        String columnName = rs.getString(COLUMN_NAME);
                        if (TARGET_COLUMN_NAME.equals(columnName)) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    tables.add(table);
                }
            }

            updated = false;
            if (!tables.isEmpty()) {
                try (Statement stmt = con.createStatement()) {
                    for (String table : tables) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("create index ").append(table).append("_idx");
                        sb.append(" on ").append(table).append(" (").append(TARGET_COLUMN_NAME).append(")");
                        stmt.addBatch(sb.toString());
                    }
                    stmt.executeBatch();
                    updated = true;
                } catch (Exception e) {
                }
            }
        }
    }

}

