package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.ProductData;

import java.text.ParseException;

/**
 * User: Thomas Storm
 * Date: 01.04.2010
 * Time: 11:45:13
 */
public interface TimeFromFileNameParser extends TimeDataHandler {

    public ProductData.UTC[] parseTimeFromFileName(String fileName) throws ParseException;

}
