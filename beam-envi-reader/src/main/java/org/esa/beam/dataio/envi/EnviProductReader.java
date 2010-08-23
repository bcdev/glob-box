package org.esa.beam.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envi.Header.BeamProperties;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// @todo 2 tb/** use header offset information in file positioning
// @todo 2 tb/** evaluate file type information and react accordingly
// @todo 2 tb/** evaluate data type information and react accordingly

public class EnviProductReader extends AbstractProductReader {

    private HashMap<String, Long> bandStreamPositionMap = null;
    private ImageInputStream imageInputStream = null;
    private ZipFile productZip = null;

    public EnviProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public static File createEnviImageFile(File headerFile) {
        final String hdrName = headerFile.getName();
        final String imgName = hdrName.substring(0, hdrName.indexOf('.'));
        String bandName = imgName + ".img";
        File imgFile = new File(headerFile.getParent(), bandName);
        if (!imgFile.exists()) {
            bandName = imgName + ".bin";
            imgFile = new File(headerFile.getParent(), bandName);
        }
        return imgFile;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        final File headerFile = EnviProductReaderPlugIn.getInputFile(inputObject);

        final BufferedReader headerReader = getHeaderReader(headerFile);

        final String headerFileName = headerFile.getName();
        String[] splittedHeaderFileName = headerFileName.split("!");
        String productName = splittedHeaderFileName.length > 1 ? splittedHeaderFileName[1] : splittedHeaderFileName[0];
        productName = productName.substring(0, productName.indexOf('.'));

        try {
            final Header header;
            synchronized (headerReader) {
                header = new Header(headerReader);
            }

            final Product product = new Product(productName, header.getSensorType(), header.getNumSamples(),
                                                header.getNumLines());
            product.setProductReader(this);
            product.setFileLocation(headerFile);
            product.setDescription(header.getDescription());

            initGeocoding(product, header);
            initBands(product, header);

            applyBeamProperties(product, header.getBeamProperties());

            // imageInputStream must be initialized last
            initializeInputStreamForBandData(headerFile, header);

            return product;
        } finally {
            if (headerReader != null) {
                headerReader.close();
            }
        }
    }


