package org.esa.beam.glob.ui.graph;

import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayAxisMapping extends AxisMappingModel {

    private final Map<String, List<Paint>> alias_paintList_Map;

    DisplayAxisMapping() {
        alias_paintList_Map = new HashMap<String, List<Paint>>();
    }

    public void addPaintForAlias(String aliasName, Paint paint) {
        if (!getAliasNames().contains(aliasName)) {
            throw new IllegalStateException("alias '" + aliasName + "' must be already registered");
        }
        if (!alias_paintList_Map.containsKey(aliasName)) {
            alias_paintList_Map.put(aliasName, new ArrayList<Paint>());
        }
        alias_paintList_Map.get(aliasName).add(paint);
    }

    public List<Paint> getPaintListForAlias(String aliasName) {
        return Collections.unmodifiableList(alias_paintList_Map.get(aliasName));
    }

    public int getNumRegisteredPaints() {
        int numRegisteredPaints = 0;
        for (List<Paint> paintList : alias_paintList_Map.values()) {
            numRegisteredPaints += paintList.size();
        }
        return numRegisteredPaints;
    }
}
