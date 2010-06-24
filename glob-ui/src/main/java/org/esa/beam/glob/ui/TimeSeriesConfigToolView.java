package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.layermanager.AbstractLayerToolView;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.WeakHashMap;


// todo (mp 20100624) If variable is removed view must be closed

public class TimeSeriesConfigToolView extends AbstractLayerToolView {
    public static final String ID = "timeSeriesConfigToolView";

    private WeakHashMap<ProductSceneView, TimeSeriesConfigForm> formMap;
    private TimeSeriesConfigForm activeForm;

    public TimeSeriesConfigToolView() {
        formMap = new WeakHashMap<ProductSceneView, TimeSeriesConfigForm>();
    }

    @Override
    protected JComponent createControl() {
        // todo (mp 20100624) Product Tree listener not yet implemented
        VisatApp.getApp().addProductTreeListener(new TimeSeriesPTL());
        final JComponent control = super.createControl();
        realizeActiveForm();
        return control;
    }

    @Override
    protected void viewClosed(ProductSceneView view) {
        formMap.remove(view);
    }

    @Override
    protected void viewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
        realizeActiveForm();
    }

    private void realizeActiveForm() {
        final JPanel controlPanel = getControlPanel();

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        activeForm = getOrCreateActiveForm(getSelectedView());
        controlPanel.add(activeForm.getControl(), BorderLayout.CENTER);

        controlPanel.validate();
        controlPanel.repaint();
    }

    protected TimeSeriesConfigForm getOrCreateActiveForm(ProductSceneView view) {
        if (formMap.containsKey(view)) {
            activeForm = formMap.get(view);
        } else {
            activeForm = new TimeSeriesConfigForm();
            formMap.put(view, activeForm);
        }
        Product product = null;
        if(view != null) {
            product = view.getProduct();
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

     private class TimeSeriesPTL extends ProductTreeListenerAdapter {

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            if (productNode instanceof RasterDataNode) {
            }
        }
    }
}
