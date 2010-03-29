package org.esa.beam.glob.export.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 25.03.2010
 * Time: 08:22:15
 */
public class NetCdfWriter extends AbstractProductWriter implements NetCdfConstants {

    private NetcdfFileWriteable outFile;
    private String outputLocation;

    private Array data;
    private Group rootGroup;

    public NetCdfWriter(ProductWriterPlugIn writerPlugIn, String outputLocation) {
        super(writerPlugIn);
        this.outputLocation = outputLocation;
        try {
            outFile = NetcdfFileWriteable.createNew(outputLocation, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDimension(Dimension dimension) {
        outFile.addDimension(dimension.getName(), dimension.getLength());
    }

    public void addUnlimitedDimension(Dimension dimension) {
        outFile.addUnlimitedDimension(dimension.getName());
    }

    public void addVariable(Variable var) {
        outFile.getRootGroup().addVariable(var);
    }

    public void addDefaultLatVariable() {
        Attribute longNameLat = new Attribute(LONG_NAME, LATITUDE);
        Attribute standardNameLat = new Attribute(STANDARD_NAME, LATITUDE);
        Attribute unitsLat = new Attribute(UNITS, DEGREES_NORTH);
        Attribute axisLat = new Attribute(AXIS, "Y");

        rootGroup = getRootGroup();
        Variable latVar = new Variable(outFile, rootGroup,
                                       null, LAT_VAR_NAME, DataType.INT, LAT_VAR_NAME);
        latVar.addAttribute(longNameLat);
        latVar.addAttribute(standardNameLat);
        latVar.addAttribute(unitsLat);
        latVar.addAttribute(axisLat);

        addVariable(latVar);
    }

    public void addDefaultLonVariable() {

    }

    public void addDefaultTimeVariable() {

    }

    int getDimensionCount() {
        if (outFile.getRootGroup().getDimensions() != null) {
            return outFile.getRootGroup().getDimensions().size();
        } else {
            return 0;
        }
    }

    List<Variable> getVariables() {
        return outFile.getRootGroup().getVariables();
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        // implement: transform product nodes into NetCDF-arrays
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
    }

    @Override
    public void flush() throws IOException {
//        outStream.flush();
    }

    @Override
    public void deleteOutput() throws IOException {
    }

    public NetcdfFileWriteable getOutFile() {
        return outFile;
    }

    public void writeCDL() throws IOException {
        outFile.create();
        outFile.writeCDL(new FileOutputStream(outputLocation), false);
    }

    public void write(String varName) throws IOException, InvalidRangeException {
        outFile.write(varName, new int[3], data);
    }

    public Group getRootGroup() {
        return outFile.getRootGroup();
    }

    @Override
    public void close() throws IOException {
        outFile.close();
    }

    public void addGlobalAttribute(String key, String value) {
        outFile.addGlobalAttribute(key, value);
    }

    public void setData(Array data) {
        this.data = data;
    }

}
