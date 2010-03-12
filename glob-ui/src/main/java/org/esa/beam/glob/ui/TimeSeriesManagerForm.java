package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBoxManager;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class TimeSeriesManagerForm extends JPanel {

    private TimeSeriesManagerFormModel model;
    private JSlider timeSlider;
    private JCheckBox syncColorChecker;
    private JCheckBox useAlphaBlendingChecker;
    private JCheckBox showWorldMapChecker;

    TimeSeriesManagerForm(TimeSeriesManagerFormModel model) {
        this.model = model;
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

        showWorldMapChecker = new JCheckBox("Show world map layer");
        syncColorChecker = new JCheckBox("Synchronise colour information");
        useAlphaBlendingChecker = new JCheckBox("Use transparency blending");
        timeSlider = new JSlider(JSlider.HORIZONTAL);
        add(showWorldMapChecker);
        add(syncColorChecker);
        add(useAlphaBlendingChecker);
        add(timeSlider);
        add(tableLayout.createVerticalSpacer());

        final BindingContext context = new BindingContext(propertySet);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_WORLDMAP, showWorldMapChecker);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_SYNCCOLOR, syncColorChecker);
        context.bind(TimeSeriesManagerFormModel.PROPERTY_NAME_BLENDING, useAlphaBlendingChecker);

        final WorldMapHandler worldMapHandler = new WorldMapHandler();
        context.addPropertyChangeListener(TimeSeriesManagerFormModel.PROPERTY_NAME_WORLDMAP,
                                          worldMapHandler);

        context.addPropertyChangeListener(GlobBoxManager.CURRENT_VIEW_PROPERTY, worldMapHandler);
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
}
