package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

class GlobCoverMosaicProductReader extends AbstractGcProductReader {

    private static final String PRODUCT_TYPE_ANNUAL = "GC_MOSAIC_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_MOSAIC_BI";

    private Map<TileIndex, GCTileFile> inputFileMap;

    protected GlobCoverMosaicProductReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return createProduct();
    }

    private Product createProduct() throws IOException {
        inputFileMap = getInputFileMap();
        final File inputfile = getInputFile();
        final GCTileFile refGcFile = new GCTileFile(inputfile);
        int width = (TileIndex.MAX_HORIZ_INDEX + 1) * TileIndex.TILE_SIZE;
        int height = (TileIndex.MAX_VERT_INDEX + 1) * TileIndex.TILE_SIZE;
        final String fileName = FileUtils.getFilenameWithoutExtension(new File(refGcFile.getFilePath()));
        // product name == file name without tile indeces
        final String prodName = fileName.substring(0, fileName.lastIndexOf('_'));
        final String prodType = getProductType(refGcFile);

        final Product product = createProduct(refGcFile, prodName, prodType, width, height);

        product.setPreferredTileSize(TileIndex.TILE_SIZE, TileIndex.TILE_SIZE);
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IOException("GlobCoverMosaicProductReader.readBandRasterDataImpl not implemented");
    }

    @Override
    protected void addBands(Product product, final GCTileFile gcTileFile) throws IOException {
        super.addBands(product, gcTileFile);

        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final AffineTransform imageToModelTransform = product.getGeoCoding().getImageToModelTransform();
            final DefaultMultiLevelModel model = new DefaultMultiLevelModel(imageToModelTransform,
                                                                            band.getSceneRasterWidth(),
                                                                            band.getSceneRasterHeight());
            final GCMosaicMultiLevelSource multiLevelSource = new GCMosaicMultiLevelSource(model, band, inputFileMap);
            band.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
        }
    }

    @Override
    protected GeoPos getUpperLeftPosition() throws IOException {
        return new GeoPos(-180, 90);
    }

    @Override
    protected String getBimonthlyProductType() {
        return PRODUCT_TYPE_BIMON;
    }

    @Override
    protected String getAnnualProductType() {
        return PRODUCT_TYPE_ANNUAL;
    }

    @Override
    public void close() throws IOException {
        for (GCTileFile tileFile : inputFileMap.values()) {
            tileFile.close();
        }
        super.close();
    }

    private Map<TileIndex, GCTileFile> getInputFileMap() throws IOException {
        final File refFile = getInputFile();
        File dir = refFile.getParentFile();
        final String filePrefix = getProductFilePrefix(refFile);
        final File[] files = dir.listFiles(new MosaicFileFilter(filePrefix));

        Map<TileIndex, GCTileFile> fileMap = new TreeMap<TileIndex, GCTileFile>();
        for (File file : files) {
            final String filename = FileUtils.getFilenameWithoutExtension(file);
            final String tilePos = filename.substring(filename.lastIndexOf('_') + 1, filename.length());
            final String[] tileIndices = tilePos.split("V");
            int horizIndex = Integer.parseInt(tileIndices[0].substring(1));   // has H as prefix
            int vertIndex = Integer.parseInt(tileIndices[1]);    // has no V as prefix
            final TileIndex tileIndex = new TileIndex(horizIndex, vertIndex);
            fileMap.put(tileIndex, new GCTileFile(file));
        }
        return Collections.unmodifiableMap(fileMap);
    }


    private static String getProductFilePrefix(File file) {
        final String fileName = FileUtils.getFilenameWithoutExtension(file);
        return fileName.substring(0, fileName.lastIndexOf('_'));
    }

    private File getInputFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        return new File(String.valueOf(input));
    }

    private class MosaicFileFilter implements FileFilter {

        private final String filePrefix;

        MosaicFileFilter(String filePrefix) {
            this.filePrefix = filePrefix;
        }

        @Override
        public boolean accept(File file) {
            final String fileName = file.getName();
            return fileName.startsWith(filePrefix) && fileName.endsWith(
                    getReaderPlugIn().getDefaultFileExtensions()[0]);
        }
    }
}