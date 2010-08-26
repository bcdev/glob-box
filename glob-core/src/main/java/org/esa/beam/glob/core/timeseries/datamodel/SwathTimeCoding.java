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

package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 * <p/>
 * Swath implementation of {@link TimeCoding}. It simply interpolates line-wise
 * between start and end time.
 */
public class SwathTimeCoding extends TimeCoding {

    private final int height;

    /**
     * Constructor for a SwathTimeCoding.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @param height    the height
     */
    public SwathTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime, int height) {
        super(startTime, endTime);
        this.height = height;
    }

    /**
     * Interpolates time line-wise
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the interpolated time at the given pixel position
     */
    @Override
    public ProductData.UTC getTime(PixelPos pos) {
        if (pos.y < 0 || pos.y > height) {
            throw new IllegalArgumentException("position invalid");
        }
        final ProductData.UTC startTime = getStartTime();
        final ProductData.UTC endTime = getEndTime();

        final double dStart = startTime.getMJD();
        final double dEnd = endTime.getMJD();
        final double vPerLine = (dEnd - dStart) / (height - 1);
        final double currentLine = vPerLine * pos.y + dStart;
        return new ProductData.UTC(currentLine);
    }

}

