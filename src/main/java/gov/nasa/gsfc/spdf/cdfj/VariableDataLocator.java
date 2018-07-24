package gov.nasa.gsfc.spdf.cdfj;
/**
 * Data locations for a Variable
 */
public interface VariableDataLocator {
    /**
     * Returns array of locations.
     * @return	int[][3] containing attributes of data segments. 
     * Attributes are first record, last record and the offset in the
     * input CDF file.
     */
    public long[][] getLocations();
}
