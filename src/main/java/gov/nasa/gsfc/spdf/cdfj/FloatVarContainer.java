package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public final class FloatVarContainer extends BaseVarContainer implements
    VDataContainer.CFloat {
    final float[] fpad;
    public FloatVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve, ByteOrder bo) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        super(thisCDF, var, pt, preserve, bo, Float.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            long[] lpad = (long[])pad;
            fpad = new float[lpad.length];
            for (int i = 0; i < lpad.length; i++) fpad[i] = (float)lpad[i];
        } else {
            double[] dpad = (double[])pad;
            fpad = new float[dpad.length];
            for (int i = 0; i < dpad.length; i++) fpad[i] = (float)dpad[i];
        }
    }

    public FloatVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
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
        return new float[size];
    }

    void doMissing(int records, ByteBuffer _buf, Object _data, int rec) {
        float[] data = (float[])_data;
        float[] repl = null;
        try {
            repl = (rec < 0)?fpad:var.asFloatArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int position = _buf.position();
        FloatBuffer fbuf = _buf.asFloatBuffer();
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
            fbuf.put(data, 0, tofill*elements);
            position += 4*tofill*elements;
            rem -= tofill;
        }
        _buf.position(position);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data) throws
        IllegalAccessException, InvocationTargetException {
        float[] data = (float[])_data;
        int position = _buf.position();
        FloatBuffer fbuf = _buf.asFloatBuffer();
        Method method;
        int processed = 0;
        switch (DataTypes.typeCategory[type]) {
        case 0:
            int ipos = bv.position();
            FloatBuffer bvf = bv.asFloatBuffer();
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvf.get(data, 0, _num);
                ipos += 4*_num;
                fbuf.put(data, 0, _num);
                position += 4*_num;
                processed += (_num/elements);
            }
            bv.position(ipos);
            _buf.position(position);
            break;
        case 1:
            double[] td = new double[data.length];
            ipos = bv.position();
            DoubleBuffer bvd = bv.asDoubleBuffer();
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvd.get(td, 0, _num);
                ipos += 8*_num;
                for (int n = 0; n < _num; n++) {
                    data[n] = (float)td[n];
                }
                fbuf.put(data, 0, _num);
                position += 4*_num;
                processed += (_num/elements);
            }
            bv.position(ipos);
            _buf.position(position);
            break;
        case 2:
            method = DataTypes.method[type];
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                for (int e = 0; e < _num; e++) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[e] = num.floatValue();
                }
                fbuf.put(data, 0, _num);
                position += 4*_num;
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
                    data[e] = (x >= 0)?(float)x:(float)(longInt + x);
                }
                fbuf.put(data, 0, _num);
                position += 4*_num;
                processed += (_num/elements);
            }
            _buf.position(position);
            break;
        case 5:
            ipos = bv.position();
            LongBuffer bvl = bv.asLongBuffer();
            long[] tl = new long[data.length];
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvl.get(tl, 0, _num);
                ipos += 8*_num;
                for (int n = 0; n < _num; n++) {
                    data[n] = (float)tl[n];
                }
                fbuf.put(data, 0, _num);
                position += 4*_num;
                processed += (_num/elements);
            }
            bv.position(ipos);
            _buf.position(position);
            break;
        }
    }
    public static boolean isCompatible(int type, boolean preserve) {
        return isCompatible(type, preserve, Float.TYPE);
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) throw new Throwable("Rank > 4 not supported yet.");
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/4;
        FloatBuffer _buf = buf.asFloatBuffer();
        int records = -1;
        switch (rank) {
        case 0:
            float[] _a0 = new float[words];
            _buf.get(_a0);
            return (singlePoint)?new Float(_a0[0]):_a0;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            float[][] _a1 = new float[records][n];
            for (int r = 0; r < records; r++) _buf.get(_a1[r]);
            return (singlePoint)?_a1[0]:_a1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            float[][][] _a2 = new float[records][n0][n1];
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
            float[][][][] _a3 = new float[records][n0][n1][n2];
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
            float[][][][][] _a4 = new float[records][n0][n1][n2][n3];
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
    public void fillArray(float[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.asFloatBuffer().get(array, offset, words);
    }
    public float[] as1DArray() {return (float[])super.as1DArray();}
    public float[] asOneDArray() {return (float[])super.asOneDArray(true);}
    public float[] asOneDArray(boolean cmtarget) {
        return (float[])super.asOneDArray(cmtarget);
    }
    public FloatArray asArray() throws Throwable {
        return new FloatArray(_asArray());
    }
/*
    public void run() {
        super.run();
    }
*/
}
