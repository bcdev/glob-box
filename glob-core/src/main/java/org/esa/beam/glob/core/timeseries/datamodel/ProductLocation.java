package org.esa.beam.glob.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;

import java.util.Collections;
import java.util.List;

public class ProductLocation {

    private final ProductLocationType productLocationType;
    private final String path;
    private List<Product> productList;

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

    public synchronized void loadProducts(ProgressMonitor pm) {
        productList = productLocationType.findProducts( path, pm);
    }

    public List<Product> getProducts() {
        if (productList == null) {
            loadProducts(ProgressMonitor.NULL);
        }
        return Collections.unmodifiableList(productList);
    }

    public synchronized void closeProducts() {
        if (productList != null) {
            for (Product product : productList) {
                product.dispose();
            }
            productList = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductLocation that = (ProductLocation) o;

        if (!path.equals(that.path)) return false;
        if (productLocationType != that.productLocationType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = productLocationType.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
