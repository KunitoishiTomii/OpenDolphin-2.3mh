package open.dolphin.infomodel;

import java.io.Serializable;

/**
 * サーバー経由CLAIMモデル
 * 
 * @author masuda, Masuda Naika
 */
public class ClaimMessageModel implements Serializable {
    
    public static enum ERROR_CODE {

        NO_ERROR, CONNECTION_REJECT, IO_ERROR, NAK_SIGNAL
    };
    
    private ERROR_CODE claimErrorCode;
    
    private String address;
    private int port;
    private String encoding;
    private String content;
    
    private String errorCode;
    private String errorMsg;
    
    public ClaimMessageModel() {    
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setErrorCode(String errCode) {
        this.errorCode = errCode;
    }
    public void setErrorMsg(String errMsg) {
        this.errorMsg = errMsg;
    }

    public String getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }
    public String getEncoding() {
        return encoding;
    }
    public String getContent() {
        return content;
    }
    public String getErrorCode() {
        return errorCode;
    }
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public ERROR_CODE getClaimErrorCode() {
        return claimErrorCode;
    }
    
    public void setClaimErrorCode(ERROR_CODE code) {
        claimErrorCode = code;
    }
}
