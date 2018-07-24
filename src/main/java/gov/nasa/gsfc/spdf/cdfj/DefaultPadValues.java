package gov.nasa.gsfc.spdf.cdfj;
public class DefaultPadValues {
    static Number[] padValues = new Number[50];
    static {
        padValues[1] = new Integer(-127);
        padValues[2] = new Integer(-32767);
        padValues[4] = new Integer(-2147483647);
        padValues[8] = new Long(-9223372036854775807L);
        padValues[11] = new Integer(254);
        padValues[12] = new Integer(65534);
        padValues[14] = new Long(4294967294l);
        padValues[44] = new Float(-1.0E30);
        padValues[45] = new Double(-1.0E30);
        padValues[31] = new Double(0);
        padValues[32] = new Double(0);
        padValues[33] = new Long(-9223372036854775807L);
    }
    public static Object value(int type) {
        return padValues[type];
    }
}
