package org.esa.beam.glob.ui;

class TimeSeriesAssistantModel {

    private ProductSourcePaneModel productSourcePaneModel;
    private VariableSelectionPaneModel variableSelectionPaneModel;

    TimeSeriesAssistantModel() {
        this(new ProductSourcePaneModel(), new VariableSelectionPaneModel());
    }

    private TimeSeriesAssistantModel(ProductSourcePaneModel productSourcePaneModel,
                                     VariableSelectionPaneModel variableSelectionPaneModel) {
        this.productSourcePaneModel = productSourcePaneModel;
        this.variableSelectionPaneModel = variableSelectionPaneModel;
    }

    public ProductSourcePaneModel getProductSourcePaneModel() {
        return productSourcePaneModel;
    }

    public VariableSelectionPaneModel getVariableSelectionPaneModel() {
        return variableSelectionPaneModel;
    }
}
