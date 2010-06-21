package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TimeCoding;

/**
 * Default implementation of {@link TimeCoding}. It simply interpolates line-wise
 * between start and end time.
 */
class DefaultTimeCoding extends TimeCoding {

    private int height;

    /**
     * Constructor for a DefaultTimeCoding.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @param height    the height
     */
    public DefaultTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime, int height) {
        super(startTime, endTime);
        this.height = height;
    }

    /**
     * Interpolates time line-wise
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the interpolated time at the given pixel position
     *
     * @throws IllegalArgumentException if pixel is out of bounds
     */
    @Override
    public ProductData.UTC getTime(PixelPos pos) {
        final ProductData.UTC startTime = getStartTime();
        final ProductData.UTC endTime = getEndTime();

        if (startTime != null && endTime != null) {
            final double dStart = startTime.getMJD();
            final double dEnd = endTime.getMJD();
            final double vPerLine = (dEnd - dStart) / (height - 1);
            final double currentLine = vPerLine * pos.y + dStart;
            return new ProductData.UTC(currentLine);
        }

        if (startTime != null) {
            return new ProductData.UTC(startTime.getMJD());
        }

        if (endTime != null) {
            return new ProductData.UTC(endTime.getMJD());
        }

        return null;
    }

}

