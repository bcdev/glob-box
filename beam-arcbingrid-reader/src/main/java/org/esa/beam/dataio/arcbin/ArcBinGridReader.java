package org.esa.beam.dataio.arcbin;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.MathUtils;

import java.io.File;
import java.io.IOException;


public class ArcBinGridReader extends AbstractProductReader {
 

    private int width;
    private int height;
    private int numTiles;
    private TileIndex tileIndex;
    private int dataType;
    private HdrAdf hdrAdf;
    private RasterData rasterData;

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
        
        DblbndAdf dblbndAdf = DblbndAdf.create(new File(dir, DblbndAdf.FILE_NAME));
        hdrAdf = HdrAdf.create(new File(dir, HdrAdf.FILE_NAME));
        int cellType = hdrAdf.cellType;
        if (cellType == 1) {
            dataType = ProductData.TYPE_INT32;
        }else if (cellType == 2) {
            dataType = ProductData.TYPE_INT32;
        } else {
            throw new ProductIOException("unsupported data type");
        }
        
        width = MathUtils.floorInt((dblbndAdf.urx - dblbndAdf.llx) / hdrAdf.pixelSizeX);
        height = MathUtils.floorInt((dblbndAdf.ury - dblbndAdf.lly) / hdrAdf.pixelSizeY);
        System.out.println("pixels width " + width);
        System.out.println("rows height  " + height);
        
        numTiles = hdrAdf.tilesPerColumn * hdrAdf.tilesPerRow;
        System.out.println("numTiles  " + numTiles);
        
        tileIndex = TileIndex.create(new File(dir, TileIndex.FILE_NAME), numTiles);
        rasterData = RasterData.create(new File(dir, RasterData.FILE_NAME));
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

//        int startTileX = sourceOffsetX / hdrAdf.tileXSize ;
//        int startTileY = sourceOffsetY / hdrAdf.tileYSize;
//        int endTileX = (sourceOffsetX + sourceWidth - 1) / hdrAdf.tileXSize;
//        int endTileY = (sourceOffsetY + sourceHeight - 1) / hdrAdf.tileYSize;
        
        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            int tileIndex = -1;
            int tileType = 0;
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                int tileIndexY = (y / hdrAdf.tileYSize) * hdrAdf.tilesPerRow;
                for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                    int currentTileIndex = (x / hdrAdf.tileXSize) + tileIndexY;
                    final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                    IndexEntry indexEntry = this.tileIndex.getIndexEntry(currentTileIndex);
                    if (indexEntry == null ) {
                        destBuffer.setElemDoubleAt(rasterIndex, -9999);
                    } else {
                        if (tileIndex != currentTileIndex) {
                            tileType = rasterData.getTileType(indexEntry.offset);
                            tileIndex = currentTileIndex;
                        }
                        destBuffer.setElemDoubleAt(rasterIndex, tileType);
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
        
        Product product = new Product("foo", "bar", width, height);
        Band band = product.addBand("grid", dataType);
        band.setNoDataValue(-9999);
        band.setNoDataValueUsed(true);
        return product;
        
    }
}
