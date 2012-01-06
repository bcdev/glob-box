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
import org.esa.beam.framework.ui.ExpressionPane;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
class TimeSeriesValidator implements TimeSeriesGraphForm.ValidatorUI, TimeSeriesGraphModel.Validation {

    private List<String> rasterNames;
    private List<String> insituNames;
    private MyExpressionPane expressionPane;
    private Parser parser;
    private Map<Object, Map<String, Term>> timeSeriesExpressionsMap = new HashMap<Object, Map<String, Term>>();
    private Map<String, Term> currentTermMap;
    private DefaultNamespace namespace;
    private final Term trueTerm;

    TimeSeriesValidator() {
        parser = new ParserImpl();
        trueTerm = createTrueTerm();
    }

    @Override
    public void show() {
        final PropertyMap preferences = new PropertyMap();
        expressionPane = new MyExpressionPane(true, parser, preferences, rasterNames, insituNames);
        expressionPane.displayPatternInsertionPane();
        final ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getMainFrame(),
                                                        "Edit valid expression", ModalDialog.ID_OK_CANCEL, "") {
            @Override
            protected void onOK() {
                super.onOK();
//                setExpression("", expressionPane.getCode(), null);
            }
        };
        modalDialog.setContent(expressionPane);
        modalDialog.show();
    }

    @Override
    public boolean validate(double value, String sourceName, TimeSeriesType type) {
        String sourceIdentifier;
        if(TimeSeriesType.INSITU.equals(type)) {
            sourceIdentifier = TimeSeriesGraphModel.QUALIFIER_INSITU + sourceName;
        } else {
            sourceIdentifier = TimeSeriesGraphModel.QUALIFIER_RASTER + sourceName;
        }
        final Symbol symbol = namespace.resolveSymbol(sourceIdentifier);
        ((Variable) symbol).assignD(null, value);
        return currentTermMap.get(sourceIdentifier).evalB(null);
    }

    @Override
    public void adaptTo(Object timeSeriesKey, AxisMappingModel axisMappingModel) {
        if (timeSeriesExpressionsMap.containsKey(timeSeriesKey)) {
            currentTermMap = timeSeriesExpressionsMap.get(timeSeriesKey);
        } else {
            currentTermMap = new HashMap<String, Term>();
            timeSeriesExpressionsMap.put(timeSeriesKey, currentTermMap);
        }
        namespace = new DefaultNamespace();
        rasterNames = new ArrayList<String>();
        insituNames = new ArrayList<String>();
        for (String alias : axisMappingModel.getAliasNames()) {
            adaptToSourceNames(axisMappingModel.getInsituNames(alias), TimeSeriesGraphModel.QUALIFIER_INSITU, insituNames);
            adaptToSourceNames(axisMappingModel.getRasterNames(alias), TimeSeriesGraphModel.QUALIFIER_RASTER, rasterNames);
        }
    }

    void setExpression(String sourceName, String expression, TimeSeriesType type) throws ParseException {
        final String qualifiedSourceName;
        if (TimeSeriesType.INSITU.equals(type)) {
            qualifiedSourceName = TimeSeriesGraphModel.QUALIFIER_INSITU + sourceName;
        } else {
            qualifiedSourceName = TimeSeriesGraphModel.QUALIFIER_RASTER + sourceName;
        }
        final Term term = parser.parse(expression, namespace);
        currentTermMap.put(qualifiedSourceName, term);
    }

    private void adaptToSourceNames(final List<String> sourceNames, final String qualifier, List<String> qualifiedSourcNames) {
        for (String sourceName : sourceNames) {
            final String qualifiedSourceName = qualifier + sourceName;
            namespace.registerSymbol(SymbolFactory.createVariable(qualifiedSourceName, 0.0));
            qualifiedSourcNames.add(qualifiedSourceName);
            if (!currentTermMap.containsKey(qualifiedSourceName)) {
                currentTermMap.put(qualifiedSourceName, trueTerm);
            }
        }
    }

    private Term createTrueTerm() {
        try {
            return parser.parse("true");
        } catch (ParseException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    final static class MyExpressionPane extends ExpressionPane {

        private final String[] rasterNames;
        private final String[] insituNames;

        MyExpressionPane(boolean requiresBoolExpr, Parser parser, PropertyMap preferences, List<String> rasterNames, List<String> insituNames) {
            super(requiresBoolExpr, parser, preferences);
            this.rasterNames = rasterNames.toArray(new String[rasterNames.size()]);
            this.insituNames = insituNames.toArray(new String[insituNames.size()]);
        }

        void displayPatternInsertionPane() {
            final JList bandList = createPatternList(rasterNames);
            final JList insituList = createPatternList(insituNames);

            final JPanel panel = new JPanel(new GridLayout(4, 1));
            panel.add(new JLabel("Bands Names"));
            panel.add(new JScrollPane(bandList));
            panel.add(new JLabel("Insitu Names"));
            panel.add(new JScrollPane(insituList));
            final JPanel defaultAccessoryPane = createDefaultAccessoryPane(panel);
            setLeftAccessory(defaultAccessoryPane);
        }
    }
}
