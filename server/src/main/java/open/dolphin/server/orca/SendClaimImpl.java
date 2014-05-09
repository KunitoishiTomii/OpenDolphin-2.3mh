package open.dolphin.server.orca;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import open.dolphin.infomodel.ClaimMessageModel;

/**
 * SendClaimImpl
 * 
 * @author masuda, Mausda Naika
 */
public class SendClaimImpl {
    
    private static final String UTF8 = "UTF-8";
    private static final int DEFAULT_PORT = 5002;
    
    private static final String NO_ERROR = "00";
    private static final String ERROR = "XXX";
    
    private ExecutorService exec;
    
    //private static final Logger logger = Logger.getLogger(SendClaimImpl.class.getSimpleName());
    
    
    public void dispose() {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(20, TimeUnit.MILLISECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            exec.shutdownNow();
        } catch (NullPointerException ex) {
        }
    }
    
    private int getPort(ClaimMessageModel model) {
        int port = model.getPort();
        return (port == 0) ? DEFAULT_PORT : port;
    }
    
    private String getEncoding(ClaimMessageModel model) {
        String encoding = model.getEncoding();
        return (encoding == null) ? UTF8 : encoding;
    }
    
    public ClaimMessageModel sendClaim(ClaimMessageModel model) {
        
        if (exec == null) {
            exec = Executors.newSingleThreadExecutor();
        }
        
        InetSocketAddress address = new InetSocketAddress(model.getAddress(), getPort(model));
        Future<ClaimMessageModel> future = exec.submit(new SendClaimTask(getEncoding(model), address, model));
        
        try {
            future.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException| ExecutionException ex) {
            model.setClaimErrorCode(ClaimMessageModel.ERROR_CODE.IO_ERROR);
        }

        processSendResult(model);
        
        return model;
    }
    
    private void processSendResult(ClaimMessageModel model) {
        
        ClaimMessageModel.ERROR_CODE code = model.getClaimErrorCode();
        String errMsg = getErrorInfo(code);
        
        if (code == ClaimMessageModel.ERROR_CODE.NO_ERROR) {
            model.setErrorCode(NO_ERROR);
        } else {
            model.setErrorCode(ERROR);
            model.setErrorMsg(errMsg);
        }
        
        model.setContent(null);
    }
    
    private String getErrorInfo(ClaimMessageModel.ERROR_CODE errorCode) {

        String ret;
        switch (errorCode) {
            case NO_ERROR:
                ret = "No Error";
                break;
            case NAK_SIGNAL:
                ret = "NAK signal received from ORCA";
                break;
            case IO_ERROR:
                ret = "I/O error";
                break;
            case CONNECTION_REJECT:
                ret = "CLAIM connection rejected";
                break;
            default:
                ret = "Unknown Error";
                break;
        }
        return ret;
    }
}
