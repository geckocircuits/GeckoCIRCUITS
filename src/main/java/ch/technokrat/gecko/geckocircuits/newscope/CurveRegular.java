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
package ch.technokrat.gecko.geckocircuits.newscope;

import java.awt.Graphics2D;

/**
 *
 * @author andy
 */
class CurveRegular extends AbstractCurve {

    private final CurveLabel _curveLabel = new CurveLabelRegular(this);        
    
    public CurveRegular(final AbstractDiagram diagram) {
        super(diagram);
        _curvePainter = new CurvePainterRegular(this);
    }

    /*
     * conversion constructor 
     * @Param curve old curve, to convert from
     */
    public CurveRegular(final AbstractCurve curve, final AbstractDiagram diagram) {
        super(curve, diagram);

        if (curve.getAxisConnection() != AxisConnection.ZUORDNUNG_NIX) {
            setAxisConnection(AxisConnection.ZUORDNUNG_Y);
        }

        _curvePainter = new CurvePainterRegular(this);

    }

    @Override
    public void drawCurve(final Graphics2D g2d, final SliderContainer slider) {                
        if (getAxisConnection() != AxisConnection.ZUORDNUNG_Y && getAxisConnection() != AxisConnection.ZUORDNUNG_Y2) {
            return;
        }        
        
        if (getAxisConnection() == AxisConnection.ZUORDNUNG_Y2) {
            _yAxis = _diagram._yAxis2;
        } else {
            _yAxis = _diagram._yAxis1;
        }        
                
        g2d.setColor(_curveSettings._curveColor.getJavaColor());        
        _curvePainter.paintComponent(g2d);        
        drawCurveLine(g2d);        
    }

    private void drawCurveLine(final Graphics2D g2d) {
        GPATH.reset();
        _curveSettings._curveLineStyle.setStrokeStyle(g2d);
        g2d.setStroke(GeckoLineStyle.SOLID_PLAIN.stroke());  // wieder auf 'default' setzen
    }

    @Override
    public CurveLabel getCurveLabel() {
        return _curveLabel;
    }
}
