package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public final class LongVarContainer extends BaseVarContainer implements
    VDataContainer.CLong  {
    final long[] lpad;
    public LongVarContainer(CDFImpl thisCDF, Variable var, int[] pt, ByteOrder bo)
        throws IllegalAccessException, InvocationTargetException, Throwable {
        super(thisCDF, var, pt, true, bo, Long.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        if (pad.getClass().getComponentType() == Double.TYPE) {
            double[] dpad = (double[])pad;
            lpad = new long[dpad.length];
            for (int i = 0; i < lpad.length; i++) lpad[i] = (long)dpad[i];
        } else {
            lpad = (long[])this.thisCDF.getPadValue(var);
        }
    }

    public LongVarContainer(CDFImpl thisCDF, Variable var, int[] pt)
        throws IllegalAccessException, InvocationTargetException, Throwable {
        this(thisCDF, var, pt, ByteOrder.nativeOrder());
    }

    ByteBuffer allocateBuffer(int words) {
        ByteBuffer _buf = ByteBuffer.allocateDirect(8*words);
        _buf.order(order);
        return _buf;
    }

    public Object allocateDataArray(int size) {
        return new long[size];
    }

    void doMissing(int records, ByteBuffer _buf, Object _data, int rec) {
        long[] data = (long[])_data;
        long[] repl = null;
        try {
            repl = (rec < 0)?lpad:var.asLongArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int position = _buf.position();
        LongBuffer lbuf = _buf.asLongBuffer();
        int rem = records;
        while (rem > 0) {
            int tofill = rem;
            if (tofill*elements > data.length) {
                tofill = data.length/elements;
            }
            int index = 0;
            for (int i = 0; i < tofill; i++) {
                for (int e = 0; e < elements; e++) {
                    data[index++] = repl[e];
                }
            }
            lbuf.put(data, 0, tofill*elements);
            position += 8*tofill*elements;
            rem -= tofill;
        }
        _buf.position(position);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data ) throws Throwable,
        IllegalAccessException, InvocationTargetException {
        long[] data = (long[])_data;
        int position = _buf.position();
        LongBuffer lbuf = _buf.asLongBuffer();
        Method method;
        int processed = 0;
        switch (DataTypes.typeCategory[type]) {
        case 2:
            method = DataTypes.method[type];
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                for (int e = 0; e < _num; e++) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[e] = num.longValue();
                }
                lbuf.put(data, 0, _num);
                position += 8*_num;
                processed += (_num/elements);
            }
            _buf.position(position);
            break;
        case 3:
            method = DataTypes.method[type];
            long longInt = DataTypes.longInt[type];
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                for (int e = 0; e < _num; e++) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    data[e] = (x >= 0)?(long)x:(long)(longInt + x);
                }
                lbuf.put(data, 0, _num);
                position += 8*_num;
                processed += (_num/elements);
            }
            _buf.position(position);
            break;
        case 5:
            int ipos = bv.position();
            LongBuffer bvl = bv.asLongBuffer();
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvl.get(data, 0, _num);
                ipos += 8*_num;
                lbuf.put(data, 0, _num);
                position += 8*_num;
                processed += (_num/elements);
            }
            bv.position(ipos);
            _buf.position(position);
            break;
        default:
            throw new Throwable("Unrecognized data type " + type);
        }
    }

    public static boolean isCompatible(int type, boolean preserve) {
        return isCompatible(type, preserve, Long.TYPE);
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) throw new Throwable("Rank > 4 not supported yet.");
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/8;
        LongBuffer _buf = buf.asLongBuffer();
        int records = -1;
        switch (rank) {
        case 0:
            long[] _a0 = new long[words];
            _buf.get(_a0);
            return (singlePoint)?new Long(_a0[0]):_a0;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            long[][] _a1 = new long[records][n];
            for (int r = 0; r < records; r++) _buf.get(_a1[r]);
            return (singlePoint)?_a1[0]:_a1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            long[][][] _a2 = new long[records][n0][n1];
            if (var.rowMajority()) {
                for (int r = 0; r < records; r++) {
                    for (int e = 0; e < n0; e++) _buf.get(_a2[r][e]);
                }
            } else {
                for (int r = 0; r < records; r++) {
                    for (int e0 = 0; e0 < n1; e0++) {
                        for (int e1 = 0; e1 < n0; e1++) {
                            _a2[r][e1][e0] = _buf.get();
                        }
                    }
                }
            }
            return (singlePoint)?_a2[0]:_a2;
        case 3:
            n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            int n2 = (((Integer)var.getElementCount().elementAt(2))).intValue();
            records = words/(n0*n1*n2);
            long[][][][] _a3 = new long[records][n0][n1][n2];
            if (var.rowMajority()) {
                for (int r = 0; r < records; r++) {
                    for (int e0 = 0; e0 < n0; e0++) {
                        for (int e1 = 0; e1 < n1; e1++) {
                            _buf.get(_a3[r][e0][e1]);
                        }
                    }
                }
            } else {
                for (int r = 0; r < records; r++) {
                    for (int e0 = 0; e0 < n2; e0++) {
                        for (int e1 = 0; e1 < n1; e1++) {
                            for (int e2 = 0; e2 < n0; e2++) {
                                _a3[r][e2][e1][e0] = _buf.get();
                            }
                        }
                    }
                }
            }
            return (singlePoint)?_a3[0]:_a3;
        case 4:
            n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            n2 = (((Integer)var.getElementCount().elementAt(2))).intValue();
            int n3 = (((Integer)var.getElementCount().elementAt(3))).intValue();
            records = words/(n0*n1*n2*n3);
            long[][][][][] _a4 = new long[records][n0][n1][n2][n3];
            if (var.rowMajority()) {
                for (int r = 0; r < records; r++) {
                    for (int e0 = 0; e0 < n0; e0++) {
                        for (int e1 = 0; e1 < n1; e1++) {
                            for (int e2 = 0; e2 < n2; e2++) {
                                _buf.get(_a4[r][e0][e1][e2]);
                            }
                        }
                    }
                }
            } else {
                for (int r = 0; r < records; r++) {
                    for (int e0 = 0; e0 < n3; e0++) {
                        for (int e1 = 0; e1 < n2; e1++) {
                            for (int e2 = 0; e2 < n1; e2++) {
                                for (int e3 = 0; e3 < n0; e3++) {
                                    _a4[r][e3][e2][e1][e0] = _buf.get();
                                }
                            }
                        }
                    }
                }
            }
            return (singlePoint)?_a4[0]:_a4;
        default:
            throw new Throwable("Internal error");
        }
    }
    public void fillArray(long[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.asLongBuffer().get(array, offset, words);
    }
    public long[] as1DArray() {return (long[])super.as1DArray();}
    public long[] asOneDArray() {return (long[])super.asOneDArray(true);}
    public long[] asOneDArray(boolean cmtarget) {
        return (long[])super.asOneDArray(cmtarget);
    }
    public LongArray asArray() throws Throwable {
        return new LongArray(_asArray());
    }
}
