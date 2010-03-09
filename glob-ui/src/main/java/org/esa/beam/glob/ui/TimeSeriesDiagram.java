package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.diagram.Diagram;
import org.esa.beam.framework.ui.diagram.DiagramAxis;
import org.esa.beam.framework.ui.diagram.DiagramGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TimeSeriesDiagram extends Diagram {

    private TimeSeriesGraph currentGraph;
    private List<RasterDataNode> raster;

    public TimeSeriesDiagram(List<Product> products) {
        setXAxis(new DiagramAxis("Time", "IpW"));
        setYAxis(new DiagramAxis("Value", "1"));
        currentGraph = new TimeSeriesGraph(products, this);
        this.addGraph(currentGraph);
    }

    public void clearDiagram() {
        final TimeSeriesGraph currentGraph = getCurrentGraph();
        if (currentGraph != null) {
            removeGraph(currentGraph);
        }
    
    }

    public TimeSeriesGraph getCurrentGraph() {
        return currentGraph;
    }

    public void update(int pixelX, int pixelY, int currentLevel) {
        for(RasterDataNode r : raster) {
            currentGraph.update(r, pixelX, pixelY, currentLevel);
        }
    }

    public void setBands(List<RasterDataNode> raster) {
        this.raster = raster;
    }
}
