package org.esa.beam.glob.core;

import com.bc.ceres.core.ExtensionManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TimeCodingExtensionFactoryTest {
    private static Product product;
    private static ExtensionManager extensionManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        extensionManager = ExtensionManager.getInstance();
        extensionManager.register(ProductNode.class, new TimeCodingExtensionFactory());
        product = createDummyProduct();
    }

    @Test
    public void testGetExtension() throws Exception {
        Band band = product.getBandAt(0);
        assertNotNull(extensionManager.getExtension(band, TimeCoding.class));
        assertNotNull(extensionManager.getExtension(product, TimeCoding.class));
    }

    private static Product createDummyProduct() {
        Product product = new Product("dummy", "t", 2, 2);
        product.addBand("b1", ProductData.TYPE_INT16);
        product.addTiePointGrid(new TiePointGrid("t1", 2, 2, 0, 0, 1, 1,
                                                 new float[]{1.0f, 2.0f, 3.0f, 4.0f}));
        return product;
    }

}
