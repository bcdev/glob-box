package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.ProductData;
import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

class KmlLayer {

    private final String name;
    private final RenderedImage overlay;
    private final BoundingBox latLonBox;
    private final ProductData.UTC startTime;
    private final ProductData.UTC endTime;
    private final List<KmlLayer> children;

    KmlLayer(final String name, final RenderedImage overlay, final BoundingBox latLonBox,
             final ProductData.UTC startTime,
             final ProductData.UTC endTime) {
        this.name = name;
        this.overlay = overlay;
        this.latLonBox = latLonBox;
        this.startTime = startTime;
        this.endTime = endTime;
        this.children = new ArrayList<KmlLayer>();
    }

    public KmlLayer(String name, RenderedImage overlay, BoundingBox latLonBox) {
        this(name, overlay, latLonBox, null, null);
    }

    public List<KmlLayer> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }

    public String getName() {
        return name;
    }

    public RenderedImage getOverlay() {
        return overlay;
    }

    public BoundingBox getLatLonBox() {
        return latLonBox;
    }

    public void addChild(final KmlLayer child) {
        children.add(child);
    }
}
