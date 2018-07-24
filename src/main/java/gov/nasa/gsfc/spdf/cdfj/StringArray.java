package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class StringArray extends AArray {
    public StringArray(Object o) throws Throwable {
        super(o);
    }
    public StringArray(Object o, boolean majority) throws Throwable {
        super(o, majority);
    }
    public Object array() {
        switch (dim) {
        case 1:
            String[] _s1 = (String[])o;
            return (String[])o;
        case 2:
            return (String[][])o;
        case 3:
            return (String[][][])o;
        case 4:
            return (String[][][][])o;
        }
        return null;
    }
    
    /**
     * create a byte buffer of a compatible type.
     */
    public ByteBuffer buffer(Class<?> cl, int size) throws Throwable {
        if (!(cl == String.class)) {
            throw new Throwable("Valid for String type only");
        }
        if (dim > 4) throw new Throwable("Rank > 4 not supported");
        ByteBuffer buf = allocate(size);
        int[] _dim = aa.getDimensions();
        switch (dim) {
        case 1:
            String[] _s1 = (String[])o;
            addString(buf, _s1, size);
            buf.flip();
            return buf;
        case 2:
            String[][] _s2 = (String[][])o;
            for (int i = 0; i < _dim[0]; i++) addString(buf, _s2[i], size);
            buf.flip();
            return buf;
        case 3:
            String[][][] _s3 = (String[][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        addString(buf, _s3[i][j], size);
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int k = 0; k < _dim[2]; k++) {
                        for (int j = 0; j < _dim[1]; j++) {
                            addString(buf, _s3[i][j][k], size);
                        }
                    }
                }
            }
            buf.flip();
            return buf;
        case 4:
            String[][][][] _s4 = (String[][][][])o;
            if (rowMajority) {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int j = 0; j < _dim[1]; j++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            addString(buf, _s4[i][j][k], size);
                        }
                    }
                }
            } else {
                for (int i = 0; i < _dim[0]; i++) {
                    for (int l = 0; l < _dim[3]; l++) {
                        for (int k = 0; k < _dim[2]; k++) {
                            for (int j = 0; j < _dim[1]; j++) {
                                addString(buf, _s4[i][j][k][l], size);
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

    void addString(ByteBuffer buf, String[] sa, int max) throws Throwable {
        for (int i = 0; i < sa.length; i++) addString(buf, sa[i], max);
    }
/*
    void addString(ByteBuffer buf, String[] sa, int max) throws Throwable {
        for (int i = 0; i < sa.length; i++) {
            int len = sa[i].length();
            if (len > max) throw new Throwable("String " + sa[i] +
                " is longer than the specified max " + max);
            byte[] _bar = sa[i].getBytes();
            buf.put(_bar);
            for (int f = 0; f < (max - _bar.length); f++) {
                buf.put((byte)0x20);
            }
        }
    }
*/
    void addString(ByteBuffer buf, String s, int max) throws Throwable {
        int len = s.length();
        if (len > max) throw new Throwable("String " + s +
            " is longer than the specified max " + max);
        byte[] _bar = s.getBytes();
        buf.put(_bar);
        for (int f = 0; f < (max - _bar.length); f++) {
            buf.put((byte)0x20);
        }
    }
}
