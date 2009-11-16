package org.esa.glob.reader.globcover;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;

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

    GCMosaicMultiLevelSource(Band band, Map<TileIndex, GCTileFile> tileFileMap) {
        super(new DefaultMultiLevelModel(ImageManager.getImageToModelTransform(band.getGeoCoding()),
                                         band.getSceneRasterWidth(), band.getSceneRasterHeight()));
        this.band = band;
        this.tileFileMap = tileFileMap;
    }

    @Override
    protected RenderedImage createImage(int level) {
        return new GCMosaicOpImage(band, tileFileMap, ResolutionLevel.create(getModel(), level));
    }
}
