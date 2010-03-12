package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBoxManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


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
                setCurrentView(evt);
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

    private void setCurrentView(PropertyChangeEvent evt) {
        propertySet.setValue(PROPERTY_CURRENT_VIEW, evt.getNewValue());
    }

    public boolean isShowingWorldMapLayer() {
        return showingWorldMapLayer;
    }
}