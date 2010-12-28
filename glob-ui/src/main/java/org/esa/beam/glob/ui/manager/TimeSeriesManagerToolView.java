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

package org.esa.beam.glob.ui.manager;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.WeakHashMap;


public class TimeSeriesManagerToolView extends AbstractToolView {

    private final AppContext appContext;
    private JPanel controlPanel;
    private Product selectedProduct;
    private String prefixTitle;

    private final WeakHashMap<Product, TimeSeriesManagerForm> formMap;
    private TimeSeriesManagerForm activeForm;
    private final TimeSeriesManagerTSL timeSeriesManagerTSL;

    public TimeSeriesManagerToolView() {
        formMap = new WeakHashMap<Product, TimeSeriesManagerForm>();
        appContext = VisatApp.getApp();
        timeSeriesManagerTSL = new TimeSeriesManagerTSL();
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

        VisatApp.getApp().addProductTreeListener(new TSManagerPTL());
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
            if (oldProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(oldProduct);
                if (timeSeries != null) {
                    timeSeries.removeTimeSeriesListener(timeSeriesManagerTSL);
                }
            }

            selectedProduct = newProduct;
            realizeActiveForm();
            updateTitle();

            if (newProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(newProduct);
                if (timeSeries != null) {
                    timeSeries.addTimeSeriesListener(timeSeriesManagerTSL);
                }
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

    protected TimeSeriesManagerForm getOrCreateActiveForm(Product product) {
        if (formMap.containsKey(product)) {
            activeForm = formMap.get(product);
        } else {
            activeForm = new TimeSeriesManagerForm(getDescriptor());
            formMap.put(product, activeForm);
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

    private class TSManagerPTL extends ProductTreeListenerAdapter {

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

    private class TimeSeriesManagerTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            if (event.getType() == TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME ||
                event.getType() == TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME) {
                activeForm.updateFormControl(getSelectedProduct());
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            activeForm.updateFormControl(getSelectedProduct());
        }

    }
}
