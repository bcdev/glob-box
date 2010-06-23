package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesEventType;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * User: Thomas Storm
 * Date: 16.06.2010
 * Time: 15:29:18
 */
public class TimeSeriesConfigToolView extends AbstractToolView {

    private DecimalFormat decimalFormat;
    public static final String UNIT_DEGREE = "°";
    private JButton removeButton;
    private ProductSceneView currentView;
    private TimeCoding productTimeCoding;
    private TimeSeriesConfigToolView.TimeSeriesIFL internalFrameListener;
    private JPanel crsPanel;
    private JPanel productsPanel;
    private JPanel toolBar;
    private TimeSeries timeSeries;
    private static final String NEW_BUTTON_NAME = "newButtonName";
    private JTable table;

    public TimeSeriesConfigToolView() {
        decimalFormat = new DecimalFormat("###0.0##", new DecimalFormatSymbols(Locale.ENGLISH));
        decimalFormat.setParseIntegerOnly(false);
        decimalFormat.setParseBigDecimal(false);
        decimalFormat.setDecimalSeparatorAlwaysShown(true);
        final VisatApp visatApp = VisatApp.getApp();
        internalFrameListener = new TimeSeriesIFL();
        visatApp.addInternalFrameListener(internalFrameListener);
    }

    public void setCurrentView(ProductSceneView currentView) {
        if (this.currentView != currentView) {
            this.currentView = currentView;
            if (currentView != null) {
                productTimeCoding = currentView.getProduct().getTimeCoding();
                timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
                if (timeSeries != null && table != null) {
                    createTableModel();
                }
            }
        }
    }

    @Override
    public void dispose() {
        VisatApp.getApp().removeInternalFrameListener(internalFrameListener);
    }

    @Override
    protected JComponent createControl() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowFill(3, TableLayout.Fill.BOTH);
        tableLayout.setRowWeightY(3, 1.0);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        crsPanel = createCrsPanel();
        contentPanel.add(crsPanel);
        productsPanel = createProductsPanel();
        contentPanel.add(productsPanel);

//        panel.add(createTimeSpanPanel());
//        panel.add(createRegionPanel());
        toolBar = createToolBar();
        panel.add(toolBar, BorderLayout.EAST);
        panel.add(contentPanel);

