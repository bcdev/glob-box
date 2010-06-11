package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.glob.core.timeseries.TimeSeriesHandler;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesProperty;
import org.esa.beam.glob.core.timeseries.datamodel.TimedRaster;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import java.awt.Component;
import java.awt.Dimension;

class TimeSeriesManagerForm extends JPanel {

    private TimeSeriesHandler handler;

    TimeSeriesManagerForm() {
        this.handler = TimeSeriesHandler.getInstance();
        createComponents();
    }

    private void createComponents() {
        setPreferredSize(new Dimension(300, 200));
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setRowWeightY(1, 1.0);
        setLayout(tableLayout);

        JPanel toolbarPanel = createToolbarPanel();
        JPanel bandListPanel = createBandListPanel();
        JPanel configPanel = createConfigPanel();
        JPanel infoPanel = createInfoPanel();

        this.add(toolbarPanel);
        this.add(bandListPanel);
        this.add(configPanel);
        this.add(infoPanel);

//        final BindingContext context = new BindingContext(propertySet);
//        context.bind(GlobToolboxManagerFormModel.PROPERTY_NAME_WORLDMAP, showWorldMapChecker);
//        context.bind(GlobToolboxManagerFormModel.PROPERTY_NAME_SYNCCOLOR, syncColorChecker);
//
//        final WorldMapHandler worldMapHandler = new WorldMapHandler();
//        context.addPropertyChangeListener(GlobToolboxManagerFormModel.PROPERTY_NAME_WORLDMAP, worldMapHandler);
//        context.addPropertyChangeListener(GlobBox.CURRENT_VIEW_PROPERTY, worldMapHandler);
//
//        final ColorSynchronizer colorSynchronizer = new ColorSynchronizer();
//        context.addPropertyChangeListener(GlobToolboxManagerFormModel.PROPERTY_NAME_SYNCCOLOR, colorSynchronizer);
//        context.addPropertyChangeListener(GlobBox.CURRENT_VIEW_PROPERTY, colorSynchronizer);

    }

    private JPanel createConfigPanel() {
        JCheckBox showWorldMapChecker = new JCheckBox("Show world map layer");
        JCheckBox syncColorChecker = new JCheckBox("Synchronise colour information");

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        final JPanel panel = new JPanel(tableLayout);

        panel.add(showWorldMapChecker);
        panel.add(syncColorChecker);
        return panel;
    }

    private JPanel createBandListPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setTableWeightX(1.0);
        final JPanel panel = new JPanel(tableLayout);
        final JLabel label = new JLabel("Raster in time series");
        final JList bandsList = new JList();
        final ListModel listModel = new TimeSeriesRasterListModel(handler.getTimeSeries());
        bandsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                                 cellHasFocus);
                label.setText(((TimedRaster) value).getName());
                return label;

            }
        });
        bandsList.setModel(listModel);

        final JScrollPane scrollPane = new JScrollPane(bandsList);

        panel.add(label);
        panel.add(scrollPane);
        return panel;
    }

    private JPanel createInfoPanel() {
        final TimeSeries timeSeries = handler.getTimeSeries();
        JLabel crsLabel = new JLabel("CRS: " + timeSeries.getCRS().getName().getCode());
        JLabel startTimeLabel = new JLabel("Start time: " + timeSeries.getStartTime());
        JLabel endTimeLabel = new JLabel("End time: " + timeSeries.getEndTime());
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        final JPanel panel = new JPanel(tableLayout);
        panel.add(crsLabel);
        panel.add(startTimeLabel);
        panel.add(endTimeLabel);
        return panel;
    }

    private JPanel createToolbarPanel() {
        return new JPanel();
    }

    private class TimeSeriesRasterListModel extends AbstractListModel {

        private final TimeSeries timeSeries;

        private TimeSeriesRasterListModel(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
            timeSeries.addListener(new MyTimeSeriesListener(this));
        }

        @Override
        public int getSize() {
            return timeSeries.getRasterCount();
        }

        @Override
        public Object getElementAt(int index) {
            return timeSeries.getRasterAt(index);
        }

        private class MyTimeSeriesListener implements TimeSeriesListener {

            private final TimeSeriesManagerForm.TimeSeriesRasterListModel model;

            public MyTimeSeriesListener(TimeSeriesRasterListModel model) {
                this.model = model;
            }

            @Override
            public void timeSeriesChanged(TimeSeriesChangeEvent timeSeriesChangeEvent) {
                if (timeSeriesChangeEvent.getProperty() == TimeSeriesProperty.RASTER_ADDED) {
                    final Integer index = (Integer) timeSeriesChangeEvent.getNewValue();
                    model.fireIntervalAdded(model, index, index);
                }
                if (timeSeriesChangeEvent.getProperty() == TimeSeriesProperty.RASTER_REMOVED) {
                    final Integer index = (Integer) timeSeriesChangeEvent.getOldValue();
                    model.fireIntervalRemoved(model, index, index);
                }
            }
        }
    }
}
