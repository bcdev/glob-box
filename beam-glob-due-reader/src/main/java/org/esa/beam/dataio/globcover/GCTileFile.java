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

package org.esa.beam.dataio.globcover;

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.jdom.Element;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class GCTileFile {

    private static final String XDIM = "XDim";
    private static final String YDIM = "YDim";
    private static final String GROUP_POSTEL = "POSTEL";
    private static final String GROUP_DATA_FIELDS = "Data Fields";
    private static final String GROUP_GRID_ATTRIBUTES = "Grid Attributes";
    private static final String STRUCT_METADATA_0 = "StructMetadata%2e0";   // StructMetadata.0
    private static final String DF_DIMENSION_X = GROUP_POSTEL + "/" + GROUP_DATA_FIELDS + "/" + XDIM;
    private static final String DF_DIMENSION_Y = GROUP_POSTEL + "/" + GROUP_DATA_FIELDS + "/" + YDIM;
    private static final String GA_START_DATE = GROUP_POSTEL + "/" + GROUP_GRID_ATTRIBUTES + "/Product start date";
    private static final String GA_END_DATE = GROUP_POSTEL + "/" + GROUP_GRID_ATTRIBUTES + "/Product end date";
    private static final String ATTRIB_UNSIGNED = "_Unsigned";
    private static final String ATTRIB_FILL_VALUE = "_FillValue";

    private final NetcdfFile ncFile;

    GCTileFile(File ncfile) throws IOException {
        ncFile = NetcdfFile.open(ncfile.getCanonicalPath());
    }

    public String getFilePath() {
        return ncFile.getLocation();
    }

    public int getWidth() {
        return ncFile.findDimension(DF_DIMENSION_X).getLength();
    }

    public int getHeight() {
        return ncFile.findDimension(DF_DIMENSION_Y).getLength();
    }

    public ProductData.UTC getStartDate() {
        return getDate(GA_START_DATE);

    }

    public ProductData.UTC getEndDate() {
        return getDate(GA_END_DATE);
    }

    public GeoPos getUpperLeftCorner() throws IOException {
        final Variable structMetadata0 = ncFile.findVariable(STRUCT_METADATA_0);
        final Array array = structMetadata0.read();
        final String structMetadata0Text = new String(array.getDataAsByteBuffer().array());
        final Element element = new ODLparser().parseFromString(structMetadata0Text);
        final Element grid = element.getChild("GridStructure").getChild("GRID_1");
        final Element ulPointDegElem = grid.getChild("UpperLeftPointMtrs");
        List ulValueElements = ulPointDegElem.getChildren("value");
        String ulPointLon = ((Element) ulValueElements.get(0)).getText();
        String ulPointLat = ((Element) ulValueElements.get(1)).getText();
        return createGeoPos(ulPointLon, ulPointLat);
    }


    public Array readData(String variableName,
                          int offsetX, int offsetY,
                          int width, int height,
                          int stepX, int stepY) throws IOException, InvalidRangeException {
        final Group group = ncFile.getRootGroup().findGroup(GROUP_POSTEL).findGroup(GROUP_DATA_FIELDS);
        final Variable variable = group.findVariable(variableName);
        if (variable == null) {
            throw new IOException("Unknown variable name: " + variableName);
        }
        final int indexDimX = variable.findDimensionIndex(XDIM);
        final int indexDimY = variable.findDimensionIndex(YDIM);
        int[] origin = new int[2];
        int[] size = new int[2];
        int[] stride = new int[2];
        origin[indexDimX] = offsetX;
        origin[indexDimY] = offsetY;
        size[indexDimX] = width;
        size[indexDimY] = height;
        stride[indexDimX] = stepX;
        stride[indexDimY] = stepY;
        final Section section = new Section(origin, size, stride);
        final Array array;
        synchronized (ncFile) {
            array = variable.read(section);
        }
        return array;
    }

    public List<BandDescriptor> getBandDescriptorList() {
        final Group rootGroup = ncFile.getRootGroup();
        final Group group = rootGroup.findGroup(GROUP_POSTEL).findGroup(GROUP_DATA_FIELDS);
        final List<Variable> bandVariables = group.getVariables();
        final List<BandDescriptor> bandDescriptors = new ArrayList<BandDescriptor>(bandVariables.size());
        for (Variable bandVariable : bandVariables) {
            bandDescriptors.add(getBandDescriptor(bandVariable));
        }
        return bandDescriptors;
    }

    public boolean isAnnualFile() {
        boolean annual;
        final File fileLocation = new File(getFilePath());
        final String prodName = FileUtils.getFilenameWithoutExtension(fileLocation);
        annual = prodName.toUpperCase().contains("ANNUAL");
        return annual;
    }

    public void readMetadata(MetadataElement metadataRoot) {
        metadataRoot.addElement(MetadataUtils.readAttributeList(ncFile.getGlobalAttributes(), "MPH"));
    }

    @Override
    public String toString() {
        return ncFile.getLocation();
    }

    public void close() throws IOException {
        ncFile.close();
    }

    private ProductData.UTC getDate(String attribName) {
        final String dateString = ncFile.findGlobalAttributeIgnoreCase(attribName).getStringValue();
        try {
            // This is the date pattern as it is defined in the documentation
            String datePattern = "yyyy/MM/dd HH:mm:ss";
            if (isAnnualFile()) {
                // for the annual mosaic the date pattern differs from the documentation
                datePattern = "yyyy/dd/MM HH:mm:ss";
            }
            return ProductData.UTC.parse(dateString, datePattern);
        } catch (ParseException e) {
            Debug.trace(String.format("Can not parse date of attriubte '%s': %s", attribName, e.getMessage()));
        }
        return ProductData.UTC.create(new Date(), 0);
    }

    private BandDescriptor getBandDescriptor(Variable variable) {
        BandDescriptor bd = new BandDescriptor();
        final String name = variable.getShortName();
        bd.setName(name);
        bd.setDataType(getProductDataType(variable));
        bd.setWidth(variable.getDimension(variable.findDimensionIndex(GCTileFile.XDIM)).getLength());
        bd.setHeight(variable.getDimension(variable.findDimensionIndex(GCTileFile.YDIM)).getLength());
        bd.setDescription(variable.getDescription());
        bd.setUnit(variable.getUnitsString());

        final String gridAttribPrefix = String.format("%s/%s/",
                                                      GCTileFile.GROUP_POSTEL, GCTileFile.GROUP_GRID_ATTRIBUTES);
        final String offsetName = String.format("%s%s%s", gridAttribPrefix, "Offset ", name);
        final Number offset = ncFile.findGlobalAttributeIgnoreCase(offsetName).getNumericValue();
        final String scaleName = String.format("%s%s%s", gridAttribPrefix, "Scale ", name);
        final Number scale = ncFile.findGlobalAttributeIgnoreCase(scaleName).getNumericValue();
        // Product Description Manual - GlobCover: p. 17
        // The physical value FDphys is given by the following formula:
        // FDphys = (FD - OffsetFD) / ScaleFD
        // that's why we have to convert the values
        bd.setScaleFactor(1 / scale.doubleValue());
        double offsetValue = 0.0;
        if (offset.doubleValue() != 0.0) {
            offsetValue = -offset.doubleValue() / scale.doubleValue();
        }
        bd.setOffsetValue(offsetValue);
        Attribute fillAttrib = variable.findAttribute(GCTileFile.ATTRIB_FILL_VALUE);
        bd.setFillValueUsed(fillAttrib != null);
        double fillValue = 0;
        if (fillAttrib != null) {
            fillValue = fillAttrib.getNumericValue().doubleValue();
        }
        bd.setFillValue(fillValue);
        return bd;
    }

    private Integer getProductDataType(Variable variable) {
        Attribute unsignedAttrib = variable.findAttribute(ATTRIB_UNSIGNED);
        boolean isUnsigned = false;
        if (unsignedAttrib != null) {
            isUnsigned = Boolean.parseBoolean(unsignedAttrib.getStringValue());
        }
        return DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), isUnsigned, true);
    }

    static GeoPos createGeoPos(String lonString, String latString) {
        float lon = dgmToDec(lonString);
        float lat = dgmToDec(latString);
        return new GeoPos(lat, lon);
    }

    private static float dgmToDec(String dgmString) {
        final int dotIndex = dgmString.indexOf('.');
        final int secondsIndex = Math.max(0, dotIndex - 3);
        final float seconds = Float.parseFloat(dgmString.substring(secondsIndex));
        final int minutes;
        final int minutesIndex = Math.max(0, secondsIndex - 3);
        if (secondsIndex > 0) {
            minutes = Integer.parseInt(dgmString.substring(minutesIndex, secondsIndex));
        } else {
            minutes = 0;
        }
        final int degrees;
        if (minutesIndex > 0) {
            degrees = Integer.parseInt(dgmString.substring(0, minutesIndex));
        } else {
            degrees = 0;
        }
        if (dgmString.startsWith("-")) {
            return degrees - minutes / 60.0f - seconds / 3600.0f;
        } else {
            return degrees + minutes / 60.0f + seconds / 3600.0f;
        }
    }

}
