package org.esa.beam.dataio.arcbin;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.media.jai.PlanarImage;


public class ArcBinGridReader extends AbstractProductReader {
    private RasterDataFile rasterDataFile;

    protected ArcBinGridReader(ArcBinGridReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File file;
        Object input = getInput();
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            throw new IllegalArgumentException("unsupported input type");
        }
        File dir = file.getParentFile();
        
        GeorefBounds georefBounds = GeorefBounds.create(new File(dir, GeorefBounds.FILE_NAME));
        RasterStatistics rasterStatistics = RasterStatistics.create(new File(dir, RasterStatistics.FILE_NAME));
        Header header = Header.create(new File(dir, Header.FILE_NAME));
//        CoordinateReferenceSystem crs = ProjectionReader.parsePrjFile(new File(dir, "prj.adf"));

        
        int width = MathUtils.floorInt((georefBounds.urx - georefBounds.llx) / header.pixelSizeX);
        int height = MathUtils.floorInt((georefBounds.ury - georefBounds.lly) / header.pixelSizeY);
        int numTiles = header.tilesPerColumn * header.tilesPerRow;

        TileIndex tileIndex = TileIndex.create(new File(dir, TileIndex.FILE_NAME), numTiles);
        rasterDataFile = RasterDataFile.create(new File(dir, RasterDataFile.FILE_NAME));
        
        Product product = new Product("foo", "bar", width, height);
        Dimension tileSize = new Dimension(header.tileXSize, header.tileYSize);
        product.setPreferredTileSize(tileSize);

        int productdataType = getDataType(header, rasterStatistics);
        Band band = product.addBand("band1", productdataType);
        band.setNoDataValue(getNodataValue(productdataType));
        band.setNoDataValueUsed(true);
        PlanarImage image;
        if (ProductData.isIntType(productdataType)) {
            int databufferType = ImageManager.getDataBufferType(productdataType);
            image = new IntegerCoverOpImage(width, height, tileSize, header, tileIndex, rasterDataFile, databufferType, (int)getNodataValue(productdataType));
        } else {
            image = new FloatCoverOpImage(width, height, tileSize, header, tileIndex, rasterDataFile);
        }
        band.setSourceImage(image);
         
        if (ProductData.isIntType(productdataType)) {
            band = product.addBand("type", ProductData.TYPE_INT8);
            band.setNoDataValue(42);
            band.setNoDataValueUsed(true);
            image = new IntegerTypeOpImage(width, height, tileSize, header, tileIndex, rasterDataFile);
            band.setSourceImage(image);
            IndexCoding indexCoding = new IndexCoding("type_coding");
            indexCoding.addIndex("const_block", ArcBinGridConstants.CONST_BLOCK, "");
            indexCoding.addIndex("raw_1bit", ArcBinGridConstants.RAW_1BIT, "");
            indexCoding.addIndex("raw_4bit", ArcBinGridConstants.RAW_4BIT, "");
            indexCoding.addIndex("raw_8bit", ArcBinGridConstants.RAW_8BIT, "");
            indexCoding.addIndex("raw_16bit", ArcBinGridConstants.RAW_16BIT, "");
            indexCoding.addIndex("raw_32bit", ArcBinGridConstants.RAW_32BIT, "");
            indexCoding.addIndex("run_16bit", ArcBinGridConstants.RUN_16BIT, "");
            indexCoding.addIndex("run_8bit", ArcBinGridConstants.RUN_8BIT, "");
            indexCoding.addIndex("run_min", ArcBinGridConstants.RUN_MIN, "");
            indexCoding.addIndex("rle_32bit", ArcBinGridConstants.RLE_32BIT, "");
            indexCoding.addIndex("rle_16bit", ArcBinGridConstants.RLE_16BIT, "");
            indexCoding.addIndex("rle_8bit", ArcBinGridConstants.RLE_8BIT, "");
            indexCoding.addIndex("rle_4bit", ArcBinGridConstants.RLE_4BIT, "");
            indexCoding.addIndex("ccitt", ArcBinGridConstants.CCITT, "");
            product.getIndexCodingGroup().add(indexCoding);
            band.setSampleCoding(indexCoding);
        }
        
        band = product.addBand("index", ProductData.TYPE_INT32);
        image = new TileIndexOpImage(width, height, tileSize, header);
        band.setSourceImage(image);
        
        MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(createHeaderElement(header));
        metadataRoot.addElement(createGeorefBoundsElement(georefBounds));
        if (rasterStatistics != null) {
            metadataRoot.addElement(createRasterStatisticsElement(rasterStatistics));
        }
        
