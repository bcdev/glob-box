package org.esa.beam.glob.ui.assistant;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.ui.ProductLocationsPaneModel;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;

import javax.swing.JLabel;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TimeSeriesAssistantPage_ReprojectingSources extends AbstractTimeSeriesAssistantPage {

    private Product crsReferenceProduct;

    TimeSeriesAssistantPage_ReprojectingSources(TimeSeriesAssistantModel model) {
        super("Reproject Source Products", model);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canHelp() {
        // @todo
        return false;
    }

    @Override
    public boolean validatePage() {
        return super.validatePage();  //Todo change body of created method. Use File | Settings | File Templates to change
    }

    @Override
    public AssistantPage getNextPage() {
        reprojectSourceProducts();
        return super.getNextPage();  //Todo change body of created method. Use File | Settings | File Templates to change
    }

    private void reprojectSourceProducts() {
        final ProductLocationsPaneModel productLocationsModel = getAssistantModel().getProductLocationsModel();
        final List<ProductLocation> productLocations = productLocationsModel.getProductLocations();
        for (ProductLocation productLocation : productLocations) {
            final List<Product> products = productLocation.getProducts();
            for (Product product : products) {
                Product targetProduct = createTargetProduct(product);
                getAssistantModel().addProjectedProduct(targetProduct);
            }
        }
    }

    private Product createTargetProduct(Product product) {
        final Map<String, Product> productMap = getProductMap(product);
        final Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("resamplingName", "Nearest");
        parameterMap.put("includeTiePointGrids", false);
        parameterMap.put("addDeltaBands", false);
        // @todo
        return GPF.createProduct("Reproject", parameterMap, productMap);
    }


    private Map<String, Product> getProductMap(Product product) {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", product);
        productMap.put("collocateWith", getCrsReferenceProduct());
        return productMap;
    }


    @Override
    protected Component createPageComponent() {
        return new JLabel("Da kommt noch was");
    }

    public Product getCrsReferenceProduct() {
        return crsReferenceProduct;
    }
}
