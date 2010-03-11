package org.esa.beam.glob.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Dimension;

class TimeSeriesManagerForm extends JPanel {

    private TimeSeriesManagerFormModel model;
    private JSlider timeSlider;
    private JCheckBox syncColorChecker;
    private JCheckBox useAlphaBlendingChecker;
    private JCheckBox showWorldMapChecker;

    TimeSeriesManagerForm(TimeSeriesManagerFormModel model) {
        this.model = model;
        PropertyContainer container = PropertyContainer.createObjectBacked(this.model);
        createComponents(container);
    }

    private void createComponents(PropertySet propertySet) {
        setPreferredSize(new Dimension(300, 200));
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4,4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setRowWeightY(4, 1.0);
        setLayout(tableLayout);

        showWorldMapChecker = new JCheckBox("Show world map layer");
        syncColorChecker = new JCheckBox("Synchronise colour information");
        useAlphaBlendingChecker = new JCheckBox("Use alpha blending");
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

    }
}
