package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
/**
 * Time Precision Definition class
 */
public final class TimePrecision {
    public static final TimePrecision MILLISECOND =
        new TimePrecision(0);
    public static final TimePrecision MICROSECOND =
        new TimePrecision(1);
    public static final TimePrecision NANOSECOND =
        new TimePrecision(2);
    public static final TimePrecision PICOSECOND =
        new TimePrecision(3);
    static Hashtable<String, TimePrecision> ht =
        new Hashtable<String, TimePrecision>();
    static {
        ht.put("millisecond", MILLISECOND);
        ht.put("microsecond", MICROSECOND);
        ht.put("nanosecond", NANOSECOND);
        ht.put("picosecond", PICOSECOND);
    }
    static final int MIN_LENGTH=3;
    int precision;
    private TimePrecision(int precision) {
        this.precision = precision;
    }
    public int getValue() {return precision;}
    public static TimePrecision getPrecision(String s) {
        String _s = s.toLowerCase();
        int len = _s.length();
        if (len < MIN_LENGTH) return null;
        Set keys = ht.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.substring(0,len).equals(_s)) return ht.get(k);
        }
        return null;
    }
}
