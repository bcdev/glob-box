package org.esa.beam.glob.export;

import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;

class KmlLayer {

    private final RenderedImage layer;
    private final BoundingBox latLonBox;
    private final String timeSpan;
    private final String name;

    KmlLayer(final String name, RenderedImage layer, BoundingBox latLonBox) {
        this(name, layer, latLonBox, null);
    }

    KmlLayer(final String name, final RenderedImage layer, final BoundingBox latLonBox, String timeSpan) {
        this.name = name;
        this.layer = layer;
        this.latLonBox = latLonBox;
        this.timeSpan = timeSpan;
    }

    public String getName() {
        return name;
    }

    public RenderedImage getLayer() {
        return layer;
    }

    public BoundingBox getLatLonBox() {
        return latLonBox;
    }

    public String getTimeSpan() {
        return timeSpan;
    }

}
