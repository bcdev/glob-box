package org.esa.glob.reader.globcover;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.FileUtils;

import java.awt.geom.Area;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class GlobCoverMosaicProductReader extends AbstractGcProductReader {

    private static final String PRODUCT_TYPE_ANNUAL = "GC_MOSAIC_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_MOSAIC_BI";

    private Map<TileIndex, GCTileFile> inputFileMap;
    private Area coveredImageArea;

    protected GlobCoverMosaicProductReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return createProduct();
    }

    private Product createProduct() throws IOException {
        final File inputFile = getInputFile();
        inputFileMap = createInputFileMap(inputFile);
        coveredImageArea = createCoveredImageArea(inputFileMap.keySet());
        final GCTileFile refGcFile = new GCTileFile(inputFile);
        int width = (TileIndex.MAX_HORIZ_INDEX + 1) * TileIndex.TILE_SIZE;
        int height = (TileIndex.MAX_VERT_INDEX + 1) * TileIndex.TILE_SIZE;
        final String fileName = FileUtils.getFilenameWithoutExtension(new File(refGcFile.getFilePath()));
        // product name == file name without tile indeces
        final String prodName = fileName.substring(0, fileName.lastIndexOf('_'));
        final String prodType = getProductType(refGcFile);

        return createProduct(refGcFile, prodName, prodType, width, height);
    }


    @Override
    protected MultiLevelImage getMultiLevelImage(Band band) {
        return new DefaultMultiLevelImage(new GCMosaicMultiLevelSource(band, inputFileMap, coveredImageArea));
    }

    @Override
    protected GeoPos getUpperLeftPosition() throws IOException {
        float lon = -180.0f + 0.5f * 1 / 360.0f;
        float lat = 90.0f - 0.5f * 1 / 360.0f;
        return new GeoPos(lat, lon);
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

    private Map<TileIndex, GCTileFile> createInputFileMap(File refFile) throws IOException {
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

    private static Area createCoveredImageArea(Set<TileIndex> tileIndexes) {
        Area coveredImageArea = new Area();
        for (TileIndex index : tileIndexes) {
            coveredImageArea.add(new Area(index.getBounds()));
        }
        return coveredImageArea;
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
            String fileExtension = getReaderPlugIn().getDefaultFileExtensions()[0];
            return fileName.startsWith(filePrefix) && fileName.endsWith(fileExtension);
        }
    }

}