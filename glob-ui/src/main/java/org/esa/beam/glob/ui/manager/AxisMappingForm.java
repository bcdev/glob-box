/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * UI component for the axis mapping.
 *
 * @author Sabine Embacher
 * @author Thomas Storm
 */
class AxisMappingForm extends ModalDialog {

    private final AxisMappingModel axisMappingModel;
    private final NameProvider nameProvider;

    private JTable aliasNames;
    private JScrollPane aliasNameScrollPane;
    private JList rasterNames;
    private JList insituNames;
    private AbstractButton removeButton;

    AxisMappingForm(AxisMappingModel axisMappingModel, NameProvider nameProvider) {
        // TODO - validate help id
        super(VisatApp.getApp().getMainFrame(), "Axis Mapping", ModalDialog.ID_OK, "axis mapping help");
        this.axisMappingModel = axisMappingModel;
        this.nameProvider = nameProvider;
        init();
    }

    private boolean shown = false;

    @Override
    public int show() {
        setButtonID(0);
        final JDialog dialog = getJDialog();
        if (!shown) {
            dialog.pack();
            center();
        }
        dialog.setMinimumSize(dialog.getSize());
        dialog.setVisible(true);
        shown = true;
        return getButtonID();
    }

    private void init() {
        final TableLayout layout = createLayout();
        final JPanel mainPanel = new JPanel(layout);
        mainPanel.add(new JLabel("Alias names"));
        mainPanel.add(new JLabel(""));
        mainPanel.add(new JLabel("Raster names"));
        mainPanel.add(new JLabel("Insitu variable names"));
        mainPanel.add(createAliasList());
        mainPanel.add(createButtonsPanel());
        mainPanel.add(createRasterNames());
        mainPanel.add(createInsituNames());
        setContent(mainPanel);
    }

    private JPanel createButtonsPanel() {
        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        final AbstractButton addButton = ToolButtonFactory.createButton(new AddAliasAction(), false);
        removeButton = ToolButtonFactory.createButton(new RemoveAliasAction(), false);
        removeButton.setEnabled(false);
        buttonsPanel.add(addButton, BorderLayout.NORTH);
        buttonsPanel.add(removeButton, BorderLayout.SOUTH);
        return buttonsPanel;
    }

    private TableLayout createLayout() {
        final TableLayout layout = new TableLayout(4);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(new Insets(4, 4, 4, 4));
        layout.setColumnFill(0, TableLayout.Fill.BOTH);
        layout.setColumnFill(1, TableLayout.Fill.VERTICAL);
        layout.setColumnFill(2, TableLayout.Fill.VERTICAL);
        layout.setColumnFill(3, TableLayout.Fill.VERTICAL);
        layout.setRowFill(0, TableLayout.Fill.NONE);
        layout.setRowAnchor(0, TableLayout.Anchor.SOUTHEAST);
        layout.setRowWeightY(1, 100);
        layout.setColumnWeightX(0, 100);
        layout.setCellFill(0, 0, TableLayout.Fill.NONE);
        layout.setCellFill(0, 1, TableLayout.Fill.NONE);
        layout.setCellFill(0, 2, TableLayout.Fill.NONE);
        layout.setCellFill(0, 3, TableLayout.Fill.NONE);
        return layout;
    }

