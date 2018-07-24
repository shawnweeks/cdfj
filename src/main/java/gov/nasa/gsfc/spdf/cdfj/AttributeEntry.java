package gov.nasa.gsfc.spdf.cdfj;
import java.util.*;
/**
 * Specifes an attribute entry.
 */
public interface AttributeEntry {
    public int getType();
    public Object getValue();
    public boolean isStringType();
    public boolean isLongType();
    public String getAttributeName();
    public int getVariableNumber(); 
    public int getNumberOfElements();
    public boolean isSameAs(AttributeEntry ae);
}
