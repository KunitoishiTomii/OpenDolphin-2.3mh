package open.dolphin.impl.claim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import open.dolphin.client.ClaimMessageEvent;

/**
 * ClaimIOHandler
 * @author masuda, Masuda Naika
 */
public class ClaimIOHandler {
 
    // Socket constants
    private static final byte EOT = 0x04;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    
    private final ClaimMessageEvent evt;
    private ByteBuffer writeBuffer;

    
    public ClaimIOHandler(ClaimMessageEvent evt, String encoding) {
        
        this.evt = evt;

        try {
            byte[] bytes = evt.getClaimInsutance().getBytes(encoding);
            writeBuffer = ByteBuffer.allocate(bytes.length + 1);
            writeBuffer.put(bytes);
            writeBuffer.put(EOT);
            writeBuffer.flip();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(System.err);
        }
    }
    
    public void handle(SelectionKey key) throws IOException {

        if (key.isConnectable()) {
            doConnect(key);
        } else {
            if (key.isValid() && key.isReadable()) {
                doRead(key);
            }
            if (key.isValid() && key.isWritable()) {
                doWrite(key);
            }
        }
    }

    // 接続する
    private void doConnect(SelectionKey key) throws IOException {
        
        SocketChannel channel = (SocketChannel) key.channel();

        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        
        key.interestOps(SelectionKey.OP_WRITE);
    }
    
    // データを書き出す
    private void doWrite(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();

        channel.write(writeBuffer);
        
        if (writeBuffer.remaining() == 0) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    // 返事を受け取る
    private void doRead(SelectionKey key) throws IOException {
        
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        channel.read(byteBuffer);
        byteBuffer.flip();
        byte b = byteBuffer.get();

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

        channel.close();
    }
}
