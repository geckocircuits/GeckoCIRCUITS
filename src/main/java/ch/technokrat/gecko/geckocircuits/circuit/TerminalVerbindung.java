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

import ch.technokrat.gecko.geckocircuits.control.Point;
import java.awt.Graphics;
import java.util.List;

/**
 *
 * @author andreas
 */
public class TerminalVerbindung implements TerminalInterface {
    private final Location _loc;
    private final Verbindung _verb;
    private final List<Point> _connectorPoints;
    

    @Override
    public ConnectorType getCategory() {
        return _verb.getSimulationDomain();
    }

    public Verbindung getParentConnection() {
        return _verb;
    }

    @Override
    public CircuitLabel getLabelObject() {
        return _verb.getLabelObject();
    }

    enum Location {
        START,
        END;
    }
    
    public TerminalVerbindung(final Verbindung verb, final List<Point> connectorPoints, final Location loc) {
        _loc = loc;
        _verb = verb;
        _connectorPoints = connectorPoints;
    }

    public final void setLabelPriority(final LabelPriority labelPriority) {
        _verb.setLabelPriority(labelPriority);
    }        
    
    @Override
    public Point getPosition() {
        switch(_loc) {
            case START:
                return _connectorPoints.get(0);
            case END:
                return _connectorPoints.get(_connectorPoints.size()-1);
            default:
                assert false;
                return null;
        }
    }

    @Override
    public CircuitSheet getCircuitSheet() {
        return _verb._parentCircuitSheet;
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        graphics.fillOval((int) (dpix * getPosition().x) - AbstractTerminal.POINT_DIAMETER / 2, 
                (int) (dpix * getPosition().y) - AbstractTerminal.POINT_DIAMETER / 2,
                AbstractTerminal.POINT_DIAMETER, AbstractTerminal.POINT_DIAMETER);
    }
    
    
    
}
