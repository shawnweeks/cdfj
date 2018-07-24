package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public final class ShortVarContainer extends BaseVarContainer implements 
    VDataContainer.CShort {
    final short[] spad;
    public ShortVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve, ByteOrder bo) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        super(thisCDF, var, pt, preserve, bo, Short.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        double[] dpad = (double[])pad;
        spad = new short[dpad.length];
        for (int i = 0; i < dpad.length; i++) spad[i] = (short)dpad[i];
    }

    public ShortVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        this(thisCDF, var, pt, preserve, ByteOrder.nativeOrder());
    }

    ByteBuffer allocateBuffer(int words) {
        ByteBuffer _buf = ByteBuffer.allocateDirect(2*words);
        _buf.order(order);
        return _buf;
    }

    public Object allocateDataArray(int size) {
        return new short[size];
    }

    void doMissing(int records, ByteBuffer _buf, Object _data, int rec) {
        short[] data = (short[])_data;
        short[] repl = null;
        try {
            repl = (rec < 0)?spad:var.asShortArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int position = _buf.position();
        ShortBuffer sbuf = _buf.asShortBuffer();
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
            sbuf.put(data, 0, tofill*elements);
            position += 2*tofill*elements;
            rem -= tofill;
        }
        _buf.position(position);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data) throws Throwable {
        short[] data = (short[])_data;
        int position = _buf.position();
        ShortBuffer sbuf = _buf.asShortBuffer();
        int processed = 0;
        int ipos;
        switch (DataTypes.typeCategory[type]) {
        case 2:
            if ((type == 1) || (type == 41)) {
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    for (int e = 0; e < _num; e++) {
                        data[e] = bv.get();
                    }
                    sbuf.put(data, 0, _num);
                    position += 2*_num;
                    processed += (_num/elements);
                 }
                 _buf.position(position);
                 break;
            }
            if (type == 2) {
                ipos = bv.position();
                ShortBuffer bvs = bv.asShortBuffer();
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    bvs.get(data, 0, _num);
                    ipos += 2*_num;
                    sbuf.put(data, 0, _num);
                    position += 2*_num;
                    processed += (_num/elements);
                 }
                 bv.position(ipos);
                 _buf.position(position);
                 break;
            }
        case 3:
            if (type == 11) {
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    for (int e = 0; e < _num; e++) {
                        int x = bv.get();
                        data[e] = (short)((x < 0)?(x + 256):x);
                    }
                    sbuf.put(data, 0, _num);
                    position += 2*_num;
                    processed += (_num/elements);
                 }
                 _buf.position(position);
                 break;
            }
            if (type == 12) {
                ipos = bv.position();
                ShortBuffer bvs = bv.asShortBuffer();
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    bvs.get(data, 0, _num);
                    ipos += 2*_num;
                    sbuf.put(data, 0, _num);
                    position += 2*_num;
                    processed += (_num/elements);
                 }
                 bv.position(ipos);
                 _buf.position(position);
                 break;
             }
        default:
             throw new Throwable("Unrecognized type " + type);
        }
    }

    public static boolean isCompatible(int type, boolean preserve) {
        return isCompatible(type, preserve, Short.TYPE);
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) throw new Throwable("Rank > 4 not supported yet.");
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/2;
        ShortBuffer _buf = buf.asShortBuffer();
        int records = -1;
        switch (rank) {
        case 0:
            short[] _a0 = new short[words];
            _buf.get(_a0);
            return (singlePoint)?new Short(_a0[0]):_a0;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            short[][] _a1 = new short[records][n];
            for (int r = 0; r < records; r++) _buf.get(_a1[r]);
            return (singlePoint)?_a1[0]:_a1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            short[][][] _a2 = new short[records][n0][n1];
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
            short[][][][] _a3 = new short[records][n0][n1][n2];
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
            short[][][][][] _a4 = new short[records][n0][n1][n2][n3];
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
    public void fillArray(short[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.asShortBuffer().get(array, offset, words);
    }
    public short[] as1DArray() {return (short[])super.as1DArray();}
    public short[] asOneDArray() {return (short[])super.asOneDArray(true);}
    public short[] asOneDArray(boolean cmtarget) {
        return (short[])super.asOneDArray(cmtarget);
    }
    public ShortArray asArray() throws Throwable {
        return new ShortArray(_asArray());
    }
}
