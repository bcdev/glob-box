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

import com.bc.jexp.EvalEnv;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.Variable;
import com.bc.jexp.impl.DefaultNamespace;
import com.bc.jexp.impl.NamespaceImpl;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.beam.framework.ui.ExpressionPane;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
class TimeSeriesValidator implements TimeSeriesGraphForm.ValidatorUI, TimeSeriesGraphModel.Validation {

    private String expression;
    private String[] rasterNames;
    private String[] insituNames;
    private MyExpressionPane expressionPane;
    private Parser parser;
    private Map<String, Variable> variableMap = new HashMap<String, Variable>();
    private final EvalEnv env;

    TimeSeriesValidator() {
        final NamespaceImpl namespace = new DefaultNamespace();
        final Variable tsValue = SymbolFactory.createVariable("tsValue", 13.3);
        final Variable insitu1 = SymbolFactory.createVariable("insitu1", 13.3);
        final Variable insitu2 = SymbolFactory.createVariable("insitu2", 5.3);
        namespace.registerSymbol(tsValue);
        namespace.registerSymbol(insitu1);
        namespace.registerSymbol(insitu2);
        parser = new ParserImpl(namespace);
        env = new EvalEnv() {};
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
                setExpression(expressionPane.getCode(), "");
            }
        };
        modalDialog.setContent(expressionPane);
        modalDialog.show();
    }

    @Override
    public boolean validate(double value, String sourceName, TimeSeriesType type) {
        variableMap.get(sourceName + type).assignD(env, value);
        final Term term;
        try {
            term = parser.parse(expression);
        } catch (ParseException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return term.evalB(env);
    }

    @Override
    public void adaptTo(String[] rasterNames, String[] insituNames) {
        this.rasterNames = rasterNames;
        this.insituNames = insituNames;
//        todo update variableMap
    }

    private void setExpression(String expression, String sourceName) {
        this.expression = expression;
    }

    final static class MyExpressionPane extends ExpressionPane {

        private final String[] rasterNames;
        private final String[] insituNames;

        MyExpressionPane(boolean requiresBoolExpr, Parser parser, PropertyMap preferences, String[] rasterNames, String[] insituNames) {
            super(requiresBoolExpr, parser, preferences);
            this.rasterNames = rasterNames;
            this.insituNames = insituNames;
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
