package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class VariableDataBuffer {
    int firstRecord;
    int lastRecord;
    ByteBuffer buffer;
    boolean compressed;
    VariableDataBuffer(int first, int last, ByteBuffer buf, boolean comp) {
        firstRecord = first;
        lastRecord = last;
        buffer = buf;
        compressed = comp;
    }
    public int getFirstRecord() {return firstRecord;}
    public int getLastRecord() {return lastRecord;}
    public ByteBuffer getBuffer() {return buffer;}
    public boolean isCompressed() {return compressed;}
}
