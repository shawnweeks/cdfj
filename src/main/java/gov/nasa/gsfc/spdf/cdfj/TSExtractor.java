package gov.nasa.gsfc.spdf.cdfj;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
public class TSExtractor extends Extractor {
    static {
        try {
            Class variableClass =
                Class.forName("gov.nasa.gsfc.spdf.cdfj.Variable");
            Class rdrClass = 
                Class.forName("gov.nasa.gsfc.spdf.cdfj.MetaData");
            Class cl = 
                Class.forName("gov.nasa.gsfc.spdf.cdfj.TSExtractor");
            Class timeSpecClass =
                Class.forName("gov.nasa.gsfc.spdf.cdfj.TimeInstantModel");
            double[] da = new double[0];
            int[] ia = new int[0];
            Class[][] arglist;
            arglist = new Class[][]{
                new Class[] {rdrClass, variableClass, Boolean.class,
                     da.getClass()},
                new Class[] {rdrClass, variableClass, Integer.class,
                     Boolean.class, da.getClass()},
                null,
                null
            };
            addFunction("TimeSeries", cl, arglist);
            arglist = new Class[][]{
                new Class[] {rdrClass, variableClass, Boolean.class,
                     da.getClass(), ia.getClass()},
                new Class[] {rdrClass, variableClass, Integer.class,
                     Boolean.class, da.getClass(), ia.getClass()},
                null,
                null
            };
            addFunction("SampledTimeSeries", cl, arglist);

            arglist = new Class[][]{
                new Class[] {rdrClass, variableClass, Boolean.class,
                     da.getClass(), timeSpecClass},
                new Class[] {rdrClass, variableClass, Integer.class,
                     Boolean.class, da.getClass(), timeSpecClass},
                null,
                null
            };
            addFunction("TimeSeriesObject", cl, arglist);

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
    public static double [][] getTimeSeries0(MetaData rdr, Variable var,
        Boolean ignoreFill, double[] timeRange) throws Throwable {
        return getTimeSeries(rdr, var, null, ignoreFill, timeRange);
    }
    public static double [][] getTimeSeries1(MetaData rdr, Variable var,
        Integer which, Boolean ignoreFill, double[] timeRange)
        throws Throwable {
        return getTimeSeries(rdr, var, which, ignoreFill, timeRange);
    }
    public static double [][] getTimeSeries(MetaData rdr, Variable var,
        Integer which, Boolean ignoreFill, double[] timeRange) throws Throwable
        {
        if (var.getNumberOfValues() == 0) return null;
        boolean ignore = ignoreFill.booleanValue();
        double [] vdata;
        int [] recordRange = null;
        TimeVariable tv = TimeVariableFactory.getTimeVariable(rdr,
            var.getName());
        double [] times = tv.getTimes();
        if (times == null) return null;
        boolean longType = false;
        int type = var.getType();
        int element = (which == null)?0:which.intValue();
        Number pad;
        if (DataTypes.typeCategory[type] == DataTypes.LONG) {
            longType = true;
            pad = new Long(((long[])getPadValue(rdr.thisCDF, var))[element]);
        } else {
            pad =
                new Double(((double[])getPadValue(rdr.thisCDF, var))[element]);
        }
        double[] stimes;
        Object o = null;
        Object[] oa = null;
        if (timeRange == null) {
            o = (which == null)?Extractor.getSeries0(rdr.thisCDF, var):
                                Extractor.getElement1(rdr.thisCDF, var, which);
            if (var.isMissingRecords()) {
                long[][] locations = var.getLocator().getLocations();
                oa = filterPad(o, times, pad, locations, 0);
            } else {
                oa = new Object[]{times, (double[])o};
            }
        } else {
            recordRange = getRecordRange(rdr, var, timeRange);
            if (recordRange == null) return null;
            if (which == null) {
                o = getRange0(rdr.thisCDF, var, new Integer(recordRange[0]),
                                  new Integer(recordRange[1]));
            } else {
                o = getRangeForElement1(rdr.thisCDF, var,
                    new Integer(recordRange[0]), new Integer(recordRange[1]),
                    which);
            }
            stimes = new double[Array.getLength(o)];
            int index = recordRange[0];
            for (int i = 0; i < stimes.length; i++) {
                stimes[i] = times[index++];
            }
            if (var.isMissingRecords()) {
                long[][] locations = var.getLocator().getLocations();
                oa = filterPad(o, stimes, pad, locations, recordRange[0]);
            } else {
                oa = new Object[]{stimes, (double[])o};
            }
        }
        stimes = (double[])oa[0];
        if (!ignore) {
            vdata = castToDouble(oa[1], longType);
            return new double [][] {stimes, vdata};
        }
        // fill values need to be filtered
        Object fill = Extractor.getFillValue(rdr.thisCDF, var);
        boolean fillDefined = true;
        Number fillValue = null;
        if (fill.getClass().getComponentType() == Double.TYPE) {
            fillDefined =  (((double[])fill)[0] == 0);
            if (fillDefined) fillValue = new Double(((double[])fill)[1]);
        } else {
            fillDefined =  (((long[])fill)[0] == 0);
            if (fillDefined) fillValue = new Long(((long[])fill)[1]);
        }
        if (!fillDefined) {
            vdata = castToDouble(oa[1], longType);
            return new double [][] {stimes, vdata};
        }
        return filterFill(stimes, oa[1], fillValue);
    }
    static int[] getRecordRange(MetaData rdr,Variable var,
        double[] timeRange) {
        return getRecordRange(rdr, var, timeRange, null);
    }

    static int[] getRecordRange(MetaData rdr, Variable var,
        double[] timeRange, TimeInstantModel ts) {
        try {
            TimeVariableX tvx = (TimeVariableX)
                TimeVariableFactory.getTimeVariable(rdr, var.getName());
            return tvx.getRecordRange(timeRange);
        } catch (Throwable t) {
        }
        return null;
    }
    public static double [][] filterFill(double[] times, double [] vdata,
        double fill, int first) {
        double [][] series;
        int count = 0;
        for (int i = 0; i < vdata.length; i++) {
            if (vdata[i] != fill) count++;
        }
        series = new double[2][count];
        int n = 0;
        for (int i = 0; i < vdata.length; i++) {
            if (vdata[i] == fill) continue;
            series[0][n] = times[i + first];
            series[1][n] = vdata[i];
            n++;
        }
        return series;
    }
    public static double [][] filterFill(double[] times, Object o,
        Number fillValue) {
        double [][] series;
        int count = 0;
        if (o.getClass().getComponentType() == Long.TYPE) {
            long fill = fillValue.longValue();
            long[] ldata = (long[])o;
            for (int i = 0; i < ldata.length; i++) {
                if (ldata[i] != fill) count++;
            }
            series = new double[2][count];
            int n = 0;
            for (int i = 0; i < ldata.length; i++) {
                if (ldata[i] == fill) continue;
                series[0][n] = times[i];
                series[1][n] = (double)ldata[i];
                n++;
            }
        } else {
            if (o.getClass().getComponentType() != Double.TYPE) return null;
            double fill = fillValue.doubleValue();
            double[] data = (double[])o;
            for (int i = 0; i < data.length; i++) {
                if (data[i] != fill) count++;
            }
            series = new double[2][count];
            int n = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == fill) continue;
                series[0][n] = times[i];
                series[1][n] = data[i];
                n++;
            }
        }
        return series;
    }
    static Object[] filterPad(Object o, double[] times, Number pad,
        long[][] locations, int first) {
        RecordSensor sensor = new RecordSensor(locations);
        if (o.getClass().getComponentType() == Double.TYPE) {
            double dpad = pad.doubleValue();
            double[] vdata = (double[])o;
            int npad = 0;
            for (int i = 0; i < vdata.length; i++) {
                if (sensor.hasRecord(first + i)) continue;
                if (vdata[i] == dpad) npad++;
            }
            if (npad == 0) return new Object[] {times, vdata};
            double[] _data = new double[vdata.length - npad];
            double[] _times = new double[vdata.length - npad];
            int index = 0;
            for (int i = 0; i < vdata.length; i++) {
                if (sensor.hasRecord(first + i) || (vdata[i] != dpad)) {
                    _data[index] = vdata[i];
                    _times[index] = times[i];
                    index++;
                }
            }
            return new Object[] {_times, _data};
        } else {
            if (!(o.getClass().getComponentType() == Long.TYPE)) return null;
            long lpad = pad.longValue();
            long[] ldata = (long[])o;
            int npad = 0;
            for (int i = 0; i < ldata.length; i++) {
                if (sensor.hasRecord(first + i)) continue;
                if (ldata[i] == lpad) npad++;
            }
            if (npad == 0) {
                return new Object[] {times, ldata};
            }
            long[] _data = new long[ldata.length - npad];
            double[] _times = new double[ldata.length - npad];
            int index = 0;
            for (int i = 0; i < ldata.length; i++) {
                if (sensor.hasRecord(first + i) || (ldata[i] != lpad)) {
                    _data[index] = ldata[i];
                    _times[index] = times[i];
                    index++;
                }
            }
            return new Object[] {_times, _data};
        }
    }
    public static double [][] getSampledTimeSeries0(MetaData rdr,
        Variable var,
        Boolean ignoreFill, double[] timeRange, int[] stride) throws Throwable
        {
        return getSampledTimeSeries(rdr, var, null, ignoreFill, timeRange,
            stride);
    }
    public static double [][] getSampledTimeSeries1(MetaData rdr,
        Variable var,
        Integer which, Boolean ignoreFill, double[] timeRange, int[] stride)
        throws Throwable {
        return getSampledTimeSeries(rdr, var, which, ignoreFill, timeRange,
            stride);
    }
    public static double [][] getSampledTimeSeries(MetaData rdr,
        Variable var,
        Integer which, Boolean ignoreFill, double[] timeRange, int[] stride)
        throws Throwable {
        if (var.getNumberOfValues() == 0) return null;
        boolean ignore = ignoreFill.booleanValue();
        double [] vdata;
        int [] recordRange = null;
        TimeVariable tv = TimeVariableFactory.getTimeVariable(rdr,
            var.getName());
        double [] times = tv.getTimes();
        if (times == null) return null;
        double[] stimes;
        Stride strideObject = new Stride(stride);
        if (timeRange == null) {
            vdata = (which == null)?
                   (double[])getSeries0(rdr.thisCDF, var, strideObject):
                   (double[])getElement1(rdr.thisCDF, var, which, strideObject);
        } else {
            recordRange = getRecordRange(rdr, var, timeRange);
            if (recordRange == null) return null;
            if (which == null) {
                vdata = (double[])getRange0(rdr.thisCDF, var,
                        new Integer(recordRange[0]),
                                  new Integer(recordRange[1]), strideObject);
            } else {
                vdata = (double[])getRangeForElement1(rdr.thisCDF, var,
                    new Integer(recordRange[0]), new Integer(recordRange[1]),
                    which, strideObject);
            }
        }
        int _stride = strideObject.getStride();
        double [] fill = (double[])getFillValue(rdr.thisCDF, var);
        if ((!ignore) || (fill[0] != 0)) {
            if (timeRange == null) {
                if (_stride == 1) {
                    return new double [][] {times, vdata};
                } else {
                    stimes = new double[vdata.length];
                    for (int i = 0; i < vdata.length; i ++) {
                        stimes[i] = times[i*_stride];
                    }
                    return new double [][] {stimes, vdata};
                }
            } else {
                stimes = new double[vdata.length];
                if (_stride == 1) {
                    System.arraycopy(times, recordRange[0], stimes, 0,
                        vdata.length);
                    return new double [][] {stimes, vdata};
                } else {
                    int srec = recordRange[0];
                    for (int i = 0; i < vdata.length; i ++) {
                        stimes[i] = times[srec + i*_stride];
                    }
                    return new double [][] {stimes, vdata};
                }
            }
        }
        // fill values need to be filtered
        if (timeRange == null) {
            if (_stride == 1) {
                return filterFill(times, vdata, fill[1], 0);
            } else {
                stimes = new double[vdata.length];
                for (int i = 0; i < vdata.length; i ++) {
                    stimes[i] = times[i*_stride];
                }
                return filterFill(stimes, vdata, fill[1], 0);
            }
        } else {
            stimes = new double[vdata.length];
            if (_stride == 1) {
                System.arraycopy(times, recordRange[0], stimes, 0,
                    vdata.length);
                return filterFill(stimes, vdata, fill[1], 0);
            } else {
                int srec = recordRange[0];
                for (int i = 0; i < vdata.length; i ++) {
                    stimes[i] = times[srec + i*_stride];
                }
                return filterFill(stimes, vdata, fill[1], 0);
            }
        }
    }
    public static TimeSeries getTimeSeriesObject0(MetaData rdr, Variable var,
        Boolean ignoreFill, double[] timeRange, TimeInstantModel ts) throws
        Throwable {
        return new GeneralTimeSeries(rdr, var, null, ignoreFill, timeRange,
           ts);
    }

