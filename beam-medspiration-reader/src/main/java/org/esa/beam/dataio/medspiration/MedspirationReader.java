package org.esa.beam.dataio.medspiration;

import org.esa.beam.dataio.netcdf.DataTypeWorkarounds;
import org.esa.beam.dataio.netcdf.NcAttributeMap;
import org.esa.beam.dataio.netcdf.NetcdfReader;
import org.esa.beam.dataio.netcdf.NetcdfReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;

import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MedspirationReader extends NetcdfReader {

    private static class MedspirationWorkarounds implements DataTypeWorkarounds {

        @Override
        public int getRasterDataType(String variableName, DataType dataType) {
            return ProductData.TYPE_UINT8;
        }

        @Override
        public boolean hasWorkaroud(String variableName, DataType dataType) {
            if (dataType == DataType.BYTE) {
                return true;
            }
            return false;
        }
    }

    private static final DataTypeWorkarounds typeWorkarounds = new MedspirationWorkarounds();
    
    protected MedspirationReader(MedspirationReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void handleVariable(Variable variable, Product product) {
        final int rank = variable.getRank();
        final int width = variable.getDimension(rank - 1).getLength();
        final int height = variable.getDimension(rank - 2).getLength();
        final NcAttributeMap attMap = NcAttributeMap.create(variable);
        final Band band = NetcdfReaderUtils.createBand(variable, attMap, typeWorkarounds, width, height);
        
        final IndexCoding indexCoding = createIndexCoding(band.getName() + "_coding", attMap);
        if (indexCoding != null) {
            product.getIndexCodingGroup().add(indexCoding);
            band.setSampleCoding(indexCoding);
        }
        final FlagCoding flagCoding = createFlagCoding(band.getName() + "_coding", attMap);
        if (flagCoding != null) {
            product.getFlagCodingGroup().add(flagCoding);
            band.setSampleCoding(flagCoding);
        }
        product.addBand(band);
    }

    private FlagCoding createFlagCoding(String codingName, NcAttributeMap attMap) {
        String comment = attMap.getStringValue("comment");
        List<MetadataAttribute> attributes;
        if (StringUtils.isNotNullAndNotEmpty(comment)) {
            attributes = getFlagAttributes(comment.trim(), ";");
        } else {
            String flagValues = attMap.getStringValue("flag_values");
            if (StringUtils.isNotNullAndNotEmpty(flagValues) && flagValues.startsWith("b0")) {
                if (flagValues.startsWith("b0,")) {
                    flagValues = flagValues.replace("b0,", "b0:");
                }
                attributes = getFlagAttributes(flagValues.trim(), ",");
            } else {
                return null;
            }
        }
        if (attributes.isEmpty()) {
            return null;
        }
        FlagCoding coding = new FlagCoding(codingName);
        String meanings = attMap.getStringValue("flag_meanings");
        if (StringUtils.isNotNullAndNotEmpty(meanings)) {
            String[] flagNames = meanings.split(" ");
            int numNames = Math.min(flagNames.length, attributes.size());
            for (int i = 0; i < numNames; i++) {
                attributes.get(i).setName(flagNames[i]);
            }
        }
        for (MetadataAttribute metadataAttribute : attributes) {
            coding.addAttribute(metadataAttribute);
        }
        return coding;
    }

    private IndexCoding createIndexCoding(String codingName, NcAttributeMap attMap) {
        String comment = attMap.getStringValue("comment");
        if (!StringUtils.isNotNullAndNotEmpty(comment)) {
            return null;
        }
        List<MetadataAttribute> attributes = getIndexAttributes(comment.trim());
        if (attributes.size() == 0) {
            return null;
        }
        IndexCoding coding = new IndexCoding(codingName);
        for (MetadataAttribute metadataAttribute : attributes) {
            coding.addAttribute(metadataAttribute);
        }
        return coding;
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

    static List<MetadataAttribute> getIndexAttributes(String comment) {
        if (comment.contains(";")) {
            String[] split = comment.split(";");
            return createAttributes(split, "(\\d+)\\s+(.+)", false);
        }
        return Collections.EMPTY_LIST;
    }
    
    private static List<MetadataAttribute> createAttributes(String[] split, String regex, boolean isFlag) {
        Pattern pattern = Pattern.compile(regex);
        List<MetadataAttribute> attributes= new ArrayList<MetadataAttribute>();
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
