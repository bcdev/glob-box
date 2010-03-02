package org.esa.glob.reader.worldfire;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.jai.PlacemarkMaskOpImage;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.Debug;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// TODO - TBD: use the optional gif-file?
// TODO - read from zip files
// TODO - consider reading zips in zip files (e.g. annual time series)

// TODO - consider changeable filename convention
/**
 * @author Marco Peters
 * @since GlobToolbox 2.0
 */
class WorldFireReader extends AbstractProductReader {

    static final String PRODUCT_TYPE_AATSR_ALGO1 = "AATSR_ALGO1";
    static final String PRODUCT_TYPE_AATSR_ALGO2 = "AATSR_ALGO2";
    static final String PRODUCT_TYPE_ATSR2_ALGO1 = "ATSR2_ALGO1";
    static final String PRODUCT_TYPE_ATSR2_ALGO2 = "ATSR2_ALGO2";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected WorldFireReader(WorldFireReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = new File(getInput().toString());
        final String productName = FileUtils.getFilenameWithoutExtension(inputFile);
        final String productType = getProductType(inputFile);
        final Product product = new Product(productName, productType, 3600, 1800);
        product.setFileLocation(inputFile);
        final AffineTransform i2m = new AffineTransform();
        i2m.translate(0, 0);
        i2m.scale(0.1, -0.1);
        i2m.translate(-product.getSceneRasterWidth() / 2, -product.getSceneRasterHeight() / 2);
        try {
            final Rectangle rectangle = new Rectangle(0, 0,
                                                      product.getSceneRasterWidth(),
                                                      product.getSceneRasterHeight());
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, rectangle, i2m));
        } catch (Exception e) {
            Debug.trace("Could not create GeoCoding");
            e.printStackTrace();
        }
        // todo - pin layer needs speed improvements; with more than 6000 pins it is slow 
        final ProductNodeGroup<Placemark> pinGroup = product.getPinGroup();
        List<Placemark> fireList = getFireSpotList(inputFile, product.getGeoCoding());
        for (Placemark fireSpot : fireList) {
            pinGroup.add(fireSpot);
        }
        Band fireBand = product.addBand("fire_" + productName, ProductData.TYPE_UINT8);
        fireBand.setNoDataValue(0);
        fireBand.setNoDataValueUsed(true);
        // todo - define final IndexCoding
        final IndexCoding indexCoding = new IndexCoding("Fire");
        indexCoding.addIndex("no-data", 0, "No data");
        indexCoding.addIndex("fire", 255, "Fire detected");
        product.getIndexCodingGroup().add(indexCoding);
        fireBand.setSampleCoding(indexCoding);
        final MultiLevelImage fireImage = createFireImage(product);
        fireBand.setSourceImage(fireImage);
        return product;
    }

    private MultiLevelImage createFireImage(final Product product) {
        final PlacemarkMaskOpImage opImage = new PlacemarkMaskOpImage(product,
                                                                      PinDescriptor.INSTANCE, 1,
                                                                      product.getSceneRasterWidth(),
                                                                      product.getSceneRasterHeight(),
                                                                      new ResolutionLevel(0, 1.0));
        final MultiLevelSource multiLevelSource = new DefaultMultiLevelSource(opImage, 4);

        return new DefaultMultiLevelImage(multiLevelSource);
    }


    private List<Placemark> getFireSpotList(File inputFile, GeoCoding geoCoding) throws IOException {
        final ArrayList<Placemark> fireList = new ArrayList<Placemark>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
        try {
            String line = bufferedReader.readLine();
            int index = 0;
            while (line != null && !line.isEmpty()) {
                final Placemark fireSpot = parseLine(line, index, geoCoding);
                if (fireSpot != null) {
                    fireList.add(fireSpot);
                }
                line = bufferedReader.readLine();
                index++;
            }
        } finally {
            bufferedReader.close();
        }

        return fireList;
    }


    String getProductType(File inputFile) {
        final String filename = FileUtils.getFilenameWithoutExtension(inputFile);
        if (filename.toUpperCase().contains("ESA")) { // ATSR2
            if (filename.endsWith("01")) { // ALGO1
                return PRODUCT_TYPE_ATSR2_ALGO1;
            } else {
                return PRODUCT_TYPE_ATSR2_ALGO2;
            }
        } else {                                      // AATSR
            if (filename.endsWith("ALGO1")) { // ALGO1
                return PRODUCT_TYPE_AATSR_ALGO1;
            } else {
                return PRODUCT_TYPE_AATSR_ALGO2;
            }
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
    }


    @Override
    public WorldFireReaderPlugIn getReaderPlugIn() {
        return (WorldFireReaderPlugIn) super.getReaderPlugIn();
    }

    @Override
    public TreeNode<File> getProductComponents() {
        final File inputFile = new File(getInput().toString());
        final File parent = inputFile.getParentFile();
        final TreeNode<File> result = new TreeNode<File>(parent.getName());
        result.setContent(parent);

        final TreeNode<File> productFile = new TreeNode<File>(inputFile.getName());
        productFile.setContent(inputFile);
        result.addChild(productFile);

        return result;
    }


    private static Placemark parseLine(String text, int index, GeoCoding geoCoding) {
        final String[] columns = text.trim().split("[\\s]++");
        if (columns.length == 5) { // AATSR
            final String dateColumn = columns[0];
            final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Date date;
            try {
                final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
                date = dateFormat.parse(dateColumn);
            } catch (ParseException e) {
                Debug.trace(e);
                return null;
            }
            calendar.setTimeInMillis(date.getTime());
            String timeColumn = columns[2];
            try {
                final DateFormat timeFormat = new SimpleDateFormat("HHmmss.SSS", Locale.ENGLISH);
                if (timeColumn.length() < 10) {
                    timeColumn = "0" + timeColumn;
                }
                date = timeFormat.parse(timeColumn);
            } catch (ParseException e) {
                Debug.trace(e);
                return null;
            }
            calendar.roll(Calendar.MILLISECOND, (int) date.getTime());
            final float lat = Float.parseFloat(columns[3]);
            final float lon = Float.parseFloat(columns[4]);
            final String name = "Fire_" + index;
            final GeoPos geoPos = new GeoPos(lat, lon);
            return new Placemark(name, String.format("%1$tF", calendar), "Fire", null, geoPos, PinDescriptor.INSTANCE.createDefaultSymbol(),
                           geoCoding);
        } else if (columns.length == 6) { // ATSR2
            // todo - implement
            return null;//new FireSpot();
        }
        return null;
    }

}
