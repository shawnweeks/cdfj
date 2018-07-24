package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
/**
 * Sparse Record Option Definition class
 */
public final class SparseRecordOption {

    /**
     * NONE option - CDF is not missing any records.
     */
    public static final SparseRecordOption NONE = new SparseRecordOption(0);

    /**
     * PADDED option - Pad value is returned for a missing record.
     */
    public static final SparseRecordOption PADDED = new SparseRecordOption(1);

    /**
     * PREVIOUS option - Previous value is returned for a missing record.
     * If no previous value, then a pad value is returned.
     */
    public static final SparseRecordOption PREVIOUS = new SparseRecordOption(2);

    static Hashtable<String, SparseRecordOption> ht = 
        new Hashtable<String, SparseRecordOption>();
    static {
        ht.put("none", NONE);
        ht.put("padded", PADDED);
        ht.put("previous", PREVIOUS);
    }
    int option;
    private SparseRecordOption(int option) {
        this.option = option;
    }
    public int getValue() {return option;}

    /**
     * Returns SparseRecordOption object for the named option.
     */
    public static SparseRecordOption getOption(String s) {
        return ht.get(s.toLowerCase());
    }
}
