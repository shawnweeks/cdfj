package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.zip.*;
import java.util.*;
/**
 * CDFFactory creates an instance of CDFImpl from a CDF source.
 * The source CDF can  be a file, a byte array, or a URL.
 */
public final class CDFFactory {
    public static final long CDF3_MAGIC =((long)0xcdf3 << 48) +
        ((long)0x0001 << 32) + 0x0000ffff;
    public static final long CDF3_COMPRESSED_MAGIC =((long)0xcdf3 << 48) +
        ((long)0x0001 << 32) + 0x00000000cccc0001l;
    public static final long CDF2_MAGIC =((long)0xcdf2 << 48) +
        ((long)0x0001 << 32) + 0x0000ffff;
    public static final long CDF2_MAGIC_DOT5 = ((long)0x0000ffff << 32) +
         0x0000ffff;
    static Map cdfMap = Collections.synchronizedMap(new WeakHashMap());
    static Long maxMappedMemory;

    private CDFFactory() {
    }

    /**
     * creates  CDFImpl object from a byte array.
     */
    public static CDFImpl getCDF(byte [] ba) throws Throwable {
        ByteBuffer buf;
        synchronized (ba) {
            buf = ByteBuffer.allocateDirect(ba.length);
            buf.put(ba);
        }
        buf.flip();
        return getVersion(buf);
    }

    static CDFImpl getCDF(ByteBuffer buf) throws Throwable {
        ByteBuffer rbuf;
        synchronized (buf) {
            ByteBuffer _buf = ByteBuffer.allocateDirect(buf.remaining());
            _buf.put(buf);
            _buf.position(0);
            rbuf = _buf.asReadOnlyBuffer();
            rbuf.order(buf.order());
        }
        return getVersion(rbuf);
    }

    static CDFImpl getVersion(ByteBuffer buf) throws Throwable {
        LongBuffer lbuf = buf.asLongBuffer();
        long magic = lbuf.get();
        if (magic == CDF3_MAGIC) {
            return new CDF3Impl(buf);
        }
        if (magic == CDF3_COMPRESSED_MAGIC) {
            ByteBuffer mbuf = uncompressed(buf, 3);
            return new CDF3Impl(mbuf);
        }
        if (magic == CDF2_MAGIC_DOT5) {
            int release = buf.getInt(24);
            return new CDF2Impl(buf, release);
        } else {
            ShortBuffer sbuf = buf.asShortBuffer();
            if (sbuf.get() == (short)0xcdf2) {
                if (sbuf.get() == (short)0x6002) {
                    short x = sbuf.get();
                    if (x == 0) {
                        if (sbuf.get() == -1) {
                            return new CDF2Impl(buf, 6);
                        }
                    } else {
                        if ((x == (short)0xcccc) && (sbuf.get() == 1)) {
                            // is compressed - positioned at CCR
                            ByteBuffer mbuf = uncompressed(buf, 2);
                            return new CDF2Impl(mbuf, 6);
                        }
                    }
                        
                }
            }
        }
        return null;
    }
    /**
     * creates  CDFImpl object from a file.
     */
    public static CDFImpl getCDF(String fname) throws Throwable {
        return getCDF(fname, false);
    }

    static CDFImpl getCDF(final String fname, final boolean option)
        throws Throwable {
        clean();
        File file = new File(fname);
        final String _fname = file.getPath();
        FileInputStream fis = new FileInputStream(file);
        FileChannel ch = fis.getChannel();
        ByteBuffer buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        fis.close();
        CDFImpl cdf = getVersion(buf);
        ((CDFImpl)cdf).setOption(new ProcessingOption() {
            public String missingRecordOption() {
                if (option) return "accept";
                return "reject";
            }
        });
        ((CDFImpl)cdf).setSource(new CDFSource() {
            public String getName() {return _fname;};
            public boolean isFile() {return true;};
        });
        cdfMap.put(cdf, _fname);
        return cdf;
    }
    /**
     * creates  CDFImpl object from a URL.
     */
    public static CDFImpl getCDF(URL url) throws Throwable {
        final String _url = url.toString();
        URLConnection con = new CDFUrl(url).openConnection();
        int remaining = con.getContentLength();
        InputStream is = con.getInputStream();
        byte [] ba = new byte[remaining];
        int offset = 0;
        while (remaining > 0) {
            int got = is.read(ba, offset, remaining);
            offset += got;
            remaining -= got;
        }
        CDFImpl cdf = getCDF(ba);
        cdf.setSource(new CDFSource() {
            public String getName() {return _url;};
            public boolean isFile() {return false;};
        });
        return cdf;
    }
    static ByteBuffer uncompressed(ByteBuffer buf, int version) {
        int DATA_OFFSET = 8 + 20;
        if (version == 3) DATA_OFFSET = 8 + 32;
        byte[] ba;
        int offset;
        int len = buf.getInt(8) - 20;
        if (version == 3) len = (int)(buf.getLong(8) - 32);
        int ulen = buf.getInt(8 + 12);
        if (version == 3) ulen = (int)(buf.getLong(8 + 20));
        byte [] udata = new byte[ulen + 8];
        buf.get(udata, 0, 8); // copy the magic words
        if (!buf.hasArray()) { // read data into byte array
            ba = new byte[len];
            buf.position(DATA_OFFSET);
            buf.get(ba);
            offset = 0;
        } else {
            ba = buf.array();
            offset = DATA_OFFSET;
        }
        int n = 0;
        try {
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(ba, offset, len);
            GZIPInputStream gz = new GZIPInputStream(bais);
            int toRead = udata.length - 8;
            int off = 8;
            while (toRead > 0) {
                n = gz.read(udata, off, toRead);
                if (n == -1) break;
                off += n;
                toRead -= n;
            }
        } catch (IOException ex) {
            System.out.println(ex.toString());
            return null;
        }
        if (n < 0) return null;
        return ByteBuffer.wrap(udata);
    }

    public static class ProcessingOption {
        String missingRecordsOption() {return "reject";}
    }

    public static class CDFSource {
        public String getName() {return "";};
        public boolean isFile() {return false;};
        public boolean isURL() {return false;};
        public boolean isByteArray() {return false;};
        public boolean isByteBuffer() {return false;};
    }
    private static long mappedMemoryUsed() {
        if (cdfMap.size() == 0) return 0;
        Set set = cdfMap.keySet();
        Iterator it = set.iterator();
        long size = 0;
        while (it.hasNext()) {
            size += ((CDFImpl)it.next()).getBuffer().limit();
        }
        return size;
    }
    public static void setMaxMappedMemory(long value) {
        if (maxMappedMemory != null) {
            if (maxMappedMemory.longValue() > value) return;
        }
        maxMappedMemory = new Long(value);
    }
    public static void clean() {
        if (maxMappedMemory != null) {
            if (mappedMemoryUsed() > maxMappedMemory.longValue()) {
                System.gc();
            }
        }
    }
}
