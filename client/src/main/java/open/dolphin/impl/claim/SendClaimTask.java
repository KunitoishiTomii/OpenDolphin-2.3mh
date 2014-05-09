package open.dolphin.impl.claim;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import open.dolphin.client.ClaimMessageEvent;

/**
 * SendClaimTask
 *
 * @author masuda, Masuda Naika
 */
public class SendClaimTask implements Callable<ClaimMessageEvent> {

    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;

    private final String encoding;
    private final InetSocketAddress address;
    private final ClaimMessageEvent evt;

    public SendClaimTask(String encoding, InetSocketAddress address, ClaimMessageEvent evt) {

        this.encoding = encoding;
        this.address = address;
        this.evt = evt;
    }

    @Override
    public ClaimMessageEvent call() throws Exception {
        
        try (SocketChannel channel = SocketChannel.open()) {
            
            byte[] bytes = evt.getClaimInsutance().getBytes(encoding);
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
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.NO_ERROR);
                    break;
                case NAK:
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.NAK_SIGNAL);
                    break;
                default:
                    evt.setErrorCode(ClaimMessageEvent.ERROR_CODE.IO_ERROR);
                    break;
            }
        }
        
        return evt;
    }
}
