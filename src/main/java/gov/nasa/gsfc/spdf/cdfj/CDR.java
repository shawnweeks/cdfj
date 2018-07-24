package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class CDR {
    static int libraryVersion = 3;
    static int libraryRelease = 7;
    static int libraryIncrement = 0;
    static int libraryIdentifier = 1;
    static final byte[] magic = new byte[] {(byte)0xCD, (byte)0xF3, 0, 1, 0, 0,
        (byte)0xFF, (byte)0xFF};
    int encoding = 6;
    int flags = 0x2; // single file always
    ByteBuffer record = ByteBuffer.allocate(8 + 4 + 8 + 4 +4 +
        4 + 4 + 4 + 4 + 4 + 4 + 4 + 256);
    static String copyRight = new String("\012Common Data Format (CDF)\012https://cdf.gsfc.nasa.gov\012Space Physics Data Facility\012NASA/Goddard Space Flight Center\012Greenbelt, Maryland 20771 USA\012(User support: gsfc-cdf-support@lists.nasa.gov)\012");
    public void setEncoding(int enc) {
        encoding = enc;
    }

    public void setRowMajority(boolean majority) {
        if (majority) flags |= 1;
        if (!majority) flags &= 0xfffffffe;
    }
    
    public void setMD5Needed(boolean needDigest) {
        if (needDigest) flags |= 0xc;
        if (!needDigest) flags &= 0xfffffff3;
    }

    public ByteBuffer get() {
        record.position(0);
        record.putLong((long)(record.capacity()));
        record.putInt(1);
        record.putLong((long)(record.capacity()) + magic.length);
        record.putInt(libraryVersion);
        record.putInt(libraryRelease);
        record.putInt(encoding);
        record.putInt(flags);
        record.putInt(0);
        record.putInt(0);
        record.putInt(libraryIncrement);
        record.putInt(libraryIdentifier);
        record.putInt(0);
        record.put(copyRight.getBytes());
        int len = 256 - copyRight.length();
        record.put(String.format("%-"+len+"."+len+"s"," ").getBytes());
//        for (int i = copyRight.length(); i < 256; i++) {
//            record.put((byte)0x20);
//        }
        record.position(0);
        ByteBuffer buf = ByteBuffer.allocate(record.capacity() + magic.length);
        buf.put(magic);
        buf.put(record);
        buf.position(0);
        return buf;
    }
    public int getSize() {return record.capacity()+ magic.length;}
}
