package gov.nasa.gsfc.spdf.cdfj;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.*;
public class DataContainer {
    VDR vdr;
    VXR vxr;
    Vector<Integer> firstRecords = new Vector<Integer>();
    Vector<Integer> lastRecords = new Vector<Integer>();
    Vector<ByteBuffer> bufs = new Vector<ByteBuffer>();
    Vector<Integer> points = new Vector<Integer>();
    protected long position;
    static final int VVR_PREAMBLE = 12;
    static final int CVVR_PREAMBLE = 24;
    final boolean rowMajority;
    final int CXR_MAX_ENTRIES = 6;
    public DataContainer(VDR vdr) {
        this(vdr, true);
    }
    public DataContainer(VDR vdr, boolean rowMajority) {
        this.vdr = vdr;
        vxr = new VXR();
        this.rowMajority = rowMajority;
    }
    public VDR getVDR() {return vdr;}
    public VXR getVXR() {return vxr;}
    CPR cpr;
    DataContainer timeContainer;
    Vector<Integer> _firstRecords;
    Vector<Integer> _lastRecords;
    Vector<ByteBuffer> _bufs;
    void setTimeContainer(DataContainer dc) {timeContainer = dc;}
    Boolean phantom = null;
    void addPhantomEntry() {
        if (phantom != null) return;
        firstRecords.add(new Integer(-1));
        lastRecords.add(new Integer(-1));
        bufs.add(null);
        phantom = Boolean.TRUE;
    }
    public void addData(Object data, int[] recordRange, boolean oned) throws
        Throwable {
        addData(data,recordRange, oned, false);
    }
    Boolean _doNotCompress = null;
    boolean doNotCompress = false;
    void addData(Object data, int[] recordRange, boolean oned,
        boolean relax) throws Throwable {
        ByteBuffer buf = null;
        if (ByteBuffer.class.isAssignableFrom(data.getClass())) {
            buf = (ByteBuffer)data;
            if (DataTypes.size[vdr.dataType] > 1) {
                if (buf.order() != ByteOrder.LITTLE_ENDIAN) {
                    throw new Throwable("For data types of size > 1, " +
                    "supplied buffer must be in LITTLE_ENDIAN order");
                }
            }
            if (vdr.isCompressed()) {
                if (recordRange == null) {
                    throw new Throwable("Record range must be specified " +
                    "since " + vdr.getName() + "is to be stored as compressed."
                    );
                }
                if (_doNotCompress == null) {
                    doNotCompress = (recordRange.length == 2);
                    _doNotCompress = new Boolean(doNotCompress);
                } else {
                    if ((doNotCompress && (recordRange.length > 2)) ||
                       (!doNotCompress && (recordRange.length == 2))) {
                       String t = "compressed";
                       if (!doNotCompress) t = "uncompressed";
                        throw new Throwable("Changing compression mode of" +
                        " input. Previous = " + t + ".");
                    }    
                }    
            }
        } else {
            if (!(data.getClass().isArray())) {
                throw new Throwable("supplied object not an array");
            }
        }
        int first = (recordRange == null)?0:recordRange[0];
        if (lastRecords.size() > 0) {
            int _last = -1;
            if (timeContainer != null) {
                _last = timeContainer.getLastRecord(lastRecords.size() - 1);
            } else {
                _last = getLastRecord();
            }
            if (recordRange == null) {
                first = _last + 1;
                int expected = getLastRecord() + 1;
                if ((first - expected) > 0) {
                    if (vdr.sRecords == 0) {
                        System.out.println("Gap: " + expected + " - " +
                        first + " for " + vdr.getName());
                        throw new Throwable(
                        " SparseRecordOption must be set. There are " +
                        " missing records between files for " +
                        vdr.getName());
                    }
                }
            } else {
                if (recordRange[0] <= _last) {
                    throw new Throwable("first record " + recordRange[0] +
                    " must follow the last seen record " + _last);
                }
                if (recordRange[0] > (_last + 1)) {
                    if (vdr.sRecords == 0) {
                        throw new Throwable("Specified start of the range " +
                        recordRange[0] + " does not follow " +
                        "last record " + _last + " immediately." +
                       " SparseRecordOption must be set if the CDF is missing" +
                        " records");
                    }
                }
            }
        } else { // first cannot be nonzero unless sparseness option was chosen
            if (first != 0) {
                if (vdr.sRecords == 0) {
                    throw new Throwable("SparseRecordOption " +
                   "must be set if the CDF is missing records");
                }
            }
        }
        boolean done = false;
        int npt = 0;
        int last = -1;
        if (!done && (buf != null)) {
            if (recordRange == null) {
                npt = buf.remaining()/DataTypes.size[vdr.dataType];;
                npt /= vdr.itemsPerPoint;
                last = first + npt -1;
            } else {
                last = recordRange[1];
                npt = last - first + 1;
            }
            firstRecords.add(new Integer(first));
            lastRecords.add(new Integer(last));
            bufs.add(buf);
            points.add(new Integer(npt));
            return;
        }
        ArrayAttribute aa = new ArrayAttribute(data);
//      if (aa.getDimensions().length > 1) {
        if (!oned ) {
/*
            if (!rowMajority) throw new Throwable("column majority " +
                "feature not supported in this context in this version.");
*/
            npt = java.lang.reflect.Array.getLength(data);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record" +
                " range");
            }
            Vector<Integer> vdim = null;
            if (vdr.dataType == 32) {
                vdim = new Vector<Integer>();
                vdim.add(new Integer(2));
            } else {
                vdim = vdr.efdim;
            }
            if (vdim.size() > 0) {
                int[] dcheck = new int[1 + vdim.size()];
                dcheck[0] = npt;
                for (int i = 0; i < vdim.size(); i++) {
                    dcheck[i + 1] = (vdim.get(i)).intValue();
                }
                //if (!(new AArray(data)).validateDimensions(dcheck)) {
                if (!Arrays.equals(aa.getDimensions(), dcheck)) {
                    StringBuffer sbe = new StringBuffer();
                    for (int i = 0; i < dcheck.length; i++) {
                        sbe.append("," + dcheck[i]);
                    }
                    StringBuffer sbf = new StringBuffer("");
                    int[] fdim = aa.getDimensions();
                    for (int i = 0; i < fdim.length; i++) {
                        sbf.append("," + fdim[i]);
                    }
                    throw new Throwable("Dimension mismatch, expected: " +
                        sbe + " found " + sbf + ".");
                }
            }
            last = first + npt -1;
            buf = addJavaArray(data, vdr.dataType, relax);
            if (buf != null) done = true;
        }
        if (!done && ((vdr.dataType == 1) || 
                      (relax && (vdr.dataType == 11)) ||
            ((vdr.dataType > 50) && (aa.getType() == Byte.TYPE)))) {
            byte[] values = (byte[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            buf = ByteBuffer.wrap(values);
            last = first + npt -1;
            done = true;
        }
        if (!done && ((vdr.dataType == 2) ||
                      (relax && (vdr.dataType == 12)))) {
            short[] values = (short[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(2*values.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                buf.asShortBuffer().put(values);
            } else {
                for (int i = 0; i < values.length; i++) buf.putShort(values[i]);
                buf.position(0);
            }
            done = true;
        }
        if (!done && ((vdr.dataType == 4) ||
                      (relax && (vdr.dataType == 14)))) {
            int[] values = (int[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(4*values.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                buf.asIntBuffer().put(values);
            } else {
                for (int i = 0; i < values.length; i++) buf.putInt(values[i]);
                buf.position(0);
            }
            done = true;
        }
        if (!done && ((vdr.dataType == 21) || (vdr.dataType == 44))) {
            float[] values = (float[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(4*values.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                buf.asFloatBuffer().put(values);
            } else {
                for (int i = 0; i < values.length; i++) buf.putFloat(values[i]);
                buf.position(0);
            }
            done = true;
        }
        if (!done && ((vdr.dataType == 22) || (vdr.dataType == 45) ||
            (vdr.dataType == 31) || (vdr.dataType == 32))) {
            double[] values = (double[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(8*values.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                buf.asDoubleBuffer().put(values);
            } else {
                for (int i = 0; i < values.length; i++) {
                    buf.putDouble(values[i]);
                }
                buf.position(0);
            }
            done = true;
        }
        if (!done && ((vdr.dataType == 33) || (vdr.dataType == 8))) {
            long[] values = (long[])data;
            npt = (values.length/vdr.itemsPerPoint);
            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(8*values.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                buf.asLongBuffer().put(values);
            } else {
                for (int i = 0; i < values.length; i++) buf.putLong(values[i]);
                buf.position(0);
            }
            done = true;
        }
        if (!done && (vdr.dataType > 50)) { // data is String[]
            String[] values = (String[])data;
            npt = (values.length*vdr.numElems/vdr.itemsPerPoint);
            //System.out.println(values.length);
            //System.out.println(vdr.itemsPerPoint);
            //System.out.println(npt);

            if (recordRange != null) {
                if (npt != (recordRange[1] - recordRange[0] + 1)) throw new
                Throwable( "array size not consistent with given record range");
            }
            last = first + npt -1;
            buf = ByteBuffer.allocateDirect(vdr.numElems*values.length);
            for (int i = 0; i < values.length; i++) {
                int len = values[i].length();
                if (len > vdr.numElems) throw new Throwable("String " +
                values[i] + " is longer than the length of variable.");
                byte[] _bar = values[i].getBytes();
                buf.put(_bar);
                for (int f = 0; f < (vdr.numElems - _bar.length); f++) {
                    buf.put((byte)0x20);
                }
            }
            buf.position(0);
            done = true;
        }
        if (!done) {
            if (relax) throw new Throwable("Unsupported data type.");
            if ((vdr.dataType > 10) && (vdr.dataType < 20)) {
                throw new Throwable("Possible incompatibility for unsigned." +
                                " Use relax = true to force acceptance");
            }
        }
        if (phantom == Boolean.TRUE) {
            firstRecords.clear();
            lastRecords.clear();
            bufs.clear();
            phantom = Boolean.FALSE;
        }
        firstRecords.add(new Integer(first));
        lastRecords.add(new Integer(last));
        bufs.add(buf);
        points.add(new Integer(npt));
    }
    long[] locs;
    VXR[] vxrs;
    public int getSize() {
        // update vdr
        int size = vdr.getSize();
        if (vdr.isCompressed()) {
            cpr = new CPR();
            cpr.position = position + size;
            vdr.setCPROffset(cpr.position);
            size += cpr.getSize();
        }
        if (bufs.size() > 0) {
            int last = -1;
            int nbuf = bufs.size() - 1;
            while (nbuf >= 0) {
                if (bufs.get(nbuf) != null) {
                    last = lastRecords.get(nbuf).intValue();
                    break;
                }
                nbuf--;
            }
            if (last < 0) return size;
            vdr.setMaxRec(last);
        } else {
            return size;
        }
        vdr.setVXRHead(position + size);
        
        _firstRecords = new Vector<Integer>();
        _lastRecords = new Vector<Integer>();
        _bufs = new Vector<ByteBuffer>();
        if (timeContainer == null) {
            int nbuf = 0;
            while (nbuf < bufs.size()) {
                if (bufs.get(nbuf) != null) {
                    _firstRecords.add(firstRecords.get(nbuf));
                    _lastRecords.add(lastRecords.get(nbuf));
                    _bufs.add(bufs.get(nbuf));
                }
                nbuf++;
            }
        } else {
            int nbuf = 0;
            while (nbuf < bufs.size()) {
                if (bufs.get(nbuf) != null) {
                    int _first = firstRecords.get(nbuf);
                    if (_first < timeContainer.firstRecords.get(nbuf)) {
                        _first = timeContainer.firstRecords.get(nbuf);
                    }
                    _firstRecords.add(_first);
                    _lastRecords.add(_first + lastRecords.get(nbuf) -
                       firstRecords.get(nbuf));
                    _bufs.add(bufs.get(nbuf));
                }
                nbuf++;
            }
        }
            
        int vxrsNeeded = _bufs.size()/CXR_MAX_ENTRIES;
        int lastVXREntries = _bufs.size() - vxrsNeeded*CXR_MAX_ENTRIES;
        if (lastVXREntries > 0) {
            vxrsNeeded++;
        } else {
            lastVXREntries = CXR_MAX_ENTRIES;
        }
        vxrs = new VXR[vxrsNeeded];
        locs = new long[_bufs.size()];
        int nbuf = 0;
        long _position = -1l;
        for (int v = 0; v < vxrs.length; v++) {
            _position = position + size;
            vxrs[v] = new VXR();
            int entries = CXR_MAX_ENTRIES;
            if (v == (vxrs.length - 1)) {
                entries = lastVXREntries;
            }
            vxrs[v].numEntries = entries;
            size += vxrs[v].getSize();
            if (!vdr.isCompressed()) {
                for (int e = 0; e < entries; e++) {
                    locs[nbuf] = position + size;
                    int len = VVR_PREAMBLE + _bufs.get(nbuf).limit();
                    size += len;
                    nbuf++;
                }
            } else {
                vdr.setBlockingFactor(getBlockingFactor());
                if (doNotCompress) {
                    for (int e = 0; e < entries; e++) {
                        locs[nbuf] = position + size;
                        int len = CVVR_PREAMBLE + _bufs.get(nbuf).limit();
                        size += len;
                        nbuf++;
                    }
                    return size;
                }
                for (int e = 0; e < entries; e++) {
                    locs[nbuf] = position + size;
                    ByteBuffer b = _bufs.get(nbuf);
                    byte[] uncompressed = null;
                    if (b.hasArray()) {
                        uncompressed = b.array();
                    } else {
                        uncompressed = new byte[b.remaining()];
                        b.get(uncompressed);
                        _bufs.setElementAt(null,nbuf);
                    }
                    ByteArrayOutputStream baos;
                    baos = new ByteArrayOutputStream(uncompressed.length);
                    try {
                        GZIPOutputStream gzos = new GZIPOutputStream(baos);
                        gzos.write(uncompressed, 0, uncompressed.length);
                        gzos.finish();
                        baos.flush();
                        b = ByteBuffer.wrap(baos.toByteArray());
                        _bufs.setElementAt(b, nbuf);
                        int len = CVVR_PREAMBLE + b.limit();
                        size += len;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    nbuf++;
                }
            }
            if (v != (vxrs.length - 1)) vxrs[v].setVXRNext(position + size);
        }
        if (vxrs.length > 1) vdr.setVXRTail(_position);
        return size;
    }

    public ByteBuffer update(ByteBuffer buf) {
        buf.position((int)position);
        buf.put(vdr.get());
        if (vdr.isCompressed()) {
            buf.put(cpr.get());
        }
        if (_bufs == null) return buf;
        int nbuf = 0;
        if (_bufs.size() > 0) {
            for (int v = 0; v < vxrs.length; v++) {
                buf.put(vxrs[v].get());
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    int n = _firstRecords.get(nbuf + e).intValue();
                    buf.putInt(n);
                }
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    int n = _lastRecords.get(nbuf + e).intValue();
                    buf.putInt(n);
                }
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    buf.putLong(locs[nbuf + e]);
                }
                if (!vdr.isCompressed()) {
                    for (int e = 0; e < vxrs[v].numEntries; e++) {
                        buf.putLong(VVR_PREAMBLE + _bufs.get(nbuf + e).limit());
                        buf.putInt(7);
                        buf.put(_bufs.get(nbuf + e));
                    }
                } else {
                    for (int e = 0; e < vxrs[v].numEntries; e++) {
                        ByteBuffer b = _bufs.get(nbuf + e);
                        buf.putLong(CVVR_PREAMBLE + b.limit());
                        buf.putInt(13);
                        buf.putInt(0);
                        buf.putLong((long)b.limit());
                        buf.put(b);
                    }
                }
                nbuf += vxrs[v].numEntries;
            }
        }
        return buf;
    }
    int getBlockingFactor() {
        int n = -1;
        for (int i = 0; i < points.size(); i++) {
            int p = points.get(i).intValue();
            if (p > n) n = p;
        }
        return n;
    }
    public ByteBuffer addJavaArray(Object data, int dataType, boolean relax)
        throws Throwable {
        ArrayAttribute aa = new ArrayAttribute(data);
        Class<?> cl = aa.getType();
        CDFDataType ctype = SupportedTypes.cdfType(dataType);
        if (ctype == null) throw new Throwable("Internal error.");
        if (cl == Long.TYPE) {
            LongArray la = new LongArray(data, rowMajority);
            boolean ok = (ctype == CDFDataType.INT8) ||
                         (ctype == CDFDataType.TT2000);
            if (ok) return la.buffer();
            if (ctype == CDFDataType.UINT4) return la.buffer(Integer.TYPE);
        }
        if (cl == Double.TYPE) {
            DoubleArray da = new DoubleArray(data, rowMajority);
            boolean ok = (ctype == CDFDataType.DOUBLE) ||
                         (ctype == CDFDataType.EPOCH) ||
                         (ctype == CDFDataType.EPOCH16);
            if (ok) return da.buffer();
            if (ctype == CDFDataType.FLOAT) return da.buffer(Float.TYPE);
        }
        if (cl == Float.TYPE) {
            FloatArray fa = new FloatArray(data, rowMajority);
            if (ctype == CDFDataType.FLOAT) return fa.buffer();
        }
        if (cl == Integer.TYPE) {
            IntArray ia = new IntArray(data, rowMajority);
            if (ctype == CDFDataType.INT4) return ia.buffer();
            if (ctype == CDFDataType.UINT2) return ia.buffer(Short.TYPE);
            if (relax && (ctype == CDFDataType.UINT4)) {
                return ia.buffer();
            }
        }
        if (cl == Short.TYPE) {
            ShortArray sa = new ShortArray(data, rowMajority);
            if (ctype == CDFDataType.INT2) return sa.buffer();
            if (ctype == CDFDataType.UINT1) return sa.buffer(Byte.TYPE);
            if (relax & (ctype == CDFDataType.UINT2)) {
                return sa.buffer();
            }
        }
        if (cl == Byte.TYPE) {
            ByteArray ba = new ByteArray(data, rowMajority);
            if (ctype == CDFDataType.INT1) return ba.buffer();
            if (relax & (ctype == CDFDataType.UINT1)) {
                return ba.buffer();
            }
        }
        if (cl == String.class) {
            StringArray st = new StringArray(data, rowMajority);
            if (ctype == CDFDataType.CHAR) return st.buffer(vdr.numElems);
        }
        return null;
    }

    int getLastRecord() {
        return getLastRecord(lastRecords.size() - 1);
    }

    int getLastRecord(int start) {
        int n = start;
	if (n < 0) return -1;
        while (n >= 0) {
            int l = lastRecords.get(n);
            if (l >= 0) return l;
            n--;
        }
        return -1;
    }

    boolean timeOrderOK(Object nextTime) {
        int last = bufs.size() - 1;
        if (last < 0) return true;
        ByteBuffer buf = null;
        while ((buf = bufs.get(last)) == null) {
            if (last == 0) break;
            last--;
        }
        if (buf == null) return true;
        if (CDFTimeType.TT2000.getValue() == vdr.dataType) {
            return (((long[])nextTime)[0] > buf.getLong(buf.limit()));
        }
        if (CDFTimeType.EPOCH16.getValue() == vdr.dataType) {
            double[] e16 = new double[2];
            e16[0] =  buf.getDouble(buf.limit() - 16);
            e16[1] =  buf.getDouble(buf.limit() - 8);
            double[] next = (double[])nextTime;
            if (next[0] > e16[0]) return true;
            if (next[0] < e16[0]) return false;
            return (next[1] > e16[1]);
        }
        double[] next = (double[])nextTime;
        return (next[0] > buf.getDouble(buf.limit() - 8));
    }

    public void update(FileChannel channel) throws IOException {
        channel.position(position);
        channel.write(vdr.get());
        if (vdr.isCompressed()) {
            channel.write(cpr.get());
        }
        if (_bufs == null) return;
        int nbuf = 0;
        ByteBuffer longbuf = ByteBuffer.allocate(8);
        ByteBuffer intbuf = ByteBuffer.allocate(4);
        if (_bufs.size() > 0) {
            for (int v = 0; v < vxrs.length; v++) {
                channel.write(vxrs[v].get());
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    int n = _firstRecords.get(nbuf + e).intValue();
                    writeInt(channel, intbuf, n);
                }
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    int n = _lastRecords.get(nbuf + e).intValue();
                    writeInt(channel, intbuf, n);
                }
                for (int e = 0; e < vxrs[v].numEntries; e++) {
                    writeLong(channel, longbuf, locs[nbuf + e]);
                }
                if (!vdr.isCompressed()) {
                    for (int e = 0; e < vxrs[v].numEntries; e++) {
                        writeLong(channel, longbuf, 
                            VVR_PREAMBLE + _bufs.get(nbuf + e).limit());
                        writeInt(channel, intbuf, 7);
                        channel.write(_bufs.get(nbuf + e));
                    }
                } else {
                    for (int e = 0; e < vxrs[v].numEntries; e++) {
                        ByteBuffer b = _bufs.get(nbuf + e);
                        writeLong(channel, longbuf, CVVR_PREAMBLE + b.limit());
                        writeInt(channel, intbuf, 13);
                        writeInt(channel, intbuf, 0);
                        writeLong(channel, longbuf, (long)b.limit());
                        channel.write(b);
                    }
                }
                nbuf += vxrs[v].numEntries;
            }
        }
    }
    void writeInt(FileChannel ch, ByteBuffer buf, int value) throws
        IOException {
        buf.position(0);
        buf.putInt(value);
        buf.position(0);
        ch.write(buf);
    }
    void writeLong(FileChannel ch, ByteBuffer buf, long value) throws
        IOException {
        buf.position(0);
        buf.putLong(value);
        buf.position(0);
        ch.write(buf);
    }
}
