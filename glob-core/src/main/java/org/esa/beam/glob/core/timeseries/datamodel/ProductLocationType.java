package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 11:21:17
 */
public enum ProductLocationType {

    FILE{
        @Override
        public List<Product> findProducts(String path) {
            try {
                final Product product = ProductIO.readProduct(path);
                if (product != null) {
                    return Arrays.asList(product);
                }
            } catch (IOException ignore) {
            }
            return Collections.emptyList();
        }
    },
    DIRECTORY,
    DIRECTORY_REC;

    public List<Product> findProducts(String path) {
        return Collections.emptyList();
    }
}
