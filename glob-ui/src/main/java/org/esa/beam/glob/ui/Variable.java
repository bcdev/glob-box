package org.esa.beam.glob.ui;

class Variable {
    private String name;
    private boolean selected;

    Variable(String name) {
        this(name, false);
    }

    Variable(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
