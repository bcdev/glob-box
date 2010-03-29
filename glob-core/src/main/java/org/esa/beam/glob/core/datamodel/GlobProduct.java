package org.esa.beam.glob.core.datamodel;

import org.esa.beam.framework.datamodel.Product;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 15:57:55
 */
public class GlobProduct extends Product {

    public GlobProduct(String name, String type, int sceneRasterWidth, int sceneRasterHeight) {
        super(name, type, sceneRasterWidth, sceneRasterHeight);
    }
}
