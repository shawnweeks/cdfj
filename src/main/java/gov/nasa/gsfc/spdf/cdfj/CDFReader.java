package gov.nasa.gsfc.spdf.cdfj;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;
/**
 * CDFReader extends GenericReader with access methods for time series
 * variables. Time series methods of this class do not require a detailed
 * knowledge of the internal structure of CDF.
 */
public class CDFReader extends GenericReader {
    Scalar scalar;
    CDFVector vector;
    public CDFReader() {
    }
    /**
     * Constructs a reader for the given CDF file.
     */
    public CDFReader(String cdfFile) throws CDFException.ReaderError {
        super(cdfFile);
        scalar = new Scalar();
        scalar.rdr = this;
        vector = new CDFVector();
        vector.rdr = this;
    }

    /**
     * Constructs a reader for the given URL for CDF file.
     */
    public CDFReader(URL url) throws CDFException.ReaderError {
        super(url);
        scalar = new Scalar();
        scalar.rdr = this;
        vector = new CDFVector();
        vector.rdr = this;
    }


    /**
     * Returns {@link TimeSeries TimeSeries} of the specified variable
     * using the default {@link TimeInstantModel time instant model}.
     */
    public TimeSeries getTimeSeries(String varName) throws
        CDFException.ReaderError {
        return getTimeSeries(varName, null, timeModelInstance());
    }

    /**
     * Returns {@link TimeSeries TimeSeries} of the specified variable
     * using the specified {@link TimeInstantModel time instant model}.
     * @param    varName   variable name
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     */
    public TimeSeries getTimeSeries(String varName, TimeInstantModel tspec)
        throws CDFException.ReaderError {
        TimeInstantModel _tspec = (tspec == null)?timeModelInstance():tspec;
        return getTimeSeries(varName, null, _tspec);
    }

    /**
     * Returns {@link TimeSeries TimeSeries} of the specified variable
     * in the specified time range using the default
     * {@link TimeInstantModel time instant model}.
     * @param    varName   variable name
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     */
    public TimeSeries getTimeSeries(String varName, int[] startTime,
        int[] stopTime) throws CDFException.ReaderError {
        return getTimeSeries(varName, startTime, stopTime, null);
    }

