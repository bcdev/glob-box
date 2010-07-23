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

package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preliminary API. Do not use.
 *
 * @author Thomas Storm
 */

// TODO move into medspiration reader
public class MedspirationXmlTimeHandler {

    protected Map<PixelPos, ProductData.UTC[]> createPixelToDateMap(File file) throws ParseException, IOException {
        final Map<PixelPos, ProductData.UTC[]> map = new HashMap<PixelPos, ProductData.UTC[]>();
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document document = builder.parse(file);
            Element root = document.getDocumentElement();
            final NodeList nodes = root.getElementsByTagName("mdb_record").item(0).getChildNodes();
            List<Node> acquisitionNodes = new ArrayList<Node>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                final String nodeName = node.getNodeName();
                if (nodeName != null && nodeName.equals("acquisition")) {
                    acquisitionNodes.add(node);
                }
            }
            List<Node> parameterNodes = new ArrayList<Node>();
            for (Node node : acquisitionNodes) {
                final NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node inner = childNodes.item(i);
                    if (inner.getNodeName().equals("parameter")) {
                        parameterNodes.add(inner);
                    }
                }
            }
            List<Node> observationNodes = new ArrayList<Node>();
            for (Node node : parameterNodes) {
                final NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node inner = childNodes.item(i);
                    if (inner.getNodeName().equals("observation")) {
                        observationNodes.add(inner);
                    }
                }
            }
            for (Node node : observationNodes) {
                final NodeList childNodes = node.getChildNodes();
                float x = 0;
                float y = 0;
                ProductData.UTC date = null;
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    if (childNode.getNodeName().equals("box_abs_x")) {
                        x = Float.parseFloat(childNode.getTextContent());
                    } else if (childNode.getNodeName().equals("box_abs_y")) {
                        y = Float.parseFloat(childNode.getTextContent());
                    } else if (childNode.getNodeName().equals("date-time")) {
                        String dateString = childNode.getTextContent().replace("T", "_");
                        dateString = dateString.replace("Z", "");
                        try {
                            date = ProductData.UTC.parse(dateString, "yyyyMMdd_HHmmss");
                        } catch (ParseException e) {
                            Debug.trace(e.getMessage());
                            date = ProductData.UTC.parse("19900101_000000", "yyyyMMdd_HHmmss");
                        }
                    }
                }
                final PixelPos pos = new PixelPos(x, y);
                final ProductData.UTC[] oldValues = map.get(pos);
                if (map.containsKey(pos)) {
                    ProductData.UTC[] dates = new ProductData.UTC[oldValues.length + 1];
                    for (int i = 0; i < oldValues.length; i++) {
                        dates[i] = oldValues[i];
                    }
                    dates[oldValues.length] = date;
                    map.put(pos, dates);
                } else {
                    map.put(pos, new ProductData.UTC[]{date});
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return map;
    }

    public ProductData.UTC[] parseTimeFromFileName(String fileName) {
        return new ProductData.UTC[0];
    }
}
