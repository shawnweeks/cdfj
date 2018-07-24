package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.io.*;
import java.util.*;
import gov.nasa.gsfc.spdf.cdfj.DataTypes;
public class VDR {
    ByteBuffer record = ByteBuffer.allocate(8 + 4 + 8 + 4 + 4 +
        8 + 8 + 4 + 4 + 4 + 2*4 + 4 + 4 + 8 + 4 + 256 + 4);
    long vDRNext;
    long longMask = (1l << 32) - 1;
    byte[] padValues;
    protected int position;
    String sname;
    public VDR(String name, int dataType, int[] dim, boolean[] varys,
        boolean recordVariance, boolean compressed, Object pad, int size,
        SparseRecordOption option) throws Throwable {
        sname = name;
        setName(name);
        setDataType(dataType);
        if (dim.length != varys.length) {
            throw new Throwable("Length of varys and dim arrays differ.");
        }
        numElems = size;
        itemsPerPoint = size;
        setDimensions(dim, varys, dataType);
        //setNumElems(dim, varys);
        if (compressed) flags |= 0x04;
        if (recordVariance) flags |= 0x01;
        setSparseRecordOption(option);
        if (pad != null) {
            Class<?> cl = pad.getClass();
            if (!cl.isArray()) throw new Throwable("Pad must be an array.");
            Number[] _pad = null;
            if (cl.getComponentType() != String.class) {
                _pad = new Number[1];
                if (cl.getComponentType() == Double.TYPE) {
                    _pad[0] = new Double(((double[])pad)[0]);
                }
                if (cl.getComponentType() == Float.TYPE) {
                    _pad[0] = new Float(((float[])pad)[0]);
                }
                if (cl.getComponentType() == Integer.TYPE) {
                    _pad[0] = new Integer(((int[])pad)[0]);
                }
                if (cl.getComponentType() == Long.TYPE) {
                    _pad[0] = new Long(((long[])pad)[0]);
                }
                if (cl.getComponentType() == Short.TYPE) {
                    _pad[0] = new Short(((short[])pad)[0]);
                }
                if (cl.getComponentType() == Byte.TYPE) {
                    _pad[0] = new Byte(((byte[])pad)[0]);
                }
            }
            int category = DataTypes.typeCategory[dataType];
            flags |= 0x02;
            ByteBuffer buf = null;
            if ((category == DataTypes.SIGNED_INTEGER) ||
                (category == DataTypes.UNSIGNED_INTEGER)) {
                if ((DataTypes.size[dataType] == 4) &&
                    (category == DataTypes.UNSIGNED_INTEGER)) {
                    long[] lvalues = new long[_pad.length];
                    for (int i = 0; i < lvalues.length; i++) {
                        lvalues[i] = _pad[i].longValue();
                    }
                    buf = ByteBuffer.allocate(4*lvalues.length);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    for (int i = 0; i < lvalues.length; i++) {
                        buf.putInt((int)(lvalues[i] & longMask));
                    }
                    buf.position(0);
                } else {
                    int[] values = new int[_pad.length];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = _pad[i].intValue();
                    }
                    buf = ByteBuffer.allocate(
                        DataTypes.size[dataType]*values.length);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    if (DataTypes.size[dataType] == 1) {
                        for (int i = 0; i < values.length; i++) {
                            buf.put((byte)(values[i] & 0xff));
                        }
                    } else {
                        if (DataTypes.size[dataType] == 2) {
                            for (int i = 0; i < values.length; i++) {
                                buf.putShort((short)(values[i] & 0xffff));
                            }
                        } else {
                            buf.asIntBuffer().put(values);
                        }
                    }
                }
            } else {
                if (category == DataTypes.FLOAT) {
                    float[] values = new float[_pad.length];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = _pad[i].floatValue();
                    }
                    buf = ByteBuffer.allocate(4*values.length);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    buf.asFloatBuffer().put(values);
                } else {
                    if (category == DataTypes.DOUBLE) {
                        double[] values = new double[_pad.length];
                        for (int i = 0; i < values.length; i++) {
                            values[i] = _pad[i].doubleValue();
                        }
                        buf = ByteBuffer.allocate(8*values.length);
                        buf.order(ByteOrder.LITTLE_ENDIAN);
                        buf.asDoubleBuffer().put(values);
                    } else {
                        if (category == DataTypes.LONG) {
                            long[] values = new long[_pad.length];
                            for (int i = 0; i < values.length; i++) {
                                values[i] = _pad[i].longValue();
                            }
                            buf = ByteBuffer.allocate(8*values.length);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            buf.asLongBuffer().put(values);
                        } else { 
                            if (category != DataTypes.STRING) {
                                throw new Throwable("Unrecognized type " +
                                " pad value");
                            }
                            //String[] values = (String[])pad;
                            String[] values = new String[]{((String[])pad)[0]};
                            int len = values[0].length();
                            len *= values.length;
                            buf = ByteBuffer.allocate(len);
                            for (int i = 0; i < values.length; i++) {
                                try {
                                    buf.put(values[i].getBytes());
                                } catch (Exception ex) {
                                    throw new Throwable("encoding");
                                }
                                buf.position(0);
                            }
                        }
                    }
                }
            }
            padValues = new byte[buf.limit()];
            buf.position(0);
            buf.get(padValues);
        }
    }

    public VDR(String name, int dataType, int[] dim, boolean[] varys,
        boolean compressed) throws Throwable {
        this(name, dataType, dim, varys, true, compressed, null, 1,
        SparseRecordOption.NONE);
    }
    public VDR(String name, int dataType, int[] dim, boolean[] varys)
        throws Throwable {
        this(name, dataType, dim, varys, false);
    }
    public void setVDRNext(long l) {
        vDRNext = l;
    }
    int dataType;
    public void setDataType(int n) {
        dataType = n;
    }
    int maxRec = -1;
    public void setMaxRec(int n) {
        maxRec = n;
    }
    long vXRHead;
    public void setVXRHead(long l) {
        vXRHead = l;
    }
    long vXRTail = -1l;
    public void setVXRTail(long l) {
        vXRTail = l;
    }
    int flags;
    public void setFlags(int n) {
        flags = n;
    }
    public boolean isCompressed() {return ((flags & 0x04) != 0);}
    int sRecords = 0;
    public void setSparseRecordOption(SparseRecordOption option) {
        sRecords = option.getValue();
    }
    protected int numElems = 1;
    public void setNumElems(int[] dim, boolean[] varys) {
        numElems = 1;
/* This is always 1 for numeric data
        for (int i = 0; i < dim.length; i++) {
            if (varys[i]) numElems *= dim[i];
        }
*/
    }
    int num;
    public void setNum(int n) {
        num = n;
    }
    public int getNum() {return num;}
    long cPROffset;
    public void setCPROffset(long l) {
        cPROffset = l;
    }
    int blockingFactor;
    public void setBlockingFactor(int n) {
        blockingFactor = n;
    }
    byte[] name = new byte[256];
    public void setName(String s) {
        byte[] bs = s.getBytes();
        int i = 0;
        for (; i < bs.length; i++) name[i] = bs[i];
        for (; i < name.length; i++) name[i] = 0;
    }
    int zNumDims;
    ByteBuffer dimBuf;
    protected int itemsPerPoint = 1;
    protected Vector<Integer> efdim;
    public void setDimensions(int[] dim, boolean[] varys, int dataType) {
        zNumDims = dim.length;
        if (dataType == 32) {
            itemsPerPoint = 2;
            zNumDims = 0;
        }
        efdim = new Vector<Integer>();
        if (zNumDims == 0) return;
        for (int i = 0; i < dim.length; i++) {
            if (varys[i]) itemsPerPoint *= dim[i];
        }
        dimBuf = ByteBuffer.allocate(4*zNumDims*2);
        for (int i = 0; i < zNumDims; i++) dimBuf.putInt(dim[i]);
        for (int i = 0; i < zNumDims; i++) {
            dimBuf.putInt(varys[i]?-1:0);
            if (varys[i]) efdim.add(new Integer(dim[i]));
        }
        dimBuf.position(0);
    }
    public ByteBuffer get() {
        int capacity = record.capacity();
        if (padValues != null) capacity += padValues.length;
        if (zNumDims > 0) capacity += dimBuf.capacity();
        ByteBuffer buf = ByteBuffer.allocate(capacity);
        record.position(0);
        record.putLong((long)(capacity));
        record.putInt(8);
        record.putLong(vDRNext);
        record.putInt(dataType);
        record.putInt(maxRec);
        record.putLong(vXRHead);
        record.putLong((vXRTail < 0)?vXRHead:vXRTail);
        record.putInt(flags);
        record.putInt(sRecords);
        record.putInt(0);
        record.putInt(-1);
        record.putInt(-1);
        record.putInt(numElems);
        record.putInt(num);
        record.putLong(cPROffset);
        record.putInt(blockingFactor);
        record.put(name);
        record.putInt(zNumDims);
        record.position(0);
        buf.put(record);
        if (zNumDims > 0) buf.put(dimBuf);
        if (padValues != null) buf.put(padValues);
        buf.position(0);
        return buf;
    }
    public int getSize() {
        int size = record.capacity();
        if (zNumDims > 0) size += dimBuf.capacity();
        if (padValues != null) size += padValues.length;
        return size;
    }
    public String getName() {return sname;}
}