        setEnabled(false);
        return panel;
    }

    private JPanel createToolBar() {
        final JButton newButton = new JButton("New TS");
        final Command newTSCommand = VisatApp.getApp().getCommandManager().getCommand(NewTimeSeriesAction.ID);
        newButton.setAction(newTSCommand.getAction());
        newButton.setName(NEW_BUTTON_NAME);
        final JButton cloneButton = new JButton("Clone TS");
        final JButton viewButton = new JButton("View TS");
        final JButton regridButton = new JButton("Regrid TS");
        final JButton netcdfButton = new JButton("NetCDF TS");

        final JPanel actionBar = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        actionBar.add(newButton, gbc);
        gbc.gridy++;
        actionBar.add(cloneButton, gbc);
        gbc.gridy++;
        actionBar.add(viewButton, gbc);
        gbc.gridy++;
        actionBar.add(regridButton, gbc);
        gbc.gridy++;
        actionBar.add(netcdfButton, gbc);
        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        actionBar.add(new JLabel(" "), gbc); // filler
        return actionBar;
    }

    private JPanel createTimeSpanPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Time Span"));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 1.0);

        panel.add(new Label("Start time:"));
        final DateComboBox startTimeBox = new DateComboBox();
        startTimeBox.setShowNoneButton(false);
        startTimeBox.setTimeDisplayed(true);
        startTimeBox.setFormat(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss"));
        if (productTimeCoding != null) {
            startTimeBox.setDate(productTimeCoding.getStartTime().getAsCalendar().getTime());
        }
        panel.add(startTimeBox);

        panel.add(new Label("End time:"));
        DateComboBox endTimeBox = new DateComboBox();
        endTimeBox.setShowNoneButton(false);
        endTimeBox.setTimeDisplayed(true);
        endTimeBox.setFormat(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss"));
        if (productTimeCoding != null) {
            endTimeBox.setDate(productTimeCoding.getEndTime().getAsCalendar().getTime());
        }
        panel.add(endTimeBox);

        return panel;
    }

    private JPanel createRegionPanel() {
        final TableLayout layout = new TableLayout(6);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setColumnWeightX(4, 1.0);
        layout.setColumnWeightX(5, 0.0);
        layout.setColumnPadding(2, new Insets(3, 0, 3, 12));
        layout.setColumnPadding(5, new Insets(3, 0, 3, 12));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Bounds"));

        panel.add(new JLabel("West:"));
        final NumberFormatter formatter = new NumberFormatter(decimalFormat);
        final JFormattedTextField westLonField = new JFormattedTextField(formatter);
        westLonField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(westLonField);
        panel.add(new JLabel(UNIT_DEGREE));
        panel.add(new JLabel("East:"));
        final JFormattedTextField eastLonField = new JFormattedTextField(formatter);
        eastLonField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(eastLonField);
        panel.add(new JLabel(UNIT_DEGREE));

        panel.add(new JLabel("North:"));
        final JFormattedTextField northLatField = new JFormattedTextField(formatter);
        northLatField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(northLatField);
        panel.add(new JLabel(UNIT_DEGREE));
        panel.add(new JLabel("South:"));
        final JFormattedTextField southLatField = new JFormattedTextField(formatter);
        southLatField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(southLatField);
        panel.add(new JLabel(UNIT_DEGREE));

        return panel;
    }

    private JPanel createCrsPanel() {
        String crs = "WGS84(DD)";
        if (currentView != null && currentView.getRaster() != null && currentView.getRaster().getGeoCoding() != null) {
            crs = currentView.getRaster().getGeoCoding().getMapCRS().getName().getCode();
        }
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setCellWeightX(0, 0, 0.0);
        tableLayout.setCellWeightX(0, 1, 1.0);
        tableLayout.setCellWeightX(1, 0, 1.0);
        tableLayout.setCellWeightX(1, 1, 0.0);
        tableLayout.setCellFill(1, 1, TableLayout.Fill.NONE);

        tableLayout.setRowAnchor(0, TableLayout.Anchor.WEST);
        tableLayout.setRowAnchor(1, TableLayout.Anchor.EAST);
        tableLayout.setTablePadding(4, 4);
        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("CRS"));

        panel.add(new JLabel("CRS:"));
        final JTextField field = new JTextField(crs);
        field.setEditable(false);
        panel.add(field);

        final JButton button = new JButton("Change CRS...");
        panel.add(button, new TableLayout.Cell(1, 1));

        return panel;
    }

    private JPanel createProductsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setRowFill(0, TableLayout.Fill.BOTH);
        layout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setRowWeightY(0, 1.0);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Product List"));

        table = new JTable();

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setMinimumSize(new Dimension(350, 80));

        final JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.showDialog(VisatApp.getApp().getMainFrame(), "Ok");
                final File[] selectedFiles = fileChooser.getSelectedFiles();
            }
        });
        buttonPane.add(addButton);
        removeButton = new JButton("Remove");

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ListSelectionModel selectionModel = table.getSelectionModel();
                final int minIndex = selectionModel.getMinSelectionIndex();
                final int maxIndex = selectionModel.getMaxSelectionIndex();
                final Product removedProduct = (Product) table.getModel().getValueAt(minIndex, 0);
                timeSeries.removeProduct(removedProduct);
            }
        });
        buttonPane.add(removeButton);

        panel.add(scrollPane);
        panel.add(buttonPane);

        return panel;
    }

    private void createTableModel() {
        table.setModel(new ProductListTableModel());
        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setHeaderValue("Product");
        final JTableHeader tableHeader = new JTableHeader(columnModel);
        tableHeader.setVisible(true);
        table.setTableHeader(tableHeader);

        table.setRowSelectionAllowed(true);
        final ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(e.getFirstIndex() != -1);
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
    }

    public void setEnabled(boolean enabled) {
        setEnabled(enabled, productsPanel);
        setEnabled(enabled, crsPanel);
        Component[] components;
        components = toolBar.getComponents();
        for (Component component : components) {
            if (!NEW_BUTTON_NAME.equals(component.getName())) {
                component.setEnabled(enabled);
            }
        }
    }

    private void setEnabled(boolean enabled, Component component) {
        if (component instanceof Container) {
            final Component[] components = ((Container) component).getComponents();
            if (components != null) {
                for (Component comp : components) {
                    setEnabled(enabled, comp);
                }
            }
        }
        for (Component comp : ((Container) component).getComponents()) {
            comp.setEnabled(enabled);
        }
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Test");
        frame.setContentPane(new TimeSeriesConfigToolView().createControl());
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    private class ProductListTableModel extends AbstractTableModel {

        private ProductListTableModel() {
            timeSeries.addListener(new TimeSeriesListener() {
                @Override
                public void timeSeriesChanged(TimeSeriesChangeEvent event) {
                    final TimeSeriesEventType eventType = event.getEventType();
                    switch (eventType) {
                        case PRODUCT_REMOVED:
//                            Product removedProduct = (Product)event.getOldValue();
//                            for( int i = 0; i < getRowCount(); i++ ) {
//                                if( getValueAt( i, 0 ).toString().equals( removedProduct.getName() ) ) {
//                                    fireTableRowsDeleted(i, i);
//                                }
//                            }
//                            break;
                            fireTableDataChanged();
                        case PRODUCT_ADDED:
                            fireTableDataChanged();
                            break;
                    }
                }
            });
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            int count = timeSeries.getProducts().size();
            setEnabled(count > 0);
            return count;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return timeSeries.getProducts().get(rowIndex);
        }
    }

    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setCurrentView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
        }
    }

}
