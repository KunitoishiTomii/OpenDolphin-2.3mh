package open.dolphin.impl.claim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import open.dolphin.client.ClaimMessageEvent;
import open.dolphin.client.KarteSenderResult;

/**
 * SendClaimTask
 *
 * @author masuda, Masuda Naika
 */
public class SendClaimTask implements Runnable {

    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    
    private static final String CLAIM = "CLAIM";

    private final String encoding;
    private final InetSocketAddress address;
    private final ClaimMessageEvent evt;

    public SendClaimTask(String encoding, InetSocketAddress address, ClaimMessageEvent evt) {

        this.encoding = encoding;
        this.address = address;
        this.evt = evt;
    }

    @Override
    public void run() {

        try (SocketChannel channel = SocketChannel.open()) {

            // write claim
            byte[] bytes = evt.getClaimInsutance().getBytes(encoding);
            ByteBuffer buff = ByteBuffer.wrap(bytes);
            channel.connect(address);
            channel.write(buff);
            
            // write EOT
            buff.clear();
            buff.put(EOT);
            buff.flip();
            channel.write(buff);

            // read ACK/NAK
            buff.clear();
            channel.read(buff);
            buff.flip();
            byte b = buff.get();
            switch (b) {
                case ACK:
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.NO_ERROR);
                    break;
                case NAK:
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.NAK_SIGNAL);
                    break;
                default:
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.IO_ERROR);
                    break;
            }
        } catch (IOException ex) {
            evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.IO_ERROR);
        }
        
        processSendResult();
    }
    
    private void processSendResult() {

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
