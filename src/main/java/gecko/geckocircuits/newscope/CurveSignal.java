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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

/**
 *
 * @author andy
 */
class CurveSignal extends AbstractCurve {

    public static final int SGN_HEIGHT = 10, SGN_DIST = 4;
    
    int _signalOffset;
    private final CurveLabel _curveLabel = new CurveLabelSignal(this);
    int _yTranslation;
    
    public CurveSignal(final AbstractCurve curve, final AbstractDiagram diagram) {
        super(curve, diagram);

        if (curve.getAxisConnection() != AxisConnection.ZUORDNUNG_NIX) {
            setAxisConnection(AxisConnection.ZUORDNUNG_SIGNAL);
        }
        _curvePainter = new CurvePainterSignal(this);
    }

    public CurveSignal(final AbstractDiagram diagram) {
        super(diagram);
        _curvePainter = new CurvePainterSignal(this);
    }

    @Override
    public void drawCurve(final Graphics2D g2d, final SliderContainer slider) {                
        final int y0Kurve = _yAxis._axisOriginPixel.y
                - (_diagram.getHeight() - (DY_IN_OBEN + DY_IN_UNTEN)) + _xAxis.getRequiredAxisSpace() - 2;

        final int positionSIGNAL = ((DiagramSignal) _diagram).getSignalPosition(this);
        _yTranslation = y0Kurve + _diagram._ySpaceUpper + SGN_HEIGHT
                - SGN_DIST + positionSIGNAL * (SGN_DIST + SGN_HEIGHT);
        
        g2d.translate(0, _yTranslation); // translate graphics object, so that the pixels are in the axis origin
        g2d.scale(1, -1); // scale, since the pixel axis is in negative direction        

        //g2d.setClip(_xAxis.axisOriginPixel.x+1, -3,_xAxis.axisLengthPix-1, _yAxis.sgnHeight+10);        
        _curvePainter.paintComponent(g2d);
        g2d.scale(1, -1); // undo scale                

        g2d.translate(0, -_yTranslation);
        g2d.setStroke(GeckoLineStyle.SOLID_PLAIN.stroke());  // wieder auf 'default' setzen            

        final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2d.setComposite(alphaComposite);
    }

    @Override
    public CurveLabel getCurveLabel() {
        return _curveLabel;
    }

}
