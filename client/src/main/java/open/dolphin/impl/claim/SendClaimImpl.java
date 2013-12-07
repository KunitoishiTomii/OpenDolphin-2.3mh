package open.dolphin.impl.claim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import open.dolphin.client.*;
import open.dolphin.delegater.OrcaDelegater;
import open.dolphin.project.Project;
import org.apache.log4j.Logger;

/**
 * SendClaimImpl
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika こりゃ失敗ｗ
 */
public class SendClaimImpl implements ClaimMessageListener {

    // Properties
    private String host;
    private int port;
    private String enc;
    private String name;
    private MainWindow context;
    private final Logger logger;
    
    private Selector selector;
    private InetSocketAddress address;
    
    private static final String CLAIM = "CLAIM";

    /**
     * Creates new ClaimQue
     */
    public SendClaimImpl() {
        logger = ClientContext.getClaimLogger();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MainWindow getContext() {
        return context;
    }

    @Override
    public void setContext(MainWindow context) {
        this.context = context;
    }

    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {

        setHost(Project.getString(Project.CLAIM_ADDRESS));
        setPort(Project.getInt(Project.CLAIM_PORT));
        setEncoding(Project.getString(Project.CLAIM_ENCODING));
        
        address = new InetSocketAddress(getHost(), getPort());
        
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        logger.info("SendClaim started with = host = " + getHost() + " port = " + getPort());
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {
        try {
            selector.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getEncoding() {
        return enc;
    }

    @Override
    public void setEncoding(String enc) {
        this.enc = enc;
    }

    /**
     * カルテで CLAIM データが生成されるとこの通知を受ける。
     */
    @Override
    public void claimMessageEvent(ClaimMessageEvent evt) {

        if (isClient()) {
            sendClaim(evt);
        } else {
            OrcaDelegater.getInstance().sendClaim(evt);
        }
    }
    
    private boolean isClient() {
        
        String str = Project.getString(Project.CLAIM_SENDER);
        boolean client = (str == null || Project.CLAIM_CLIENT.equals(str));
        return client;
    }
    
    private void sendClaim(ClaimMessageEvent evt) {

        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);
            channel.connect(address);
            ClaimIOHandler handler = new ClaimIOHandler(evt, getEncoding());
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
            evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.IO_ERROR);
        } finally {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException ex) {
                }
            }
        }
        
        processSendResult(evt);
    }

    private void processSendResult(ClaimMessageEvent evt) {

        ClaimMessageEvent.ERROR_CODE errCode = evt.getErrorCode();
        String errMsg = getErrorInfo(errCode);
        boolean noError = (errCode == ClaimMessageEvent.ERROR_CODE.NO_ERROR);

        Object evtSource = evt.getSource();
        if (evtSource instanceof ClaimSender) {
            ClaimSender sender = (ClaimSender) evtSource;
            KarteSenderResult result = !noError
                    ? new KarteSenderResult(CLAIM, KarteSenderResult.ERROR, errMsg, sender)
                    : new KarteSenderResult(CLAIM, KarteSenderResult.NO_ERROR, null, sender);
            sender.fireResult(result);
        } else if (evtSource instanceof DiagnosisSender) {
            DiagnosisSender sender = (DiagnosisSender) evtSource;
            KarteSenderResult result = !noError
                    ? new KarteSenderResult(CLAIM, KarteSenderResult.ERROR, errMsg, sender)
                    : new KarteSenderResult(CLAIM, KarteSenderResult.NO_ERROR, null, sender);
            sender.fireResult(result);
        }
    }

    private String getErrorInfo(ClaimMessageEvent.ERROR_CODE errorCode) {

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