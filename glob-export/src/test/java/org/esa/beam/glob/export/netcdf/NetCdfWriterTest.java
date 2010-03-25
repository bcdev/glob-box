package org.esa.beam.glob.export.netcdf;

import org.esa.beam.dataio.netcdf.NetcdfConstants;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 25.03.2010
 * Time: 08:39:26
 */
public class NetCdfWriterTest implements NetcdfConstants {

    private NetCdfWriter writer;
    private static final String OUTPUT_FILE = "C:\\dev\\Ressourcen\\NetCDF\\netcdfTest.nc";
    private Group rootGroup;
    private Dimension lat;
    private Dimension lon;
    private Dimension time;

    @Before
    public void setUp() {
        writer = (NetCdfWriter) new NetCdfWriterPlugIn(OUTPUT_FILE).createWriterInstance();
        rootGroup = writer.getRootGroup();

        lat = new Dimension(LAT_VAR_NAME, 180);
        lon = new Dimension(LON_VAR_NAME, 360);
        time = new Dimension("time", 0, true, true, false);
    }

    @Test
    public void testCreateHeaderAndData() throws IOException, InvalidRangeException {
        assertEquals(0, writer.getDimensionCount());

        lat.setGroup(rootGroup);
        lon.setGroup(rootGroup);
        time.setGroup(rootGroup);

        writer.addDimension(lat);
        writer.addDimension(lon);
        writer.addUnlimitedDimension(time);

        assertEquals(3, writer.getDimensionCount());

        final List<Variable> variableList = writer.getVariables();
        assertEquals(0, variableList.size());

        Attribute longNameLat = new Attribute("long_name", LATITUDE_VAR_NAME);
        Attribute standardNameLat = new Attribute("standard_name", LATITUDE_VAR_NAME);
        Attribute unitsLat = new Attribute("units", "degrees_north");
        Attribute axisLat = new Attribute("axis", "Y");

        Attribute longNameLon = new Attribute("long_name", LONGITUDE_VAR_NAME);
        Attribute standardNameLon = new Attribute("standard_name", LONGITUDE_VAR_NAME);
        Attribute unitsLon = new Attribute("units", "degrees_east");
        Attribute axisLon = new Attribute("axis", "X");

        Attribute longNameTime = new Attribute("long_name", "the time coordinate");
        Attribute standardNameTime = new Attribute("standard_name", "time");
        Attribute unitsTime = new Attribute("units", "days since 1990-1-1");
        Attribute axisTime = new Attribute("axis", "T");

        Attribute longNameTsm = new Attribute("long_name", "Total suspended matter");
        Attribute standardNameTsm = new Attribute("standard_name",
                                                  "mass_concentration_of_suspended_matter_in_sea_water");
        Attribute unitsTsm = new Attribute("units", "kg/m3");
        Attribute missingValueTsm = new Attribute("missing_value", "NaN");

        final NetcdfFileWriteable outFile = writer.getOutFile();
        final Group rootGroup = outFile.getRootGroup();

        Variable latVar = new Variable(outFile, rootGroup,
                                       null, LATITUDE_VAR_NAME, DataType.INT, LAT_VAR_NAME);
        latVar.addAttribute(longNameLat);
        latVar.addAttribute(standardNameLat);
        latVar.addAttribute(unitsLat);
        latVar.addAttribute(axisLat);
        writer.addVariable(latVar);

        Variable lonVar = new Variable(outFile, rootGroup,
                                       null, LONGITUDE_VAR_NAME, DataType.INT, LON_VAR_NAME);
        lonVar.addAttribute(longNameLon);
        lonVar.addAttribute(standardNameLon);
        lonVar.addAttribute(unitsLon);
        lonVar.addAttribute(axisLon);
        writer.addVariable(lonVar);

        Variable timeVar = new Variable(outFile, rootGroup,
                                        null, "time", DataType.INT, "time");
        timeVar.addAttribute(longNameTime);
        timeVar.addAttribute(standardNameTime);
        timeVar.addAttribute(unitsTime);
        timeVar.addAttribute(axisTime);
        writer.addVariable(timeVar);

        Variable tsmVar = new Variable(outFile, rootGroup,
                                       null, "tsm", DataType.DOUBLE, LAT_VAR_NAME + " " + LON_VAR_NAME + " time");
        tsmVar.addAttribute(longNameTsm);
        tsmVar.addAttribute(standardNameTsm);
        tsmVar.addAttribute(unitsTsm);
        tsmVar.addAttribute(missingValueTsm);
        writer.addVariable(tsmVar);

        writer.addGlobalAttribute("Conventions", "CF-1.0");

        assertEquals(4, variableList.size());

        assertEquals(4, variableList.get(0).getAttributes().size());
        assertEquals(4, variableList.get(1).getAttributes().size());
        assertEquals(4, variableList.get(2).getAttributes().size());
//    }
//
//    @Test
//    public void testCreateData() throws IOException, InvalidRangeException {
        ArrayDouble data = new ArrayDouble.D3(lat.getLength(), lon.getLength(), time.getLength());
        int la, lo, t;
        Index ima = data.getIndex();
        for (lo = 0; lo < lon.getLength(); lo++) {
            for (la = 0; la < lat.getLength(); la++) {
                for (t = 0; t < time.getLength(); t++) {
                    data.setDouble(ima.set(la, lo, 0), la * 0.1 + lo * 0.1);
                }
            }
        }

        int[] origin = new int[3];

        writer.writeCDL();
        writer.write("tsm", origin, data);
        writer.close();
    }

    @Test
    public void testReadData() throws IOException {
        NetcdfFile testFile = NetcdfFile.open(OUTPUT_FILE);
        final Group rootGroup = testFile.getRootGroup();
        assertEquals(4, rootGroup.getVariables().size());

        assertEquals(LATITUDE_VAR_NAME, rootGroup.getVariables().get(0).getName());
        assertEquals(LONGITUDE_VAR_NAME, rootGroup.getVariables().get(1).getName());
        assertEquals("time", rootGroup.getVariables().get(2).getName());
    }

}
