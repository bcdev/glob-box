package org.esa.beam.glob;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static junit.framework.Assert.*;

public class GlobBoxManagerTest {

    private static GlobBoxManager globBoxManager;
    private static ProductManager productManager;
    private static final String RASTER_1 = "raster_1";

    @BeforeClass
    public static void setUp() {
        globBoxManager = GlobBoxManager.getInstance();
        productManager = new ProductManager();
        globBoxManager.setProductManager(productManager);
    }

    @After
    public void tearDown() {
        productManager.removeAllProducts();
    }

    @Test
    public void testBasics() {
        assertNotNull( globBoxManager );

        final List<Product> productList = globBoxManager.getCurrentProductList();
        assertEquals(0, productList.size());

        assertEquals( 0, globBoxManager.getCompatibleRasterList().size() );

        assertNotNull( globBoxManager.getSceneViewListener() );

    }

    @Test
    public void testCurrentProductList() {
        final Product firstProduct = createProduct("p1");

        productManager.addProduct(firstProduct);
        assertEquals(0, globBoxManager.getCurrentProductList().size());

        globBoxManager.setReferenceRaster(firstProduct.getRasterDataNode(RASTER_1));
        assertEquals(1, globBoxManager.getCurrentProductList().size());

        productManager.addProduct(createProduct("p2"));
        assertEquals(2, globBoxManager.getCurrentProductList().size());

        productManager.addProduct(createProduct("p3"));
        productManager.removeProduct(firstProduct);
        assertEquals(2, globBoxManager.getCurrentProductList().size());
    }

    @Test
    public void testCompatibleRasterList() {
        final Product firstProduct = createProduct("p1");
        globBoxManager.setReferenceRaster(firstProduct.getRasterDataNode(RASTER_1));
        productManager.addProduct(firstProduct);
        productManager.addProduct(createProduct("p2"));

        List<RasterDataNode> rasterDataNodes = globBoxManager.getCompatibleRasterList();
        assertEquals(2, rasterDataNodes.size());

        productManager.addProduct(createProduct("p3"));
        assertEquals(3, productManager.getProducts().length);
        assertEquals(3, globBoxManager.getCurrentProductList().size());

        rasterDataNodes = globBoxManager.getCompatibleRasterList();
        assertEquals(3, rasterDataNodes.size());

        assertSame(rasterDataNodes, globBoxManager.getCompatibleRasterList());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void productListUnmodifiable() {
        globBoxManager.getCurrentProductList().add( null );
    }

    private Product createProduct(String name) {
        final Product product = new Product(name, "T1", 10, 10);
        product.setStartTime(ProductData.UTC.create(new Date(), 0));
        product.addBand(RASTER_1, ProductData.TYPE_INT8);
        return product;
    }

}
