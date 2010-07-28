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

import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesFactory;
import org.esa.beam.visat.VisatApp;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

abstract class AbstractTimeSeriesAssistantPage extends AbstractAssistantPage {

    private final TimeSeriesAssistantModel assistantModel;
    private AbstractTimeSeriesAssistantPage.MyChangeListener changeListener;

    AbstractTimeSeriesAssistantPage(String pageTitle, TimeSeriesAssistantModel model) {
        super(pageTitle);
        assistantModel = model;
        changeListener = new MyChangeListener();
        assistantModel.addChangeListener(changeListener);
    }

    protected TimeSeriesAssistantModel getAssistantModel() {
        return assistantModel;
    }

    @Override
    public boolean performFinish() {
        addTimeSeriesProductToVisat(getAssistantModel());
        assistantModel.removeChangeListener(changeListener);
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        assistantModel.removeChangeListener(changeListener);
        return null;
    }

    private void addTimeSeriesProductToVisat(TimeSeriesAssistantModel assistantModel) {
        final ProductLocationsPaneModel locationsModel = assistantModel.getProductLocationsModel();
        final VariableSelectionPaneModel variablesModel = assistantModel.getVariableSelectionModel();
        final AbstractTimeSeries timeSeries = TimeSeriesFactory.create(assistantModel.getTimeSeriesName(),
                                                                       locationsModel.getProductLocations(),
                                                                       variablesModel.getSelectedVariableNames());
        VisatApp.getApp().getProductManager().addProduct(timeSeries.getTsProduct());
    }

    private class MyChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            getContext().updateState();
        }
    }
}
