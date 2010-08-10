/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
            if (product != null && product.getStartTime() != null) {
                return Arrays.asList(product);
            }
        } catch (IOException ignore) {
        }
        return Collections.emptyList();
    }
}
