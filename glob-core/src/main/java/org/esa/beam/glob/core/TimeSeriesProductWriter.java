package org.esa.beam.glob.core;

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;

import static org.esa.beam.glob.core.TimeSeriesProductBuilder.*;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 15:47:52
 */
class TimeSeriesProductWriter extends DimapProductWriter {

    public TimeSeriesProductWriter(ProductWriterPlugIn productWriterPlugIn) {
        super(productWriterPlugIn);
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        boolean shouldWrite = super.shouldWrite(node);
        if (shouldWrite && node.getProduct().getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            return !(node instanceof RasterDataNode);
        }
        return shouldWrite;
    }
}
