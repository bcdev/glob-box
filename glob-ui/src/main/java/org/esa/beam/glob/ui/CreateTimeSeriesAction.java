package org.esa.beam.glob.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DefaultTimeCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.awt.Dimension;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 16:17:58
 */
public class CreateTimeSeriesAction extends AbstractVisatAction {

    public static final String TIME_SERIES_METADATA_ELEMENT = "TIME_SERIES";

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
            final Product tsProduct = createTimeSeriesProduct(timeSeriesName, (RasterDataNode) node);
            app.getProductManager().addProduct(tsProduct);
        }
    }

    private Product createTimeSeriesProduct(String timeSeriesName, RasterDataNode refRaster) {
        final Product tsProduct = new Product(timeSeriesName, "TIME_SERIES",
                                              refRaster.getSceneRasterWidth(),
                                              refRaster.getSceneRasterHeight());
        final Product refProduct = refRaster.getProduct();
        ProductUtils.copyGeoCoding(refProduct, tsProduct);
        // todo replace default time coding
        final Product[] products = VisatApp.getApp().getProductManager().getProducts();
        final MetadataElement timeSeriesMetaData = new MetadataElement(TIME_SERIES_METADATA_ELEMENT);
        final MetadataElement productListElement = new MetadataElement("PRODUCT_LIST");
        timeSeriesMetaData.addElement(productListElement);
        for (Product product : products) {
            final String nodeName = refRaster.getName();
            if (isProductCompatible(product, tsProduct, nodeName)) {
                final ProductData productPath = ProductData.createInstance(product.getFileLocation().getPath());
                productListElement.addAttribute(new MetadataAttribute(product.getName(), productPath, true));

                final RasterDataNode raster = product.getRasterDataNode(nodeName);
                final Band band = tsProduct.addBand(nodeName + "_" + raster.getTimeCoding().getStartTime().format(),
                                                    raster.getGeophysicalDataType());
                band.setSourceImage(raster.getGeophysicalImage());
                copyBandProperties(raster, band);
                // todo replace default time coding
                final ProductData.UTC rasterStartTime = raster.getTimeCoding().getStartTime();
                final ProductData.UTC rasterEndTime = raster.getTimeCoding().getEndTime();
                band.setTimeCoding(new DefaultTimeCoding(rasterStartTime,
                                                         rasterEndTime,
                                                         raster.getSceneRasterHeight()));
                band.setValidPixelExpression(null);
                if (tsProduct.getTimeCoding() == null) {
                    final TimeCoding timeCoding = new DefaultTimeCoding(rasterStartTime,
                                                                        rasterEndTime,
                                                                        refRaster.getSceneRasterHeight());
                    tsProduct.setTimeCoding(timeCoding);
                } else {
                    if (!isWithinTimeSpan(rasterStartTime, tsProduct.getTimeCoding())) {
                        tsProduct.getTimeCoding().setStartTime(rasterStartTime);
                    }
                    if (!isWithinTimeSpan(rasterEndTime, tsProduct.getTimeCoding())) {
                        tsProduct.getTimeCoding().setEndTime(rasterEndTime);
                    }
                }
            }
        }
        tsProduct.getMetadataRoot().addElement(timeSeriesMetaData);
        return tsProduct;
    }

    private void copyBandProperties(RasterDataNode sourceRaster, Band targetBand) {
        targetBand.setDescription(sourceRaster.getDescription());
        targetBand.setUnit(sourceRaster.getUnit());
        targetBand.setNoDataValueUsed(sourceRaster.isNoDataValueUsed());
        targetBand.setGeophysicalNoDataValue(sourceRaster.getGeophysicalNoDataValue());
        targetBand.setValidPixelExpression(sourceRaster.getValidPixelExpression());
        if (sourceRaster instanceof Band) {
            Band sourceBand = (Band) sourceRaster;
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
        }
    }

    private boolean isWithinTimeSpan(ProductData.UTC utc, TimeCoding timeCoding) {
        final long utcSecs = utc.getAsDate().getTime();
        return (utcSecs >= timeCoding.getStartTime().getAsDate().getTime()) &&
               (utcSecs <= timeCoding.getEndTime().getAsDate().getTime());
    }

    private boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f) && product.getFileLocation() != null;
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
