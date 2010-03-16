package org.esa.beam.glob.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.RasterImageLayerType;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.GlobBox;
import org.esa.beam.visat.VisatApp;

import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

class GlobToolboxManagerForm extends JPanel {

    private GlobToolboxManagerFormModel model;

    private JSlider timeSlider;
    private final GlobToolboxManagerForm.SliderChangeListener sliderChangeListener;

    GlobToolboxManagerForm(GlobToolboxManagerFormModel model) {
        this.model = model;
        sliderChangeListener = new SliderChangeListener();
        createComponents(model.getPropertySet());
    }


    private void createComponents(PropertySet propertySet) {
        setPreferredSize(new Dimension(300, 200));
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setRowWeightY(4, 1.0);
        setLayout(tableLayout);

        JCheckBox showWorldMapChecker = new JCheckBox("Show world map layer");
        JCheckBox syncColorChecker = new JCheckBox("Synchronise colour information");
        JCheckBox useAlphaBlendingChecker = new JCheckBox("Use transparency blending");
        timeSlider = new JSlider(JSlider.HORIZONTAL);
        timeSlider.setPaintLabels(true);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintTrack(true);
        timeSlider.addChangeListener(sliderChangeListener);
        configureTimeSlider();
        add(showWorldMapChecker);
        add(syncColorChecker);
//        add(useAlphaBlendingChecker);  todo - not yet implemented
        add(timeSlider);
        add(tableLayout.createVerticalSpacer());

        final BindingContext context = new BindingContext(propertySet);
        context.bind(GlobToolboxManagerFormModel.PROPERTY_NAME_WORLDMAP, showWorldMapChecker);
        context.bind(GlobToolboxManagerFormModel.PROPERTY_NAME_SYNCCOLOR, syncColorChecker);
        context.bind(GlobToolboxManagerFormModel.PROPERTY_NAME_BLENDING, useAlphaBlendingChecker);

        final WorldMapHandler worldMapHandler = new WorldMapHandler();
        context.addPropertyChangeListener(GlobToolboxManagerFormModel.PROPERTY_NAME_WORLDMAP, worldMapHandler);
        context.addPropertyChangeListener(GlobBox.CURRENT_VIEW_PROPERTY, worldMapHandler);

        final ColorSynchronizer colorSynchronizer = new ColorSynchronizer();
        context.addPropertyChangeListener(GlobToolboxManagerFormModel.PROPERTY_NAME_SYNCCOLOR, colorSynchronizer);
        context.addPropertyChangeListener(GlobBox.CURRENT_VIEW_PROPERTY, colorSynchronizer);

        final SliderUpdater sliderUpdater = new SliderUpdater();
        GlobBox.getInstance().addPropertyChangeListener(GlobBox.RASTER_LIST_PROPERTY, sliderUpdater);
        context.addPropertyChangeListener(GlobBox.CURRENT_VIEW_PROPERTY, sliderUpdater);
    }

    private void configureTimeSlider() {
        final List<RasterDataNode> rasterList = model.getCurrentRasterList();

        timeSlider.setMinimum(0);
        final int maximum = rasterList.size() - 1;
        timeSlider.setMaximum(maximum);
        timeSlider.setInverted(true);

        if (!rasterList.isEmpty()) {
            timeSlider.setEnabled(true);
            final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            for (int i = 0; i < rasterList.size(); i++) {
                final ProductData.UTC utcStartTime = rasterList.get(i).getProduct().getStartTime();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                dateFormat.setCalendar(utcStartTime.getAsCalendar());
                final String dateText = dateFormat.format(utcStartTime.getAsDate());
                final String timeText = timeFormat.format(utcStartTime.getAsDate());
                String labelText = String.format("<html><p align=\"center\"> <font size=\"2\">%s<br>%s</font></p>",
                                                 dateText, timeText);
                labelTable.put(i, new JLabel(labelText));
            }
            timeSlider.setLabelTable(labelTable);
        } else {
            timeSlider.setEnabled(false);
        }

        timeSlider.setValue(maximum);
    }

    private class WorldMapHandler implements PropertyChangeListener {

