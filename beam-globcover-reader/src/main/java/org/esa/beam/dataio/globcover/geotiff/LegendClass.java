package org.esa.beam.dataio.globcover.geotiff;

import java.awt.Color;

public class LegendClass {

    private final String description;
    private final Color color;
    private final int value;
    private final String name;

    public LegendClass(int value, String description, String name, Color color) {
        this.value = value;
        this.description = description;
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }

    public int getValue() {
        return value;
    }
}
