package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 18.06.2010
 * Time: 15:48:31
 */
public class TimeSeriesPlayerToolView extends AbstractToolView {

    public static final String TIME_PROPERTY = "timeProperty";

    private static final String NEXT_IMAGE_LAYER = "nextImageLayer";

    private final SceneViewListener sceneViewListener;
    private final ProductNodeListener productNodeListener;

    private ProductSceneView currentView;
    private TimeSeriesPlayerForm form;

    public TimeSeriesPlayerToolView() {
        sceneViewListener = new SceneViewListener();
        productNodeListener = new TimeSeriesProductNodeListener();
    }

    @Override
    public void componentShown() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void componentHidden() {
        VisatApp.getApp().removeInternalFrameListener(sceneViewListener);
    }

    @Override
    protected JComponent createControl() {
        form = new TimeSeriesPlayerForm();
        form.getTimeSlider().addChangeListener(new SliderChangeListener());
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }
        return form;
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != newView) {
            if (currentView != null) {
                currentView.getProduct().removeProductNodeListener(productNodeListener);
            }
            currentView = newView;
            form.setView(currentView);
            if (currentView != null) {
                form.setTimeSeries(TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct()));
                currentView.getProduct().addProductNodeListener(productNodeListener);
                form.configureTimeSlider(currentView.getRaster());
            } else {
                form.setTimeSeries(null);
                form.configureTimeSlider(null);
                form.getTimer().stop();
            }
        }
    }

    // todo (mp) - The following should be done on ProductSceneView.setRasters()

    private void exchangeRasterInProductSceneView(Band nextRaster, ProductSceneView sceneView) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = sceneView.getRaster();
        final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
        nextRaster.setImageInfo(imageInfoClone);
        reconfigureBaseImageLayer(nextRaster, currentView);
        sceneView.setRasters(new RasterDataNode[]{nextRaster});
        sceneView.setImageInfo(imageInfoClone.createDeepCopy());
        VisatApp.getApp().getSelectedInternalFrame().setTitle(nextRaster.getDisplayName());
    }

    private void reconfigureBaseImageLayer(RasterDataNode rasterDataNode, ProductSceneView sceneView) {
        final Layer rootLayer = sceneView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        MultiLevelSource multiLevelSource;
        final ImageLayer nextLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer, NEXT_IMAGE_LAYER);
        if (nextLayer != null) {
            multiLevelSource = nextLayer.getMultiLevelSource();

            final List<Layer> children = rootLayer.getChildren();
            final int baseIndex = children.indexOf(baseImageLayer);
            children.remove(baseIndex);
            children.remove(nextLayer);
            nextLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
            nextLayer.setName(rasterDataNode.getDisplayName());
            children.add(baseIndex, nextLayer);
            nextLayer.setTransparency(0);
        } else {
            // todo use a real ProgressMonitor
            multiLevelSource = BandImageMultiLevelSource.create(rasterDataNode, ProgressMonitor.NULL);

            baseImageLayer.setMultiLevelSource(multiLevelSource);
            baseImageLayer.setTransparency(0);
        }

        // TODO why this code  ????
//        baseImageLayer.getConfiguration().setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, rasterDataNode);
//        baseImageLayer.getConfiguration().setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
//        baseImageLayer.setName(rasterDataNode.getDisplayName());
//        try {
//            // todo add comment: what does this code do?
//            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
//            sceneImageField.setAccessible(true);
//            final Object sceneImage = sceneImageField.get(sceneView);
//            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
//            multiLevelSourceField.setAccessible(true);
//            multiLevelSourceField.set(sceneImage, multiLevelSource);
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
    }

    private void changeTransparency(RasterDataNode nextRaster, float transparency) {
        final Layer rootLayer = currentView.getRootLayer();

        ImageLayer nextImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer, NEXT_IMAGE_LAYER);
        if (nextImageLayer == null) {
            RasterDataNode currentRaster = currentView.getRaster();
            final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
            nextRaster.setImageInfo(imageInfoClone);

            final BandImageMultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(nextRaster,
                                                                                                ProgressMonitor.NULL);
            nextImageLayer = new ImageLayer(multiLevelSource);
            nextImageLayer.setId(NEXT_IMAGE_LAYER);
            rootLayer.getChildren().add(nextImageLayer);
        }

        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        nextImageLayer.setTransparency(1 - transparency);
        baseImageLayer.setTransparency(transparency);
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (currentView != view && !view.isRGB() && viewProductType.equals(
                        AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class SliderChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (currentView == null) {
                return;
            }
            final int currentValue = form.getTimeSlider().getValue();
            int stepsPerTimespan = form.getStepsPerTimespan();
            final float transparency = (currentValue % stepsPerTimespan) / (float) stepsPerTimespan;
            final int currentBandIndex = currentValue / stepsPerTimespan;
            final int newBandIndex = MathUtils.ceilInt(currentValue / (float) stepsPerTimespan);

            final List<Band> bandList = form.getBandList(currentView.getRaster());
            if (currentBandIndex == newBandIndex) {
                final Band newRaster = bandList.get(newBandIndex);
                exchangeRasterInProductSceneView(newRaster, currentView);
                currentView.firePropertyChange(TIME_PROPERTY, -1, newBandIndex);
            } else {
                if (bandList.size() > currentBandIndex + 1) {
                    changeTransparency(bandList.get(currentBandIndex + 1), transparency);
                }
            }
        }
    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                form.configureTimeSlider(currentView.getRaster());
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                form.configureTimeSlider(currentView.getRaster());
            }
        }
    }

}
