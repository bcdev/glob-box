package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.ProductData;
import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;

/**
 * User: Thomas Storm
 * Date: 19.03.2010
 * Time: 15:18:34
 */
public class TimedKmlLayer extends KmlLayer {

    private final ProductData.UTC startTime;

    private final ProductData.UTC endTime;

    TimedKmlLayer(final String name, final RenderedImage layer, final BoundingBox latLonBox,
                  final ProductData.UTC startTime,
                  final ProductData.UTC endTime) {
        super(name, layer, latLonBox);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }
}
