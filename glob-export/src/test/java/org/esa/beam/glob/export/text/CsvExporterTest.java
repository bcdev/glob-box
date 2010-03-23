package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 23.03.2010
 * Time: 14:36:48
 */
public class CsvExporterTest {

    @Test
    public void testCsvExporterInit() {
        Band band1 = new Band("TSM_mean", ProductData.TYPE_INT32, 0, 0);
        Band band2 = new Band("TSM_max", ProductData.TYPE_INT32, 10, 10);
        Band band3 = new Band("TSM_min", ProductData.TYPE_INT32, 20, 20);
        Band band4 = new Band("TSM_median", ProductData.TYPE_INT32, 30, 30);
        List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        rasterList.add(band1);
        rasterList.add(band2);
        rasterList.add(band3);
        rasterList.add(band4);

        File file = new File(System.getProperty("java.io.tmpdir"));

        final CsvExporter exporter = new CsvExporter(rasterList, file, 2);
        final List<RasterDataNode> dataNodes = exporter.getRasterList();
        assertNotNull(dataNodes);
        assertEquals(4, dataNodes.size());
    }

}
