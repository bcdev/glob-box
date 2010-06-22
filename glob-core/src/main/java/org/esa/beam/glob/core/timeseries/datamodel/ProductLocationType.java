package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Product;

import java.util.Collection;
import java.util.Collections;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 11:21:17
 */
public enum ProductLocationType {

    FILE,
    DIRECTORY,
    DIRECTORY_REC;

    public Collection<Product> findProducts(String path) {
        return Collections.emptyList();
    }
}
