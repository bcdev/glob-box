package org.esa.beam.glob.core.timeseries.datamodel;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 11:26:17
 */
public class TimeVariable {

    private final String name;
    private boolean isSelected;

    public TimeVariable(String name) {
        this.name = name;
    }

    public TimeVariable(String name, boolean isSelected) {
        this(name);
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean fitsToPattern(String name) {
        return this.name.equals(name);
    }
}
