package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class LongArray extends AArray {
    public LongArray(Object o) throws Throwable {
        super(o);
    }
    public LongArray(Object o, boolean majority) throws Throwable {
        super(o, majority);
    }
    public Object array() {
        switch (dim) {
        case 1:
            return (long[])o;
        case 2:
            return (long[][])o;
        case 3:
            return (long[][][])o;
        case 4:
            return (long[][][][])o;
        }
        return null;
    }
    
    /**
     * create a byte buffer of a compatible type.
     */
    public ByteBuffer buffer(Class<?> cl, int ignore) throws Throwable {
        if (!((cl == Long.TYPE) || (cl == Integer.TYPE))) {
            throw new Throwable("Only int and long targets supported");
        }
        if (dim > 4) throw new Throwable("Rank > 4 not supported");
        int elementSize = (cl == Integer.TYPE)?4:8;
        ByteBuffer buf = allocate(elementSize);
        if (cl == Integer.TYPE) return doInt(buf);
        return doLong(buf);
    }

    ByteBuffer doInt(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        int[] temp = null;
        IntBuffer _buf = buf.asIntBuffer();
        switch (dim) {
        case 1:
            long[] data = (long[])o;
            temp = new int[data.length];
            for (int i = 0; i < data.length; i++) temp[i] = (int)data[i];
            _buf.put(temp);
            return buf;
        case 2:
            long[][] data2 = (long[][])o;
            temp = new int[_dim[1]];
            for (int i = 0; i < _dim[0]; i++) {
                long[] di = data2[i];
                for (int j = 0; j < _dim[1]; j++) temp[j] = (int)di[j];
                _buf.put(temp);
            }
            return buf;
        case 3:
            long[][][] data3 = (long[][][])o;
            if (rowMajority) {
                temp = new int[_dim[2]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        long[] di = data3[i][j];
                        for (int k = 0; k < _dim[2]; k++) temp[k] = (int)di[k];
                        _buf.put(temp);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            _buf.put((int)data3[i][j][k]);
                        }
                    }
                }
            }
            return buf;
        case 4:
            long[][][][] data4 = (long[][][][])o;
            if (rowMajority) {
                temp = new int[_dim[3]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            long[] di = data4[i][j][k];
                            for (int l = 0; l < _dim[3]; l++) {
                                temp[l] = (int)di[l];
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
                                _buf.put((int)data4[i][j][k][l]);
                            }
                        }
                    }
                }
            }
            return buf;
        }
        return null;
    }

    ByteBuffer doLong(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        LongBuffer _buf = buf.asLongBuffer();
        switch (dim) {
        case 1:
            long[] data = (long[])o;
            _buf.put(data);
            return buf;
        case 2:
            long[][] data2 = (long[][])o;
            for (int i = 0; i < _dim[0]; i++) {
                _buf.put(data2[i]);
            }
            return buf;
        case 3:
            long[][][] data3 = (long[][][])o;
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
            long[][][][] data4 = (long[][][][])o;
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
