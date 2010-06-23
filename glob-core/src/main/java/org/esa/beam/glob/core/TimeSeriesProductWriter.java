package org.esa.beam.glob.core;

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;

import java.io.IOException;
import java.util.List;

import static org.esa.beam.glob.core.TimeSeriesProductBuilder.*;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 15:47:52
 */
class TimeSeriesProductWriter extends DimapProductWriter {

    public TimeSeriesProductWriter(ProductWriterPlugIn productWriterPlugIn) {
        super(productWriterPlugIn);
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        boolean shouldWrite = super.shouldWrite(node);
        if (shouldWrite && node.getProduct().getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
            return !(node instanceof RasterDataNode);
        }
        return shouldWrite;
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        final Product tsProduct = getSourceProduct();
        final TimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(tsProduct);
        if (timeSeries != null) {

            final List<ProductLocation> productLocations = timeSeries.getProductLocations();
            final MetadataElement metadataRoot = timeSeries.getTsProduct().getMetadataRoot();
            MetadataElement timeSeriesElement = metadataRoot.getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME);
            if (timeSeriesElement != null) {
                metadataRoot.removeElement(timeSeriesElement);
            }
            TimeSeriesProductBuilder.addTimeSeriesMetadataStructure(tsProduct);

            //////////////////////////////////////////////
            MetadataElement productListElement = metadataRoot.
                    getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME).
                    getElement(TimeSeriesProductBuilder.PRODUCT_LIST_NAME);
            int index = 0;
            for (ProductLocation productLocation : productLocations) {
                final ProductData productPath = ProductData.createInstance(productLocation.getPath());
                final ProductData productType = ProductData.createInstance(
                        productLocation.getProductLocationType().toString());
                MetadataElement elem = new MetadataElement(Integer.toString(index++));
                elem.addAttribute(new MetadataAttribute("path", productPath, true));
                elem.addAttribute(new MetadataAttribute("type", productType, true));
                productListElement.addElement(elem);
            }

            //////////////////////////////////////////////
            timeSeries.getTimeVariables();
            MetadataElement variableListElement = metadataRoot.
                    getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME).
                    getElement(TimeSeriesProductBuilder.VARIABLES_LIST_NAME);
            index = 0;
            for (TimeVariable variable : timeSeries.getTimeVariables()) {
                final ProductData variableName = ProductData.createInstance(variable.getName());
                final ProductData isSelected = ProductData.createInstance(Boolean.toString(variable.isSelected()));
                MetadataElement elem = new MetadataElement(Integer.toString(index++));
                elem.addAttribute(new MetadataAttribute("name", variableName, true));
                elem.addAttribute(new MetadataAttribute("selection", isSelected, true));
                variableListElement.addElement(elem);
            }
        }
        super.writeProductNodesImpl();
    }


}
