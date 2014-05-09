package open.dolphin.server.orca;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import open.dolphin.infomodel.ClaimMessageModel;

/**
 * SendClaimTask
 * 
 * @author masuda, Masuda Naika
 */
public class SendClaimTask implements Callable<ClaimMessageModel>{
 
    //private static final Logger logger = Logger.getLogger(ClaimIOHandler.class.getSimpleName());
    
    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    
    private final String encoding;
    private final InetSocketAddress address;
    private final ClaimMessageModel model;
    
    public SendClaimTask(String encoding, InetSocketAddress address, ClaimMessageModel model) {

        this.encoding = encoding;
        this.address = address;
        this.model = model;
    }

    @Override
    public ClaimMessageModel call() throws Exception {

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

        }
        return model;
    }
}