        return product;
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IOException("ArcBinGridReader.readBandRasterDataImpl itentionally not implemented");
    }
    


    @Override
    public void close() throws IOException {
        super.close();
        // TODO close files
    }
    
    private int getDataType(Header header, RasterStatistics rasterStatistics) throws ProductIOException {
        int cellType = header.cellType;
        if (cellType == ArcBinGridConstants.CELL_TYPE_INT) {
            if (rasterStatistics != null && rasterStatistics.min >= 0 && rasterStatistics.max <= 254) {
                return ProductData.TYPE_UINT8;
            } else if (rasterStatistics != null && rasterStatistics.min >= -32767 && rasterStatistics.max <= 32767) {
                return ProductData.TYPE_INT16;
            } else {
                return ProductData.TYPE_INT32;
            }
        } else if (cellType == ArcBinGridConstants.CELL_TYPE_FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else {
            throw new ProductIOException("Unsupported data type: "+cellType);
        }
    }

    private double getNodataValue(int dataType) throws ProductIOException {
        if (dataType == ProductData.TYPE_FLOAT32) {
            return ArcBinGridConstants.NODATA_VALUE_FLOAT;
        } else if (dataType == ProductData.TYPE_UINT8) {
            return 255;
        } else if (dataType == ProductData.TYPE_INT16) {
            return Short.MIN_VALUE;
        } else if (dataType == ProductData.TYPE_INT32) {
            return -2147483647; // taken from gdal
        } else {
            throw new ProductIOException("Unsupported data type: "+dataType);
        }
    }
    
    private MetadataElement createHeaderElement(Header header) {
        MetadataElement elem = new MetadataElement("Header");
        elem.addAttribute(createIntAttr("cellType", header.cellType, "1 = int cover, 2 = float cover."));
        elem.addAttribute(createDoubleAttr("pixelSizeX", header.pixelSizeX, "Width of a pixel in georeferenced coordinates."));
        elem.addAttribute(createDoubleAttr("pixelSizeY", header.pixelSizeY, "Height of a pixel in georeferenced coordinates."));
        elem.addAttribute(createDoubleAttr("xRef", header.xRef, null));
        elem.addAttribute(createDoubleAttr("yRef", header.yRef, null));
        elem.addAttribute(createIntAttr("tilesPerRow", header.tilesPerRow, "The width of the file in tiles."));
        elem.addAttribute(createIntAttr("tilesPerColumn", header.tilesPerColumn, "The height of the file in tiles. Note this may be much more than the number of tiles actually represented in the index file."));
        elem.addAttribute(createIntAttr("tileXSize", header.tileXSize, "The width of a file in pixels. Normally 256."));
        elem.addAttribute(createIntAttr("tileYSize", header.tileYSize, "Height of a tile in pixels, usually 4."));
        return elem;
    }
    
    private MetadataElement createGeorefBoundsElement(GeorefBounds georefBounds) {
        MetadataElement elem = new MetadataElement("GeorefBounds");
        elem.addAttribute(createDoubleAttr("llx", georefBounds.llx, "Lower left X (easting) of the grid."));
        elem.addAttribute(createDoubleAttr("lly", georefBounds.lly, "Lower left Y (northing) of the grid."));
        elem.addAttribute(createDoubleAttr("urx", georefBounds.urx, "Upper right X (northing) of the grid."));
        elem.addAttribute(createDoubleAttr("ury", georefBounds.ury, "Upper right Y (northing) of the grid."));
        return elem;
    }
    
    private MetadataElement createRasterStatisticsElement(RasterStatistics rasterStat) {
        MetadataElement elem = new MetadataElement("RasterStatistics");
        elem.addAttribute(createDoubleAttr("min", rasterStat.min, "Minimum value of a raster cell in this grid."));
        elem.addAttribute(createDoubleAttr("max", rasterStat.max, "Maximum value of a raster cell in this grid."));
        elem.addAttribute(createDoubleAttr("mean", rasterStat.mean, "Mean value of a raster cells in this grid."));
        elem.addAttribute(createDoubleAttr("stddev", rasterStat.stddev, "Standard deviation of raster cells in this grid."));
        return elem;
    }

    private MetadataAttribute createIntAttr(String name, int value, String desc) {
        ProductData productData = ProductData.createInstance(new int[] {value});
        MetadataAttribute attribute = new MetadataAttribute(name, productData, true);
        if (desc != null) {
            attribute.setDescription(desc);
        }
        return attribute;
    }
    
    private MetadataAttribute createDoubleAttr(String name, double value, String desc) {
        ProductData productData = ProductData.createInstance(new double[] {value});
        MetadataAttribute attribute = new MetadataAttribute(name, productData, true);
        if (desc != null) {
            attribute.setDescription(desc);
        }
        return attribute;
    }
}
