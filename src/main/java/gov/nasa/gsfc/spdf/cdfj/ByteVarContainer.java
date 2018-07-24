package gov.nasa.gsfc.spdf.cdfj;
//import gov.nasa.gsfc.spdf.common.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
public class ByteVarContainer extends BaseVarContainer implements 
    VDataContainer.CByte {
    final byte[] bpad;
    public ByteVarContainer(CDFImpl thisCDF, Variable var, int[] pt) throws
        IllegalAccessException, InvocationTargetException, Throwable {
        super(thisCDF, var, pt, true, ByteOrder.BIG_ENDIAN, Byte.TYPE);
        Object pad = this.thisCDF.getPadValue(var);
        if (DataTypes.isStringType(type)) {
            String[] sa = (String[])pad;
            int count = 0;
            for (int i = 0; i < sa.length; i++) {
                count += sa[i].length();
            }
            bpad = new byte[count];
            byte[] ba;
            count = 0;
            for (int i = 0; i < sa.length; i++) {
                ba = sa[i].getBytes();
                for (int b = 0; b < ba.length; b++) bpad[count++] = ba[b];
            }
        } else {
            double[] dpad = (double[])pad;
            bpad = new byte[dpad.length];
            for (int i = 0; i < dpad.length; i++) {
                bpad[i] = (byte)dpad[i];
            }
        }
    }

    ByteBuffer allocateBuffer(int words) {
        ByteBuffer _buf = ByteBuffer.allocateDirect(words);
        _buf.order(order);
        return _buf;
    }

    public Object allocateDataArray(int size) {
        return null;
    }

    void doMissing(int records, ByteBuffer buf, Object _data, int rec) {
        byte[] repl = null;
        try {
            repl = (rec < 0)?bpad:var.asByteArray(new int[]{rec});
        } catch(Throwable th) {
            th.printStackTrace();
            System.out.println("Should not see this.");
        }
        int rem = records;
        byte[] ba = new byte[rem*elements];
        int n = 0;
        for (int i = 0; i < rem; i++) {
            ba[n++] = repl[0];;
            for (int j = 1; j < repl.length; j++) {
                ba[n++] = repl[j];;
            }
        }
        buf.put(ba, 0, rem*elements);
    }

    void doData(ByteBuffer bv, int type, int elements, int toprocess,
        ByteBuffer _buf, Object _data) {
        ByteBuffer needed = bv.slice();
        needed.limit(itemSize*toprocess);
        _buf.put(needed);
    }

    public static boolean isCompatible(int type, boolean preserve) {
        if  (isCompatible(type, preserve, Byte.TYPE)) {
/*
            boolean stringType = DataTypes.isStringType(type);
            if (stringType) return false;
*/
            return true;
        }
        return false;
    }

    public Object _asArray() throws Throwable {
        int rank = var.getEffectiveRank();
        if (rank > 4) {
            throw new Throwable("Rank > 4 not supported at this time.");
        }
        ByteBuffer buf = getBuffer();
        if (buf == null) return null;
        int words = buf.remaining();
        int records = -1;
        switch (rank) {
        case 0:
            if (singlePoint) return new Byte(buf.get());
            byte[] ba = new byte[words];
            buf.get(ba);
            return ba;
        case 1:
            int n = (((Integer)var.getElementCount().elementAt(0))).intValue();
            records = words/n;
            byte[][] ba1 = new byte[records][n];
            for (int r = 0; r < records; r++) buf.get(ba1[r]);
            return (singlePoint)?ba1[0]:ba1;
        case 2:
            int n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            int n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            records = words/(n0*n1);
            byte[][][] ba2 = new byte[records][n0][n1];
            for (int r = 0; r < records; r++) {
                for (int e0 = 0; e0 < n0; e0++) buf.get(ba2[r][e0]);
            }
            return (singlePoint)?ba2[0]:ba2;
        case 3:
            n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            int n2 = (((Integer)var.getElementCount().elementAt(2))).intValue();
            records = words/(n0*n1*n2);
            byte[][][][] ba3 = new byte[records][n0][n1][n2];
            for (int r = 0; r < records; r++) {
                for (int e0 = 0; e0 < n0; e0++) {
                    for (int e1 = 0; e1 < n1; e1++) buf.get(ba3[r][e0][e1]);
                }
            }
            return (singlePoint)?ba3[0]:ba3;
        case 4:
            n0 = (((Integer)var.getElementCount().elementAt(0))).intValue();
            n1 = (((Integer)var.getElementCount().elementAt(1))).intValue();
            n2 = (((Integer)var.getElementCount().elementAt(2))).intValue();
            int n3 = (((Integer)var.getElementCount().elementAt(3))).intValue();
            records = words/(n0*n1*n2*n3);
            byte[][][][][] ba4 = new byte[records][n0][n1][n2][n3];
            for (int r = 0; r < records; r++) {
                for (int e0 = 0; e0 < n0; e0++) {
                    for (int e1 = 0; e1 < n1; e1++) {
                        for (int e2 = 0; e2 < n2; e2++) {
                            buf.get(ba4[r][e0][e1][e2]);
                        }
                    }
                }
            }
            return (singlePoint)?ba4[0]:ba4;
        default:
            throw new Throwable("Internal error");
        }
    }

    public void fillArray(byte[] array, int offset, int first, int last)
        throws Throwable {
        if (buffers.size() == 0) throw new Throwable("buffer not available");
        int words = (last - first + 1)*elements;
        ByteBuffer b = getBuffer();
        int pos = (first - getRecordRange()[0])*elements*getLength();
        b.position(pos);
        b.get(array, offset, words);
    }
    public byte[] as1DArray() {return (byte[])super.as1DArray();}
    public byte[] asOneDArray() {return (byte[])super.asOneDArray(true);}
    public byte[] asOneDArray(boolean cmtarget) {
        return (byte[])super.asOneDArray(cmtarget);
    }
    public AArray asArray() throws Throwable {
        return new ByteArray(_asArray());
    }
}
