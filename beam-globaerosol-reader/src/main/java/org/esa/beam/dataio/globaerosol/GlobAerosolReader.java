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

package org.esa.beam.dataio.globaerosol;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.merisl3.ISINGrid;
import org.esa.beam.dataio.netcdf.NcAttributeMap;
import org.esa.beam.dataio.netcdf.NcVariableMap;
import org.esa.beam.dataio.netcdf.NetcdfReaderUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GlobAerosolReader extends AbstractProductReader {

    private static final String NC_ATTRIBUTE_START_DATE = "StartDate";
    private static final String NC_VARIABLE_MODEL = "model";

    private static final int ROW_COUNT = 2004;

    private NetcdfFile ncfile;
    private ISINGrid isinGrid;
    private Map<Band, VariableAccessor1D> accessorMap;
    private RowInfo[] rowInfos;
    private Band lonBand;

    private int width;
    private int height;
    static final String UTC_DATE_PATTERN = "yyyy-MM-dd";
    private static final String NC_ATTRIBUTE_PERIOD = "Period";
    private static final String NC_ATTRIBUTE_PRODUCT_ID = "ProductID";

    protected GlobAerosolReader(GlobAerosolReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        ncfile = getInputNetcdfFile();
        accessorMap = new HashMap<Band, VariableAccessor1D>();
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        synchronized (this) {
            if (rowInfos == null) {
                rowInfos = createRowInfos();
            }
        }

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                if (destBand.isNoDataValueUsed()) {
                    double noDataValue = destBand.getNoDataValue();
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                        destBuffer.setElemDoubleAt(rasterIndex, noDataValue);
                    }
                }
                final int rowIndex = (height - 1) - y;
                RowInfo rowInfo = rowInfos[rowIndex];
                if (rowInfo != null) {
                    Array lonData = read(lonBand, rowInfo);
                    Array bandData = read(destBand, rowInfo);
                    int dataSize = (int) bandData.getSize();
                    for (int dataIndex = 0; dataIndex < dataSize; dataIndex++) {
                        double lon = lonData.getDouble(dataIndex) + 180;
                        int colIndex = isinGrid.getColIndex(rowIndex, lon);

                        int rowLength = isinGrid.getRowLength(rowIndex);
                        int x = isinGrid.getRowCount() - (rowLength / 2) + colIndex;

                        if (x >= sourceOffsetX && x < sourceOffsetX + sourceWidth) {
                            final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                            destBuffer.setElemDoubleAt(rasterIndex, bandData.getDouble(dataIndex));
                        }
                    }
                } else {
                    // ???
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private Array read(Band band, RowInfo rowInfo) throws IOException {
        VariableAccessor1D accessor = accessorMap.get(band);
        synchronized (ncfile) {
            return accessor.read(rowInfo).reduce();
        }
    }

    @Override
    public void close() throws IOException {
        accessorMap.clear();
        isinGrid = null;
        if (ncfile != null) {
            ncfile.close();
            ncfile = null;
        }
        super.close();
    }

    private Product createProduct() throws IOException {
        isinGrid = new ISINGrid(ROW_COUNT);
        width = isinGrid.getRowCount() * 2;
        height = isinGrid.getRowCount();
        NcAttributeMap globalAttributes = NcAttributeMap.create(ncfile);
        String prodName = globalAttributes.getStringValue(NC_ATTRIBUTE_PRODUCT_ID);
        if (prodName == null) {
            prodName = FileUtils.getFilenameWithoutExtension(ncfile.getLocation());
        }
        final String[] idElements = prodName.split("_");
        final String prodType = idElements[0] + "_" + idElements[1];
        final Product product = new Product(prodName, prodType, width, height);

        try {
            final Attribute startDateAttribute = ncfile.findGlobalAttribute(NC_ATTRIBUTE_START_DATE);
            final ProductData.UTC startTime = ProductData.UTC.parse(startDateAttribute.getStringValue(),
                                                                    UTC_DATE_PATTERN);
            product.setStartTime(startTime);
            product.setEndTime(calcEndTime(startTime, getPeriod()));
        } catch (ParseException e) {
            Debug.trace(e);
        }

        addBands(product);
        lonBand = product.getBand("lon");
        addGeoCoding(product);

        NetcdfReaderUtils.transferMetadata(ncfile, product.getMetadataRoot());
        return product;
    }

    static ProductData.UTC calcEndTime(ProductData.UTC startDate, Period period) {
        final Calendar asCalendar = startDate.getAsCalendar();
        asCalendar.add(period.getCalendarFieldIndex(), period.getAmount());
        asCalendar.add(Calendar.SECOND, -1);
        return ProductData.UTC.create(asCalendar.getTime(), 0);
    }

    private Period getPeriod() {
        final Attribute startDateAttribute = ncfile.findGlobalAttribute(NC_ATTRIBUTE_PERIOD);
        return Period.valueOf(startDateAttribute.getStringValue());
    }

    private void addGeoCoding(Product product) throws IOException {

        DefaultGeographicCRS base = DefaultGeographicCRS.WGS84;
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        ParameterValueGroup parameters;
        try {
            parameters = transformFactory.getDefaultParameters("OGC:Sinusoidal");
        } catch (NoSuchIdentifierException e) {
            throw new IOException(e);
        }
        DefaultEllipsoid ellipsoid = (DefaultEllipsoid) base.getDatum().getEllipsoid();
        parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
        double latExtendMeters = ellipsoid.orthodromicDistance(0.0, 90.0, 0.0, -90);
        double lonExtendMeters = ellipsoid.orthodromicDistance(0.0, 0.0, 180.0, 0.0) * 2;

        MathTransform mathTransform;
        try {
            mathTransform = transformFactory.createParameterizedTransform(parameters);
        } catch (Exception e) {
            throw new IOException(e);
        }

        CoordinateReferenceSystem modelCrs = new DefaultProjectedCRS("Sinusoidal", base, mathTransform,
                                                                     DefaultCartesianCS.PROJECTED);
        try {
            double pixelSizeX = lonExtendMeters / width;
            double pixelSizeY = latExtendMeters / height;
            double easting = -lonExtendMeters / 2;
            double northing = latExtendMeters / 2;
            CrsGeoCoding geoCoding = new CrsGeoCoding(modelCrs, width, height, easting, northing, pixelSizeX,
                                                      pixelSizeY);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void addBands(Product product) {
        Variable modelVar = ncfile.getRootGroup().findVariable(NC_VARIABLE_MODEL);
        String[] modeNames = null;
        if (modelVar != null && modelVar.getDimension(0).getLength() > 1) {
            NcAttributeMap modelAttMap = NcAttributeMap.create(modelVar);
            modeNames = modelAttMap.getStringValue("flag_meanings").split(" ");
        }
        List<Variable> variableList = ncfile.getRootGroup().getVariables();
        for (Variable variable : variableList) {
            int cellDimemsionIndex = variable.findDimensionIndex("cell");
            if (cellDimemsionIndex != -1) {
                final NcAttributeMap attMap = NcAttributeMap.create(variable);
                int modelDimemsionIndex = variable.findDimensionIndex(NC_VARIABLE_MODEL);
                String bandName = NcVariableMap.getAbsoluteName(variable);
                IndexCoding indexCoding = NetcdfReaderUtils.createIndexCoding(bandName + "_coding", attMap);
                if (indexCoding != null) {
                    product.getIndexCodingGroup().add(indexCoding);
                }
                if (modeNames != null && modelDimemsionIndex != -1) {
                    for (int i = 0; i < modeNames.length; i++) {
                        Band band = NetcdfReaderUtils.createBand(variable, attMap, null, width, height);
                        band.setName(band.getName() + "_" + modeNames[i]);
                        Map<Integer, Integer> dimSelection = new HashMap<Integer, Integer>();
                        dimSelection.put(modelDimemsionIndex, i);
                        handleBand(band, product, variable, cellDimemsionIndex, indexCoding, dimSelection);
                    }
                } else {
                    Band band = NetcdfReaderUtils.createBand(variable, attMap, null, width, height);
                    Map<Integer, Integer> dimSelection = Collections.EMPTY_MAP;
                    handleBand(band, product, variable, cellDimemsionIndex, indexCoding, dimSelection);
                }
            }
        }
    }

    private void handleBand(Band band, Product product, Variable variable, int cellDimemsionIndex,
                            IndexCoding indexCoding, Map<Integer, Integer> dimSelection) {
        VariableAccessor1D accessor = new VariableAccessor1D(variable, cellDimemsionIndex, dimSelection);
        if (indexCoding != null) {
            band.setSampleCoding(indexCoding);
        }
        accessorMap.put(band, accessor);
        product.addBand(band);
    }

    private NetcdfFile getInputNetcdfFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        final String path;
        if (input instanceof String) {
            path = (String) input;
        } else {
            path = ((File) input).getPath();
        }

        return NetcdfFile.open(path);
    }

    private RowInfo[] createRowInfos() throws IOException {
        final RowInfo[] binLines = new RowInfo[height];
        final Variable latVariable = ncfile.getRootGroup().findVariable("lat");
        final float[] latValues = (float[]) latVariable.read().getStorage();
        double deltaLat = isinGrid.getDeltaLat();
        double lastLatValue = -91;
        int lastRowIndex = -1;
        int lineOffset = 0;
        int lineLength = 0;
        for (int i = 0; i < latValues.length; i++) {

            final double lat = latValues[i];
            if (lat < lastLatValue) {
                throw new IOException(
                        "Unrecognized level-3 format. Bins numbers expected to appear in ascending order.");
            }
            if (lastLatValue == lat) {
                lineLength++;
            } else {
                lastLatValue = lat;
                int rowIndex = (int) Math.round(((lat + 90.0) / deltaLat) + 0.5);

                if (rowIndex == lastRowIndex) {
                    throw new IOException("boooo");
                }
                if (lineLength > 0) {
                    lastRowIndex = rowIndex;
                    binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
                }
                lineOffset = i;
                lineLength = 1;
            }
        }

        if (lineLength > 0) {
            binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
        }

        return binLines;
    }

    private static final class RowInfo {

        final int rowOffset;
        final int numBins;

        private RowInfo(int rowOffset, int numBins) {
            this.rowOffset = rowOffset;
            this.numBins = numBins;
        }
    }

    private static class VariableAccessor1D {

        private final Variable variable;
        private final int indexDim;
        private final int rank;
        private final Map<Integer, Integer> dimSelection;

        private VariableAccessor1D(Variable variable, int indexDim, Map<Integer, Integer> dimSelection) {
            this.variable = variable;
            this.indexDim = indexDim;
            this.dimSelection = dimSelection;
            this.rank = variable.getRank();
        }

        public Array read(RowInfo rowInfo) throws IOException {
            try {
                Section section = getSection(rowInfo.rowOffset, rowInfo.numBins);
                return variable.read(section).reduce();
            } catch (InvalidRangeException e) {
                throw new IOException(e);
            }
        }

        private Section getSection(int offset, int length) throws InvalidRangeException {
            int[] origin = new int[rank];
            int[] size = new int[rank];
            for (int i = 0; i < rank; i++) {
                if (i == indexDim) {
                    origin[i] = offset;
                    size[i] = length;
                } else if (dimSelection.containsKey(i)) {
                    origin[i] = dimSelection.get(i);
                    size[i] = 1;
                } else {
                    origin[i] = 0;
                    size[i] = 1;
                }
            }
            return new Section(origin, size);
        }
    }

}
