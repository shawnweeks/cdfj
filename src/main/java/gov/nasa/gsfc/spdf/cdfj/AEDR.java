package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
public class AEDR {
    static int INT_TYPE = 4;
    static int FLOAT_TYPE = 21;
    static int DOUBLE_TYPE = 22;
    static int LONG_TYPE = 8;
    static int SHORT_TYPE = 2;
    static int BYTE_TYPE = 1;
    static int STRING_TYPE = 51;
    static String STRINGDELIMITER = new String("\\N ");
    ByteBuffer record = ByteBuffer.allocate(8 + 4 + 8 + 4 + 4 +
        4 + 4 + 3*4 + 2*4);
    /**
     * Constructs AEDR of a given type and value for an ADR.
     * Specification of type is deferred if type = -1. Once set,
     * data type cannot be changed.
     */
    public AEDR(ADR adr, int type, Object value) throws Throwable {
        setAttrNum(adr.num);
        setDataType(type);
        Class<?> c = value.getClass();
        if (c == String.class) {
            String s = (String)value;
            setValues(s);
            return;
        }
        if (c.isArray() && (c.getComponentType()==String.class)) {
            String[] strings = (String[]) value;
            int x;
            StringBuffer str = new StringBuffer();
            for (x = 0; x < strings.length; ++x) {
               str.append(strings[x]);
               if (x != (strings.length - 1)) str.append(STRINGDELIMITER);
            }
            setValues(str.toString());
            return;
        }
        if (!c.isArray()) {
            throw new Throwable("supplied object not an array");
        }
        c = c.getComponentType();
        if (c == Long.TYPE) {
            long[] la = (long[])value;
            setValues(la);
            return;
        }
        if (c == Double.TYPE) {
            double[] da = (double[])value;
            setValues(da);
            return;
        }
        if (c == Float.TYPE) {
            float[] fa = (float[])value;
            setValues(fa);
            return;
        }
        if (c == Integer.TYPE) {
            int[] ia = (int[])value;
            setValues(ia);
            return;
        }
        if (c == Short.TYPE) {
            short[] sa = (short[])value;
            setValues(sa);
            return;
        }
        if (c == Byte.TYPE) {
            byte[] ba = (byte[])value;
            setValues(ba);
            return;
        }
        throw new Throwable("Arrays of type " + c + " not supported");
    }
    public AEDR(ADR adr, Object value) throws Throwable {
        this(adr, -1, value);
    }
    long aEDRNext;
    protected long position;
    public void setAEDRNext(long l) {
        aEDRNext = l;
    }
    int attributeType;
    public void setAttributeType(int n) {
        attributeType = n;
    }
    int attrNum;
    public void setAttrNum(int n) {
        attrNum = n;
    }
    int dataType = -1;
    public void setDataType(int n) throws Throwable {
        if (dataType != -1) throw new Throwable("Data type is already defined");
        dataType = n;
    }
    int num;
    public void setNum(int n) {
        num = n;
    }
    public int getNum() {return  num;}

    int numElems;
    public void setNumElems(int n) {
        numElems = n;
    }
    byte[] values;
    public void setValues(String s) throws Throwable {
        setNumElems(s.length());
        if ( dataType == -1) {
            setDataType(STRING_TYPE);
        } else {
            if ((dataType < 50) || (dataType > 52)) throw new Throwable(
            "Incompatible data type " + dataType + " for String.");
        }
        values = s.getBytes();
    }

    public void setValues(String[] s) throws Throwable {
        int x = s.length;
        int i;
        StringBuffer str = new StringBuffer();
        for (i = 0; i < x; ++i) {
           str.append(s[i]);
           if (i != (x - 1)) str.append(STRINGDELIMITER);
        }
        this.setValues(str.toString());
    }

    public void setValues(byte[] ba) throws Throwable {
        if ( dataType == -1) {
            setDataType(BYTE_TYPE);
        } else {
            if (!((dataType == 1) || (dataType == 11))) throw new Throwable(
            "Incompatible data type " + dataType + " for Byte.");
        }
        values = new byte[ba.length];
        for (int i = 0; i < ba.length; i++) values[i] = ba[i];
        setNumElems(ba.length);
    }

