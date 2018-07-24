package gov.nasa.gsfc.spdf.cdfj;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
public class Extractor {
    static int MAX_ARRAY = 3;
    static Hashtable numericMethodMap = new Hashtable();
    static Hashtable stringMethodMap = new Hashtable();
    public static void addFunction(String func, Class cl, Class[][] args) {
        Method[] ma = new Method[MAX_ARRAY + 1];
        for (int j = 0; j <= MAX_ARRAY; j++) {
            if (args[j] == null) continue;
            try {
                ma[j] = cl.getMethod("get" + func + j, args[j]);
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        if (numericMethodMap.put(func, ma) != null) {
            System.out.println("replacing value for the function " + func);
        }
    }

    static {
        Class cl = null;
        Class cdfClass = null;
        Class variableClass = null;
        try {
            cl  = Class.forName("gov.nasa.gsfc.spdf.cdfj.Extractor");
            cdfClass = Class.forName("gov.nasa.gsfc.spdf.cdfj.CDFImpl");
            variableClass =
                Class.forName("gov.nasa.gsfc.spdf.cdfj.Variable");
        } catch (ClassNotFoundException ex) {
        }
        int[] ia = new int[0];
        double[] da = new double[0];
        // Series
        Class[] seriesArgs = new Class[] {cdfClass, variableClass};
        Class[][] arglist = new Class[MAX_ARRAY + 1][];
        for (int i = 0; i <= MAX_ARRAY; i++) arglist[i] = seriesArgs;
        addFunction("Series",cl,  arglist);
        // Element
        arglist = new Class[][]{
            null,
            new Class[] {cdfClass, variableClass, Integer.class},
            new Class[] {cdfClass, variableClass, Integer.class,
                Integer.class},
            null};
        addFunction("Element", cl, arglist);
        // Point
        arglist = new Class[][]{
            new Class[] {cdfClass, variableClass, Integer.class},
            new Class[] {cdfClass, variableClass, Integer.class},
            new Class[] {cdfClass, variableClass, Integer.class},
            new Class[] {cdfClass, variableClass, Integer.class}};
        addFunction("Point", cl, arglist);
        // Range
        arglist = new Class[][]{
            new Class[] {cdfClass, variableClass, Integer.class, Integer.class},
            new Class[] {cdfClass, variableClass, Integer.class, Integer.class},
/*
            new Class[] {cdfClass, variableClass, Integer.class, Integer.class},
*/
            null,
            null};
        addFunction("Range", cl, arglist);
        // Elements
        arglist = new Class[][]{
            null,
            new Class[] {cdfClass, variableClass, ia.getClass()},
            null,
            null};
        addFunction("Elements", cl, arglist);
        // RangeForElements
        arglist = new Class[][]{
            null,
            new Class[] {cdfClass, variableClass, Integer.class,
                         Integer.class, ia.getClass()},
            null,
            null};
        addFunction("RangeForElements", cl, arglist);
        // RangeForElement
        arglist = new Class[][]{
            null,
            new Class[] {cdfClass, variableClass, Integer.class,
                         Integer.class, Integer.class},
            null,
            null};
        addFunction("RangeForElement", cl, arglist);
        // String rank 0 and 1 only
        Method[] ma;
        try {
            ma = new Method[] {
                cl.getMethod("getStringSeries0",
                    new Class[] {cdfClass, variableClass}),
                cl.getMethod("getStringSeries1",
                    new Class[] {cdfClass, variableClass})
            };
            stringMethodMap.put("Series", ma);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public static Method getMethod(Variable var, String func) throws 
        IllegalAccessException, InvocationTargetException {
        int rank = var.getEffectiveRank();
        Method[] ma;
        if (DataTypes.isStringType(var.getType())) {
            ma = ( Method[])stringMethodMap.get(func);
            if (ma == null) return null;
            if (rank >= ma.length ) return null;
            return ma[rank];
        }
        ma = ( Method[])numericMethodMap.get(func);
        if (ma == null) return null;
        if (DataTypes.typeCategory[var.getType()] == DataTypes.LONG) {
            if (rank > 0) return null;
        }
        return ma[rank];
    }

    public static Object getSeries0(CDFImpl thisCDF, Variable var) throws 
        IllegalAccessException, InvocationTargetException, Throwable {
        if (var.isMissingRecords()) {
             return thisCDF.get(var.getName());
        }
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        int type = var.getType();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        Number pad;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numberOfValues];
            longType = true;
            pad = new Long(((long[])getPadValue(thisCDF, var))[0]);
        } else {
            data = new double[numberOfValues];
            pad = new Double(((double[])getPadValue(thisCDF, var))[0]);
        }
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int offset = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            // fill if necessary
            if (!longType) {
                while (offset < first) data[offset++] = pad.doubleValue();
            } else {
                while (offset < first) ldata[offset++] = pad.longValue();
            }
                
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                while (offset <= last) data[offset++] = bvf.get();
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                while (offset <= last) data[offset++] = bvd.get();
                break;
            case 2:
                method = DataTypes.method[type];
                while (offset <= last) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[offset++] = num.doubleValue();
                }
                break;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                while (offset <= last) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    data[offset++] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                break;
            case 5:
                LongBuffer bvl = bv.asLongBuffer();
                while (offset <= last) ldata[offset++] = bvl.get();
                break;
            }
            if (offset > numberOfValues) break;
            if (blk == (locations.size() - 1)) {
                if (!longType) {
                    while (offset < numberOfValues) {
                        data[offset++] = pad.doubleValue();
                    }
                } else {
                    while (offset < numberOfValues) {
                        ldata[offset++] = pad.longValue();
                    }
                }
            }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = 1; i < numberOfValues; i++) {
                    data[i] = data[0];
                }
            } else {
                for (int i = 1; i < numberOfValues; i++) {
                    ldata[i] = ldata[0];
                }
            }
        }
        if (var.isMissingRecords()) {
            if (var.missingRecordValueIsPrevious()) {
                if (longType) {
                    long lpad = pad.longValue();
                    for (int i = 1; i < ldata.length; i++) {
                        if (ldata[i] == lpad) ldata[i] = ldata[i-1];
                    }
                } else {
                    double dpad = pad.doubleValue();
                    for (int i = 1; i < data.length; i++) {
                        if (data[i] == dpad) data[i] = data[i-1];
                    }
                }
            }
        }
        if (longType) return ldata;
        return data;
    }
    public static double[] castToDouble(Object o, boolean longType) {
        double[] vdata;
        if (!longType) {
            vdata = (double[])o;
        } else {
            long[] ldata = (long[])o;
            vdata = new double[ldata.length];
            for (int i = 0; i < ldata.length; i++) {
                vdata[i] = (double)ldata[i];
            }
        }
        return vdata;
    }

    // padValue when var.getPadValue() returns null
    // if fill
    public static Object getPadValue(CDFImpl thisCDF, Variable var) {
        Object o = var.getPadValue(true);
        if (o == null) {
            Object fill = getFillValue(thisCDF, var);
            boolean fillDefined = true;
            Number fillValue = null;
            if (fill.getClass().getComponentType() == Double.TYPE) {
                fillDefined =  (((double[])fill)[0] == 0);
                if (fillDefined) fillValue = new Double(((double[])fill)[1]);
            } else {
                fillDefined =  (((long[])fill)[0] == 0);
                if (fillDefined) fillValue = new Long(((long[])fill)[1]);
            }
            int type = var.getType();
            int n = var.getDataItemSize()/DataTypes.size[type];
            if (DataTypes.typeCategory[type] == DataTypes.LONG) {
                long[] lpad = new long[n];
                if (!fillDefined) {
                    for (int i = 0; i < n; i++) lpad[i] = Long.MIN_VALUE;
                } else {
                    for (int i = 0; i < n; i++) {
                        lpad[i] = fillValue.longValue();
                    }
                }
                return lpad;
            } else {
                double[] dpad = new double[n];
                if (!fillDefined) {
                    for (int i = 0; i < n; i++) {
                        dpad[i] = Double.NEGATIVE_INFINITY;
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        dpad[i] = fillValue.doubleValue();
                    }
                }
                return dpad;
            }
        } else {
            return o;
        }
    }

    public static Object getFillValue(CDFImpl thisCDF, Variable var) {
        Vector fill = (Vector)thisCDF.getAttribute(var.getName(), "FILLVAL");
        int type = var.getType();
        if (fill.size() != 0) {
             if (fill.get(0).getClass().getComponentType() == Double.TYPE) {
                 double dfill = ((double[])fill.get(0))[0];
                 if (DataTypes.typeCategory[type] == DataTypes.LONG) {
                     return new long[] {0l, (long)dfill};
                 } else {
                     return new double[] {0, dfill};
                 }
            } else {
                 long lfill = ((long[])fill.get(0))[0];
                 if (DataTypes.typeCategory[type] == DataTypes.LONG) {
                     return new long[] {0l, lfill};
                 } else {
                     return new double[] {0, (double)lfill};
                 }
            }
        } else {
            if (DataTypes.typeCategory[type] == DataTypes.LONG) {
                return new long[] {Long.MIN_VALUE, 0l};
            } else {
                return new double[] {Double.NEGATIVE_INFINITY, 0};
            }
        }
    }

    public static double [][] getSeries1(CDFImpl thisCDF, Variable var) throws
        IllegalAccessException, InvocationTargetException, Throwable {
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        int elements = (((Integer)elementCount(var).elementAt(0))).intValue();
        double [][] data = new double[numberOfValues][elements];

        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        double[] padValue = (double[])getPadValue(thisCDF, var);
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int offset = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            while (offset < first) {
                for (int m = 0; m < elements; m++) {
                    data[offset][m] = padValue[m];
                }
                offset++;
            }
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                for (; offset <= last; offset++) {
                    for (int m = 0; m < elements; m++) {
                        data[offset][m] = bvf.get();
                    }
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                for (; offset <= last; offset++) {
                    for (int m = 0; m < elements; m++) {
                        data[offset][m] = bvd.get();
                    }
                }
                break;
            case 2:
                doSignedInteger(bv, type, first, last, elements, data);
                offset += (last - first + 1);
                break;
            case 3:
                doUnsignedInteger(bv, type, first, last, elements, data);
                offset += (last - first + 1);
                break;
            }

        }
        return data;
    }
    // for a range of points of one dimensional variable of count elements
    // start at the current buffer position;
    // on return, buffer position is advanced by the data read
    static void doSignedInteger(ByteBuffer bv, int type, int first, 
        int last, int count, double[][] data) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < count; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                data[n][e] = num.doubleValue();
            }
        }
    }

    // for a range of points of scalar variable
    // on return, buffer position is advanced by the data read
    static void doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data) throws
        IllegalAccessException, InvocationTargetException {
        int index = first;
        doSignedInteger(bv, pos, type, size, first, last, data, index);
/*
        Method method = DataTypes.method[type];
        bv.position(pos);
        for (int n = first; n <= last; n++) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            data[n] = num.doubleValue();
            pos += size;
        }
*/
    }

    // 
    static int doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int index) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        bv.position(pos);
        for (int n = first; n <= last; n++) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            data[index++] = num.doubleValue();
            pos += size;
        }
        return index;
    }

    static void doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, int[] offsets, double[][] data) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        bv.position(pos);
        int ne = offsets.length;
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < ne; e++) {
                bv.position(pos + offsets[e]);
                Number num = (Number)method.invoke(bv, new Object[] {});
                data[n][e] = num.doubleValue();
            }
            pos += size;
        }
    }

    static int doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, int[] offsets, double[][] data,
        int index) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        bv.position(pos);
        int ne = offsets.length;
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < ne; e++) {
                bv.position(pos + offsets[e]);
                Number num = (Number)method.invoke(bv, new Object[] {});
                data[index][e] = num.doubleValue();
            }
            pos += size;
            index++;
        }
        return index;
    }

    static void doUnsignedInteger(ByteBuffer bv, int type, int first, 
        int last, int count, double[][] data) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < count; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                data[n][e] = (x >= 0)?(double)x:(double)(longInt + x);
            }
        }
    }

    static void doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        bv.position(pos);
        for (int n = first; n <= last; n++) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            int x = num.intValue();
            data[n] = (x >= 0)?(double)x:(double)(longInt + x);
            pos += size;
        }
    }

    static int doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int index) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        bv.position(pos);
        for (int n = first; n <= last; n++) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            int x = num.intValue();
            data[index++] = (x >= 0)?(double)x:(double)(longInt + x);
            pos += size;
        }
        return index;
    }

    static void doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, int[] offsets, double[][] data) throws
        IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        bv.position(pos);
        int ne = offsets.length;
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < ne; e++) {
                bv.position(pos + offsets[e]);
                Number num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                data[n][e] = (x >= 0)?(double)x:(double)(longInt + x);
            }
            pos += size;
        }
    }

    static int doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, int[] offsets, double[][] data,
        int index) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        bv.position(pos);
        int ne = offsets.length;
        for (int n = first; n <= last; n++) {
            for (int e = 0; e < ne; e++) {
                bv.position(pos + offsets[e]);
                Number num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                data[index][e] = (x >= 0)?(double)x:(double)(longInt + x);
            }
            pos += size;
            index++;
        }
        return index;
    }

    public static Object getElement1(CDFImpl thisCDF, Variable var, Integer idx)
        throws Throwable {
        if (var.isMissingRecords()) {
             return thisCDF.get(var.getName(), idx.intValue());
        }
        int element = idx.intValue();
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        if (!validElement(var, new int[] {element})) return null;
        int size = var.getDataItemSize();

        int type = var.getType();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        double[] padValue = null;
        long[] longPadValue = null;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numberOfValues];
            longType = true;
            longPadValue = (long[])getPadValue(thisCDF, var);
        } else {
            data = new double[numberOfValues];
            padValue = (double[])getPadValue(thisCDF, var);
        }
        int loff = element*DataTypes.size[type];
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int offset = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            if (!longType) {
                while (offset < first) data[offset++] = padValue[element];
            } else {
                while (offset < first) ldata[offset++] = longPadValue[element];
            }
            Method method;
            int pos = bv.position() + loff;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                while (offset <= last) {
                    data[offset++] = bv.getFloat(pos);
                    pos += size;
                }
                break;
            case 1:
                while (offset <= last) {
                    data[offset++] = bv.getDouble(pos);
                    pos += size;
                }
                break;
            case 2:
                doSignedInteger(bv, pos, type, size, first, last, data);
                offset += (last - first + 1);
                break;
            case 3:
                doUnsignedInteger(bv, pos, type, size, first, last, data);
                offset += (last - first + 1);
                break;
            case 5:
                while (offset <= last) {
                    ldata[offset++] = bv.getLong(pos);
                    pos += size;
                }
                break;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
            if (offset > numberOfValues) break;
            if (blk == (locations.size() - 1)) {
                if (!longType) {
                    while (offset < numberOfValues) {
                        data[offset++] = padValue[element];
                    }
                } else {
                    while (offset < numberOfValues) {
                        ldata[offset++] = longPadValue[element];
                    }
                }
            }
        }
        if (longType) return ldata;
        return data;
    }

    public static Object getElements1(CDFImpl thisCDF, Variable var,
        int[] idx) throws Throwable {
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) {
            numberOfValues = 1;
        }
        if (!validElement(var, idx)) return null;
        int ne = idx.length;
        int size = var.getDataItemSize();

        int type = var.getType();
        int[] loff = new int[ne];
        for (int i = 0; i < ne; i++) {
            loff[i] = idx[i]*DataTypes.size[type];
        }
        long[][] ldata = null;
        double[][] data = null;
        boolean longType = false;
        double[] padValue = null;
        long[] longPadValue = null;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numberOfValues][ne];
            longType = true;
            longPadValue = (long[])getPadValue(thisCDF, var);
        } else {
            data = new double[numberOfValues][ne];
            padValue = (double[])getPadValue(thisCDF, var);
        }
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int offset = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            if (!longType) {
                while (offset < first) {
                    for (int e = 0; e < ne; e++) {
                        data[offset][e] = padValue[idx[e]];
                    }
                    offset++;
                }
            } else {
                while (offset < first) {
                    for (int e = 0; e < ne; e++) {
                        ldata[offset][e] = longPadValue[idx[e]];
                    }
                    offset++;
                }
            }
            Method method;
            int pos = bv.position();
            switch (DataTypes.typeCategory[type]) {
            case 0:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        data[n][e] = bv.getFloat(pos + loff[e]);
                    }
                    pos += size;
                }
                break;
            case 1:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        data[n][e] = bv.getDouble(pos + loff[e]);
                    }
                    pos += size;
                }
                break;
            case 2:
                doSignedInteger(bv, pos, type, size, first, last, loff, data);
                break;
            case 3:
                doUnsignedInteger(bv, pos, type, size, first, last, loff, data);
                break;
            case 5:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        ldata[n][e] = bv.getLong(pos + loff[e]);
                    }
                    pos += size;
                }
                break;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
            offset += (last - first + 1);
        }
        if (longType) return ldata;
        return data;
    }

    public static double [][][] getSeries2(CDFImpl thisCDF, Variable var) throws 
        Throwable {
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        int n0 = (((Integer)elementCount(var).elementAt(0))).intValue();
        int n1 = (((Integer)elementCount(var).elementAt(1))).intValue();
        double [][][] data = new double[numberOfValues][n0][n1];
        double[] padValue = (double[])getPadValue(thisCDF, var);
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int offset = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            if (var.rowMajority()) {
                while (offset < first) {
                    for (int m = 0; m < n0; m++) {
                        for (int l = 0; l < n1; l++) {
                            data[offset][m][l] = padValue[m*n0 + l];
                        }
                    }
                    offset++;
                }
            } else {
                while (offset < first) {
                    for (int m = 0; m < n1; m++) {
                        for (int l = 0; l < n0; l++) {
                            data[offset][l][m] = padValue[l*n0 + m];
                        }
                    }
                    offset++;
                }
            }
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                if (var.rowMajority()) {
                    while (offset <= last) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                data[offset][m][l] = bvf.get();
                            }
                        }
                        offset++;
                    }
                } else {
                    while (offset <= last) {
                        for (int m = 0; m < n1; m++) {
                            for (int l = 0; l < n0; l++) {
                                data[offset][l][m] = bvf.get();
                            }
                        }
                        offset++;
                    }
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                if (var.rowMajority()) {
                    while (offset <= last) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                data[offset][m][l] = bvd.get();
                            }
                        }
                        offset++;
                    }
                } else {
                    while (offset <= last) {
                        for (int m = 0; m < n1; m++) {
                            for (int l = 0; l < n0; l++) {
                                data[offset][l][m] = bvd.get();
                            }
                        }
                        offset++;
                    }
                }
                break;
            case 2:
                if ((type == 1) || (type == 41)) {
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    data[offset][m][l] = bv.get();
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    data[offset][l][m] = bv.get();
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
                if (type == 2) {
                    ShortBuffer bvs = bv.asShortBuffer();
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    data[offset][m][l] = bvs.get();
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    data[offset][l][m] = bvs.get();
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
                if (type == 4) {
                    IntBuffer bvi = bv.asIntBuffer();
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    data[offset][m][l] = bvi.get();
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    data[offset][l][m] = bvi.get();
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
            case 3:
                if (type == 11) {
                    int _num = (1 << 8);
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    int x = bv.get();
                                    data[offset][m][l] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    int x = bv.get();
                                    data[offset][l][m] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
                if (type == 12) {
                    int _num = (1 << 16);
                    ShortBuffer bvs = bv.asShortBuffer();
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    int x = bvs.get();
                                    data[offset][m][l] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    int x = bvs.get();
                                    data[offset][l][m] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
                if (type == 14) {
                    long _num = ((long)1 << 32);
                    IntBuffer bvi = bv.asIntBuffer();
                    if (var.rowMajority()) {
                        while (offset <= last) {
                            for (int m = 0; m < n0; m++) {
                                for (int l = 0; l < n1; l++) {
                                    int x = bvi.get();
                                    data[offset][m][l] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    } else {
                        while (offset <= last) {
                            for (int m = 0; m < n1; m++) {
                                for (int l = 0; l < n0; l++) {
                                    int x = bvi.get();
                                    data[offset][l][m] =
                                        (x >= 0)?(double)x:(double)(_num + x);
                                }
                            }
                            offset++;
                        }
                    }
                    break;
                }
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
        }
        return data;
    }

    public static Object getPoint0(CDFImpl thisCDF,Variable var, Integer pt) 
        throws Throwable {
        if (var.isMissingRecords()) {
             return thisCDF.getPoint(var.getName(), pt.intValue());
        }
        int point = pt.intValue();
        int type = var.getType();
        int itemSize = var.getDataItemSize();
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            if (loc[1] < point) continue;
            if (loc[0] > point) return null;
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position() + (point - (int)loc[0])*itemSize;
            Method method;
            Number num;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                return new Double((double)bv.getFloat(pos));
            case 1:
                return new Double(bv.getDouble(pos));
            case 2:
                method = DataTypes.method[type];
                num = (Number)method.invoke(bv, new Object[] {});
                return new Double(num.doubleValue());
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                double d = (x >= 0)?(double)x:(double)(longInt + x);
                return new Double(d);
            case 5:
                return new Long(bv.getLong(pos));
            }
        }
        return null;
    }

    public static double[] getPoint1(CDFImpl thisCDF,Variable var, Integer pt) 
        throws Throwable {
        int point = pt.intValue();
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        int itemSize = var.getDataItemSize();
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            if (loc[1] < point) continue;
            if (loc[0] > point) return null;
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position() + (point - (int)loc[0])*itemSize;
            bv.position(pos);
            int n = (((Integer)elementCount(var).elementAt(0))).intValue();
            double [] da = new double[n];
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                for (int i = 0; i < n; i++) {
                    da[i] = bvf.get();
                }
                return da;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                for (int i = 0; i < n; i++) {
                    da[i] = bvd.get();
                }
                return da;
            case 2:
                method = DataTypes.method[type];
                for (int i = 0; i < n; i++) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    da[i] = num.doubleValue();
                }
                return da;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                for (int i = 0; i < n; i++) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    da[i] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                return da;
            }
        }
        return null;
    }

    public static double[][] getPoint2(CDFImpl thisCDF, Variable var,
        Integer pt) throws Throwable {
        int point = pt.intValue();
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        int itemSize = var.getDataItemSize();
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            if (loc[1] < point) continue;
            if (loc[0] > point) return null;
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position() + (point - (int)loc[0])*itemSize;
            bv.position(pos);
            int n0 = (((Integer)elementCount(var).elementAt(0))).intValue();
            int n1 = (((Integer)elementCount(var).elementAt(1))).intValue();
            double [][] da = new double[n0][n1];
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            da[i][j] = bvf.get();
                        }
                    }
                } else {
                    for (int i = 0; i < n1; i++) {
                        for (int j = 0; j < n0; j++) {
                            da[j][i] = bvf.get();
                        }
                    }
                }
                return da;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            da[i][j] = bvd.get();
                        }
                    }
                } else {
                    for (int i = 0; i < n1; i++) {
                        for (int j = 0; j < n0; j++) {
                            da[j][i] = bvd.get();
                        }
                    }
                }
                return da;
            case 2:
                method = DataTypes.method[type];
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            Number num = 
                                (Number)method.invoke(bv, new Object[] {});
                            da[i][j] = num.doubleValue();
                        }
                    }
                } else {
                    for (int i = 0; i < n1; i++) {
                        for (int j = 0; j < n0; j++) {
                            Number num = 
                                (Number)method.invoke(bv, new Object[] {});
                            da[j][i] = num.doubleValue();
                        }
                    }
                }
                return da;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            Number num = 
                                (Number)method.invoke(bv, new Object[] {});
                            int x = num.intValue();
                            double d = (x >= 0)?(double)x:(double)(longInt + x);
                            da[i][j] = d;
                        }
                    }
                } else {
                    for (int i = 0; i < n1; i++) {
                        for (int j = 0; j < n0; j++) {
                            Number num = 
                                (Number)method.invoke(bv, new Object[] {});
                            int x = num.intValue();
                            double d = (x >= 0)?(double)x:(double)(longInt + x);
                            da[j][i] = d;
                        }
                    }
                }
                return da;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
        }
        return null;
    }

    public static double[] getElement2(CDFImpl thisCDF,Variable var,
        Integer pt1, Integer pt2) throws Throwable {
        throw new Throwable("getElement2 is not supported currently");
        //return null;
    }

    public static Object getRange0(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend) throws Throwable {
        int start = istart.intValue();
        int end = iend.intValue();
        if (var.isMissingRecords()) {
             return thisCDF.getRange(var.getName(), start, end);
        }
        int numberOfValues = var.getNumberOfValues();
        int itemSize = var.getDataItemSize();
        int type = var.getType();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        double[] padValue = null;
        long[] longPadValue = null;
        Object _data = null;
        Object _pad = getPadValue(thisCDF, var);
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[end - start + 1];
            _data = ldata;
            longType = true;
            longPadValue = (long[])getPadValue(thisCDF, var);
        } else {
            data = new double[end - start + 1];
            _data = data;
            padValue = (double[])getPadValue(thisCDF, var);
        }
        int [] blks = null;
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        if (locations == null) {
            fillWithPad(longType, _data, start, end, _pad);
            return _data;
        } else {
            blks =  getBlockRange(locations, var.recordVariance(), start, end);
            if (blks == null) { // no overlap 
                if (!(var.missingRecordValueIsPad() ||
                    var.missingRecordValueIsPrevious())) {
                    return null;
                } else {
                    if (var.missingRecordValueIsPad()) {
                        fillWithPad(longType, _data, start, end, _pad);
                    }
                    if (var.missingRecordValueIsPrevious()) {
                        fillWithPrevious(var, longType, _data, start, end, _pad);
                    }
                    return _data;
                }
            }
        }
        boolean substitute = var.missingRecordValueIsPrevious();
