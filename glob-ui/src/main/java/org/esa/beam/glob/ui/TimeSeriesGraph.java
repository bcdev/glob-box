package org.esa.beam.glob.ui;

import org.esa.beam.framework.ui.diagram.AbstractDiagramGraph;

public class TimeSeriesGraph extends AbstractDiagramGraph {

    @Override
    public String getXName() {
        return null;
    }

    @Override
    public String getYName() {
        return null;
    }

    @Override
    public int getNumValues() {
        return 0;
    }

    @Override
    public double getXValueAt(int index) {
        return 0;
    }

    @Override
    public double getYValueAt(int index) {
        return 0;
    }

    @Override
    public double getXMin() {
        return 0;
    }

    @Override
    public double getXMax() {
        return 0;
    }

    @Override
    public double getYMin() {
        return 0;
    }

    @Override
    public double getYMax() {
        return 0;
    }
}
