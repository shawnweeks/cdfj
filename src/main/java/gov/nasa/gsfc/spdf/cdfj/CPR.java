package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class CPR {
    ByteBuffer record = ByteBuffer.allocate(8/*RecordSize*/ +
        4/*RecordType*/ + 4/*cType*/ + 4/*rfuA*/ + 4/*pCount*/ +
        4/*cParms*/);
    protected long position;
    
    public ByteBuffer get() {
        record.position(0);
        record.putLong((long)(record.capacity()));
        record.putInt(11);
        record.putInt(5);
        record.putInt(0);
        record.putInt(1);
        record.putInt(9);
        record.position(0);
        return record;
    }
    public int getSize() {return record.capacity();}
}
