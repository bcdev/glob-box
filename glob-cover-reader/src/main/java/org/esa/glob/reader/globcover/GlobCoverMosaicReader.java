package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GlobCoverMosaicReader extends AbstractProductReader {

    private NetcdfFile ncfile;

    protected GlobCoverMosaicReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            ncfile = getInputNetcdfFile();
            return createProduct();
        } finally {
            close();
        }
    }

    private Product createProduct() throws IOException {
        final List<Dimension> dimensionList = ncfile.getDimensions();
        final List<Variable> variableList = ncfile.getVariables();
        final List<Attribute> attributeList = ncfile.getGlobalAttributes();

        for (Dimension dimension : dimensionList) {
            System.out.println("dimension.getName() = " + dimension.getName());
        }
        for (Variable variable : variableList) {
            System.out.println("variable.getNameEscaped() = " + variable.getNameEscaped());
        }
        for (Attribute attribute : attributeList) {
            final String stringValue = attribute.getStringValue();
            if (stringValue != null) {
                System.out.println(attribute.getName() + " = " + stringValue);
            } else {
                System.out.println(attribute.getName() + " = " + attribute.getNumericValue());
            }
        }

        // geocoding information is contained in variable StructMetadata.0, which is a string
        final Variable structMetadata0 = ncfile.findVariable("StructMetadata%2e0");
        final Array array = structMetadata0.read();
        final char[] chars = (char[]) array.getStorage();
        final String structMetadata0Text = new String(chars);
        System.out.println("StructMetadata.0 = " + structMetadata0Text);
        // todo - parse text
        
        return new Product("test", "test", 10, 10);
    }

    @Override
    public void close() throws IOException {
        if (ncfile != null) {
            try {
                ncfile.close();
            } catch (IOException e) {
                // ignore
            }
            ncfile = null;
        }
        super.close();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
    }

    private NetcdfFile getInputNetcdfFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        final String path;
        if (input instanceof String) {
            path = (String) input;
        } else {
            path = ((File) input).getPath();
        }

        return NetcdfFile.open(path);
    }
}
