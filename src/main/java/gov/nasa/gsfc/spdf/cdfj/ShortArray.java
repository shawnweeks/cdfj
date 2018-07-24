package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class ShortArray extends AArray {
    public ShortArray(Object o) throws Throwable {
        super(o);
    }
    public ShortArray(Object o, boolean majority) throws Throwable {
        super(o, majority);
    }
    public Object array() {
        switch (dim) {
        case 1:
            return (short[])o;
        case 2:
            return (short[][])o;
        case 3:
            return (short[][][])o;
        case 4:
            return (short[][][][])o;
        }
        return null;
    }
    
    /**
     * create a byte buffer of a compatible type.
     */
    public ByteBuffer buffer(Class<?> cl, int ignore) throws Throwable {
        if (!((cl == Short.TYPE) || (cl == Byte.TYPE))) {
            throw new Throwable("Only byte and short targets supported");
        }
        if (dim > 4) throw new Throwable("Rank > 4 not supported");
        int elementSize = (cl == Byte.TYPE)?1:2;
        ByteBuffer buf = allocate(elementSize);
        if (cl == Byte.TYPE) return doByte(buf);
        return doShort(buf);
    }

    ByteBuffer doByte(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        byte[] temp = null;
        ByteBuffer _buf = buf;
        switch (dim) {
        case 1:
            short[] data = (short[])o;
            temp = new byte[data.length];
            for (int i = 0; i < data.length; i++) temp[i] = (byte)data[i];
            _buf.put(temp);
            buf.flip();
            return buf;
        case 2:
            short[][] data2 = (short[][])o;
            temp = new byte[_dim[1]];
            for (int i = 0; i < _dim[0]; i++) {
                short[] di = data2[i];
                for (int j = 0; j < _dim[1]; j++) temp[j] = (byte)di[j];
                _buf.put(temp);
            }
            buf.flip();
            return buf;
        case 3:
            short[][][] data3 = (short[][][])o;
            if (rowMajority) {
                temp = new byte[_dim[2]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        short[] di = data3[i][j];
                        for (int k = 0; k < _dim[2]; k++) temp[k] = (byte)di[k];
                        _buf.put(temp);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            _buf.put((byte)data3[i][j][k]);
                        }
                    }
                }
            }
            buf.flip();
            return buf;
        case 4:
            short[][][][] data4 = (short[][][][])o;
            if (rowMajority) {
                temp = new byte[_dim[3]];
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            short[] di = data4[i][j][k];
                            for (int l = 0; l < _dim[3]; l++) {
                                temp[l] = (byte)di[l];
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
                                _buf.put((byte)data4[i][j][k][l]);
                            }
                        }
                    }
                }
            }
            buf.flip();
            return buf;
        }
        return null;
    }

    ByteBuffer doShort(ByteBuffer buf) {
        int[] _dim = aa.getDimensions();
        ShortBuffer _buf = buf.asShortBuffer();
        switch (dim) {
        case 1:
            short[] data = (short[])o;
            _buf.put(data);
            return buf;
        case 2:
            short[][] data2 = (short[][])o;
            for (int i = 0; i < _dim[0]; i++) {
                _buf.put(data2[i]);
            }
            return buf;
        case 3:
            short[][][] data3 = (short[][][])o;
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
            short[][][][] data4 = (short[][][][])o;
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
