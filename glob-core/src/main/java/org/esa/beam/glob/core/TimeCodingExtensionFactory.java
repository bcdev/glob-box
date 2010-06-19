package org.esa.beam.glob.core;

import com.bc.ceres.core.ExtensionFactory;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;

class TimeCodingExtensionFactory implements ExtensionFactory {
    @Override
    public Object getExtension(Object object, Class<?> extensionType) {
        if (object instanceof ProductNode) {
            ProductNode node = (ProductNode) object;
            Product product = node.getProduct();
            ProductData.UTC startTime = product.getStartTime();
            ProductData.UTC endTime = product.getEndTime();
            return new DefaultTimeCoding(startTime, endTime,
                                         product.getSceneRasterHeight());
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[] {TimeCoding.class};
    }
}
