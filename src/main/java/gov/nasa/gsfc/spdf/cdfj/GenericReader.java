package gov.nasa.gsfc.spdf.cdfj;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
/**
 * GenericReader extends MetaData class with methods to access variable
 * data. Data access methods of this class do not require a detailed knowledge
 * of the structure of CDF. Derived class CDFReader extends this class with
 * methods to access
 * time series.
 */
public class GenericReader extends MetaData {
    private ThreadGroup tgroup;
    private Hashtable threadMap = new Hashtable();
    static final Hashtable classMap = new Hashtable();
    static {
        classMap.put("long", Long.TYPE);
        classMap.put("double", Double.TYPE);
        classMap.put("float", Float.TYPE);
        classMap.put("int", Integer.TYPE);
        classMap.put("short", Short.TYPE);
        classMap.put("byte", Byte.TYPE);
        classMap.put("string", String.class);
    }
    GenericReader() {
        //setup();
    }
    void setImpl(CDFImpl impl) {thisCDF = impl;}
    /**
     * Constructs a reader for the given CDF file.
     */
    public GenericReader(String cdfFile) throws CDFException.ReaderError {
        File _file = new File(cdfFile);
        if (!_file.exists()) throw new CDFException.ReaderError(
            cdfFile + " does not exist.");
        if (_file.length() > Integer.MAX_VALUE) throw new 
            CDFException.ReaderError("Size of file " + cdfFile + " exceeds " +
            "Integer.MAX_VALUE. If data for individual variables is less " +
            "than this limit, you can use ReaderFactory.getReader(fileName) " +
            "to get a " + "GenericReader instance for this file.");
        try {
            thisCDF = CDFFactory.getCDF(cdfFile);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        //setup();
    }
    void setup() {
        tgroup = new ThreadGroup(Integer.toHexString(hashCode()));
    }
    /**
     * Constructs a reader for the given CDF URL.
     */
    public GenericReader(URL url) throws CDFException.ReaderError {
        try {
            thisCDF = CDFFactory.getCDF(url);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        //setup();
    }

    /**
     * Returns all available values for the given variable.
     * @param    varName   variable name
     * @return   a double array of dimension appropriate to the variable.
     * <p>
     * Type of object returned depends on the number of varying dimensions
     * and type of the CDF variable.<br>
     * For a numeric variable,
     * a double[], double[][], double[][][], or double[][][][] object is
     * returned for scalar, one-dimensional, two-dimensional, or
     * three-dimensional variable, respectively.</p>
     * <p>For a character string variable, a String[] or a String[][]
     * object is returned for a scalar or one-dimensional variable.</p>
     * @throws CDFException.ReaderError
     * for numeric variables of dimension higher than 3, and for
     * character string variables of dimension higher than 1.
     * @see #getOneD(String varName, boolean columnMajor)
     */
    public final Object get(String varName) throws CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        try {
            Method method = Extractor.getMethod(var, "Series");
            if ((method == null) || coreNeeded(var)) {
                return thisCDF.get(varName);
            }
            return method.invoke(null, new Object [] {thisCDF, var});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns all available values for the given long type variable.
     * @param    varName   variable name
     * @return   a long array of dimension appropriate to the variable.
     * <p>
     * Type of object returned depends on the number of varying dimensions.
     * <br>
     * For a numeric variable,
     * a long[], long[][], long[][][], or long[][][][] object is
     * returned for scalar, one-dimensional, two-dimensional, or
     * three-dimensional variable, respectively.</p>
     * @throws CDFException.ReaderError
     * for variables of type other than long, or dimension higher than 3.
     * @see #getOneD(String varName, boolean columnMajor)
     */
    public final Object getLong(String varName) throws CDFException.ReaderError 
        {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        try {
            return thisCDF.getLong(varName);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns all available values for the given scalar variable.
     * For variable of type long, loss of precision may occur.
     * @param    varName   variable name
     * @return   a double array
     * @throws CDFException.ReaderError
     * if variable is not a scalar, or is not a numeric type
     */
    public final double[] asDouble0(String varName) throws
        CDFException.ReaderError {
        try {
            int ndim = getEffectiveDimensions(varName).length;
            if (ndim != 0) throw new CDFException.ReaderError("Use asDouble" +
                    ndim + "(" + varName + ") for " + ndim +
                    "-dimensional variable " + varName);
            Object o = get(varName);
            double[] da;
            ArrayAttribute aa = new ArrayAttribute(o);
            if (aa.getType() == Long.TYPE) {
                long[] la = (long[])o;
                da = new double[la.length];
                for (int i = 0; i < la.length; i++) da[i] = (double)la[i];
            } else {
                da = (double[])o;
            }
            return da;
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns all available values for the given variable of rank 1.
     * For variable of type long, loss of precision may occur.
     * @param    varName   variable name
     * @return   a double[][]
     * @throws CDFException.ReaderError
     * if effective rank of variable is not 1, or the variable is not numeric.
     */
    public final double[][] asDouble1(String varName) throws 
        CDFException.ReaderError {
        try {
            int ndim = getEffectiveDimensions(varName).length;
            if (ndim != 1) throw new CDFException.ReaderError("Use asDouble" +
                    ndim + "(" + varName + ") for " + ndim +
                    "-dimensional variable " + varName);
            return (double[][])get(varName);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns all available values for the given variable of rank 2.
     * For variable of type long, loss of precision may occur.
     * @param    varName   variable name
     * @return   a double[][][]
     * @throws CDFException.ReaderError
     * if effective rank of variable is not 2, or the variable is not numeric.
     */
    public final double[][][] asDouble2(String varName) throws 
        CDFException.ReaderError {
        try {
            int ndim = getEffectiveDimensions(varName).length;
            if (ndim != 2) throw new CDFException.ReaderError("Use asDouble" +
                    ndim + "(" + varName + ") for " + ndim +
                    "-dimensional variable " + varName);
            return (double[][][])get(varName);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns all available values for the given variable of rank 3.
     * For variable of type long, loss of precision may occur.
     * @param    varName   variable name
     * @return   a double[][][][]
     * @throws CDFException.ReaderError
     * if effective rank of variable is not 3, or the variable is not numeric.
     */
    public final double[][][][] asDouble3(String varName) throws 
        CDFException.ReaderError {
        try {
            int ndim = getEffectiveDimensions(varName).length;
            if (ndim != 3) throw new CDFException.ReaderError("Use asDouble" +
                    ndim + "(" + varName + ") for " + ndim +
                    "-dimensional variable " + varName);
            return (double[][][][])get(varName);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns one dimensional representation of the values of a numeric
     * variable whose type is not INT8 or TT2000.
     * @param    varName   variable name
     * @param   columnMajor specifies whether the returned array conforms
     * to a columnMajor storage mode, i.e. the first index of a multi
     * dimensional array varies the fastest.
     * @return a double[] that represents values
     * of multi-dimensional arrays stored in a manner prescribed by the
     * columnMajor parameter.
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types
     * @see #get(String varName)
     */
    public final double[] getOneD(String varName, boolean columnMajor) throws
        CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        if (getNumberOfValues(varName) == 0) return new double[0];
        try {
            return (double[])thisCDF.getOneD(varName, columnMajor);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns values for a range of records for the given numeric variable
     * of rank &lt;&#61; 2.
     * @param    varName   variable name
     * @param    first     first record of range
     * @param    last     last record of range
     * @return   a double array of dimension appropriate to the variable.
     * <p>
     * Type of object returned depends on the number of varying dimensions
     * and type of the CDF variable.<br>
     * For a numeric variable,
     * a double[], double[][], double[][][] object is
     * returned for scalar, one-dimensional, or two-dimensional
     * variable, respectively.</p>
     * @throws CDFException.ReaderError
     * for non-numeric variables, or  variables whose effective rank  &gt; 2
     * @see #getRangeOneD(String varName, int first, int last,
     * boolean columnMajor)
     */
    public final Object getRange(String varName, int first, int last) throws
        CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        try {
            Method method = Extractor.getMethod(var, "Range");
            if ((method == null) || coreNeeded(var)) {
                return thisCDF.getRange(varName, first, last);
            }
            return method.invoke(null,
                new Object[] {thisCDF, var, new Integer(first),
                new Integer(last)});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns one dimensional representation of the values
     * for a range of records of a numeric 
     * variable whose type is not INT8 or TT2000.
     * @param    varName   variable name
     * @param    first     first record of range
     * @param    last     last record of range
     * @param   columnMajor specifies whether the returned array conforms
     * to a columnMajor storage mode, i.e. the first index of a multi
     * dimensional array varies the fastest.
     * @return a double[] that represents values
     * of multi-dimensional arrays stored in a manner prescribed by the
     * columnMajor parameter.
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types
     * @see #getRange(String varName, int first, int last)
     */
    public final double[] getRangeOneD(String varName, int first, int last,
        boolean columnMajor) throws CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        try {
            return (double[]) thisCDF.getRangeOneD(varName, first, last,
            columnMajor);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
/*
    public final double[] getOneD(String varName, int first, int last,
        int[] stride) throws CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        try {
            return thisCDF.get1D(varName, first, last, stride);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
*/
    /**
     * Returns values of the specified component of 1 dimensional
     * variable of numeric types other than INT8 or TT2000.
     * @param    varName   variable name
     * @param    component   component
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types,
     * and if the variable's effective rank is not 1.
     * @see #get(String varName)
     */
    public final double[] getVectorComponent(String varName, int component)
        throws CDFException.ReaderError {
        checkType(varName);
        if (getEffectiveRank(varName) != 1) throw new
            CDFException.ReaderError(varName + " is not a vector.");
        try {
            Variable var = thisCDF.getVariable(varName);
            Method method = Extractor.getMethod(var, "Element");
            if ((method == null) || coreNeeded(var)) {
                return (double[])thisCDF.get(varName, component);
            }
            return (double[])method.invoke(null,
                new Object[] {thisCDF, var, new Integer(component)});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns value of the specified component of 1 dimensional
     * variable of numeric types other than INT8 or TT2000.
     * @param    varName   variable name
     * @param    components   array containg components to be extracted
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types,
     * and if the variable's effective rank is not 1.
     * @see #get(String varName)
     */
    public final double[][] getVectorComponents(String varName,
        int[] components) throws Throwable {
        checkType(varName);
        if (getEffectiveRank(varName) != 1) throw new
            CDFException.ReaderError(varName + " is not a vector.");
        try {
            Variable var = thisCDF.getVariable(varName);
            Method method = Extractor.getMethod(var, "Elements");
            if ((method == null) || coreNeeded(var)) {
                return (double[][])thisCDF.get(varName, components);
            }
            return (double[][])method.invoke(null,
                new Object[] {thisCDF, var, components});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns values, in the specified record range,  of the specified
     * component of 1 dimensional * variable of numeric types other than
     * INT8 or TT2000.
     * @param    varName   variable name
     * @param    first     first record of range
     * @param    last     last record of range
     * @param    component   component
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types,
     * and if the variable's effective rank is not 1.
     * @see #getRange(String varName, int first, int last)
     */
    public final double[]  getRangeForComponent(String varName, int first,
        int last, int component) throws CDFException.ReaderError {
        checkType(varName);
        if (getEffectiveRank(varName) != 1) throw new
            CDFException.ReaderError(varName + " is not a vector.");
        try {
            Variable var = thisCDF.getVariable(varName);
            Method method = Extractor.getMethod(var, "RangeForElement");
            if ((method == null) || coreNeeded(var, new int[]{first, last})) {
                return (double[])thisCDF.getRange(varName, first, last,
                component);
            }
            return (double[])method.invoke(null,
                new Object[] {thisCDF, var, new Integer(first),
                new Integer(last), new Integer(component)});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns values, in the specified record range,  of specified
     * components of 1 dimensional variable of numeric types other than
     * INT8 or TT2000.
     * @param    varName   variable name
     * @param    first     first record of range
     * @param    last     last record of range
     * @param    components   components
     * @throws CDFException.ReaderError for character, INT8 or TT2000 types,
     * and if the variable's effective rank is not 1.
     * @see #getRange(String varName, int first, int last)
     */
    public final double[][] getRangeForComponents(String varName, int first,
        int last, int[] components) throws CDFException.ReaderError {
        checkType(varName);
        if (getEffectiveRank(varName) != 1) throw new
            CDFException.ReaderError(varName + " is not a vector.");
        try {
            Variable var = thisCDF.getVariable(varName);
            Method method = Extractor.getMethod(var, "RangeForElements");
            if ((method == null) || coreNeeded(var)) {
                return (double[][])thisCDF.get(varName, first, last,
                components);
            }
            return (double[][])method.invoke(null,
                new Object[] {thisCDF, var, new Integer(first),
                new Integer(last), components});
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Starts a new thread to extract specified data.
     * @param    varName   variable name
     * @param    targetType  desired type of extracted data - one of
     *                       the following: long, double, float, int, short,
     *                       byte or string
     * @param    recordRange 
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     * @return  Name of the thread. Methods to ascertain the
     *          availability and to retrieve require this name.
     * @throws CDFException.ReaderError
     * @see     #threadFinished(String threadName)
     * @see #getOneDArray(String threadName, boolean columnMajor)
     * @see #getBuffer(String threadName)
     */
    public final String startContainerThread(String varName,
        String  targetType, int[] recordRange, boolean preserve) throws
        CDFException.ReaderError {
        try {
            return startContainerThread(varName, targetType, recordRange,
            preserve, ByteOrder.nativeOrder());
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Starts a new thread to extract specified data.
     * @param    varName   variable name
     * @param    targetType  desired type of extracted data
     * @param    recordRange 
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     * @param    bo          ByteOrder for target ByteBuffer. ByteOrder
     *                       other than the native order may be specified
     *                       if the application requires it.
     * @return  Name of the container's thread. Methods to ascertain the
     *          availability and to retrieve  require this name.
     * @throws   Throwable
     * @see     #threadFinished(String threadName)
     * @see #getOneDArray(String threadName, boolean columnMajor)
     * @see #getBuffer(String threadName)
     */

    String startContainerThread(String varName,  String  targetType,
        int[] recordRange, boolean preserve, java.nio.ByteOrder bo) throws
        Throwable {
        String tname = threadName(varName, targetType, recordRange, preserve,
            bo);
        Class type = getContainerClass(targetType);
        VDataContainer container = getContainer(varName, type,
            recordRange, preserve, bo);
        if (tgroup == null) setup();
        Thread thread = new Thread(tgroup, container, tname);
        thread.start();
        threadMap.put(tname, new ThreadMapEntry(container, thread));
        return tname;
    }

    /**
     * Returns  whether the named thread (started via this object) has
     * finished.
     */
    public final boolean threadFinished(String threadName) throws
        CDFException.ReaderError {
        Thread thread =
            ((ThreadMapEntry)threadMap.get(threadName)).getThread();
        if (thread == null) {
            throw new CDFException.ReaderError("Invalid thread name " +
            threadName);
        }
        return (thread.getState() == Thread.State.TERMINATED);
    }

    /**
     * Returns data extracted by the named thread as ByteBuffer.
     * After this method returns the ByteBuffer, threadName is forgotten.
     */ 
    public final ByteBuffer getBuffer(String threadName) throws Throwable {
        if (threadFinished(threadName)) {
            synchronized (threadMap) {
                VDataContainer container =
                ((ThreadMapEntry)threadMap.get(threadName)).getContainer();
                ByteBuffer buffer = null;
                try {
                    buffer = container.getBuffer();
                } catch (Throwable th) {
                    throw new CDFException.ReaderError(th.getMessage());
                }
                threadMap.remove(threadName);
                return buffer;
            }
        } else {
            throw new CDFException.ReaderError("Thread " + threadName +
            " is working");
        }
    }

    /**
     * Returns data extracted by the named thread as a one dimensional
     * array, organized according to specified row majority..
     */ 
    public final Object getOneDArray(String threadName, boolean columnMajor)
        throws CDFException.ReaderError {
        if (threadFinished(threadName)) {
            synchronized (threadMap) {
                VDataContainer container =
                ((ThreadMapEntry)threadMap.get(threadName)).getContainer();
                //System.out.println("getOneDArray: " + container);
                Object array = null;
                try {
                    array = container.asOneDArray(columnMajor);
                } catch (Throwable th) {
                    throw new CDFException.ReaderError(th.getMessage());
                }
                threadMap.remove(threadName);
                return array;
            }
        } else {
            throw new CDFException.ReaderError("Thread " + threadName +
            " is working");
        }
    }

    /**
     * Returns specified data as ByteBuffer of specified type.
     * Order of the ByteBuffer is 'native'. Data is organized according to
     * storage model of the variable returned by rowMajority(). A DirectBuffer
     * is allocated.
     * @param    varName   variable name
     * @param    targetType  desired type of extracted data
     * @param    recordRange 
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     */
    public final ByteBuffer getBuffer(String varName,  String  targetType,
        int[] recordRange, boolean preserve) throws CDFException.ReaderError {
        return getBuffer(varName, targetType, recordRange, preserve, true);
    }

    /**
     * Returns specified data as ByteBuffer of specified type.
     * Order of the ByteBuffer is 'native'. Data is organized according to
     * storage model of the variable returned by rowMajority().
     * @param    varName   variable name
     * @param    targetType  desired type of extracted data
     * @param    recordRange 
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     * @param    useDirect   specifies whether a DirectBuffer should be used.
     *                       if set to false, an array backed buffer will be
     *                       allocated.
     */ 
    public final ByteBuffer getBuffer(String varName,  String  targetType,
        int[] recordRange, boolean preserve, boolean useDirect) throws
        CDFException.ReaderError {
        Class type;
        try {
            type = getContainerClass(targetType);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        if (!isCompatible(varName, type, preserve)) throw
            new CDFException.ReaderError("Requested type " + targetType +
            " not compatible with preserve = " + preserve);
        VDataContainer container = null;
        try {
            container = getContainer(varName, type,
            recordRange, preserve, ByteOrder.nativeOrder());
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        container.setDirect(useDirect);
        container.run();
        return container.getBuffer();
    }

    /**
     * Returns specified data as a one dimensional
     * array, organized according to specified row majority..
     * @param    varName   variable name
     * @param    targetType  desired type of extracted data
     * @param    recordRange 
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     * @param   columnMajor specifies whether the returned array conforms
     * to a columnMajor storage mode, i.e. the first index of a multi
     * dimensional array varies the fastest.
     */ 
    public final Object getOneDArray(String varName, String targetType,
        int[] recordRange, boolean preserve, boolean columnMajor) throws
        CDFException.ReaderError {
        VDataContainer container = null;
        try {
            Class type = getContainerClass(targetType);
            container = getContainer(varName, type,
            recordRange, preserve, ByteOrder.nativeOrder());
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        container.run();
        return container.asOneDArray(columnMajor);
    }

    String threadName(String varName, String type, int[] recordRange,
        boolean preserve, java.nio.ByteOrder bo) {
        StringBuffer sb = new StringBuffer(varName + "_" + type + "_");
        if (recordRange == null) {
            sb.append("null_");
        } else {
            sb.append(recordRange[0]).append("_").append(recordRange[1]);
            sb.append("_");
        }
        sb.append(preserve).append("_" + Math.random() + "_" + bo);
        return sb.toString();
    }

    Class getContainerClass(String stype) throws Throwable {
        Class cl = (Class)classMap.get(stype.toLowerCase());
        if (cl == null) throw new Throwable("Unrecognized type " + stype);
        return cl;
    }

    class ThreadMapEntry {
        VDataContainer container;
        Thread thread;
        ThreadMapEntry(VDataContainer container, Thread thread) {
            this.container = container;
            this.thread = thread;
        }
        VDataContainer getContainer() {return container;}
        Thread getThread() {return thread;}
    }

    void checkType(String varName) throws CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new CDFException.ReaderError(
            "No such variable " + varName);
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new CDFException.ReaderError(
            "This method cannot be used for " +
            "variables of type long. Use the get methods for the " +
            "variable " + "and the associated time variable. ");
        }
    }

    public final boolean sourceIsFile() {return thisCDF.getSource().isFile();}

    /**
     * Returns the name of the source CDF
     */
    public final String getSource() {return thisCDF.getSource().getName();}

    /**
     * Returns whether a variable is scalar.
     */
    public final boolean isScalar(String varName) throws
        CDFException.ReaderError {
        return getEffectiveRank(varName) == 0;
    }

    /**
     * Returns whether a variable is vector.
     */
    public final boolean isVector(String varName) throws
        CDFException.ReaderError {
        return getEffectiveRank(varName) == 1;
    }
    /**
     * Returns the name of the user supplied time variable for
     * the given variable. 
     * For CDF that does not conform to ISTP specification for identifying
     * time variable associated with a variable, applications need to
     * override this method via a subclass. Default implementation assumes
     * ISTP compliance, and returns null.
     * @param    varName   variable name
     * @return  String   user supplied name, or null if none 
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public String userTimeVariableName(String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return null;
    }

    BaseVarContainer getRangeContainer(String varName, int[] range,
        String type, boolean preserve) throws Throwable {
        if (!existsVariable(varName)) throw new Throwable(
            "CDF does not hava a variable named " + varName);
        if (DataTypes.isStringType(getType(varName))) {
            throw new Throwable("Function not supported for string variables");
        }
        Class cl = (Class)classMap.get(type);
        if (cl == null) throw new Throwable("Invalid type " + type);
        BaseVarContainer container = null;
        Variable var = thisCDF.getVariable(varName);
        if (type == "float") {
            container = new FloatVarContainer(thisCDF, var, range, preserve);
        }
        if (type == "double") {
            container = new DoubleVarContainer(thisCDF, var, range, preserve);
        }
        if (type == "int") {
            container = new IntVarContainer(thisCDF, var, range, preserve);
        }
        if (type == "short") {
            container = new ShortVarContainer(thisCDF, var, range, preserve);
        }
        if (type == "byte") {
            container = new ByteVarContainer(thisCDF, var, range);
        }
        if (type == "long") {
            container = new LongVarContainer(thisCDF, var, range);
        }
/*
        String pkg = getClass().getPackage().getName();
        String cname = pkg + "." + (type.substring(0,1)).toUpperCase() +
            type.substring(1) + "VarContainer";
        Class cclass = Class.forName(cname);
        Constructor ccons;
        if (type == "byte") {
            ccons = cclass.getConstructor(
               new Class[]{thisCDF.getClass(), Class.forName(pkg + ".Variable"),
               range.getClass()});
        } else {
            ccons = cclass.getConstructor(
               new Class[]{thisCDF.getClass(), Class.forName(pkg + ".Variable"),
               range.getClass(), Boolean.TYPE});
        }
        container = (BaseVarContainer)
            ccons.newInstance(thisCDF, thisCDF.getVariable(varName), range,
            preserve);
*/
        container.run();
        return container;
    }

    /**
     * Returns sampled values of a  numeric variable as one dimensional
     * array of specified type and storage model.
     * @param    varName   variable name
     * @param    stride    array of length 1 where value specifies stride
     * @param    type  desired type of extracted data - one of
     *                 the following: "long", "double", "float", "int", "short",
     *                 or "byte"
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     * @param   columnMajor specifies whether the returned array conforms
     * to a columnMajor storage mode, i.e. the first index of a multi
     * dimensional array varies the fastest.
     */
    public Object getSampled(String varName, int[] range, 
        int stride, String type, boolean preserve, boolean columnMajor) throws 
        CDFException.ReaderError {
        try {
            BaseVarContainer container = getRangeContainer(varName,
                range, type, preserve);
            int[] _stride = (stride > 0)?new int[]{stride}:
                   new int[] {-1, -stride};
            return container.asOneDArray(columnMajor, new Stride(_stride));
        } catch (Throwable t) {
            throw new CDFException.ReaderError(t.getMessage());
        }
    }
    /**
     * Returns sampled values of a  numeric variable as one dimensional
     * array of specified type.
     * Data for records is organized according to the storage model of the
     * variable (as returned by rowMajority()).
     * @param    varName   variable name
     * @param    stride    array of length 1 where value specifies stride
     * @param    type  desired type of extracted data - one of
     *                 the following: "long", "double", "float", "int", "short",
     *                 or "byte"
     * @param    preserve    specifies whether the target must preserve
     *                       precision. if false, possible loss of precision
     *                       is deemed acceptable.
     */
    public Object getSampled(String varName, int first, int last,
        int stride, String type, boolean preserve) throws 
        CDFException.ReaderError {
        try {
            BaseVarContainer container = getRangeContainer(varName,
                new int[]{first, last}, type, preserve);
            int[] _stride = (stride > 0)?new int[]{stride}:
                   new int[] {-1, -stride};
            return container.asSampledArray(new Stride(_stride));
        } catch (Throwable t) {
            throw new CDFException.ReaderError(t.getMessage());
        }
    }

    private static final boolean coreNeeded(Variable var) {
        return var.isMissingRecords();
    }

    private static final boolean coreNeeded(Variable var, int[] range) {
        int[] available = var.getRecordRange();
        if (range.length == 1) {
            if (range[0] >= available[0]) {
                return var.isMissingRecords();
            }
            return true;
        }
        if ((range[0] >= available[0]) && (range[1] <= available[1])) {
            return var.isMissingRecords();
        }
        return true;
    }

    VDataContainer getContainer(String varName, Class type,
        int[] recordRange, boolean preserve, ByteOrder bo) throws Throwable {
        Variable var = thisCDF.getVariable(varName);
        if (var == null) throw new Throwable("No such variable " + varName);
        if (type == Double.TYPE) {
            return var.getDoubleContainer(recordRange, preserve, bo);
        }
        if (type == Float.TYPE) {
            return var.getFloatContainer(recordRange, preserve, bo);
        }
        if (type == Long.TYPE) {
            return var.getLongContainer(recordRange, bo);
        }
        if (type == Integer.TYPE) {
            return var.getIntContainer(recordRange, preserve, bo);
        }
        if (type == Short.TYPE) {
            return var.getShortContainer(recordRange, preserve, bo);
        }
        if (type == Byte.TYPE) {
            return var.getByteContainer(recordRange);
        }
        if (type == String.class) {
            return var.getStringContainer(recordRange);
        }
        throw new Throwable("Invalid type ");
    }
    public final int getBufferCapacity(String varName,  String  targetType,
        int[] recordRange) throws CDFException.ReaderError {
        VDataContainer container = null;
        try {
            Class type = getContainerClass(targetType);
            container = getContainer(varName, type,
            recordRange, false, ByteOrder.nativeOrder());
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        return container.getCapacity();
    }
    public final ByteBuffer getBuffer(String varName,  String  targetType,
        int[] recordRange, boolean preserve, ByteBuffer buffer) throws
        CDFException.ReaderError {
        VDataContainer container = null;
        try {
            Class type = getContainerClass(targetType);
            container = getContainer(varName, type,
            recordRange, preserve, ByteOrder.nativeOrder());
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        container.setUserBuffer(buffer);
        container.run();
        return container.getBuffer();
    }
}
