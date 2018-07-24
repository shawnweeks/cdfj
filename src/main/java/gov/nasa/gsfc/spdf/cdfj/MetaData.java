package gov.nasa.gsfc.spdf.cdfj;
import java.lang.reflect.*;
import java.util.*;
import java.nio.*;
/**
 * Abstract base class for GenericReader.
 * Provides methods to access CDF properties, global attributes, variable
 * properties and attributes.
 */
public abstract class MetaData {
    CDFImpl thisCDF;
    /**
     * Returns ByteOrder.LITTLE_ENDIAN, or ByteOrder.BIG_ENDIAN depending
     * the CDF encoding
     */
    public final ByteOrder getByteOrder() {
        return thisCDF.getByteOrder();
    }

    /**
     * Returns whether the arrays are stored in row major order in the source
     */
    public final boolean rowMajority() {
        return thisCDF.rowMajority();
    }

    /**
     * Returns names of variables in the CDF
     */
    public final String [] getVariableNames() {
        return thisCDF.getVariableNames();
    }

    /**
     * returns variable names of a given VAR_TYPE in a String[]
     */
    public final String [] getVariableNames(String type) {
        return thisCDF.getVariableNames(type);
    }

    /**
     * Returns names of global attributes.
     */
    public final String [] globalAttributeNames() {
        return thisCDF.globalAttributeNames();
    }

    /**
     * Returns names of variable attributes.
     */
    public final String [] variableAttributeNames(String name) {
        return thisCDF.variableAttributeNames(name);
    }

    /**
     * Returns value of the first entry for the named global attribute.
     * <p>
     * For a  character string attribute, a String[] is returned
     * For a  numeric attribute, a long[] is returned for long type;
     * double[] is returned for all other numeric types.
     * <p>
     * This method is deprecated. Use {@link #getGlobalAttribute(String atr)
     * getGlobalAttribute(String atr)} method to extract all entries.
     */
    public final Object getAttribute( String atr) {
        return thisCDF.getAttribute(atr);
    }

    /**
     * Returns number of entries for the named global attribute.
     */
    public final int globalAttributeEntryCount(String atr) throws
        CDFException.ReaderError {
        return getGlobalAttribute(atr).getEntryCount();
    }

