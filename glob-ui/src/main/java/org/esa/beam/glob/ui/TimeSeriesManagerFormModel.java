package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.RasterImageLayerType;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.GlobBoxManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


@SuppressWarnings({"UnusedDeclaration"})
class TimeSeriesManagerFormModel {

    static final String PROPERTY_NAME_WORLDMAP = "showingWorldMapLayer";
    static final String PROPERTY_NAME_SYNCCOLOR = "syncColorInformation";
    static final String PROPERTY_NAME_BLENDING = "alphaBlending";
    static final String PROPERTY_NAME_TIMEPOS = "timePos";
    static final String PROPERTY_CURRENT_VIEW = GlobBoxManager.CURRENT_VIEW_PROPERTY;

    private boolean showingWorldMapLayer;
    private boolean syncColorInformation;
    private boolean alphaBlending;
    private float timePos;
    private GlobBoxManager globBoxManager;
    private ProductSceneView currentView;
    private final PropertyContainer propertySet;

    TimeSeriesManagerFormModel() {
        globBoxManager = GlobBoxManager.getInstance();
        globBoxManager.addPropertyChangeListener(PROPERTY_CURRENT_VIEW, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setCurrentView((ProductSceneView) evt.getNewValue());
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

    private void setCurrentView(ProductSceneView view) {
        propertySet.setValue(PROPERTY_CURRENT_VIEW, view);
        final List<RasterDataNode> rasterList = getCurrentRasterList();
        if (view != null && rasterList.size() > 1) {
            CollectionLayer collection = new CollectionLayer(view.getRaster().getName());
            final Layer rootLayer = view.getRootLayer();
            final ImageLayer baseLayer = view.getBaseImageLayer();
            final int childIndex = rootLayer.getChildIndex(baseLayer.getId());


            Collections.sort(rasterList, new Comparator<RasterDataNode>() {
                @Override
                public int compare(RasterDataNode raster1, RasterDataNode raster2) {
                    final Date raster1Date = raster1.getProduct().getStartTime().getAsDate();
                    final Date raster2Date = raster2.getProduct().getStartTime().getAsDate();
                    return raster1Date.compareTo(raster2Date) * -1;
                }
            });

            for (RasterDataNode raster : rasterList) {
                final RasterImageLayerType imageLayerType = LayerTypeRegistry.getLayerType(RasterImageLayerType.class);
                final BandImageMultiLevelSource mls = BandImageMultiLevelSource.create(raster, ProgressMonitor.NULL);
                final Layer layer = imageLayerType.createLayer(raster, mls);
                final ProductData.UTC utcStartTime = raster.getProduct().getStartTime();

                //todo - add more granularity to date format
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy_HH:mm:ss");
                sdf.setCalendar(utcStartTime.getAsCalendar());
                String layerName = String.format("%s-%s", raster.getProductRefString(),
                                                 sdf.format(utcStartTime.getAsDate()));
                layer.setName(layerName);
                collection.getChildren().add(layer);
            }
            rootLayer.getChildren().add(childIndex, collection);
            baseLayer.setVisible(false);
        }
    }

    public boolean isShowingWorldMapLayer() {
        return showingWorldMapLayer;
    }

    public boolean isSynchronizingColorInformation() {
        return syncColorInformation;
    }

    public List<RasterDataNode> getCurrentRasterList() {
        return globBoxManager.getCurrentRasterList();
    }
}