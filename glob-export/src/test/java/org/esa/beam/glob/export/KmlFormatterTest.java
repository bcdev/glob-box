package org.esa.beam.glob.export;

import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;
import org.xml.sax.SAXException;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * User: Thomas Storm
 * Date: 22.03.2010
 * Time: 16:44:35
 */
public class KmlFormatterTest {

    @Test
    public void testFolderHierarchy() throws ParserConfigurationException, IOException, SAXException {
        String xml = buildXmlForFolderHierarchyTest();

        final Document document = convertToDocument(xml);

        final Element root = document.getRootElement();
        Namespace nameSpace = root.getNamespace();
        assertEquals("http://earth.google.com/kml/2.0", nameSpace.getURI());
        final List children = root.getChildren();
        assertEquals(1, children.size());

        final Element outerFolder = root.getChild("Folder", nameSpace);
        assertEquals(4, outerFolder.getChildren().size());
        final Element nameChild = outerFolder.getChild("name", nameSpace);
        assertEquals("root", nameChild.getValue());

        List overlays = outerFolder.getChildren("GroundOverlay", nameSpace);
        assertEquals(2, overlays.size());
        validateGroundOverlay(nameSpace, overlays.get(0), "layer2");
        validateGroundOverlay(nameSpace, overlays.get(1), "layer3");

        final Element folder1 = outerFolder.getChild("Folder", nameSpace);
        assertEquals("folder1", folder1.getChildText("name", nameSpace));
        overlays = folder1.getChildren("GroundOverlay", nameSpace);
        assertEquals(1, overlays.size());
        validateGroundOverlay(nameSpace, overlays.get(0), "layer1_2");

        List folders = folder1.getChildren("Folder", nameSpace);
        assertEquals(1, folders.size());

        final Element folder1_1 = (Element) folders.get(0);
        assertEquals("folder1_1", folder1_1.getChildText("name", nameSpace));

        overlays = folder1_1.getChildren("GroundOverlay", nameSpace);
        assertEquals(2, overlays.size());
        validateGroundOverlay(nameSpace, overlays.get(0), "layer1_1_1");
        validateGroundOverlay(nameSpace, overlays.get(1), "layer1_1_2");
    }

    @Test
    public void testDocument() throws IOException, SAXException, ParserConfigurationException {
        final String xml = buildXmlForDocumentTest();
        final Document document = convertToDocument(xml);
//        printDocument(document);

        assertNotNull(document);
        final Element root = document.getRootElement();
        Namespace nameSpace = root.getNamespace();
        assertEquals("http://earth.google.com/kml/2.0", nameSpace.getURI());

        final Element doc = root.getChild("Document", nameSpace);
        assertNotNull(doc);
        final Element overlay = doc.getChild("GroundOverlay", nameSpace);
        assertNotNull(overlay);
        validateGroundOverlay(nameSpace, overlay, "layer1");
    }

    @Test
    public void testPlacemarks() throws IOException, SAXException, ParserConfigurationException {
        final String xml = buildXmlForPlacemarksTest();
        final Document document = convertToDocument(xml);
        printDocument(document);

        assertNotNull(document);
        final Element root = document.getRootElement();
        assertNotNull(root);
        Namespace nameSpace = root.getNamespace();

        final Element doc = root.getChild("Document", nameSpace);
        assertNotNull(doc);

        final List placemarks = doc.getChildren("Placemark", nameSpace);
        assertEquals(4, placemarks.size());
        for (int i = 0; i < placemarks.size(); i++) {
            validatePlacemark((Element) placemarks.get(i), "placemark " + (i + 1), nameSpace);
        }
    }

    @Test
    public void testLegend() throws IOException, SAXException, ParserConfigurationException {
        final String xml = buildXmlForLegendTest();
        final Document document = convertToDocument(xml);
        printDocument(document);

        assertNotNull(document);
        final Element root = document.getRootElement();
        assertNotNull(root);
        Namespace nameSpace = root.getNamespace();

        final Element doc = root.getChild("Document", nameSpace);
        assertNotNull(doc);

        final Element screenOverlay = doc.getChild("ScreenOverlay", nameSpace);
        assertNotNull(screenOverlay);
        assertEquals("Legend", screenOverlay.getChildText("name", nameSpace));
        final Element icon = screenOverlay.getChild("Icon", nameSpace);
        assertNotNull(icon);
        assertEquals("Legend.png", icon.getChildText("href", nameSpace));
        assertNotNull(screenOverlay.getChild("overlayXY", nameSpace));
        assertNotNull(screenOverlay.getChild("screenXY", nameSpace));

    }

    private void validatePlacemark(Element placemark, String s, Namespace nameSpace) {
        assertEquals(s, placemark.getChildText("name", nameSpace));
        final Element point = placemark.getChild("Point", nameSpace);
        assertNotNull(point);
        assertNotNull(point.getChild("coordinates", nameSpace));
    }

