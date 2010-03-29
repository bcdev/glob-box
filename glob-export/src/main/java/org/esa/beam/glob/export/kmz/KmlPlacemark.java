package org.esa.beam.glob.export.kmz;

import java.awt.geom.Point2D;

public class KmlPlacemark extends KmlFeature {

    private Point2D position;

    public KmlPlacemark(String name, String description, Point2D position) {
        super(name, description);
        this.position = position;
    }

    public Point2D getPosition() {
        return position;
    }

    @Override
    protected String getKmlElementName() {
        return "Placemark";
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        final Point2D position = getPosition();
        sb.append("<Point>");
        sb.append(String.format("<coordinates>%s,%s,0</coordinates>", getPosition().getX(), position.getY()));
        sb.append("</Point>");
    }
}
