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

package time.series.chart;

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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExpressionDialog {

    public static void main(String[] args) throws ParseException {

        final NamespaceImpl namespace = new DefaultNamespace();
        final Variable tsValue = SymbolFactory.createVariable("tsValue", 13.3);
        final Variable insitu1 = SymbolFactory.createVariable("insitu1", 13.3);
        final Variable insitu2 = SymbolFactory.createVariable("insitu2", 5.3);
        namespace.registerSymbol(tsValue);
        namespace.registerSymbol(insitu1);
        namespace.registerSymbol(insitu2);
        final Parser parser = new ParserImpl(namespace);
        final Term term = parser.parse("tsValue > 0 or insitu1 > 0 or insitu2 > 0");
        System.out.println("b = " + term.evalB(new EvalEnv() {}));

        final Button button = new Button("view expression dialog");
        final JPanel panel = new JPanel(new FlowLayout());
        panel.add(button);
        final JFrame mainFrame = new JFrame();
        mainFrame.setContentPane(panel);
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final PropertyMap preferences = new PropertyMap();
        final MyExpressionPane expressionPane = new MyExpressionPane(true, parser, preferences);
        expressionPane.displayPatternInsertionPane();
        final ModalDialog modalDialog = new ModalDialog(mainFrame, "", ModalDialog.ID_OK_CANCEL, "");
        modalDialog.setContent(expressionPane);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modalDialog.show();
                System.out.println("expressionPane.getCode() = " + expressionPane.getCode());
            }
        });

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    final static class MyExpressionPane extends ExpressionPane {

        MyExpressionPane(boolean requiresBoolExpr, Parser parser, PropertyMap preferences) {
            super(requiresBoolExpr, parser, preferences);
        }

        void displayPatternInsertionPane() {
            final JList bandList = createPatternList(new String[]{"tsValue", "band_2"});
            final JList insituList = createPatternList(new String[]{"insitu1", "insitu2"});

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
