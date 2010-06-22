package org.esa.beam.glob.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.TimeSeriesProductBuilder;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.awt.Dimension;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 16:17:58
 */
public class CreateTimeSeriesAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final VisatApp app = VisatApp.getApp();
        final ProductNode node = app.getSelectedProductNode();
        if (node instanceof RasterDataNode) {
            final Model model = new Model("TimeSeries_" + node.getName());
            PropertyContainer propertyContainer = model.createPropertyContainer();
            PropertyPane timeSeriesPane = new PropertyPane(propertyContainer);
            if (showDialog(app, timeSeriesPane)) {
                app.getApplicationPage().showToolView("org.esa.beam.glob.ui.SliderToolView");
            }
            final String timeSeriesName = model.getName();
            final ProductManager productManager = app.getProductManager();
            final TimeSeries tsProduct = TimeSeriesProductBuilder.createTimeSeriesProductFromProductsView(
                    timeSeriesName,
                    (RasterDataNode) node,
                    productManager);
            productManager.addProduct(tsProduct.getProduct());
        }
    }

    private boolean showDialog(VisatApp app, final PropertyPane timeSeriesPane) {
        ModalDialog modalDialog = new ModalDialog(app.getMainFrame(), "Create time series",
                                                  ModalDialog.ID_OK_CANCEL_HELP, null) {
            @Override
            protected boolean verifyUserInput() {
                return !timeSeriesPane.getBindingContext().hasProblems();
            }
        };
        modalDialog.setContent(timeSeriesPane.createPanel());
        final int status = modalDialog.show();
        modalDialog.getJDialog().setMinimumSize(new Dimension(300, 80));
        modalDialog.getJDialog().dispose();
        return status == ModalDialog.ID_OK;
    }

    public static class Model {

        private static final String PROPERTY_NAME_NAME = "name";

        private String name;

        public Model(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public PropertyContainer createPropertyContainer() {
            final PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this);
            final PropertyDescriptor nameDescriptor = propertyContainer.getDescriptor(Model.PROPERTY_NAME_NAME);
            nameDescriptor.setValidator(new Validator() {
                @Override
                public void validateValue(Property property, Object value) throws ValidationException {
                    if (value instanceof String) {
                        String name = (String) value;
                        if (!Product.isValidNodeName(name)) {
                            throw new ValidationException("Name of time series is not valid");
                        }
                    }
                }
            });
            return propertyContainer;
        }
    }

}
