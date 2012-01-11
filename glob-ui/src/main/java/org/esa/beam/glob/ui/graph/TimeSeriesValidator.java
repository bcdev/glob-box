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
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

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

    private final Map<Object, Map<String, String>> timeSeriesExpressionsMap = new HashMap<Object, Map<String, String>>();
    private final Set<TimeSeriesGraphModel.ValidationListener> validationListeners = new HashSet<TimeSeriesGraphModel.ValidationListener>();
    private final Parser parser = new ParserImpl();

    private Map<String, String> currentExpressionMap;
    private List<String> qualifiedSourceNames;
    private DefaultNamespace namespace;
    private JComboBox sourceNamesDropDown;
    private JTextField expressionTextField;

    private boolean hasUI = false;

    @Override
    public JComponent makeUI() {
        JPanel uiPanel = new JPanel(new BorderLayout(5, 0));

        final JLabel introductionLabel = new JLabel("Valid expression:");

        expressionTextField = new JTextField("true");

        final JLabel expressionErrorLabel = new JLabel();
        expressionErrorLabel.setPreferredSize(new Dimension(95, 20));
        expressionErrorLabel.setForeground(Color.red.darker());

        sourceNamesDropDown = new JComboBox();
        sourceNamesDropDown.setPreferredSize(new Dimension(120, 20));
        sourceNamesDropDown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (ItemEvent.SELECTED == e.getStateChange()) {
                    final String selectedSourceName = e.getItem().toString();
                    expressionTextField.setText(getExpressionFor(selectedSourceName));
                }
            }
        });

        expressionTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String expression = expressionTextField.getText();
                final String selectedSourceName = getSelectedSourceName();
                final boolean hasSet = setExpression(selectedSourceName, expression);
                if (hasSet) {
                    expressionErrorLabel.setText("");
                } else {
                    expressionErrorLabel.setText("Invalid expression.");
                }
            }
        });

        sourceNamesDropDown.setEnabled(false);
        expressionTextField.setEnabled(false);

        uiPanel.add(introductionLabel, BorderLayout.WEST);
        final JPanel innerPanel = new JPanel(new BorderLayout(5, 0));
        innerPanel.add(sourceNamesDropDown, BorderLayout.WEST);
        innerPanel.add(expressionTextField);
        uiPanel.add(innerPanel);
        uiPanel.add(expressionErrorLabel, BorderLayout.EAST);
        hasUI = true;
        return uiPanel;
    }

    @Override
    public void adaptTo(Object timeSeriesKey, AxisMappingModel axisMappingModel) {
        if (timeSeriesExpressionsMap.containsKey(timeSeriesKey)) {
            currentExpressionMap = timeSeriesExpressionsMap.get(timeSeriesKey);
        } else {
            currentExpressionMap = new HashMap<String, String>();
            timeSeriesExpressionsMap.put(timeSeriesKey, currentExpressionMap);
        }
        qualifiedSourceNames = extractQualifiedSourceNames(axisMappingModel);

        namespace = new DefaultNamespace();
        for (String qualifiedSourceName : qualifiedSourceNames) {
            namespace.registerSymbol(SymbolFactory.createVariable(qualifiedSourceName, 0.0));
        }

        if (qualifiedSourceNames.size() > 0 && hasUI) {
            expressionTextField.setEnabled(true);
            sourceNamesDropDown.setEnabled(true);
            sourceNamesDropDown.setModel(new DefaultComboBoxModel(getSourceNames()));
            expressionTextField.setText(getExpressionFor(getSelectedSourceName()));
        }
    }

    @Override
    public TimeSeries validate(TimeSeries timeSeries, String sourceName, TimeSeriesType type) throws ParseException {
        String qualifiedSourceName = createQualifiedSourcename(sourceName, type);
        final Symbol symbol = namespace.resolveSymbol(qualifiedSourceName);
        if (symbol == null) {
            throw new ParseException("No variable for identifier '" + qualifiedSourceName + "' registered.");
        }
        final Variable variable = (Variable) symbol;
        final String expression = getExpressionFor(qualifiedSourceName);
        final Term term = parser.parse(expression, namespace);

        final int seriesCount = timeSeries.getItemCount();
        final TimeSeries validatedSeries = new TimeSeries(sourceName);
        for (int i = 0; i < seriesCount; i++) {
            final TimeSeriesDataItem dataItem = timeSeries.getDataItem(i);
            final Number value = dataItem.getValue();
            variable.assignD(null, value.doubleValue());
            if (term.evalB(null)) {
                validatedSeries.add(dataItem);
            }
        }

        return validatedSeries;
    }

    @Override
    public void addValidationListener(TimeSeriesGraphModel.ValidationListener listener) {
        validationListeners.add(listener);
    }

    boolean setExpression(String qualifiedSourceName, String expression) {
        final Symbol symbol = namespace.resolveSymbol(qualifiedSourceName);
        if (symbol == null) {
            return false;
        }
        if (isExpressionValid(expression, qualifiedSourceName)) {
            if (expression.isEmpty()) {
                expression = "true";
            }
            currentExpressionMap.put(qualifiedSourceName, expression);
            fireExpressionChanged();
            return true;
        }
//        currentExpressionMap.put(qualifiedSourceName, "true");
//        fireExpressionChanged();
        return false;
    }

    private boolean isExpressionValid(String expression, String qualifiedSorceName) {
        if (expression.trim().equals(qualifiedSorceName.trim())) {
            return false;
        }
        try {
            final DefaultNamespace expressionValidationNamespace = new DefaultNamespace();
            expressionValidationNamespace.registerSymbol(SymbolFactory.createVariable(qualifiedSorceName, 0.0));
            final Term term = parser.parse(expression, expressionValidationNamespace);
            return term != null && term.isB();
        } catch (ParseException ignored) {
            return false;
        }
    }

    private void fireExpressionChanged() {
        for (TimeSeriesGraphModel.ValidationListener validationListener : validationListeners) {
            validationListener.expressionChanged();
        }
    }

    private String getSelectedSourceName() {
        return sourceNamesDropDown.getSelectedItem().toString();
    }

    private String[] getSourceNames() {
        return qualifiedSourceNames.toArray(new String[qualifiedSourceNames.size()]);
    }

    private void collectSourceNames(ArrayList<String> names, List<String> sourceNames, String qualifier) {
        for (String sourceName : sourceNames) {
            final String qualifiedSourceName = qualifier + sourceName;
            names.add(qualifiedSourceName);
        }
    }

    private List<String> extractQualifiedSourceNames(AxisMappingModel axisMappingModel) {
        final ArrayList<String> names = new ArrayList<String>();
        for (String alias : axisMappingModel.getAliasNames()) {
            collectSourceNames(names, axisMappingModel.getInsituNames(alias), QUALIFIER_INSITU);
            collectSourceNames(names, axisMappingModel.getRasterNames(alias), QUALIFIER_RASTER);
        }
        return names;
    }

    private String createQualifiedSourcename(String sourceName, TimeSeriesType type) {
        String qualifiedSourceName;
        if (TimeSeriesType.INSITU.equals(type)) {
            qualifiedSourceName = QUALIFIER_INSITU + sourceName;
        } else {
            qualifiedSourceName = QUALIFIER_RASTER + sourceName;
        }
        return qualifiedSourceName;
    }

    private String getExpressionFor(String qualifiedSourceName) {
        if (!currentExpressionMap.containsKey(qualifiedSourceName)) {
            setExpression(qualifiedSourceName, "true");
        }
        return currentExpressionMap.get(qualifiedSourceName);
    }
}
