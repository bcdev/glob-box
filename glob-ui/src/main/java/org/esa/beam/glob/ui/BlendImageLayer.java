package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Rendering;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * User: Thomas Storm
 * Date: 08.07.2010
 * Time: 10:15:10
 */
public class BlendImageLayer extends ImageLayer {

    private ImageLayer baseLayer;
    private ImageLayer blendLayer;
    private float blendFactor;

    public BlendImageLayer(MultiLevelSource baseMultiLevelSource, MultiLevelSource blendMultiLevelSource) {
        super(DefaultMultiLevelSource.NULL);
        blendFactor = 0.0f;
        baseLayer = new ImageLayer(baseMultiLevelSource);
        blendLayer = new ImageLayer(blendMultiLevelSource);
    }

    public void setBlendFactor(float factor) {
        blendFactor = factor;
    }

    @Override
    public MultiLevelSource getMultiLevelSource() {
        return getBaseMultiLevelSource();
    }

    public BandImageMultiLevelSource getBaseMultiLevelSource() {
        return (BandImageMultiLevelSource) baseLayer.getMultiLevelSource();
    }

    @Override
    public AffineTransform getImageToModelTransform(int level) {
        return baseLayer.getImageToModelTransform(level);
    }

    @Override
    public AffineTransform getModelToImageTransform(int level) {
        return baseLayer.getModelToImageTransform(level);
    }

    /**
     * Returns the image of the base layer
     */
    @Override
    public RenderedImage getImage(int level) {
        return baseLayer.getImage(level);
    }


    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D graphics = rendering.getGraphics();
        final Composite oldComposite = graphics.getComposite();
        try {
            final float layerAlpha = 1 - (float) getTransparency();
            final float baseLayerAlpha = (1 - blendFactor) * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseLayerAlpha));
            if (baseLayerAlpha != 0) {
                baseLayer.render(rendering);
            }
            final float blendLayerAlpha = blendFactor * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blendLayerAlpha));
            if (blendLayerAlpha != 0) {
                blendLayer.render(rendering);
            }
        } finally {
            graphics.setComposite(oldComposite);
        }
    }

    @Override
    public void regenerate() {
        baseLayer.regenerate();
        blendLayer.regenerate();
    }

    public void swap(BandImageMultiLevelSource multiLevelSource, boolean forward) {
        if (forward) {
            baseLayer = blendLayer;
            blendLayer = new ImageLayer(multiLevelSource);
        } else {
            blendLayer = baseLayer;
            baseLayer = new ImageLayer(multiLevelSource);
        }
    }

}

