package org.esa.beam.dataio.globcover.geotiff;

import org.esa.beam.dataio.geotiff.GeoTiffProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.util.io.FileUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GlobCoverGeoTiffProductReader extends GeoTiffProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected GlobCoverGeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        final Product product = super.readProductNodesImpl();
        final File inputFile = new File(getInput().toString());
        final Band band = product.getBandAt(0);
        band.setName(getBandName(inputFile));
        final File parentDir = inputFile.getParentFile();
        final File legendFile = parentDir.listFiles(new LegendFilenameFilter())[0];
        final LegendClass[] legendClasses = LegendParser.parse(legendFile, isFileRegional(inputFile));
        if (legendClasses.length > 0) {
            assignLegend(product, band, legendFile, legendClasses);
        }

        return product;
    }

    private boolean isFileRegional(File inputFile) {
        final String filename = FileUtils.getFilenameWithoutExtension(inputFile);
        return filename.endsWith("_Reg");
    }

    private void assignLegend(Product product, Band band, File legendFile, LegendClass[] legendClasses) {
        final IndexCoding indexCoding = new IndexCoding(FileUtils.getFilenameWithoutExtension(legendFile));
        final List<ColorPaletteDef.Point> points = new ArrayList<ColorPaletteDef.Point>();

        for (LegendClass lClass : legendClasses) {
                final int value = lClass.getValue();
                final String description = lClass.getDescription();
                final Color color = lClass.getColor();

                final String name = lClass.getName();
                final MetadataAttribute attribute = indexCoding.addIndex(name, value, description);
                final ColorPaletteDef.Point point = new ColorPaletteDef.Point(value, color, name);
                points.add(point);
                addMask( attribute, band.getName() + " == " + value, color, product );
            }

        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);
        final ColorPaletteDef.Point[] pointsArray = points.toArray(new ColorPaletteDef.Point[points.size()]);
        band.setImageInfo(new ImageInfo(new ColorPaletteDef(pointsArray)));
    }

    private String getBandName(File inputFile) {
        if (FileUtils.getFilenameWithoutExtension(inputFile).endsWith("_QL")) {
            return "CLA_QL";
        } else {
            return "CLA";
        }
    }

    private void addMask(MetadataAttribute metadataSample, String expression, Color color, Product product) {
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        Mask mask = Mask.BandMathsType.create(metadataSample.getName().toLowerCase(),
                                              metadataSample.getDescription(),
                                              width, height,
                                              expression, color, 0.5);
        maskGroup.add(mask);
    }

}
