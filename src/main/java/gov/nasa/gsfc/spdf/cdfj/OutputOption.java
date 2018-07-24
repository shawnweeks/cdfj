package gov.nasa.gsfc.spdf.cdfj;
public interface OutputOption {
    public void add(String name, boolean compression);
    public boolean isCompressed(String name);
    public String[] getNames();
    public boolean hasVariable(String name);
    public void setRowMajority(boolean rowMajority);
}
