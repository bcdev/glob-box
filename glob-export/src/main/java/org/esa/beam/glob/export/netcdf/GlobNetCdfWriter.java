package org.esa.beam.glob.export.netcdf;

import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * User: Thomas Storm
 * Date: 25.03.2010
 * Time: 16:36:10
 */
public class GlobNetCdfWriter implements NetCdfConstants {

    private NetCdfWriter writer;
    private static final String OUTPUT_FILE = System.getProperty("java.io.tmpdir") + System.getProperty(
            "file.separator") + "netcdfTest.nc";
    private Group rootGroup;
    private Dimension lat;
    private Dimension lon;
    private Dimension time;
    private ArrayDouble data;

    public GlobNetCdfWriter() {
        writer = (NetCdfWriter) new NetCdfWriterPlugIn(OUTPUT_FILE).createWriterInstance();
        rootGroup = writer.getRootGroup();

        lat = new Dimension(LAT_VAR_NAME, 180);
        lon = new Dimension(LON_VAR_NAME, 360);
        time = new Dimension(TIME_VAR_NAME, 0, true, true, false);
    }

    public void createHeader() {
        lat.setGroup(rootGroup);
        lon.setGroup(rootGroup);
        time.setGroup(rootGroup);

        writer.addDimension(lat);
        writer.addDimension(lon);
        writer.addUnlimitedDimension(time);

        Attribute longNameLon = new Attribute(LONG_NAME, LONGITUDE);
        Attribute standardNameLon = new Attribute(STANDARD_NAME, LONGITUDE);
        Attribute unitsLon = new Attribute(UNITS, DEGREES_EAST);
        Attribute axisLon = new Attribute(AXIS, "X");

        Attribute longNameTime = new Attribute(LONG_NAME, TIME);
        Attribute standardNameTime = new Attribute(STANDARD_NAME, TIME);
        Attribute unitsTime = new Attribute(UNITS, "days since 1990-1-1");
        Attribute axisTime = new Attribute(AXIS, "T");

        Attribute longNameTsm = new Attribute(LONG_NAME, "Total suspended matter");
        Attribute standardNameTsm = new Attribute(STANDARD_NAME,
                                                  "mass_concentration_of_suspended_matter_in_sea_water");
        Attribute unitsTsm = new Attribute(UNITS, "kg/m3");
        Attribute missingValueTsm = new Attribute(MISSING_VALUE, NOT_A_NUMBER);

        final NetcdfFileWriteable outFile = writer.getOutFile();
        final Group rootGroup = outFile.getRootGroup();

        Variable lonVar = new Variable(outFile, rootGroup,
                                       null, LON_VAR_NAME, DataType.INT, LON_VAR_NAME);
        lonVar.addAttribute(longNameLon);
        lonVar.addAttribute(standardNameLon);
        lonVar.addAttribute(unitsLon);
        lonVar.addAttribute(axisLon);
        writer.addVariable(lonVar);

        Variable timeVar = new Variable(outFile, rootGroup,
                                        null, TIME_VAR_NAME, DataType.INT, TIME_VAR_NAME);
        timeVar.addAttribute(longNameTime);
        timeVar.addAttribute(standardNameTime);
        timeVar.addAttribute(unitsTime);
        timeVar.addAttribute(axisTime);
        writer.addVariable(timeVar);

        Variable tsmVar = new Variable(outFile, rootGroup,
                                       null, "tsm", DataType.DOUBLE,
                                       LAT_VAR_NAME + " " + LON_VAR_NAME + " " + TIME_VAR_NAME);
        tsmVar.addAttribute(longNameTsm);
        tsmVar.addAttribute(standardNameTsm);
        tsmVar.addAttribute(unitsTsm);
        tsmVar.addAttribute(missingValueTsm);
        writer.addVariable(tsmVar);

        writer.addGlobalAttribute("Conventions", "CF-1.0");
    }

    public void createData() {
        data = new ArrayDouble.D3(lat.getLength(), lon.getLength(), time.getLength());
        int la, lo, t;
        Index ima = data.getIndex();
        for (lo = 0; lo < lon.getLength(); lo++) {
            for (la = 0; la < lat.getLength(); la++) {
                for (t = 0; t < time.getLength(); t++) {
                    data.setDouble(ima.set(la, lo, t), la * 0.1 + lo * 0.1);
                }
            }
        }
    }

    public void write() throws IOException, InvalidRangeException {
        writer.setData(data);
        writer.writeCDL();
        writer.write("tsm");
        writer.close();
    }
}