/*
        if (locations != null) {
            blks =  getBlockRange(locations, var.recordVariance(), start, end);
        }
        if (blks == null) { // no overlap 
            if (locations != null) { // there is some data
                int _last = ((int[])var.getRecordRange())[1];
                int n = 0;
                if (substitute) {
                    if (start > _last) { // after the last record
                        if (!longType) {
                            double lastValue =
                                var.asDoubleArray(new int[]{_last})[0];
                            for (int i = start; i <= end; i++) {
                                data[n++] = lastValue;
                            }
                            return data;
                        } else {
                            long lastValue =
                                var.asLongArray(new int[]{_last})[0];
                            for (int i = start; i <= end; i++) {
                                ldata[n++] = lastValue;
                            }
                            return ldata;
                        }
                    } else { // before the first record 
                    }
                }
            }
            if ((locations != null) && 
                !(var.missingRecordValueIsPad()
                  || var.missingRecordValueIsPrevious())) {
                return null;
            }
            // padding required
            int n = 0;
            if (longType) {
                for (int i = start; i <= end; i++) {
                    ldata[n++] = longPadValue[0];
                }
                return ldata;
            } else {
                for (int i = start; i <= end; i++) {
                    data[n++] = padValue[0];
                }
                return data;
            }
        }
*/
        int firstBlock = blks[0];
        int lastBlock = blks[1];
        int offset = 0;
        int last = -1;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                start, end);
            if (oa == null) { //
                long [] loc = (long [])locations.get(blk - 1);
                if (!longType) {
                    double lastValue = 
                        var.asDoubleArray(new int[]{(int)loc[1]})[0];
                    while (offset < data.length) {
                        data[offset++] = 
                            (substitute)?lastValue:padValue[0];
                    }
                } else {
                    long lastValue = var.asLongArray(new int[]{(int)loc[1]})[0];
                     while (offset < data.length) {
                         ldata[offset++] =
                            (substitute)?lastValue:longPadValue[0];
                    }
                }
                break;
            }
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            // fill if necessary
            substitute = var.missingRecordValueIsPrevious();
            if (blk == firstBlock) substitute = false;
            if (!substitute) {
                if (!longType) {
                    while (offset < (first - start)) {
                        data[offset++] = padValue[0];
                    }
                } else {
                    while (offset < (first - start)) {
                        ldata[offset++] = longPadValue[0];
                    }
                }
            } else {
                if (!longType) {
                    double lastValue = var.asDoubleArray(new int[]{last})[0];
                    while (offset < (first - start)) {
                        data[offset++] = lastValue;
                    }
                } else {
                    long lastValue = var.asLongArray(new int[]{last})[0];
                    while (offset < (first - start)) {
                        ldata[offset++] = lastValue;
                    }
                }
            }
            last = ((Integer)oa[2]).intValue();
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                while (offset <= (last - start)) data[offset++] = bvf.get();
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                while (offset <= (last - start)) data[offset++] = bvd.get();
                break;
            case 2:
                method = DataTypes.method[type];
                while (offset <= (last - start)) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[offset++] = num.doubleValue();
                }
                break;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                while (offset <= (last - start)) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    data[offset++] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                break;
            case 5:
                LongBuffer bvl = bv.asLongBuffer();
                while (offset <= (last - start)) ldata[offset++] = bvl.get();
                break;
            }
            if (offset > (end - start)) break;
            if (blk == lastBlock) {
                substitute = var.missingRecordValueIsPrevious();
                if (!longType) {
                    double lastValue = data[offset - 1];
                    while (offset <= end) {
                        data[offset++] = (substitute)?lastValue:padValue[0];
                    }
                } else {
                    long lastValue = ldata[offset - 1];
                    while (offset <= end) {
                        ldata[offset++] =
                            (substitute)?lastValue:longPadValue[0];
                    }
                }
                break;
            }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = start; i <= end; i++) {
                    data[i] = data[0];
                }
            } else {
                for (int i = start; i <= end; i++) {
                    ldata[i] = ldata[0];
                }
            }
        }
        if (longType) return ldata;
        return data;
    }

    /**
     * returns values for the specified one
     * dimensional variable for the specified range of records.
     * long type not supported in this context - use getRangeForElements1
     */
    public static double [][] getRange1(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend) throws Throwable {
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Long type not supported in this context");
        }
        int start = istart.intValue();
        int end = iend.intValue();
        int numberOfValues = var.getNumberOfValues();
        int itemSize = var.getDataItemSize();
        int elements = (((Integer)elementCount(var).elementAt(0))).intValue();
        double [][] data = new double[end - start + 1][elements];
        double [] padValue = (double[])getPadValue(thisCDF, var);

        int [] blks = null;
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        if (locations != null) {
             blks =  getBlockRange(locations, var.recordVariance(), start, end);
        }
        boolean substitute = var.missingRecordValueIsPrevious();
        if (blks == null) { // no overlap 
            if (locations != null) { // there is some data
                int _last = ((int[])var.getRecordRange())[1];
                int n = 0;
                if (substitute) {
                    if (start > _last) { // after the last record
                        double[] lastValue =
                                var.asDoubleArray(new int[]{_last});
                        for (int i = start; i <= end; i++) {
                            data[n++] = lastValue;
                        }
                        return data;
                    }
                }
            }
            if ((locations != null) && 
                !(var.missingRecordValueIsPad()
                  || var.missingRecordValueIsPrevious())) {
                return null;
            }
            // padding required
            int n = 0;
            for (int i = start; i <= end; i++) {
                data[n++] = padValue;
            }
            return data;
        }
        int firstBlock = blks[0];
        int lastBlock = blks[1];

        int offset = 0;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                start, end);
            if (oa == null) {
                if (substitute) {
                    int [] loc = (int [])locations.get(blk - 1);
                    double[] lastValue = var.asDoubleArray(new int[]{loc[1]});
                    while (offset < data.length) {
                        data[offset] = lastValue;
                        offset++;
                    }
                } else {
                    while (offset < data.length) {
                        data[offset] = padValue;
                        offset++;
                    }
                }
                break;
            }
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            int last = ((Integer)oa[2]).intValue();
            // fill if necessary
            while (offset < (first - start)) {
                for (int m = 0; m < elements; m++) {
                    data[offset][m] = padValue[m];
                }
                offset++;
            }
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                while (offset <= (last - start)) {
                    for (int m = 0; m < elements; m++) {
                        data[offset][m] = bvf.get();
                    }
                    offset++;
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                while (offset <= (last - start)) {
                    for (int m = 0; m < elements; m++) {
                        data[offset][m] = bvd.get();
                    }
                    offset++;
                }
                break;
            case 2:
                doSignedInteger(bv, type, first - start, last - start,
                    elements, data);
                offset += (last - first + 1);
                break;
            case 3:
                doUnsignedInteger(bv, type, first - start, last - start,
                    elements, data);
                offset += (last - first + 1);
                break;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
            if (offset > (end - start)) break;
            if (blk == lastBlock) {
                substitute = var.missingRecordValueIsPrevious();
                double[] lastValue = data[offset - 1];
                while (offset <= (end - start)) {
                    data[offset] = (substitute)?lastValue:padValue;
                    offset++;
                }
                break;
            }
        }
        if (!var.recordVariance()) {
            for (int i = start; i <= end; i++) {
                for (int m = 0; m < elements; m++) {
                    data[i - start][m] = data[0][m];
                }
            }
        }
        return data;
    }


    static Vector elementCount(Variable var) {
        int [] dimensions = var.getDimensions();
        Vector ecount = new Vector();
        for (int i = 0; i < dimensions.length; i++) {
                if (var.getVarys()[i]) ecount.add(new Integer(dimensions[i]));
        }
        return ecount;
    }
    /** good for rank 1
     */
    public static boolean validElement(Variable var, int[] idx) {
        int elements = (((Integer)elementCount(var).elementAt(0))).intValue();
        for (int i = 0; i < idx.length; i++) {
            if ((idx[i] >= 0) && (idx[i] < elements)) continue;
            return false;
        } 
        return true;
    }

    /**
     * returns range of values for the specified element of a one
     * dimensional variable.
     * returns null if the specified element is not valid.
     */
    public static Object getRangeForElement1(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend, Integer ielement) throws Throwable {
        int element = ielement.intValue();
        if (!validElement(var, new int[] {element})) return null;
        int start = istart.intValue();
        int end = iend.intValue();
        if (var.isMissingRecords()) {
             return ((CDFImpl)thisCDF).getRange(var.getName(), start, end,
             element);
        }
        int numberOfValues = var.getNumberOfValues();
        int size = var.getDataItemSize();
        int type = var.getType();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        double[] padValue = null;
        long[] longPadValue = null;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[end - start + 1];
            longType = true;
            longPadValue = (long[])getPadValue(thisCDF, var);
        } else {
            data = new double[end - start + 1];
            padValue = (double[])getPadValue(thisCDF, var);
        }
        int loff = element*DataTypes.size[type];
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int [] blks =  
            getBlockRange(locations, var.recordVariance(), start, end);
        int firstBlock = blks[0];
        int lastBlock = blks[1];
        int offset = 0;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                start, end);
            if (oa == null) {
                if (!longType) {
                    while (offset < data.length) {
                        data[offset++] = padValue[element];
                    }
                } else {
                    while (offset < ldata.length) {
                        ldata[offset++] = longPadValue[element];
                    }
                }
                break;
            }
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            if (!longType) {
                while (offset < (first - start)) {
                    data[offset++] = padValue[element];
                }
            } else {
                while (offset < (first - start)) {
                        ldata[offset++] = longPadValue[element];
                }
            }
            int last = ((Integer)oa[2]).intValue();
            int pos = bv.position() + loff;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                while (offset <= (last - start)) {
                    data[offset++] = bv.getFloat(pos);
                    pos += size;
                }
                break;
            case 1:
                while (offset <= (last - start)) {
                    data[offset++] = bv.getDouble(pos);
                    pos += size;
                }
                break;
            case 2:
                doSignedInteger(bv, pos, type, size, first - start,
                    last - start, data);
                offset += (last - first + 1);
                break;
            case 3:
                doUnsignedInteger(bv, pos, type, size, first - start,
                    last - start, data);
                offset += (last - first + 1);
                break;
            case 5:
                while (offset <= (last - start)) {
                    ldata[offset++] = bv.getLong(pos);
                    pos += size;
                }
                break;
            }
            if (offset > (end - start)) break;
            if (blk == lastBlock) {
                if (!longType) {
                    while (offset < data.length) {
                        data[offset++] = padValue[element];
                    }
                } else {
                    while (offset < ldata.length) {
                        ldata[offset++] = longPadValue[element];
                    }
                }
                break;
             }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = start; i <= end; i++) {
                    data[i - start] = data[0];
                }
            } else {
                for (int i = start; i <= end; i++) {
                    ldata[i - start] = ldata[0];
                }
            }
        }
        if (longType) return ldata;
        return data;
    }

    /**
     * returns range of values for the specified elements of a one
     * dimensional variable.
     * returns null if any of the specified elements is not valid.
     * -- does not respect 'previous' and cases where the requested
     * range has partial overlap with the available range
     */
    public static Object getRangeForElements1(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend, int[] idx) throws Throwable {
        if (!validElement(var, idx)) return null;
        int start = istart.intValue();
        int end = iend.intValue();
        int numberOfValues = var.getNumberOfValues();
        int size = var.getDataItemSize();
        int ne = idx.length;

        int type = var.getType();
        long[][] ldata = null;
        double[][] data = null;
        boolean longType = false;
        double[] padValue = null;
        long[] longPadValue = null;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[end - start + 1][ne];
            longType = true;
            longPadValue = (long[])getPadValue(thisCDF, var);
        } else {
            data = new double[end - start + 1][ne];
            padValue = (double[])getPadValue(thisCDF, var);
        }
        int[] loff = new int[ne];
        for (int i = 0; i < ne; i++) {
            loff[i] = idx[i]*DataTypes.size[type];
        }
        // loff contains offsets from the beginning of the item
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int [] blks =  
            getBlockRange(locations, var.recordVariance(), start, end);
        int firstBlock = blks[0];
        int lastBlock = blks[1];

        int offset = 0;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                start, end);
            if (oa == null) {
                if (!longType) {
                    while (offset < data.length) {
                        for (int i = 0; i < ne; i++) {
                            data[offset][i] = padValue[idx[i]];
                        }
                        offset++;
                    }
                } else {
                    while (offset < ldata.length) {
                        for (int i = 0; i < ne; i++) {
                            ldata[offset][i] = longPadValue[idx[i]];
                        }
                        offset++;
                    }
                }
                break;
            }
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            int last = ((Integer)oa[2]).intValue();
            int pos = bv.position();
            switch (DataTypes.typeCategory[type]) {
            case 0:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        data[offset][e] = bv.getFloat(pos + loff[e]);
                    }
                    pos += size;
                    offset++;
                }
                break;
            case 1:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        data[offset][e] = bv.getDouble(pos + loff[e]);
                    }
                    pos += size;
                    offset++;
                }
                break;
            case 2:
                offset = doSignedInteger(bv, pos, type, size, first, last,
                    loff,  data, offset);
                break;
            case 3:
                offset = doUnsignedInteger(bv, pos, type, size, first, last,
                    loff,  data, offset);
                break;
            case 5:
                for (int n = first; n <= last; n++) {
                    for (int e = 0; e < ne; e++) {
                        ldata[offset][e] = bv.getLong(pos + loff[e]);
                    }
                    pos += size;
                    offset++;
                }
                break;
            }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = start; i <= end; i++) {
                    for (int e = 0; e < ne; e++) {
                        data[i - start][e] = data[0][e];
                    }
                }
            } else {
                for (int i = start; i <= end; i++) {
                    for (int e = 0; e < ne; e++) {
                        ldata[i - start][e] = ldata[0][e];
                    }
                }
            }
        }
        if (longType) return ldata;
        return data;
    }
