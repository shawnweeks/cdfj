package gov.nasa.gsfc.spdf.cdfj;
public interface CDF3 extends CDFCore {
    public final int MAX_STRING_SIZE = 256;
    public final int AgrEDRHead_OFFSET = 20;
    public final int AzEDRHead_OFFSET = 48;
    public final int rDimSizes_OFFSET = 84;
    public final int CDF_VERSION = 3;
    public final int OFFSET_NEXT_VDR = 12;
    public final int OFFSET_NEXT_ADR = 12;
    // attribute
    public final int ATTR_OFFSET_NAME = 68;
    public final int OFFSET_NEXT_AEDR = 12;
    public final int OFFSET_SCOPE = 28;
    public final int OFFSET_ENTRYNUM = 28;
    public final int ATTR_OFFSET_DATATYPE = 24;
    public final int ATTR_OFFSET_NUM_ELEMENTS = 32;
    public final int OFFSET_VALUE = 56;
    // variable
    public final int VAR_OFFSET_DATATYPE = 20;
    public final int OFFSET_MAXREC = 24;
    public final int VAR_OFFSET_NAME = 84;
    public final int OFFSET_zNumDims = VAR_OFFSET_NAME + 256;
    public final int VAR_OFFSET_NUM_ELEMENTS = 64;
    public final int OFFSET_NUM = 68;
    public final int OFFSET_FIRST_VXR = 28;
    public final int OFFSET_FLAGS = 44;
    public final int OFFSET_sRecords = 48;
    public final int OFFSET_RECORDS = 12;
    public final int OFFSET_BLOCKING_FACTOR = 80;
    // data
    public final int OFFSET_NEXT_VXR = 12;
    public final int OFFSET_NENTRIES = 20;
    public final int OFFSET_NUSED = 24;
    public final int OFFSET_FIRST = 28;
    public final int OFFSET_RECORD_TYPE = 8;
    // compressed
    public final int OFFSET_CDATA = 24;
    public final int OFFSET_CSIZE = 16;
}
