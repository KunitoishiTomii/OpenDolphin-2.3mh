package open.dolphin.server.orca;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import open.dolphin.infomodel.ClaimMessageModel;

/**
 * SendClaimImpl
 * 
 * @author masuda, Mausda Naika
 */
public class SendClaimImpl {
    
    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final String UTF8 = "UTF-8";
    private static final int DEFAULT_PORT = 5002;
    
    private static final String NO_ERROR = "00";
    private static final String ERROR = "XXX";
    
    //private static final Logger logger = Logger.getLogger(SendClaimImpl.class.getSimpleName());

    
    private int getPort(ClaimMessageModel model) {
        int port = model.getPort();
        return (port == 0) ? DEFAULT_PORT : port;
    }
    
    private String getEncoding(ClaimMessageModel model) {
        String encoding = model.getEncoding();
        return (encoding == null) ? UTF8 : encoding;
    }
    
    public ClaimMessageModel sendClaim(ClaimMessageModel model) {

        InetSocketAddress address = new InetSocketAddress(model.getAddress(), getPort(model));
        String encoding =  getEncoding(model);
        
        try (SocketChannel channel = SocketChannel.open()) {

            byte[] bytes = model.getContent().getBytes(encoding);
            ByteBuffer writeBuffer = ByteBuffer.wrap(bytes);
            ByteBuffer buff = ByteBuffer.allocate(1);
            buff.put(EOT);
            buff.flip();

            channel.connect(address);
            channel.write(writeBuffer);
            channel.write(buff);

            buff.clear();
            channel.read(buff);
            buff.flip();
            byte b = buff.get();
            switch (b) {
                case ACK:
                    model.setClaimErrorCode(ClaimMessageModel.ERROR_CODE.NO_ERROR);
                    break;
                case NAK:
                    model.setClaimErrorCode(ClaimMessageModel.ERROR_CODE.NAK_SIGNAL);
                    break;
                default:
                    model.setClaimErrorCode(ClaimMessageModel.ERROR_CODE.IO_ERROR);
                    break;
            }

        } catch (IOException ex) {
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
