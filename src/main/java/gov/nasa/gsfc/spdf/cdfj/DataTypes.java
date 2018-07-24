package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.io.*;
import java.lang.reflect.*;
public final class DataTypes {
    public static final int ENCODING_COUNT = 17;
    static final ByteOrder[] endian_ness = new ByteOrder[ENCODING_COUNT];
    static {
        for (int i = 0; i < ENCODING_COUNT; i++) {
            endian_ness[i] = null;
        }
        endian_ness[1] = ByteOrder.BIG_ENDIAN;
        endian_ness[2] = ByteOrder.BIG_ENDIAN;
        endian_ness[4] = ByteOrder.LITTLE_ENDIAN;
        endian_ness[5] = ByteOrder.BIG_ENDIAN;
        endian_ness[6] = ByteOrder.LITTLE_ENDIAN;
        endian_ness[7] = ByteOrder.BIG_ENDIAN;
        endian_ness[9] = ByteOrder.BIG_ENDIAN;
        endian_ness[12] = ByteOrder.BIG_ENDIAN;
        endian_ness[13] = ByteOrder.LITTLE_ENDIAN;
        endian_ness[16] = ByteOrder.LITTLE_ENDIAN;
    }

    public static final int EPOCH16 = 32;
    public static final int CDF_TIME_TT2000 = 33;
    public static final int FLOAT = 0;
    public static final int DOUBLE = 1;
    public static final int SIGNED_INTEGER = 2;
    public static final int UNSIGNED_INTEGER = 3;
    public static final int STRING = 4;
    public static final int LONG = 5;
    public static final int LAST_TYPE = 53;
    static final Method[] method = new Method[LAST_TYPE];
    static final int[] typeCategory = new int[LAST_TYPE];
    static final int[] size = new int[LAST_TYPE];
    static final long[] longInt = new long[LAST_TYPE];
    static {
        for (int i = 0; i < LAST_TYPE; i++) {
            method[i] = null;
            size[i] = 1;
            typeCategory[i] = -1;
        }
        // byte
        Class bb = ByteBuffer.class;
        try {
            Method meth = bb.getMethod("get", new Class[] {});
            method[1] = meth;
            typeCategory[1] = SIGNED_INTEGER;
            method[11] = meth;
            typeCategory[11] = UNSIGNED_INTEGER;
            method[41] = meth;
            typeCategory[41] = SIGNED_INTEGER;
            meth = bb.getMethod("getShort", new Class[] {});
            method[2] = meth;
            typeCategory[2] = SIGNED_INTEGER;
            size[2] = 2;
            method[12] = meth;
            typeCategory[12] = UNSIGNED_INTEGER;
            size[12] = 2;
            meth = bb.getMethod("getInt", new Class[] {});
            method[4] = meth;
            typeCategory[4] = SIGNED_INTEGER;
            size[4] = 4;
            method[14] = meth;
            typeCategory[14] = UNSIGNED_INTEGER;
            size[14] = 4;
            meth = bb.getMethod("getLong", new Class[] {});
            method[8] = meth;
            typeCategory[8] = LONG;
            size[8] = 8;
            method[33] = meth;
            typeCategory[33] = LONG;
            size[33] = 8;
            meth = bb.getMethod("getFloat", new Class[] {});
            method[21] = meth;
            typeCategory[21] = FLOAT;
            size[21] = 4;
            method[44] = meth;
            typeCategory[44] = FLOAT;
            size[44] = 4;
            meth = bb.getMethod("getDouble", new Class[] {});
            method[22] = meth;
            typeCategory[22] = DOUBLE;
            size[22] = 8;
            method[45] = meth;
            typeCategory[45] = DOUBLE;
            size[45] = 8;
            method[31] = meth;
            typeCategory[31] = DOUBLE;
            size[31] = 8;
            method[32] = meth;
            typeCategory[32] = DOUBLE;
            size[32] = 8;
            typeCategory[41] = SIGNED_INTEGER;
            typeCategory[51] = STRING;
            typeCategory[52] = STRING;
        } catch (Exception ex) {
        }
        for (int i = 0; i < LAST_TYPE; i++) {
            if (size[i] <= 4) longInt[i] = ((long)1) << 8*size[i];
        }
    }
    public DataTypes() {
        Class tc = getClass();
        try {
            Method meth = tc.getMethod("getString",
                new Class[] {ByteBuffer.class, Integer.class});
            method[51] = meth;
            method[52] = meth;
        } catch (Exception ex) {
        }
    }
    public static String getString(ByteBuffer buf, Integer nc)  {
        ByteBuffer slice = buf.slice();
        byte [] ba = new byte[nc.intValue()];
        int i = 0;
        for (; i < ba.length; i++) {
            ba[i] = slice.get();
            if (ba[i] == 0) break;
        }
        return new String(ba, 0, i);
    }
    public static ByteOrder getByteOrder(int encoding) throws Throwable {
        if (endian_ness[encoding] != null) return endian_ness[encoding];
        throw new Throwable("Unsupported encoding " + encoding);
    }
    public static boolean isStringType(int type) {
        return (typeCategory[type] == STRING);
    }
    public static boolean isLongType(int type) {
        return (typeCategory[type] == LONG);
    }
    public static Object defaultPad(int type) {
        if (isLongType(type)) return new Long(-9223372036854775807L);
        if (isStringType(type)) return new Byte(" ".getBytes()[0]);
        return new Double(0);
    }
}
