package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public final class DoubleVarContainer extends BaseVarContainer implements
    VDataContainer.CDouble {
    final double[] dpad;
    public DoubleVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve, ByteOrder bo) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        super(thisCDF, var, pt, preserve, bo, Double.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            long[] lpad = (long[])pad;
            dpad = new double[lpad.length];
            for (int i = 0; i < lpad.length; i++) dpad[i] = (double)lpad[i];
        } else {
            dpad = (double[])pad;
        }
    }

    public DoubleVarContainer(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        this(thisCDF, var, pt, preserve, ByteOrder.nativeOrder());
    }

    ByteBuffer allocateBuffer(int words) {
        ByteBuffer _buf = ByteBuffer.allocateDirect(8*words);
        _buf.order(order);
        return _buf;
    }

    Object allocateDataArray(int size) {
        return new double[size];
    }

    void doMissing(int records, ByteBuffer _buf, Object _data, int rec) {
        double[] data = (double[])_data;
        double[] repl = null;
        try {
            repl = (rec < 0)?dpad:var.asDoubleArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int position = _buf.position();
        DoubleBuffer dbuf = _buf.asDoubleBuffer();
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
/*
            System.out.println(dbuf + "," + tofill + "," + elements + "," +
            data.length);
*/
            dbuf.put(data, 0, tofill*elements);
            position += 8*tofill*elements;
            rem -= tofill;
        }
        _buf.position(position);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data) throws Throwable,
        IllegalAccessException, InvocationTargetException {
        double[] data = (double[])_data;
        int position = _buf.position();
        DoubleBuffer dbuf = _buf.asDoubleBuffer();
        Method method;
        int processed = 0;
        switch (DataTypes.typeCategory[type]) {
        case 0:
            float[] tf = new float[data.length];
            int ipos = bv.position();
            FloatBuffer bvf = bv.asFloatBuffer();
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvf.get(tf, 0, _num);
                ipos += 4*_num;
                for (int n = 0; n < _num; n++) {
                    data[n] = tf[n];
                }
                dbuf.put(data, 0, _num);
                position += 8*_num;
                processed += (_num/elements);
            }
            bv.position(ipos);
            _buf.position(position);
            break;
        case 1:
            ipos = bv.position();
            DoubleBuffer bvd = bv.asDoubleBuffer();
            while (processed < toprocess) {
                int _num = (toprocess - processed)*elements;
                if (_num > data.length) _num = data.length;
                bvd.get(data, 0, _num);
                ipos += 8*_num;
                dbuf.put(data, 0, _num);
                position += 8*_num;
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
                    data[e] = num.doubleValue();
                }
                dbuf.put(data, 0, _num);
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
                    data[e] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                dbuf.put(data, 0, _num);
                position += 8*_num;
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
                    data[n] = (double)tl[n];
                }
                dbuf.put(data, 0, _num);
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
        return isCompatible(type, preserve, Double.TYPE);
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) throw new Throwable("Rank > 4 not supported yet.");
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/8;
        DoubleBuffer _buf = buf.asDoubleBuffer();
        int records = -1;
        switch (rank) {
        case 0:
            double[] _a0 = new double[words];
            _buf.get(_a0);
            return (singlePoint)?new Double(_a0[0]):_a0;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            double[][] _a1 = new double[records][n];
            for (int r = 0; r < records; r++) _buf.get(_a1[r]);
            return (singlePoint)?_a1[0]:_a1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            double[][][] _a2 = new double[records][n0][n1];
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
            double[][][][] _a3 = new double[records][n0][n1][n2];
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
            double[][][][][] _a4 = new double[records][n0][n1][n2][n3];
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

    public Object asArrayElement(int[] elements) throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank != 1) throw new Throwable("Rank > 1 not supported.");
        if (!validElement(var, elements)) return null;
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/8;
        DoubleBuffer _buf = buf.asDoubleBuffer();
        int records = -1;
        int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
        records = words/n;
        if (elements.length == 1) {
            int element = elements[0];
            double[] _a1 = new double[records];
            int pos = element;
            for (int r = 0; r < records; r++) {
                _a1[r] = _buf.get(pos);
                pos += n;
            }
            return _a1;
        } else {
            int ne = elements.length;
            double[][] data = new double[records][ne];
            int pos = 0;
            for (int r = 0; r < records; r++) {
                for (int i = 0; i < ne; i++) {
                    data[r][i] = _buf.get(pos + elements[i]);
                }
                pos += n;
            }
            return data;
        }
    }

    public Object asArrayElement(int index0, int index1) throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank != 2) throw new Throwable("Rank other than 2 not supported.");
        int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
        if ((index0 < 0) || (index0 >= n0)) {
            throw new Throwable("Invalid first index " + index0);
        }
        int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
        if ((index1 < 0) || (index1 >= n1)) {
            throw new Throwable("Invalid second index " + index1);
        }
        int pointSize = n0*n1;
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = (buf.remaining())/8;
        DoubleBuffer _buf = buf.asDoubleBuffer();
        int records = words/pointSize;
        double[] _a1 = new double[records];
        int loc = (var.rowMajority())?n1*index0 + index1:n0*index1 + index0;
        int pos = 0;
        for (int r = 0; r < records; r++) {
            _a1[r] = _buf.get(pos + loc);
            pos += pointSize;
        }
        return _a1;
    }
/*
    public double[] asSampledArray(int[] stride) {
        int[] range = getRecordRange();
        Stride strideObject = new Stride(stride);
        int numberOfValues = range[1] - range[0] + 1;
        int _stride = strideObject.getStride(numberOfValues);
        if (_stride > 1) {
            int n = (numberOfValues/_stride);
            if ((numberOfValues % _stride) != 0) n++;
            numberOfValues = n;
        }
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        DoubleBuffer _buf = buf.asDoubleBuffer();
        int words = elements*numberOfValues;
        double[] sampled = new double[words];
        int advance = _stride*elements;
        int pos = 0;
        int off = 0;
        for (int i = 0; i < numberOfValues; i++) {
            for (int w = 0; w < elements; w++) {
                sampled[off++] = _buf.get(pos + w);
            }
            pos += advance;
        }
        return sampled;
    }
*/
    public void fillArray(double[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.asDoubleBuffer().get(array, offset, words);
    }
    public double[] as1DArray() {return (double[])super.as1DArray();}
    public double[] asOneDArray() {
        return (double[])super.asOneDArray(true);
    }
    public double[] asOneDArray(boolean cmtarget) {
        return (double[])super.asOneDArray(cmtarget);
    }
    public DoubleArray asArray() throws Throwable {
        return new DoubleArray(_asArray());
    }
}
