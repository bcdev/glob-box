package org.esa.beam.glob.ui;

public class NewTimeSeriesAssistantAction extends AbstractTimeSeriesAssistantAction {

    public static final String ID = "newTimeSeriesAssistantAction";

    @Override
    protected TimeSeriesAssistantModel createModel() {
        return new TimeSeriesAssistantModel();
    }
}
