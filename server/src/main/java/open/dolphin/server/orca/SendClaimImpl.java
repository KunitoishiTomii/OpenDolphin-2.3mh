package open.dolphin.server.orca;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Logger;
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
    
    private static final Logger logger = Logger.getLogger(SendClaimImpl.class.getSimpleName());
    
    private Selector selector;


    public  SendClaimImpl() {
        
        try {
            // セレクタの生成
            selector = Selector.open();
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }

    public void stop() {
        try {
            selector.close();
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
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
    
    public void sendClaim(ClaimMessageModel model) {
        
        SocketChannel channel = null;
        try {
            InetSocketAddress address = new InetSocketAddress(model.getAddress(), getPort(model));
            String encoding = getEncoding(model);
            channel = SocketChannel.open();
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);
            channel.connect(address);
            ClaimIOHandler handler = new ClaimIOHandler(model, encoding);
            channel.register(selector, SelectionKey.OP_CONNECT, handler);

            while (channel.isOpen() && selector.select() > 0) {
                for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
                    SelectionKey key = itr.next();
                    itr.remove();
                    ClaimIOHandler ch = (ClaimIOHandler) key.attachment();
                    ch.handle(key);
                }
            }
        } catch (IOException ex) {
            model.setClaimErrorCode(ClaimMessageModel.ERROR_CODE.IO_ERROR);
        } finally {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException ex) {
                }
            }
        }
        
        processSendResult(model);
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