    @Override
    protected void readBandRasterDataImpl(final int sourceOffsetX, final int sourceOffsetY,
                                          final int sourceWidth, final int sourceHeight,
                                          final int sourceStepX, final int sourceStepY,
                                          final Band destBand,
                                          final int destOffsetX, final int destOffsetY,
                                          final int destWidth, final int destHeight,
                                          final ProductData destBuffer,
                                          final ProgressMonitor pm) throws IOException {

        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = sourceOffsetX + sourceWidth - 1;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;

        final int sourceRasterWidth = destBand.getProduct().getSceneRasterWidth();
        final long bandOffset = bandStreamPositionMap.get(destBand.getName());

        final int elemSize = destBuffer.getElemSize();
        int destPos = 0;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
        // For each scan in the data source
        try {
            for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                final int sourcePosY = sourceY * sourceRasterWidth;
                synchronized (imageInputStream) {
                    if (sourceStepX == 1) {
                        imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceMinX));
                        destBuffer.readFrom(destPos, destWidth, imageInputStream);
                        destPos += destWidth;
                    } else {
                        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                            imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                            destBuffer.readFrom(destPos, 1, imageInputStream);
                            destPos++;
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void close() throws IOException {
        if (imageInputStream != null) {
            imageInputStream.close();
        }
        if (productZip != null) {
            productZip.close();
            productZip = null;
        }
        super.close();
    }

    public TreeNode<File> getProductComponents() {
        try {
            final File headerFile = EnviProductReaderPlugIn.getInputFile(getInput());
            File parentDir = headerFile.getParentFile();
            final TreeNode<File> root = new TreeNode<File>(parentDir.getCanonicalPath());
            root.setContent(parentDir);

            final TreeNode<File> header = new TreeNode<File>(headerFile.getName());
            header.setContent(headerFile);
            root.addChild(header);

            if (productZip == null) {
                final File imageFile = createEnviImageFile(headerFile);
                final TreeNode<File> image = new TreeNode<File>(imageFile.getName());
                image.setContent(imageFile);
                root.addChild(image);
            }

            return root;

        } catch (IOException e) {
            return null;
        }
    }

    private void initializeInputStreamForBandData(File headerFile, Header header) throws IOException {
        if (EnviProductReaderPlugIn.isCompressedFile(headerFile)) {
            imageInputStream = createImageStreamFromZip(headerFile);
        } else {
            imageInputStream = createImageStreamFromFile(headerFile);
        }
        imageInputStream.setByteOrder(header.getJavaByteOrder());
    }

    private static void applyBeamProperties(Product product, BeamProperties beamProperties) throws IOException {
        if (beamProperties == null) {
            return;
        }
        final String sensingStart = beamProperties.getSensingStart();
        if (sensingStart != null) {
            try {
                product.setStartTime(ProductData.UTC.parse(sensingStart));
            } catch (ParseException e) {
                final String message = e.getMessage() + " at property sensingStart in the header file.";
                throw new IOException(message, e);
            }
        }

        final String sensingStop = beamProperties.getSensingStop();
        if (sensingStop != null) {
            try {
                product.setEndTime(ProductData.UTC.parse(sensingStop));
            } catch (ParseException e) {
                final String message = e.getMessage() + " at property sensingStop in the header file.";
                throw new IOException(message, e);
            }
        }
    }

    private static ImageInputStream createImageStreamFromZip(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        ZipFile productZip;
        String innerHdrZipPath;
        if (filePath.contains("!")) {
            // headerFile is in zip
            String[] splittedHeaderFile = filePath.split("!");
            innerHdrZipPath = splittedHeaderFile[1].replace("\\", "/");
            productZip = new ZipFile(new File(splittedHeaderFile[0]));
        } else {
            productZip = new ZipFile(file, ZipFile.OPEN_READ);
            innerHdrZipPath = file.getName();
        }


        try {
            String innerImgZipPath = innerHdrZipPath.substring(0, innerHdrZipPath.length() -4) + ".img";
            InputStream inputStream = productZip.getInputStream(productZip.getEntry(innerImgZipPath));
            return new FileCacheImageInputStream(inputStream, null);
        } catch (IOException e) {
            try {
                productZip.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    private static ImageInputStream createImageStreamFromFile(final File headerFile) throws IOException {
        final File imageFile = createEnviImageFile(headerFile);

        if (!imageFile.exists()) {
            throw new FileNotFoundException("file not found: <" + imageFile + ">");
        }
        return new FileImageInputStream(imageFile);
    }

    private static void initGeocoding(final Product product, final Header header) throws IOException {
        final EnviMapInfo enviMapInfo = header.getMapInfo();
        if (enviMapInfo == null) {
            return;
        }
        final EnviProjectionInfo projectionInfo = header.getProjectionInfo();
        if (projectionInfo != null) {

            final MapTransform transform = EnviMapTransformFactory.create(projectionInfo.getProjectionNumber(),
                                                                          projectionInfo.getParameter());
            final MapProjection projection = new MapProjection(transform.getDescriptor().getName(), transform);
            final MapInfo mapInfo = new MapInfo(projection,
                                                (float) enviMapInfo.getReferencePixelX(),
                                                (float) enviMapInfo.getReferencePixelY(),
                                                (float) enviMapInfo.getEasting(),
                                                (float) enviMapInfo.getNorthing(),
                                                (float) enviMapInfo.getPixelSizeX(),
                                                (float) enviMapInfo.getPixelSizeY(),
                                                Datum.WGS_84
            );
            mapInfo.setSceneWidth(product.getSceneRasterWidth());
            mapInfo.setSceneHeight(product.getSceneRasterHeight());
            MapGeoCoding mapGeoCoding = new MapGeoCoding(mapInfo);
            product.setGeoCoding(mapGeoCoding);
            return;
        }

        try {
            GeoCoding geoCoding = null;
            if (EnviConstants.PROJECTION_NAME_WGS84.equalsIgnoreCase(enviMapInfo.getProjectionName())) {
                geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                             product.getSceneRasterWidth(),
                                             product.getSceneRasterHeight(),
                                             enviMapInfo.getEasting(),
                                             enviMapInfo.getNorthing(),
                                             enviMapInfo.getPixelSizeX(),
                                             enviMapInfo.getPixelSizeY(),
                                             enviMapInfo.getReferencePixelX() - 1,
                                             enviMapInfo.getReferencePixelY() - 1);
            }
            product.setGeoCoding(geoCoding);
        } catch (FactoryException ignore) {
        } catch (TransformException ignore) {
        }


    }

    /**
     * Creates a buffered reader that is opened on the *.hdr file to read the header information.
     * This method works for both compressed and uncompressed ENVI files.
     *
     * @param headerFile the input file
     *
     * @return a reader on the header file
     *
     * @throws IOException on disk IO failures
     */
    private BufferedReader getHeaderReader(File headerFile) throws IOException {
        if (EnviProductReaderPlugIn.isCompressedFile(headerFile)) {
            String[] splittedHeaderFile = headerFile.getAbsolutePath().split("!");
            ZipFile zipFile = new ZipFile(new File(splittedHeaderFile[0]));
            final String innerZipPath = splittedHeaderFile[1].replace("\\", "/");
            InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(innerZipPath));
            return new BufferedReader(new InputStreamReader(inputStream));
        } else {
            return new BufferedReader(new FileReader(headerFile));
        }
    }

    private void initBands(Product product, Header header) {
        final int enviDataType = header.getDataType();
        final int dataType = DataTypeUtils.toBeam(enviDataType);
        final int sizeInBytes = DataTypeUtils.getSizeInBytes(enviDataType);
        final int bandSizeInBytes = header.getNumSamples() * header.getNumLines() * sizeInBytes;

        bandStreamPositionMap = new HashMap<String, Long>();
        final int headerOffset = header.getHeaderOffset();

        final String[] bandNames = getBandNames(header);
        for (int i = 0; i < bandNames.length; i++) {
            final String originalBandName = bandNames[i];
            final String validBandName;
            final String description;
            if (Product.isValidNodeName(originalBandName)) {
                validBandName = originalBandName;
                description = "";
            } else {
                validBandName = createValidNodeName(originalBandName);
                description = "non formatted band name: " + originalBandName;
            }
            final Band band = new Band(validBandName,
                                       dataType,
                                       product.getSceneRasterWidth(),
                                       product.getSceneRasterHeight());
            band.setDescription(description);
            product.addBand(band);

            long bandStartPosition = headerOffset + bandSizeInBytes * i;
            bandStreamPositionMap.put(validBandName, bandStartPosition);
        }
    }

    protected static String[] getBandNames(final Header header) {
        final String[] bandNames = header.getBandNames();
        // there must be at least 1 bandname because in DIMAP-Files are no bandnames given.
        if (bandNames == null || bandNames.length == 0) {
            return new String[]{"Band"};
        } else {
            return bandNames;
        }
    }

    private static String createValidNodeName(final String originalBandName) {
        String name = StringUtils.createValidName(originalBandName, null, '_');
        while (name.startsWith("_")) {
            name = name.substring(1);
        }
        while (name.endsWith("_")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }
}