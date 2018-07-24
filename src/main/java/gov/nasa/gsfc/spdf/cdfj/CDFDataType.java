package gov.nasa.gsfc.spdf.cdfj;
/**
 * CDF Data types
 */
public final class CDFDataType {

    /**
     * INT1
     */
    public static final CDFDataType INT1 = new CDFDataType(1);

    /**
     * INT2
     */
    public static final CDFDataType INT2 = new CDFDataType(2);

    /**
     * INT4
     */
    public static final CDFDataType INT4 = new CDFDataType(4);

    /**
     * INT8
     */
    public static final CDFDataType INT8 = new CDFDataType(8);

    /**
     * UINT1
     */
    public static final CDFDataType UINT1 = new CDFDataType(11);

    /**
     * UINT2
     */
    public static final CDFDataType UINT2 = new CDFDataType(12);

    /**
     * UINT4
     */
    public static final CDFDataType UINT4 = new CDFDataType(14);

    /**
     * FLOAT
     */
    public static final CDFDataType FLOAT = new CDFDataType(21);

    /**
     * DOUBLE
     */
    public static final CDFDataType DOUBLE = new CDFDataType(22);

    /**
     * EPOCH
     */
    public static final CDFDataType EPOCH = new CDFDataType(31);

    /**
     * EPOCH16
     */
    public static final CDFDataType EPOCH16 = new CDFDataType(32);

    /**
     * CHAR
     */
    public static final CDFDataType CHAR = new CDFDataType(51);

    /**
     * TT2000
     */
    public static final CDFDataType TT2000 = new CDFDataType(33);
    int type;
    private CDFDataType(int type) {
        this.type = type;
    }
    public int getValue() {return type;}

    /**
     * Returns CDFDataType for a given CDFTimeType.
     */
    public static CDFDataType getType(CDFTimeType type) {
        if (type.getValue() == 31) return EPOCH;
        if (type.getValue() == 32) return EPOCH16;
        if (type.getValue() == 33) return TT2000;
        return null;
    }
}
