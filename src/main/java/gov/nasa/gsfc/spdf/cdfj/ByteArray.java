package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class ByteArray extends AArray {
    public ByteArray(Object o) throws Throwable {
        super(o);
    }
    public ByteArray(Object o, boolean rowMajority) throws Throwable {
        super(o, rowMajority);
    }
    public Object array() {
        switch (dim) {
        case 1:
            return (byte[])o;
        case 2:
            return (byte[][])o;
        case 3:
            return (byte[][][])o;
        case 4:
            return (byte[][][][])o;
        }
        return null;
    }
    
    public ByteBuffer buffer(Class<?> cl, int ignore) throws Throwable {
        if (!(cl == Byte.TYPE)) {
            throw new Throwable("Only byte targets supported");
        }
        if (dim > 4) throw new Throwable("Rank > 4 not supported");
        ByteBuffer buf = allocate(1);
        int[] _dim = aa.getDimensions();
        switch (dim) {
        case 1:
            byte[] data = (byte[])o;
            buf.put(data);
            buf.flip();
            return buf;
        case 2:
            byte[][] data2 = (byte[][])o;
            for (int i = 0; i < _dim[0]; i++) {
                buf.put(data2[i]);
            }
            buf.flip();
            return buf;
        case 3:
            byte[][][] data3 = (byte[][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        buf.put(data3[i][j]);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            buf.put(data3[i][j][k]);
                        }
                    }
                }
            }
            buf.flip();
            return buf;
        case 4:
            byte[][][][] data4 = (byte[][][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            buf.put(data4[i][j][k]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int l = 0; l < _dim[3]; l++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            for (int j = 0; j < _dim[1]; j++) {
                                buf.put(data4[i][j][k][l]);
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
}
