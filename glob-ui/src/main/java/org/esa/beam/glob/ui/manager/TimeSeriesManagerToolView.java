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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.insitu.InsituSource;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    Product getSelectedProduct() {
        return selectedProduct;
    }

    private void productClosed(Product product) {
        formMap.remove(product);
        setSelectedProduct(null);
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

    private void realizeActiveForm() {
        final JPanel controlPanel = this.controlPanel;

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        activeForm = getOrCreateActiveForm(getSelectedProduct());
        controlPanel.add(activeForm.getControl(), BorderLayout.CENTER);

        controlPanel.validate();
        controlPanel.repaint();
    }

    private TimeSeriesManagerForm getOrCreateActiveForm(Product product) {
        if (formMap.containsKey(product)) {
            activeForm = formMap.get(product);
        } else {
            activeForm = new TimeSeriesManagerForm(getDescriptor());
            formMap.put(product, activeForm);
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

    private void updateInsituPins(String insituVariable) {
        final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(selectedProduct);
        final Product tsProduct = timeSeries.getTsProduct();
        final PlacemarkGroup pinGroup = tsProduct.getPinGroup();
        if(!timeSeries.hasInsituData()) {
            for (Placemark insituPin : timeSeries.getInsituPlacemarks()) {
                pinGroup.remove(insituPin);
            }
            return;
        }
        final InsituSource insituSource = timeSeries.getInsituSource();
        final List<String> selectedInsituVariables = getSelectedInsituVariables(timeSeries, insituSource);
        if(selectedInsituVariables.contains(insituVariable)) {
            removePlacemarks(timeSeries.getInsituPlacemarks(), pinGroup);
        }

        addPlacemarks(tsProduct, timeSeries, selectedInsituVariables);
    }

    private void addPlacemarks(Product tsProduct, AbstractTimeSeries timeSeries, List<String> selectedInsituVariables) {
        final InsituSource insituSource = timeSeries.getInsituSource();
        PlacemarkGroup pinGroup = tsProduct.getPinGroup();
        final Map<String, GeoPos[]> geoPoses = new HashMap<String, GeoPos[]>();
        for (String selectedInsituVariable : selectedInsituVariables) {
            geoPoses.put(selectedInsituVariable, insituSource.getInsituPositionsFor(selectedInsituVariable));
        }
        final GeoCoding geoCoding = tsProduct.getGeoCoding();

        int counter = 1;
        final PixelPos pixelPos = new PixelPos();
        for (Map.Entry<String, GeoPos[]> entry : geoPoses.entrySet()) {
            for (GeoPos geoPos : entry.getValue()) {
                geoCoding.getPixelPos(geoPos, pixelPos);
                if (!AbstractTimeSeries.isPixelValid(tsProduct, pixelPos)) {
                    continue;
                }
                final String name = "Insitu_" + entry.getKey() + "_" + counter;
                // todo - ts - create better name, label, and description
                final Placemark placemark = Placemark.createPointPlacemark(
                        PinDescriptor.getInstance(),
                        name,
                        name,
                        name,
                        null,
                        geoPos,
                        geoCoding);
                if (placemark != null) {
                    pinGroup.add(placemark);
                }
                counter++;
                timeSeries.getInsituPlacemarks().add(placemark);
            }
        }
    }

    private void removePlacemarks(List<Placemark> insituVariable, PlacemarkGroup pinGroup) {
        for (int i = 0; i < pinGroup.getNodeCount(); i++) {
            final Placemark placemark = pinGroup.get(i);
            if(insituVariable.contains(placemark)) {
                pinGroup.remove(placemark);
            }
        }
    }

    private List<String> getSelectedInsituVariables(AbstractTimeSeries timeSeries, InsituSource insituSource) {
        final String[] parameterNames = insituSource.getParameterNames();
        final List<String> selectedInsituVariables = new ArrayList<String>();
        for (String parameterName : parameterNames) {
            if (timeSeries.isInsituVariableSelected(parameterName)) {
                selectedInsituVariables.add(parameterName);
            }
        }
        return selectedInsituVariables;
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
            final int type = event.getType();
            if (type == TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME ||
                type == TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME) {
                activeForm.updateFormControl(getSelectedProduct());
            } else if(type == TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION) {
                updateInsituPins(event.getValue().toString());
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            activeForm.updateFormControl(getSelectedProduct());
        }
    }
}
