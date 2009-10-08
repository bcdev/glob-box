package org.esa.glob.reader.globcover;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
// the tileFileMap is unmodifiable
@SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter"})
class GCMosaicMultiLevelSource extends AbstractMultiLevelSource {

    private final Band band;
    private final Map<TileIndex, GCTileFile> tileFileMap;

    GCMosaicMultiLevelSource(MultiLevelModel multiLevelModel, Band band,
                             Map<TileIndex, GCTileFile> tileFileMap) {
        super(multiLevelModel);
        this.band = band;
        this.tileFileMap = tileFileMap;
    }

    @Override
    protected RenderedImage createImage(int level) {
        final MultiLevelModel model = getModel();
        final double scale = model.getScale(level);
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        int levelWidth = (int) Math.floor(sourceWidth / scale);
        int levelHeight = (int) Math.floor(sourceHeight / scale);
        final int dataType = ImageManager.getDataBufferType(band.getDataType());

        int tileWidth = levelWidth / (TileIndex.MAX_HORIZ_INDEX + 1);
        int tileHeight = levelHeight / (TileIndex.MAX_VERT_INDEX + 1);
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, tileWidth, tileHeight);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        final ImageLayout imageLayout = new ImageLayout(0, 0,
                                                        levelWidth,
                                                        levelHeight,
                                                        0, 0,
                                                        tileWidth,
                                                        tileHeight,
                                                        sampleModel,
                                                        colorModel);
        return new GCMosaicOpImage(imageLayout, tileFileMap, (int) scale);
    }

    private class GCMosaicOpImage extends SourcelessOpImage {

        private final Map<TileIndex, GCTileFile> tileMap;
        private final int subsampling;
        private WritableRaster noDataRaster;

        GCMosaicOpImage(ImageLayout imageLayout, Map<TileIndex, GCTileFile> tileMap, int subsampling) {
            super(imageLayout, null,
                  imageLayout.getSampleModel(null),
                  imageLayout.getMinX(null),
                  imageLayout.getMinY(null),
                  imageLayout.getWidth(null),
                  imageLayout.getHeight(null));
            this.tileMap = tileMap;
            this.subsampling = subsampling;
        }

        @Override
        public Raster computeTile(int tileX, int tileY) {
            final Point location = new Point(tileXToX(tileX), tileYToY(tileY));
            final TileIndex tileIndex = new TileIndex(tileX, tileY);
            final GCTileFile file = tileMap.get(tileIndex);
            if (file == null) {
                if(noDataRaster == null) {
                    noDataRaster = createNoDataRaster();
                }
                return noDataRaster.createTranslatedChild(location.x, location.y);
            }
            try {
                final WritableRaster targetRaster = createWritableRaster(sampleModel, location);
                final DataBuffer dataBuffer = targetRaster.getDataBuffer();
                int subsampledSize = (int) Math.sqrt(dataBuffer.getSize());
                int sourceSize = subsampledSize * subsampling;
                final Array array = file.readData(band.getName(), 0, 0,
                                                  sourceSize, sourceSize,
                                                  subsampling, subsampling);
                targetRaster.setDataElements(targetRaster.getSampleModelTranslateX(),
                                             targetRaster.getSampleModelTranslateY(),
                                             targetRaster.getWidth(),
                                             targetRaster.getHeight(),
                                             array.getStorage());
                return targetRaster;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read mosaic tile.", e);
            } catch (InvalidRangeException e) {
                throw new RuntimeException("Failed to read mosaic tile.", e);
            }
        }

        private WritableRaster createNoDataRaster() {
            final WritableRaster raster = createWritableRaster(sampleModel, new Point(0, 0));
            final DataBuffer dataBuffer = raster.getDataBuffer();
            final double noDataValue = band.getNoDataValue();
            for (int i = 0; i < dataBuffer.getSize(); i++) {
                dataBuffer.setElemDouble(i, noDataValue);
            }
            return raster;
        }
    }
}
