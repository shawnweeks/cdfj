package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.util.*;
public class VXR {
    ByteBuffer record = ByteBuffer.allocate(8 + 4 + 8 + 4 + 4);
    long vXRNext = 0l;
    public void setVXRNext(long l) {
        vXRNext = l;
    }
    protected int position;
    protected int numEntries;
    ByteBuffer firstbuf;
    ByteBuffer lastbuf;
    ByteBuffer locbuf;
    public void setLocations(Vector<int[]> locs) {
        numEntries = locs.size();
        firstbuf = ByteBuffer.allocate(4*numEntries);
        lastbuf = ByteBuffer.allocate(4*numEntries);
        locbuf = ByteBuffer.allocate(8*numEntries);
        for (int i = 0; i < numEntries; i++) {
            int[] locarr = locs.get(i);
            firstbuf.putInt(locarr[0]);
            lastbuf.putInt(locarr[1]);
            locbuf.putLong((long)locarr[2]);
        }
        firstbuf.position(0);
        lastbuf.position(0);
        locbuf.position(0);
    }
    public ByteBuffer get() {
        int capacity = record.capacity() + 16*numEntries;
        ByteBuffer buf = ByteBuffer.allocate(capacity);
        record.position(0);
        record.putLong((long)(capacity));
        record.putInt(6);
        record.putLong(vXRNext);
        record.putInt(numEntries);
        record.putInt(numEntries);
        record.position(0);
/*
        buf.put(record);
        buf.put(firstbuf);
        buf.put(lastbuf);
        buf.put(locbuf);
        buf.position(0);
*/
        return record;
    }
    public int getSize() {
        int size = record.capacity() + 16*numEntries;
        return size;
    }
}
