package open.dolphin.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * ByteBufferOutputStream
 * @author masuda, Masuda Naika
 */
public class ByteBufferOutputStream extends OutputStream {

    private ByteBuffer buf;
    private static final int INITIAL_BUF_SIZE = 16384;
    
    public ByteBufferOutputStream() {
        this(INITIAL_BUF_SIZE);
    }
    
    public ByteBufferOutputStream(int size) {
        buf = ByteBuffer.allocate(size);
    }
    
    public ByteBufferOutputStream(ByteBuffer buf) {
        this.buf = buf;
    }
    
    private void verifyBuffer(int length) {
        if (buf.remaining() < length) {
            ByteBuffer old = buf;
            int bufLength = old.position();
            old.flip();
            int newCapacity = old.capacity() * 2;
            while (newCapacity < bufLength + length) {
                newCapacity *= 2;
            }
            buf = ByteBuffer.allocate(newCapacity);
            buf.put(old);
        }
    }

    @Override
    public void write(int b) throws IOException {
        verifyBuffer(1);
        buf.put((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        verifyBuffer(b.length);
        buf.put(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        verifyBuffer(len);
        buf.put(b, off, len);
    }
    
    public ByteBuffer getBuffer() {
        return buf;
    }
    
    public byte[] getByteArray() {
        if (buf.hasArray()) {
            return buf.array();
        }
        return null;
    }
    
    public byte[] getBytes() {
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }
}
