package org.esa.glob.reader.globcover;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class TileIndex implements Comparable{

    public static final int TILE_SIZE = 1800;
    public static final int MAX_HORIZ_INDEX = 71;
    public static final int MAX_VERT_INDEX = 35;

    private final int horizIndex;
    private final int vertIndex;
    private final int index;

    TileIndex(int horizIndex, int vertIndex) {
        this.horizIndex = horizIndex;
        this.vertIndex = vertIndex;
        index = horizIndex + MAX_HORIZ_INDEX * vertIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TileIndex)) {
            return false;
        }

        TileIndex tileIndex = (TileIndex) o;

        return index == tileIndex.index;

    }

    @Override
    public String toString() {
        return "TileIndex{" +
               "horizIndex=" + horizIndex +
               ", vertIndex=" + vertIndex +
               '}';
    }

    @Override
    public int hashCode() {
        int result = horizIndex;
        result = 31 * result + vertIndex;
        result = 31 * result + index;
        return result;
    }

    public int compareTo(Object o) {
        final TileIndex tileIndex = (TileIndex) o;
        return Integer.valueOf(this.index).compareTo(tileIndex.index);
    }

}
