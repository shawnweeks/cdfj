package gov.nasa.gsfc.spdf.cdfj;
import java.nio.ByteBuffer;
/**
 * Time Variable.
 */
public interface TimeVariableX extends TimeVariable {
    /**
     * Returns relative times for the specified time range using the given
     * {@link TimeInstantModel time instant model}.
     * <p>
     * @param    timeRange  relative time range 
     */
    public double[] getTimes(double[] timeRange, TimeInstantModel tspec) throws
        Throwable;

    public int[] getRecordRange(double[] timeRange) throws Throwable;
    public int [] getRecordRange(int[] startTime, int[] stopTime,
            TimeInstantModel ts) throws Throwable;
    public ByteBuffer getRawBuffer();
    public TimePrecision getPrecision();
}
