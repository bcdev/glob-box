package org.esa.beam.glob.ui;

import javax.swing.ListModel;
import java.io.Serializable;
import java.util.List;

public interface VariableSelectionPaneModel extends ListModel, Serializable {

    @Override
    int getSize();

    @Override
    Variable getElementAt(int index);

    void set(Variable... variables);

    void add(Variable... variables);

    void setSelectedVariableAt(int index, boolean selected);

    List<String> getSelectedVariableNames();
}
