/*
    $Id: ProductUtilities.java 1288 2007-11-06 13:53:25Z ralf $

    Copyright (c) 2006 Brockmann Consult. All rights reserved. Use is
    subject to license terms.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as
    published by the Free Software Foundation; either version 2 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the BEAM software; if not, download BEAM from
    http://www.brockmann-consult.de/beam/ and install it.
*/
package org.esa.beam.dataio.globcolour;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

import java.text.ParseException;

/**
 * The class <code>ProductUtilities</code> encapsulates several utility
 * methods specific for GlobColour products only.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class ProductUtilities {

    private static final String MPH = "MPH";

    private static final String[] DATE_TIME_PATTERNS = new String[]{
            // ISO 8601 standard
            "yyyyMMdd'T'HHmmss'Z'",
            "yyyyMMddHHmmss",
            "yyyyMMdd",
    };

    static boolean setFlagCodingsAndBitmaskDefs(final Product product) {
        final FlagCoding flagCoding = new FlagCoding("GLOBCOLOUR");

        for (Flags flag : Flags.values()) {
            flagCoding.addFlag(flag.name(), flag.getMask(), flag.getDescription());
        }

        boolean codingAdded = false;

        for (final String name : product.getBandNames()) {
            if (name.endsWith("flags")) {
                final Band band = product.getBand(name);

                if (band == null || band.isFloatingPointType() || band.getFlagCoding() != null) {
                    continue;
                }
                if (!product.getFlagCodingGroup().contains(flagCoding.getName())) {
                    product.getFlagCodingGroup().add(flagCoding);
                }

                band.setSampleCoding(flagCoding);
                addBitmaskDefinitions(product, name);

                codingAdded = true;
            }
        }

        return codingAdded;
    }

    static void addBitmaskDefinitions(final Product product, final String flagsBandName) {
        for (final Flags flag : Flags.values()) {
            final String name = new StringBuilder(flagsBandName.split("flags")[0]).append(flag.name()).toString();

            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            if (maskGroup.contains(name)) {
                continue;
            }

            final String expression = new StringBuilder(flagsBandName).append(".").append(flag.name()).toString();
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            Mask mask = Mask.BandMathsType.create(name, flag.getDescription(), width, height,
                    expression, flag.getColor(), flag.getTransparency());
            maskGroup.add(mask);
        }
    }

    static boolean addDiagnosticSitePin(Product product) {
        final MetadataElement pa = product.getMetadataRoot().getElement(MPH);
        if (pa == null || product.getGeoCoding() == null) {
            return false;
        }

        final MetadataAttribute siteIdAttr = pa.getAttribute(ProductAttributes.SITE_ID);
        final MetadataAttribute siteLonAttr = pa.getAttribute(ProductAttributes.SITE_LON);
        final MetadataAttribute siteLatAttr = pa.getAttribute(ProductAttributes.SITE_LAT);
        if (siteIdAttr == null || siteLonAttr == null || siteLatAttr == null) {
            return false;
        }

        final String siteId = siteIdAttr.getData().getElemString();
        final float siteLon = siteLonAttr.getData().getElemFloat();
        final float siteLat = siteLatAttr.getData().getElemFloat();

        final String pinName = new StringBuilder("SITE").append("_").append(siteId).toString();
        if (product.getPinGroup().contains(pinName)) {
            return false;
        }

        final String pinLabel = pa.getAttributeString(ProductAttributes.SITE_NAME, pinName);
        product.getPinGroup().add(new Placemark(pinName, pinLabel, "GlobColour diagnostic site",
                                          null, new GeoPos(siteLat, siteLon),
                                          PlacemarkSymbol.createDefaultPinSymbol(), product.getGeoCoding()));
        return true;
    }

    static boolean setQuicklookBandName(Product product) {
        if (product.getQuicklookBandName() == null) {
            for (String name : product.getBandNames()) {
                if (name.endsWith("mean") || name.endsWith("value")) {
                    product.setQuicklookBandName(name);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the UTC for a global time attribute.
     *
     * @param product      the product.
     * @param timeAttrName the name of the time attribute.
     *
     * @return the attribute value as UTC or {@code null} if the attribute vallue cannot be parsed.
     *
     * @see MetadataAttribute
     * @see ProductData.UTC
     */
    public static ProductData.UTC getTimeAttrValue(final Product product, final String timeAttrName) {
        final MetadataElement pa = product.getMetadataRoot().getElement(MPH);
        if (pa == null) {
            return null;
        }
        final MetadataAttribute timeAttr = pa.getAttribute(timeAttrName);
        if (timeAttr == null) {
            return null;
        }
        final ProductData timeAttrData = timeAttr.getData();
        if (timeAttrData == null) {
            return null;
        }

        final String timeString = timeAttrData.getElemString();
        if ("".equals(timeString.trim())) {
            return null;
        }

        ProductData.UTC utc = null;
        for (final String pattern : DATE_TIME_PATTERNS) {
            try {
                utc = ProductData.UTC.parse(timeString, pattern);
                break; // utc is never null here
            } catch (ParseException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return utc;
    }


    static boolean setStartTime(final Product product) {
        if (product.getStartTime() == null) {
            final ProductData.UTC utc = getTimeAttrValue(product, ProductAttributes.START_TIME);

            if (utc != null) {
                product.setStartTime(utc);
                return true;
            }
        }

        return false;
    }

    static boolean setEndTime(final Product product) {
        if (product.getEndTime() == null) {
            final ProductData.UTC utc = getTimeAttrValue(product, ProductAttributes.END_TIME);

            if (utc != null) {
                product.setEndTime(utc);
                return true;
            }
        }

        return false;
    }

    static boolean isDiagnosticDataSet(final Product product) {
        final MetadataElement pa = product.getMetadataRoot().getElement(MPH);

        if (pa == null) {
            return false;
        }

        final MetadataAttribute siteId = pa.getAttribute(ProductAttributes.SITE_ID);
        final MetadataAttribute siteLat = pa.getAttribute(ProductAttributes.SITE_LAT);
        final MetadataAttribute siteLon = pa.getAttribute(ProductAttributes.SITE_LON);

        return siteId != null && siteLat != null && siteLon != null;
    }

    static boolean extend(Product product) {
        boolean modified = false;

        if (setStartTime(product)) {
            modified = true;
        }
        if (setEndTime(product)) {
            modified = true;
        }
        if (setQuicklookBandName(product)) {
            modified = true;
        }
        if (setFlagCodingsAndBitmaskDefs(product)) {
            modified = true;
        }
        if (addDiagnosticSitePin(product)) {
            modified = true;
        }

        return modified;
    }

}
