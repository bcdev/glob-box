package org.esa.beam.glob.ui;

import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;

import javax.swing.ListModel;
import java.io.File;
import java.io.Serializable;
import java.util.List;

public interface ProductLocationsPaneModel extends ListModel, Serializable {

    @Override
    int getSize();

    @Override
    ProductLocation getElementAt(int index);

    void addFiles(File... files);

    void addDirectory(File directory, boolean recursive);

    void remove(int... indices);

    List<ProductLocation> getProductLocations();
}