    public void setValues(long[] la) throws Throwable {
        if ( dataType == -1) {
            setDataType(LONG_TYPE);
        } else {
            if (!((dataType == 8) || (dataType == 33))) throw new Throwable(
            "Incompatible data type " + dataType + " for Long.");
        }
        setNumElems(la.length);
        ByteBuffer buf = ByteBuffer.allocate(8*la.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.asLongBuffer().put(la);
        values = new byte[8*la.length];
        buf.get(values);
    }

    public void setValues(double[] da) throws Throwable {
        setNumElems(da.length);
        if ( dataType == -1) {
            setDataType(DOUBLE_TYPE);
            ByteBuffer buf = ByteBuffer.allocate(8*da.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.asDoubleBuffer().put(da);
            values = new byte[8*da.length];
            buf.get(values);
            return;
        } else {
            if ((dataType == 22) || (dataType == 45) || (dataType == 31) ||
                (dataType == 32)) {
                if (dataType == 32) setNumElems(da.length/2);
                ByteBuffer buf = ByteBuffer.allocate(8*da.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.asDoubleBuffer().put(da);
                values = new byte[8*da.length];
                buf.get(values);
                return;
            }
            if ((dataType == 21) || (dataType == 44)) {
                ByteBuffer buf = ByteBuffer.allocate(4*da.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < da.length; i++) {
                    buf.putFloat((float)da[i]);
                }
                values = new byte[4*da.length];
                buf.position(0);
                buf.get(values);
                return;
            }
            if ((dataType == 1) || (dataType == 11)) {
                values = new byte[da.length];
                for (int i = 0; i < da.length; i++) {
                    values[i] = (byte)da[i];
                }
                return;
            }
            if ((dataType == 2) || (dataType == 12)) {
                ByteBuffer buf = ByteBuffer.allocate(2*da.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < da.length; i++) {
                    buf.putShort((short)da[i]);
                }
                values = new byte[2*da.length];
                buf.position(0);
                buf.get(values);
                return;
            }
            if (dataType == 4) {
                ByteBuffer buf = ByteBuffer.allocate(4*da.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < da.length; i++) {
                    buf.putInt((int)da[i]);
                }
                values = new byte[4*da.length];
                buf.position(0);
                buf.get(values);
                return;
            }
            if (dataType == 14) {
                ByteBuffer buf = ByteBuffer.allocate(4*da.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < da.length; i++) {
                    long lval = (long)da[i];
                    buf.putInt((int)lval);
                }
                values = new byte[4*da.length];
                buf.position(0);
                buf.get(values);
                return;
            }
        }
        throw new Throwable("Incompatible data type " + dataType +
        " for Double.");
    }
    public void setValues(int[] ia) throws Throwable {
        setNumElems(ia.length);
        if ( dataType == -1) {
            setDataType(INT_TYPE);
        } else {
            if (!(( dataType == 4) || ( dataType == 14))) {
                throw new Throwable("Incompatible data type " + dataType +
                " for Int.");
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(4*ia.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.asIntBuffer().put(ia);
        values = new byte[4*ia.length];
        buf.get(values);
    }
    public void setValues(float[] fa) throws Throwable {
        setNumElems(fa.length);
        if (dataType == -1) {
            setDataType(FLOAT_TYPE);
        } else {
            if (!(( dataType == 21) || ( dataType == 44))) {
                throw new Throwable("Incompatible data type " + dataType +
                " for Float.");
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(4*fa.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.asFloatBuffer().put(fa);
        values = new byte[4*fa.length];
        buf.get(values);
    }
    public void setValues(short[] sa) throws Throwable {
        setNumElems(sa.length);
        if (dataType == -1) {
            setDataType(SHORT_TYPE);
        } else {
            if (!(( dataType == 2) || ( dataType == 12))) {
                throw new Throwable("Incompatible data type " + dataType +
                " for Short.");
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(2*sa.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.asShortBuffer().put(sa);
        values = new byte[2*sa.length];
        buf.get(values);
    }
    public ByteBuffer get() {
        int capacity = record.capacity() + values.length;
        ByteBuffer buf = ByteBuffer.allocate(capacity);
        record.position(0);
        record.putLong((long)(capacity));
        record.putInt(attributeType);
        record.putLong(aEDRNext);
        record.putInt(attrNum);
        record.putInt(dataType);
        record.putInt(num);
        record.putInt(numElems);
        if (attributeType != 5 && (dataType == 51 || dataType == 52)) {
           int lastIndex = 0;
           int count = 1;
           while ((lastIndex = new String(values).indexOf(STRINGDELIMITER,
                                                          lastIndex)) != -1) {
                count++;
                lastIndex += STRINGDELIMITER.length() - 1;
           }
           record.putInt(count);
           for (int i = 0; i < 2; i++) record.putInt(0);
        } else
           for (int i = 0; i < 3; i++) record.putInt(0);
        for (int i = 0; i < 2; i++) record.putInt(-1);
        record.position(0);
        buf.put(record);
        buf.put(values);
        buf.position(0);
        return buf;
    }
    public int getSize() {
        return record.capacity() + values.length;
    }
}
