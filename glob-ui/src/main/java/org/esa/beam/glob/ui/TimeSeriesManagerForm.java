package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesEventType;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;

import static org.esa.beam.framework.ui.UIUtils.*;

class TimeSeriesManagerForm extends JPanel {

    private TimeSeries timeSeries;

    TimeSeriesManagerForm() {
        this.timeSeries = TimeSeries.getInstance();
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

        JToolBar toolBar = createToolbarPanel();
        JPanel bandListPanel = createBandListPanel();
        JPanel configPanel = createConfigPanel();
        JPanel infoPanel = createInfoPanel();

        this.add(toolBar);
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
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);
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
        tableLayout.setTablePadding(4, 4);
        final JPanel panel = new JPanel(tableLayout);
        final JLabel label = new JLabel("Raster in time series");
        final JList bandsList = new JList();
        final ListModel listModel = new TimeSeriesRasterListModel(timeSeries);
        bandsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                                 cellHasFocus);
                label.setText(((RasterDataNode) value).getName());
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
        JLabel crsLabel = new JLabel("CRS: ");
        JLabel crsValue = new JLabel(timeSeries.getCRS().getName().getCode());
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        final ProductData.UTC startTime = timeSeries.getStartTime();
        final ProductData.UTC endTime = timeSeries.getEndTime();

        JLabel startTimeLabel = new JLabel("Start time: ");
        JLabel startTimeValue = new JLabel(sdf.format(startTime.getAsDate()));
        JLabel endTimeLabel = new JLabel("End time: ");
        JLabel endTimeValue = new JLabel(sdf.format(endTime.getAsDate()));
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setTablePadding(4, 4);
        final JPanel panel = new JPanel(tableLayout);
        panel.add(crsLabel);
        panel.add(crsValue);
        panel.add(startTimeLabel);
        panel.add(startTimeValue);
        panel.add(endTimeLabel);
        panel.add(endTimeValue);
        return panel;
    }

    private JToolBar createToolbarPanel() {
        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        addButtons(toolBar);

        return toolBar;
    }

    private void addButtons(JToolBar toolBar) {
        final AbstractButton importButton = new JButton(loadImageIcon("icons/Import16.gif"));
        importButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VisatApp.getApp().showInfoDialog("In forthcoming versions you will be able to import a time series",
                                                 "");
            }
        });
        toolBar.add(importButton);

        final AbstractButton exportButton = new JButton(loadImageIcon("icons/Export16.gif"));
        exportButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VisatApp.getApp().showInfoDialog("In forthcoming versions you will be able to export a time series",
                                                 "");
            }
        });
        toolBar.add(exportButton);

//        final AbstractButton magnifierButton = new JButton(loadImageIcon("icons/ZoomTool16.gif"));
//        magnifierButton.setAction(new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                VisatApp.getApp().showInfoDialog( "In forthcoming versions you will be able to use a magnifier.",
//                                                  "");
//            }
//        });
//        toolBar.add( magnifierButton );
    }

    private class TimeSeriesRasterListModel extends AbstractListModel {

        private final TimeSeries timeSeries;

        private TimeSeriesRasterListModel(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
            timeSeries.addListener(new ManagerTSL(this));
        }

        @Override
        public int getSize() {
            return timeSeries.getRasterList().size();
        }

        @Override
        public Object getElementAt(int index) {
            return timeSeries.getRasterList().get(index);
        }

        private class ManagerTSL implements TimeSeriesListener {

            private final TimeSeriesManagerForm.TimeSeriesRasterListModel model;

            public ManagerTSL(TimeSeriesRasterListModel model) {
                this.model = model;
            }

            @Override
            public void timeSeriesChanged(TimeSeriesChangeEvent timeSeriesChangeEvent) {
                if (timeSeriesChangeEvent.getProperty() == TimeSeriesEventType.RASTER_ADDED) {
                    final Integer index = (Integer) timeSeriesChangeEvent.getNewValue();
                    model.fireIntervalAdded(model, index, index);
                }
                if (timeSeriesChangeEvent.getProperty() == TimeSeriesEventType.RASTER_REMOVED) {
                    final Integer index = (Integer) timeSeriesChangeEvent.getOldValue();
                    model.fireIntervalRemoved(model, index, index);
                }
            }
        }
    }

}
