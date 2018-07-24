package gov.nasa.gsfc.spdf.cdfj;
public interface CDF2 extends CDFCore {
    public final int MAX_STRING_SIZE = 64;
    public final int AgrEDRHead_OFFSET = 12;
    public final int AzEDRHead_OFFSET = 36;
    public final int rDimSizes_OFFSET = 60;
    public final int CDF_VERSION = 2;
    public final int OFFSET_NEXT_VDR = 8;
    public final int OFFSET_NEXT_ADR = 8;
    // attribute
    public final int ATTR_OFFSET_NAME = 52;
    public final int OFFSET_NEXT_AEDR = 8;
    public final int OFFSET_SCOPE = 16;
    public final int OFFSET_ENTRYNUM = 20;
    public final int ATTR_OFFSET_DATATYPE = 16;
    public final int ATTR_OFFSET_NUM_ELEMENTS = 24;
    public final int OFFSET_VALUE = 48;
    // variable
    public final int VAR_OFFSET_DATATYPE = 12;
    public final int OFFSET_MAXREC = 16;
/*
    public final int VAR_OFFSET_NAME = 64;
    public final int OFFSET_zNumDims = VAR_OFFSET_NAME + MAX_STRING_SIZE;
    public final int VAR_OFFSET_NUM_ELEMENTS = 48;
    public final int OFFSET_NUM = 52;
*/
    public final int OFFSET_FIRST_VXR = 20;
    public final int OFFSET_FLAGS = 28;
    public final int OFFSET_sRecords = 32;
    public final int OFFSET_RECORDS = 8;
    public final int OFFSET_BLOCKING_FACTOR = 60;
    // data
    public final int OFFSET_NEXT_VXR = 8;
    public final int OFFSET_NENTRIES = 12;
    public final int OFFSET_NUSED = 16;
    public final int OFFSET_FIRST = 20;
    public final int OFFSET_RECORD_TYPE = 4;
    // compressed
    public final int OFFSET_CDATA = 16;
    public final int OFFSET_CSIZE = 12;
}