    public static TimeSeries getTimeSeriesObject1(MetaData rdr, Variable var,
        Integer which, Boolean ignoreFill, double[] timeRange,
        TimeInstantModel ts) throws Throwable {
        return new GeneralTimeSeries(rdr, var, which, ignoreFill, timeRange,
           ts);
    }
    /**
     * Loss of precision may occur if type of var is LONG
     * times obtained are millisecond since 1970 regardless of the
     * precision of time variable corresponding to variable var
     */
    public static class GeneralTimeSeries implements TimeSeries {
        double [] vdata;
        double [] times;
        TimeInstantModel tspec;
        double[][] filtered = null;
        public GeneralTimeSeries(MetaData rdr, Variable var, Integer which,
            Boolean ignoreFill, double[] timeRange, TimeInstantModel ts) throws
            Throwable {
            boolean ignore = ignoreFill.booleanValue();
            int [] recordRange = null;
            if (ts != null) {
                synchronized (ts) {
                    tspec = (TimeInstantModel)ts.clone();
                }
            }
            TimeVariable tv = TimeVariableFactory.getTimeVariable(rdr,
                var.getName());
            times = tv.getTimes(tspec);
            if (times == null) throw new Throwable("times not available for " +
                var.getName());
            double[] stimes;
            boolean longType = false;
            int type = var.getType();
            if (DataTypes.typeCategory[type] == DataTypes.LONG) {
                longType = true;
            }
            Object o = null;
            if (timeRange == null) {
                o = (which == null)?getSeries0(rdr.thisCDF, var):
                    getElement1(rdr.thisCDF, var, which);
            } else {
                recordRange = getRecordRange(rdr, var, timeRange, ts);
                if (recordRange == null) throw new Throwable("no record range");
                if (which == null) {
                    o = getRange0(rdr.thisCDF, var, new Integer(recordRange[0]),
                                  new Integer(recordRange[1]));
                } else {
                    o = getRangeForElement1(rdr.thisCDF, var,
                    new Integer(recordRange[0]), new Integer(recordRange[1]),
                    which);
                }
            }
            vdata = castToDouble(o, longType);
            if (!ignore) {
                if (timeRange != null) {
                    stimes = new double[vdata.length];
                    System.arraycopy(times, recordRange[0], stimes, 0,
                    vdata.length);
                    times = stimes;
                }
            } else {
                // fill values need to be filtered
                double [] fill = (double[])getFillValue(rdr.thisCDF, var);
                int first = (timeRange != null)?recordRange[0]:0;
                if (fill[0] != 0) { // there is no fill value
                    stimes = new double[vdata.length];
                    System.arraycopy(times, first, stimes, 0, vdata.length);
                    times = stimes;
                } else {
                    filtered = filterFill(times, vdata, fill[1], first);
                }
            }
        }
        public double[] getTimes() {
            return (filtered != null)?filtered[0]:times;
        }
        public double[] getValues() {
            return (filtered != null)?filtered[1]:vdata;
        }
        public TimeInstantModel getTimeInstantModel() {return tspec;}
    }