/*
    public static double [][][] getRange2(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend) throws Throwable {
        throw new Throwable("getRange2 is not supported currently");
        //return null;
    }
*/

    /**
     * returns String of length 'size' starting at current position
     * in the given ByteBuffer. On return, the buffer position is 1
     * advanced by the smaller of size, or the length of the null
     * terminated string
     */
    public static String getStringValue(ByteBuffer bv, int size) {
        byte [] ba = new byte[size];
        int i = 0;
        for (; i < size; i++) {
            ba[i] = bv.get();
            if (ba[i] == 0) break;
        }
        return new String(ba, 0, i);
    }

    /**
      * 0D series of string
      */
    public static String [] getStringSeries0(CDFImpl thisCDF, Variable var) {
        int numberOfValues = var.getNumberOfValues();
        String [] data = new String[numberOfValues];
        int len = var.getNumberOfElements();
        Vector locations = 
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position();
            for (int n = (int)loc[0]; n <= (int)loc[1]; n++) {
                data[n] = getStringValue(bv, len);
                pos += len;
                bv.position(pos);
            }
        }
        if (!var.recordVariance()) {
            for (int i = 1; i < numberOfValues; i++) {
                data[i] = data[0];
            }
        }
        return data;
    }
    /**
      * 1D series of string
      */
    public static String [][] getStringSeries1(CDFImpl thisCDF, Variable var) {
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        int elements = (((Integer)elementCount(var).elementAt(0))).intValue();
        String [][] data = new String[numberOfValues][elements];
        int size = var.getDataItemSize();
        int len = var.getNumberOfElements();
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position();
            for (int n = (int)loc[0]; n <= (int)loc[1]; n++) {
                for (int m = 0; m < elements; m++) {
                    data[n][m] = getStringValue(bv, len);
                    pos += len;
                    bv.position(pos);
                }
            }
        }
        return data;
    }

    public static String [][][] getStringSeries2(CDFImpl thisCDF, Variable var)
        {
        return null;
    }

    /**
     * returns range of blocks containing the range of records (start, end).
     */
    public static int [] getBlockRange(Vector locations, boolean recordVariance,
        int start, int end) {

        int firstBlock;
        int lastBlock;
        if (recordVariance) {
            if (end < ((long [])locations.get(0))[0]) return null;
            lastBlock = locations.size() - 1;
            if (start > ((long [])locations.get(lastBlock))[1]) return null;
            firstBlock = -1;
            int blk = 0;
            for (; blk < locations.size(); blk++) {
                long [] loc = (long [])locations.get(blk);
                if (start > loc[1]) continue;
                firstBlock = blk;
                break;
            }
            if (firstBlock < 0) return null;
            blk = firstBlock;
            for (; blk < locations.size(); blk++) {
                long [] loc = (long [])locations.get(blk);
                lastBlock = blk;
                if (end <= loc[1]) break;
                if (blk < (locations.size() - 1)) {
                    if (end < ((long [])locations.get(blk+1))[0]) break;
                }
            }
        } else {
            firstBlock = 0;
            lastBlock = 0;
        }
        return new int[] {firstBlock, lastBlock};
    }

    /**
     * returns ByteBuffer containing count values for variable var starting at
     * CDF offset value offset.
     */
    static ByteBuffer positionBuffer(CDFImpl impl, Variable var, long offset,
        int count) {
        ByteBuffer bv;
        if (!var.isCompressed()) {
            bv = impl.getValueBuffer(offset);
        } else {
            int size = var.getDataItemSize();
            bv = impl.getValueBuffer(offset, size , count);
        }
        bv.order(impl.getByteOrder());
        return bv;
    }

    /**
     * returns ByteBuffer, index of first entry and index of last entry for
     * the specified block of data corresponding to variable 'var' for the
     * range of records (start, end).
     */
    static Object[] positionBuffer(CDFImpl impl, Variable var, int[] blockRange,
        int blk, int start, int end) {
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        long [] loc = (long [])locations.elementAt(blk);
        int first = (int)loc[0];
        int last = (int)loc[1];
        ByteBuffer bv = positionBuffer(impl, var, loc[2], (last - first + 1));
        if (var.recordVariance()) {
            if (blk == blockRange[0]) {// position to first needed
                int size = var.getDataItemSize();
                if (start > first) {
                    bv.position(bv.position() + size*(start - first));
                    first = start;
                } else {
                    if (end < first) return null;
                }
            }
            if (blk == blockRange[1]) {
                if (last > end) last = end;
            }
        }
        return new Object[] {bv, new Integer(first), new Integer(last)};
    }
    /**
      * 3D Series
      */
    public static double [][][][] getSeries3(CDFImpl thisCDF, Variable var) 
        throws Throwable {
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        int n0 = (((Integer)elementCount(var).elementAt(0))).intValue();
        int n1 = (((Integer)elementCount(var).elementAt(1))).intValue();
        int n2 = (((Integer)elementCount(var).elementAt(2))).intValue();
        double [][][][] data = new double[numberOfValues][n0][n1][n2];
        double [] fill = (double[])getFillValue(thisCDF, var);
        double fillValue = (fill[0] != 0)?Double.NaN:fill[1];
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int next = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(last - first + 1));
            if (var.rowMajority()) {
                for (int n = next; n < first; n++) {
                    for (int m = 0; m < n0; m++) {
                        for (int l = 0; l < n1; l++) {
                            for (int k = 0; k < n2; k++) {
                                data[n][m][l][k] = fillValue;
                            }
                        }
                    }
                }
            } else {
                for (int n = next; n < first; n++) {
                    for (int m = 0; m < n2; m++) {
                        for (int l = 0; l < n1; l++) {
                            for (int k = 0; k < n0; k++) {
                                data[n][k][l][m] = fillValue;
                            }
                        }
                    }
                }
            }
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                if (var.rowMajority()) {
                    float[] temp = new float[n2];
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                bvf.get(temp);
                                for (int k = 0; k < n2; k++) {
                                    data[n][m][l][k] = temp[k];
                                }
                            }
                        }
                    }
                } else {
                    float[] temp = new float[n0];
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n2; m++) {
                            for (int l = 0; l < n1; l++) {
                                bvf.get(temp);
                                for (int k = 0; k < n0; k++) {
                                    data[n][k][l][m] = temp[k];
                                }
                            }
                        }
                    }
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                if (var.rowMajority()) {
                    double[] temp = new double[n2];
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                bvd.get(temp);
                                for (int k = 0; k < n2; k++) {
                                    data[n][m][l][k] = temp[k];
                                }
                            }
                        }
                    }
                } else {
                    double[] temp = new double[n0];
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n2; m++) {
                            for (int l = 0; l < n1; l++) {
                                bvd.get(temp);
                                for (int k = 0; k < n0; k++) {
                                    data[n][k][l][m] = temp[k];
                                }
                            }
                        }
                    }
                }
                break;
            case 2:
                Method method = DataTypes.method[type];
                if (var.rowMajority()) {
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                for (int k = 0; k < n2; k++) {
                                    Number num =
                                    (Number)method.invoke(bv, new Object[] {});
                                    data[n][m][l][k] = num.doubleValue();
                                }
                            }
                        }
                    }
                } else {
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n2; m++) {
                            for (int l = 0; l < n1; l++) {
                                for (int k = 0; k < n0; k++) {
                                    Number num =
                                    (Number)method.invoke(bv, new Object[] {});
                                    data[n][k][l][m] = num.doubleValue();
                                }
                            }
                        }
                    }
                }
                break;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                if (var.rowMajority()) {
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n0; m++) {
                            for (int l = 0; l < n1; l++) {
                                for (int k = 0; k < n2; k++) {
                                    Number num =
                                    (Number)method.invoke(bv, new Object[] {});
                                    int x = num.intValue();
                                    data[n][m][l][k] =
                                      (x >= 0)?(double)x:(double)(longInt + x);
                                }
                            }
                        }
                    }
                } else {
                    for (int n = first; n <= last; n++) {
                        for (int m = 0; m < n2; m++) {
                            for (int l = 0; l < n1; l++) {
                                for (int k = 0; k < n0; k++) {
                                    Number num =
                                    (Number)method.invoke(bv, new Object[] {});
                                    int x = num.intValue();
                                    data[n][k][l][m] =
                                      (x >= 0)?(double)x:(double)(longInt + x);
                                }
                            }
                        }
                    }
                }
                break;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
            next = last + 1;
        }
        return data;
    }
    /**
      * 3D Point
      */
    public static double[][][] getPoint3(CDFImpl thisCDF, Variable var,
        Integer pt) throws Throwable {
        int point = pt.intValue();
        int type = var.getType();
        int itemSize = var.getDataItemSize();
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            if (loc[1] < point) continue;
            if (loc[0] > point) return null;
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (int)(loc[1] - loc[0] + 1));
            int pos = bv.position() + (point - (int)loc[0])*itemSize;
            bv.position(pos);
            int n0 = (((Integer)elementCount(var).elementAt(0))).intValue();
            int n1 = (((Integer)elementCount(var).elementAt(1))).intValue();
            int n2 = (((Integer)elementCount(var).elementAt(2))).intValue();
            double [][][] da = new double[n0][n1][n2];
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n2; k++) {
                                da[i][j][k] = bvf.get();
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < n2; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n0; k++) {
                                da[k][j][i] = bvf.get();
                            }
                        }
                    }
                }
                return da;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n2; k++) {
                                da[i][j][k] = bvd.get();
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < n2; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n0; k++) {
                                da[k][j][i] = bvd.get();
                            }
                        }
                    }
                }
                return da;
            case 2:
                method = DataTypes.method[type];
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n2; k++) {
                                Number num = 
                                    (Number)method.invoke(bv, new Object[] {});
                                da[i][j][k] = num.doubleValue();
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < n2; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n0; k++) {
                                Number num = 
                                    (Number)method.invoke(bv, new Object[] {});
                                da[k][j][i] = num.doubleValue();
                            }
                        }
                    }
                }
                return da;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                if (var.rowMajority()) {
                    for (int i = 0; i < n0; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n2; k++) {
                                Number num = 
                                    (Number)method.invoke(bv, new Object[] {});
                                int x = num.intValue();
                                double d = (x >= 0)?(double)x:
                                    (double)(longInt + x);
                                da[i][j][k] = d;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < n2; i++) {
                        for (int j = 0; j < n1; j++) {
                            for (int k = 0; k < n0; k++) {
                                Number num = 
                                    (Number)method.invoke(bv, new Object[] {});
                                int x = num.intValue();
                                double d = (x >= 0)?(double)x:
                                    (double)(longInt + x);
                                da[k][j][i] = d;
                            }
                        }
                    }
                }
                return da;
            default:
                throw new Throwable(var.getName() + " has unsupported type " +
                "in this context.");
            }
        }
        return null;
    }

    /**
      * 1D 
      */
    public static double [] get1DSeries(CDFImpl thisCDF, Variable var, int[] pt)
        throws Throwable {
        return (double[])get1DSeries(thisCDF, var, pt, false);
    }
    static void do1D(ByteBuffer bv, int type, Object temp, Object result,
       int offset, int number, boolean swap, int[] edim) throws Throwable {
       do1D(bv, type, temp, result, offset, number, false, swap, edim);
    }

    static void do1D(ByteBuffer bv, int type, Object temp, Object result,
       int offset, int number, boolean preserve, boolean swap, int[] edim)
       throws Throwable {
       if (edim.length > 1) {
           if (swap) {
               do1DSwap(bv, type, temp, result, offset, number, preserve, edim);
               return;
           }
       }
        Method method;
        double[] data = null;
        if (DataTypes.typeCategory[type] != DataTypes.LONG) {
            data = (double[])result;
        }
        switch (DataTypes.typeCategory[type]) {
        case 0:
            float[] tf = new float[number];
            FloatBuffer bvf = bv.asFloatBuffer();
            bvf.get(tf, 0, number);
            for (int n = 0; n < number; n++) {
                data[offset + n] = tf[n];
            }
            break;
        case 1:
            DoubleBuffer bvd = bv.asDoubleBuffer();
            bvd.get(data, offset, number);
            break;
        case 2:
            method = DataTypes.method[type];
            for (int e = 0; e < number; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                data[offset + e] = num.doubleValue();
            }
            break;
        case 3:
            method = DataTypes.method[type];
            long longInt = DataTypes.longInt[type];
            for (int e = 0; e < number; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                data[offset + e] = (x >= 0)?(double)x:(double)(longInt + x);
            }
            break;
        case 5:
            LongBuffer bvl = bv.asLongBuffer();
            if (!preserve) {
                long[] tl = new long[number];
                data = (double[])result;
                bvl.get(tl, 0, number);
                for (int n = 0; n < number; n++) {
                    data[offset + n] = (double)tl[n];
                }
                break;
            }
            long[] ldata = (long[])result;
            bvl.get(ldata, offset, number);
        }
    }

    public static Object get1DSeries(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve) throws Throwable {
        return get1DSeries(thisCDF, var, pt, preserve, false);
    }

    public static Object get1DSeries(CDFImpl thisCDF, Variable var, int[] pt,
        boolean preserve, boolean swap) throws Throwable {
        
        int begin = 0;
        int numberOfValues = var.getNumberOfValues();
        int type = var.getType();
        boolean longType = (DataTypes.typeCategory[type] == DataTypes.LONG);
        if (numberOfValues == 0) {
            if (longType) return new long[0];
            if (!longType) return new double[0];
        }
        int[] edim = var.getEffectiveDimensions();
        int end = numberOfValues - 1;
        if (pt != null) {
            if (var.recordVariance()) {
                begin = pt[0];
                if (begin < 0) begin = 0;
                if (pt.length > 1) {
                    if (pt[1] < end) end = pt[1];
                    numberOfValues = end - begin + 1;
                } else {
                    end = -1;
                    numberOfValues = 1;
                }
            }
        }
        if (!var.recordVariance()) numberOfValues = 1;
        long[] ldata = null;
        double[] data = null;
        int itemSize = var.getDataItemSize();
        int elements = itemSize/DataTypes.size[type];
        
        double padValue = Double.NEGATIVE_INFINITY;
        long longPadValue = Long.MIN_VALUE;
        if (longType && preserve) {
            ldata = new long[numberOfValues*elements];
        } else {
            data = new double[numberOfValues*elements];
        }
        Object temp = null;

        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        ByteBuffer bv;
        int blk = 0;
        if (begin > 0) {// position to first needed block
            for (; blk < locations.size(); blk++) {
                long [] loc = (long [])locations.elementAt(blk);
                int first = (int)loc[0];
                int last = (int)loc[1];
                if (last >= begin) break;
                if (blk < (locations.size() - 1)) continue;
                // last block - begin is beyond this block
                // should we return null here?
                if (longType && preserve) {
                    do1DMissing(ldata, longPadValue);
                    return ldata;
                } else {
                    do1DMissing(data, padValue);
                    return data;
                }
            }
            if (blk == locations.size()) return null;
        }
        // there is valid data to send back
        // begin may lie before blk. This is handled later
        boolean firstBlock = true;
        int next = begin;
        int offset = 0;
        int[] _edim = Arrays.copyOf(edim, edim.length);
        if (swap) {
            if (!var.rowMajority()) {
                _edim = new int[edim.length];
                for (int i = 0; i < edim.length; i++) {
                    _edim[i] = edim[edim.length -1 -i]; 
                }           
            }
        }
        for (; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];

            int count = (last - first + 1);
            bv = positionBuffer((CDFImpl)thisCDF, var, loc[2], count);
            if (firstBlock) {
                if (pt != null) {
                    if (begin > first) {
                        int pos = bv.position() + (begin - first)*itemSize;
                        bv.position(pos);
                    }
                    if (end < 0) { // single point needed
                        boolean available = (begin >= first);
                        if (longType && preserve) {
                            if (available) {
/*
                                int[] _edim = edim;
                                if (swap) {
                                    if (!var.rowMajority()) {
                                        _edim = new int[edim.length];
                                        for (int i = 0; i < edim.length; i++) {
                                            _edim[i] = edim[edim.length -1 -i]; 
                                        }           
                                    }
                                }
*/
                                do1D(bv, type, temp, ldata, 0, elements,
                                preserve, swap, _edim);
                            } else {
                                do1DMissing(ldata, longPadValue, 0, elements);
                            }
                            return ldata;
                        } else {
                            if (available) {
/*
                                int[] _edim = edim;
                                if (swap) {
                                    if (!var.rowMajority()) {
                                        _edim = new int[edim.length];
                                        for (int i = 0; i < edim.length; i++) {
                                            _edim[i] = edim[edim.length -1 -i]; 
                                        }           
                                    }
                                }
*/
                                do1D(bv, type, temp, data, 0, elements, swap,
                                    _edim);
                            } else {
                                do1DMissing(data, padValue);
                            }
                            return data;
                        }
                    }            
                }
                firstBlock = false;
            }
            // pad if necessary
            if (next < first) { // next cannot exceed first
                int target = (end >= first)?first:end + 1 ;
                int stop = target*elements; // beginning of non pad
                int start = next*elements;
                int n = stop - start;
                if (longType && preserve) {
                    do1DMissing(ldata, longPadValue, offset, n);
                } else {
                    do1DMissing(data, padValue, offset, n);
                }
                offset += (stop - start);
                if (target > end) break;
                next = first;
            }
                 
            int term = (end <= last)?end:last;               
            count = (term - next + 1);
/*
            int[] _edim = edim;
            if (swap) {
                if (!var.rowMajority()) {
                    _edim = new int[edim.length];
                    for (int i = 0; i < edim.length; i++) {
                        _edim[i] = edim[edim.length -1 -i]; 
                    }           
                }
            }
*/
            if (longType && preserve) {
                do1D(bv, type, temp, ldata, offset, count*elements,
                    preserve, swap, _edim);
            } else {
                do1D(bv, type, temp, data, offset, count*elements, swap,
                    _edim);
            }
            next += count;
            offset += count*elements;
            if (end == term) break;
            if (blk == (locations.size() - 1)) {
            // last block may end prior to last record written
            // i.e. end may lie between last and numberOfValues - 1
                if (longType && preserve) {
                    do1DMissing(ldata, longPadValue, offset);
                } else {
                    do1DMissing(data, padValue, offset);
                }
                break;
            }
        }
        if (offset == 0) return null;
        if (longType && preserve) return ldata;
        return data;
    }

    public static void do1DMissing(long[] ldata, long padValue, int start,
       int count) {
       int offset = start;
       for (int i = 0; i < count; i++) {
            ldata[offset++] = padValue;
       }
    }

    public static void do1DMissing(long[] ldata, long padValue) {
        do1DMissing(ldata, padValue, 0, ldata.length);
    }

    public static void do1DMissing(long[] ldata, long padValue, int start) {
        do1DMissing(ldata, padValue, start, ldata.length - start);
    }

    public static void do1DMissing(double[] data, double padValue, int start,
       int count) {
       int offset = start;
       for (int i = 0; i < count; i++) {
            data[offset++] = padValue;
       }
    }

    public static void do1DMissing(double[] data, double padValue) {
        do1DMissing(data, padValue, 0, data.length);
    }

    public static void do1DMissing(double[] data, double padValue, int start) {
        do1DMissing(data, padValue, start, data.length - start);
    }

    public static double [] get1DSeries(CDFImpl thisCDF, Variable var, int[] pt,
        int[] stride) throws IllegalAccessException, InvocationTargetException,
        Throwable {
        double [] data;
        
        int end = -1;
        int begin = 0;
        int numberOfValues = 0;
        if (pt != null) {
            if (var.recordVariance()) {
                begin = pt[0];
                numberOfValues = 1;
                if (pt.length > 1) {
                    end = pt[1];
                    numberOfValues = end - begin + 1;
                }
            }
        } else {
            numberOfValues = var.getNumberOfValues();
        }
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        Stride strideObject = new Stride(stride);
        int _stride = strideObject.getStride(numberOfValues);
        if (_stride > 1) {
            int n = numberOfValues;
            numberOfValues = (numberOfValues/_stride);
            if ((n % _stride) != 0) numberOfValues++;
        }
        int type = var.getType();
        int itemSize = var.getDataItemSize();
        int elements = itemSize/DataTypes.size[type];
        data = new double[numberOfValues*elements];

        float[] tf = null;
        if (_stride == 1) {
            if (DataTypes.typeCategory[type] == 0) {
                tf = new float[numberOfValues*elements];
            }
        }

        int[] edim = var.getEffectiveDimensions();
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        ByteBuffer bv;
        int blk = 0;
        int offset = 0;
        if (pt == null) {
            begin = (int)(((long [])locations.elementAt(0))[0]);
            end = (int)
                (((long [])locations.elementAt(locations.size() - 1))[1]);
        }
        int index = 0;
        for (; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            if (last < begin) continue;
            int count = (last - first + 1);
            bv = positionBuffer((CDFImpl)thisCDF, var, loc[2], count);
            // position buffer at the first point desired
            // init is the index of the first point desired
            int pos = 0;
            int init;
            if (begin > first) {
                init = begin;
            } else {
                init = first;
                if (_stride > 1) {
                    int elapsed = first - begin;
                    if ((elapsed % _stride) != 0) {
                        init = first - (elapsed % _stride) + _stride;
                    }
                }
            }
            pos = bv.position() + (init - first)*itemSize;
            bv.position(pos);
            // compute number of points to be extracted and
            // allocate temporary storage if necessary
            if (end < 0) { // single point needed
                do1D(bv, type, tf, data, 0, elements, false, edim);
                offset += elements;
            } else {
                int term = (end <= last)?end:last;
                int rem = (term - init + 1);
                if (_stride == 1) {
                    count = rem;
                    do1D(bv, type, tf, data, offset, count*elements, false,
                    edim);
                } else {
                    count = rem/_stride;
                    if (count*_stride < rem) count++;
                    if (DataTypes.typeCategory[type] == 0) {
                        tf = new float[count*elements];
                    }
                    do1D(bv, type, tf, data, offset, count,
                        elements, _stride);
                }
                offset += count*elements;
            }
            if (end <= last) break;
        }
        if (offset == 0) return null;
        return data;
    }
    // bv is positioned at the first point desired
    static void do1D(ByteBuffer bv, int type, float[] tf, double[] data,
       int offset, int count, int elements, int _stride) throws
       IllegalAccessException, InvocationTargetException, Throwable {
        Method method;
        int span = _stride*elements;
        int pos = bv.position();
        switch (DataTypes.typeCategory[type]) {
        case 0:
            FloatBuffer bvf = bv.asFloatBuffer();
            for (int n = 0; n < count; n++) {
                bvf.position(n*span);
                bvf.get(tf, n*elements, elements);
            }
            for (int n = 0; n < count*elements; n++) {
                data[offset + n] = tf[n];
            }
            break;
        case 1:
            DoubleBuffer bvd = bv.asDoubleBuffer();
            for (int n = 0; n < count; n++) {
                bvd.position(n*span);
                bvd.get(data, offset, elements);
                offset += elements;
            }
            break;
        case 2:
            method = DataTypes.method[type];
            span *= DataTypes.size[type];
            for (int n = 0; n < count; n++) {
                bv.position(pos + n*span);
                for (int e = 0; e < elements; e++) {
                    Number num = 
                        (Number)method.invoke(bv, new Object[] {});
                    data[offset++] = num.doubleValue();
                }
            }
            break;
        case 3:
            method = DataTypes.method[type];
            span *= DataTypes.size[type];
            long longInt = DataTypes.longInt[type];
            for (int n = 0; n < count; n++) {
                bv.position(n*span);
                for (int e = 0; e < elements; e++) {
                    Number num = (Number)method.invoke(bv,
                        new Object[] {});
                    int x = num.intValue();
                    data[offset++] =
                        (x >= 0)?(double)x:(double)(longInt + x);
                }
            }
            break;
        default:
            throw new Throwable("Unsupported data type for this " +
                "context");
        }
    }

    // not supported when there are gaps 
    public static Object getSeries0(CDFImpl thisCDF, Variable var,
        Stride strideObject) throws IllegalAccessException,
        InvocationTargetException, Throwable {
        int type = var.getType();
        if (DataTypes.typeCategory[type] == 4) {
            throw new Throwable("Type " + type +
            " not supported in this context");
        }
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        int numpt;
        int _stride = strideObject.getStride(numberOfValues);
        int size = var.getDataItemSize();
        int advance = _stride*size;
        if (_stride == 1) {
            return getSeries0(thisCDF, var);
        } else {
            numpt = numberOfValues/_stride;
            if ((numpt*_stride) <  numberOfValues) numpt++;
        }
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numpt];
            longType = true;
        } else {
            data = new double[numpt];
        }
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int next = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            Method method;
            int n = first % _stride;
            if (n == 0) {
                n = first;
            } else {
                n = (first - n) + _stride;
            }
            int pos = (n - first); // unit is item size
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                for (; pos <= last; pos += _stride) {
                    data[next++] = bvf.get(pos);
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                for (; pos <= last; pos += _stride) {
                    data[next++] = bvd.get(pos);
                }
                break;
            case 2:
                method = DataTypes.method[type];
                for (; pos <= last; pos += _stride) {
                    bv.position(pos*size);
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[next++] = num.doubleValue();
                }
                break;
            case 3:
                method = DataTypes.method[type];    
                long longInt = DataTypes.longInt[type];
                for (; pos <= last; pos += _stride) {
                    bv.position(pos*size);
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    data[next++] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                break;
            case 5:
                LongBuffer bvl = bv.asLongBuffer();
                for (; pos <= last; pos += _stride) {
                    ldata[next++] = bvl.get(pos);
                }
                break;
            default:
                throw new Throwable("Unsupported data type for this " +
                    "context");
            }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = 1; i < numpt; i++) {
                    data[i] = data[0];
                }
            } else {
                for (int i = 1; i < numpt; i++) {
                    ldata[i] = ldata[0];
                }
            }
        }
        if (longType) return ldata;
        return data;
    }
    public static double [] getElement1(CDFImpl thisCDF, Variable var,
        Integer idx, Stride strideObject) throws Throwable {
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("Only scalar variables of type int8 " +
              "are supported at this time.");
        }
        int element = idx.intValue();
        int numberOfValues = var.getNumberOfValues();
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        if (!validElement(var, new int[] {element})) return null;
        int numpt = numberOfValues;
        int _stride = strideObject.getStride(numberOfValues);
        if (_stride != 1) {
            numpt = numberOfValues/_stride;
            if ((numpt*_stride) <  numberOfValues) numpt++;
        }
        double [] data = new double[numpt];
        int size = var.getDataItemSize();
        int advance = size*_stride;

        int loff = element*DataTypes.size[type];
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int point = 0;
        for (int blk = 0; blk < locations.size(); blk++) {
            long [] loc = (long [])locations.elementAt(blk);
            int first = (int)loc[0];
            int last = (int)loc[1];
            ByteBuffer bv = positionBuffer((CDFImpl)thisCDF, var, loc[2],
                (last - first + 1));
            Method method;
            int n = first % _stride;
            if (n == 0) {
                n = first;
            } else {
                n = (first - n) + _stride;
            }
            int pos = bv.position() + (n - first)*size + loff;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                while (n <= last) {
                    data[point++] = bv.getFloat(pos);
                    n += _stride;
                    pos += advance;
                }
                break;
            case 1:
                while (n <= last) {
                    data[point++] = bv.getDouble(pos);
                    n += _stride;
                    pos += advance;
                }
                break;
            case 2:
                int res = doSignedInteger(bv, pos, type, size, n, last, data,
                new int[]{_stride}, point);
                point = res;
                break;
            case 3:
                res = doUnsignedInteger(bv, pos, type, size, n, last, data,
                new int[]{_stride}, point);
                point = res;
                break;
            default:
                throw new Throwable("Unsupported data type for this " +
                    "context");
            }
        }
        return data;
    }
    static int doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int[] stride,
        int point) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        int index = point;
        bv.position(pos);
        int _stride = stride[0];
        int advance = _stride*size;
        int n = first;
        while (n <= last) {
            Number num = (Number)method.invoke(bv, new Object[] {});
            data[index++] = num.doubleValue();
            n += _stride;
            pos += advance;
            bv.position(pos);
        }
        return index;
    }
    static int doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int[] stride,
        int point) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        int index = point;
        bv.position(pos);
        int _stride = stride[0];
        int advance = _stride*size;
        int n = first;
        while (n <= last) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            int x = num.intValue();
            data[index++] = (x >= 0)?(double)x:(double)(longInt + x);
            n += _stride;
            pos += advance;
        }
        return index;
    }
    public static Object getRange0(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend, Stride strideObject) throws Throwable {
        int begin = istart.intValue();
        if (begin < 0) throw new Throwable("getRange0 start < 0");
        int end = iend.intValue();
        int numberOfValues = var.getNumberOfValues();
        if (end > numberOfValues) {
            throw new Throwable("getRange0 end > available " + numberOfValues);
        }
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) numberOfValues = 1;
        numberOfValues = end - begin + 1;
        int _stride = strideObject.getStride(numberOfValues);
        if (_stride > 1) {
            int numpt = numberOfValues/_stride;
            if ((numpt*_stride) <  numberOfValues) numpt++;
            numberOfValues = numpt;
        }
        int type = var.getType();
        int itemSize = var.getDataItemSize();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numberOfValues];
            longType = true;
        } else {
            data = new double[numberOfValues];
        }

        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int [] blks =
            getBlockRange(locations, var.recordVariance(), begin, end);
        int firstBlock = blks[0];
        int lastBlock = blks[1];
        int index = 0;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                begin, end);
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            int last = ((Integer)oa[2]).intValue() - first;
            int n;
            if (_stride > 1) {
                if (blk > firstBlock) {
                    int elapsed = first - begin;
                    if ((elapsed  % _stride) > 0) {
                        n = _stride - ((first - begin) % _stride);
                        int pos = bv.position() + n*itemSize;
                        bv.position(pos);
                        last -= n;
                    }
                }
            }
            n = 0;
            Method method;
            switch (DataTypes.typeCategory[type]) {
            case 0:
                FloatBuffer bvf = bv.asFloatBuffer();
                for (; n <= last; n += _stride) {
                    data[index++] = bvf.get(n);
                }
                break;
            case 1:
                DoubleBuffer bvd = bv.asDoubleBuffer();
                for (; n <= last; n += _stride) {
                    data[index++] = bvd.get();
                }
                break;
            case 2:
                method = DataTypes.method[type];
                for (; n <= last; n += _stride) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    data[index++] = num.doubleValue();
                }
                break;
            case 3:
                method = DataTypes.method[type];
                long longInt = DataTypes.longInt[type];
                for (; n <= last; n += _stride) {
                    Number num = (Number)method.invoke(bv, new Object[] {});
                    int x = num.intValue();
                    data[index++] = (x >= 0)?(double)x:(double)(longInt + x);
                }
                break;
            case 5:
                LongBuffer bvl = bv.asLongBuffer();
                for (; n <= last; n += _stride) {
                    ldata[index++] = bvl.get();
                }
                break;
            default:
                throw new Throwable("Unsupported data type for this " +
                    "context");
            }
        }
        if (!var.recordVariance()) {
            if (!longType) {
                for (int i = begin; i <= end; i += _stride) {
                    data[i - begin] = data[0];
                }
            } else {
                for (int i = begin; i <= end; i += _stride) {
                    ldata[i - begin] = ldata[0];
                }
            }
        }
        if (longType) return ldata;
        return data;
    }
    public static Object getRangeForElement1(CDFImpl thisCDF, Variable var,
        Integer istart, Integer iend, Integer ielement, Stride strideObject)
        throws Throwable {
        int element = ielement.intValue();
        if (!validElement(var, new int[] {element})) return null;
        int begin = istart.intValue();
        int end = iend.intValue();
        int numberOfValues = var.getNumberOfValues();
        if (end > numberOfValues) {
            throw new Throwable("getRange0 end > available " + numberOfValues);
        }
        if (numberOfValues == 0) return null;
        if (!var.recordVariance()) {
            numberOfValues = 1;
        } else {
            numberOfValues = end - begin + 1;
        }
        int _stride = strideObject.getStride(numberOfValues);
        if (_stride > 1) {
            int numpt = numberOfValues/_stride;
            if ((numpt*_stride) <  numberOfValues) numpt++;
            numberOfValues = numpt;
        }
        int type = var.getType();
        long[] ldata = null;
        double[] data = null;
        boolean longType = false;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            ldata = new long[numberOfValues];
            longType = true;
        } else {
            data = new double[numberOfValues];
        }
        int itemSize = var.getDataItemSize();
        int advance = itemSize*_stride;
        int loff = element*DataTypes.size[type];
        Vector locations =
            ((CDFImpl.DataLocator)var.getLocator()).getLocationsAsVector();
        int [] blks =
            getBlockRange(locations, var.recordVariance(), begin, end);
        int firstBlock = blks[0];
        int lastBlock = blks[1];
        int index = 0;
        for (int blk = firstBlock; blk <= lastBlock; blk++) {
            Object[] oa = positionBuffer((CDFImpl)thisCDF, var, blks, blk,
                begin, end);
            ByteBuffer bv = (ByteBuffer)oa[0];
            int first = ((Integer)oa[1]).intValue();
            int last = ((Integer)oa[2]).intValue();
            int pos = bv.position() + loff;
            int n = first;
            if (_stride > 1) {
                if (blk > firstBlock) {
                    int elapsed = first - begin;
                    if ((elapsed % _stride) != 0) {
                        n = first + _stride - (elapsed % _stride);
                        pos += (n - first)*itemSize;
                    }
                }
            }
            switch (DataTypes.typeCategory[type]) {
            case 0:
                for (; n <= last; n += _stride) {
                    data[index++] = bv.getFloat(pos);
                    pos += advance;
                }
                break;
            case 1:
                for (; n <= last; n += _stride) {
                    data[index++] = bv.getDouble(pos);
                    pos += advance;
                }
                break;
            case 2:
                index = doSignedInteger(bv, pos, type, itemSize, n, last,
                     data, index, new int[]{_stride});
                break;
            case 3:
                index = doUnsignedInteger(bv, pos, type, itemSize, n, last,
                     data, index, new int[]{_stride});
                break;
            case 5:
                for (; n <= last; n += _stride) {
                    ldata[index++] = bv.getLong(pos);
                    pos += advance;
                }
                break;
            default:
                throw new Throwable("Unsupported data type for this " +
                    "context");
            }
        }
        if (!var.recordVariance()) {
            int i = begin;
            int n = 0;
            if (!longType) {
                while (i <= end) {
                    data[n++] = data[0];
                    i += _stride;
                }
            } else {
                while (i <= end) {
                    ldata[n++] = ldata[0];
                    i += _stride;
                }
            }
        }
        if (longType) return ldata;
        return data;
    }
    static int doSignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int index,
        int[] stride) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        bv.position(pos);
        for (int n = first; n <= last; n += stride[0]) {
            Number num = (Number)method.invoke(bv, new Object[] {});
            data[index++] = num.doubleValue();
            pos += size;
            bv.position(pos);
        }
        return index;
    }
    static int doUnsignedInteger(ByteBuffer bv, int pos, int type,
        int size, int first, int last, double[] data, int index,
        int[] stride) throws IllegalAccessException, InvocationTargetException {
        Method method = DataTypes.method[type];
        long longInt = DataTypes.longInt[type];
        bv.position(pos);
        for (int n = first; n <= last; n += stride[0]) {
            bv.position(pos);
            Number num = (Number)method.invoke(bv, new Object[] {});
            int x = num.intValue();
            data[index++] = (x >= 0)?(double)x:(double)(longInt + x);
            pos += size;
        }
        return index;
    }
    static void fillWithPad(boolean longType, Object _data, int start, int end,
        Object _pad) throws Throwable {
        int n = 0;
        if (longType) {
            long lpad = ((long[])_pad)[0];
            long[] ldata = (long[])_data;
            for (int i = start; i <= end; i++) {
                ldata[n++] = lpad;
            }
        } else {
            double dpad = ((double[])_pad)[0];
            double[] ddata = (double[])_data;
            for (int i = start; i <= end; i++) {
                ddata[n++] = dpad;
            }
        }
    }

    static void fillWithPrevious(Variable var, boolean longType, Object _data,
        int start, int end, Object _pad) throws Throwable {
        int _last = ((int[])var.getRecordRange())[1];
        int n = 0;
        if (start > _last) { // after the last record
            if (!longType) {
                double lastValue = var.asDoubleArray(new int[]{_last})[0];
                double[] data = (double[])_data;
                for (int i = start; i <= end; i++) {
                        data[n++] = lastValue;
                }
            } else {
                long lastValue = var.asLongArray(new int[]{_last})[0];
                long[] ldata = (long[])_data;
                for (int i = start; i <= end; i++) {
                    ldata[n++] = lastValue;
                }
            }
        }
    }

    public static double [] getOneDSeries(CDFImpl thisCDF, Variable var,
        int[] pt, boolean cm) throws Throwable {
        boolean toswap = (cm)?thisCDF.rowMajority():!thisCDF.rowMajority();
        return (double[])get1DSeries(thisCDF, var, pt, false, toswap);
    }
    static void do1DSwap(ByteBuffer bv, int type, Object temp, Object result,
       int offset, int number, boolean preserve, int[] dim) throws
       Throwable {
        double[] data = null;
        if (DataTypes.typeCategory[type] != DataTypes.LONG) {
            data = (double[])result;
        }
        double[] td = null;
        int n = 0;
        Method method;
        switch (DataTypes.typeCategory[type]) {
        case 0:
            float[] tf = new float[number];
            FloatBuffer bvf = bv.asFloatBuffer();
            bvf.get(tf, 0, number);
            if (dim.length == 2) {
                while (n < number) {
                    for (int j = 0; j < dim[1]; j++) {
                        for (int i = 0; i < dim[0]; i++) {
                            data[offset + n] = tf[i*dim[1] + j];
                            n++;
                        }
                    }
                }
            }
            if (dim.length == 3) {
                while (n < number) {
                    //System.out.println("number " + number + "," + n);
                    for (int k = 0; k < dim[2]; k++) {
                        for (int j = 0; j < dim[1]; j++) {
                            for (int i = 0; i < dim[0]; i++) {
                                data[offset + n] = 
                                    tf[i*dim[1]*dim[2] + j*dim[2] + k];
                                n++;
                            }
                        }
                    }
                }
            }
            break;
        case 1:
            DoubleBuffer bvd = bv.asDoubleBuffer();
            if (dim.length == 2) {
                while (n < number) {
                    for (int j = 0; j < dim[1]; j++) {
                        for (int i = 0; i < dim[0]; i++) {
                            data[offset + n] = bvd.get(i*dim[1] + j);
                            n++;
                        }
                    }
                }
            }
            if (dim.length == 3) {
                while (n < number) {
                    for (int k = 0; k < dim[2]; k++) {
                        for (int j = 0; j < dim[1]; j++) {
                            for (int i = 0; i < dim[0]; i++) {
                                data[offset + n] = 
                                    bvd.get(i*dim[1]*dim[2] + j*dim[2] + k);
                                n++;
                            }
                        }
                    }
                }
            }
            break;
        case 2:
            method = DataTypes.method[type];
            td = new double[number];
            for (int e = 0; e < number; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                td[e] = num.doubleValue();
            }
            break;
        case 3:
            method = DataTypes.method[type];
            td = new double[number];
            long longInt = DataTypes.longInt[type];
            for (int e = 0; e < number; e++) {
                Number num = (Number)method.invoke(bv, new Object[] {});
                int x = num.intValue();
                td[e] = (x >= 0)?(double)x:(double)(longInt + x);
            }
            break;
        case 5:
            LongBuffer bvl = bv.asLongBuffer();
            if (!preserve) {
                long[] tl = new long[number];
                data = (double[])result;
                td = new double[number];
                bvl.get(tl, 0, number);
                for (int i = 0; i < number; i++) {
                    td[i] = (double)tl[i];
                }
                break;
            }
            long[] ldata = (long[])result;
            if (dim.length == 2) {
                while (n < number) {
                    for (int j = 0; j < dim[1]; j++) {
                        for (int i = 0; i < dim[0]; i++) {
                            ldata[offset + n] = bvl.get(i*dim[1] + j);
                            n++;
                        }
                    }
                }
            }
            if (dim.length == 3) {
                while (n < number) {
                    for (int k = 0; k < dim[2]; k++) {
                        for (int j = 0; j < dim[1]; j++) {
                            for (int i = 0; i < dim[0]; i++) {
                                ldata[offset + n] = 
                                    bvl.get(i*dim[1]*dim[2] + j*dim[2] + k);
                                n++;
                            }
                        }
                    }
                }
            }
            break;
        }
        if (td != null) {
            if (dim.length == 2) {
                while (n < number) {
                    for (int j = 0; j < dim[1]; j++) {
                        for (int i = 0; i < dim[0]; i++) {
                            data[offset + n] = td[i*dim[1] + j];
                            n++;
                        }
                    }
                }
            }
            if (dim.length == 3) {
                while (n < number) {
                    for (int k = 0; k < dim[2]; k++) {
                        for (int j = 0; j < dim[1]; j++) {
                            for (int i = 0; i < dim[0]; i++) {
                                data[offset + n] = 
                                    td[i*dim[1]*dim[2] + j*dim[2] + k];
                                n++;
                            }
                        }
                    }
                }
            }
        }
    }
}
