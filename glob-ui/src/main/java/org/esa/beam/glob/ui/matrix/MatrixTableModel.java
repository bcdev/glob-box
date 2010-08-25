package org.esa.beam.glob.ui.matrix;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;

import javax.swing.table.AbstractTableModel;

class MatrixTableModel extends AbstractTableModel {

    private int size;
    private Band band;
    private int centerPixelX;
    private int centerPixelY;

    MatrixTableModel(int matrixSize) {
        this.size = matrixSize;
        band = null;
        centerPixelX = -1;
        centerPixelY = -1;
    }

    @Override
    public int getRowCount() {
        return size;
    }

    @Override
    public int getColumnCount() {
        return size;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Double.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final int centerOffset = MathUtils.floorInt(size / 2.0);
        int pixelX = centerPixelX - centerOffset + columnIndex;
        int pixelY = centerPixelY - centerOffset + rowIndex;

        if (band != null && band.isPixelValid(pixelX, pixelY)) {
            return ProductUtils.getGeophysicalSampleDouble(band, pixelX, pixelY, 0);
        } else {
            return Double.NaN;
        }
    }

    public void setMatrixSize(int matrixSize) {
        if (this.size != matrixSize) {
            this.size = matrixSize;
            fireTableStructureChanged();
        }
    }

    public void setBand(Band band) {
        if (this.band != band) {
            this.band = band;
            fireTableDataChanged();
        }
    }

    public Band getBand() {
        return band;
    }

    /**
     * Sets the center pixel position of the matrix
     *
     * @param pixelX center x position
     * @param pixelY center y position
     */
    public void setCenterPixel(int pixelX, int pixelY) {
        if (this.centerPixelX != pixelX || this.centerPixelY != pixelY) {
            this.centerPixelX = pixelX;
            this.centerPixelY = pixelY;
            fireTableDataChanged();
        }
    }
}
