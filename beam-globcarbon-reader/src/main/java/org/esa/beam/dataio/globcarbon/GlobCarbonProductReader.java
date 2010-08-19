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
import org.esa.beam.dataio.envi.Header;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReader extends AbstractProductReader {

    private static GlobCarbonProductReaderPlugIn readerPlugIn;
    private Map<String, String> productDescriptionMap;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected GlobCarbonProductReader(GlobCarbonProductReaderPlugIn readerPlugIn) {
        super(GlobCarbonProductReader.readerPlugIn);
        GlobCarbonProductReader.readerPlugIn = readerPlugIn;
        initDescriptionMap();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        List<String> headerFiles = getHeaderFiles();
        if (headerFiles.isEmpty()) {
            // should never come here
            throw new IllegalStateException("No header files specified.");
        }
        String firstHeaderFile = headerFiles.get(0);
        Product product = createProduct(firstHeaderFile);

        for (String headerFile : headerFiles) {
            ProductReader delegate = new EnviProductReaderPlugIn().createReaderInstance();
            Product temp = delegate.readProductNodes(headerFile, null);
            if (product.getGeoCoding() == null) {
                product.setGeoCoding(temp.getGeoCoding());
            }
            for (Band delegateBand : temp.getBands()) {
                String fileName = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(headerFile));
                String bandName = fileName.substring(fileName.lastIndexOf('_') + 1);
                Band band = product.addBand(bandName, delegateBand.getDataType());
                band.setSourceImage(delegateBand.getSourceImage());
            }
        }

        if (product.getStartTime() == null || product.getEndTime() == null) {
            String fileName = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(firstHeaderFile));
            try {
                ProductData.UTC[] timeInfos = parseTimeInformation(fileName);
                product.setStartTime(timeInfos[0]);
                product.setEndTime(timeInfos[1]);
            } catch (ParseException e) {
                Debug.trace("Could not parse date from filename: " + fileName + "\nCause: " + e.getMessage());
            }
        }
        return product;
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

    private Product createProduct(String headerFile) throws IOException {
        Reader reader;
        if (headerFile.contains("!")) {
            // headerFile is in zip
            String[] splittedHeaderFile = headerFile.split("!");
            ZipFile zipFile = new ZipFile(new File(splittedHeaderFile[0]));
            InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(splittedHeaderFile[1]));
            reader = new InputStreamReader(inputStream);
        } else {
            reader = new FileReader(headerFile);
        }
        Header header = new Header(new BufferedReader(reader));
        String fileName = FileUtils.getFilenameWithoutExtension(FileUtils.getFileNameFromPath(headerFile));
        if (fileName.contains("!")) {
            fileName = fileName.substring(fileName.indexOf("!") + 1, fileName.lastIndexOf("_"));
        }
        String carbonType = fileName.split("_")[0];
        String fileType = GlobCarbonProductReaderPlugIn.FORMAT_NAME + "_" + carbonType;
        Product product = new Product(fileName, fileType, header.getNumSamples(), header.getNumLines());
        product.setProductReader(this);
        product.setFileLocation(new File(headerFile));
        product.setDescription(productDescriptionMap.get(carbonType));
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Not expected to come here");
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

    private void initDescriptionMap() {
        productDescriptionMap = new HashMap<String, String>();
        productDescriptionMap.put("BAE", "Global monthly Burnt Area Estimates");
        productDescriptionMap.put("LAI", "Global monthly Leaf Area Index");
        productDescriptionMap.put("VGCP", "Global yearly Vegetation Growth Cycle Period");
        productDescriptionMap.put("FAPAR", "Global daily Fraction of Absorbed Photosynthetically Active Radiation");
    }

}
