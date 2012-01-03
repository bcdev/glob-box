package org.esa.beam.glob.ui.graph;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
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

    private static Shape createTriangle() {
        final GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(-5,-5);
        generalPath.lineTo(5, -5);
        generalPath.lineTo(0, 5);
        generalPath.closePath();
        return generalPath;
    }

    private static Shape createPlus() {
        final GeneralPath generalPath = new GeneralPath();
        generalPath.moveTo(-5,-1);
        generalPath.lineTo(-1, -1);
        generalPath.lineTo(-1, -5);
        generalPath.lineTo(1, -5);
        generalPath.lineTo(1, -1);
        generalPath.lineTo(5, -1);
        generalPath.lineTo(5, 1);
        generalPath.lineTo(1, 1);
        generalPath.lineTo(1, 5);
        generalPath.lineTo(-1, 5);
        generalPath.lineTo(-1, 1);
        generalPath.lineTo(-5, 1);
        generalPath.closePath();
        return generalPath;
    }
    private static final AffineTransform ROTATION_45 = AffineTransform.getRotateInstance(Math.toRadians(45));
    private static final AffineTransform ROTATION_180 = AffineTransform.getRotateInstance(Math.toRadians(180));

    private static final Shape[] SHAPES = {
            new Rectangle(-5, -5, 10, 10),
            new Ellipse2D.Float(-5,-5, 10,10),
            ROTATION_45.createTransformedShape( new Rectangle(-5, -5, 10, 10)),
            createTriangle(),
            ROTATION_180.createTransformedShape(createTriangle()),
            createPlus()
    };

    public static final Shape CURSOR_SHAPE = ROTATION_45.createTransformedShape(createPlus());

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

    public Shape getShape(int posIdx) {
        return SHAPES[posIdx % SHAPES.length];
    }

    interface PinSupport {
        boolean isShowingAllPins();
        boolean isShowingSelectedPins();
        Placemark[] getSelectedPins();

    }
}
