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

package org.esa.beam.dataio.globcarbon;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envi.EnviProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReader extends AbstractProductReader {

    private static final String PRODUCT_PROPERTIES_RESOURCE_PATTERN = "%s.%s.properties";
    private static final String HIGH_RES_IDENTIFIER = "01km";
    private GlobCarbonProductReaderPlugIn readerPlugIn;
    private List<Product> delegateProductList;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected GlobCarbonProductReader(GlobCarbonProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        List<String> headerFiles = getHeaderFiles();
        if (headerFiles.isEmpty()) {
            // should never come here
            throw new IllegalStateException("No header files specified.");
        }
        delegateProductList = initDelegateProductList(headerFiles);

        return createProduct(delegateProductList.get(0));
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Not expected to come here");
    }

    @Override
    public void close() throws IOException {
        super.close();
        for (Product delegateProduct : delegateProductList) {
            delegateProduct.dispose();
        }

    }

    static ProductData.UTC[] parseTimeInformation(String fileName) throws ParseException {
        String[] tokens = fileName.split("_");
        String date = tokens[tokens.length - 2];
        ProductData.UTC startTime;
        Calendar endTimeCal;
        switch (date.length()) {
            case 4:
                startTime = ProductData.UTC.parse(date, "yyyy");
                endTimeCal = startTime.getAsCalendar();
                endTimeCal.set(Calendar.DAY_OF_YEAR, endTimeCal.getActualMaximum(Calendar.DAY_OF_YEAR));
                break;
            case 6:
                startTime = ProductData.UTC.parse(date, "yyyyMM");
                endTimeCal = startTime.getAsCalendar();
                endTimeCal.set(Calendar.DAY_OF_MONTH, endTimeCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case 8:
                startTime = ProductData.UTC.parse(date, "yyyyMMdd");
                endTimeCal = startTime.getAsCalendar();
                break;
            default:
                throw new ParseException("Could not parse date: " + date + ".", -1);
        }
        endTimeCal.set(Calendar.HOUR_OF_DAY, endTimeCal.getActualMaximum(Calendar.HOUR_OF_DAY));
        endTimeCal.set(Calendar.MINUTE, 59);
        endTimeCal.set(Calendar.SECOND, 59);
        return new ProductData.UTC[]{startTime, ProductData.UTC.create(endTimeCal.getTime(), 0)};
    }

    private List<Product> initDelegateProductList(List<String> headerFiles) throws IOException {
        List<Product> list = new ArrayList<Product>(headerFiles.size());
        for (String headerFile : headerFiles) {
            ProductReader delegateReader = new EnviProductReaderPlugIn().createReaderInstance();
            list.add(delegateReader.readProductNodes(headerFile, null));
        }
        return list;
    }

    private Product createProduct(Product templateProduct) {
        final int rasterWidth = templateProduct.getSceneRasterWidth();
        final int rasterHeight = templateProduct.getSceneRasterHeight();
        final String filePath = templateProduct.getFileLocation().getPath();
        String productName = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(filePath));
        final File fileLocation;
        if (productName.contains("!")) {
            productName = productName.substring(productName.indexOf('!') + 1, productName.lastIndexOf('_'));
            fileLocation = new File(filePath.substring(0, filePath.indexOf('!')));
        } else {
            productName = productName.substring(0, productName.lastIndexOf('_'));
            fileLocation = templateProduct.getFileLocation().getParentFile();
        }

        final String[] fileNameTokens = productName.split("_");
        String productType = GlobCarbonProductReaderPlugIn.FORMAT_NAME + "_" + fileNameTokens[0];
        Product product = new Product(productName, productType, rasterWidth, rasterHeight);
        product.setFileLocation(fileLocation);
        templateProduct.transferGeoCodingTo(product, null);
        product.setProductReader(this);
        if (product.getStartTime() == null || product.getEndTime() == null) {
            try {
                ProductData.UTC[] timeInfos = parseTimeInformation(productName);
                product.setStartTime(timeInfos[0]);
                product.setEndTime(timeInfos[1]);
            } catch (ParseException e) {
                Debug.trace("Could not parse date from filename: " + productName + "\nCause: " + e.getMessage());
            }
        }

        String resolutionString = fileNameTokens[2];
        final boolean isHighRes = HIGH_RES_IDENTIFIER.equalsIgnoreCase(resolutionString);
        Properties properties = loadProductProperties(product.getProductType(), isHighRes);
        product.setDescription(properties.getProperty("productDescription"));

        addBands(product, properties, delegateProductList);

        return product;

    }

    private void addBands(Product product, Properties properties, List<Product> delegateProductList) {
        for (Product delegateProduct : delegateProductList) {
            // GlobCarbon products consist of envi products containing one band
            Band delegateBand = delegateProduct.getBandAt(0);
            final String filePath = delegateProduct.getFileLocation().getPath();
            String fileName = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(filePath));
            String bandName = fileName.substring(fileName.lastIndexOf('_') + 1);
            Band band = product.addBand(bandName, delegateBand.getDataType());
            band.setSourceImage(delegateBand.getSourceImage());

            final String propertyKey = bandName.toLowerCase();
            final String noData = properties.getProperty(propertyKey + ".noData");
            if(noData != null) {
                band.setNoDataValue(Integer.parseInt(noData));
                band.setNoDataValueUsed(true);
            }
            final String validExpression = properties.getProperty(propertyKey + ".validExpression");
            if(validExpression != null) {
                band.setValidPixelExpression(validExpression);
            }
            final String scaling = properties.getProperty(propertyKey + ".scaling");
            if(scaling != null) {
                band.setScalingFactor(Double.parseDouble(scaling));
            }
            final String offset = properties.getProperty(propertyKey + ".offset");
            if(offset != null) {
                band.setScalingOffset(Double.parseDouble(offset));
            }
            band.setUnit(properties.getProperty(propertyKey + ".unit"));

            if (band.getName().toLowerCase().contains("flag")) {
                FlagCoding flagCoding = new FlagCoding(band.getName());
                final String[] flagNames = StringUtils.csvToArray(properties.getProperty(propertyKey + ".flagNames"));
                for (String flagName : flagNames) {
                    final String maskString = properties.getProperty(propertyKey + "." + flagName + ".mask");
                    final int flagMask = Integer.decode(maskString);
                    flagCoding.addFlag(flagName, flagMask, "");
                }
                band.setSampleCoding(flagCoding);
                product.getFlagCodingGroup().add(flagCoding);
            }
        }
    }

    private Properties loadProductProperties(String productType, boolean highRes) {
        final Properties properties = new Properties();
        final String resourceName = String.format(PRODUCT_PROPERTIES_RESOURCE_PATTERN,
                                                  productType.toLowerCase(),
                                                  (highRes ? "highRes" : "lowRes"));
        final InputStream inStream = getClass().getResourceAsStream(resourceName);
        try {
            properties.load(inStream);
        } catch (IOException e) {
            Debug.trace("Could not load properties of product.");
            Debug.trace(e);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    private List<String> getHeaderFiles() throws IOException {
        List<String> headerFiles = new ArrayList<String>();
        File inputFile = new File(getInput().toString());
        String[] files = readerPlugIn.getProductFiles(inputFile.getAbsolutePath());
        for (String file : files) {
            if (".hdr".equalsIgnoreCase(FileUtils.getExtension(file))) {
                headerFiles.add(file);
            }
        }
        return headerFiles;
    }
}
