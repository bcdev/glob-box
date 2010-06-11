package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.glob.core.timeseries.TimeSeriesHandler;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 16:17:58
 */
public class AddToTimeSeriesAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        if (node instanceof RasterDataNode) {
            final TimeSeriesHandler timeSeriesHandler = TimeSeriesHandler.getInstance();
            timeSeriesHandler.addRasterToTimeSeries((RasterDataNode) node);
        }
    }
}
