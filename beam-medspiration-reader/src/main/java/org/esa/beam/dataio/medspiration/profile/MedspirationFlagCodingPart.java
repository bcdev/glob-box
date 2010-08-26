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

package org.esa.beam.dataio.medspiration.profile;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Zühlke
 * @author Thomas Storm
 */
public class MedspirationFlagCodingPart extends ProfilePart {

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        // we solely read here; nothing to define
    }

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Variable[] variables = ctx.getRasterDigest().getRasterVariables();
        for (Variable variable : variables) {
            List<MetadataAttribute> attributes;

            Attribute commentAttribute = variable.findAttribute("comment");
            if (commentAttribute != null && !commentAttribute.getStringValue().isEmpty()) {
                attributes = getFlagAttributes(commentAttribute.getStringValue().trim(), ";");
            } else {
                Attribute flagAttribute = variable.findAttribute("flag_values");
                if (flagAttribute != null && flagAttribute.getStringValue().startsWith("b0")) {
                    String flagValues = flagAttribute.getStringValue();
                    if (flagValues.startsWith("b0,")) {
                        flagValues = flagValues.replace("b0,", "b0:");
                    }
                    attributes = getFlagAttributes(flagValues.trim(), ",");
                } else {
                    continue;
                }
            }

            if (attributes.isEmpty()) {
                continue;
            }

            FlagCoding flagCoding = new FlagCoding(variable.getName() + "_coding");
            Attribute meaningsAttribute = variable.findAttribute("flag_meanings");
            if (meaningsAttribute != null && !meaningsAttribute.getStringValue().isEmpty()) {
                String meanings = meaningsAttribute.getStringValue();
                String[] flagNames = meanings.split(" ");
                int numNames = Math.min(flagNames.length, attributes.size());
                for (int i = 0; i < numNames; i++) {
                    attributes.get(i).setName(flagNames[i]);
                }
            }
            for (MetadataAttribute metadataAttribute : attributes) {
                flagCoding.addAttribute(metadataAttribute);
            }
            p.getFlagCodingGroup().add(flagCoding);
            p.getBand(variable.getName()).setSampleCoding(flagCoding);
        }
    }

    static List<MetadataAttribute> getFlagAttributes(String comment, String separator) {
        String[] split;
        if (comment.contains(separator)) {
            split = comment.split(separator);
        } else {
            split = comment.split("(?=b\\d\\d?)");
        }
        return createAttributes(split, "b(\\d\\d?)\\s?:\\s?1\\s?=\\s?(.+)", true);
    }

    private static List<MetadataAttribute> createAttributes(String[] split, String regex, boolean isFlag) {
        Pattern pattern = Pattern.compile(regex);
        List<MetadataAttribute> attributes = new ArrayList<MetadataAttribute>();
        for (String entry : split) {
            Matcher matcher = pattern.matcher(entry.trim());
            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                String description = matcher.group(2).trim();
                if (isFlag) {
                    value = (int) Math.pow(2, value);
                }
                String name = description.replaceAll("[^a-zA-Z0-9_]", "_");
                MetadataAttribute attribute = new MetadataAttribute(name, ProductData.TYPE_INT32);
                attribute.setDataElems(new int[]{value});
                attribute.setDescription(description);
                attributes.add(attribute);
            }
        }
        return attributes;
    }


}
