/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.glob.ui;

import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;

import javax.swing.ListModel;
import java.io.File;
import java.io.Serializable;
import java.util.List;

public interface ProductLocationsPaneModel extends ListModel, Serializable {

    @Override
    ProductLocation getElementAt(int index);

    void addFiles(File... files);

    void addDirectory(File directory, boolean recursive);

    void remove(int... indices);

    List<ProductLocation> getProductLocations();
}
