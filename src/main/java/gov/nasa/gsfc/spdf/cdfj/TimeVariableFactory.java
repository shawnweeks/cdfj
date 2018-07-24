package gov.nasa.gsfc.spdf.cdfj;
import java.nio.*;
import java.util.*;
public class TimeVariableFactory {
    private TimeVariableFactory() {
    }
    public static final double JANUARY_1_1970;
    static final long longFill = -9223372036854775807L;
    static final double doubleFill = -1.0e31;
    static {
        int offset = 0;
        for (int year=0; year < 1970; year++) {
            int days = 365;
            if ((year%4 == 0)) {
                days++;
                if ((year%100 == 0)) {
                    days--;
                    if ((year%400 == 0)) days++;
                }
            }
            offset += days;
        }
        JANUARY_1_1970 = offset*8.64e7;
    }
    private static TimeInstantModel defaultTimeInstantModel =
        new DefaultTimeInstantModelImpl();
    public static final long JANUARY_1_1970_LONG = (long)JANUARY_1_1970;
    public static final long TT2000_DATE = JANUARY_1_1970_LONG +
        Date.UTC(100, 0, 1, 12, 0, 0) - 42184;

    public static TimeInstantModel getDefaultTimeInstantModel() {
        return (TimeInstantModel)defaultTimeInstantModel.clone();
    }

    public static TimeInstantModel getDefaultTimeInstantModel(double msec) {
        TimeInstantModel tspec =
            (TimeInstantModel)defaultTimeInstantModel.clone();
        ((DefaultTimeInstantModelImpl)tspec).setBaseTime(msec);
        return tspec;
    }

    public static CDFTimeVariable getTimeVariable(MetaData rdr,
        String vname) throws Throwable {
        CDFImpl cdf = rdr.thisCDF;
        Variable var = (Variable)cdf.getVariable(vname);
        int precision = -1;
        String tname = null;
        int recordCount = var.getNumberOfValues();
        CDFTimeVariable tv;
        if (var == null) {
            throw new Throwable("Bad variable name " + vname);
        }
        tname = rdr.getTimeVariableName(vname);
/*
        Vector v = (Vector)cdf.getAttribute(var.getName(), "DEPEND_0");
        if (v.size() > 0) tname = (String)v.elementAt(0);
        if (tname == null) {
            if (!vname.equals("Epoch")) {
                if (cdf.getVariable("Epoch") != null) {
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
*/
        Variable tvar = (Variable)cdf.getVariable(tname);
        if (tvar == null) {
            throw new Throwable("Time variable not found for " + vname);
        }
        boolean themisLike = false;
        if (tvar.getNumberOfValues() == 0) { //themis like
            Vector v = (Vector)cdf.getAttribute(var.getName(), "DEPEND_TIME");
            if (v.size() > 0) {
                tname = (String)v.elementAt(0);
                tvar = cdf.getVariable(tname);
                themisLike = true;
            } else {
                throw new Throwable("Expected unix time variable " +
                "not found " + "for " + var.getName());
            }
        }
        if (tvar.getNumberOfValues() == 0) {
            throw new Throwable("Empty time variable for " + var.getName());
        }
        ByteBuffer buf = null;
        if (tvar.getType() == DataTypes.CDF_TIME_TT2000) {
            LongVarContainer lbuf = new LongVarContainer(cdf, tvar, null);
            lbuf.run();
            buf = lbuf.getBuffer();
        } else {
            DoubleVarContainer dbuf = new DoubleVarContainer(cdf, tvar, null,
            true);
            dbuf.run();
            buf = dbuf.getBuffer();
        }
        if (tvar.getType() == DataTypes.EPOCH16) {
            tv = new CDFEpoch16Variable(cdf, tname, buf);
        } else {
            if (tvar.getType() == DataTypes.CDF_TIME_TT2000) {
                tv = new CDFTT2000Variable(cdf, tname, buf);
            } else {
                if (themisLike) {
                    tv = new UnixTimeVariable(cdf, tname, buf);
                } else {
                    tv = new CDFEpochVariable(cdf, tname, buf);
                }
            }
        }
        tv.setRecordCount(recordCount); 
        return tv;
    }

