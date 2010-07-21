package org.esa.beam.dataio.globcover;

import com.bc.ceres.jai.NoDataRaster;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.MathUtils;
import ucar.ma2.Array;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class GCMosaicOpImage extends SingleBandedOpImage {

    private final Map<TileIndex, GCTileFile> tileMap;
    private final double noDataValue;
    private final String variableName;
    private NoDataRaster noDataTargetRaster;
    private WritableRaster noDataSourceRaster;

    GCMosaicOpImage(Band band, Map<TileIndex, GCTileFile> fileMap, ResolutionLevel level) {
        this(ImageManager.getDataBufferType(band.getDataType()),
             band.getSceneRasterWidth(), band.getSceneRasterHeight(),
             fileMap, band.getName(), band.getNoDataValue(), level);
    }

    private GCMosaicOpImage(int dataBufferType, int sourceWidth, int sourceHeight,
                    Map<TileIndex, GCTileFile> fileMap,
                    String name, double noDataValue, ResolutionLevel level) {
        super(dataBufferType, sourceWidth, sourceHeight,
              JAIUtils.computePreferredTileSize(sourceWidth, sourceHeight, 1),
              null, level);
        this.tileMap = fileMap;
        this.variableName = name;
        this.noDataValue = noDataValue;

    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        Rectangle targetRect = getTileRect(tileX, tileY);
        Rectangle rect = getSourceRect(targetRect);
        TileIndex[] indexes = getAffectedSourceTiles(rect);
        for (TileIndex index : indexes) {
            final GCTileFile file = tileMap.get(index);
            if (file != null) {
                return super.computeTile(tileX, tileY);
            }
        }
        return getNoDataTargetTile(new Point(tileXToX(tileX), tileYToY(tileY)));

    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle targetRect) {
        Rectangle sourceRect = getSourceRect(targetRect);
        TileIndex[] indexes = getAffectedSourceTiles(sourceRect);
        for (TileIndex index : indexes) {
            Rectangle sourceTileRect = index.getBounds();
            Rectangle sourceRectToRead = sourceRect.intersection(sourceTileRect);
            if (sourceRectToRead.isEmpty()) {
                continue;
            }
            final GCTileFile tileFile = tileMap.get(index);
            if (tileFile != null) {
                sourceRectToRead.translate(-sourceTileRect.x, -sourceTileRect.y);
                Object data = readRectangle(tileFile, sourceRectToRead, MathUtils.floorInt(getScale()));
                sourceRectToRead.translate(sourceTileRect.x, sourceTileRect.y);
                Rectangle destRect = getDestRect(sourceRectToRead);
                writableRaster.setDataElements(destRect.x, destRect.y,
                                               destRect.width, destRect.height,
                                               data);
            } else {
                Rectangle destRect = getDestRect(sourceRectToRead);
                WritableRaster raster = getNoDataSourceTile(destRect);
                writableRaster.setRect(raster);
            }

        }

    }

    private Object readRectangle(GCTileFile tileFile, Rectangle sourceRect, int subsampling) {
        try {
            Array array = tileFile.readData(variableName,
                                            sourceRect.x,
                                            sourceRect.y,
                                            sourceRect.width, sourceRect.height,
                                            subsampling, subsampling);
            return array.getStorage();
        } catch (Exception e) {
            throw new RuntimeException("Could not read " + tileFile, e);
        }
    }

    private Rectangle getSourceRect(Rectangle rect) {
        int sourceX = getSourceX(rect.x);
        int sourceY = getSourceY(rect.y);
        int sourceWidth = getSourceWidth(rect.width);
        int sourceHeight = getSourceHeight(rect.height);
        return new Rectangle(sourceX, sourceY, sourceWidth, sourceHeight);
    }

    private Rectangle getDestRect(Rectangle rect) {
        int destX0 = MathUtils.floorInt(rect.x / getScale());
        int destY0 = MathUtils.floorInt(rect.y / getScale());
        int destX1 = MathUtils.ceilInt(rect.width / getScale());
        int destY1 = MathUtils.ceilInt(rect.height / getScale());
        return new Rectangle(destX0, destY0, destX1, destY1);
    }

    private TileIndex[] getAffectedSourceTiles(Rectangle rect) {
        final int indexX0 = rect.x / TileIndex.TILE_SIZE;
        final int indexY0 = rect.y / TileIndex.TILE_SIZE;
        final int indexWidth = ((rect.x + rect.width) / TileIndex.TILE_SIZE) + 1;
        final int indexHeight = ((rect.y + rect.height) / TileIndex.TILE_SIZE) + 1;
        List<TileIndex> indexes = new ArrayList<TileIndex>();
        for (int y = indexY0; y < indexHeight; y++) {
            for (int x = indexX0; x < indexWidth; x++) {
                indexes.add(new TileIndex(x, y));
            }
        }
        return indexes.toArray(new TileIndex[indexes.size()]);
    }

    private NoDataRaster getNoDataTargetTile(Point location) {
        if (noDataTargetRaster == null) {
            noDataTargetRaster = createNoDataRaster(noDataValue);
        }
        return noDataTargetRaster.createTranslatedChild(location.x, location.y);
    }

    private WritableRaster getNoDataSourceTile(Rectangle rectangle) {
        if (noDataSourceRaster == null) {
            noDataSourceRaster = createNoDataSourceRaster(noDataValue);
        }
        return noDataSourceRaster.createWritableChild(0, 0,
                                                      rectangle.width, rectangle.height,
                                                      rectangle.x, rectangle.y,
                                                      new int[]{0});
    }

    private WritableRaster createNoDataSourceRaster(double noDataValue) {
        SampleModel sampleModel1 = getSampleModel().createCompatibleSampleModel(TileIndex.TILE_SIZE,
                                                                                TileIndex.TILE_SIZE);
        final WritableRaster raster = createWritableRaster(sampleModel1, new Point(0, 0));
        final DataBuffer buffer = raster.getDataBuffer();

        for (int i = 0; i < buffer.getSize(); i++) {
            buffer.setElemDouble(i, noDataValue);
        }

        return raster;
    }

}
