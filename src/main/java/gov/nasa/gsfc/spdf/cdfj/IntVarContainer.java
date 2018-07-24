package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public final class IntVarContainer extends BaseVarContainer implements
    VDataContainer.CInt {
    final int[] ipad;
    public IntVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve, ByteOrder bo) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        super(thisCDF, var, pt, preserve, bo, Integer.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        double[] dpad = (double[])pad;
        ipad = new int[dpad.length];
        for (int i = 0; i < dpad.length; i++) ipad[i] = (int)dpad[i];
    }

    public IntVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        this(thisCDF, var, pt, preserve, ByteOrder.nativeOrder());
    }

    ByteBuffer allocateBuffer(int words) {
        ByteBuffer _buf = ByteBuffer.allocateDirect(4*words);
        _buf.order(order);
        return _buf;
    }

    public Object allocateDataArray(int size) {
        return new int[size];
    }

    void doMissing(int records, ByteBuffer _buf, Object _data, int rec) {
        int[] data = (int[])_data;
        int[] repl = null;
        try {
            repl = (rec < 0)?ipad:var.asIntArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int position = _buf.position();
        IntBuffer ibuf = _buf.asIntBuffer();
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
            ibuf.put(data, 0, tofill*elements);
            position += 4*tofill*elements;
            rem -= tofill;
        }
        _buf.position(position);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data) throws Throwable {
        int[] data = (int[])_data;
        int position = _buf.position();
        IntBuffer ibuf = _buf.asIntBuffer();
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
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
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
                    for (int e = 0; e < _num; e++) {
                        data[e] = bvs.get();
                    }
                    ipos += 2*_num;
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
                    processed += (_num/elements);
                 }
                 bv.position(ipos);
                 _buf.position(position);
                 break;
             }
            if (type == 4) {
                ipos = bv.position();
                IntBuffer bvi = bv.asIntBuffer();
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    bvi.get(data, 0, _num);
                    ipos += 4*_num;
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
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
                        data[e] = (x < 0)?(x + 256):x;
                    }
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
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
                    for (int e = 0; e < _num; e++) {
                        int x = bvs.get();
                        data[e] = (x < 0)?(x + (1 << 16)):x;
                    }
                    ipos += 2*_num;
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
                    processed += (_num/elements);
                 }
                 bv.position(ipos);
                 _buf.position(position);
                 break;
             }
             if (type == 14) {
                ipos = bv.position();
                IntBuffer bvi = bv.asIntBuffer();
                while (processed < toprocess) {
                    int _num = (toprocess - processed)*elements;
                    if (_num > data.length) _num = data.length;
                    bvi.get(data, 0, _num);
                    ipos += 4*_num;
                    ibuf.put(data, 0, _num);
                    position += 4*_num;
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
        return isCompatible(type, preserve, Integer.TYPE);
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) throw new Throwable("Rank > 4 not supported yet.");
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/4;
        IntBuffer _buf = buf.asIntBuffer();
        int records = -1;
        switch (rank) {
        case 0:
            int[] _a0 = new int[words];
            _buf.get(_a0);
            return (singlePoint)?new Integer(_a0[0]):_a0;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            int[][] _a1 = new int[records][n];
            for (int r = 0; r < records; r++) _buf.get(_a1[r]);
            return (singlePoint)?_a1[0]:_a1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            int[][][] _a2 = new int[records][n0][n1];
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
            int[][][][] _a3 = new int[records][n0][n1][n2];
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
            int[][][][][] _a4 = new int[records][n0][n1][n2][n3];
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

    public void fillArray(int[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.asIntBuffer().get(array, offset, words);
    }

    public int[] as1DArray() {return (int[])super.as1DArray();}
    public int[] asOneDArray() {return (int[])super.asOneDArray(true);}
    public int[] asOneDArray(boolean cmtarget) {
        return (int[])super.asOneDArray(cmtarget);
    }
    public IntArray asArray() throws Throwable {
        return new IntArray(_asArray());
    }
}
