package org.esa.beam.glob.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas Storm
 * Date: 08.03.2010
 * Time: 11:18:08
 * To change this template use File | Settings | File Templates.
 */
public class TimeSeriesGraphTest {

    @Test
    public void addProductGraphTest() {
        List<Product> products = new ArrayList<Product>();

        Product product1 = new Product("name", "type", 1, 1);
        Product product2 = new Product("name2", "type2", 1, 1);
        Product product3 = new Product("name3", "type3", 1, 1);

        product1.setStartTime(new ProductData.UTC(10, 10, 10));
        product2.setStartTime(new ProductData.UTC(30, 10, 10));
        product3.setStartTime(new ProductData.UTC(20, 10, 10));
        
        products.add(product1);

        TimeSeriesDiagram tsd = new TimeSeriesDiagram(products);
        TimeSeriesGraph tsg = tsd.getCurrentGraph();

        assertNotNull(tsg.getDiagram());
        assertEquals(1, tsg.getNumValues());

        tsg.addProduct(product3);
        tsg.addProduct(product2);

        assertTrue(!tsg.getProducts().isEmpty());
        assertEquals(3, tsg.getNumValues());

        assertTrue(tsg.getProducts().get(0).getStartTime().getAsDate().before(tsg.getProducts().get(1).getStartTime().getAsDate()) );
        assertTrue(tsg.getProducts().get(1).getStartTime().getAsDate().before(tsg.getProducts().get(2).getStartTime().getAsDate()) );
    }

}
