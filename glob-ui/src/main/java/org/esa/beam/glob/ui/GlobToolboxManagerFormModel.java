package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;


@SuppressWarnings({"UnusedDeclaration"})
public class GlobToolboxManagerFormModel {

    static final String PROPERTY_NAME_WORLDMAP = "showingWorldMapLayer";
    static final String PROPERTY_NAME_SYNCCOLOR = "syncColorInformation";
    static final String PROPERTY_NAME_BLENDING = "alphaBlending";
    static final String PROPERTY_NAME_TIMEPOS = "timePos";
    static final String PROPERTY_CURRENT_VIEW = GlobBox.CURRENT_VIEW_PROPERTY;

    private boolean showingWorldMapLayer;
    private boolean syncColorInformation;
    private boolean alphaBlending;
    private GlobBox globBox;
    private ProductSceneView currentView;
    private final PropertyContainer propertySet;

    public GlobToolboxManagerFormModel(final GlobBox globBox) {
        this.globBox = globBox;
        globBox.addPropertyChangeListener(PROPERTY_CURRENT_VIEW, new PropertyChangeListener() {
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

}