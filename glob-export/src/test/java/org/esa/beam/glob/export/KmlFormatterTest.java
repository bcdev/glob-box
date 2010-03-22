package org.esa.beam.glob.export;

import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * User: Thomas Storm
 * Date: 22.03.2010
 * Time: 16:44:35
 */
public class KmlFormatterTest {

    @Test
    public void testFormatOutput() throws ParserConfigurationException, IOException, SAXException {
        StringBuilder sb = new StringBuilder();
        sb.append(KmlFormatter.createHeader());
        final ArrayList<KmlLayer> layerList = new ArrayList<KmlLayer>();

        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlLayer layer1 = new KmlLayer("folder1", layer, boundBox);
        KmlLayer layer2 = new KmlLayer("folder1_1", layer, boundBox);
        KmlLayer layer3 = new KmlLayer("layer1_2", layer, boundBox);
        KmlLayer layer4 = new KmlLayer("layer1_1_1", layer, boundBox);
        KmlLayer layer5 = new KmlLayer("layer1_1_2", layer, boundBox);
        KmlLayer layer6 = new KmlLayer("folder1_1_3", layer, boundBox);
        KmlLayer layer7 = new KmlLayer("layer1_1_3_1", layer, boundBox);
        KmlLayer layer8 = new KmlLayer("layer1_1_3_2", layer, boundBox);
        KmlLayer layer9 = new KmlLayer("layer2", layer, boundBox);
        KmlLayer layer10 = new KmlLayer("layer3", layer, boundBox);

        layer1.addChild(layer2);
        layer1.addChild(layer3);

        layer2.addChild(layer4);
        layer2.addChild(layer5);
        layer2.addChild(layer6);

        layer6.addChild(layer7);
        layer6.addChild(layer8);

        layerList.add(layer1);
        layerList.add(layer9);
        layerList.add(layer10);

        sb.append(KmlFormatter.createOverlays(layerList, false));

        sb.append(KmlFormatter.createFooter());

        final Document document = convertToDocument(sb.toString());
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(false);
        prettyFormat.setOmitEncoding(true);
        prettyFormat.setOmitDeclaration(true);
        prettyFormat.setTextMode(Format.TextMode.NORMALIZE);

        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        final String xml = xmlOutputter.outputString(new DOMBuilder().build(document));
        System.out.println(xml);

    }

    private Document convertToDocument(String xmlString) throws ParserConfigurationException, SAXException,
                                                                IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
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
