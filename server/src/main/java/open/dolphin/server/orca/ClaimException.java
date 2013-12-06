package open.dolphin.server.orca;

import open.dolphin.infomodel.ClaimMessageModel;

/**
 * ClaimException
 * @author masuda, Masuda Naika
 */
public class ClaimException extends Exception {

    public static enum ERROR_CODE {

        NO_ERROR, CONNECTION_REJECT, IO_ERROR, NAK_SIGNAL
    };
    
    private final ERROR_CODE code;
    private final ClaimMessageModel model;

    public ClaimException(ERROR_CODE code, ClaimMessageModel model) {
        this.code = code;
        this.model = model;
    }

    public ERROR_CODE getErrorCode() {
        return code;
    }

    public ClaimMessageModel getClaimMessageModel() {
        return model;
    }
}
