package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.diagram.Diagram;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesDiagram extends Diagram {

    private TimeSeriesGraph currentGraph;

    public void clearDiagram() {
        final TimeSeriesGraph currentGraph = getCurrentGraph();
        if (currentGraph != null) {
            removeGraph(currentGraph);
        }
    
    }

    public TimeSeriesGraph getCurrentGraph() {
        return (TimeSeriesGraph) getGraph(0);
    }

    public void update(int pixelX, int pixelY, int currentLevel) {
        
    }

    public void setBands(List<RasterDataNode> raster) {

    }
}
