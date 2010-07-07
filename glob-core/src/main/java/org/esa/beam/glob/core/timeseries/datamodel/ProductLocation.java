package org.esa.beam.glob.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;

import java.util.List;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 11:22:05
 */
public class ProductLocation {

    private final ProductLocationType productLocationType;
    private final String path;

    public ProductLocation(ProductLocationType productLocationType, String path) {
        this.productLocationType = productLocationType;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public ProductLocationType getProductLocationType() {
        return productLocationType;
    }

    public List<Product> findProducts(ProgressMonitor pm) {
        return this.productLocationType.findProducts( path, pm);
    }
}
