package gov.nasa.gsfc.spdf.cdfj;
/**
 * Specifies the selection and options for the aggregated CDF.
 */
public interface SelectedVariableCollection {
    /**
     * Add a variable to the output with specified compression and
     * default specification for {@link SparseRecordOption SparseRecordOption},
     * (PAD).
     */
    public void add(String name, boolean compression);

    /**
     * Add a variable to the output with specified compression and
     * specified setting for {@link SparseRecordOption SparseRecordOption}.
     */
    public void add(String name, boolean compression, SparseRecordOption opt);

    /**
     * Returns whether compression was chosen for the variable.
     */
    public boolean isCompressed(String name);

    /**
     * Returns a list of variable selected.
     */
    public String[] getNames();

    /**
     * Returns whather a given variable is in the list of variable selected.
     */
    public boolean hasVariable(String name);

    /**
     * Returns {@link SparseRecordOption SparseRecordOption} chosen for
     * the given variable.
     */
    public SparseRecordOption getSparseRecordOption(String name);
}
