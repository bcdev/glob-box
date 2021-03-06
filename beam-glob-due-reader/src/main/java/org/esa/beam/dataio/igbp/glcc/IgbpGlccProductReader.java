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

package org.esa.beam.dataio.igbp.glcc;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.CsvReader;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

class IgbpGlccProductReader extends AbstractProductReader {

    private static final int RASTER_WIDTH = 43200;
    private static final int RASTER_HEIGHT = 21600;
    private static final String PRODUCT_TYPE = "IGBP_GLCC";
    private static final String BAND_NAME = "classes";
    private static final String GLCC_PROPERTIES_FILE = "glcc.properties";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected IgbpGlccProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = getInputFile();
        String inputFileName = inputFile.getName();
        String productId = inputFileName.substring(1, inputFileName.indexOf('2'));
        String productName = PRODUCT_TYPE + "_" + productId.toUpperCase();
        final Product product = new Product(productName, PRODUCT_TYPE, RASTER_WIDTH, RASTER_HEIGHT);
        final CrsGeoCoding geoCoding = createGeoCoding();
        product.setGeoCoding(geoCoding);
        product.setDescription( getDescription( productId ) );
        final Band band = product.addBand(BAND_NAME, ProductData.TYPE_INT8);
        applyIndexCoding(band, productId);
        band.setSourceImage(getMultiLevelImage(ImageManager.getImageToModelTransform(geoCoding)));

        return product;
    }

    private String getDescription(String productName) throws IOException {
        Properties descriptions = new Properties();
        descriptions.load( new InputStreamReader( getClass().getResourceAsStream(GLCC_PROPERTIES_FILE) ) );
        return descriptions.getProperty( productName + ".description" );
    }

    private void applyIndexCoding(Band band, String productId) throws IOException {
        final Product product = band.getProduct();
        final InputStream stream = this.getClass().getResourceAsStream(productId.toLowerCase() + ".csv");
        final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{';'});
        final List<String[]> legendStrings = csvReader.readStringRecords();
        final IndexCoding indexCoding = new IndexCoding(productId + "_classes");
        ColorPaletteDef.Point[] colorPoints = new ColorPaletteDef.Point[legendStrings.size()];
        for (int i = 0; i < legendStrings.size(); i++) {
            String[] legendString = legendStrings.get(i);
            final int value = Integer.parseInt(legendString[0]);
            final String description = legendString[1].trim();
            final String name = "class_" + i;
            indexCoding.addIndex(name, value, description);
            final String colorString = legendString[2];
            final String[] rgb = StringUtils.csvToArray(colorString);
            final Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
            colorPoints[i] = new ColorPaletteDef.Point(value, color, name);
        }

        band.setImageInfo(new ImageInfo(new ColorPaletteDef(colorPoints)));
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);
    }

    private File getInputFile() {
        return new File(getInput().toString());
    }

    private CrsGeoCoding createGeoCoding() {
        AffineTransform i2m = new AffineTransform();
        double scale = 1.0 / 120.0;
        double easting = -647985.0 / 3600.0;
        double northing = 323985.0 / 3600.0;
        i2m.translate(easting, northing);
        i2m.scale(scale, -scale);
        i2m.translate(-0.5, -0.5);

        try {
            // todo - validate if it is WGS_84
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(RASTER_WIDTH, RASTER_HEIGHT), i2m);
        } catch (FactoryException e) {
            Debug.trace(e);
        } catch (TransformException e) {
            Debug.trace(e);
        }
        return null;
    }

    private DefaultMultiLevelImage getMultiLevelImage(AffineTransform i2mTransform) {

        final DefaultMultiLevelModel multiLevelModel = new DefaultMultiLevelModel(i2mTransform,
                                                                                  RASTER_WIDTH, RASTER_HEIGHT);
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new IgbpGlccOpImage(RASTER_WIDTH, RASTER_HEIGHT,
                                           ResolutionLevel.create(multiLevelModel, level), getInputFile());
            }
        });
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Nothing to read here.");
    }

}
