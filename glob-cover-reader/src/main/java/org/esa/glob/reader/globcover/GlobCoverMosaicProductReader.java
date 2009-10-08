package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class GlobCoverMosaicProductReader extends AbstractProductReader {

    private static final String PRODUCT_TYPE_ANNUAL = "GC_MOSAIC_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_MOSAIC_BI";
    private static final double PIXEL_SIZE_DEG = 1 / 360.0;
    private static final double PIXEL_CENTER = 0.5;

    private Map<TileIndex, GCTileFile> inputFileMap;
    private static final String HDF_EXTENSION = ".hdf";

    protected GlobCoverMosaicProductReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        inputFileMap = getInputFileMap();
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IOException("GlobCoverMosaicProductReader.readBandRasterDataImpl not implemented");
    }

    @Override
    public void close() throws IOException {
        for (GCTileFile tileFile : inputFileMap.values()) {
            tileFile.close();
        }
        super.close();
    }

    private Product createProduct() throws IOException {
        final GCTileFile firstFile = inputFileMap.values().toArray(new GCTileFile[inputFileMap.size()])[0];
        final Product product;
        int width = (TileIndex.MAX_HORIZ_INDEX + 1) * TileIndex.TILE_SIZE;
        int height = (TileIndex.MAX_VERT_INDEX + 1) * TileIndex.TILE_SIZE;
        final String fileName = FileUtils.getFilenameWithoutExtension(new File(firstFile.getFilePath()));
        // product name == file name without tile indeces
        final String prodName = fileName.substring(0, fileName.lastIndexOf('_'));
        final String prodType;
        if (firstFile.isAnnualFile()) {
            prodType = PRODUCT_TYPE_ANNUAL;
        } else {
            prodType = PRODUCT_TYPE_BIMON;
        }
        product = new Product(prodName, prodType, width, height);
        product.setFileLocation(getProductDir());
        product.setStartTime(firstFile.getStartDate());
        product.setEndTime(firstFile.getEndDate());

        addGeoCoding(product);
        addBands(product, firstFile);
        GCTileFile.addIndexCodingAndBitmasks(product.getBand("SM"));
        product.getMetadataRoot().addElement(firstFile.getMetadata());
        product.setPreferredTileSize(TileIndex.TILE_SIZE, TileIndex.TILE_SIZE);
        return product;
    }

    private void addBands(Product product, final GCTileFile gcTileFile) throws IOException {
        final List<BandDescriptor> bandDescriptorList = gcTileFile.getBandDescriptorList();
        for (BandDescriptor descriptor : bandDescriptorList) {
            final Band band = new Band(descriptor.getName(), descriptor.getDataType(),
                                       product.getSceneRasterWidth(),
                                       product.getSceneRasterHeight());
            band.setScalingFactor(descriptor.getScaleFactor());
            band.setScalingOffset(descriptor.getOffsetValue());
            band.setDescription(descriptor.getDescription());
            band.setUnit(descriptor.getUnit());
            band.setNoDataValueUsed(descriptor.isFillValueUsed());
            band.setNoDataValue(descriptor.getFillValue());

            final AffineTransform imageToModelTransform = product.getGeoCoding().getImageToModelTransform();
            final DefaultMultiLevelModel model = new DefaultMultiLevelModel(imageToModelTransform,
                                                                            band.getSceneRasterWidth(),
                                                                            band.getSceneRasterHeight());
            final GCMosaicMultiLevelSource multiLevelSource = new GCMosaicMultiLevelSource(model, band, inputFileMap);
            band.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
            product.addBand(band);
        }
    }

    private void addGeoCoding(Product product) throws IOException {
        final Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
                                                               product.getSceneRasterWidth(),
                                                               product.getSceneRasterHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(-180, 90);
        transform.scale(PIXEL_SIZE_DEG, -PIXEL_SIZE_DEG);
        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);

        try {
            final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, rect, transform);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            throw new IOException("Not able to create GeoCoding: ", e);
        }
    }

    private Map<TileIndex, GCTileFile> getInputFileMap() throws IOException {
        File dir = getProductDir();
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                final String fileName = file.getName();
                return fileName.startsWith(GlobCoverMosaicReaderPlugIn.FILE_PREFIX) &&
                       fileName.endsWith(HDF_EXTENSION);
            }
        });

        String filePrefix = getProductFilePrefix(files[0]);

        Map<TileIndex, GCTileFile> fileMap = new TreeMap<TileIndex, GCTileFile>();
        for (File file : files) {
            if(file.getName().startsWith(filePrefix)) {
                final String filename = FileUtils.getFilenameWithoutExtension(file);
                final String tilePos = filename.substring(filename.lastIndexOf('_') + 1, filename.length());
                final String[] tileIndices = tilePos.split("V");
                int horizIndex = Integer.parseInt(tileIndices[0].substring(1));   // has H as prefix
                int vertIndex = Integer.parseInt(tileIndices[1]);    // has no V as prefix
                final TileIndex tileIndex = new TileIndex(horizIndex, vertIndex);
                fileMap.put(tileIndex, new GCTileFile(file));
            }
        }
        return Collections.unmodifiableMap(fileMap);
    }


    private static String getProductFilePrefix(File file) {
        final String fileName = FileUtils.getFilenameWithoutExtension(file);
        return fileName.substring(0, fileName.lastIndexOf('_'));
    }

    private File getProductDir() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        final File inputFile = new File(String.valueOf(input));
        File dir;
        if (inputFile.isDirectory()) {
            dir = inputFile;
        } else {
            dir = inputFile.getParentFile();
        }
        return dir;
    }

}