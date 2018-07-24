package gov.nasa.gsfc.spdf.cdfj;
/**
 * Time series specification for one-dimensional representation of values.
 * TimeSeriesOneD objects are returned by the getTimeSeriesOneD method of
 * CDFReader.
 */
public interface TimeSeriesOneD {
    public boolean oned = true;
    /**
     * Returns times according to the
     * {@link TimeInstantModel time instant model}
     * returned by {@link #getTimeInstantModel() getTimeInstantModel()}.
     * @see CDFReader#timeModelInstance()
     */
    public double[] getTimes() throws CDFException.ReaderError;

    /**
     * Returns one dimensional representation of the values of the variable
     * at times returned by getTimes().
     * <p>
     * Returned array represents multi dimensional arrays in a manner
     * determined by the value returned by {@link #isColumnMajor()
     * isColumnMajor()} method.
     * </p>
     */
    public double[] getValues() throws CDFException.ReaderError;

    /**
     * Returns time instant model used to derive times returned
     * by {@link #getTimes() getTimes()}.
     */
    public TimeInstantModel getTimeInstantModel();

    /**
     * Returns whether the array returned by getValues() is to be
     * interpreted as having the first index of variable's dimension varying
     * the fastest, as in IDL
     */
    public boolean isColumnMajor();
}
