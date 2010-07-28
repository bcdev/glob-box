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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;

public class NewTimeSeriesFromBandAssistantAction extends AbstractTimeSeriesAssistantAction {

    @Override
    protected TimeSeriesAssistantModel createModel() {
        final TimeSeriesAssistantModel assistantModel = new TimeSeriesAssistantModel();
        final AppContext appContext = getAppContext();
        final Product[] allProducts = appContext.getProductManager().getProducts();
        final Product selectedProduct = appContext.getSelectedProduct();
        for (Product product : allProducts) {
            if (product.getProductType().equals(selectedProduct.getProductType())) {
                assistantModel.getProductLocationsModel().addFiles(product.getFileLocation());
            }
        }
        final Band[] bands = selectedProduct.getBands();
        for (Band band : bands) {
            final ProductNode selectedProductNode = VisatApp.getApp().getSelectedProductNode();
            final Variable variable = new Variable(band.getName(), band == selectedProductNode);
            assistantModel.getVariableSelectionModel().add(variable);
        }
        return assistantModel;
    }
}