    private JComponent createAliasList() {
        aliasNames = new JTable();
        final AbstractTableModel tableModel = new AbstractTableModel() {

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }

            @Override
            public int getRowCount() {
                return axisMappingModel.getAliasNames().size();
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                final String beforeName = getAliasNameAt(rowIndex);
                final String changedName = aValue.toString();
                axisMappingModel.replaceAlias(beforeName, changedName);
                aliasNameScrollPane.repaint();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return getAliasNameAt(rowIndex);
            }
        };
        aliasNames.setModel(tableModel);
        aliasNames.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        aliasNames.setColumnSelectionAllowed(true);
        aliasNames.setRowSelectionAllowed(true);
        aliasNames.setTableHeader(null);
        aliasNames.getSelectionModel().addListSelectionListener(new AliasNamesSelectionListener());
        aliasNameScrollPane = new JScrollPane(aliasNames);
        aliasNameScrollPane.setPreferredSize(new Dimension(160, 200));
        aliasNameScrollPane.setMinimumSize(new Dimension(160, 200));
        return aliasNameScrollPane;
    }

    private String getAliasNameAt(int rowIndex) {
        final Set<String> names = axisMappingModel.getAliasNames();
        return names.toArray(new String[names.size()])[rowIndex];
    }

    private JComponent createRasterNames() {
        rasterNames = new JList(new AbstractListModel() {
            @Override
            public int getSize() {
                return nameProvider.getRasterNames().length;
            }

            @Override
            public Object getElementAt(int index) {
                return nameProvider.getRasterNames()[index];
            }
        });
        rasterNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rasterNames.addListSelectionListener(new RasterNamesSelectionListener(rasterNames));
        rasterNames.setEnabled(false);
        final JScrollPane scrollPane = new JScrollPane(rasterNames);
        scrollPane.setPreferredSize(new Dimension(160, 200));
        return scrollPane;
    }

    private JComponent createInsituNames() {
        insituNames = new JList(new AbstractListModel() {

            @Override
            public int getSize() {
                return nameProvider.getInsituNames().length;
            }

            @Override
            public Object getElementAt(int index) {
                return nameProvider.getInsituNames()[index];
            }
        });
        insituNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        insituNames.addListSelectionListener(new InsituNamesSelectionListener(insituNames));
        insituNames.setEnabled(false);
        final JScrollPane scrollPane = new JScrollPane(insituNames);
        scrollPane.setPreferredSize(new Dimension(160, 200));
        return scrollPane;
    }

    static interface NameProvider {

        String[] getRasterNames();

        String[] getInsituNames();
    }

    private class AddAliasAction extends AbstractAction {

        private AddAliasAction() {
            super("Add alias", UIUtils.loadImageIcon("icons/Plus16.gif"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            axisMappingModel.addAlias("...");
            removeButton.setEnabled(true);
            aliasNameScrollPane.repaint();
            int rowIndex = 0;
            for (String aliasName : axisMappingModel.getAliasNames()) {
                if (aliasName.equals("...")) {
                    break;
                }
                rowIndex++;
            }
            DefaultCellEditor editor = (DefaultCellEditor) aliasNames.getCellEditor(rowIndex, 0);
            aliasNames.editCellAt(rowIndex, 0);
            final JTextField textField = (JTextField) editor.getComponent();
            textField.requestFocus();
            textField.selectAll();
        }

    }

    private class RemoveAliasAction extends AbstractAction {

        private RemoveAliasAction() {
            super("Remove alias", UIUtils.loadImageIcon("icons/Minus16.gif"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final ListSelectionModel selectionModel = aliasNames.getSelectionModel();
            final int minSelectionIndex = selectionModel.getMinSelectionIndex();
            final int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
            selectionModel.clearSelection();
            if (minSelectionIndex != -1) {
                for (int i = maxSelectionIndex; i >= minSelectionIndex; i--) {
                    axisMappingModel.removeAlias(getAliasNameAt(i));
                }
            }
            removeButton.setEnabled(axisMappingModel.getAliasNames().size() > 0);
            aliasNameScrollPane.repaint();
        }

    }

    private abstract class VariableNamesSelectionListener implements ListSelectionListener {

        private final JList variableNames;

        private VariableNamesSelectionListener(JList variableNames) {
            this.variableNames = variableNames;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            final ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
            for (int index : variableNames.getSelectedIndices()) {
                selectedIndices.add(index);
            }
            final int selectedAliasRow = aliasNames.getSelectedRow();
            if (selectedAliasRow != -1) {
                final String currentAlias = aliasNames.getModel().getValueAt(selectedAliasRow, 0).toString();
                for (int i = 0; i < variableNames.getModel().getSize(); i++) {
                    final String variableName = variableNames.getModel().getElementAt(i).toString();
                    if (selectedIndices.contains(i)) {
                        addVariableName(currentAlias, variableName);
                    } else {
                        removeVariableName(currentAlias, variableName);
                    }
                }
            }
        }

        abstract void addVariableName(String currentAlias, String name);
        abstract void removeVariableName(String currentAlias, String name);
    }

    private class RasterNamesSelectionListener extends VariableNamesSelectionListener {

        private RasterNamesSelectionListener(JList variableNames) {
            super(variableNames);
        }

        @Override
        void addVariableName(String currentAlias, String variableName) {
            axisMappingModel.addRasterName(currentAlias, variableName);
        }

        @Override
        void removeVariableName(String currentAlias, String variableName) {
            axisMappingModel.removeRasterName(currentAlias, variableName);
        }
    }

    private class InsituNamesSelectionListener extends VariableNamesSelectionListener {

        private InsituNamesSelectionListener(JList variableNames) {
            super(variableNames);
        }

        @Override
        void addVariableName(String currentAlias, String variableName) {
            axisMappingModel.addInsituName(currentAlias, variableName);
        }

        @Override
        void removeVariableName(String currentAlias, String variableName) {
            axisMappingModel.removeInsituName(currentAlias, variableName);
        }
    }

    private class AliasNamesSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            final boolean isSomeAliasSelected = aliasNames.getSelectionModel().getMinSelectionIndex() != -1;
            rasterNames.setEnabled(isSomeAliasSelected);
            insituNames.setEnabled(isSomeAliasSelected);
            removeButton.setEnabled(isSomeAliasSelected);

            if(isSomeAliasSelected) {
                final int[] selectedRasterIndices = getSelectedRasterIndices();
                final int[] selectedInsituIndices = getSelectedInsituIndices();
                rasterNames.setSelectedIndices(selectedRasterIndices);
                insituNames.setSelectedIndices(selectedInsituIndices);
            } else {
                rasterNames.clearSelection();
                insituNames.clearSelection();
            }
        }

        private int[] getSelectedRasterIndices() {
            final String currentAlias = getCurrentAlias();
            final Set<String> selectedRasterNames = axisMappingModel.getRasterNames(currentAlias);
            return getSelectedIndices(selectedRasterNames, rasterNames);
        }

        private int[] getSelectedInsituIndices() {
            final String currentAlias = getCurrentAlias();
            final Set<String> selectedInsituNames = axisMappingModel.getInsituNames(currentAlias);
            return getSelectedIndices(selectedInsituNames, insituNames);
        }

        private String getCurrentAlias() {
            final int minSelectionIndex = aliasNames.getSelectionModel().getMinSelectionIndex();
            return aliasNames.getModel().getValueAt(minSelectionIndex, 0).toString();
        }

        private int[] getSelectedIndices(Set<String> selectedVariableNames, JList variableNames) {
            final List<Integer> selectedIndices = new ArrayList<Integer>(selectedVariableNames.size());
            final ListModel variableNamesModel = variableNames.getModel();
            for(int i = 0; i < variableNamesModel.getSize(); i++) {
                final String rasterName = variableNamesModel.getElementAt(i).toString();
                if (selectedVariableNames.contains(rasterName)) {
                    selectedIndices.add(i);
                }
            }
            final int[] selectedIndicesArray = new int[selectedIndices.size()];
            int i = 0;
            for (Integer index : selectedIndices) {
                selectedIndicesArray[i] = index;
                i++;
            }
            return selectedIndicesArray;
        }
    }
}
