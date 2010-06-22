package org.esa.beam.glob.core;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;

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
            return new TimeSeriesProductReader(this);
        }
    }

    private static class PatchedDimapProductWriterPlugIn extends DimapProductWriterPlugIn {

        @Override
        public ProductWriter createWriterInstance() {
            return new TimeSeriesProductWriter(this);
        }

    }

}
