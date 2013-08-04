package open.dolphin.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * ByteBufferInputStream
 * @author masuda, Masuda Naika
 */
public class ByteBufferInputStream extends InputStream {

    private ByteBuffer buf;
    
    public ByteBufferInputStream(byte[] bytes) {
        buf = ByteBuffer.wrap(bytes);
    }
    
    public ByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        return buf.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }

        len = Math.min(len, buf.remaining());
        buf.get(bytes, off, len);
        return len;
    }
    
    public ByteBuffer getBuffer() {
        return buf;
    }
}