    private void validateGroundOverlay(Namespace nameSpace, Object overlay, String name) {
        Element groundOverlay = (Element) overlay;
        assertEquals("GroundOverlay", groundOverlay.getName());
        assertEquals(3, groundOverlay.getChildren().size());
        assertEquals(name, groundOverlay.getChildText("name", nameSpace));
        assertEquals(name + ".png", groundOverlay.getChildText("Icon", nameSpace));
        final Element latLonBox = groundOverlay.getChild("LatLonBox", nameSpace);
        assertNotNull(latLonBox);
        assertEquals("70.0", latLonBox.getChildText("north", nameSpace));
        assertEquals("30.0", latLonBox.getChildText("south", nameSpace));
        assertEquals("20.0", latLonBox.getChildText("east", nameSpace));
        assertEquals("0.0", latLonBox.getChildText("west", nameSpace));
    }

    private String buildXmlForFolderHierarchyTest() {
        StringBuilder sb = new StringBuilder();
        sb.append(KmlFormatter.createHeader());
        final ArrayList<KmlLayer> layerList = new ArrayList<KmlLayer>();

        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlLayer folder1 = new KmlLayer("folder1", layer, boundBox);
        KmlLayer folder1_1 = new KmlLayer("folder1_1", layer, boundBox);
        KmlLayer layer1_2 = new KmlLayer("layer1_2", layer, boundBox);
        KmlLayer layer1_1_1 = new KmlLayer("layer1_1_1", layer, boundBox);
        KmlLayer layer1_1_2 = new KmlLayer("layer1_1_2", layer, boundBox);
        KmlLayer layer2 = new KmlLayer("layer2", layer, boundBox);
        KmlLayer layer3 = new KmlLayer("layer3", layer, boundBox);

        folder1.addChild(folder1_1);
        folder1.addChild(layer1_2);

        folder1_1.addChild(layer1_1_1);
        folder1_1.addChild(layer1_1_2);

        layerList.add(folder1);
        layerList.add(layer2);
        layerList.add(layer3);

        sb.append(KmlFormatter.createOverlays(layerList, false));

        sb.append(KmlFormatter.createFooter());
        return sb.toString();
    }

    private String buildXmlForDocumentTest() {
        StringBuilder sb = new StringBuilder();
        sb.append(KmlFormatter.createHeader());
        final ArrayList<KmlLayer> layerList = new ArrayList<KmlLayer>();

        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlLayer layer1 = new KmlLayer("layer1", layer, boundBox);

        layerList.add(layer1);

        sb.append(KmlFormatter.createOverlays(layerList, false));

        sb.append(KmlFormatter.createFooter());
        return sb.toString();
    }

    private String buildXmlForLegendTest() {
        StringBuilder sb = new StringBuilder();
        sb.append(KmlFormatter.createHeader());
        sb.append(KmlFormatter.createLegend("Legend"));
        sb.append(KmlFormatter.createFooter());
        return sb.toString();
    }

    private String buildXmlForPlacemarksTest() {
        StringBuilder sb = new StringBuilder();
        sb.append(KmlFormatter.createHeader());
        List<KmlPlacemark> placemarks = new ArrayList<KmlPlacemark>();
        placemarks.add(new KmlPlacemark("placemark 1", new Point2D.Double(80.0, 89.0)));
        placemarks.add(new KmlPlacemark("placemark 2", new Point2D.Double(70.0, 70.0)));
        placemarks.add(new KmlPlacemark("placemark 3", new Point2D.Double(60.0, 60.0)));
        placemarks.add(new KmlPlacemark("placemark 4", new Point2D.Double(40.0, 50.0)));

        sb.append(KmlFormatter.createPlacemarks(placemarks));
        sb.append(KmlFormatter.createFooter());
        return sb.toString();
    }

    private void printDocument(org.jdom.Document document) {
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(false);
        prettyFormat.setOmitEncoding(true);
        prettyFormat.setOmitDeclaration(true);
        prettyFormat.setTextMode(Format.TextMode.NORMALIZE);

        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        final String xml = xmlOutputter.outputString(document);
        System.out.println(xml);
    }

    private org.jdom.Document convertToDocument(String xmlString) throws ParserConfigurationException, SAXException,
                                                                         IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return new DOMBuilder().build(builder.parse(new ByteArrayInputStream(xmlString.getBytes())));
    }

    private static class DummyTestOpImage extends SourcelessOpImage {

        DummyTestOpImage(int width, int height) {
            super(ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_BYTE, width, height, width, height),
                  null,
                  ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE, width, height),
                  0, 0, width, height);
        }

        @Override
        protected void computeRect(PlanarImage[] sources, java.awt.image.WritableRaster dest,
                                   java.awt.Rectangle destRect) {
            double[] value = new double[1];
            for (int y = 0; y < destRect.height; y++) {
                for (int x = 0; x < destRect.width; x++) {
                    value[0] = x + y;
                    dest.setPixel(x, y, value);
                }
            }
        }
    }

}