    /**
     * Returns value of the named attribute for specified variable.
     * For a  character string attribute, a Vector of String is returned.
     * For a  numeric attribute, a Vector of size 1 is returned. The 
     * single element of the Vector is a long[] if attribute's type is long;
     * For all other numeric types, the element is a double[].
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final Object getAttribute( String varName,
        String aname) throws CDFException.ReaderError {
        return thisCDF.getAttribute(varName, aname);
    }

    /**
     * Returns list of {@link AttributeEntry AttributeEntry} objects for
     * the named attribute for the named variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final Vector<AttributeEntry> getAttributeEntries(
         String varName, String aname) throws CDFException.ReaderError {
        return thisCDF.getAttributeEntries(varName, aname);
    }

    /**
     * Returns list of {@link AttributeEntry AttributeEntry} objects for the
     * named global attribute.
     */
    public final Vector<AttributeEntry> getAttributeEntries(
         String aname) throws CDFException.ReaderError {
        try {
            return thisCDF.getAttributeEntries(aname);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns {@link GlobalAttribute GlobalAttribute} object for
     * the named global attribute.
     */
    public final GlobalAttribute getGlobalAttribute( String atr)
        throws CDFException.ReaderError {
        try {
            return thisCDF.getGlobalAttribute(atr);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns an indication of the record varying property of a variable.
     * @return false if variable has a constant value for this CDF.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean recordVariance( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).recordVariance();
    }

    /**
     * Returns whether the values of the variable are represented in a
     * compressed form in the CDF.
     * For variables declared to be compressed, CDF specification allows
     * the values to be stored in uncompressed form if the latter results in
     * a smaller size.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean isCompressed( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).isCompressed();
    }

    /**
     * Returns CDF type of the variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getType( String varName) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getType();
    }

    /**
     * Returns size of a data item for the given variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getDataItemSize( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getDataItemSize();
    }

    /**
     * Returns given variable's number property.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getNumber( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getNumber();
    }

    /**
     * Returns given variable's 'number of elements' property.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getNumberOfElements( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getNumberOfElements();
    }

    /**
     * Returns 'number of values' property of the given variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getNumberOfValues( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getNumberOfValues();
    }

    /**
     * Returns 'pad value' property of the given variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final Object getPadValue( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getPadValue();
    }

    /**
     * Returns 'pad value' property of the given variable subject to the given
     * precision  preservation constraint.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final Object getPadValue( String varName,
        boolean preservePrecision) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        return var.getPadValue(preservePrecision);
    }

    /**
     * Returns dimensions the given variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int[] getDimensions( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getDimensions();
    }

    /**
     * Returns 'varys' property of the given variable.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean[] getVarys( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getVarys();
    }

    /**
     * Returns effective rank of this variable.
     * Dimensions for which dimVarys is false do not count.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int getEffectiveRank( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getEffectiveRank();
    }

    /**
     * Shows whether one or more records (in the range returned by
     * getRecordRange()) are missing. 
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean isMissingRecords( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).isMissingRecords();
    }

    /**
     * Returns record range for this variable
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int[] getRecordRange( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getRecordRange();
    }

    /**
     * returns whether conversion of this variable to type specified by
     * cl is supported while preserving precision.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean isCompatible( String varName, Class cl) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        try {
            return thisCDF.getVariable(varName).isCompatible(cl);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns whether conversion of this variable to type specified by
     * cl is supported under the given precision preserving constraint.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean isCompatible( String varName,
        Class cl, boolean preserve) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        return var.isCompatible(cl, preserve);
    }

    /**
     * Return whether the missing record should be assigned the last 
     * seen value. If none has been seen, pad value is assigned.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean missingRecordValueIsPrevious(
        String varName) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        return var.missingRecordValueIsPrevious();
    }

    /**
     * Return whether the missing record should be assigned the pad 
     * value.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean missingRecordValueIsPad(
        String varName) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        return var.missingRecordValueIsPad();
    } 

    /**
     * Return element count for this variable's dimensions.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final Vector<Integer> getElementCount(
        String varName) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getElementCount();
    } 

    /**
     * Returns effective dimensions of the given variable.
     * Dimensions for which dimVarys is false are ignored.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final int[] getEffectiveDimensions(
        String varName) throws CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        return var.getEffectiveDimensions();
    } 

    /**
     * Returns whether the given variable represents time.
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final boolean isTimeType( String varName) throws
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError(
            "CDF does not hava a variable named " + varName);
        Variable var = thisCDF.getVariable(varName);
        int type = var.getType();
        boolean isTimeType = (CDFTimeType.EPOCH.getValue() == type);
        isTimeType |= (CDFTimeType.EPOCH16.getValue() == type);
        isTimeType |= (CDFTimeType.TT2000.getValue() == type);
        return isTimeType;
    }

    /**
     * Returns whether there is a variable with the given name.
     */
    public final boolean existsVariable(String varName) {
        if (varName == null) return false;
        return (thisCDF.getVariable(varName) != null);
    }

    public abstract String userTimeVariableName(String varName) throws
        CDFException.ReaderError;

    /**
     * Returns the name of the time variable for the given variable.
     * @param    varName   variable name
     * @return  String
     * @throws   CDFException.ReaderError  if variable does not exist
     */
    public final String getTimeVariableName(String varName) throws Throwable {
        if (!existsVariable(varName)) throw new Throwable (
            "CDF does not hava a variable named " + varName);
        String tname = userTimeVariableName(varName);
        if (tname != null) return tname;
        // assume istp convention
        Variable var = thisCDF.getVariable(varName);
        String vname = var.getName();
        Vector v = (Vector)thisCDF.getAttribute(vname, "DEPEND_0");
        if (v.size() > 0) tname = (String)v.elementAt(0);
        if (tname == null) {
            if (!vname.equals("Epoch")) {
                if (thisCDF.getVariable("Epoch") != null) {
                    tname = "Epoch";
                    System.out.println("Variable " + vname + " has no DEPEND_0"+
                    " attribute. Variable named Epoch " +
                    "assumed to be the right time variable");
                } else {
                    throw new Throwable("Time variable not found for " + vname);
                }
            } else {
                throw new Throwable("Variable named Epoch has no DEPEND_0 " +
                "attribute.");
            }
        }
        return tname;
    }

    /**
     * Identifies the leap second table used in creating this CDF.
     * Returns the id of the last leap second in the leap second table.
     * Leap second id is an integer = year*10000 + month*100 + day, where
     * year, month and day refer to the day following the leap second. Until
     * 2015, leap second has been added at the end of December, or June. Thus
     * leapSecondId is either (10000*year + 101), or (10000*year + 701).
     */
    public final int getLastLeapSecondId() {
        return thisCDF.lastLeapSecondId;
    }

    /**
     * Returns the blocking factor used to compress this variable.
     * See the CDF User's Guide for details.
     */
    public final int getBlockingFactor(String varName) throws 
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError (
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).getBlockingFactor();
    }
    /**
     * Returns whether a variable of type r-variable..
     * See the CDF User's Guide for details.
     */
    public final boolean isTypeR(String varName) throws 
        CDFException.ReaderError {
        if (!existsVariable(varName)) throw new CDFException.ReaderError (
            "CDF does not hava a variable named " + varName);
        return thisCDF.getVariable(varName).isTypeR();
    }
}
