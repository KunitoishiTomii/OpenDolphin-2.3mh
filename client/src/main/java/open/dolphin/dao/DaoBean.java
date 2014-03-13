package open.dolphin.dao;

import javax.swing.JOptionPane;
import open.dolphin.client.ClientContext;
import org.apache.log4j.Logger;

/**
 * DaoBean
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class DaoBean {
    
    protected String host;
    protected int port;
    protected String user;
    protected String passwd;
    
    protected Logger logger;
    
    /**
     * Creates a new instance of DaoBean
     */
    public DaoBean() {
        logger = ClientContext.getBootLogger();
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getPasswd() {
        return passwd;
    }
    
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }
    
    /**
     * 例外を解析しエラーコードとエラーメッセージを設定する。
     *
     * @param e Exception
     */
    protected void processError(Exception e) throws DaoException {

        logger.warn(e);

        DaoException ex = new DaoException(e);
        StringBuilder sb = new StringBuilder();

        if (e instanceof org.postgresql.util.PSQLException) {
            sb.append("サーバに接続できません。ネットワーク環境をお確かめください。");
            sb.append("\n");
            sb.append(appenExceptionInfo(e));
            ex.setErrorCode(DaoException.TT_CONNECTION_ERROR);
            ex.setErrorMessage(sb.toString());
        } else if (e instanceof java.sql.SQLException) {
            sb.append("データベースアクセスエラー");
            sb.append("\n");
            sb.append(appenExceptionInfo(e));
            ex.setErrorCode(DaoException.TT_DATABASE_ERROR);
            ex.setErrorMessage(sb.toString());
        } else {
            sb.append("アプリケーションエラー");
            sb.append("\n");
            sb.append(appenExceptionInfo(e));
            ex.setErrorCode(DaoException.TT_UNKNOWN_ERROR);
            ex.setErrorMessage(sb.toString());
        }

        String msg = ex.getErrorMessage();
        String title = "ORCA接続";
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.WARNING_MESSAGE);
        
        throw ex;
    }
    
    /**
     * 例外の持つ情報を加える。
     * @param e 例外
     */
    protected String appenExceptionInfo(Exception e) {
        
        StringBuilder sb  = new StringBuilder();
        sb.append("例外クラス: ");
        sb.append(e.getClass().getName());
        sb.append("\n");
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            sb.append("原因: ");
            sb.append(e.getCause().getMessage());
            sb.append("\n");
        }
        if (e.getMessage() != null) {
            sb.append("内容: ");
            sb.append(e.getMessage());
        }
        
        return sb.toString();
    }
}