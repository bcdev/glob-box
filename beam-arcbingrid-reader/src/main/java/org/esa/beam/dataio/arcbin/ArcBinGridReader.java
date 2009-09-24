package org.esa.beam.dataio.arcbin;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;


public class ArcBinGridReader extends AbstractProductReader {
 

    protected ArcBinGridReader(ArcBinGridReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                if (destBand.isNoDataValueUsed()) {
                    double noDataValue = destBand.getNoDataValue();
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                        destBuffer.setElemDoubleAt(rasterIndex, noDataValue);
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
    


    @Override
    public void close() throws IOException {
        super.close();
    }

    private Product createProduct() throws IOException {
        return null;
        
    }
}
