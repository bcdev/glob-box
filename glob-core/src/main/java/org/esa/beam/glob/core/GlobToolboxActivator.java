package org.esa.beam.glob.core;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.io.IOException;
import java.util.Iterator;

public class GlobToolboxActivator implements Activator {


    public GlobToolboxActivator() {
    }


    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        patchDefaultReader();
        patchDefaultWriter();
    }

    private void patchDefaultReader() {
        ProductIOPlugInManager productIOPIM = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> readerIterator = productIOPIM.getReaderPlugIns(ProductIO.DEFAULT_FORMAT_NAME);
        //todo call of next() not sufficient
        final ProductReaderPlugIn readerPlugIn = readerIterator.next();
        productIOPIM.removeReaderPlugIn(readerPlugIn);
        productIOPIM.addReaderPlugIn(new PatchedDimapProductReaderPlugIn());
    }

    private void patchDefaultWriter() {
        ProductIOPlugInManager productIOPIM = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> dimapWriterIterator = productIOPIM.getWriterPlugIns(
                ProductIO.DEFAULT_FORMAT_NAME);
        //todo call of next() not sufficient
        final ProductWriterPlugIn dimapWriterPlugIn = dimapWriterIterator.next();
        productIOPIM.removeWriterPlugIn(dimapWriterPlugIn);
        productIOPIM.addWriterPlugIn(new PatchedDimapProductWriterPlugIn());
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }

    private static class PatchedDimapProductReaderPlugIn extends DimapProductReaderPlugIn {

        @Override
        public ProductReader createReaderInstance() {

            return new DimapProductReader(this) {
                @Override
                protected Product readProductNodesImpl() throws IOException {
                    final Product product = super.readProductNodesImpl();
                    if (product.getProductType().equals("TIME_SERIES")) {
                        // parse metadata
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
                    // 1) Identify source product   (use metadata index and band timestamp-postfix)
                    // 2) Identiofy band in source prodzct
                    // 3) open new / reuse opened product
                    // 4) delegate call to source product's reader
                    // 5) manage resources (opt)
                }
            };
        }
    }

    private static class PatchedDimapProductWriterPlugIn extends DimapProductWriterPlugIn {

        @Override
        public ProductWriter createWriterInstance() {

            return new DimapProductWriter(this) {
                @Override
                public boolean shouldWrite(ProductNode node) {
                    boolean shouldWrite = super.shouldWrite(node);
                    if (shouldWrite && node.getProduct().getProductType().equals("TIME_SERIES")) {
                        return !(node instanceof RasterDataNode);
                    }
                    return shouldWrite;
                }
            };
        }
    }
}
