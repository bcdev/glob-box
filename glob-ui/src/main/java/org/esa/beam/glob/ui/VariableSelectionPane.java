package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

class VariableSelectionPane extends JPanel {

    private VariableSelectionPaneModel model;

    VariableSelectionPane(VariableSelectionPaneModel variableSelectionPaneModel) {
        model = variableSelectionPaneModel;
        createUI();
    }

    private void createUI() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTableWeightX(1.0);
        setLayout(tableLayout);
        final CheckBoxList variableList = new CheckBoxList(model);
        variableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final CheckBoxListSelectionModel selectionModel = variableList.getCheckBoxListSelectionModel();
        variableList.setCheckBoxListSelectedIndices(getSelectedIndices(model));
        selectionModel.addListSelectionListener(new CheckBoxSelectionListener(variableList));
        add(new JScrollPane(variableList));
    }

    private int[] getSelectedIndices(VariableSelectionPaneModel model) {
        final List<Integer> indexList = new ArrayList<Integer>();
        for( int i = 0; i < model.getSize(); i++) {
            if(model.getElementAt(i).isSelected()) {
                indexList.add(i);
            }
        }
        final int[] indices = new int[indexList.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indexList.get(i);
        }
        return indices;
    }

    private class CheckBoxSelectionListener implements ListSelectionListener {

        private final CheckBoxList variableList;

        private CheckBoxSelectionListener(CheckBoxList variableList) {
            this.variableList = variableList;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                final CheckBoxListSelectionModel selectionModel = variableList.getCheckBoxListSelectionModel();
                for (int i = e.getFirstIndex(); i <= + e.getLastIndex(); i++) {
                    final TimeVariable variable = model.getElementAt(i);
                    variable.setSelected(selectionModel.isSelectedIndex(i));
                }
            }
        }
    }
}
