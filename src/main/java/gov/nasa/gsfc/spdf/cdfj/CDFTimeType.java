package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
/**
 * CDF Time types
 */
public final class CDFTimeType {
    /**
     * EPOCH Type
     */
    public static final CDFTimeType EPOCH = new CDFTimeType(31);

    /**
     * EPOCH16 Type
     */
    public static final CDFTimeType EPOCH16 = new CDFTimeType(32);

    /**
     * TT2000 Type
     */
    public static final CDFTimeType TT2000 = new CDFTimeType(33);

    static Hashtable<String, CDFTimeType> ht = 
        new Hashtable<String, CDFTimeType>();
    static {
        ht.put("epoch", EPOCH);
        ht.put("epoch16", EPOCH16);
        ht.put("tt2000", TT2000);
    }
    int _type;
    private CDFTimeType(int _type) {
        this._type = _type;
    }
    public int getValue() {return _type;}

    /**
     * Returns CDFTimeType of the named time type.
     */
    public static CDFTimeType getType(String s) {
        return ht.get(s.toLowerCase());
    }
}
