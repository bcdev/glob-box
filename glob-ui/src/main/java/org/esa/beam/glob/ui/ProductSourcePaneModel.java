package org.esa.beam.glob.ui;

import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocationType;

import javax.swing.AbstractListModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ProductSourcePaneModel extends AbstractListModel {

    private List<ProductLocation> productSourceList;

    ProductSourcePaneModel() {
        productSourceList = new ArrayList<ProductLocation>();
    }

    @Override
    public int getSize() {
        return productSourceList.size();
    }

    @Override
    public Object getElementAt(int index) {
        return productSourceList.get(index);
    }

    public void addFiles(File... files) {
        final int startIndex = productSourceList.size();
        for (File file : files) {
            productSourceList.add(new ProductLocation(ProductLocationType.FILE, file.getAbsolutePath()));
        }
        final int stopIndex = productSourceList.size() - 1;
        fireIntervalAdded(this, startIndex, stopIndex);
    }

    public void addDirectory(File currentDir, boolean recursive) {
        final ProductLocationType locationType = recursive ? ProductLocationType.DIRECTORY_REC : ProductLocationType.DIRECTORY;
        productSourceList.add(new ProductLocation(locationType,currentDir.getPath()));
        final int index = productSourceList.size() - 1;
        fireIntervalAdded(this, index, index);
    }

    public void remove(int... indices) {
        if (indices.length > 0) {
            final List<ProductLocation> toRemoveList = new ArrayList<ProductLocation>();
            for (int index : indices) {
                toRemoveList.add(productSourceList.get(index));
            }
            productSourceList.removeAll(toRemoveList);
            fireContentsChanged(this, indices[0], indices[indices.length - 1]);
        }
    }
}
