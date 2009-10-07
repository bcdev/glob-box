package org.esa.glob.reader.globcover;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.TreeMap;

import com.bc.ceres.glevel.support.DefaultMultiLevelModel;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class TileIndexTest {

    @Test
    public void testFindInMap(){
        final TreeMap<TileIndex, Integer> map = new TreeMap<TileIndex, Integer>();
        map.put(new TileIndex(0, 3), 0);
        map.put(new TileIndex(1, 3), 1);
        map.put(new TileIndex(5, 3), 2);

        assertNotNull(map.get(new TileIndex(1,3)));
        assertNotNull(map.get(new TileIndex(5,3)));
        assertNotNull(map.get(new TileIndex(0,3)));
    }

}
