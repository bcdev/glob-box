package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPane;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesFactory;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class AbstractTimeSeriesAssistantAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        super.actionPerformed(event);
        final TimeSeriesAssistantModel assistantModel = createModel();
        final AssistantPane assistant = new AssistantPane(getAppContext().getApplicationWindow(), "New Time Series");
        assistant.show(new NewTimeSeriesAssistantPage1(assistantModel));
    }

    protected abstract TimeSeriesAssistantModel createModel();

    private abstract class AbstractTimeSeriesAssistantPage extends AbstractAssistantPage {

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

        @Override
        public boolean performFinish() {
            addTimeSeriesProductToVisat(getAssistantModel());
            return true;

        }

        private void addTimeSeriesProductToVisat(TimeSeriesAssistantModel assistantModel) {
            final ProductLocationsPaneModel locationsModel = assistantModel.getProductLocationsModel();
            final VariableSelectionPaneModel variablesModel = assistantModel.getVariableSelectionModel();
            final AbstractTimeSeries timeSeries = TimeSeriesFactory.create(assistantModel.getTimeSeriesName(),
                                                                           locationsModel.getProductLocations(),
                                                                           variablesModel.getSelectedVariableNames());
            getAppContext().getProductManager().addProduct(timeSeries.getTsProduct());
        }
    }

    private class NewTimeSeriesAssistantPage1 extends AbstractTimeSeriesAssistantPage {

        protected NewTimeSeriesAssistantPage1(TimeSeriesAssistantModel model) {
            super("Define Time Series Sources", model);
        }

        @Override
        protected Component createPageComponent() {
            final ProductLocationsPaneModel locationsModel = getAssistantModel().getProductLocationsModel();
            return new ProductLocationsPane(locationsModel);
        }

        @Override
        public boolean validatePage() {
            if (super.validatePage()) {
                final ProductLocationsPaneModel locationsModel = getAssistantModel().getProductLocationsModel();
                if (locationsModel.getSize() > 0) {
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
            final ProgressMonitorSwingWorker worker = new MyProgressMonitorSwingWorker(model);
            worker.executeWithBlocking();

            return new NewTimeSeriesAssistantPage2(model);
        }

        @Override
        public boolean canFinish() {
            return false;

        }

        private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Variable[], Object> {

            private final TimeSeriesAssistantModel model;

            private MyProgressMonitorSwingWorker(TimeSeriesAssistantModel model) {
                super(NewTimeSeriesAssistantPage1.this.getContext().getCurrentPage().getPageComponent(),
                      "Scanning for products");
                this.model = model;
            }

            @Override
            protected Variable[] doInBackground(ProgressMonitor pm) throws Exception {
                return getVariables(getAssistantModel().getProductLocationsModel(), pm);
            }

            @Override
            protected void done() {
                try {
                    model.getVariableSelectionModel().set(get());
                } catch (InterruptedException ignored) {
                } catch (ExecutionException e) {
                    getContext().showErrorDialog("Failed to scan for products: \n" + e.getMessage());
                    e.printStackTrace();
                }
            }

            private Variable[] getVariables(ProductLocationsPaneModel locationsModel, ProgressMonitor pm) {
                try {
                    pm.beginTask("Scanning product locations...", locationsModel.getSize());
                    for (int i = 0; i < locationsModel.getSize(); i++) {
                        final ProductLocation location = locationsModel.getElementAt(i);
                        location.loadProducts(new SubProgressMonitor(pm, 1));
                        final List<Product> products = location.getProducts();
                        if (!products.isEmpty()) {
                            final Product product = products.get(0);
                            final String[] bandNames = product.getBandNames();
                            final Variable[] variables = new Variable[bandNames.length];
                            for (int j = 0; j < bandNames.length; j++) {
                                variables[j] = new Variable(bandNames[j]);
                            }
                            location.closeProducts();
                            return variables;
                        } else {
                            location.closeProducts();
                        }
                    }
                } finally {
                    pm.done();
                }

                return new Variable[0];
            }

        }
    }

    private class NewTimeSeriesAssistantPage2 extends AbstractTimeSeriesAssistantPage {

        protected NewTimeSeriesAssistantPage2(TimeSeriesAssistantModel assistantModel) {
            super("Select Variables", assistantModel);
        }

        @Override
        protected Component createPageComponent() {
            return new VariableSelectionPane(getAssistantModel().getVariableSelectionModel());
        }

        @Override
        public boolean validatePage() {
            if (super.validatePage()) {
                VariableSelectionPaneModel variableModel = getAssistantModel().getVariableSelectionModel();
                for (int i = 0; i < variableModel.getSize(); i++) {
                    final Variable variable = variableModel.getElementAt(i);
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
            final VariableSelectionPaneModel variableModel = model.getVariableSelectionModel();
            for (int i = 0; i < variableModel.getSize(); i++) {
                final Variable variable = variableModel.getElementAt(i);
                if (variable.isSelected()) {
                    model.setTimeSeriesName("TimeSeries_" + variable.getName());
                    break;
                }
            }
            return new NewTimeSeriesAssistantPage3(model);
        }

    }

    private class NewTimeSeriesAssistantPage3 extends AbstractTimeSeriesAssistantPage {

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
        public boolean validatePage() {
            if (super.validatePage()) {
                final String name = field.getText();
                if (!name.isEmpty() && ProductNode.isValidNodeName(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canFinish() {
            return true;
        }

        @Override
        public boolean performFinish() {
            getAssistantModel().setTimeSeriesName(field.getText());
            return super.performFinish();

        }
    }

}