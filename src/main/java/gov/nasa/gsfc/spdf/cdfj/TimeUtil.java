package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
public class TimeUtil {
    static final long[] jtimes;
    static final int[] leapSecondIds;
    static final long[] tt_times;;
    static final int highest;
    static SimpleDateFormat sdf =
        new SimpleDateFormat("y'-'M'-'dd'T'HH:mm:ss.SSS");
    public static final long TT_JANUARY_1_1970 = -946727957816000000l;
    static final long JANUARY_1_1972 = Date.UTC(72,0,1,0,0,0);
    static final int lastLeapSecondId;
    static {
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        boolean[][] transition = new boolean[100][2];
        transition[2][0] = true;
        transition[2][1] = true;
        transition[3][1] = true;
        transition[4][1] = true;
        transition[5][1] = true;
        transition[6][1] = true;
        transition[7][1] = true;
        transition[8][1] = true;
        transition[9][1] = true;
        transition[11][0] = true;
        transition[12][0] = true;
        transition[13][0] = true;
        transition[15][0] = true;
        transition[17][1] = true;
        transition[19][1] = true;
        transition[20][1] = true;
        transition[22][0] = true;
        transition[23][0] = true;
        transition[24][0] = true;
        transition[25][1] = true;
        transition[27][0] = true;
        transition[28][1] = true;
        transition[35][1] = true;
        transition[38][1] = true;
        transition[42][0] = true;
        transition[45][0] = true;
        transition[46][1] = true;
/*
        try {
            URL url = new URL(
            "https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history");
            URLConnection con = url.openConnection();
            InputStream is = con.getInputStream();
            byte[] ba = new byte[con.getContentLength()];
            int index = 0;
            int c;
            while ((c = is.read()) != -1) ba[index++] = (byte)c;
            String s = new String(ba);
            Scanner sc = new Scanner(s);
            Vector lines = new Vector();
            while (sc.hasNextLine()) lines.add(sc.nextLine());
            int n = lines.size() -2;
            while (n > 0) {
                Scanner scl = new Scanner((String)lines.get(n));
                int year = scl.nextInt();
                String mon = scl.next();
                if (mon.startsWith("Jul")) transition[year-1970][0]=true;
                if (mon.startsWith("Jan")) transition[year-1970-1][1]=true;
                if (year == 1973) break;
                n--;
            }
            System.out.println("Leap second table updated");
        } catch (Exception ex) {
            System.out.println("Unable to retrieve leap second table. " +
            "Using existing table for version 3.6");
        }
*/
        Vector<Long> times = new Vector<Long>();
        Vector<Integer> ids = new Vector<Integer>();
        for (int i = 0; i < transition.length; i++) {
            if (transition[i][0]) {
                times.add(new Long(Date.UTC(70 + i, 5, 30, 23, 59, 59)));
                ids.add(new Integer((1970 + i)*10000 + 701));
            }
            if (transition[i][1]) {
                times.add(new Long(Date.UTC(70 + i, 11, 31, 23, 59, 59)));
                ids.add(new Integer((1971 + i)*10000 + 101));
            }
        }
        jtimes = new long[times.size()];
        tt_times = new long[times.size()];
        leapSecondIds = new int[times.size()];
        for (int i = 0; i < jtimes.length; i++) {
            jtimes[i] = times.get(i).longValue();
            leapSecondIds[i] = ids.get(i).intValue();
            try {
                tt_times[i] = tt2000(jtimes[i]);
            } catch (Throwable t) {
                System.out.println("Internal error.");
            }
        }
        highest = 1000*jtimes.length;
        lastLeapSecondId = leapSecondIds[leapSecondIds.length - 1];
    }


    public static double getOffset(long l) throws Throwable {
        if (l < JANUARY_1_1972) throw new Throwable("Times before " +
            "January 1, 1972 are not supported at present");
        int i;
        double start;
        if (l < jtimes[0]) {
            start = (double)l;
        } else {
            start = -1;
            i = 0;
            while (i < (jtimes.length - 1)) {
                if (l < jtimes[i+1]) {
                    start = (double)(l + (i + 1)*1000);
                    break;
                }
                i++;
            }
            if (start < 0) start = (double)(l + highest);
        }
        return start;
    }