    public static abstract class CDFTimeVariable implements TimeVariableX {
        CDFImpl cdf;
        String name;
        TimePrecision precision;
        final ByteBuffer tbuf;
        long offset;
        int recordCount;
        CDFTimeVariable(CDFImpl cdf, String name, ByteBuffer obuf) {
            this.name = name;
            this.cdf = cdf;
            tbuf = obuf;
        }

        public String getName() {return name;}

        public TimePrecision getPrecision() {return precision;}

        public double [] getTimes() {
            try {
                return getTimes(0, recordCount - 1, null);
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

        public double [] getTimes(TimeInstantModel ts) throws Throwable {
            return getTimes(0, recordCount - 1, ts);
        }

        public double [] getTimes(int[] recordRange) throws Throwable {
            try {
                return getTimes(recordRange, defaultTimeInstantModel);
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

        abstract double [] getTimes(int first, int last, TimeInstantModel ts)
            throws Throwable;

        public double [] getTimes(int[] recordRange, TimeInstantModel ts) throws
            Throwable {
            return getTimes(recordRange[0], recordRange[1],  ts);
        }

        public double [] getTimes(double[] timeRange) {
            try {
                return getTimes(timeRange, null);
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

        public double [] getTimes(double[] timeRange, TimeInstantModel ts) throws
            Throwable {
            if (timeRange == null) {
                return getTimes(0, recordCount - 1, ts);
            }
            int [] rr = getRecordRange(timeRange, ts);
            if (rr == null) return null;
            return getTimes(rr[0], rr[1], ts);
        }

        public double [] getTimes(int[] startTime, int[] stopTime,
            TimeInstantModel ts) throws Throwable {
            if (startTime == null) {
               throw new Throwable("start time is required");
            }
            if (stopTime == null) {
               throw new Throwable("stop time is required");
            }
            long start;
            long stop;
            if (startTime.length < 3) throw new Throwable(
                "incomplete start time " + "definition.");
            start = TSExtractor.getTime(startTime);
            if (stopTime.length < 3) throw new Throwable(
                "incomplete stop time " + "definition.");
            stop = TSExtractor.getTime(stopTime);
            if (isTT2000()) {
                start = (long)TimeUtil.milliSecondSince1970(start);
                stop = (long)TimeUtil.milliSecondSince1970(stop);
            }
            return getTimes(new double[]{(double)start, (double)stop}, ts);
        }

        public double [] getTimes(int[] startTime, int[] stopTime) throws
            Throwable {
            return  getTimes(startTime, stopTime, null);
        }

        public int[] getRecordRange(double[] timeRange) throws Throwable {
            return getRecordRange(timeRange, null);
        }

        public int [] getRecordRange(int[] startTime, int[] stopTime) throws
            Throwable {
            return getRecordRange(startTime, stopTime, null);
        }

        public int [] getRecordRange(int[] startTime, int[] stopTime,
            TimeInstantModel ts) throws Throwable {
            if (startTime.length < 3) throw new Throwable(
               "incomplete start time " + "definition.");
            if (stopTime.length < 3) throw new Throwable(
               "incomplete stop time " + "definition.");
            long start = TSExtractor.getTime(startTime);
            long stop = TSExtractor.getTime(stopTime);
            if (isTT2000()) {
                start = (long)TimeUtil.milliSecondSince1970(start);
                stop = (long)TimeUtil.milliSecondSince1970(stop);
            }
            return getRecordRange(
               new double[]{(double)start, (double)stop}, ts);
        }

        public int[] getRecordRange(double[] timeRange, TimeInstantModel ts)
            throws Throwable {
            double[] temp = getTimes(0, recordCount - 1, ts);
            double start = timeRange[0]; //offset in millis since 1970
            double stop = timeRange[1];
            if (!((ts == null) || (ts == defaultTimeInstantModel))) {
                start = start - ts.getBaseTime() + JANUARY_1_1970_LONG;
                stop = stop - ts.getBaseTime() + JANUARY_1_1970_LONG;
                if (ts.getOffsetUnits() == TimePrecision.MICROSECOND) {
                    start *= 1.0e3;
                    stop *= 1.0e3;
                } else {
                    if (ts.getOffsetUnits() == TimePrecision.NANOSECOND) {
                        start *= 1.0e6;
                        stop *= 1.0e6;
                    }
                }
            }
            int i = 0;
            for (; i < temp.length; i++) {
                if (temp[i] == Double.NaN) continue;
                if (start > temp[i]) continue;
                break;
            }
            if (i == temp.length) return null;
            int low = i;
            int last = i;
            for (; i < temp.length; i++) {
                if (temp[i] == Double.NaN) continue;
                last = i;
                if (stop < temp[i]) {
                    break;
                } else {
                    if (stop == temp[i]) {
                        last = i - 1;
                        break;
                    }
                }
            }
            return new int[] {low, last};
        }

        protected void setRecordCount(int count) {
            recordCount = count;
        }

        public double getFirstMilliSecond() {
            TimeInstantModel tspec = getDefaultTimeInstantModel();
            ((DefaultTimeInstantModelImpl)tspec).setBaseTime((double)0);
            tspec.setOffsetUnits(TimePrecision.MILLISECOND);
            try {
                double d = Double.NaN;
                int n = 0;
                while (n < recordCount) {
                    d = getTimes(n, n, tspec)[0];
                    if (d != Double.NaN) return d;
                    n++;
                }
                return d;
            } catch (Throwable t) {
                t.printStackTrace();
                return Double.NaN;
            }
        }
        abstract void reset();
        public abstract boolean isTT2000();
        public ByteBuffer getRawBuffer() {return tbuf;}
    }

    public static class CDFEpochVariable extends CDFTimeVariable {
        TimePrecision offsetUnits = TimePrecision.MILLISECOND;
        DoubleBuffer _dbuf;
        CDFEpochVariable(CDFImpl cdf, String name, ByteBuffer obuf) {
            super(cdf, name, obuf);
            precision = TimePrecision.MILLISECOND;
            _dbuf = tbuf.asDoubleBuffer();
        }
        public double [] getTimes(int first, int last, TimeInstantModel ts)
            throws Throwable {
            double base = (double)JANUARY_1_1970_LONG;
            if (ts != null) {
                if (ts.getOffsetUnits() != TimePrecision.MILLISECOND) {
                    throw new Throwable("Unsupported offset units: " +
                    "Only millisecond offset units are supported for this " +
                    "variable.");
                }
                base = ts.getBaseTime();
            }
            int count = last - first + 1;
            double [] da = new double[count];
            ByteBuffer bbuf = tbuf.duplicate();
            bbuf.order(tbuf.order());
            DoubleBuffer dbuf = bbuf.asDoubleBuffer();
            dbuf.position(first);
            dbuf.get(da);
            for (int i = 0; i < count; i++) {
                if (da[i] == doubleFill) {
                    System.out.println("at " + i + " fill found");
                    da[i] = Double.NaN;
                    continue;
                }
                da[i] -= base;
            }
            return da;
        }
        void reset() {
            _dbuf.position(0);
        }
        public boolean isTT2000() {return false;}
        public boolean canSupportPrecision(TimePrecision tp) {
            if (tp == TimePrecision.MILLISECOND) return true;
            return false;
        }
    }
    public static class CDFTT2000Variable extends CDFTimeVariable {
        LongBuffer _lbuf;
        CDFTT2000Variable(CDFImpl cdf, String name, ByteBuffer obuf) {
            super(cdf, name, obuf);
            precision = TimePrecision.NANOSECOND;
            _lbuf = tbuf.asLongBuffer();
        }
        public double [] getTimes(int first, int last, TimeInstantModel ts)
            throws Throwable {
            TimePrecision offsetUnits = TimePrecision.MILLISECOND;
            long base = JANUARY_1_1970_LONG;
            if (ts != null) {
                base = (long)ts.getBaseTime();
                offsetUnits = ts.getOffsetUnits();
            }
            int count = last - first + 1;
            double [] da = new double[count];
            ByteBuffer bbuf = tbuf.duplicate();
            bbuf.order(tbuf.order());
            LongBuffer lbuf = bbuf.asLongBuffer();
            if (offsetUnits == TimePrecision.MILLISECOND) {
                offset = base - TT2000_DATE;
                for (int i = first; i <= last; i++) {
                    long nano = lbuf.get(i);
                    if (nano == longFill) {
                        da[i - first] = Double.NaN;
                        continue;
                    }
                    long milli = (nano/1000000) - offset;
                    double rem = ((double)(nano % 1000000))/1.0e6;
                    da[i - first] = (double)(milli) + rem;
                }
            } else {
                if (offsetUnits == TimePrecision.MICROSECOND) {
                    offset = 1000*(base - TT2000_DATE);
                    for (int i = first; i <= last; i++) {
                        long nano = lbuf.get(i);
                        if (nano == longFill) {
                            da[i - first] = Double.NaN;
                            continue;
                        }
                        long micro = (nano/1000) - offset;
                        double rem = ((double)(nano % 1000))/1.0e3;
                        da[i - first] = (double)(micro) + rem;
                    }
                } else {
                    if (offsetUnits != TimePrecision.NANOSECOND) {
                        throw new Throwable("You may request only " +
                        "millisecond, microsecond or nanosecond offset " +
                        "for a variable whose time variable is TT2000 type.");
                    }
                    offset = 1000000*(base - TT2000_DATE);
                    for (int i = first; i <= last; i++) {
                        long nano = lbuf.get(i);
                        if (nano == longFill) {
                            da[i - first] = Double.NaN;
                            continue;
                        }
                        da[i - first] = (double)(nano - offset);
                    }
                }
            }
            return da;
        }
        void reset() {
            _lbuf.position(0);
        }
        public boolean isTT2000() {return true;}
        public boolean canSupportPrecision(TimePrecision tp) {
            if (tp == TimePrecision.PICOSECOND) return false;
            return true;
        }
    }
    public static class CDFEpoch16Variable extends CDFTimeVariable {
        DoubleBuffer _dbuf;
        CDFEpoch16Variable(CDFImpl cdf, String name, ByteBuffer obuf) {
            super(cdf, name, obuf);
            precision = TimePrecision.PICOSECOND;
            _dbuf = tbuf.asDoubleBuffer();
        }
        public double [] getTimes(int first, int last, TimeInstantModel ts)
            throws Throwable {
            TimePrecision offsetUnits = TimePrecision.MILLISECOND;
            long base = JANUARY_1_1970_LONG;
            if (ts != null) {
                base = (long)ts.getBaseTime();
                offsetUnits = ts.getOffsetUnits();
            }
            int count = last - first + 1;
            double [] da = new double[count];
            ByteBuffer bbuf = tbuf.duplicate();
            bbuf.order(tbuf.order());
            DoubleBuffer dbuf = bbuf.asDoubleBuffer();
            double d;
            double _d;
            long mul;
            if (offsetUnits == TimePrecision.MILLISECOND) {
                mul = 1000;
                for (int i = first; i <= last; i++) {
                    _d = dbuf.get(2*i);
                    if (_d == doubleFill) {
                        da[i - first] = Double.NaN;
                        continue;
                    }
                    d = (double)(((long)(dbuf.get(2*i))*mul) - base);
                    da[i - first] = d + (dbuf.get(2*i + 1)/1.0e9);
                }
            } else {
                if (offsetUnits == TimePrecision.MICROSECOND) {
                    offset = 1000*base;
                    mul = 1000000;
                    for (int i = first; i <= last; i++) {
                        _d = dbuf.get(2*i);
                        if (_d == doubleFill) {
                            da[i - first] = Double.NaN;
                            continue;
                        }
                        d = (double)(((long)(dbuf.get(2*i))*mul) - offset);
                        da[i - first] = d + (dbuf.get(2*i + 1)/1.0e6);
                    }
                } else {
                    if (offsetUnits == TimePrecision.NANOSECOND) {
                        offset = 1000000*base;
                        mul = 1000000000;
                        for (int i = first; i <= last; i++) {
                            _d = dbuf.get(2*i);
                            if (_d == doubleFill) {
                                da[i - first] = Double.NaN;
                                continue;
                            }
                            d = (double)(((long)(dbuf.get(2*i))*mul) -
                                offset);
                            da[i - first] = d + (dbuf.get(2*i + 1)/1.0e3);
                        }
                    } else { // pico
                        for (int i = first; i <= last; i++) {
                            _d = dbuf.get(2*i);
                            if (_d == doubleFill) {
                                da[i - first] = Double.NaN;
                                continue;
                            }
                            d = dbuf.get(2*i)*1.0e3 - base; //millisec
                            da[i - first] = d*1.0e9 + dbuf.get(2*i + 1);
                        }
                    }
                }
            }
            return da;
        }
        void reset() {
            _dbuf.position(0);
        }
        public boolean isTT2000() {return false;}
        public boolean canSupportPrecision(TimePrecision tp) {return true;}
    }
    public static class UnixTimeVariable extends CDFTimeVariable {
        DoubleBuffer _dbuf;
        UnixTimeVariable(CDFImpl cdf, String name, ByteBuffer obuf) {
            super(cdf, name, obuf);
            precision = TimePrecision.MICROSECOND;
            _dbuf = tbuf.asDoubleBuffer();
        }
        public double [] getTimes(int first, int last, TimeInstantModel ts)
            throws Throwable {
            TimePrecision offsetUnits = TimePrecision.MILLISECOND;
            long base = JANUARY_1_1970_LONG;
            if (ts != null) {
                base = (long)ts.getBaseTime();
                offsetUnits = ts.getOffsetUnits();
            }
            int count = last - first + 1;
            double [] da = new double[count];
            ByteBuffer bbuf = tbuf.duplicate();
            bbuf.order(tbuf.order());
            DoubleBuffer dbuf = bbuf.asDoubleBuffer();
            dbuf.position(first);
            dbuf.get(da);
            if (offsetUnits == TimePrecision.MILLISECOND) {
                if (base == JANUARY_1_1970_LONG) {
                    for (int i = 0; i < count; i++) {
                        if (da[i] == doubleFill) {
                            da[i] = Double.NaN;
                            continue;
                        }
                        da[i] *= 1.0e3;
                    }
                } else {
                    offset = base - JANUARY_1_1970_LONG;
                    for (int i = 0; i < count; i++) {
                        if (da[i] == doubleFill) {
                            da[i] = Double.NaN;
                            continue;
                        }
                        long milli = (long)(da[i]*1000) - offset;
                        da[i] = (double)(milli);
                    }
                }
            } else { // it must be micro second
                if (offsetUnits != TimePrecision.MICROSECOND) {
                    throw new Throwable("Desired precision exceeds " +
                    "highest available precision -- microsecond");
                }
                if (base == JANUARY_1_1970) {
                    for (int i = 0; i < count; i++) {
                        if (da[i] == doubleFill) {
                            da[i] = Double.NaN;
                            continue;
                        }
                        da[i] *= 1.0e6;
                    }
                } else {
                    offset = 1000*(base - JANUARY_1_1970_LONG);
                    for (int i = 0; i < count; i++) {
                        if (da[i] == doubleFill) {
                            da[i] = Double.NaN;
                            continue;
                        }
                        long micro = (long)(da[i]*1000000) - offset;
                        da[i] = (double)(micro);
                    }
                }
            }
            return da;
        }
        void reset() {
            _dbuf.position(0);
        }
        public boolean isTT2000() {return false;}
        public boolean canSupportPrecision(TimePrecision tp) {
            if (tp == TimePrecision.MICROSECOND) return true;
            if (tp == TimePrecision.MILLISECOND) return true;
            return false;
        }
    }

    static class DefaultTimeInstantModelImpl implements TimeInstantModel {
        double baseTime = JANUARY_1_1970;
        TimePrecision baseTimeUnits = TimePrecision.MILLISECOND;
        TimePrecision offsetUnits = TimePrecision.MILLISECOND;
        public double getBaseTime() {return baseTime;}
        public TimePrecision getBaseTimeUnits() {return baseTimeUnits;}
        public TimePrecision getOffsetUnits() {return offsetUnits;}
        public void setOffsetUnits(TimePrecision offsetUnits) {
            this.offsetUnits = offsetUnits;
        }
        void setBaseTime(double msec) {baseTime = msec;}
        public Object clone() {
            try {
                return super.clone();
            } catch (java.lang.CloneNotSupportedException ex) {
                ex.printStackTrace();
            }
            return null;
       }
    }
}
