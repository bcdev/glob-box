package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Rendering;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;

/**
 * User: Thomas Storm
 * Date: 08.07.2010
 * Time: 10:15:10
 */
public class BlendImageLayer extends ImageLayer {

    private ImageLayer firstImageLayer;
    private ImageLayer secondImageLayer;
    private float blendFactor;

    public BlendImageLayer(MultiLevelSource firstMultiLevelSource, MultiLevelSource secondMultiLevelSource) {
        super(firstMultiLevelSource);
        blendFactor = 0.0f;
        firstImageLayer = new ImageLayer(firstMultiLevelSource);
        secondImageLayer = new ImageLayer(secondMultiLevelSource);
    }

    public MultiLevelSource getSecondMultiLevelSource() {
        return secondImageLayer.getMultiLevelSource();
    }

    public void setBlendFactor(float factor) {
        blendFactor = factor;
    }

    /**
     * Returns the image of the first layer
     */
    @Override
    public RenderedImage getImage(int level) {
        return firstImageLayer.getImage(level);
    }


    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D graphics = rendering.getGraphics();
//        System.out.println("blendFactor = " + blendFactor);
        final Composite oldComposite = graphics.getComposite();
        try {
            final float layerAlpha = 1 - (float) getTransparency();
            final float firstLayerAlpha = (1 - blendFactor) * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, firstLayerAlpha));
            if (firstLayerAlpha != 0) {
                firstImageLayer.render(rendering);
            }
            final float secondLayerAlpha = blendFactor * layerAlpha;
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, secondLayerAlpha));
            if (secondLayerAlpha != 0) {
                secondImageLayer.render(rendering);
            }
        } finally {
            graphics.setComposite(oldComposite);
        }
    }

    @Override
    public void regenerate() {
        // do nothing
        System.out.printf("");
    }

}

