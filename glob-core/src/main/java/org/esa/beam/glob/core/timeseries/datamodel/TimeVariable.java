package org.esa.beam.glob.core.timeseries.datamodel;

public class TimeVariable {

    private final String name;
    private boolean isSelected;

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

    public void setSelected(boolean selected) {
        if (isSelected != selected) {
            isSelected = selected;
        }
    }

    public boolean fitsToPattern(String name) {
        return this.name.equals(name);
    }
}
