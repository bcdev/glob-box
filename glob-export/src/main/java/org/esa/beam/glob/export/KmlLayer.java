package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;

class KmlLayer {

    private final String name;
    private final RenderedImage layer;
    private final BoundingBox latLonBox;
    private GeoCoding geoCoding;
    private Product product;

    KmlLayer(final String name, final RenderedImage layer, final BoundingBox latLonBox) {
        this.name = name;
        this.layer = layer;
        this.latLonBox = latLonBox;
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

    public GeoCoding getGeoCoding() {
        return geoCoding;
    }

    public Product getProduct() {
        return product;
    }

    public void setGeoCoding(final GeoCoding geoCoding) {
        this.geoCoding = geoCoding;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }
}
