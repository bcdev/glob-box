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

package org.esa.beam.dataio.globcarbon;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReaderPlugInTest {

    private GlobCarbonEnviProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new GlobCarbonEnviProductReaderPlugIn();
    }

    @Test
    public void testDecodeQualificationForZipFile() {
        File input = new File(getClass().getResource("FAPAR_PLC_10KM_AATSR_20030102.zip").getFile());
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));

        input = new File(getClass().getResource("BAE_PLC_025D_199907.zip").getFile());
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
    }

    @Test
    public void testImgFileExists() throws Exception {
        File input = new File(getClass().getResource("VGCP_PLC_050D_AV_2005_DAFTER.hdr").getFile());
        assertEquals(true, plugIn.existsImgFileInDirectory(input));

        input = new File(getClass().getResource("VGCP_PLC_050D_AV_2005_PLOC.hdr").getFile());
        assertEquals(true, plugIn.existsImgFileInDirectory(input));

        input = new File(getClass().getResource("VGCP_PLC_050D_AV_2005_DBEFORE.hdr").getFile());
        assertEquals(false, plugIn.existsImgFileInDirectory(input));

        input = new File(getClass().getResource("VGCP_PLC_050D_AV_2005_MAX2NDSD.hdr").getFile());
        assertEquals(false, plugIn.existsImgFileInDirectory(input));
    }


    @SuppressWarnings({"OverlyLongMethod"})
    @Test
    public void testFileNameOk() throws Exception {
        assertEquals(true, plugIn.isFileNameOk("BAE_PLC_200007_ASCII_COMB.zip"));
        assertEquals(true, plugIn.isFileNameOk("BAE_PLC_01KM_200507_DOD.HDR"));
        assertEquals(true, plugIn.isFileNameOk("BAE_PLC_01KM_200507_NUMALGO.HDR"));
        assertEquals(true, plugIn.isFileNameOk("BAE_PLC_01KM_200507_VALUE.HDR"));

        assertEquals(true, plugIn.isFileNameOk("LAI_PLC_01KM_AV_199807_FLAG.HDR"));
        assertEquals(true, plugIn.isFileNameOk("LAI_PLC_01KM_AV_199807_LOW20.HDR"));
        assertEquals(true, plugIn.isFileNameOk("LAI_PLC_01KM_AV_199807_NUMVEG.HDR"));
        assertEquals(true, plugIn.isFileNameOk("LAI_PLC_01KM_AV_199807_VALUE.HDR"));

        assertEquals(true, plugIn.isFileNameOk("VGCP_PLC_050D_AV_2005_DAFTER.hdr"));
        assertEquals(true, plugIn.isFileNameOk("VGCP_PLC_050D_AV_2005_DAFTERSD.hdr"));
        assertEquals(true, plugIn.isFileNameOk("VGCP_PLC_050D_AV_2005_GRMLAISD.hdr"));
        assertEquals(true, plugIn.isFileNameOk("VGCP_PLC_050D_AV_2005_LFOFFSD.hdr"));

        final File input10 = new File("BAE_PLC_200007_ASCII_COMB.zip");
        assertEquals(true, plugIn.isFileNameOk(input10.toString()));
        final File input2 = new File("BAE_PLC_01KM_200507_DOD.HDR");
        assertEquals(true, plugIn.isFileNameOk(input2.toString()));
        final File input5 = new File("BAE_PLC_01KM_200507_NUMALGO.HDR");
        assertEquals(true, plugIn.isFileNameOk(input5.toString()));
        final File input14 = new File("BAE_PLC_01KM_200507_VALUE.HDR");
        assertEquals(true, plugIn.isFileNameOk(input14.toString()));

        final File input1 = new File("LAI_PLC_01KM_AV_199807_FLAG.HDR");
        assertEquals(true, plugIn.isFileNameOk(input1.toString()));
        final File input8 = new File("LAI_PLC_01KM_AV_199807_LOW20.HDR");
        assertEquals(true, plugIn.isFileNameOk(input8.toString()));
        final File input4 = new File("LAI_PLC_01KM_AV_199807_NUMVEG.HDR");
        assertEquals(true, plugIn.isFileNameOk(input4.toString()));
        final File input11 = new File("LAI_PLC_01KM_AV_199807_VALUE.HDR");
        assertEquals(true, plugIn.isFileNameOk(input11.toString()));

        final File input7 = new File("VGCP_PLC_050D_AV_2005_DAFTER.hdr");
        assertEquals(true, plugIn.isFileNameOk(input7.toString()));
        final File input9 = new File("VGCP_PLC_050D_AV_2005_DAFTERSD.hdr");
        assertEquals(true, plugIn.isFileNameOk(input9.toString()));
        final File input6 = new File("VGCP_PLC_050D_AV_2005_GRMLAISD.hdr");
        assertEquals(true, plugIn.isFileNameOk(input6.toString()));
        final File input12 = new File("VGCP_PLC_050D_AV_2005_LFOFFSD.hdr");
        assertEquals(true, plugIn.isFileNameOk(input12.toString()));

        final File input = new File("FAPAR_PLC_025D_VGT_19980505_AAD.hdr");
        assertEquals(true, plugIn.isFileNameOk(input.toString()));
        final File input13 = new File("FAPAR_PLC_025D_VGT_19980505_SZASD.hdr");
        assertEquals(true, plugIn.isFileNameOk(input13.toString()));
        final File input3 = new File("FAPAR_PLC_025D_VGT_19980505.zip");
        assertEquals(true, plugIn.isFileNameOk(input3.toString()));
        final File input15 = new File("FAPAR_PLC_025D_VGT_19980514.zip");
        assertEquals(true, plugIn.isFileNameOk(input15.toString()));
    }

    @Test
    public void testFileNameNotOk() {
        assertEquals(false, plugIn.isFileNameOk("BAE_199807_ASCII.img"));
        assertEquals(false, plugIn.isFileNameOk("BAE_IGH_200007_ASCII_COMB.ascii"));
        assertEquals(false, plugIn.isFileNameOk("VGCP_PLC_050D_AV_2005_DAFTER.img"));
        assertEquals(false, plugIn.isFileNameOk("FAPAR_PLC_050D_AV_2005_DAFTER.img"));
        assertEquals(false, plugIn.isFileNameOk("BAE_IGH_01KM_200507_NUMALGO.HDR"));
        assertEquals(false, plugIn.isFileNameOk("FAPAR_IGH_01KM_200507_VALUE.HDR"));
    }

    @Test
    public void testGetProductFiles() throws Exception {
        final String[] productFiles = plugIn.getProductFiles(
                getClass().getResource("BAE_PLC_025D_199907.zip").getFile());
        final List<String> productFileList = Arrays.asList(productFiles);

        assertTrue(productFileList.contains("BAE_PLC_025D_199907_DISP.img"));
        assertTrue(productFileList.contains("BAE_PLC_025D_199907_DISP.hdr"));
        assertTrue(productFileList.contains("BAE_PLC_025D_199907_BPROP.img"));
        assertTrue(productFileList.contains("BAE_PLC_025D_199907_BPROP.hdr"));
    }
}
