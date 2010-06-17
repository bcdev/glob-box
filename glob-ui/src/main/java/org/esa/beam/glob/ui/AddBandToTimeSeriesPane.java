package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.grid.BooleanCheckBoxCellRenderer;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 14.06.2010
 * Time: 16:07:54
 */
public class AddBandToTimeSeriesPane extends JPanel {

    private ProductNode node;
    private List<RasterDataNode> rasterList;
    private JTable table;
    private AddBandToTimeSeriesPane.AddBandsTableModel tableModel;

    public AddBandToTimeSeriesPane(ProductNode node, List<RasterDataNode> rasterList) {
        this.node = node;
        this.rasterList = rasterList;
        createComponents();
    }

    private void createComponents() {
        TableLayout layout = new TableLayout(1);
        layout.setTablePadding(8, 8);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setRowFill(1, TableLayout.Fill.BOTH);
        layout.setRowWeightY(1, 1.0);

        JLabel textLabel = new JLabel("<html>Adding band " + node.getDisplayName() + " to time series.<br>" +
                                      "The following products feature a band with the same name.<br>" +
                                      "Choose which to add to the time series.</html>");

        tableModel = new AddBandsTableModel(node, rasterList);
        table = new JTable(tableModel);
        table.setRowSelectionAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setBorder(new LineBorder(Color.BLACK, 1, false));
        table.setPreferredScrollableViewportSize(new Dimension(450, 100));
        table.setFillsViewportHeight(true);

        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setHeaderValue("Product");
        columnModel.getColumn(1).setHeaderValue("Within Time-Span");
        columnModel.getColumn(2).setHeaderValue("Add");
        final JTableHeader tableHeader = new JTableHeader(columnModel);
        tableHeader.setVisible(true);
        table.setTableHeader(tableHeader);
        final BooleanCheckBoxCellRenderer checkBoxCellRenderer = new BooleanCheckBoxCellRenderer();
        checkBoxCellRenderer.setEnabled(false);
        columnModel.getColumn(1).setCellRenderer(checkBoxCellRenderer);
        columnModel.getColumn(2).setCellRenderer(new AddStateRenderer());

        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(1).setMinWidth(100);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMinWidth(50);

        final JButton selectAllButton = new JButton("Select all");
        final JButton selectNoneButton = new JButton("Select none");

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(selectAllButton);
        buttonPanel.add(selectNoneButton);

        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(true, i, 2);
                    table.repaint();
                }
            }
        });
        selectNoneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(false, i, 2);
                    table.repaint();
                }
            }
        });

        setLayout(layout);
        add(textLabel);
        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setMinimumSize(new Dimension(250, 80));
        add(scrollPane);
        add(buttonPanel);
    }

    public List<RasterDataNode> getAddedRasterList() {
        final ArrayList<RasterDataNode> addedRasterList = new ArrayList<RasterDataNode>();
        for (TableEntry tableEntry : tableModel.entryList) {
            if (tableEntry.isAddToTimeSeries()) {
                addedRasterList.add(tableEntry.getRaster());
            }
        }
        return addedRasterList;
    }

    private class AddBandsTableModel extends AbstractTableModel {

        List<TableEntry> entryList = new ArrayList<TableEntry>();
        ProductNode refRaster;

        public AddBandsTableModel(ProductNode refRaster, List<RasterDataNode> rasters) {
            this.refRaster = refRaster;
            for (RasterDataNode raster : rasters) {
                entryList.add(new TableEntry(raster));
            }
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return entryList.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final TableEntry tableEntry = entryList.get(rowIndex);
            final RasterDataNode raster = tableEntry.getRaster();
            final TimeSeries timeSeries = TimeSeries.getInstance();
            switch (columnIndex) {
                case 0:
                    return raster.getProduct().getDisplayName();
                case 1:
                    return timeSeries.isWithinTimeSpan(raster);
                case 2:
                    return tableEntry.isAddToTimeSeries();
                default:
                    throw new IllegalStateException("Wrong column number specified.");
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final RasterDataNode raster = entryList.get(rowIndex).getRaster();
            if (columnIndex == 2) {
                if (!raster.equals(refRaster)) {
                    entryList.get(rowIndex).setAddToTimeSeries((Boolean) aValue);
                }
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0) {
                return String.class;
            } else {
                return Boolean.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 && !refRaster.equals(entryList.get(rowIndex).getRaster());
        }
    }

    private static class TableEntry {

        private RasterDataNode raster;
        private boolean addToTimeSeries;

        private TableEntry(RasterDataNode raster) {
            this.addToTimeSeries = true;
            this.raster = raster;
        }

        public RasterDataNode getRaster() {
            return raster;
        }

        public boolean isAddToTimeSeries() {
            return addToTimeSeries;
        }

        public void setAddToTimeSeries(boolean addToTimeSeries) {
            this.addToTimeSeries = addToTimeSeries;
        }
    }

    private class AddStateRenderer extends BooleanCheckBoxCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int i, int i1) {
            final AddBandsTableModel model = (AddBandsTableModel) jTable.getModel();
            final Component component = super.getTableCellRendererComponent(jTable, o, b, b1, i, i1);
            component.setEnabled(!node.equals(model.entryList.get(i).getRaster()));
            return component;
        }
    }
}
