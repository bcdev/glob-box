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
            if(product.getProductType().equals(selectedProduct.getProductType())) {
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
