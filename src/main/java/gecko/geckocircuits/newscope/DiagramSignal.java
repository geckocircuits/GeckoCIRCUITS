/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.geckocircuits.newscope;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author andy
 */
final class DiagramSignal extends AbstractDiagram {

    private static final int DEF_MIN_WIDTH = 30;
    public static final String DIAGRAM_TYPE_STRING = "DiagramSignal";

    public DiagramSignal(final AbstractDiagram oldDiagram) {        
        super(oldDiagram._grafer, oldDiagram._diagramSettings);
        _xAxis = oldDiagram._xAxis;
        _yAxis1 = oldDiagram._yAxis1;
        _yAxis2 = oldDiagram._yAxis2;                
    }
    
    
    @Override
    List<AbstractCurve> getCurvesCopy(final List<AbstractCurve> oldCurves) {
        final List<AbstractCurve> newCurves = new ArrayList<AbstractCurve>();
        for (AbstractCurve curve : oldCurves) {
            newCurves.add(new CurveSignal(curve, this));
        }
        return Collections.unmodifiableList(newCurves);
    }
    
    @Override
    AbstractCurve curveFabric() {
        return new CurveSignal(this);
    }

    @Override
    public int getHeight() {
        int anzSGN = 0;  // Anzahl der SIGNAL-Verlaeufe pro SIGNAL-Graph
        for (AbstractCurve curve : getCurves()) {
            if (curve.getAxisConnection() == AxisConnection.ZUORDNUNG_SIGNAL) {         
                anzSGN++;
            }
        }

        final int axisHeight = _xAxis.getRequiredAxisSpace();
        return axisHeight + anzSGN * (CurveSignal.SGN_HEIGHT + CurveSignal.SGN_DIST) + _ySpaceUpper + DY_IN_UNTEN;
    }            

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(DEF_MIN_WIDTH, this.getHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(DEF_PREF_WIDTH, this.getHeight());
    }
    

    int getSignalPosition(final CurveSignal curveSignal) {
        int returnValue = 0;
        for (AbstractCurve curve : getCurves()) {
            if (curve.equals(curveSignal)) {
                return returnValue;
            }

            if (curve.getAxisConnection() == AxisConnection.ZUORDNUNG_SIGNAL) {
                returnValue++;
            }
        }
        throw new AssertionError("Could not find curve in diagram!");
    }

    @Override
    double getDiagramYWeight() {
        return 0;
    }

    @Override
    String getDiagramTypeString() {        
        return DIAGRAM_TYPE_STRING;
    }

    
    
}
