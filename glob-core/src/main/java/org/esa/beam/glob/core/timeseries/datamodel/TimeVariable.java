package org.esa.beam.glob.core.timeseries.datamodel;

public class TimeVariable {

    private final String name;
    private final boolean isSelected;

    /**
     * Constructor setting the name of the time variable and setting the selection state finally (!) to false.
     * Use <code>ITimeSeries.setVariableSelected( String, boolean )</code> to re-set selection state
     *
     * @deprecated do not use, use <code>TimeVariable(String, boolean)</code> instead
     */
    @Deprecated
    public TimeVariable(String name) {
        this(name, false);
    }

    public TimeVariable(String name, boolean isSelected) {
        this.name = name;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    /**
     * does nothing
     *
     * @deprecated do not use, use <code>ITimeSeries.setVariableSelected( String, boolean )</code> to re-set selection state
     */
    @Deprecated
    public void setSelected(boolean selected) {
    }

    public boolean fitsToPattern(String name) {
        return this.name.equals(name);
    }
}
