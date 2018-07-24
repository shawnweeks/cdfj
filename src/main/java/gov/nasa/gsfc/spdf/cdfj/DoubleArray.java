package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class DoubleArray extends AArray {
    public DoubleArray(Object o) throws Throwable {
        super(o);
    }
    public DoubleArray(Object o, boolean rowMajority) throws Throwable {
        super(o, rowMajority);
    }
    public Object array() {
        switch (dim) {
        case 1:
            return (double[])o;
        case 2:
            return (double[][])o;
        case 3:
            return (double[][][])o;
        case 4:
            return (double[][][][])o;
        }
        return null;
    }
    
    /**
     * create a byte buffer of a compatible type.
     */
    public ByteBuffer buffer(Class<?> cl, int ignore) throws Throwable {
        if (!((cl == Double.TYPE) || (cl == Float.TYPE))) {
            throw new Throwable("Only float and double targets supported");
        }
        if (dim > 4) throw new Throwable("Rank > 4 not supported");
        int elementSize = (cl == Float.TYPE)?4:8;
        ByteBuffer buf = allocate(elementSize);
        if (cl == Float.TYPE) return doFloat(buf);
        return doDouble(buf);
    }

    ByteBuffer doFloat(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        float[] temp = null;
        FloatBuffer _buf = buf.asFloatBuffer();
        switch (dim) {
        case 1:
            double[] data = (double[])o;
            temp = new float[data.length];
            for (int i = 0; i < data.length; i++) temp[i] = (float)data[i];
            _buf.put(temp);
            return buf;
        case 2:
            double[][] data2 = (double[][])o;
            temp = new float[_dim[1]];
            for (int i = 0; i < _dim[0]; i++) {
                double[] di = data2[i];
                for (int j = 0; j < _dim[1]; j++) temp[j] = (float)di[j];
                _buf.put(temp);
            }
            return buf;
        case 3:
            double[][][] data3 = (double[][][])o;
            if (rowMajority) {
                temp = new float[_dim[2]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        double[] di = data3[i][j];
                        for (int k = 0; k < _dim[2]; k++) {
                            temp[k] = (float)di[k];
                        }
                        _buf.put(temp);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            _buf.put((float)data3[i][j][k]);
                        }
                    }
                }
            }
            return buf;
        case 4:
            double[][][][] data4 = (double[][][][])o;
            if (rowMajority) {
                temp = new float[_dim[3]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            double[] di = data4[i][j][k];
                            for (int l = 0; l < _dim[3]; l++) {
                                temp[l] = (float)di[l];
                            }
                            _buf.put(temp);
                        }
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int l = 0; l < _dim[3]; l++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            for (int j = 0; j < _dim[1]; j++) {
                                _buf.put((float)data4[i][j][k][l]);
                            }
                        }
                    }
                }
            }
            return buf;
        }
        return null;
    }

    ByteBuffer doDouble(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        DoubleBuffer _buf = buf.asDoubleBuffer();
        switch (dim) {
        case 1:
            double[] data = (double[])o;
            _buf.put(data);
            return buf;
        case 2:
            double[][] data2 = (double[][])o;
            for (int i = 0; i < _dim[0]; i++) {
                _buf.put(data2[i]);
            }
            return buf;
        case 3:
            double[][][] data3 = (double[][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        _buf.put(data3[i][j]);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            _buf.put(data3[i][j][k]);
                        }
                    }
                }
            }
            return buf;
        case 4:
            double[][][][] data4 = (double[][][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            _buf.put(data4[i][j][k]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int l = 0; l < _dim[3]; l++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            for (int j = 0; j < _dim[1]; j++) {
                                _buf.put(data4[i][j][k][l]);
                            }
                        }
                    }
                }
            }
            return buf;
        }
        return null;
    }
}
