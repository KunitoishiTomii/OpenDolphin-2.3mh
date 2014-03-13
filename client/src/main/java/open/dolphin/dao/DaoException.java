package open.dolphin.dao;

/**
 * DaoBeanから分離
 * 
 * @author masuda, Masuda Naika
 */
public class DaoException extends Exception {

    public static final int TT_NONE = 10;
    public static final int TT_NO_ERROR = 0;
    public static final int TT_CONNECTION_ERROR = -1;
    public static final int TT_DATABASE_ERROR = -2;
    public static final int TT_UNKNOWN_ERROR = -3;

    private int errorCode;
    private String errorMessage;
    
    public DaoException(Exception ex) {
        super(ex);
    }
    public DaoException(String str) {
        super(str);
    }

    public boolean isNoError() {
        return errorCode == TT_NO_ERROR;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass()).append(",");
        sb.append("code=").append(errorCode).append(",");
        sb.append("message=").append(errorMessage);
        return sb.toString();
    }
}
