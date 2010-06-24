package org.esa.beam.glob.ui;

import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocationType;

import javax.swing.AbstractListModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ProductLocationsPaneModel extends AbstractListModel {

    private List<ProductLocation> productLocationList;

    ProductLocationsPaneModel() {
        productLocationList = new ArrayList<ProductLocation>();
    }

    @Override
    public int getSize() {
        return productLocationList.size();
    }

    @Override
    public ProductLocation getElementAt(int index) {
        return productLocationList.get(index);
    }

    public void addFiles(File... files) {
        final int startIndex = productLocationList.size();
        for (File file : files) {
            productLocationList.add(new ProductLocation(ProductLocationType.FILE, file.getAbsolutePath()));
        }
        final int stopIndex = productLocationList.size() - 1;
        fireIntervalAdded(this, startIndex, stopIndex);
    }

    public void addDirectory(File currentDir, boolean recursive) {
        final ProductLocationType locationType = recursive ? ProductLocationType.DIRECTORY_REC : ProductLocationType.DIRECTORY;
        productLocationList.add(new ProductLocation(locationType,currentDir.getPath()));
        final int index = productLocationList.size() - 1;
        fireIntervalAdded(this, index, index);
    }

    public void remove(int... indices) {
        if (indices.length > 0) {
            final List<ProductLocation> toRemoveList = new ArrayList<ProductLocation>();
            for (int index : indices) {
                toRemoveList.add(productLocationList.get(index));
            }
            productLocationList.removeAll(toRemoveList);
            fireContentsChanged(this, indices[0], indices[indices.length - 1]);
        }
    }

    public List<ProductLocation> getProductLocations() {
        return new ArrayList<ProductLocation>(productLocationList); 
    }
}
