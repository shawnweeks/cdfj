package gov.nasa.gsfc.spdf.cdfj;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
/**
*/
public class ByteBufferURLReader {
    InputStream is;
    boolean eof = false;
    public int total;
    public int len = -1;
    Chunk chunk = new Chunk();
    byte[] block = chunk.getBlock();
    FileChannel cacheFileChannel;
    ByteBuffer buffer;
    public ByteBufferURLReader(URL url) throws IOException {
        URLConnection con = url.openConnection();
        con.connect();
        len = con.getContentLength();
        if (len >= 0) chunk.setLength(len);
        is = con.getInputStream();
        boolean gzipped = url.getPath().trim().endsWith(".gz");
        if (gzipped) is = new GZIPInputStream(is);
    }

    public ByteBufferURLReader(URL url, Chunk chunk) throws IOException {
        this(url);
        setChunk(chunk);
    }

    public ByteBufferURLReader(URL url, FileChannel fileChannel, Chunk chunk)
        throws IOException {
        this(url, chunk);
        cacheFileChannel = fileChannel;
        buffer = chunk.allocateBuffer();
    }

    public ByteBufferURLReader(URL url, FileChannel fileChannel) throws
        IOException {
        this(url);
        cacheFileChannel = fileChannel;
        buffer = chunk.allocateBuffer();
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
        block = chunk.getBlock();
    }

    public ByteBuffer getBuffer() throws IOException {
        Vector<ByteBuffer> buffers = new Vector<ByteBuffer>();
        while (!eof) {
            if (cacheFileChannel == null) {
                buffers.add(read());
            } else {
                transfer();
            }
        }
        if (cacheFileChannel != null) {
            long pos = cacheFileChannel.position();
            FileChannel.MapMode mode = FileChannel.MapMode.READ_ONLY;
            return cacheFileChannel.map(mode, 0l, pos);
        }
        if (buffers.size() == 1) {
            ByteBuffer _buf = buffers.get(0);
            return _buf.asReadOnlyBuffer();
        }
        int size = 0;
        for (int i = 0; i < buffers.size(); i++) {
            ByteBuffer _buf = buffers.get(i);
            size += _buf.remaining();
        }
        ByteBuffer ball = ByteBuffer.allocateDirect(size);
        for (int i = 0; i < buffers.size(); i++) {
            ByteBuffer _buf = buffers.get(i);
            ball.put(_buf);
        }
        ball.position(0);
        return ball.asReadOnlyBuffer();
    }

    public ByteBuffer read() throws IOException {
        ByteBuffer buf = chunk.allocateBuffer();
        _read(buf);
        return buf;
    }

    public void transfer() throws IOException {
        _read(buffer);
        cacheFileChannel.write(buffer);
        return;
    }

    private void _read(ByteBuffer buffer) throws IOException {
        int count = 0;
        int n;
        buffer.position(0);
        buffer.limit(buffer.capacity());
        if (buffer.capacity() < 2*block.length) {
            for (int i = 0; i < buffer.capacity(); i++) {
                n = is.read();
                if (n == -1) throw new IOException("Premature end of data");
                buffer.put((byte)n);
            }
            if ((n = is.read(block)) != -1) {
                throw new IOException("Unread data remains");
            }
            count = buffer.capacity();
            total = count;
            eof = true;
        } else {
            while ((n = is.read(block)) != -1) {
                buffer.put(block, 0, n);
                total += n;
                count += n;
                if (len == buffer.capacity()) continue;
                if (count >= chunk.chunkSize) break;
            }
            if (n == -1) eof = true;
            if (eof && (len >= 0)) {
                if (total != len) throw new IOException("Mismatched length " +
                    total + " expected: " + len);
            }
            buffer.limit(buffer.position());
        }
        buffer.position(0);
    }

    public boolean endOfFile() {return eof;}

    public static class Chunk {
        int chunkSize = 1024*1024;
        int blockSize = 64*1024;
        int len = -1;
        public Chunk() {}

        public Chunk(int blockSize, int chunkSize) throws Throwable {
            if (chunkSize < blockSize) {
                throw new Throwable("Chunk size must be >= block size");
            }
            this.blockSize = blockSize;
            this.chunkSize = chunkSize;
        }

        void setLength(int length) {len = length;}
        ByteBuffer allocateBuffer() {
            int bufsize = chunkSize + blockSize;
            if (len < 0) return ByteBuffer.allocateDirect(bufsize);
            if (len > bufsize) return ByteBuffer.allocateDirect(bufsize);
            return ByteBuffer.allocateDirect(len);
        }

        byte[] getBlock() {
            return  new byte[blockSize];
        }
    }
}
