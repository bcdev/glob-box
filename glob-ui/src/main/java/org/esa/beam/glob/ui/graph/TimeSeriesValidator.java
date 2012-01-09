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

package org.esa.beam.glob.ui.graph;

import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.Variable;
import com.bc.jexp.impl.DefaultNamespace;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sabine Embacher
 * @author Thomas Storm
 */
class TimeSeriesValidator implements TimeSeriesGraphForm.ValidatorUI, TimeSeriesGraphModel.Validation {

    private static final String QUALIFIER_RASTER = "r.";
    private static final String QUALIFIER_INSITU = "i.";

    private final DefaultNamespace namespace = new DefaultNamespace();
    private final Map<Object, Map<String, String>> timeSeriesExpressionsMap = new HashMap<Object, Map<String, String>>();
    private final Set<TimeSeriesGraphModel.ValidationListener> validationListeners = new HashSet<TimeSeriesGraphModel.ValidationListener>();
    private final Parser parser = new ParserImpl();

    private Map<String, String> currentExpressionMap;
    private List<String> rasterNames;
    private List<String> insituNames;
    private JComboBox sourceNamesDropDown;
    private JTextField expressionField;

    private boolean hasUI = false;

    @Override
    public JComponent makeUI() {
        JPanel ui = new JPanel(new BorderLayout(5, 0));

        final JLabel introductionLabel = new JLabel("Valid expression:");

        expressionField = new JTextField("true");

        final JLabel expressionErrorLabel = new JLabel();
        expressionErrorLabel.setPreferredSize(new Dimension(95, 20));
        expressionErrorLabel.setForeground(Color.red.darker());

        sourceNamesDropDown = new JComboBox();
        sourceNamesDropDown.setPreferredSize(new Dimension(120, 20));
        sourceNamesDropDown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final boolean isItemSelected = e.getStateChange() == ItemEvent.SELECTED;
                if (isItemSelected) {
                    final String selectedSourceName = e.getItem().toString();
                    if (!currentExpressionMap.containsKey(selectedSourceName)) {
                        setExpression(selectedSourceName, "true");
                    }
                    expressionField.setText(currentExpressionMap.get(selectedSourceName));
                }
            }
        });

        expressionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String expression = expressionField.getText();
                final String selectedSourceName = sourceNamesDropDown.getSelectedItem().toString();
                final boolean hasSet = setExpression(selectedSourceName, expression);
                if (hasSet) {
                    expressionErrorLabel.setText("");
                } else {
                    expressionErrorLabel.setText("Invalid expression.");
                }
            }
        });

        sourceNamesDropDown.setEnabled(false);
        expressionField.setEnabled(false);

        ui.add(introductionLabel, BorderLayout.WEST);
        final JPanel innerPanel = new JPanel(new BorderLayout(5, 0));
        innerPanel.add(sourceNamesDropDown, BorderLayout.WEST);
        innerPanel.add(expressionField);
        ui.add(innerPanel);
        ui.add(expressionErrorLabel, BorderLayout.EAST);
        hasUI = true;
        return ui;
    }

    @Override
    public boolean validate(double value, String sourceName, TimeSeriesType type) throws ParseException {
        String sourceIdentifier;
        if (TimeSeriesType.INSITU.equals(type)) {
            sourceIdentifier = QUALIFIER_INSITU + sourceName;
        } else {
            sourceIdentifier = QUALIFIER_RASTER + sourceName;
        }
        final Symbol symbol = namespace.resolveSymbol(sourceIdentifier);
        if (symbol == null) {
            throw new ParseException("No variable for identifier '" + sourceIdentifier + "' registered.");
        }
        ((Variable) symbol).assignD(null, value);
        final String expression = currentExpressionMap.get(sourceIdentifier);
        final Term term = parser.parse(expression, namespace);
        return term.evalB(null);
    }

    @Override
    public void adaptTo(Object timeSeriesKey, AxisMappingModel axisMappingModel) {
        if (timeSeriesExpressionsMap.containsKey(timeSeriesKey)) {
            currentExpressionMap = timeSeriesExpressionsMap.get(timeSeriesKey);
        } else {
            currentExpressionMap = new HashMap<String, String>();
            timeSeriesExpressionsMap.put(timeSeriesKey, currentExpressionMap);
        }
        rasterNames = new ArrayList<String>();
        insituNames = new ArrayList<String>();
        for (Symbol symbol : namespace.getAllSymbols()) {
            namespace.deregisterSymbol(symbol);
        }
        for (String alias : axisMappingModel.getAliasNames()) {
            adaptToSourceNames(axisMappingModel.getInsituNames(alias), QUALIFIER_INSITU, insituNames);
            adaptToSourceNames(axisMappingModel.getRasterNames(alias), QUALIFIER_RASTER, rasterNames);
        }

        final String[] sourceNames = getSourceNames();
        if(sourceNames.length > 0 && hasUI) {
            expressionField.setEnabled(true);
            sourceNamesDropDown.setEnabled(true);
            sourceNamesDropDown.setModel(new DefaultComboBoxModel(sourceNames));
        }
    }

    @Override
    public void addValidationListener(TimeSeriesGraphModel.ValidationListener listener) {
        validationListeners.add(listener);
    }
    
    @Override
    public void removeValidationListener(TimeSeriesGraphModel.ValidationListener listener) {
        validationListeners.remove(listener);
    }

    boolean setExpression(String qualifiedSourceName, String expression) {
        if(isExpressionValid(expression, qualifiedSourceName)) {
            if(expression.isEmpty()) {
                expression = "true";
            }
            currentExpressionMap.put(qualifiedSourceName, expression);
            fireExpressionChanged();
            return true;
        }
        currentExpressionMap.put(qualifiedSourceName, "true");
        fireExpressionChanged();
        return false;
    }

    private boolean isExpressionValid(String expression, String selectedSourceName){
        if(expression.trim().equals(selectedSourceName.trim())) {
            return false;
        }
        try {
            final DefaultNamespace expressionValidationNamespace = new DefaultNamespace();
            expressionValidationNamespace.registerSymbol(SymbolFactory.createVariable(selectedSourceName, 0.0));
            parser.parse(expression, expressionValidationNamespace);
            return true;
        } catch (ParseException ignored) {
            return false;
        }
    }

    private String[] getSourceNames() {
        final String[] sourceNames = new String[rasterNames.size() + insituNames.size()];
        for (int i = 0; i < rasterNames.size(); i++) {
            sourceNames[i] = rasterNames.get(i);
        }
        for (int i = 0; i < insituNames.size(); i++) {
            sourceNames[i + rasterNames.size()] = insituNames.get(i);
        }
        return sourceNames;
    }

    private void adaptToSourceNames(final List<String> sourceNames, final String qualifier, List<String> qualifiedSourceNames) {
        for (String sourceName : sourceNames) {
            final String qualifiedSourceName = qualifier + sourceName;
            qualifiedSourceNames.add(qualifiedSourceName);
            if (!currentExpressionMap.containsKey(qualifiedSourceName)) {
                setExpression(qualifiedSourceName, "true");
            }
            namespace.registerSymbol(SymbolFactory.createVariable(qualifiedSourceName, 0.0));
        }
    }

    private void fireExpressionChanged() {
        for (TimeSeriesGraphModel.ValidationListener validationListener : validationListeners) {
            validationListener.expressionChanged();
        }
    }
}
