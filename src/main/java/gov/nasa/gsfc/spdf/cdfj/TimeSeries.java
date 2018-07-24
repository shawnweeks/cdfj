package gov.nasa.gsfc.spdf.cdfj;
/**
 * Time series specification. TimeSeries objects are returned by a subset of
 * CDFReader methods, such as those that specify a TimeInstantModel.
 */
public interface TimeSeries {
    /**
     * Returns times according to the
     * {@link TimeInstantModel time instant model}
     * returned by {@link #getTimeInstantModel() getTimeInstantModel()}.
     * @see CDFReader#timeModelInstance()
     */
    public double[] getTimes() throws CDFException.ReaderError;

    /**
     * Returns values of the variable at times returned by getTimes().
     * @return <ul>
     *   <li>double[n] for scalar variable</li>
     *   <li>double[n][n1] for  1-d variable of dimension n1</li>
     *   <li>double[n][n1][n2] for  2-d variable of dimension n1,n2</li>
     *   <li>double[n][n1][n2][n3] for  3-d variable of dimension n1,n2,n3</li>
     * </ul> where n is the length of the array returned by
     * {@link #getTimes() getTimes()}
     */
    public Object getValues() throws CDFException.ReaderError;

    /**
     * Returns time instant model used to derive times returned
     * by {@link #getTimes() getTimes()}.
     */
    public TimeInstantModel getTimeInstantModel();
}
