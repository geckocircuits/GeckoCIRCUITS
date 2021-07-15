/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.NonLinearReluctance;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ReluctanceInductor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public abstract class AbstractTerminal implements TerminalInterface {

    protected static final int DX_IN = 3, DX_OUT = DX_IN, DY_TEXT = -3;
    public static final int POINT_DIAMETER = 5;
    protected final CircuitLabel _label = new CircuitLabel();
    protected boolean _hasDoubleLabel = false;
    protected int _pFa = 11, _pFb = 3;  // Symbol-Pfeil fuer Flussrichtung --> Pfeilspitzenabmessung
    public final AbstractBlockInterface _parentElement;
    private int _circuitTerminalIndex;

    public AbstractTerminal(final AbstractBlockInterface parentElement) {
        _parentElement = parentElement;
    }

    public final void setHasDoubleLabel(final boolean value) {
        _hasDoubleLabel = value;
    }

    /**
     * set this flag when more than 1 component has a terminal at terminal
     * postition
     *
     * @return
     */
    public final boolean getHasDoubleValue() {
        return _hasDoubleLabel;
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        graphics.fillOval((int) (dpix * getPosition().x) - POINT_DIAMETER / 2,
                (int) (dpix * getPosition().y) - POINT_DIAMETER / 2, POINT_DIAMETER, POINT_DIAMETER);

    }

    public void paintLabelString(final Graphics2D graphics) {
        if (!_label.getLabelString().isEmpty()) {
            graphics.drawString(_label.getLabelString(), (int) (_parentElement.dpix * getPosition().x) + DX_IN,
                    (int) (_parentElement.dpix * getPosition().y) + DY_TEXT);
        }
    }

    public abstract AbstractTerminal createCopy(AbstractBlockInterface relatedComponent);

    @Override
    public final CircuitSheet getCircuitSheet() {
        return _parentElement._parentCircuitSheet;
    }

    @Override
    public ConnectorType getCategory() {
        if (_parentElement instanceof ReluctanceInductor) {
            if (_parentElement.XIN.contains(this)) {
                return ConnectorType.LK;
            } else {
                return ConnectorType.RELUCTANCE;
            }
        } else if (_parentElement instanceof NonLinearReluctance) {
            return ConnectorType.RELUCTANCE;
        } else {
            return _parentElement.getSimulationDomain();
        }
    }

    @Override
    public CircuitLabel getLabelObject() {
        return _label;
    }

    public int getIndex() {
        return _circuitTerminalIndex;
    }
    
    protected void paintFlowSymbol(int orientierung, Graphics g) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        int x = getPosition().x;
        int y = getPosition().y;
        int[] xFl = new int[3];
        int[] yFl = new int[3];
                
        if (_parentElement.getSheetPosition().y < y) {            
            xFl[0] = (int) (dpix * x);
            xFl[1] = xFl[0] - _pFb;
            xFl[2] = xFl[0] + _pFb;
            yFl[0] = (int) (dpix * (y));
            yFl[1] = yFl[0] - _pFa;
            yFl[2] = yFl[1];
        }
            
        if (_parentElement.getSheetPosition().y > y) {
            xFl[0] = (int) (dpix * x);
            xFl[1] = xFl[0] - _pFb;
            xFl[2] = xFl[0] + _pFb;
            yFl[0] = (int) (dpix * (y));
            yFl[1] = yFl[0] + _pFa;
            yFl[2] = yFl[1];
        }
        
        if (_parentElement.getSheetPosition().x < x) {
            xFl[0] = (int) (dpix * (x));
            xFl[1] = xFl[0] - _pFa;
            xFl[2] = xFl[1];
            yFl[0] = (int) (dpix * y);
            yFl[1] = yFl[0] - _pFb;
            yFl[2] = yFl[0] + _pFb;
          }
        

        if(_parentElement.getSheetPosition().x > x) {
            xFl[0] = (int) (dpix * (x));
            xFl[1] = xFl[0] + _pFa;
            xFl[2] = xFl[1];
            yFl[0] = (int) (dpix * y);
            yFl[1] = yFl[0] - _pFb;
            yFl[2] = yFl[0] + _pFb;
        }
        Color origColor = g.getColor();
        g.setColor(Color.magenta);
        g.drawPolygon(xFl, yFl, 3);
        g.setColor(origColor);
    }
}
