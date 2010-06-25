package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocationType;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class TimeSeriesConfigForm {

    private final SimpleDateFormat dateFormat;
    private final JComponent control;
    private JLabel nameField;
    private JLabel crsField;
    private JLabel startField;
    private JLabel endField;
    private JLabel dimensionField;
    private VariableSelectionPane variablePane;
    private ProductLocationsPane locationsPane;

    TimeSeriesConfigForm() {
        dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
        control = createControl();
    }

    private JComponent createControl() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);
        layout.setColumnFill(0, TableLayout.Fill.BOTH);
        layout.setColumnFill(1, TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 1.0);
        layout.setColumnWeightY(0, 1.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightY(1, 0.0);
        layout.setCellFill(0, 0, TableLayout.Fill.HORIZONTAL);
        layout.setCellWeightY(0, 0, 0.0);
        layout.setCellColspan(1, 0, 2);
        layout.setCellColspan(2, 0, 2);

        JPanel infoPanel = createInfoPanel();
        JPanel buttonPanel = createButtonPanel();
        JPanel variablePanel = createVariablePanel();
        JPanel productsPanel = createProductsPanel();

        final JPanel control = new JPanel(layout);
        control.add(infoPanel);
        control.add(buttonPanel);
        control.add(variablePanel, new TableLayout.Cell(1, 0));
        control.add(productsPanel, new TableLayout.Cell(2, 0));
        return control;
    }

    public JComponent getControl() {
        return control;
    }

    public void updateFormControl(Product product) {
        TimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(product);
        updateInfoPanel(timeSeries);
        updateVariablePanel(timeSeries);
        updateProductsPanel(timeSeries);
    }


    private JPanel createInfoPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);
        layout.setColumnWeightX(0, 0.1);
        layout.setColumnWeightX(1, 1.0);
        layout.setTableWeightY(0.0);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setCellColspan(0, 0, 2);

        final JLabel nameLabel = new JLabel("Name:");
        nameField = new JLabel();
        final JLabel crsLabel = new JLabel("CRS:");
        crsField = new JLabel();
        final JLabel startLabel = new JLabel("Start time:");
        startField = new JLabel();
        final JLabel endLabel = new JLabel("End time:");
        endField = new JLabel();
        final JLabel dimensionLabel = new JLabel("Dimension:");
        dimensionField = new JLabel("Dimension:");

        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator("Information"));
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(crsLabel);
        panel.add(crsField);
        panel.add(startLabel);
        panel.add(startField);
        panel.add(endLabel);
        panel.add(endField);
        panel.add(dimensionLabel);
        panel.add(dimensionField);
        return panel;
    }

    private void updateInfoPanel(TimeSeries timeSeries) {
        if (timeSeries == null) {
            nameField.setVisible(false);
            crsField.setVisible(false);
            startField.setVisible(false);
            endField.setVisible(false);
            dimensionField.setVisible(false);
            return;
        }
        final Product tsProduct = timeSeries.getTsProduct();

        nameField.setText(tsProduct.getDisplayName());
        crsField.setText(tsProduct.getGeoCoding().getMapCRS().getName().getCode());
        final String startTime = dateFormat.format(tsProduct.getStartTime().getAsDate());
        startField.setText(startTime);
        final String endTime = dateFormat.format(tsProduct.getEndTime().getAsDate());
        endField.setText(endTime);
        final String dimensionString = tsProduct.getSceneRasterWidth() + " x " + tsProduct.getSceneRasterHeight();
        dimensionField.setText(dimensionString);
    }


    private JPanel createButtonPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);

        final JButton newButton = new JButton("New TS");
        final Command newTSCommand = VisatApp.getApp().getCommandManager().getCommand(NewTimeSeriesAssistantAction.ID);
        newButton.setAction(newTSCommand.getAction());
        final JButton cloneButton = new JButton("Clone TS");
        final JButton viewButton = new JButton("View TS");
        final JButton regridButton = new JButton("Regrid TS");
        final JButton exportButton = new JButton("Export TS");

        final JPanel panel = new JPanel(layout);
        panel.add(newButton);
        panel.add(cloneButton);
        panel.add(viewButton);
        panel.add(regridButton);
        panel.add(exportButton);
        return panel;
    }

    private JPanel createVariablePanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(0, 0.0);
        layout.setRowWeightY(1, 1.0);
        layout.setRowFill(1, TableLayout.Fill.BOTH);

        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator("Variables"));
        variablePane = new VariableSelectionPane();
        variablePane.setPreferredSize(new Dimension(150, 80));
        panel.add(variablePane);
        return panel;
    }

    private void updateVariablePanel(TimeSeries timeSeries) {
        final VariableSelectionPaneModel model;
        if (timeSeries != null) {
            model = new TimeSeriesVariableSelectionPaneModel(timeSeries);
        } else {
            model = new DefaultVariableSelectionPaneModel();
        }

        variablePane.setModel(model);
    }

    private JPanel createProductsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(0, 0.0);
        layout.setRowWeightY(1, 1.0);
        layout.setRowFill(1, TableLayout.Fill.BOTH);


        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator("Product Sources"));
        locationsPane = new ProductLocationsPane();
        locationsPane.setPreferredSize(new Dimension(150, 80));
        panel.add(locationsPane);
        return panel;
    }

    private void updateProductsPanel(TimeSeries timeSeries) {
        final ProductLocationsPaneModel locationsModel;
        if (timeSeries != null) {
            locationsModel = new TimeSeriesProductLocationsPaneModel(timeSeries);
        } else {
            locationsModel = new DefaultProductLocationsPaneModel();
        }
        locationsPane.setModel(locationsModel);
    }

    private static class TimeSeriesVariableSelectionPaneModel extends AbstractListModel
            implements VariableSelectionPaneModel {

        private final TimeSeries timeSeries;

        private TimeSeriesVariableSelectionPaneModel(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public int getSize() {
            return timeSeries.getTimeVariables().size();
        }

        @Override
        public Variable getElementAt(int index) {
            final String varName = timeSeries.getTimeVariables().get(index);
            return new Variable(varName, timeSeries.isVariableSelected(varName));
        }

        @Override
        public void set(Variable... variables) {
        }

        @Override
        public void add(Variable... variables) {
        }

        @Override
        public void setSelectedVariableAt(int index, boolean selected) {
            final String varName = timeSeries.getTimeVariables().get(index);
            if (timeSeries.isVariableSelected(varName) != selected) {
                if (!selected) {
                    closeAssociatedViews(varName);
                }
                timeSeries.setVariableSelected(varName, selected);
                fireContentsChanged(this, index, index);
            }
        }

        private void closeAssociatedViews(String varName) {
            final Band[] bands = timeSeries.getBandsForVariable(varName);
            for (Band band : bands) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(band);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
            }

        }

        @Override
        public List<String> getSelectedVariableNames() {
            final List<String> allVars = timeSeries.getTimeVariables();
            final List<String> selectedVars = new ArrayList<String>(allVars.size());
            for (String varName : allVars) {
                if (timeSeries.isVariableSelected(varName)) {
                    selectedVars.add(varName);
                }
            }
            return selectedVars;
        }
    }

    private static class TimeSeriesProductLocationsPaneModel extends AbstractListModel
            implements ProductLocationsPaneModel {

        private final TimeSeries timeSeries;

        private TimeSeriesProductLocationsPaneModel(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public int getSize() {
            return timeSeries.getProductLocations().size();

        }

        @Override
        public ProductLocation getElementAt(int index) {
            return timeSeries.getProductLocations().get(index);

        }

        @Override
        public List<ProductLocation> getProductLocations() {
            return timeSeries.getProductLocations();

        }

        @Override
        public void addFiles(File... files) {
            final int startIndex = timeSeries.getProductLocations().size();
            for (File file : files) {
                timeSeries.addProductLocation(ProductLocationType.FILE, file.getAbsolutePath());
            }
            final int stopIndex = timeSeries.getProductLocations().size() - 1;
            fireIntervalAdded(this, startIndex, stopIndex);
        }

        @Override
        public void addDirectory(File directory, boolean recursive) {
            timeSeries.addProductLocation(recursive ? ProductLocationType.DIRECTORY_REC : ProductLocationType.DIRECTORY,
                                          directory.getAbsolutePath());
            final int index = timeSeries.getProductLocations().size() - 1;
            fireIntervalAdded(this, index, index);
        }

        @Override
        public void remove(int... indices) {
            final List<ProductLocation> locationList = timeSeries.getProductLocations();
            final List<ProductLocation> toRemove = new ArrayList<ProductLocation>();
            for (int index : indices) {
                toRemove.add(locationList.get(index));
            }
            for (ProductLocation location : toRemove) {
                timeSeries.removeProductLocation(location);
            }
            fireContentsChanged(this, indices[0], indices[indices.length - 1]);
        }
    }

}
