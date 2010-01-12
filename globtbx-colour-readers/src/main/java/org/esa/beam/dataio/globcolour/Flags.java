package org.esa.beam.dataio.globcolour;

import java.awt.Color;

/**
 * The enumeration type <code>Flags</code> is a representation of
 * flags set by the GlobColour processor.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
enum Flags {

    /**
     * No measurement flag.
     */
    NO_MEASUREMENT(0x0001, "Pixel area is not included in any instrument swath", Color.red.darker()),
    /**
     * Invalid measurement flag.
     */
    INVALID(0x0002, "Pixel area is included in some instrument swaths, but measured value is invalid", Color.red),
    /**
     * Replica flag.
     */
    REPLICA(0x0004, "Pixel area does not include the center of the source pixel", Color.orange),
    /**
     * Land flag.
     */
    LAND(0x0008, "More than half the pixel area is covered by Land", Color.yellow.darker()),
    /**
     * Cloud flag (less significant bit).
     */
    CLOUD1(0x0010, "Cloud fraction (less significant bit)", Color.gray.brighter()),
    /**
     * Cloud flag (more significant bit).
     */
    CLOUD2(0x0020, "Cloud fraction (more significant bit)", Color.gray),
    /**
     * Water depth flag (less significant bit).
     */
    DEPTH1(0x0040, "Water depth (less significant bit)", Color.cyan),
    /**
     * Water depth flag (more significant bit).
     */
    DEPTH2(0x0080, "Water depth (more significant bit)", Color.blue),
    /**
     * Turbid water flag.
     */
    TURBID(0x0100, "Turbid water", Color.yellow),
    /**
     * SeaWiFS flag.
     */
    SEAWIFS(0x2000, "Measurement is based on SeaWiFS data", Color.magenta.darker()),
    /**
     * MODIS flag.
     */
    MODIS(0x4000, "Measurement is based on MODIS data", Color.magenta),
    /**
     * MERIS flag.
     */
    MERIS(0x8000, "Measurement is based on MERIS data", Color.green);

    private int mask;
    private Color color;
    private float transparency;
    private String description;

    Flags(final int mask, final String description, final Color color) {
        this(mask, description, color, 0.5f);
    }

    Flags(final int mask, final String description, final Color color, final float transparency) {
        this.mask = mask;
        this.color = color;
        this.transparency = transparency;
        this.description = description;
    }

    /**
     * Returns the bit mask associated with the flag.
     *
     * @return the bit mask.
     */
    public int getMask() {
        return mask;
    }

    /**
     * Returns the textual description of the flag.
     *
     * @return the textual description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the color associated with this flag (useful for colored bit mask layers).
     *
     * @return the color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the transparency associated with this flag (useful for colored bit mask layers).
     *
     * @return the transparency.
     */
    public float getTransparency() {
        return transparency;
    }

    /**
     * Tests a bit pattern for the status of the flag.
     *
     * @param value the bit pattern.
     * @return true if the flag is set, false otherwise.
     */
    public boolean isSet(final int value) {
        return (value & mask) != 0;
    }
}
