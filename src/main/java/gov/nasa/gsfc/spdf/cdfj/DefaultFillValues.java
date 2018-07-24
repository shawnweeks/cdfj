package gov.nasa.gsfc.spdf.cdfj;
public class DefaultFillValues {
    static Number[] fillValues = new Number[50];
    static {
        fillValues[1] = new Integer(-128);
        fillValues[2] = new Integer(-32768);
        fillValues[4] = new Integer(-2147483648);
        fillValues[8] = new Long(-9223372036854775808L);
        fillValues[11] = new Integer(255);
        fillValues[12] = new Integer(65535);
        fillValues[14] = new Long(4294967295l);
        fillValues[44] = new Float(-1.0E31);
        fillValues[45] = new Double(-1.0E31);
        fillValues[31] = new Double(-1.0E31);
        fillValues[32] = new Double(-1.0E31);
        fillValues[33] = new Long(-9223372036854775808L);
    }
    public static Object value(int type) {
        return fillValues[type];
    }
}
