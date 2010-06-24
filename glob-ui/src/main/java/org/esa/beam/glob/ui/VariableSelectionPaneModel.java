package org.esa.beam.glob.ui;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class VariableSelectionPaneModel extends AbstractListModel {

    private List<Variable> variableList;

    VariableSelectionPaneModel() {
        variableList = new ArrayList<Variable>();
    }

    @Override
    public int getSize() {
        return variableList.size();
    }

    @Override
    public Variable getElementAt(int index) {
        return variableList.get(index);
    }

    public void set(Variable... variables) {
        final int index = variableList.size();
        variableList.clear();
        fireIntervalRemoved(this, 0, index);
        add(variables);
    }

    public void add(Variable... variables) {
        final int startIndex = variableList.size();
        variableList.addAll(Arrays.asList(variables));
        final int stopIndex = variableList.size() - 1;
        fireIntervalAdded(this, startIndex, stopIndex);
    }

    public void setSelectedVariableAt(int index, boolean selected) {
        final Variable variable = variableList.get(index);
        if (variable.isSelected() != selected) {
            variable.setSelected(selected);
            fireContentsChanged(this, index, index);
        }
    }

    public List<String> getSelectedVariableNames() {
        final List<String> nameList = new ArrayList<String>();

        for (Variable variable : variableList) {
            if(variable.isSelected()) {
                nameList.add(variable.getName());
            }
        }
        return nameList;
    }

}
