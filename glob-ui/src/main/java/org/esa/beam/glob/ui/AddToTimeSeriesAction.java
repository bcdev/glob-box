package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.TimeSeriesHandler;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

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
            ModalDialog modalDialog = new ModalDialog(app.getMainFrame(), "Add band to time series",
                                                      ModalDialog.ID_OK_CANCEL_HELP, null);
            final List<RasterDataNode> rasters = new ArrayList<RasterDataNode>();
            final String rasterName = node.getName();
            final Product[] products = app.getProductManager().getProducts();
            for (Product product : products) {
                final RasterDataNode dataNode = product.getRasterDataNode(rasterName);
                if (dataNode != null) {
                    rasters.add(dataNode);
                }
            }
            final AddBandToTimeSeriesDialog timeSeriesDialog = new AddBandToTimeSeriesDialog(node, rasters);
            modalDialog.setContent(timeSeriesDialog);
            final int status = modalDialog.show();
            modalDialog.getJDialog().dispose();
            final boolean ok = status == ModalDialog.ID_OK;
            if (ok) {
                timeSeriesDialog.getAddedRasterList();
                timeSeriesHandler.addRasterToTimeSeries((RasterDataNode) node);
            }
        }
    }
}
