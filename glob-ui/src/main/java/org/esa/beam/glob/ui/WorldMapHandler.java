package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.Layer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@Deprecated
class WorldMapHandler implements PropertyChangeListener {

    private Layer layer;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        ProductSceneView currentView = model.getCurrentView();
//        if (currentView != null) {
//            if (layer == null) {
//                final LayerType type = LayerTypeRegistry.getLayerType("BlueMarbleLayerType");
//                layer = type.createLayer(currentView, type.createLayerConfig(currentView));
//            }
//            if (LayerUtils.getChildLayerById(currentView.getRootLayer(), layer.getId()) != null) {
//                if (!model.isShowingWorldMapLayer()) {
//                    currentView.getRootLayer().getChildren().remove(layer);
//                }
//            } else {
//                if (model.isShowingWorldMapLayer()) {
//                    layer.setVisible(true);
//                    currentView.getRootLayer().getChildren().add(layer);
//                }
//            }
//        }
    }
}
