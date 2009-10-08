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
        MetadataElement headerElement = new MetadataElement("Header");
        headerElement.addAttribute(createMetadataAttributeInt("cellType", header.cellType));
        headerElement.addAttribute(createMetadataAttributeDouble("pixelSizeX", header.pixelSizeX));
        headerElement.addAttribute(createMetadataAttributeDouble("pixelSizeY", header.pixelSizeY));
        headerElement.addAttribute(createMetadataAttributeDouble("xRef", header.xRef));
        headerElement.addAttribute(createMetadataAttributeDouble("yRef", header.yRef));
        headerElement.addAttribute(createMetadataAttributeInt("tilesPerRow", header.tilesPerRow));
        headerElement.addAttribute(createMetadataAttributeInt("tilesPerColumn", header.tilesPerColumn));
        headerElement.addAttribute(createMetadataAttributeInt("tileXSize", header.tileXSize));
        headerElement.addAttribute(createMetadataAttributeInt("tileYSize", header.tileYSize));
        return headerElement;
    }
    
    private MetadataElement createGeorefBoundsElement(GeorefBounds georefBounds) {
        MetadataElement georefBoundsElement = new MetadataElement("GeorefBounds");
        georefBoundsElement.addAttribute(createMetadataAttributeDouble("llx", georefBounds.llx));
        georefBoundsElement.addAttribute(createMetadataAttributeDouble("lly", georefBounds.lly));
        georefBoundsElement.addAttribute(createMetadataAttributeDouble("urx", georefBounds.urx));
        georefBoundsElement.addAttribute(createMetadataAttributeDouble("ury", georefBounds.ury));
        return georefBoundsElement;
    }
    
    private MetadataElement createRasterStatisticsElement(RasterStatistics rasterStatistics) {
        MetadataElement rasterStatisticsElement = new MetadataElement("RasterStatistics");
        rasterStatisticsElement.addAttribute(createMetadataAttributeDouble("min", rasterStatistics.min));
        rasterStatisticsElement.addAttribute(createMetadataAttributeDouble("max", rasterStatistics.max));
        rasterStatisticsElement.addAttribute(createMetadataAttributeDouble("mean", rasterStatistics.mean));
        rasterStatisticsElement.addAttribute(createMetadataAttributeDouble("stddev", rasterStatistics.stddev));
        return rasterStatisticsElement;
    }

    private MetadataAttribute createMetadataAttributeInt(String name, int value) {
        ProductData productData = ProductData.createInstance(new int[] {value});
        return new MetadataAttribute(name, productData, true);
    }
    
    private MetadataAttribute createMetadataAttributeDouble(String name, double value) {
        ProductData productData = ProductData.createInstance(new double[] {value});
        return new MetadataAttribute(name, productData, true);
    }
}
