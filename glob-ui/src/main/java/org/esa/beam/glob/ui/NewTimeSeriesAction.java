package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.framework.ui.assistant.AssistantPane;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Component;

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
            sourcePaneModel.addListDataListener(new ListDataListener() {
                @Override
                public void intervalAdded(ListDataEvent e) {
                    getContext().updateState();
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    getContext().updateState();
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                    getContext().updateState();
                }
            });
            return new ProductSourcePane(sourcePaneModel);
        }

        @Override
        public boolean hasNextPage() {
            return true;
        }

        @Override
        public boolean validatePage() {
            if(super.validatePage()) {
                final ProductSourcePaneModel sourcePaneModel = getAssistantModel().getProductSourcePaneModel();
                return sourcePaneModel.getSize() > 0;
            }
            return false;
        }

        @Override
        public AssistantPage getNextPage() {
            return new NewTimeSeriesAssistantPage2(getAssistantModel());
        }
    }

    private static class NewTimeSeriesAssistantPage2 extends AbstractTimeSeriesAssistantPage {

        protected NewTimeSeriesAssistantPage2(TimeSeriesAssistantModel assistantModel) {
            super("Select Variables", assistantModel);
        }

        @Override
        protected Component createPageComponent() {
            final VariableSelectionPaneModel variableModel = getAssistantModel().getVariableSelectionPaneModel();
            variableModel.add(new TimeVariable("band_1"), new TimeVariable("band_2", true), new TimeVariable("band_3"));
            return new VariableSelectionPane(variableModel);
        }

        @Override
        public boolean hasNextPage() {
            return true;
        }

        @Override
        public AssistantPage getNextPage() {
            return new NewTimeSeriesAssistantPage3(getAssistantModel());
        }

    }

    private static class NewTimeSeriesAssistantPage3  extends AbstractTimeSeriesAssistantPage {

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
            final JTextField field = new JTextField();
            panel.add(label);
            panel.add(field);
            return panel;
        }

        @Override
        public boolean canFinish() {
            return true;
        }

        @Override
        public boolean performFinish() {
            return true; 

        }
    }

}
