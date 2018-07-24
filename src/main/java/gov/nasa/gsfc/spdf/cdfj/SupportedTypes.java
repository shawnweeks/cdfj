package gov.nasa.gsfc.spdf.cdfj;
/**
 * Supported CDF data types.
 */
public class SupportedTypes {
     static final CDFDataType[] supportedTypes = 
         new CDFDataType[DataTypes.LAST_TYPE];
     static {
         for (int i = 0; i < supportedTypes.length; i++) {
             supportedTypes[i] = null;
         }
         supportedTypes[1] = CDFDataType.INT1;
         supportedTypes[11] = CDFDataType.UINT1;
         supportedTypes[2] = CDFDataType.INT2;
         supportedTypes[12] = CDFDataType.UINT2;
         supportedTypes[4] = CDFDataType.INT4;
         supportedTypes[14] = CDFDataType.UINT4;
         supportedTypes[8] = CDFDataType.INT8;
         supportedTypes[33] = CDFDataType.TT2000;
         supportedTypes[21] = CDFDataType.FLOAT;
         supportedTypes[44] = CDFDataType.FLOAT;
         supportedTypes[22] = CDFDataType.DOUBLE;
         supportedTypes[45] = CDFDataType.DOUBLE;
         supportedTypes[31] = CDFDataType.EPOCH;
         supportedTypes[32] = CDFDataType.EPOCH16;
         supportedTypes[41] = CDFDataType.INT1;
         supportedTypes[51] = CDFDataType.CHAR;
         supportedTypes[52] = CDFDataType.CHAR;
    }
    /**
     * Returns CDFDataType object for specified CDF type.
     * returns null if the type is not supported by this package.
     */
    public static CDFDataType cdfType(int type) {
        if ((type < 0) || (type > (supportedTypes.length - 1))) return null;
        return supportedTypes[type];
    }
}
