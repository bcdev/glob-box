package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.TimeSeriesHandler;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 16:17:58
 */
public class AddToTimeSeriesAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final VisatApp app = VisatApp.getApp();
        final ProductNode node = app.getSelectedProductNode();
        if (node instanceof RasterDataNode) {
            final TimeSeriesHandler timeSeriesHandler = TimeSeriesHandler.getInstance();
            final AddBandToTimeSeriesPane timeSeriesPane = createPane(app, node);

            if (showDialog(app, timeSeriesPane)) {
                timeSeriesPane.getAddedRasterList();
                timeSeriesHandler.addRasterToTimeSeries((RasterDataNode) node);
                app.getApplicationPage().showToolView("org.esa.beam.glob.ui.TimeSeriesManagerToolView");
            }
        }
    }

    private boolean showDialog(VisatApp app, AddBandToTimeSeriesPane timeSeriesPane) {
        ModalDialog modalDialog = new ModalDialog(app.getMainFrame(), "Add band to time series",
                                                  ModalDialog.ID_OK_CANCEL_HELP, null);
        modalDialog.setContent(timeSeriesPane);
        final int status = modalDialog.show();
        modalDialog.getJDialog().setMinimumSize(new Dimension(300, 80));
        modalDialog.getJDialog().dispose();
        return status == ModalDialog.ID_OK;
    }

    private AddBandToTimeSeriesPane createPane(VisatApp app, ProductNode node) {
        final List<RasterDataNode> rasters = new ArrayList<RasterDataNode>();
        final String rasterName = node.getName();
        final Product[] products = app.getProductManager().getProducts();
        for (Product product : products) {
            final RasterDataNode dataNode = product.getRasterDataNode(rasterName);
            if (dataNode != null) {
                rasters.add(dataNode);
            }
        }
        final AddBandToTimeSeriesPane timeSeriesPane = new AddBandToTimeSeriesPane(node, rasters);
        return timeSeriesPane;
    }
}
