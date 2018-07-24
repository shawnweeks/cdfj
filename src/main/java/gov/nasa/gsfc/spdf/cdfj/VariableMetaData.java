package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
import java.nio.*;
/**
 * Interface that defines methods for getting  properties of
 * a CDF variable.
 */
public interface VariableMetaData {
    /**
     * Determines whether the value of this variable is the same at
     * all time points. 
     * returns true if value may change, false otherwise
     */
    public boolean recordVariance();

    /**
     * Determines whether the value of this variable is represented as
     * a compressed byte sequence in the CDF.
     */
    public boolean isCompressed();

    /**
     * Determines whether the value of this variable is presented in
     * a row-major order in the CDF.
     */
    public boolean rowMajority();

    /**
     * Gets the name of this of this variable
     */
    public String getName();

    /**
     * Gets the type of values of the variable.
     * Supported types are defined in the CDF Internal Format Description
     */
    public int getType();

    /**
     * Gets the size of an item (defined as number of bytes needed to
     * represent the value of this variable at a point).
     */
    public int getDataItemSize();

    /**
     * Gets the sequence number of the variable inside the CDF. 
     */
    public int getNumber();

    /**
     * Gets the number of elements (of type returned by getType()).
     */
    public int getNumberOfElements();

    /**
     * Gets the number of values (size of time series)
     */
    public int getNumberOfValues();

    /**
     * Gets an object that represents a padded instance.
     * For variable of type 'string', a String is returned;
     * For numeric data, a double[] is returned. If the variable type is
     * long, a loss of precision may occur. 
     */
    public Object getPadValue();

    /**
     * Gets an object that represents a padded instance for a variable of
     * numeric type.
     * A double[] is returned, unless the variable type is long and
     * preservePrecision is set to true;
     */
    public Object getPadValue(boolean preservePrecision);

    /**
     * Gets the dimensions.
     */
    public int[] getDimensions();

    /**
     * Gets the dimensional variance. This determines the effective
     * dimensionality of values of the variable.
     */
    public boolean[] getVarys();

    /**
     * Gets a list of regions that contain data for the variable.
     * Each element of the vector describes a region as an int[3] array.
     * Array elements are: record number of first point
     * in the region, record number of last point in the
     * region, and offset of the start of region.
     */
    public VariableDataLocator getLocator();

    /**
     * Gets an array of VariableDataBuffer objects that provide location of
     * data for this variable if this variable is not compressed. This method
     * throws a Throwable if invoked for a compressed variable.
     * getBuffer method of VariableDataBuffer object returns a read only 
     * ByteBuffer that contains data for this variable for a range of
     * records. getFirstRecord() and getLastRecord() define the
     * range of records.
     */
    public VariableDataBuffer[] getDataBuffers() throws Throwable;
    public VariableDataBuffer[] getDataBuffers(boolean raw) throws Throwable;

    /**
     * Returns effective rank of this variable.
     * Dimensions for which dimVarys is false do not count.
     */
    public int getEffectiveRank();

    /**
     * Returns ByteBuffer containing uncompressed values converted to
     * a stream of numbers of the type specified by 'type' using the
     * specified byte ordering (specified by bo) for the specified range
     * of records. Original  ordering of values (row majority) is preserved.
     * recordRange[0] specifies the first record, and recordRange[1] the last
     * record. If 'preserve' is true, a Throwable is thrown if the conversion
     * to specified type will result in loss of precision. If 'preserve' is
     * false, compatible conversions will be made even if it results in loss
     * of precision. 
     */
    public ByteBuffer getBuffer(Class type, int[] recordRange, boolean preserve,
        ByteOrder bo) throws Throwable;

    /**
     * Shows whether one or more records (in the range returned by
     * getRecordRange()) are missing. 
     */
    public boolean isMissingRecords();

    /**
     * Returns record range for this variable
     */
    public int[] getRecordRange();

    /**
     * returns whether conversion of this variable to type specified by
     * cl is supported while preserving precision.
     * equivalent to isCompatible(Class cl, true)
     */
    public boolean isCompatible(Class cl);

    /**
     * returns whether conversion of this variable to type specified by
     * cl is supported under the given precision preserving constraint.
     */
    public boolean isCompatible(Class cl, boolean preserve);

    /**
     * Return whether the missing record should be assigned the last 
     * seen value. If none has been seen, pad value is assigned.
     */
    public boolean missingRecordValueIsPrevious();

    /**
     * Return whether the missing record should be assigned the pad 
     * value.
     */
    public boolean missingRecordValueIsPad();

    /**
     * Return element count for this variable's dimensions.
     */
    public Vector getElementCount();

    /**
     * Returns effective dimensions
     */
    public int[] getEffectiveDimensions();
    public int getBlockingFactor();
    public boolean isTypeR();
}
