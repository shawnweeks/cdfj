package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.net.*;
/**
 * ReaderFactory creates an instance of CDFReader from a CDF source.
 * Uses array backed ByteBuffer for CDFReader.
 * The source CDF can  be a file,  or a URL.
 */
public final class ReaderFactory {
    /**
     * creates  CDFReader object from a file using array backed ByteBuffer.
     */
    static int preamble = 3000;
    public static CDFReader getReader(String fname) throws
        CDFException.ReaderError {
        CDFImpl cdf = null;
        File file = new File(fname);
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long len = raf.length(); 
            if (len > preamble) len = preamble;
            byte[] ba = new byte[(int)len];
            raf.readFully(ba);
            ByteBuffer buf = ByteBuffer.wrap(ba);
            cdf = getVersion(buf, raf.getChannel());
        } catch (Throwable th) {
            throw new CDFException.ReaderError("I/O Error reading " + fname);
        }
        final String _fname = file.getPath();
        cdf.setSource(new CDFFactory.CDFSource() {
            public String getName() {return _fname;};
            public boolean isFile() {return true;};
        });
        CDFReader rdr = new CDFReader();
        rdr.setImpl(cdf);
        return rdr;
    }

    /**
     * creates  CDFReader object from a URL using array backed ByteBuffer.
     */
    public static CDFReader getReader(URL url) throws
        CDFException.ReaderError {
        CDFImpl cdf = null;
        try {
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
            ((HttpURLConnection)con).disconnect();
            ByteBuffer buf = ByteBuffer.wrap(ba);
            cdf = CDFFactory.getVersion(buf);
        } catch (Throwable th) {
            throw new CDFException.ReaderError("I/O Error reading " + url);
        }
        CDFReader rdr = new CDFReader();
        rdr.setImpl(cdf);
        final String _url = url.toString();
        cdf.setSource(new CDFFactory.CDFSource() {
            public String getName() {return _url;};
            public boolean isFile() {return false;};
        });
        return rdr;
    }

    static CDFImpl getVersion(ByteBuffer buf, FileChannel ch) throws
        Throwable {
        LongBuffer lbuf = buf.asLongBuffer();
        long magic = lbuf.get();
        if (magic == CDFFactory.CDF3_MAGIC) {
            return new CDF3Impl(buf, ch);
        }
        if (magic == CDFFactory.CDF3_COMPRESSED_MAGIC) {
            ByteBuffer mbuf = CDFFactory.uncompressed(buf, 3);
            return new CDF3Impl(mbuf);
        }
        if (magic == CDFFactory.CDF2_MAGIC_DOT5) {
            int release = buf.getInt(24);
            return new CDF2Impl(buf, release, ch);
        } else {
            ShortBuffer sbuf = buf.asShortBuffer();
            if (sbuf.get() == (short)0xcdf2) {
                if (sbuf.get() == (short)0x6002) {
                    short x = sbuf.get();
                    if (x == 0) {
                        if (sbuf.get() == -1) {
                            return new CDF2Impl(buf, 6, ch);
                        }
                    } else {
                        if ((x == (short)0xcccc) && (sbuf.get() == 1)) {
                            // is compressed - positioned at CCR
                            ByteBuffer mbuf = CDFFactory.uncompressed(buf, 2);
                            return new CDF2Impl(mbuf, 6, ch);
                        }
                    }
                        
                }
            }
        }
        return null;
    }
    public static CDFReader getReader(String fname, boolean map) throws
        CDFException.ReaderError {
        CDFImpl cdf = null;
        File file = new File(fname);
        try {
            int len = (int)file.length();
            byte[] ba = new byte[len];
            int rem = len;
            FileInputStream fis = new FileInputStream(file);
            int n = 0;
            while (rem > 0) {
                len = fis.read(ba, n, rem);
                n += len;
                rem -= len;
            }
            fis.close();
            cdf = CDFFactory.getCDF(ba);
        } catch (Throwable th) {
            throw new CDFException.ReaderError("I/O Error reading " + fname);
        }
        final String _fname = file.getPath();
        cdf.setSource(new CDFFactory.CDFSource() {
            public String getName() {return _fname;};
            public boolean isFile() {return true;};
        });
        CDFReader rdr = new CDFReader();
        rdr.setImpl(cdf);
        return rdr;
    }
}
