package org.esa.beam.glob.core;

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

class TimeSeriesProductWriter extends DimapProductWriter {

    public TimeSeriesProductWriter(ProductWriterPlugIn productWriterPlugIn) {
        super(productWriterPlugIn);
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        boolean shouldWrite = super.shouldWrite(node);
        if (shouldWrite && node.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
            return !(node instanceof RasterDataNode);
        }
        return shouldWrite;
    }
}
