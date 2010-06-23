package org.esa.beam.glob.ui;

import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class VariableSelectionPaneModel extends AbstractListModel {

    private List<TimeVariable> timeVariables;

    VariableSelectionPaneModel() {
        timeVariables = new ArrayList<TimeVariable>();
    }

    @Override
    public int getSize() {
        return timeVariables.size();
    }

    @Override
    public TimeVariable getElementAt(int index) {
        return timeVariables.get(index);
    }

    public void add(TimeVariable... variables) {
        final int startIndex = timeVariables.size();
        timeVariables.addAll(Arrays.asList(variables));
        final int stopIndex = timeVariables.size() - 1;
        fireIntervalAdded(this, startIndex, stopIndex);

    }
}
