package org.esa.beam.glob.ui.assistant;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.ui.ProductLocationsPaneModel;
import org.esa.beam.gpf.operators.reproject.CollocationCrsForm;
import org.esa.beam.visat.VisatApp;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TimeSeriesAssistantPage_ReprojectingSources extends AbstractTimeSeriesAssistantPage {

    private MyCollocationCrsForm collocationCrsForm;

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
        return collocationCrsForm.getCollocationProduct() != null;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        final Reprojector reprojector = new Reprojector(this.getPageComponent());
        reprojector.executeWithBlocking();
        return new TimeSeriesAssistantPage_VariableSelection(getAssistantModel());
    }

    @Override
    protected Component createPageComponent() {
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                getContext().updateState();
            }
        };
        collocationCrsForm = new MyCollocationCrsForm(listener);
        collocationCrsForm.addMyChangeListener();

        final JPanel jPanel = new JPanel(new BorderLayout());
        final JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Use CRS of "), BorderLayout.WEST);
        northPanel.add(collocationCrsForm.getCrsUI());
        jPanel.add(northPanel, BorderLayout.NORTH);
        return jPanel;
    }

    private static class MyCollocationCrsForm extends CollocationCrsForm {

        private final PropertyChangeListener listener;

        public MyCollocationCrsForm(PropertyChangeListener listener) {
            super(VisatApp.getApp());
            this.listener = listener;
        }

        void addMyChangeListener() {
            super.addCrsChangeListener(listener);
        }
    }

    private class Reprojector extends ProgressMonitorSwingWorker<Void, TimeSeriesAssistantModel> {

        protected Reprojector(Component parentComponent) {
            super(parentComponent, "Reprojecting source products ...");
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            reprojectSourceProducts(pm);
            return null;
        }

        private void reprojectSourceProducts(ProgressMonitor pm) {
            final ProductLocationsPaneModel productLocationsModel = getAssistantModel().getProductLocationsModel();
            final List<ProductLocation> productLocations = productLocationsModel.getProductLocations();
            pm.beginTask("Reprojecting...", productLocations.size());
            for (ProductLocation productLocation : productLocations) {
                final Map<String, Product> products = productLocation.getProducts();
                final Product crsReferenceProduct = getCrsReferenceProduct();
                for (Map.Entry<String, Product> productEntry : products.entrySet()) {
                    final Product product = productEntry.getValue();
                    if (!product.isCompatibleProduct(crsReferenceProduct, 0.1E-4f)) {
                        Product reprojectedProduct = createProjectedProduct(product, crsReferenceProduct);
                        productEntry.setValue(reprojectedProduct);
                    }
                }
                pm.worked(1);
            }
            pm.done();
        }

        private Product createProjectedProduct(Product toReproject, Product crsReference) {
            final Map<String, Product> productMap = getProductMap(toReproject, crsReference);
            final Map<String, Object> parameterMap = new HashMap<String, Object>();
            parameterMap.put("resamplingName", "Nearest");
            parameterMap.put("includeTiePointGrids", false);
            parameterMap.put("addDeltaBands", false);
            // @todo - generalise
            final Product reprojectedProduct = GPF.createProduct("Reproject", parameterMap, productMap);
            reprojectedProduct.setStartTime(toReproject.getStartTime());
            reprojectedProduct.setEndTime(toReproject.getEndTime());
            return reprojectedProduct;
        }


        private Map<String, Product> getProductMap(Product product, Product crsReference) {
            final Map<String, Product> productMap = new HashMap<String, Product>(2);
            productMap.put("source", product);
            productMap.put("collocateWith", crsReference);
            return productMap;
        }


        private Product getCrsReferenceProduct() {
            return collocationCrsForm.getCollocationProduct();
        }
    }
}
