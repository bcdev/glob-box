package org.esa.beam.glob.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 11:21:17
 */
public enum ProductLocationType {

    FILE {
        @Override
        public List<Product> findProducts(String path, ProgressMonitor pm) {
            return readSingleProduct(new File(path));
        }
    },
    DIRECTORY {
        @Override
        public List<Product> findProducts(String path, ProgressMonitor pm) {
            List<Product> products = new ArrayList<Product>();
            final File[] files = listFiles(path);
            pm.beginTask("Scanning for products...", files.length);
            try {
                for (File file : files) {
                    if (!file.isDirectory()) {
                        products.addAll(readSingleProduct(file));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
            return products;
        }
    },
    DIRECTORY_REC {
        @Override
        public List<Product> findProducts(String path, ProgressMonitor pm) {
            List<Product> products = new ArrayList<Product>();
            final File[] files = listFiles(path);
            pm.beginTask("Scanning for products...", files.length);
            try {
                for (File file : files) {
                    if (file.isDirectory()) {
                        products.addAll(findProducts(file.getPath(), new SubProgressMonitor(pm, 1)));
                    } else {
                        products.addAll(readSingleProduct(file));
                    }
                }
            } finally {
                pm.done();
            }
            return products;
        }
    };

    abstract List<Product> findProducts(String path, ProgressMonitor pm);

    private static File[] listFiles(String path) {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The given path is not a directory: " + path);
        }
        return dir.listFiles();
    }

    private static List<Product> readSingleProduct(File path) {
        try {
            final Product product = ProductIO.readProduct(path);
            if (product != null && product.getStartTime() != null && product.getEndTime() != null) {
                return Arrays.asList(product);
            }
        } catch (IOException ignore) {
        }
        return Collections.emptyList();
    }
}
