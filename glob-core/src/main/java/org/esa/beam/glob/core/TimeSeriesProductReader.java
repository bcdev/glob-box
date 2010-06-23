package org.esa.beam.glob.core;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import static org.esa.beam.glob.core.TimeSeriesProductBuilder.*;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 14:53:52
 */
class TimeSeriesProductReader extends DimapProductReader {

    private TimeSeries timeSeriesProduct;
    private Map<Band, Band> bandMap = new WeakHashMap<Band, Band>();

    public TimeSeriesProductReader(ProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Product product = super.readProductNodesImpl();
        if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            timeSeriesProduct = new TimeSeries(product);
        }
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        Band srcBand = bandMap.get(destBand);
        // 1) Identify source product   (use metadata index and band timestamp-postfix)
        // 2) Identify band in source prodzct
        // 3) open new / reuse opened product
        if (srcBand == null) {
            srcBand = timeSeriesProduct.getBand(destBand.getName());
            if (srcBand != null) {
                bandMap.put(destBand, srcBand);
            }
        }
        // 4) delegate call to source product's reader
        if (srcBand != null) { // TODO ????
            srcBand.readRasterData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, destBuffer, pm);
        }
        // 5) manage resources (opt)
    }
}