    public static class GeneralTimeSeriesX implements TimeSeriesX {
        final TimeInstantModel tspec;
        final TimeVariableX tv;
        final String vname;
        final CDFImpl thisCDF;
        final  double[] timeRange;
        final boolean oned;
        final boolean columnMajor;
        public GeneralTimeSeriesX(MetaData rdr, Variable var,
            Boolean ignoreFill, final double[] timeRange, TimeInstantModel ts,
            boolean oned, boolean columnMajor) throws Throwable {
            boolean ignore = ignoreFill.booleanValue();
            if (ts != null) {
                synchronized (ts) {
                    tspec = (TimeInstantModel)ts.clone();
                }
            } else {
                tspec = null;
            }
            vname = var.getName();
            tv = (TimeVariableX)
                TimeVariableFactory.getTimeVariable(rdr, vname);
            thisCDF = rdr.thisCDF;
            this.timeRange = timeRange;
            this.oned = oned;
            this.columnMajor = columnMajor;
        }

        public double[] getTimes() throws CDFException.ReaderError {
            try {
                if (timeRange == null)  return tv.getTimes(tspec);
                return tv.getTimes(timeRange, tspec);
            } catch (Throwable th) {
                throw new CDFException.ReaderError(th.getMessage());
            }
        }

        public Object getValues() throws CDFException.ReaderError {
            try {
                if (timeRange == null)  {
                    return (oned)?thisCDF.getOneD(vname,columnMajor):
                           thisCDF.get(vname);
                }
                int[] recordRange = tv.getRecordRange(timeRange);
                if (recordRange == null) {
                    throw new CDFException.ReaderError("no data");
                }
                if (!oned) {
                    return thisCDF.getRange(vname, recordRange[0],
                    recordRange[1]);
                } else {
                    return thisCDF.getRangeOneD(vname, recordRange[0],
                    recordRange[1], columnMajor);
                }
            } catch (Throwable th) {
                throw new CDFException.ReaderError(th.getMessage());
            }
        }

