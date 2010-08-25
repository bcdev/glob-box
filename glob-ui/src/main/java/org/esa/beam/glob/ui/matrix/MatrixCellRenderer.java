package org.esa.beam.glob.ui.matrix;

import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

class MatrixCellRenderer extends DefaultTableCellRenderer {

    private MatrixTableModel tableModel;
    private DecimalFormat valueFormatter;

    MatrixCellRenderer(MatrixTableModel tableModel) {
        this.tableModel = tableModel;
        valueFormatter = new DecimalFormat("0.0000");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                                                                          isSelected, hasFocus,
                                                                          row, column);
        double rasterValue = (Double) value;
        final RasterDataNode raster = tableModel.getBand();
        Color bgColor = Color.WHITE;
        if (raster != null) {
            bgColor = raster.getImageInfo().getColorPaletteDef().computeColor(raster, rasterValue);
        }
        label.setBackground(bgColor);
        label.setText(valueFormatter.format(rasterValue));
        return label;

    }
}
