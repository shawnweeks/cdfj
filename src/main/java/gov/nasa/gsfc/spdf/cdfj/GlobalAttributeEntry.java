package gov.nasa.gsfc.spdf.cdfj;
public class GlobalAttributeEntry extends AEDR {
    static int GLOBAL_ATTRIBUTE_RECORD_TYPE = 5;
    public GlobalAttributeEntry(ADR adr, int type, Object value) throws
        Throwable {
        super(adr, type, value);
        setAttributeType(GLOBAL_ATTRIBUTE_RECORD_TYPE);
    }
    public GlobalAttributeEntry(ADR adr, Object value) throws
        Throwable {
        this(adr, -1, value);
    }
}
