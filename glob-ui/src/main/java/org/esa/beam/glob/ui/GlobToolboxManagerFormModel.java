package org.esa.beam.glob.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.RasterImageLayerType;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.GlobBox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"UnusedDeclaration"})
class GlobToolboxManagerFormModel {

    static final String PROPERTY_NAME_WORLDMAP = "showingWorldMapLayer";
    static final String PROPERTY_NAME_SYNCCOLOR = "syncColorInformation";
    static final String PROPERTY_NAME_BLENDING = "alphaBlending";
    static final String PROPERTY_NAME_TIMEPOS = "timePos";
    static final String PROPERTY_CURRENT_VIEW = GlobBox.CURRENT_VIEW_PROPERTY;

    private boolean showingWorldMapLayer;
    private boolean syncColorInformation;
    private boolean alphaBlending;
    private float timePos;
    private GlobBox globBox;
    private ProductSceneView currentView;
    private final PropertyContainer propertySet;

    GlobToolboxManagerFormModel() {
        globBox = GlobBox.getInstance();
        globBox.addPropertyChangeListener(PROPERTY_CURRENT_VIEW, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setCurrentView((ProductSceneView) evt.getNewValue());
            }
        });
        globBox.addPropertyChangeListener(GlobBox.RASTER_LIST_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateLayerGroup(getCurrentView());
            }
        });
        propertySet = PropertyContainer.createObjectBacked(this);
    }

    public PropertySet getPropertySet() {
        return propertySet;
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    public CollectionLayer getLayerGroup() {
        final ProductSceneView sceneView = getCurrentView();
        if (sceneView != null) {
            final String rasterName = sceneView.getRaster().getName();
            return (CollectionLayer) LayerUtils.getChildLayerByName(sceneView.getRootLayer(), rasterName);
        }
        return null;
    }

    private void setCurrentView(ProductSceneView view) {
        propertySet.setValue(PROPERTY_CURRENT_VIEW, view);
        updateLayerGroup(view);
    }

    // todo - think about creating own CollectionLayer which handles synchronizing with rasterList
    private void updateLayerGroup(ProductSceneView view) {
        final List<RasterDataNode> rasterList = getCurrentRasterList();
        if (view != null && rasterList.size() > 1) {
            final Layer rootLayer = view.getRootLayer();
            final String rasterName = view.getRaster().getName();
            CollectionLayer collectionLayer = getLayerGroup();
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
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy_HH:mm:ss");
                sdf.setCalendar(utcStartTime.getAsCalendar());
                String layerName = String.format("%s-%s", raster.getProductRefString(),
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

    public boolean isShowingWorldMapLayer() {
        return showingWorldMapLayer;
    }

    public boolean isSynchronizingColorInformation() {
        return syncColorInformation;
    }

    public List<RasterDataNode> getCurrentRasterList() {
        return globBox.getRasterList();
    }

    private static class RasterLayerFilter implements LayerFilter {

        private final RasterDataNode raster;

        public RasterLayerFilter(RasterDataNode raster) {
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
}