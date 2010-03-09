package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.diagram.AbstractDiagramGraph;

import java.util.*;

public class TimeSeriesGraph extends AbstractDiagramGraph {

    private List<Product> products;
    private int numValues;

    public TimeSeriesGraph(List<Product> products, TimeSeriesDiagram diagram) {
        this.products = products;
        this.setDiagram(diagram);
        updateGraphInfo();
    }

    @Override
    public String getXName() {
        // time
        return getDiagram().getXAxis().getName();
    }

    @Override
    public String getYName() {
        // value ("energy"?)
        return getDiagram().getYAxis().getName();
    }

    @Override
    public int getNumValues() {
        return this.numValues;
    }

    @Override
    public double getXValueAt(int index) {
        // the time-point corresponding to the index
        return index;
//        return 0;
    }

    @Override
    public double getYValueAt(int index) {
        // for the one chosen band the value at time-point index
        return 10;
//        return 0;
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

    public void addProduct(final Product product) {
        this.products.add(product);
        updateGraphInfo();
    }

    public void removeProduct(final Product product) {
        this.products.remove(product);
        updateGraphInfo();
    }

    private void updateGraphInfo() {
        numValues = products.size();
        if(numValues > 0) {
            Collections.sort(products, new Comparator<Product>(){
                @Override
                public int compare(Product p1, Product p2) {
                    if(p1.getStartTime() == null ) {
                        return -1;
                    }
                    if(p2.getStartTime() == null) {
                        return 1;
                    }
                    return p1.getStartTime().getAsDate().compareTo(p2.getStartTime().getAsDate());
                }
            });
            getDiagram().getXAxis().setValueRange(0, numValues);
        }
    }

    public void update(RasterDataNode r, int pixelX, int pixelY, int currentLevel) {
        final String rasterName = r.getName();
        getDiagram().getYAxis().setName(rasterName);
        getDiagram().getYAxis().setUnit(r.getUnit());

        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        for (Product product : products) {
            if(product.containsRasterDataNode(rasterName)) {
                rasterList.add(product.getRasterDataNode(rasterName));
            }
        }

        for (RasterDataNode node : rasterList) {

        }
    }

    public List<Product> getProducts() {
        return products;
    }
}
