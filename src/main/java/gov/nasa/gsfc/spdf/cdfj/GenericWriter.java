package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.lang.reflect.*;
/**
 * Base class for creating a version 3.6 CDF.
 * Derived class CDFWriter provides methods for creating a CDF
 * which includes user selected data from existing CDFs.
 */
public class GenericWriter {
    LinkedHashMap<String, ADR> attributes = new LinkedHashMap<String, ADR>();
    LinkedHashMap<String, Vector<AEDR>> attributeEntries = 
        new LinkedHashMap<String, Vector<AEDR>>();
    LinkedHashMap<String, VDR> variableDescriptors =
        new LinkedHashMap<String, VDR>();
    int lastLeapSecondId = -1;
    CDR cdr = new CDR();
    GDR gdr = new GDR();
    public final boolean rowMajority;
    /**
     * Constructs a column major GenericWriter 
     */
    public GenericWriter() {
        this(true);
    }

    /**
     * Constructs a GenericWriter of specified row majority
     */
    public GenericWriter(boolean rowMajority) {
        this.rowMajority = rowMajority;
    }

    ADR getAttribute(String name, boolean global) {
        return getAttribute(name, global, true);
    }

    ADR getAttribute(String name, boolean global, boolean create) {
        ADR adr = attributes.get(name);
        if (adr != null) return attributes.get(name);
        if (!create) return null;
        adr = new ADR();
        adr.setScope((global)?1:2);
        adr.name = name;
        int anumber = attributes.size();
        adr.setNum(anumber);
        attributes.put(name, adr);
        return adr;
    }
    /**
     * Adds a global attribute entry.
     * @param   name name of the attribute
     * @param   value array or wrapped scalar value to assign to attribute
     */
    public void addGlobalAttributeEntry(String name, Object value) throws
        CDFException.WriterError {
        addGlobalAttributeEntry(name, null, value);
    }
    /**
     * Adds a global attribute entry of specified type..
     * @param   name name of the attribute
     * @param   dataType {@link CDFDataType CDFDataType} desired
     * @param   value array or wrapped scalar value to assign to attribute
     */
    public void addGlobalAttributeEntry(String name, CDFDataType dataType,
        Object value) throws CDFException.WriterError {
        ADR adr = getAttribute(name, true);
        Vector<AEDR> values = attributeEntries.get(name);
        if (values == null) {
            values = new Vector<AEDR>();
            attributeEntries.put(name, values);
        }
        GlobalAttributeEntry gae;
        int type = (dataType == null)?-1:dataType.getValue();
        try {
            gae = new GlobalAttributeEntry(adr, type, value);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
        gae.setNum(adr.ngrEntries);
        adr.mAXgrEntry = adr.ngrEntries;
        adr.ngrEntries++;
        values.add(gae);
    }

    /**
     * Returns AttributeEntry collection for the given variable and
     * attribute.
     * @param   vname name of the variable
     * @param   aname name of the attribute
     */
    Vector<VariableAttributeEntry> findVariableAttributeEntries(String vname,
        String aname) throws CDFException.WriterError {
        VDR vdesc = variableDescriptors.get(vname);
        if (vdesc == null) {
            throw new CDFException.WriterError("Variable " + vname +
            " has not been defined.");
        }
        Vector<VariableAttributeEntry> result =
            new Vector<VariableAttributeEntry>();
        Vector<AEDR> entries = attributeEntries.get(aname);
        if (entries == null) return result;
        for (int i = 0; i < entries.size(); i++) {
            VariableAttributeEntry vae;
            try {
                vae = (VariableAttributeEntry) entries.get(i);
            } catch (Exception ex) {
                continue;
            }
            if (vae.getNum() == vdesc.getNum()) result.add(vae);
        }
        return result;
    }

    /**
     * Sets the value of a given attribute for a variable.
     * @param   vname name of the variable
     * @param   aname name of the attribute
     * @param   value array of primitives, or String value to assign to
     *                attribute
     */
    public void setVariableAttributeEntry(String vname, String aname,
        Object value) throws CDFException.WriterError {
        setVariableAttributeEntry(vname, aname, null, value);
    }
    /**
     * Sets the value of a given attribute for a variable.
     * @param   vname name of the variable
     * @param   aname name of the attribute
     * @param   dataType {@link CDFDataType CDFDataType} desired
     * @param   value array of primitives, or String value to assign to
     *                attribute
     * Overwrites previous value, if any
     */
    public void setVariableAttributeEntry(String vname, String aname,
        CDFDataType dataType, Object value) throws CDFException.WriterError {
        Vector<VariableAttributeEntry> entries;
        entries = findVariableAttributeEntries(vname, aname);
        if (entries.size() > 0) {
            if (!(value.getClass().isArray())) {
//              attributeEntries.get(aname).remove(entries.get(0));
//          } else {
                if (value.getClass() != String.class) {
                    throw new CDFException.WriterError(
                    "Value should be numeric array or a String.");
                }
            }
            for (int i = 0; i < entries.size(); i++) {
                attributeEntries.get(aname).remove(entries.get(i));
            }
            ADR adr = getAttribute(aname, false);
            adr.nzEntries--;
        }
        addVariableAttributeEntry(vname, aname, dataType, value);
    }

    /**
     * Sets the value of a given attribute for a variable.
     * This method creates a new value for the given attribute.
     * If an entry exists, new value is added to the existing entry 
     * if both are of String type.
     * @param   vname name of the variable
     * @param   aname name of the attribute
     * @param   value array of primitives, or String value to assign to
     *                attribute
     */
    public void addVariableAttributeEntry(String vname, String aname,
        Object value) throws CDFException.WriterError {
        addVariableAttributeEntry(vname, aname, null, value);
    }

    /**
     * Sets the value of a given attribute for a variable.
     * This method creates a new value for the given attribute.
     * If an entry exists, new value is added to the existing entry 
     * if both are of String type.
     * @param   vname name of the variable
     * @param   aname name of the attribute
     * @param   dataType {@link CDFDataType CDFDataType} desired
     * @param   value array of primitives, or String value to assign to
     *                attribute
     */
    public void addVariableAttributeEntry(String vname, String aname,
        CDFDataType dataType, Object value) throws CDFException.WriterError {

        VDR vdesc = variableDescriptors.get(vname);
        if (vdesc == null) {
            throw new CDFException.WriterError("Variable " + vname +
            " has not been defined.");
        }
        Vector<VariableAttributeEntry> currentEntries;
        currentEntries = findVariableAttributeEntries(vname, aname);
        if (currentEntries.size() == 0) {
            if (!attributeEntries.containsKey(aname)) {
                attributeEntries.put(aname, new Vector<AEDR>());
            }
        } else {
            if (value.getClass() != String.class) {
                int _type = 
                     currentEntries.get(currentEntries.size() - 1).dataType;
                if (DataTypes.isStringType(_type)) {
                    throw new CDFException.WriterError(
                    "Only String values can be added");
                }
            }
        }
        ADR adr = getAttribute(aname, false);
        VariableAttributeEntry vae;
        int type = (dataType == null)?-1:dataType.getValue();
        try {
            vae = new VariableAttributeEntry(adr, type, value);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
        vae.setNum(vdesc.getNum());
        attributeEntries.get(aname).add(vae);
        if (vdesc.getNum() > adr.mAXzEntry) adr.mAXzEntry = vdesc.getNum();
        adr.nzEntries++;
    }

    /**
     * Returns whether the time variable has been defined for a variable.
     */
    public boolean hasTimeVariable(String name) throws
        CDFException.WriterError {
        VDR vdr = variableDescriptors.get(name);
        if (vdr == null) throw new CDFException.WriterError("Variable " + name +
            " has not been defined yet.");
        return (findVariableAttributeEntries(name, "DEPEND_0").size() == 0);
    }

    /**
     * Defines a time variable of the specified {@link CDFTimeType time type}.
     */
    public void defineTimeVariable(String name, CDFTimeType timeType) throws
        CDFException.WriterError {
        defineVariable(name, CDFDataType.getType(timeType), new int[0]);
    }

    /**
     * Defines a time series for a new variable of specified data type.
     * Variable's times are contained in a time variable named Epoch which
     * must have been created before this method is called.
     */
    public void defineTimeSeries(String name, CDFDataType dataType, int[] dim)
        throws CDFException.WriterError {
        defineTimeSeries(name, dataType, dim, "Epoch");
    }

    /**
     * Defines a time series of the named variable of specified data type
     * and the time variable of specified name.
     * The named time variable must have been defined before this method
     * is called. Name of the time variable is assigned to the DEPEND_0
     * attribute.
     */
    public void defineTimeSeries(String name, CDFDataType dataType, int[] dim,
        String tname) throws CDFException.WriterError {
        defineVariable(name, dataType, dim);
        VDR vdr = variableDescriptors.get(name);
        VDR tvdr = variableDescriptors.get(tname);
        if (tvdr == null) {
            throw new CDFException.WriterError("TimeVariable " + tname +
            " does not exist.");
        }
        addVariableAttributeEntry(name, "DEPEND_0", tname);
    }

    /**
     * Defines a time series of the named variable of specified data type and
     * the time variable of specified name and type.
     * Variable's data is compressed before it is stored 
     */
    public void defineCompressedTimeSeries(String name, CDFDataType dataType,
        int[] dim, String tname, CDFTimeType timeType) throws
        CDFException.WriterError {
        defineTimeSeries(name, dataType, dim, tname, timeType, true);
    }

    /**
     * Defines a time series of a new named variable of specified data type
     * and the time variable of specified name and type.
     */
    public void defineTimeSeries(String name, CDFDataType dataType, int[] dim,
        String tname, CDFTimeType timeType, boolean compressed) throws
        CDFException.WriterError {
        if (!compressed) defineVariable(name, dataType, dim);
        if (compressed) defineCompressedVariable(name, dataType, dim);
        VDR vdr = variableDescriptors.get(name);
        VDR tvdr = variableDescriptors.get(tname);
        if (tvdr != null) {
            throw new CDFException.WriterError("TimeVariable " + tname +
            " already exists.");
        }
        defineTimeVariable(tname, timeType);
        addVariableAttributeEntry(name, "DEPEND_0", tname);
    }
            
    /**
     * Defines a named variable of specified numeric data type
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim)
        throws CDFException.WriterError {
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        defineVariable(name, dataType, dim, 1);
    }

    /**
     * Defines a named variable of string data type
     */
    public void defineStringVariable(String name, int[] dim, int size) throws
        CDFException.WriterError {
        defineVariable(name, CDFDataType.CHAR, dim, size);
    }

    /**
     * Defines a named variable of specified numeric data type and dimensions.
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim,
        int size) throws CDFException.WriterError {
        if (!(dataType == CDFDataType.CHAR) && (size > 1)) throw new
            CDFException.WriterError( "incompatible size for type " + dataType);
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        defineVariable(name, dataType, dim, varys, true, false, null, size);
    }

    /**
     * Defines a compressed variable of specified numeric data type and
     * dimensions.
     */
    public void defineCompressedVariable(String name, CDFDataType dataType,
        int[] dim) throws CDFException.WriterError {
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        defineCompressedVariable(name, dataType, dim, 1);
    }

    /**
     * Defines a compressed variable of string type with given dimensions.
     */
    public void defineCompressedStringVariable(String name, int[] dim, int size)
        throws CDFException.WriterError {
        defineCompressedVariable(name, CDFDataType.CHAR, dim, size);
    }

    /**
     * Defines a compressed variable of string type with given dimensions.
     */
    public void defineCompressedVariable(String name, CDFDataType dataType,
        int[] dim, int size) throws CDFException.WriterError {
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        defineVariable(name, dataType, dim, varys, true, true, null, size);
    }

    LinkedHashMap<String, DataContainer> dataContainers =
        new LinkedHashMap<String, DataContainer>();

    /**
     * Adds an NRV record of string type.
     */ 
    public void addNRVString(String name, String value) throws
        CDFException.WriterError {
        addNRVVariable(name, CDFDataType.CHAR, new int[0], value.length(),
                       value);
    }

    /**
     * Adds a scalar NRV record of the given numeric type.
     */ 
    public void addNRVVariable(String name, CDFDataType dataType,
        Object value) throws CDFException.WriterError {
        addNRVVariable(name, dataType, new int[0], 1, value);
    }

    /**
     * Adds a NRV record of the given numeric type and dimension.
     */ 
    public void addNRVVariable(String name, CDFDataType dataType, int[] dim,
        Object value) throws CDFException.WriterError {
        if (dataType == CDFDataType.CHAR) throw new CDFException.WriterError(
        "Invalid method for string type. Use " +
        "addNRVVariable(name, dataType, dim, size, value)");
        addNRVVariable(name, dataType, dim, 1, value);
    }

    /**
     * Adds a NRV record of the given type and dimensions.
     */ 
    public void addNRVVariable(String name, CDFDataType dataType, int[] dim,
        int size, Object value) throws CDFException.WriterError {
        if (!(dataType == CDFDataType.CHAR) && (size > 1)) throw new
            CDFException.WriterError( "incompatible size for type " + dataType);
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        defineVariable(name, dataType, dim, varys, false, false, null, size);
        if ((dim.length > 0) || (dataType == CDFDataType.EPOCH16)) {
            try {
                addData(name, AArray.getPoint(value));
            } catch (Throwable th) {
                throw new CDFException.WriterError(th.getMessage());
            }
        } else {
            dispatch(name, value);
        }
    }

    /**
     * Defines a NRV record of the given type and dimensions.
     * Parameter size is ignored for variables of numeric types.
     */ 
    public void defineNRVVariable(String name, CDFDataType dataType, int[] dim,
        int size) throws CDFException.WriterError {
        boolean[] varys = new boolean[dim.length];
        for (int i = 0; i < varys.length; i++) varys[i] = true;
        int _size = (dataType == CDFDataType.CHAR)?size:1;
        defineVariable(name, dataType, dim, varys, false, false, null, _size);
    }

    /**
     * Defines a variable of specified numeric data type using default
     * sparse record option.
     * @param name
     * @param dataType {@link CDFDataType data tpe}
     * @param  dim      dimensions
     * @param  varys    dimension variance
     * @param recordVariance
     * @param compressed whether the values will be saved in compressed form
     * @param pad     array or wrapped scalar value to assign to use as pad
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad) throws CDFException.WriterError {
        defineVariable(name, dataType, dim, varys, recordVariance, compressed,
            pad, 1, SparseRecordOption.NONE);
    }
    /**
     * Defines a variable of specified numeric data type using given
     * sparse record option.
     * @param name
     * @param dataType {@link CDFDataType data tpe}
     * @param  dim      dimensions
     * @param  varys    dimension variance
     * @param recordVariance
     * @param compressed whether the values will be saved in compressed form
     * @param pad     array or wrapped scalar value to assign to use as pad
     * @param option {@link SparseRecordOption sparse record option}
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad, SparseRecordOption option) throws CDFException.WriterError {
        defineVariable(name, dataType, dim, varys, recordVariance, compressed,
            pad, 1, option);
    }
    /**
     * Defines a variable of string type using default
     * sparse record option.
     * same as:
     * defineStringVariable(String name, int[] dim,
     *  boolean[] varys, boolean recordVariance, boolean compressed,
     *  Object pad, int size, SparseRecordOption.NONE)
     * @see
     * #defineStringVariable(String name, int[] dim,
     *  boolean[] varys, boolean recordVariance, boolean compressed,
     *  Object pad, int size, SparseRecordOption option)
     */
    public void defineStringVariable(String name, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad, int size) throws CDFException.WriterError {
        defineVariable(name, CDFDataType.CHAR, dim, varys, recordVariance,
            compressed, pad, size, SparseRecordOption.NONE);
    }
    /**
     * Defines a variable of string type using given
     * sparse record option.
     * @param name
     * @param  dim      dimensions
     * @param  varys    dimension variance
     * @param recordVariance
     * @param compressed whether the values will be saved in compressed form
     * @param pad     array or wrapped scalar value to assign to use as pad
     * @param size    length of character string
     * @param option {@link SparseRecordOption sparse record option}
     */
    public void defineStringVariable(String name, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad, int size, SparseRecordOption option) throws
        CDFException.WriterError {
        defineVariable(name, CDFDataType.CHAR, dim, varys, recordVariance,
            compressed, pad, size, option);
    }

    /**
     * Defines a variable of string type using given
     * sparse record option.
     * @param name
     * @param  dim      dimensions
     * @param  varys    dimension variance
     * @param recordVariance
     * @param compressed whether the values will be saved in compressed form
     * @param pad     array or wrapped scalar value to assign to use as pad
     * @param size    length of character string
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad, int size) throws CDFException.WriterError {
        defineVariable(name, dataType, dim, varys, recordVariance, compressed,
            pad, size, SparseRecordOption.NONE);
    }

    /**
     * Defines a new variable
     * @param   name   Variable name
     * @param   dataType {@link CDFDataType data type} of the variable
     * @param   dim    dimension
     * @param   varys  
     * @param  recordVariance
     * @param  compressed  whether the variable data appears in compressed
     *                     form in the CDF
     * @param  pad         Object to use as a pad value - a Number object
     *                     for a numeric variable, a String for a character
     *                     variable
     * @param  size        length of charater string for character variable,
     *                     Must be 1 for numeric type variable
     * @param  option      {@link SparseRecordOption sparse record option}
     *
     */
    public void defineVariable(String name, CDFDataType dataType, int[] dim,
        boolean[] varys, boolean recordVariance, boolean compressed,
        Object pad, int size, SparseRecordOption option) throws
        CDFException.WriterError {
        int[] _dim;
        boolean[] _varys;
        synchronized (dim) {
            _dim = new int[dim.length];
            for (int i = 0; i < dim.length; i++) _dim[i] = dim[i];
        }
        synchronized (varys) {
            _varys = new boolean[varys.length];
            for (int i = 0; i < varys.length; i++) _varys[i] = varys[i];
        }
        if (dataType == CDFDataType.EPOCH16) {
            if (dim.length > 0) throw new CDFException.WriterError(
            "Only scalar variables of type EPOCH16 are supported.");
        }
        VDR vdr = variableDescriptors.get(name);
        if (vdr != null) throw new CDFException.WriterError("Variable " + name +
            " exists already.");
        Object _pad = null;
        if (pad != null) {
            Class<?> cl = pad.getClass();
            if (!cl.isArray()) {
                _pad = java.lang.reflect.Array.newInstance(cl, 1);
                java.lang.reflect.Array.set(_pad, 0, pad);
            } else {
                _pad = pad;
            }
        }
        try {
            vdr = new VDR(name, dataType.getValue(), dim, varys, recordVariance,
            compressed, _pad, size, option);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
        vdr.setNum(variableDescriptors.size());
        variableDescriptors.put(name, vdr);
        DataContainer dc = new DataContainer(vdr, rowMajority);
        dataContainers.put(name, dc);
    }

    HashMap<String, VDR> getVariableDescriptors() {
        return variableDescriptors;
    }

    DataContainer getContainer(String name, Object data) throws
        CDFException.WriterError {
        ArrayAttribute aa = null;
        try {
            aa = new ArrayAttribute(data);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
        if (aa.getDimensions().length != 1) throw new CDFException.WriterError(
        "data must be a 1 dimensional array. ");
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        return container;
    }
    /**
     * Adds data (represented as a one dimensional array) to a variable.
     * same as addOneD(String name, Object data, null, false)
     * @see
     *  #addOneD(String name, Object data, int[] recordRange, boolean relax)
     */
    public void addOneD(String name, Object data) throws
        CDFException.WriterError {
        DataContainer container = getContainer(name, data);
        try {
            container.addData(data, null, true, false);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }

    /**
     * Adds data (represented as a one dimensional array) to a variable.
     * same as addOneD(String name, Object data, null, boolean relax)
     * @see
     *  #addOneD(String name, Object data, int[] recordRange, boolean relax)
     */
    public void addOneD(String name, Object data, boolean relax) throws
        CDFException.WriterError {
        DataContainer container = getContainer(name, data);
        try {
            container.addData(data, null, true, relax);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }
    /**
     * Adds data (represented as a one dimensional array) 
     * for a specified record range to a variable.
     * same as addOneD(String name, Object data, int[] recordRange, false)
     * @see
     *  #addOneD(String name, Object data, int[] recordRange, boolean relax)
     */
    public void addOneD(String name, Object data, int[] recordRange) throws
        CDFException.WriterError {
        DataContainer container = getContainer(name, data);
        try {
            container.addData(data, recordRange, true, false);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }
    /**
     * Adds data (represented as a one dimensional array) 
     * for a specified record range to a variable.
     * @param name name of the variable.
     * @param data a one dimensional array of a type compatible with
     * the type of variable.
     * @param recordRange int[2] containing record range. May be null, in
     * which case range is assumed to be follow last record added.
     * @param relax  relevant for unsigned data types, CDF_UINT1,
     * CDF_UINT2 and CDF_UINT4 only,specifies that values in
     * data array can be interpreted as unsigned.
     * <table summary="">
     * <tr><td>CDF Type of Variable</td><td>Type of Array</td></tr>
     * <tr><td>INT8, TT2000, UINT4</td><td>long</td></tr>
     * <tr><td>UINT4</td><td>int, if relax = true, long otherwise</td></tr>
     * <tr><td>DOUBLE, EPOCH, EPOCH16</td><td>double</td></tr>
     * <tr><td>FLOAT</td><td>float or double</td></tr>
     * <tr><td>INT4</td><td>int</td></tr>
     * <tr><td>UINT2</td><td>short, if relax = true, int otherwise</td></tr>
     * <tr><td>INT2</td><td>short</td></tr>
     * <tr><td>UINT1</td><td>byte, if relax = true, short otherwise</td></tr>
     * <tr><td>INT1</td><td>byte</td></tr>
     * <tr><td>CHAR</td><td>String</td></tr>
     * </table>
     * data must contain an integral number of points, and its
     * contents must conform to the row majority
     * of this GenericWriter.
     */
    public void addOneD(String name, Object data, int[] recordRange,
        boolean relax) throws CDFException.WriterError {
        DataContainer container = getContainer(name, data);
        try {
            container.addData(data, recordRange, true, relax);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }

    /**
     * Adds data to a variable.
     *   same as addData(String name, Object data, false)
     *@see
     *  #addData(String name, Object data, boolean relax)
     */
    public void addData(String name, Object data) throws
        CDFException.WriterError {
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        try {
            container.addData(data, null, false, false);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }
    /**
     * Adds data to a variable.
     * @param name name of the variable.
     * @param data an array of type compatible with the type of variable.
     * @param relax  relevant for unsigned data types, CDF_UINT1,
     * CDF_UINT2 and CDF_UINT4 only,specifies that values in
     * data array can be interpreted as unsigned.
     *@see
     *  #addData(String name, Object data, int[] recordRange, boolean relax)
     * for more details.
     */
    public void addData(String name, Object data, boolean relax) throws
        CDFException.WriterError {
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        try {
            container.addData(data, null, false, relax);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }

    /**
     * Adds data for a specified record range to a variable.
     * @param name name of the variable.
     * @param data an array of type compatible with the type of variable, or
     * a ByteOrder.LITTLE_ENDIAN ByteBuffer containing data to be added. 
     * @param recordRange int[2] containing record range. If data is an
     * array, or variable is to be saved uncompressed, recordRange may be null,
     * in which case range is assumed to be follow last record added.
     *@see
     *  #addData(String name, Object data, int[] recordRange, boolean relax)
     */
    public void addData(String name, Object data, int[] recordRange) throws
        CDFException.WriterError {
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        try {
            container.addData(data, recordRange, false, false);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }
    /**
     * Adds data for a specified record range to a variable.
     * @param name name of the variable.
     * @param data an array of type compatible with the type of variable, or
     * a ByteOrder.LITTLE_ENDIAN ByteBuffer containing data to be added. 
     * @param recordRange int[2] containing record range. If data is an
     * array, or variable is to be saved uncompressed, recordRange may be null,
     * in which case range is assumed to be follow last record added.
     * @param relax  relevant for unsigned data types, CDF_UINT1,
     * CDF_UINT2 and CDF_UINT4 only,specifies that values in
     * data array can be interpreted as unsigned.
     * <table summary="">
     * <tr><td>CDF Type of Variable</td><td>Type of Array</td></tr>
     * <tr><td>INT8, TT2000, UINT4</td><td>long</td></tr>
     * <tr><td>UINT4</td><td>int, if relax = true, long otherwise</td></tr>
     * <tr><td>DOUBLE, EPOCH, EPOCH16</td><td>double</td></tr>
     * <tr><td>FLOAT</td><td>float or double</td></tr>
     * <tr><td>INT4</td><td>int</td></tr>
     * <tr><td>UINT2</td><td>short, if relax = true, int otherwise</td></tr>
     * <tr><td>INT2</td><td>short</td></tr>
     * <tr><td>UINT1</td><td>byte, if relax = true, short otherwise</td></tr>
     * <tr><td>INT1</td><td>byte</td></tr>
     * <tr><td>CHAR</td><td>String</td></tr>
     * </table>
     * data must contain an integral number of points. If data is an array,
     * rank of the array must be 1 greater than the rank of the 
     * variable, and the dimensions after the first must match the 
     * variable dimensions in number and order.
     * If data is a ByteBuffer, contents must conform to the row majority
     * of this GenericWriter. If the variable is to be stored as compressed,
     * then the buffer should contain compressed data, and record range must
     * be specified.
     */

    public void addData(String name, Object data, int[] recordRange,
        boolean relax) throws CDFException.WriterError {
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        try {
            container.addData(data, recordRange, false, relax);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }

    void addBuffer(String name, VariableDataBuffer data) throws
        CDFException.WriterError {
        DataContainer container = dataContainers.get(name);
        if (container == null) {
            throw new CDFException.WriterError("Variable " + name +
            " is not defined.");
        }
        try {
            container.addData(data.getBuffer(), 
            new int[] {data.getFirstRecord(), data.getLastRecord()},
            false, false);
        } catch (Throwable th) {
            throw new CDFException.WriterError(th.getMessage());
        }
    }
    boolean needDigest = false;

    /**
     * Prescribes whether an MD5 digest is to be included in
     * the output file.
     */
    public void setMD5Needed(boolean need) {needDigest = need;}

    long getSize() {
        long size = cdr.getSize();
        size += gdr.getSize();
        Set<String> atset = attributes.keySet();
        Iterator<String> ait = atset.iterator();
        while (ait.hasNext()) {
            ADR adr = attributes.get(ait.next());
            size += adr.getSize();
        }
        Set<String> ateset = attributeEntries.keySet();
        Iterator<String> aeit = ateset.iterator();
        while (aeit.hasNext()) {
            Vector<AEDR> vec = attributeEntries.get(aeit.next());
            for (int i = 0; i < vec.size(); i++) {
                size += vec.get(i).getSize();
            }
        }
        Set<String> dcset = dataContainers.keySet();
        Iterator<String> dcit = dcset.iterator();
        boolean first = true;
        DataContainer lastContainer = null;
        while (dcit.hasNext()) {
            DataContainer dc = dataContainers.get(dcit.next());
            dc.position = size;
            if (first) {
                gdr.setZVDRHead(size);
                first = false;
            } else {
                lastContainer.getVDR().setVDRNext(dc.position);
            }
            lastContainer = dc;
            size += dc.getSize();
        }
        return size;
    }

    /**
     * Writes CDF to a file.
     */
    public void write(String fname) throws IOException {
        Vector<AEDR> vec = attributeEntries.get("cdfj_source");
        if (vec != null) {
            if (new String(vec.get(0).values).equals(fname)) {
                System.out.println("overwriting " + fname);
                write(fname, true);
                return;
            }
        }
        write(fname, false);
    }

    public boolean write(String fname, boolean overwrite) throws IOException {
        if (lastLeapSecondId != -1) {
            gdr.setLastLeapSecondId(lastLeapSecondId); 
        }
        long len = getSize();
        if (needDigest) len += 32;
        RandomAccessFile raf = null;
        FileChannel channel = null;
        if (len > Integer.MAX_VALUE) {
            raf = new RandomAccessFile(new File(fname), "rw");
            channel = raf.getChannel();
            write(channel, len);
            raf.close();
            return true;
        }
        ByteBuffer obuf;
        if (isWindows()) {
            obuf = ByteBuffer.allocate((int)len);
        } else {
            if (overwrite) {
                obuf = ByteBuffer.allocateDirect((int)len);
            } else {
                raf = new RandomAccessFile(new File(fname), "rw");
                channel = raf.getChannel();
                obuf = channel.map(FileChannel.MapMode.READ_WRITE, 0l, len);
            }
        }
        cdr.setRowMajority(rowMajority);
        cdr.setMD5Needed(needDigest);
        obuf.put(cdr.get());
        // need gdrbuf for insertion of pointers later
        gdr.position = obuf.position();
        obuf.position((int)(gdr.position + gdr.getSize()));
        // assemble attributes
        Set<String> atset = attributes.keySet();
        Iterator<String> ait = atset.iterator();
        boolean first = true;
        ADR lastADR = null;
        while (ait.hasNext()) {
            ADR adr = attributes.get(ait.next());
            String name = adr.name;
            //if (adr.scope != 1) continue;
            adr.position = obuf.position();
            obuf.position((int)(adr.position + adr.getSize()));
            Vector<AEDR> vec = attributeEntries.get(name);
            for (int i = 0; i < vec.size(); i++) {
                AEDR ae = vec.get(i);
                ae.position = obuf.position();
                if (i == 0) {
                    if (adr.scope == 1) {
                        adr.setAgrEDRHead(ae.position);
                    } else {
                        adr.setAzEDRHead(ae.position);
                    }
                } else {
                    vec.get(i - 1).setAEDRNext(ae.position);
                }
                obuf.position(obuf.position() + ae.getSize());
            }
            if (first) {
                gdr.setADRHead(adr.position);
                first = false;
            } else {
                if (lastADR != null) lastADR.setADRNext(adr.position);
            }
            lastADR = adr;
        }
        //obuf.limit(obuf.position());

        // write attributes
        ait = atset.iterator();
        while (ait.hasNext()) {
            ADR adr = attributes.get(ait.next());
            String name = adr.name;
            obuf.position((int)adr.position);
            obuf.put(adr.get());
            Vector<AEDR> vec = attributeEntries.get(name);
            for (int i = 0; i < vec.size(); i++) {
                AEDR ae = vec.get(i);
                obuf.position((int)ae.position);
                obuf.put(ae.get());
            }
        }
        Set<String> dcset = dataContainers.keySet();
        Iterator<String> dcit = dcset.iterator();
        ByteBuffer cbuf = obuf;
        while (dcit.hasNext()) {
            DataContainer dc = dataContainers.get(dcit.next());
            cbuf = dc.update(cbuf);
        }
        obuf.position((int)gdr.position);
        gdr.setEof(obuf.limit());
        gdr.setNumAttr(attributes.size());
        gdr.setNzVars(dataContainers.size());
        obuf.put(gdr.get());
        ByteBuffer digest = null;
        if (needDigest) {
            obuf.position(0);
            digest = getDigest(obuf);
        }
        if (digest != null) cbuf.put(digest);
        if (isWindows()) {
            writeWin(fname, obuf);
        } else {
            if (overwrite) {
                raf = new RandomAccessFile(new File(fname), "rw");
                channel = raf.getChannel();
                obuf.position(0);
                channel.write(obuf);
            }
            channel.force(true);
            raf.close();
        }
        return true;
    }
    ByteBuffer getDigest(ByteBuffer obuf) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception nsa) {
            nsa.printStackTrace();
            return null;
        }
        int pos = obuf.position();
        byte[] ba = new byte[1024*1024];
        while (obuf.remaining() > 0) {
            int csize = obuf.remaining();
            if (csize > ba.length) csize = ba.length;
            obuf.get(ba, 0, csize);
            md.update(ba, 0, csize);
        }
        obuf.position(pos);
        return ByteBuffer.wrap(md.digest());
    }

    void dispatch(String name, Object value) throws CDFException.WriterError {
        Class<?> cl = value.getClass();
        if (cl == String.class) {
            addData(name, new String[]{(String)value});
            return;
        }
        Number num = (Number)value;
        if (cl == Byte.class) {
            addData(name, new byte[]{num.byteValue()});
            return;
        }
        if (cl == Short.class) {
            addData(name, new short[]{num.shortValue()});
            return;
        }
        if (cl == Integer.class) {
            addData(name, new int[]{num.intValue()});
            return;
        }
        if (cl == Double.class) {
            addData(name, new double[]{num.doubleValue()});
            return;
        }
        if (cl == Float.class) {
            addData(name, new float[]{num.floatValue()});
            return;
        }
        if (cl == Long.class) {
            addData(name, new long[]{num.longValue()});
            return;
        }
        throw new CDFException.WriterError("Unrecognized type " + cl);
    }
    /**
     * Sets the last Leap Second Id.
     * This value is intended to allow applications that read the
     * created CDF to validate TT2000 times in the CDF.
     * @param n integer = year*10000 + month*100 + day, where year
     * month and day refer to the day following the leap second.
     * A 0 value for n asserts that applications accept the
     * validity of TT2000 times. n = -1 implies lastLeapSecondId=20120701,
     * which is the default for CDF versions prior to 3.6.
     */
    public void setLastLeapSecondId(int n) {
        lastLeapSecondId = n;
    }
    void write(FileChannel channel, long len) throws IOException {
        cdr.setRowMajority(rowMajority);
        cdr.setMD5Needed(needDigest);
        channel.write(cdr.get());
        // need gdrbuf for insertion of pointers later
        gdr.position = channel.position();
        channel.position(gdr.position + gdr.getSize());
        // assemble attributes
        Set<String> atset = attributes.keySet();
        Iterator<String> ait = atset.iterator();
        boolean first = true;
        ADR lastADR = null;
        while (ait.hasNext()) {
            ADR adr = attributes.get(ait.next());
            String name = adr.name;
            //if (adr.scope != 1) continue;
            adr.position = channel.position();
            channel.position(adr.position + adr.getSize());
            Vector<AEDR> vec = attributeEntries.get(name);
            for (int i = 0; i < vec.size(); i++) {
                AEDR ae = vec.get(i);
                ae.position = channel.position();
                if (i == 0) {
                    if (adr.scope == 1) {
                        adr.setAgrEDRHead(ae.position);
                    } else {
                        adr.setAzEDRHead(ae.position);
                    }
                } else {
                    vec.get(i - 1).setAEDRNext(ae.position);
                }
                channel.position(channel.position() + ae.getSize());
            }
            if (first) {
                gdr.setADRHead((long)adr.position);
                first = false;
            } else {
                if (lastADR != null) lastADR.setADRNext(adr.position);
            }
            lastADR = adr;
        }

        // write attributes
        ait = atset.iterator();
        while (ait.hasNext()) {
            ADR adr = attributes.get(ait.next());
            String name = adr.name;
            channel.position(adr.position);
            channel.write(adr.get());
            Vector<AEDR> vec = attributeEntries.get(name);
            for (int i = 0; i < vec.size(); i++) {
                AEDR ae = vec.get(i);
                channel.position(ae.position);
                channel.write(ae.get());
            }
        }
        Set<String> dcset = dataContainers.keySet();
        Iterator<String> dcit = dcset.iterator();
        while (dcit.hasNext()) {
            DataContainer dc = dataContainers.get(dcit.next());
            dc.update(channel);
        }
        channel.position(gdr.position);
        gdr.setEof(channel.size());
        gdr.setNumAttr(attributes.size());
        gdr.setNzVars(dataContainers.size());
        channel.write(gdr.get());
        channel.position(channel.size());
        if (needDigest) {
            getDigest(channel);
        }
    }
    void getDigest(FileChannel channel) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception nsa) {
            nsa.printStackTrace();
            return;
        }
        //System.out.println(channel.position());
        byte[] ba = new byte[1024*1024];
        ByteBuffer buf = ByteBuffer.wrap(ba);
        long remaining = channel.size();
        channel.position(0);
        while (remaining > 0) {
            long csize = remaining;
            if (csize > ba.length) csize = ba.length;
            buf.position(0);
            buf.limit((int)csize);
            int trans = channel.read(buf);
            if (trans == -1) throw new IOException("Unexpected end of data");
            md.update(ba, 0, trans);
            remaining -= trans;
        }
        //System.out.println(channel.position());
        //channel.position(channel.size());
        channel.write(ByteBuffer.wrap(md.digest()));
        //System.out.println(channel.size());
    }
    void writeWin(String fname, ByteBuffer buf) throws IOException {
        FileOutputStream fos = new FileOutputStream(fname);
        byte[] ba = buf.array();
        fos.write(ba);
        fos.close();
        //System.out.println("finished writing to " + fname);
    }
/*
        byte[] ba = new byte[chunkSize];
        int toWrite = buf.remaining();
        int written = 0;
        while (written < toWrite) {
            int n = toWrite - written;
            if (n > chunkSize) n = chunkSize;
            buf.get(ba, 0, n);
            fos.write(ba, 0, n);
            written += n;
        }
    }
    static int chunkSize = 64*1024;
*/
    boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase().startsWith("win")); 
    }
}