    private TimeSeries getTimeSeries(String varName, double[] timeRange,
        TimeInstantModel tspec) throws CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        TimeSeriesX ts = null;
        try {
            ts = new TSExtractor.GeneralTimeSeriesX(this, var,
            false, timeRange, tspec, false, true);
            return new TimeSeriesImpl(ts);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns {@link TimeSeries TimeSeries} of the specified variable in
     * the specified time range using the given
     * {@link TimeInstantModel time instant model}.
     * @param    varName   variable name
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @return   {@link TimeSeries time series}
     */
    public TimeSeries getTimeSeries(String varName, int[] startTime,
        int[] stopTime, TimeInstantModel tspec) throws
        CDFException.ReaderError {
        TimeVariable tv = null;
        try {
            tv = TimeVariableFactory.getTimeVariable(this, varName);
            TimeInstantModel _tspec = tspec;
            if (_tspec == null) _tspec = timeModelInstance();
            if (!tv.canSupportPrecision(_tspec.getOffsetUnits())) {
            throw new CDFException.ReaderError(varName +
                " has lower time precision than " + "requested.");
            }
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(this, trange, varName,
                startTime, stopTime);
            return getTimeSeries(varName, tr, tspec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    private TimeSeriesOneD getTimeSeries(String varName, double[] timeRange,
        TimeInstantModel tspec, boolean columnMajor) throws
        CDFException.ReaderError {
        Variable var = thisCDF.getVariable(varName);
        TimeSeriesX ts = null;
        try {
            ts = new TSExtractor.GeneralTimeSeriesX(this, var,
            false, timeRange, tspec, true, columnMajor);
            return new TimeSeriesOneDImpl(ts);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns {@link TimeSeriesOneD time series} of the specified variable
     * in the specified time range using the given
     * {@link TimeInstantModel time instant model}.
     * @param    varName   variable name
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @param    columnMajor specifies whether the first index of the
     * variable dimension varies the fastest, i.e. IDL like.
     * @return   {@link TimeSeriesOneD time series}
     */
    public TimeSeriesOneD getTimeSeriesOneD(String varName, int[] startTime,
        int[] stopTime, TimeInstantModel tspec, boolean columnMajor) throws
        CDFException.ReaderError {
        try {
            TimeVariable tv =
                TimeVariableFactory.getTimeVariable(this, varName);
            TimeInstantModel _tspec = tspec;
            if (_tspec == null) _tspec = timeModelInstance();
            if (!tv.canSupportPrecision(_tspec.getOffsetUnits())) {
                System.out.println("cannot support");
                throw new Throwable(varName +
                " has lower time precision than " + "requested.");
            }
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(this, trange, varName,
                startTime, stopTime);
            return getTimeSeries(varName, tr, _tspec, columnMajor);
        } catch (Throwable th) {
                System.out.println(th.getMessage());
            th.printStackTrace();
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns available time range using default
     * {@link TimeInstantModel time instant model}.
     * @param    varName   variable name
     * @return   double[2] 0th element is the first available offset time; 
     *                     1st element is the last available offset time; 
     */
    public double [] getAvailableTimeRange(String varName) throws
        CDFException.ReaderError {
        try {
            TimeVariable tv =
                TimeVariableFactory.getTimeVariable(this, varName);
            double[] times = tv.getTimes();
            return new double[] {times[0], times[times.length - 1]};
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
    boolean overlaps(double[] t) {
        if (t[0] == Double.MIN_VALUE) return false;
        if (t[0] == Double.MAX_VALUE) return false;
        return true;
    }

    /**
     * Returns first available time for a variable.
     * Returned time has millisecond precision.
     * @return  int[7] containing year, month, day, hour, minute, second and
     * millisecond, or null.
     * @throws CDFException.ReaderError if not a valid variable name.
     */
    public int[] firstAvailableTime(String varName)
        throws CDFException.ReaderError {
        return firstAvailableTime(varName, null);
    }

    /**
     * Returns first available time which is not before the given time for a
     * variable.
     * Returned time has millisecond precision.
     * @param start a 3 to 7 element int[], containing year,
     *  month (January is 1), day, hour, minute, second and millisecond.
     * @return  int[7] containing year, month, day, hour, minute, second and
     * millisecond, or null.
     * @throws CDFException.ReaderError if not a valid variable name.
     */
    public int[] firstAvailableTime(String varName, int[] start)
        throws CDFException.ReaderError {
        try {
            TimeVariable tv =
                TimeVariableFactory.getTimeVariable(this, varName);
            double[] times = tv.getTimes();
            double[] trange = new double[] {times[0], times[times.length - 1]};
            double[] tr;
            try {
                tr = TSExtractor.getOverlap(this, trange, varName,
                start, null);
            } catch (Exception ex) {
                return null;
            }
            if (tr[0] != Double.MIN_VALUE) {
                Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                c.setTimeInMillis((long)tr[0]);
                if (tv.isTT2000()) {
                    long l0 = c.getTime().getTime();
                    long l = (long)TimeUtil.getOffset(l0);
                    c.setTimeInMillis((long)tr[0] - l + l0);
                }
                return GMT(c);
            }
            return null;
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
    /**
     * Returns last available time for a variable.
     * Returned time has millisecond precision.
     * @return  int[7] containing year, month, day, hour, minute, second and
     * millisecond, or null.
     * @throws CDFException.ReaderError if not a valid variable name.
     */
    public int[] lastAvailableTime(String varName)
        throws CDFException.ReaderError {
        return lastAvailableTime(varName, null);
    }

    /**
     * Returns last available time which is not later than the given time
     * for a variable.
     * Returned time has millisecond precision.
     * @param stop a 3 to 7 element int[], containing year,
     *  month (January is 1), day, hour, minute, second and millisecond.
     * @return  int[7] containing year, month, day, hour, minute, second and
     * millisecond, or null.
     * @throws CDFException.ReaderError if not a valid variable name.
     */
    public int[] lastAvailableTime(String varName, int[] stop)
        throws CDFException.ReaderError {
        try {
            TimeVariable tv =
                TimeVariableFactory.getTimeVariable(this, varName);
            double[] times = tv.getTimes();
            double[] trange = new double[] {times[0], times[times.length - 1]};
            double[] tr;
            try {
                tr = TSExtractor.getOverlap(this, trange, varName,
                null, stop);
            } catch (Exception ex) {
                return null;
            }
            if (tr[1] != Double.MAX_VALUE) {
                Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                c.setTimeInMillis((long)tr[1]);
                if (tv.isTT2000()) {
                    long l0 = c.getTime().getTime();
                    long l = (long)TimeUtil.getOffset(l0);
                    c.setTimeInMillis((long)tr[1] - l + l0);
                }
                return GMT(c);
            }
            return null;
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
    int[] GMT(Calendar c) {
            return new int[] {c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                 c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY),
                 c.get(Calendar.MINUTE), c.get(Calendar.SECOND),
                 c.get(Calendar.MILLISECOND)};
    }

    /**
     * Returns {@link TimeInstantModel time instant model} with specified base
     * time and default offset units (millisecond) for a variable.
     * @param    varName   variable name
     * @param baseTime a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond.
     */
    public TimeInstantModel timeModelInstance(String varName, int[] baseTime)
        throws CDFException.ReaderError {
        if (baseTime.length < 3) throw new CDFException.ReaderError(
            "incomplete base time " + "definition.");
        try {
            boolean isTT2000 =
                TimeVariableFactory.getTimeVariable(this, varName).isTT2000();
            long l = TSExtractor.getTime(baseTime);
            double msec = (!isTT2000)?(double)l:
                TimeUtil.milliSecondSince1970(l);
            msec += TimeVariableFactory.JANUARY_1_1970_LONG;
            return getTimeInstantModel(msec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns {@link TimeInstantModel time instant model} with specified base
     * time and specified offset units (millisecond) for a variable.
     * @param    varName   variable name
     * @param baseTime a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond.
     */
    public TimeInstantModel timeModelInstance(String varName, int[] baseTime,
        TimePrecision offsetUnits) throws CDFException.ReaderError {
        TimeInstantModel model = timeModelInstance(varName, baseTime);
        model.setOffsetUnits(offsetUnits);
        return model;
    }

    private TimeInstantModel getTimeInstantModel(double msec) {
        TimeInstantModel tspec =
            TimeVariableFactory.getDefaultTimeInstantModel(msec);
        return tspec;
    }

    private double getTime(String varName, int[] time) throws
        CDFException.ReaderError {
        try {
            return TSExtractor.getTime(this, varName, time);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns default {@link TimeInstantModel time instant model}.
     * <p>
     * The base of default TimeInstantModel is January 1,1970 0:0:0
     */
    public static TimeInstantModel timeModelInstance() {
        return TimeVariableFactory.getDefaultTimeInstantModel();
    }

    /**
     * Returns {@link TimeInstantModel time instant model} that uses
     * given units for the offset..
     * <p>
     * The base of default TimeInstantModel is January 1,1970 0:0:0
     */
    public static TimeInstantModel timeModelInstance(String offsetUnits) {
        TimeInstantModel tim = TimeVariableFactory.getDefaultTimeInstantModel();
        tim.setOffsetUnits(TimePrecision.getPrecision(offsetUnits));
        return tim;
    }

    class TimeSeriesOneDImpl extends TimeSeriesImpl implements
        TimeSeriesOneD {
        boolean columnMajor;
        TimeSeriesOneDImpl(TimeSeriesX ts) throws
            CDFException.ReaderError {
            super(ts);
            if (!ts.isOneD()) {
                throw new CDFException.ReaderError( "Not 1D timeseries.");
            }
            columnMajor = ts.isColumnMajor();
        }

        public double[] getValues() throws CDFException.ReaderError {
            return (double[])values;
        }

        public double[] getValuesOneD() throws CDFException.ReaderError {
            return (double[])values;
        }

        public boolean isColumnMajor() {return columnMajor;}
    }

    class TimeSeriesImpl implements TimeSeries {
        double[] times;
        Object values;
        TimeInstantModel tspec;
        public TimeSeriesImpl(TimeSeries ts) throws CDFException.ReaderError {
            times = ts.getTimes();
            values = ts.getValues();
            tspec = ts.getTimeInstantModel();
        }
        public TimeInstantModel getTimeInstantModel() {return tspec;}
        public double[] getTimes() throws CDFException.ReaderError {
            return times;
        }
        public Object getValues() throws CDFException.ReaderError {
            return values;
        }
    }

    /**
     * Returns names of variables that the specified  variable depends on.
     * @return String[] 
     */
    public String[] getDependent(String varName) {
        String[] anames = thisCDF.variableAttributeNames(varName);
        if (anames == null) return new String[0];
        Vector dependent = new Vector();
        for (int i = 0; i < anames.length; i++) {
            if (!anames[i].startsWith("DEPEND_")) continue;
            dependent.add(
                ((Vector)thisCDF.getAttribute(varName, anames[i])).get(0));
        }
        String[] sa = new String[dependent.size()];
        dependent.toArray(sa);
        return sa;
    }

    /**
     * Returns the name of the specified index of a multi-dimensional
     * variable
     * @param    varName   variable name
     * @param    index     index whose name is required
     * @return  String
     */
    public String getIndexName(String varName, int index) throws
        CDFException.ReaderError {
        try {
            int[] dim = getDimensions(varName);
            if (dim.length == 0) return null;
            if (index >= dim.length) return null;
            Vector attr = 
               (Vector)getAttribute(varName, "DEPEND_" + (1 + index));
            return (String)attr.get(0);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
    /**
     * Returns the time series of the specified scalar variable, ignoring
     * points whose value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded.
     * </p>
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public double[][] getScalarTimeSeries(String varName) throws
        CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified scalar variable in the
     * specified time range, ignoring points whose value equals
     * fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values.
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then points where the
     * value is equal to fill value are excluded.
     * </p>
     * For numeric variables of dimension other than 0, and for
     * character string variables an  exception is thrown.
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public double[][] getScalarTimeSeries(String varName, int[] startTime,
        int[] stopTime) throws CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName, startTime, stopTime);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series as a {@link TimeSeries TimeSeries}, of the
     * specified scalar variable in the specified time range,  using the given
     * {@link TimeInstantModel time instant model}, ignoring  points whose
     * value  equals fill  value.
     * <p>
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then points where the value is equal to fill value are
     * excluded.
     * </p>
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public TimeSeries getScalarTimeSeries(String varName, int[] startTime,
        int[] stopTime, TimeInstantModel tspec) throws
        CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName, startTime, stopTime, tspec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified scalar variable, optionally
     * ignoring points whose value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public double[][] getScalarTimeSeries(String varName, boolean ignoreFill)
        throws CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName, ignoreFill);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified scalar variable in the
     * specified time range, optionally ignoring points whose value equals
     * fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values.
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then if ignoreFill has the value true, points where the
     * value is equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public double[][] getScalarTimeSeries(String varName, boolean ignoreFill,
        int[] startTime, int[] stopTime) throws CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName, ignoreFill, startTime,
            stopTime);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series as a {@link TimeSeries TimeSeries} of the
     * specified scalar variable in the  specified time range using the given
     * {@link TimeInstantModel time instant model}, optionally ignoring points
     * whose
     * value equals fill value.
     * <p>
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then if ignoreFill has the value true, points where the
     * value is equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a scalar.
     */
    public TimeSeries getScalarTimeSeries(String varName, boolean ignoreFill,
        int[] startTime, int[] stopTime, TimeInstantModel tspec) throws
        CDFException.ReaderError {
        try {
            return scalar.getTimeSeries(varName, ignoreFill, startTime,
            stopTime, tspec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }
/*
    void checkType(String varName) throws Throwable {
        Variable var = thisCDF.getVariable(varName);
        int type = var.getType();
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            throw new Throwable("This method cannot be used for " +
            "variables of type long. Use the get methods for the " +
            "variable " + "and the associated time variable. ");
        }
    }
*/
    class Scalar {
        MetaData rdr;
        double[][] _getTimeSeries(String varName, boolean ignoreFill,
            double[] timeRange) throws Throwable {
            checkType(varName);
            Variable var = thisCDF.getVariable(varName);
            Method method = TSExtractor.getMethod(var, "TimeSeries", 0);
            return (double[][])method.invoke(null, new Object []
                {rdr, var, new Boolean(ignoreFill), timeRange});
        }
        TimeSeries _getTimeSeries(String varName, boolean ignoreFill,
            double[] timeRange, TimeInstantModel tspec) throws Throwable {
            checkType(varName);
            Variable var = thisCDF.getVariable(varName);
            Method method = TSExtractor.getMethod(var, "TimeSeriesObject", 0);
            TimeSeries ts =  (TimeSeries)method.invoke(null, new Object []
                {rdr, var, new Boolean(ignoreFill), timeRange, tspec});
            return new TimeSeriesImpl(ts);
        }
        public double[][] getTimeSeries(String varName) throws Throwable {
            Variable var = thisCDF.getVariable(varName);
            if (var.getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            return _getTimeSeries(varName, true, null);
        }
        public double[][] getTimeSeries(String varName, int[] startTime,
            int[] stopTime) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, true, tr);
        }
        public TimeSeries getTimeSeries(String varName, int[] startTime,
            int[] stopTime, TimeInstantModel tspec) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, true, tr, tspec);
        }
        public double[][] getTimeSeries(String varName, boolean ignoreFill)
            throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            return _getTimeSeries(varName, ignoreFill, null);
        }
        public double[][] getTimeSeries(String varName, boolean ignoreFill,
            int[] startTime, int[] stopTime) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, ignoreFill, tr);
        }
        public TimeSeries getTimeSeries(String varName, boolean ignoreFill,
            int[] startTime, int[] stopTime, TimeInstantModel tspec) throws
            Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 0) throw new
                Throwable(varName + " is not a scalar.");
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, ignoreFill, tr, tspec);
        }
    }

    /**
     * Returns the time series of the specified component of a 1 dimensional
     * variable, ignoring points whose value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded.
     * </p>
     * @throws   CDFException.ReaderError  if variables is non-numeric, or is
     * not a vector.
     */
    public double[][] getVectorTimeSeries(String varName, int component)
        throws CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified component of 1 dimensional
     * variable in the specified time range, ignoring points whose
     * value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded.
     * </p>
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or is
     * not a vector.
     */
    public double[][] getVectorTimeSeries(String varName, int component,
        int[] startTime, int[] stopTime) throws CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component, startTime,
            stopTime);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series as a {@link TimeSeries TimeSeries}, of the
     * specified component of a vector variable in the specified time range
     * using the given {@link TimeInstantModel time instant model}, ignoring
     * points
     * whose value
     * equals fill value.
     * <p>
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then points where the  value is equal to fill value are
     * excluded.
     * </p>
     * @param    varName   variable name
     * @param    component   index of the vector component
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or is
     * not a vector.
     */
    public TimeSeries getVectorTimeSeries(String varName, int component, 
        int[] startTime, int[] stopTime, TimeInstantModel tspec) throws
        CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component, startTime, stopTime,
            tspec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified component of a 1 dimensional
     * variable, optionally ignoring points whose value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @throws   CDFException.ReaderError  if variables is non-numeric, or is
     * not a vector.
     */
    public double[][] getVectorTimeSeries(String varName, int component,
        boolean ignoreFill) throws CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component, ignoreFill);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the time series of the specified component of 1 dimensional
     * variable in the specified time range, optionally ignoring points whose
     * value equals fill value.
     * <p>
     * A double[2][] array is returned. The 0th element is the
     * array containing times, and the 1st element is the array containing
     * corresponding values. If a fill value has been specified for this
     * variable via the FILLVAL attribute, then points where the value is
     * equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or is
     * not a vector.
     */
    public double[][] getVectorTimeSeries(String varName, int component,
        boolean ignoreFill, int[] startTime, int[] stopTime) throws
        CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component, ignoreFill,
            startTime, stopTime);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
    }

    /**
     * Returns the {@link TimeSeries time series} of the specified component
     * of 1 dimensional variable in the specified time range using the given
     * {@link TimeInstantModel time instant model}, optionally ignoring points
     * whose
     * value
     * equals fill value.
     * <p>
     * If a fill value has been specified for this variable via the FILLVAL
     * attribute, then if ignoreFill has the value true, points where the
     * value is equal to fill value are excluded if ignoreFill = true.
     * </p>
     * @param    varName   variable name
     * @param    startTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the first available time is used.
     * @param    stopTime   a 3 to 7 element int[], containing year,
     *  month (January is 1),
     * day,hour, minute, second and millisecond. May be null, in which case
     * the stop time is assumed to be later than the last available time.
     * @param    tspec  {@link TimeInstantModel time instant model}, May be
     * null, in which case the default model is used.
     * @throws   CDFException.ReaderError  if variables is non-numeric, or
     * is not a vector.
     * Use {@link #getTimeSeries(String varName) getTimeSeries(
     *  String varName)} for string type.
     */
    public TimeSeries getVectorTimeSeries(String varName, int component,
        boolean ignoreFill, int[] startTime, int[] stopTime,
        TimeInstantModel tspec) throws CDFException.ReaderError {
        try {
            return vector.getTimeSeries(varName, component, ignoreFill,
            startTime, stopTime, tspec);
        } catch (Throwable th) {
            throw new CDFException.ReaderError(th.getMessage());
        }
        
    }
    class CDFVector {
        MetaData rdr;
        private double[][] _getTimeSeries(String varName, int component,
            boolean ignoreFill, double[] timeRange) throws Throwable {
            checkType(varName);
            Variable var = thisCDF.getVariable(varName);
            Method method = TSExtractor.getMethod(var, "TimeSeries", 1);
            return (double[][])method.invoke(null, new Object []
                {rdr, var, new Integer(component), new Boolean(ignoreFill),
                timeRange});
        }
        private TimeSeries _getTimeSeries(String varName, int component,
            boolean ignoreFill, double[] timeRange, TimeInstantModel tspec)
            throws Throwable {
            checkType(varName);
            Variable var = thisCDF.getVariable(varName);
            Method method = TSExtractor.getMethod(var, "TimeSeriesObject", 1);
            TimeSeries ts = (TimeSeries)method.invoke(null, new Object []
                {rdr, var, new Integer(component), new Boolean(ignoreFill),
                timeRange, tspec});
            return new TimeSeriesImpl(ts);
        }
        public double[][] getTimeSeries(String varName, int component)
            throws Throwable {
            Variable var = thisCDF.getVariable(varName);
            if (var.getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            int dim = var.getEffectiveDimensions()[0];
            if ((component < 0) || (component > dim)) throw new Throwable(
                "component exceeds dimension of " + varName + " (" + dim +")");
            return _getTimeSeries(varName, component, true, null);
        }
        public double[][] getTimeSeries(String varName, int component,
            int[] startTime, int[] stopTime) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            Integer dim = (Integer)
                (thisCDF.getVariable(varName).getElementCount().get(0));
            if ((component < 0) || (component > dim.intValue())) throw new
                Throwable("Invalid component " + component + " for " +
                varName);
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, component, true, tr);
        }
        public TimeSeries getTimeSeries(String varName, int component,
            int[] startTime, int[] stopTime, TimeInstantModel tspec) throws
            Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            Integer dim = (Integer)
                (thisCDF.getVariable(varName).getElementCount().get(0));
            if ((component < 0) || (component > dim.intValue())) throw new
                Throwable("Invalid component " + component + " for " +
                varName);
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, component, true, tr, tspec);
        }
        public double[][] getTimeSeries(String varName, int component,
            boolean ignoreFill) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            Integer dim = (Integer)
                (thisCDF.getVariable(varName).getElementCount().get(0));
            if ((component < 0) || (component > dim.intValue())) throw new
                Throwable("Invalid component " + component + " for " +
                varName);
            return _getTimeSeries(varName, component, ignoreFill, null);
        }
        public double[][] getTimeSeries(String varName, int component,
            boolean ignoreFill, int[] startTime, int[] stopTime) throws
            Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            Integer dim = (Integer)
                (thisCDF.getVariable(varName).getElementCount().get(0));
            if ((component < 0) || (component > dim.intValue())) throw new
                Throwable("Invalid component " + component + " for " +
                varName);
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, component, ignoreFill, tr);
        }
        public TimeSeries getTimeSeries(String varName, int component,
            boolean ignoreFill, int[] startTime, int[] stopTime,
            TimeInstantModel tspec) throws Throwable {
            if (thisCDF.getVariable(varName).getEffectiveRank() != 1) throw new
                Throwable(varName + " is not a vector.");
            Integer dim = (Integer)
               (thisCDF.getVariable(varName).getElementCount().get(0));
            if ((component < 0) || (component > dim.intValue())) throw new
                Throwable("Invalid component " + component + " for " +
                varName);
            double[] trange = getAvailableTimeRange(varName);
            double[] tr = TSExtractor.getOverlap(rdr, trange, varName,
                startTime, stopTime);
            return _getTimeSeries(varName, component, ignoreFill, tr, tspec);
        }
    }
}
