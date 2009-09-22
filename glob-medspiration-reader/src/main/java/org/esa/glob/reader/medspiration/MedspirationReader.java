package org.esa.glob.reader.medspiration;

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
    
//    static {
//        typeWorkarounds = new NetcdfDataTypeWorkarounds();
//        typeWorkarounds.addWorkaround("mask", DataType.BYTE, ProductData.TYPE_UINT8);
//        typeWorkarounds.addWorkaround("sea_ice_fraction", DataType.BYTE, ProductData.TYPE_UINT8);
//        typeWorkarounds.addWorkaround("wind_speed", DataType.BYTE, ProductData.TYPE_UINT8);
//    }
    
    protected MedspirationReader(MedspirationReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

//    @Override
//    protected Product readProductNodesImpl() throws IOException {
//        Product product = super.readProductNodesImpl();
//        
//        if (product.containsBand("mask")) {
//            FlagCoding maskFlagCoding = new FlagCoding("mask_coding");
//            maskFlagCoding.addFlag("sea", 1, "grid cell is open sea water");
//            maskFlagCoding.addFlag("land", 2, "land is present in this grid cell");
//            maskFlagCoding.addFlag("lake", 4, "lake surface is present in this grid cell");
//            maskFlagCoding.addFlag("ice", 8, "sea ice is present in this grid cell");
//            ProductNodeGroup<IndexCoding> indexCodingGroup = product.getIndexCodingGroup();
//            if (indexCodingGroup.contains("mask_coding")) {
//                IndexCoding indexCoding = indexCodingGroup.get("mask_coding");
//                indexCodingGroup.remove(indexCoding);
//            }
//            product.getFlagCodingGroup().add(maskFlagCoding);
//            product.getBand("mask").setSampleCoding(maskFlagCoding);
//        }
//        
//        if (product.containsBand("sources_of_wind_speed")) {
//            IndexCoding windSpeed = new IndexCoding("sources_of_wind_speed_coding");
//            windSpeed.addIndex("No-Data", 0, "No wind speed data available");
//            windSpeed.addIndex("AMSR-E", 1, "AMSR-E data");
//            windSpeed.addIndex("TMI", 2, "TMI data");
//            windSpeed.addIndex("NWP:ECMWF", 3, "NWP:ECMWF");
//            windSpeed.addIndex("NWP:Met Office", 4, "NWP:Met Office");
//            windSpeed.addIndex("NWP:NCEP", 5, "NWP:NCEP");
//            windSpeed.addIndex("Climatology", 6, "Reference climatology");
//            product.getIndexCodingGroup().add(windSpeed);
//            product.getBand("sources_of_wind_speed").setSampleCoding(windSpeed);
//        }
//        if (product.containsBand("sources_of_ssi")) {
//            IndexCoding ssi = new IndexCoding("sources_of_ssi_coding");
//            ssi.addIndex("No-Data", 0, "No SSI data available");
//            ssi.addIndex("MSG_SEVIRI", 1, "Data from MSG_SEVIRI");
//            ssi.addIndex("GOES_E", 2, "Data from GOES_E");
//            ssi.addIndex("GOES_W", 3, "Data from GOES_W");
//            ssi.addIndex("ECMWF", 4, "Data from ECMWF");
//            ssi.addIndex("NCEP", 5, "Data from NCEP");
//            ssi.addIndex("METOFFICE", 6, "Data from Met Office");
//            product.getIndexCodingGroup().add(ssi);
//            product.getBand("sources_of_ssi").setSampleCoding(ssi);
//        }
//        if (product.containsBand("sources_of_aod")) {
//            IndexCoding aod = new IndexCoding("sources_of_aod_coding");
//            aod.addIndex("No-Data", 0, "No AOD data available");
//            aod.addIndex("NESDIS", 1, "Data from NESDIS");
//            aod.addIndex("NAVOCEANO", 2, "Data from NAVOCEANO");
//            aod.addIndex("NAAPS", 3, "Data from NAAPS");
//            product.getIndexCodingGroup().add(aod);
//            product.getBand("sources_of_aod").setSampleCoding(aod);
//        }
//        if (product.containsBand("sources_of_sea_ice_fraction")) {
//            IndexCoding seaIce = new IndexCoding("sources_of_sea_ice_fraction_coding");
//            seaIce.addIndex("No-Data", 0, "No sea ice set");
//            seaIce.addIndex("NSIDC", 1, "Data from NSIDC SSM/I Cavialeri et al (1992)");
//            seaIce.addIndex("NAVOCEANO", 2, "Data from AMSR-E");
//            seaIce.addIndex("ECMWF", 3, "Data from ECMWF");
//            seaIce.addIndex("CMS", 4, "Data from CMS (France) cloud mask used by Medspiration");
//            product.getIndexCodingGroup().add(seaIce);
//            product.getBand("sources_of_sea_ice_fraction").setSampleCoding(seaIce);
//        } 
//        if (product.containsBand("rejection_flag")) {
//            FlagCoding rejection = new FlagCoding("rejection_flag_coding");
//            rejection.addFlag("out_of_range", 1, "SST out of range");
//            rejection.addFlag("cosmetic", 2, "Cosmetic value");
//            rejection.addFlag("ir_cloudy", 4, "IR cloudy");
//            rejection.addFlag("mw_rain", 8, "MW rain");
//            rejection.addFlag("ice", 16, "Ice");
//            rejection.addFlag("spare", 32, "Spare");
//            rejection.addFlag("land", 64, "Land");
//            rejection.addFlag("unprocessed", 128, "Unprocessed");
//            product.getFlagCodingGroup().add(rejection);
//            product.getBand("rejection_flag").setSampleCoding(rejection);
//        }
//        if (product.containsBand("confidence_flag")) {
//            FlagCoding confidence = new FlagCoding("confidence_flag_coding");
//            confidence.addFlag("side_lobe", 1, "potential side lobe contamination");
//            confidence.addFlag("rain_contamination", 2, "relaxed rain contamination suspected");
//            confidence.addFlag("small_sst", 4, "TMI SST retrieved in SST < 285K");
//            confidence.addFlag("high_wind_speed", 8, "high wind speed retrieval");
//            confidence.addFlag("sea_ice", 16, "sea ice retrieval for MW data");
//            confidence.addFlag("sun_glint", 32, "sun glint suspected");
//            confidence.addFlag("l2_native_bias", 64, "L2 native bias and standard deviation");
//            confidence.addFlag("l2_native_confidence", 128, "L2 native confidence value");
//            product.getFlagCodingGroup().add(confidence);
//            product.getBand("confidence_flag").setSampleCoding(confidence);
//        }
//        
//        product.setModified(false);
//        return product;
//    }

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
        if (!StringUtils.isNotNullAndNotEmpty(comment)) {
            return null;
        }
        List<MetadataAttribute> attributes = getFlagAttributes(comment.trim());
        if (attributes.size() == 0) {
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
    
    static List<MetadataAttribute> getFlagAttributes(String comment) {
        String[] split;
        if (comment.contains(";")) {
            split = comment.split(";");
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
