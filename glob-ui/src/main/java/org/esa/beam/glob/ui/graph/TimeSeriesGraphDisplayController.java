package org.esa.beam.glob.ui.graph;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TimeSeriesGraphDisplayController {

    private static final Color[] COLORS = {
                Color.red,
                Color.green,
                Color.blue,
                Color.magenta,
                Color.orange,
                Color.darkGray,
                Color.pink,
                Color.cyan,
                Color.yellow,
                Color.red.brighter(),
                Color.green.brighter(),
                Color.blue.brighter(),
                Color.blue.brighter(),
                Color.magenta.brighter(),
                Color.orange.darker(),
    };

    private final List<String> eoVariablesToDisplay;
    private final List<String> insituVariablesToDisplay;
    private final PinSupport pinSupport;
    private AbstractTimeSeries timeSeries;

    TimeSeriesGraphDisplayController(PinSupport pinSupport) {
        this.pinSupport = pinSupport;
        eoVariablesToDisplay = new ArrayList<String>();
        insituVariablesToDisplay = new ArrayList<String>();
    }

    public Paint getPaint(int i) {
        return COLORS[i % COLORS.length];
    }

    public List<String> getEoVariablesToDisplay() {
        return Collections.unmodifiableList(eoVariablesToDisplay);
    }

    public List<String> getInsituVariablesToDisplay() {
        return Collections.unmodifiableList(insituVariablesToDisplay);
    }

    public void adaptTo(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
        for (String eoVariableName : timeSeries.getEoVariables()) {
            if (timeSeries.isEoVariableSelected(eoVariableName)) {
                if (!eoVariablesToDisplay.contains(eoVariableName)) {
                    eoVariablesToDisplay.add(eoVariableName);
                }
            } else {
                eoVariablesToDisplay.remove(eoVariableName);
            }
        }
        if (timeSeries.hasInsituData()) {
            for (String insituVariableName : timeSeries.getInsituSource().getParameterNames()) {
                if (timeSeries.isInsituVariableSelected(insituVariableName)) {
                    if (!insituVariablesToDisplay.contains(insituVariableName)) {
                        insituVariablesToDisplay.add(insituVariableName);
                    }
                } else {
                    insituVariablesToDisplay.remove(insituVariableName);
                }
            }
        }
    }

    public List<GeoPos> getPinPositionsToDisplay() {
        final ArrayList<GeoPos> pinPositionsToDisplay = new ArrayList<GeoPos>(0);
        if (pinSupport.isShowingAllPins()) {
            final PlacemarkGroup pinGroup = timeSeries.getTsProduct().getPinGroup();
            for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                final Placemark pin = pinGroup.get(i);
                pinPositionsToDisplay.add(pin.getGeoPos());
            }
        } else if (pinSupport.isShowingSelectedPins()) {
            final Placemark[] selectedPins = pinSupport.getSelectedPins();
            for (Placemark selectedPin : selectedPins) {
                pinPositionsToDisplay.add(selectedPin.getGeoPos());
            }
        }
        return pinPositionsToDisplay;
    }

    interface PinSupport {
        boolean isShowingAllPins();
        boolean isShowingSelectedPins();
        Placemark[] getSelectedPins();

    }
}