        public TimeInstantModel getTimeInstantModel() {return tspec;}

        public boolean isOneD() {return oned;}

        public boolean isColumnMajor() {return columnMajor;}
    }
      
    public static String identifier() {return "TSExtractor";}

    static class RecordSensor {
        long[][] locations;
        int last = 0;
        RecordSensor (long[][] locations) {
            this.locations = locations;
        }
        boolean hasRecord(int number) {
            for (int i = last; i < locations.length; i++) {
                if ((number >= locations[i][0]) &&
                    (number <= locations[i][1])) {
                    last = i;
                    return true;
                }
            }
            return false;
        }
    }
    public static Method getMethod(Variable var, String name, int rank) throws
        Throwable {
        return getMethod(var, name, rank, false);
    }

    static Method getMethod(Variable var, String name, int rank,
        boolean checkMissing) throws Throwable {
        if (var == null) throw new Throwable("Internal error. Null variable " +
            "encountered in call to TSExtractor.getMethod()");
        int _rank = var.getEffectiveRank();
        if (_rank != rank) throw new
            Throwable("Called method is not appropriate for variables of " +
           "effective rank " + _rank);
        if (checkMissing && (var.isMissingRecords())) {
            System.out.println("Variable " + var.getName() +
            " has gaps." +
            " Sampled time series code is being tested. Feature is not " +
            " currently available if the variable has gaps.");
            return null;
        }
        Method method = getMethod(var, name);
        if (method == null) throw new Throwable("get" + name + " not " +
           "implemented for " + var.getName());
        return method;
    }
    static Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    public static long getTime(int[] time) {
        int[] t = new int[6];
        for (int i = 0; i < 3; i++) t[i] = time[i];
        t[1]--;
        for (int i = 3; i < 6; i++) t[i] = 0;
        int n = time.length;
        if (n >= 4) {
            t[3] = time[3];
            if (n >= 5) {
                t[4] = time[4];
                if (n >= 6) {
                    t[5] = time[5];
                }
            }
        }
        cal.clear();
        cal.set(t[0], t[1], t[2], t[3], t[4], t[5]);
        cal.set(Calendar.MILLISECOND,(n > 6)?time[6]:0);
        return cal.getTimeInMillis();
    }

