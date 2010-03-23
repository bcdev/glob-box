package org.esa.beam.glob.export.kmz;

import java.awt.geom.Point2D;

public class KmlPlacemark {

    private String label;
    private Point2D pos;

    public KmlPlacemark(String label, Point2D pos) {
        this.label = label;
        this.pos = pos;
    }

    public String getLabel() {
        return label;
    }

    public Point2D getPos() {
        return pos;
    }
}
