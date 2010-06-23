package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPane;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.util.List;

public class NewTimeSeriesAction extends AbstractVisatAction {

    public static final String ID = "newTimeSeriesAction";

    @Override
    public void actionPerformed(CommandEvent event) {
        super.actionPerformed(event);
        final AssistantPane assistant = new AssistantPane(VisatApp.getApp().getApplicationWindow(), "New Time Series");
        assistant.show(new NewTimeSeriesAssistantPage1(new TimeSeriesAssistantModel()));
    }

    private abstract static class AbstractTimeSeriesAssistantPage extends AbstractAssistantPage {

        private TimeSeriesAssistantModel assistantModel;

        protected AbstractTimeSeriesAssistantPage(String pageTitle, TimeSeriesAssistantModel model) {
            super(pageTitle);
            assistantModel = model;
            assistantModel.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    getContext().updateState();
                }
            });
        }

        protected TimeSeriesAssistantModel getAssistantModel() {
            return assistantModel;
        }
    }

    private static class NewTimeSeriesAssistantPage1 extends AbstractTimeSeriesAssistantPage {

        protected NewTimeSeriesAssistantPage1(TimeSeriesAssistantModel model) {
            super("Define Time Series Sources", model);
        }

        @Override
        protected Component createPageComponent() {
            final ProductSourcePaneModel sourcePaneModel = getAssistantModel().getProductSourcePaneModel();
            return new ProductSourcePane(sourcePaneModel);
        }

        @Override
        public boolean validatePage() {
            if (super.validatePage()) {
                final ProductSourcePaneModel sourcePaneModel = getAssistantModel().getProductSourcePaneModel();
                if(sourcePaneModel.getSize() > 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasNextPage() {
            return true;
        }

        @Override
        public AssistantPage getNextPage() {
            final TimeSeriesAssistantModel model = getAssistantModel();
            return new NewTimeSeriesAssistantPage2(model);
        }

        @Override
        public boolean canFinish() {
            return false;

        }

    }

    private static class NewTimeSeriesAssistantPage2 extends AbstractTimeSeriesAssistantPage {

        protected NewTimeSeriesAssistantPage2(TimeSeriesAssistantModel assistantModel) {
            super("Select Variables", assistantModel);
        }

        @Override
        protected Component createPageComponent() {
            final VariableSelectionPaneModel variableModel = getAssistantModel().getVariableSelectionPaneModel();
            variableModel.set(getVariables(getAssistantModel().getProductSourcePaneModel()));
            return new VariableSelectionPane(variableModel);
        }

        @Override
        public boolean validatePage() {
            if (super.validatePage()) {
                VariableSelectionPaneModel variableModel = getAssistantModel().getVariableSelectionPaneModel();
                for (int i = 0; i < variableModel.getSize(); i++) {
                    final TimeVariable variable = variableModel.getElementAt(i);
                    if (variable.isSelected()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canFinish() {
            return false;

        }

        @Override
        public boolean hasNextPage() {
            return true;
        }

        @Override
        public AssistantPage getNextPage() {
            final TimeSeriesAssistantModel model = getAssistantModel();
            final VariableSelectionPaneModel variableModel = model.getVariableSelectionPaneModel();
            for (int i = 0; i < variableModel.getSize(); i++) {
                final TimeVariable variable = variableModel.getElementAt(i);
                if (variable.isSelected()) {
                    model.setTimeSeriesName("TimeSeries_" + variable.getName());
                    break;
                }
            }
            return new NewTimeSeriesAssistantPage3(model);
        }

        private TimeVariable[] getVariables(ProductSourcePaneModel sourceModel) {
            for (int i = 0; i < sourceModel.getSize(); i++) {
                final ProductLocation location = sourceModel.getElementAt(i);
                final List<Product> products = location.getProductLocationType().findProducts(location.getPath());
                if (!products.isEmpty()) {
                    final Product product = products.get(0);
                    final String[] bandNames = product.getBandNames();
                    final TimeVariable[] variables = new TimeVariable[bandNames.length];
                    for (int j = 0; j < bandNames.length; j++) {
                        variables[j] = new TimeVariable(bandNames[j]);
                    }
                    return variables;
                }
            }

            return new TimeVariable[0];
        }

    }

    private static class NewTimeSeriesAssistantPage3 extends AbstractTimeSeriesAssistantPage {

        private JTextField field;

        protected NewTimeSeriesAssistantPage3(TimeSeriesAssistantModel assistantModel) {
            super("Set Product Name", assistantModel);
        }

        @Override
        protected Component createPageComponent() {
            final TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setTableWeightY(1.0);
            tableLayout.setColumnWeightX(0, 0.0);
            tableLayout.setColumnWeightX(1, 1.0);

            final JPanel panel = new JPanel(tableLayout);
            final JLabel label = new JLabel("Time Series Name:");
            field = new JTextField(getAssistantModel().getTimeSeriesName());
            panel.add(label);
            panel.add(field);
            return panel;
        }

        @Override
        public boolean canFinish() {
            return !field.getText().isEmpty();
        }

        @Override
        public boolean performFinish() {
            getAssistantModel().setTimeSeriesName(field.getText());
            return true;

        }
    }

}
