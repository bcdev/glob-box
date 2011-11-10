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

package org.esa.beam.glob.ui.assistant;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.ui.ProductLocationsPane;
import org.esa.beam.glob.ui.ProductLocationsPaneModel;
import org.esa.beam.glob.ui.Variable;

import java.awt.Component;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

class TimeSeriesAssistantPage_SourceProducts extends AbstractTimeSeriesAssistantPage {

    TimeSeriesAssistantPage_SourceProducts(TimeSeriesAssistantModel model) {
        super("Define Time Series Sources", model);
    }

    @Override
    protected Component createPageComponent() {
        final ProductLocationsPaneModel locationsModel = getAssistantModel().getProductLocationsModel();
        return new ProductLocationsPane(locationsModel);
    }

    @Override
    public boolean validatePage() {
        if (super.validatePage()) {
            final ProductLocationsPaneModel locationsModel = getAssistantModel().getProductLocationsModel();
            if (locationsModel.getSize() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        removeModeListener();
        final TimeSeriesAssistantModel model = getAssistantModel();
        final ProgressMonitorSwingWorker worker = new MyProgressMonitorSwingWorker(model);
        worker.executeWithBlocking();

        if (allProductsOnSameGrid()) {
            return new TimeSeriesAssistantPage_VariableSelection(model);
        } else {
            return new TimeSeriesAssistantPage_ReprojectingSources(model);
        }
    }

    private boolean allProductsOnSameGrid() {
//        @todo se - implement
        return false;
    }

    @Override
    public boolean canFinish() {
        return false;

    }

    private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Variable[], Object> {

        private final TimeSeriesAssistantModel model;

        private MyProgressMonitorSwingWorker(TimeSeriesAssistantModel model) {
            super(TimeSeriesAssistantPage_SourceProducts.this.getContext().getCurrentPage().getPageComponent(),
                  "Scanning for products");
            this.model = model;
        }

        @Override
        protected Variable[] doInBackground(ProgressMonitor pm) throws Exception {
            return getVariables(getAssistantModel().getProductLocationsModel(), pm);
        }

        @Override
        protected void done() {
            try {
                model.getVariableSelectionModel().set(get());
            } catch (InterruptedException ignored) {
            } catch (ExecutionException e) {
                getContext().showErrorDialog("Failed to scan for products: \n" + e.getMessage());
                e.printStackTrace();
            }
        }

        private Variable[] getVariables(ProductLocationsPaneModel locationsModel, ProgressMonitor pm) {
            try {
                pm.beginTask("Scanning product locations...", locationsModel.getSize());
                for (int i = 0; i < locationsModel.getSize(); i++) {
                    final ProductLocation location = locationsModel.getElementAt(i);
                    location.loadProducts(new SubProgressMonitor(pm, 1));
                    final Collection<Product> products = location.getProducts().values();
                    if (!products.isEmpty()) {
                        final Product product = products.iterator().next();
                        final String[] bandNames = product.getBandNames();
                        final Variable[] variables = new Variable[bandNames.length];
                        for (int j = 0; j < bandNames.length; j++) {
                            variables[j] = new Variable(bandNames[j]);
                        }
                        location.closeProducts();
//                        @todo se - ?? return variables ?? - really a shortcut after the first product?
                        return variables;
                    } else {
                        location.closeProducts();
                    }
                }
            } finally {
                pm.done();
            }

            return new Variable[0];
        }

    }
}
