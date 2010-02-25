package org.esa.glob.reader.globcover;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.RenderedImage;
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
    private final Area coveredModelArea;

    GCMosaicMultiLevelSource(Band band, Map<TileIndex, GCTileFile> tileFileMap, Area coveredImageArea) {
        super(new DefaultMultiLevelModel(ImageManager.getImageToModelTransform(band.getGeoCoding()),
                                         band.getSceneRasterWidth(), band.getSceneRasterHeight()));
        this.band = band;
        this.tileFileMap = tileFileMap;
        final AffineTransform i2mTransform = ImageManager.getImageToModelTransform(band.getGeoCoding());
        coveredModelArea = coveredImageArea.createTransformedArea(i2mTransform);
    }

    @Override
    protected RenderedImage createImage(int level) {
        return new GCMosaicOpImage(band, tileFileMap, ResolutionLevel.create(getModel(), level));
    }

    @Override
    public Shape getImageShape(int level) {
        return coveredModelArea.createTransformedArea(getModel().getModelToImageTransform(level));
    }
}