        private Layer layer;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            ProductSceneView currentView = model.getCurrentView();
            if (currentView != null) {
                if (layer == null) {
                    final LayerType type = LayerTypeRegistry.getLayerType("BlueMarbleLayerType");
                    layer = type.createLayer(currentView, type.createLayerConfig(currentView));
                }
                if (LayerUtils.getChildLayerById(currentView.getRootLayer(), layer.getId()) != null) {
                    if (!model.isShowingWorldMapLayer()) {
                        currentView.getRootLayer().getChildren().remove(layer);
                    }
                } else {
                    if (model.isShowingWorldMapLayer()) {
                        layer.setVisible(true);
                        currentView.getRootLayer().getChildren().add(layer);
                    }
                }
            }
        }
    }

    private class ColorSynchronizer implements PropertyChangeListener {

        private GlobToolboxManagerForm.ColorSynchronizer.ImageInfoListener listener;
        private RasterDataNode currentRaster;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final ProductSceneView currentView = model.getCurrentView();
            if (currentView != null) {
                final List<RasterDataNode> rasterList = model.getCurrentRasterList();
                if (model.isSynchronizingColorInformation()) {
                    currentRaster = currentView.getRaster();
                    transferImageInfo(currentRaster, rasterList);
                    listener = new ImageInfoListener(currentRaster);
                    currentRaster.getProduct().addProductNodeListener(listener);
                } else {
                    if (currentRaster != null) {
                        currentRaster.getProduct().removeProductNodeListener(listener);
                    }
                }
            } else {
                if (currentRaster != null && listener != null) {
                    currentRaster.getProduct().removeProductNodeListener(listener);
                }
                currentRaster = null;
                listener = null;
            }
        }

        private void transferImageInfo(final RasterDataNode referenceRaster, final List<RasterDataNode> rasterList) {
            ProgressMonitorSwingWorker pmsw = new ProgressMonitorSwingWorker<Void, RasterDataNode>(
                    GlobToolboxManagerForm.this,
                    "Synchronising colour information") {
                @Override
                protected Void doInBackground(ProgressMonitor pm) throws Exception {
                    pm.beginTask("Synchronising...", (rasterList.size() - 1) * 2);
                    try {
                        for (RasterDataNode raster : rasterList) {
                            if (raster != referenceRaster) {
                                // just to trigger computation
                                raster.getImageInfo(new SubProgressMonitor(pm, 1));
                                raster.getStx(false, new SubProgressMonitor(pm, 1));
                                publish(raster);
                            }
                        }
                    } finally {
                        pm.done();
                    }
                    return null;
                }

                @Override
                protected void process(List<RasterDataNode> chunks) {
                    final ColorPaletteDef colorDef = referenceRaster.getImageInfo().getColorPaletteDef();
                    for (RasterDataNode raster : chunks) {
                        if (raster != referenceRaster) {
                            applyColorPaletteDef(raster, colorDef);
                        }
                    }

                }

                @Override
                protected void done() {
                    updateLayerCollection(referenceRaster);
                }
            };
            pmsw.executeWithBlocking();
        }

        private void updateLayerCollection(RasterDataNode referenceRaster) {
            // todo this block should be moved to CollectionLayer
            final CollectionLayer collectionLayer = model.getLayerGroup();
            if (collectionLayer != null) {
                final List<Layer> children = collectionLayer.getChildren();
                final ImageInfo imageInfo = referenceRaster.getImageInfo();
                for (Layer child : children) {
                    if (child instanceof ImageLayer) {
                        ImageLayer imageLayer = (ImageLayer) child;
                        final MultiLevelSource source = imageLayer.getMultiLevelSource();
                        if (source instanceof BandImageMultiLevelSource) {
                            BandImageMultiLevelSource multiLevelSource = (BandImageMultiLevelSource) source;
                            multiLevelSource.setImageInfo(imageInfo);
                        }
                    }
                    child.regenerate();
                }
            }
        }

        private void applyColorPaletteDef(RasterDataNode targetRaster, ColorPaletteDef refColorDef) {
            final ImageInfo targetImageInfo = targetRaster.getImageInfo();
            final ColorPaletteDef.Point[] targetPoints = targetImageInfo.getColorPaletteDef().getPoints();

            if (!Arrays.equals(targetPoints, refColorDef.getPoints())) {
                if (isIndexCoded(targetRaster)) {
                    targetImageInfo.setColors(refColorDef.getColors());
                } else {
                    Stx stx = targetRaster.getStx();
                    targetImageInfo.setColorPaletteDef(refColorDef,
                                                       targetRaster.scale(stx.getMin()),
                                                       targetRaster.scale(stx.getMax()),
                                                       false);
                }
                targetRaster.fireImageInfoChanged();
                updateSceneView(targetRaster, targetImageInfo);
            }
        }

        private void updateSceneView(RasterDataNode targetRaster, ImageInfo targetImageInfo) {
            final VisatApp app = VisatApp.getApp();
            app.updateImages(new RasterDataNode[]{targetRaster});
            final JInternalFrame[] internalFrames = app.findInternalFrames(targetRaster, 1);
            for (JInternalFrame internalFrame : internalFrames) {
                final Container contentPane = internalFrame.getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    ProductSceneView view = (ProductSceneView) contentPane;
                    view.setImageInfo(targetImageInfo.createDeepCopy());
                }
            }
        }

        private boolean isIndexCoded(RasterDataNode targetRaster) {
            return targetRaster instanceof Band && ((Band) targetRaster).getIndexCoding() != null;
        }

        private class ImageInfoListener extends ProductNodeListenerAdapter {

            private final RasterDataNode raster;

            ImageInfoListener(RasterDataNode raster) {
                this.raster = raster;
            }

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (event.getSourceNode() == raster &&
                    event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_IMAGE_INFO)) {
                    transferImageInfo(raster, model.getCurrentRasterList());
                }

            }
        }
    }

    // todo - think about creating own CollectionLayer which handles synchronizing with rasterList
    private void updateLayerGroup(ProductSceneView view) {
        final List<RasterDataNode> rasterList = model.getCurrentRasterList();
        if (view != null && rasterList.size() > 1) {
            final Layer rootLayer = view.getRootLayer();
            final String rasterName = view.getRaster().getName();
            CollectionLayer collectionLayer = model.getLayerGroup();
            boolean layerGroupCreated = false;
            if (collectionLayer == null) {
                layerGroupCreated = true;
                collectionLayer = createLayerGroup(view, rootLayer, rasterName);
            }
            final List<Layer> children = synchronizeLayerGroup(rasterList, collectionLayer);

            if (layerGroupCreated && !children.isEmpty()) {
                children.get(0).setVisible(true);
            }

        }
    }

    private List<Layer> synchronizeLayerGroup(List<RasterDataNode> rasterList, CollectionLayer collectionLayer) {
        final List<Layer> children = collectionLayer.getChildren();
        final List<Layer> layerToRemove = new ArrayList<Layer>();
        for (Layer child : children) {
            final PropertySet propertySet1 = child.getConfiguration();
            final Property property = propertySet1.getProperty(RasterImageLayerType.PROPERTY_NAME_RASTER);
            if (property != null) {
                RasterDataNode layerRaster = (RasterDataNode) property.getValue();
                if (!rasterList.contains(layerRaster)) {
                    layerToRemove.add(child);
                }
            }
        }
        children.removeAll(layerToRemove);

        for (int i = 0, rasterListSize = rasterList.size(); i < rasterListSize; i++) {
            RasterDataNode raster = rasterList.get(i);
            final Layer foundLayer = LayerUtils.getChildLayer(collectionLayer,
                                                              LayerUtils.SearchMode.DEEP,
                                                              new RasterLayerFilter(raster));
            if (foundLayer == null) {
                final RasterImageLayerType imageLayerType = LayerTypeRegistry.getLayerType(
                        RasterImageLayerType.class);
                final BandImageMultiLevelSource mls = BandImageMultiLevelSource.create(raster,
                                                                                       ProgressMonitor.NULL);
                final ProductData.UTC utcStartTime = raster.getProduct().getStartTime();

                //todo - add more granularity to date format
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                sdf.setCalendar(utcStartTime.getAsCalendar());
                String layerName = String.format("%s %s", raster.getProductRefString(),
                                                 sdf.format(utcStartTime.getAsDate()));

                final Layer layer = imageLayerType.createLayer(raster, mls);
                layer.setVisible(false);
                layer.setName(layerName);
                collectionLayer.getChildren().add(i, layer);
            }
        }
        return children;
    }

    private CollectionLayer createLayerGroup(ProductSceneView view, Layer rootLayer, String rasterName) {
        CollectionLayer collectionLayer;
        collectionLayer = new CollectionLayer(rasterName);
        final ImageLayer baseLayer = view.getBaseImageLayer();
        final int childIndex = rootLayer.getChildIndex(baseLayer.getId());
        rootLayer.getChildren().add(childIndex, collectionLayer);
        baseLayer.setVisible(false);
        return collectionLayer;
    }

    private static class RasterLayerFilter implements LayerFilter {

        private final RasterDataNode raster;

        private RasterLayerFilter(RasterDataNode raster) {
            this.raster = raster;
        }

        @Override
        public boolean accept(Layer layer) {
            final PropertySet propertySet1 = layer.getConfiguration();
            final Property property = propertySet1.getProperty(RasterImageLayerType.PROPERTY_NAME_RASTER);
            if (property != null) {
                RasterDataNode layerRaster = (RasterDataNode) property.getValue();
                return layerRaster == raster;
            }
            return false;
        }
    }

    private class SliderUpdater implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateLayerGroup(model.getCurrentView());
            configureTimeSlider();
        }
    }

    private class SliderChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            final CollectionLayer collectionLayer = model.getLayerGroup();
            if (collectionLayer != null) {
                final List<Layer> children = collectionLayer.getChildren();
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    Layer child = children.get(i);
                    child.setVisible(i == timeSlider.getValue());
                }
            }
        }
    }
}
