package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.glob.core.timeseries.TimeSeriesHandler;
import org.esa.beam.visat.VisatApp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
import javax.swing.table.AbstractTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    private static final TimeSeriesHandler handler = TimeSeriesHandler.getInstance();
    private DecimalFormat decimalFormat;
    public static final String UNIT_DEGREE = "°";

    public TimeSeriesConfigToolView() {
        decimalFormat = new DecimalFormat("###0.0##", new DecimalFormatSymbols(Locale.ENGLISH));
        decimalFormat.setParseIntegerOnly(false);
        decimalFormat.setParseBigDecimal(false);
        decimalFormat.setDecimalSeparatorAlwaysShown(true);

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

        JPanel panel = new JPanel(tableLayout);
        panel.add(createTimeSpanPanel());
        panel.add(createRegionPanel());
        panel.add(createCrsPanel());
        panel.add(createProductsPanel());

        return panel;
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
        startTimeBox.setFormat(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
        panel.add(startTimeBox);

        panel.add(new Label("End time:"));
        DateComboBox endTimeBox = new DateComboBox();
        endTimeBox.setShowNoneButton(false);
        endTimeBox.setTimeDisplayed(true);
        startTimeBox.setFormat(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"));
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
        final CoordinateReferenceSystem crs = handler.getTimeSeries().getCRS();
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
        final JTextField field = new JTextField(crs.getName().getCode());
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
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Product List"));

        final JTable table = new JTable();
        table.setModel(new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public int getRowCount() {
                return handler.getProducts().size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return handler.getProducts().get(rowIndex).getName();
            }
        });

        table.setRowSelectionAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//        table.setPreferredScrollableViewportSize(new Dimension(250, 80));
        table.setFillsViewportHeight(true);

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
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        buttonPane.add(removeButton);

        panel.add(scrollPane);
        panel.add(buttonPane);

        return panel;
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Test");
        frame.setContentPane(new TimeSeriesConfigToolView().createControl());
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }
}