    /**
     * converts a Date to number of milliseconds since 1970 (corrected for
     * leap seconds
     */
    public static double milliSecondSince1970(Date d) throws Throwable {
        return milliSecondSince1970(d.getTime());
    }

    /**
     * corrects (java Date returned) number of milliseconds since 1970 for
     * leap seconds
     */
    public static double milliSecondSince1970(long javaMilliSecond) throws
        Throwable {
        long l = javaMilliSecond;
        if (l < JANUARY_1_1972) throw new Throwable("Times before " +
            "January 1, 1972 are not supported at present");
        int i = (jtimes.length - 1);
        while (i >= 0) {
            if (l > jtimes[i]) return (double)(l + (i + 1)*1000);
            i--;
        }
        return (double)l;
    }

    /**
     * returns tt2000 for
     * (java Date returned) number of milliseconds since 1970
     */
    public static long tt2000(long l) throws Throwable {
        return  TT_JANUARY_1_1970 + 1000000*(long)milliSecondSince1970(l);
    }

    /**
     * returns tt2000 for a Date
     */
    public static long tt2000(Date d) throws Throwable {
        return  TT_JANUARY_1_1970 + 1000000*(long)milliSecondSince1970(d);
    }
    static Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    /**
     * returns number of milliseconds since 1970 for given time ignoring
     * leap seconds
     */
    public static long milliSecondSince1970(int[] time) throws Throwable {
        return milliSecondSince1970(time, false);
    }

    /**
     * returns number of milliseconds since 1970 for given time, optionally
     * ignoring leap seconds
     */
    static long milliSecondSince1970(int[] time, boolean tt) throws Throwable {
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
        int adjust = 0;
        if (t[5] == 60) {
            if (!tt) throw new Throwable("second value 60 is valid " +
                "for tt2000 only.");
            int id = -1;
            if ((t[4] == 59) && (t[3] == 23)) {
                if ((time[2] == 30) && (time[1] == 6)) {
                    id = time[0]*10000 + 701;
                } else {
                    if ((time[2] == 31) && (time[1] == 12)) {
                        id = (1 + time[0])*10000 + 101;
                    }
                }
            }
            if (id == -1) throw new Throwable("Invalid leap second time");
            for (int i = (leapSecondIds.length - 1); i >= 0; i--) {
                if (id != leapSecondIds[i]) continue;
                adjust = 1000000000;
                break;
            }
            if (adjust == 0) throw new Throwable("Invalid leap second time");
            t[5] = 59;
        }
        cal.clear();
        cal.set(t[0], t[1], t[2], t[3], t[4], t[5]);
        cal.set(Calendar.MILLISECOND,(n > 6)?time[6]:0);
        return cal.getTimeInMillis();
    }
    /**
     * returns tt2000 for the given time
     */
    public static long tt2000(int[] time) throws Throwable {
        long msec = milliSecondSince1970(time, true);
        if (time.length < 6) return tt2000(msec);
        int adjust = (time[5] == 60)?1000000000:0;
        if (time.length <= 7) return adjust + tt2000(msec);
        if (time.length == 8) return adjust + tt2000(msec) + time[7]*1000;
        return adjust + tt2000(msec) + time[7]*1000 + time[8];
    }
        
    public static class Validator {
        public static long correctedIfNecessary(long varTime, int leapId) throws
            Throwable {
            if (leapId == lastLeapSecondId) return varTime;
            if (leapId < lastLeapSecondId) { //
                int id = -1;
                for (int i = (leapSecondIds.length - 1); i >= 0; i--) {
                    if (leapId == leapSecondIds[i]) {
                        id = i;
                        break;
                    }
                }
                if (id < 0) throw new Throwable("Invalid leapId");
                //if (varTime < tt2000(jtimes[id + 1])) return varTime;
                if (varTime < tt_times[id + 1]) return varTime;
                int i = (id + 1);
                for (; i < (jtimes.length -1); i++) {
                    //if (varTime < tt2000(jtimes[i + 1])) break;
                    if (varTime < tt_times[i + 1]) break;
                }
                return varTime + (i - id)*1000000000L;
            } else {
                if (varTime < tt_times[jtimes.length - 1]) return varTime;
                throw new Throwable("Out of date Leap second table");
            }
        }
    }
}