    public static double getTime(MetaData rdr, String vname, int[] time)
        throws Throwable {
        boolean isTT2000 =
            TimeVariableFactory.getTimeVariable(rdr, vname).isTT2000();
        long t = getTime(time);
        return (isTT2000)?TimeUtil.milliSecondSince1970(t):(double)t;
    }

    public static double[] getOverlap(MetaData rdr, double[] trange,
        String varName,
        int[] startTime, int[] stopTime) throws Throwable {
        double[] overlap = new double[] {Double.MIN_VALUE, Double.MAX_VALUE};;
        if (startTime != null) {
            if (startTime.length < 3) throw new Throwable("incomplete start" +
                " time " + "definition.");
            double _start = getTime(rdr, varName, startTime);
            if (_start > trange[1]) throw new Throwable("Start time is " +
                "beyond end of data");
            overlap[0] = (_start < trange[0])?trange[0]:_start;
        } else {
            overlap[0] = trange[0];
        }
        if (stopTime != null) {
            if (stopTime.length < 3) throw new Throwable("incomplete stop" +
                " time " + "definition.");
            double _stop = getTime(rdr, varName, stopTime);
            if (_stop < trange[0]) throw new Throwable("Stop time is " +
                "before start of data");
            if (_stop < overlap[0]) throw new Throwable("Stop time is " +
                "before start time");
            overlap[1] = /*(_stop > trange[1])?trange[1]:*/_stop;
        } else {
            overlap[1] = trange[1];
        }
        return overlap;
    }
}
