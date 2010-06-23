package org.esa.beam.glob.ui;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

class TimeSeriesAssistantModel {

    private ProductSourcePaneModel productSourcePaneModel;
    private VariableSelectionPaneModel variableSelectionPaneModel;
    private String timeSeriesName;
    private List<ChangeListener> changeListenerList;

    TimeSeriesAssistantModel() {
        this("TimeSeries", new ProductSourcePaneModel(), new VariableSelectionPaneModel());
    }

    private TimeSeriesAssistantModel(String timeSeriesName, ProductSourcePaneModel productSourcePaneModel,
                                     VariableSelectionPaneModel variableSelectionPaneModel) {
        this.productSourcePaneModel = productSourcePaneModel;
        this.variableSelectionPaneModel = variableSelectionPaneModel;
        this.timeSeriesName = timeSeriesName;
        final ListDataListenerDelegate dataListenerDelegate = new ListDataListenerDelegate();
        productSourcePaneModel.addListDataListener(dataListenerDelegate);
        variableSelectionPaneModel.addListDataListener(dataListenerDelegate);

    }

    public ProductSourcePaneModel getProductSourcePaneModel() {
        return productSourcePaneModel;
    }

    public VariableSelectionPaneModel getVariableSelectionPaneModel() {
        return variableSelectionPaneModel;
    }

    public void setTimeSeriesName(String timeSeriesName) {
        if (!this.timeSeriesName.equals(timeSeriesName)) {
            this.timeSeriesName = timeSeriesName;
            fireChangeEvent();
        }
    }

    public String getTimeSeriesName() {
        return timeSeriesName;
    }

    public void addChangeListener(ChangeListener changeListener) {
        if (changeListenerList == null) {
            changeListenerList = new ArrayList<ChangeListener>();
        }
        if(!changeListenerList.contains(changeListener)) {
            changeListenerList.add(changeListener);
        }
    }

    public void removeChangeListener(ChangeListener changeListener) {
        if(changeListenerList != null) {
            changeListenerList.remove(changeListener);
        }
    }

    private void fireChangeEvent() {
        if(changeListenerList != null) {
            for (ChangeListener changeListener : changeListenerList) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    private class ListDataListenerDelegate implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            fireChangeEvent();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            fireChangeEvent();

        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            fireChangeEvent();
        }
    }
}
