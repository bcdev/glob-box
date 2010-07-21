package org.esa.beam.glob.core;

import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesFactory;

import java.io.IOException;

import static org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries.*;

class TimeSeriesProductReader extends DimapProductReader {

    public TimeSeriesProductReader(ProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Product product = super.readProductNodesImpl();
        if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            TimeSeriesFactory.create(product);
        }
        return product;
    }
}
