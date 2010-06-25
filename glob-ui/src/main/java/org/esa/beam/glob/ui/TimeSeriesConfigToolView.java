package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.WeakHashMap;


// todo (mp 20100624) If variable is removed view must be closed

public class TimeSeriesConfigToolView extends AbstractToolView {

    private AppContext appContext;
    private JPanel controlPanel;
    private Product selectedProduct;
    private String prefixTitle;

    private WeakHashMap<Product, TimeSeriesConfigForm> formMap;
    private TimeSeriesConfigForm activeForm;
    private TimeSeriesConfigToolView.TSConfigPNL tsConfigPNL;

    public TimeSeriesConfigToolView() {
        formMap = new WeakHashMap<Product, TimeSeriesConfigForm>();
        appContext = VisatApp.getApp();
        tsConfigPNL = new TSConfigPNL();
    }

    protected JPanel getControlPanel() {
        return controlPanel;
    }

    @Override
    protected JComponent createControl() {
        controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        prefixTitle = getDescriptor().getTitle();

        setSelectedProduct(appContext.getSelectedProduct());

        VisatApp.getApp().addProductTreeListener(new TSConfigPTL());
        realizeActiveForm();
        updateTitle();
        return controlPanel;
    }

    private void updateTitle() {
        final String suffix;
        final Product product = getSelectedProduct();
        if (product != null) {
            suffix = " - " + product.getDisplayName();
        } else {
            suffix = "";
        }
        getDescriptor().setTitle(prefixTitle + suffix);
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    private void setSelectedProduct(Product newProduct) {
        Product oldProduct = selectedProduct;
        if (newProduct != oldProduct) {
            if(oldProduct != null) {
                oldProduct.removeProductNodeListener(tsConfigPNL);
            }
            
            selectedProduct = newProduct;
            realizeActiveForm();
            updateTitle();

            if(newProduct != null) {
                selectedProduct.addProductNodeListener(tsConfigPNL);
            }
        }
    }

    protected void productClosed(Product product) {
        formMap.remove(product);
        setSelectedProduct(null);
    }

    private void realizeActiveForm() {
        final JPanel controlPanel = getControlPanel();

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        activeForm = getOrCreateActiveForm(getSelectedProduct());
        controlPanel.add(activeForm.getControl(), BorderLayout.CENTER);

        controlPanel.validate();
        controlPanel.repaint();
    }

    protected TimeSeriesConfigForm getOrCreateActiveForm(Product product) {
        if (formMap.containsKey(product)) {
            activeForm = formMap.get(product);
        } else {
            activeForm = new TimeSeriesConfigForm();
            formMap.put(product, activeForm);
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

    private class TSConfigPTL extends ProductTreeListenerAdapter {

        @Override
        public void productRemoved(Product product) {
            productClosed(product);
        }

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            setSelectedProduct(getProduct(productNode));
        }

        private Product getProduct(ProductNode productNode) {
            while (true) {
                if (productNode instanceof ProductNodeGroup<?>) {
                    ProductNodeGroup<?> productNodeGroup = (ProductNodeGroup<?>) productNode;
                    if (productNodeGroup.getNodeCount() > 0) {
                        productNode = productNodeGroup.get(0);
                        continue;
                    }
                }
                return productNode.getProduct();
            }
        }
    }

    private class TSConfigPNL extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            activeForm.updateFormControl(getSelectedProduct());
        }
    }
}
