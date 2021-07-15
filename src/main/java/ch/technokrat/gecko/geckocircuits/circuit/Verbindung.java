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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.control.DialogLabelEingeben;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public class Verbindung extends AbstractCircuitSheetComponent implements ComponentTerminable, Labable {

    private boolean _inMoveMode = false;  // wird nur zur De-selektion mittels ESCAPE verwendet 
    private final List<Point> _connectorPoints = new ArrayList<Point>();
    private boolean _movementWestEast;  // Bewegungsrichtung mit der Maus beim Ziehen der Verbindung
    private CircuitLabel _label = new CircuitLabel();
    private final List<Point> _pointsBeforeMove = new ArrayList<Point>();
    private ConnectorType _connectorType;
    private final TerminalVerbindung _startTerminal =
            new TerminalVerbindung(this, _connectorPoints, TerminalVerbindung.Location.START);
    private final TerminalVerbindung _endTerminal =
            new TerminalVerbindung(this, _connectorPoints, TerminalVerbindung.Location.END);
    private boolean _isInitialized;    
    public List<Point> _subPaths = new ArrayList<Point>();
    private List<Point> _trimmedCoords = new ArrayList<Point>();

    public Verbindung(final ConnectorType connectorType, final CircuitSheet parentSheet) {
        super();
        _connectorType = connectorType;
        if(! (this instanceof VerbindungShortConnector)) {
            setParentCircuitSheet(parentSheet);
        }                
    }

    public final ConnectorType getSimulationDomain() {
        return _connectorType;
    }

    public Verbindung(final TokenMap tokenMap, final ConnectorType connectorType) {
        super();
        _connectorType = connectorType;
        this.importASCII(tokenMap);
        _isInitialized = true;
        trimCoordinates();
    }

    public final boolean wirdGeradeGezogen() {
        return _inMoveMode;
    }

    public final Point getStartPoint() {
        assert !_connectorPoints.isEmpty();
        return _connectorPoints.get(0);
    }

    public final Point getEndPoint() {
        assert !_connectorPoints.isEmpty();
        return _connectorPoints.get(_connectorPoints.size() - 1);
    }

    public final List<Point> getAllPointCoordinates() {
        return Collections.unmodifiableList(_connectorPoints);
    }

    public final void setLabel(final String label) {        
        _label.setLabel(label);
    }

    public final String getLabel() {
        return _label.getLabelString();
    }

    public final void setzeStartKnoten(final Point clickPoint) {
        _inMoveMode = true;        
        _connectorPoints.clear();
        _connectorPoints.add(clickPoint);
    }

    public final void setzeEndKnoten(final int pointX, final int pointY) {
        _inMoveMode = false;        
        final Point endPoint = new Point(pointX, pointY);
        _connectorPoints.set(_connectorPoints.size() - 1, endPoint);
        this.absetzenElement();
        trimCoordinates();        
    }

    @Override
    public final void deselectViaESCAPE() {
        _connectorPoints.clear();
        _connectorPoints.addAll(_pointsBeforeMove);

        setModus(ComponentState.FINISHED);
    }

    @Override
    public final void absetzenElement() {          
        if(_isInitialized) {
            MoveVerbindungUndoAction undoAction = new MoveVerbindungUndoAction(_pointsBeforeMove, _connectorPoints);            
            AbstractUndoGenericModel.undoManager.addEdit(undoAction);
        }
        _pointsBeforeMove.clear();
        _pointsBeforeMove.addAll(_connectorPoints);        
        setModus(ComponentState.FINISHED);
        _isInitialized = true;
    }

    public final void moveComponent(final Point moveToPoint) {
        for (int i1 = 0; i1 < _connectorPoints.size(); i1++) {
            final Point movedPoint = new Point(_pointsBeforeMove.get(i1).x + moveToPoint.x, _pointsBeforeMove.get(i1).y + moveToPoint.y);
            _connectorPoints.set(i1, movedPoint);
        }
    }

    public final void setzeAktuellenPunktAufVerbindung(final Point point) {
        if (_connectorPoints.size() == 1) { // do the direction change only when crossing the zero-size
            if (Math.abs(point.x - _connectorPoints.get(0).x) >= Math.abs(point.y - _connectorPoints.get(0).y)) {
                _movementWestEast = true;
            } else {
                _movementWestEast = false;
            }
        }
        final Point startPoint = _connectorPoints.get(0);
        _connectorPoints.clear();
        _connectorPoints.add(startPoint);
        if (_movementWestEast) {  // erster Teil der Linie entlang x-Achse (waagrecht)
            moveHorizontal(point, startPoint);
        } else {    // erster Teil der Linie entlang y-Achse (senkrecht)
            moveVertical(point, startPoint);
        }
    }

    private void moveHorizontal(final Point point, final Point startPoint) {
        final int xEcke = point.x, yEcke = startPoint.y;

        int dirX = -1;
        if (xEcke > startPoint.x) {
            dirX = 1;
        }

        int dirY = -1;
        if (point.y > startPoint.y) {
            dirY = 1;
        }

        for (int ix = 1; ix <= Math.abs(xEcke - startPoint.x); ix++) {
            final Point newPoint = new Point(startPoint.x + _connectorPoints.size() * dirX, startPoint.y);
            _connectorPoints.add(newPoint);
        }
        for (int iy = 1; iy <= Math.abs(point.y - yEcke); iy++) {
            final Point newPoint = new Point(xEcke, startPoint.y + iy * dirY);
            _connectorPoints.add(newPoint);
        }
    }

    private void moveVertical(final Point point, final Point startPoint) {
        final int xEcke = startPoint.x, yEcke = point.y;
        int dirX = -1;
        if (point.x > startPoint.x) {
            dirX = 1;
        }

        int dirY = -1;
        if (point.y > startPoint.y) {
            dirY = 1;
        }

        for (int iy = 1; iy <= Math.abs(yEcke - startPoint.y); iy++) {
            final Point newPoint = new Point(startPoint.x, startPoint.y + _connectorPoints.size() * dirY);
            _connectorPoints.add(newPoint);
        }
        for (int ix = 1; ix <= Math.abs(point.x - xEcke); ix++) {
            final Point newPoint = new Point(startPoint.x + ix * dirX, yEcke);
            _connectorPoints.add(newPoint);
        }
    }

    @Override
    public boolean testDoDoubleClickAction(final Point clickedPoint) {
        for (Point rasterPoint : _connectorPoints) {
            if (rasterPoint.equals(clickedPoint)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doDoubleClickAction(Point point) {
        DialogLabelEingeben labelDialog = new DialogLabelEingeben(_startTerminal);
        labelDialog.setVisible(true);
    }

    @Override
    public int elementAngeklickt(final Point clickPoint) {
        for (Point testPoint : _connectorPoints) {
            if (testPoint.equals(clickPoint)) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void paintComponentForeGround(Graphics2D graphics) {
        //nothing todo here!
    }

    @Override
    public final void paintGeckoComponent(final Graphics2D graphics) {        
        Color fFertig = GlobalColors.farbeFertigVerbindungLK;
        switch (_connectorType) {
            case RELUCTANCE:
                fFertig = GlobalColors.farbeFertigElementRELUCTANCE;
                break;
            case LK:
                fFertig = GlobalColors.farbeFertigVerbindungLK;
                break;
            case CONTROL:
                fFertig = GlobalColors.farbeFertigVerbindungCONTROL;
                break;
            case THERMAL:
                fFertig = GlobalColors.farbeFertigVerbindungTHERM;
                break;
            case NONE:
                fFertig = Color.gray;
                break;
            default:
                assert false;
        }

        graphics.setColor(fFertig);

        if (_connectorType == ConnectorType.RELUCTANCE) {
            graphics.setColor(GlobalColors.farbeElementRELFOREGROUND);
        }

        int[] xPix = new int[_connectorPoints.size()];
        int[] yPix = new int[_connectorPoints.size()];
        final int dpix = AbstractCircuitSheetComponent.dpix;
        for (int i = 0; i < _connectorPoints.size(); i++) {
            xPix[i] = _connectorPoints.get(i).x * dpix;
            yPix[i] = _connectorPoints.get(i).y * dpix;
        }

        graphics.drawPolyline(xPix, yPix, _connectorPoints.size());

        _startTerminal.paintComponent(graphics);
        _endTerminal.paintComponent(graphics);

    }

    @Override
    public final Verbindung copyFabric(final long shiftValue) {
        final Verbindung vNeu = new Verbindung(this._connectorType, this.getParentCircuitSheet());
        for (int i1 = 0; i1 < _connectorPoints.size(); i1++) {
            vNeu._connectorPoints.add(_connectorPoints.get(i1));
        }
        vNeu._pointsBeforeMove.addAll(_connectorPoints);
        vNeu.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + shiftValue);
        return vNeu;
    }

    // zum Speichern im ASCII-Format (anstatt als Object-Stream) -->
    public final void exportASCII(final StringBuffer ascii) {
        ascii.append("<Verbindung>");
        DatenSpeicher.appendAsString(ascii.append("\nlabel"), _label.getLabelString());

        int[] xPoints = new int[_connectorPoints.size()];
        int[] yPoints = new int[_connectorPoints.size()];
        for (int i = 0; i < _connectorPoints.size(); i++) {
            xPoints[i] = _connectorPoints.get(i).x;
            yPoints[i] = _connectorPoints.get(i).y;
        }

        DatenSpeicher.appendAsString(ascii.append("\nx"), xPoints);
        DatenSpeicher.appendAsString(ascii.append("\ny"), yPoints);

        super.exportASCII(ascii);

        DatenSpeicher.appendAsString(ascii.append("\nconnectorType"), _connectorType.ordinal());
        ascii.append(new StringBuffer("\n<\\Verbindung>\n"));
    }

    @Override
    public final void importASCII(final TokenMap tokenMap) {
        _label.setLabelWithoutUndo(tokenMap.readDataLine("label", ""));

        super.importASCII(tokenMap);

        int[] xPoints = new int[0];
        int[] yPoints = new int[0];
        xPoints = tokenMap.readDataLine("x[]", xPoints);
        yPoints = tokenMap.readDataLine("y[]", yPoints);

        for (int i = 0; i < xPoints.length; i++) {
            _connectorPoints.add(new Point(xPoints[i], yPoints[i]));
        }

        _pointsBeforeMove.addAll(_connectorPoints);


        if (tokenMap.containsToken("connectorType")) {
            _connectorType = ConnectorType.fromOrdinal(tokenMap.readDataLine("connectorType", ConnectorType.LK.ordinal()));
        }
    }

    /**
     * @return the _labelPriority
     */
    public final LabelPriority getLabelPriority() {
        return _label.getLabelPriority();
    }
    
    public void changeConnectorType(ConnectorType newType) {
        _connectorType = newType;
    }

    /**
     * @param labelPriority the _labelPriority to set
     */
    public final void setLabelPriority(final LabelPriority labelPriority) {
        _label.setLabelPriority(labelPriority);
    }

    @Override
    public Collection<? extends TerminalInterface> getAllTerminals() {
        return Arrays.asList(_startTerminal, _endTerminal);
    }

    @Override
    public String getExportImportCharacters() {
        switch (getSimulationDomain()) {
            case CONTROL:
                return "verbindungCONTROL ";
            case LK_AND_RELUCTANCE:
            case RELUCTANCE:
            case NONE:
            case LK:
                return "verbindungLK ";
            case THERMAL:
                return "verbindungTHERM ";                        
            default:
                assert false;
                return null;
        }
    }

    @Override
    Collection<? extends Point> getAllDimensionPoints() {
        return Arrays.asList(getStartPoint(), getEndPoint());
    }

    @Override
    public Collection<String> getAllNodeLabels() {
        return Arrays.asList(_label.getLabelString());
    }

    @Override
    public int[] getAussenabmessungenRechteckEckpunkte() {        
        int[] returnValue = new int[4];
        returnValue[0] = dpix * Math.min(_startTerminal.getPosition().x, _endTerminal.getPosition().x);
        returnValue[1] = dpix * Math.min(_startTerminal.getPosition().y, _endTerminal.getPosition().y);
        returnValue[2] = dpix * Math.max(_startTerminal.getPosition().x, _endTerminal.getPosition().x);
        returnValue[3] = dpix * Math.max(_startTerminal.getPosition().y, _endTerminal.getPosition().y);
        return returnValue;
    }

    @Override
    public void setPositionWithUndo() {
        absetzenElement();
    }

    @Override
    public CircuitLabel getLabelObject() {
        return _label;
    }

    public void initAnimationParts() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public void trimCoordinates() {
        if (_connectorPoints.size() > 1) {
            _trimmedCoords.add(new Point(_connectorPoints.get(0).x, _connectorPoints.get(0).y));

            if (_connectorPoints.get(0).x == _connectorPoints.get(1).x) { // then y changes
                for (int j = 0; j < _connectorPoints.size() - 1; j++) {
                    if (_connectorPoints.get(j).y == _connectorPoints.get(j + 1).y) {
                        _trimmedCoords.add(new Point(_connectorPoints.get(j).x, _connectorPoints.get(j).y));
                        break;
                    }
                }
            } else {
                for (int j = 0; j < _connectorPoints.size() - 1; j++) {
                    if (_connectorPoints.get(j).x == _connectorPoints.get(j + 1).x) {
                        _trimmedCoords.add(new Point(_connectorPoints.get(j).x, _connectorPoints.get(j).y));
                        break;
                    }
                }
            }

            _trimmedCoords.add(new Point(_connectorPoints.get(_connectorPoints.size() - 1).x, _connectorPoints.get(_connectorPoints.size() - 1).y));
            
           
        }
    }

    public final List<Point> getTrimmedCoords() {
        return _trimmedCoords;
    }

    private class MoveVerbindungUndoAction implements UndoableEdit {
        private final List<Point> _newPositions = new ArrayList<Point>();
        private final List<Point> _oldPositions = new ArrayList<Point>();
        
        private MoveVerbindungUndoAction(final List<Point> oldPositions, final List<Point> newPositions) {
            _newPositions.addAll(newPositions);
            _oldPositions.addAll(oldPositions);
        }

        @Override
        public void undo() {
            _connectorPoints.clear();
            _connectorPoints.addAll(_oldPositions);
            _pointsBeforeMove.clear();
            _pointsBeforeMove.addAll(_oldPositions);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() throws CannotRedoException {
            _connectorPoints.clear();
            _connectorPoints.addAll(_newPositions);
            _pointsBeforeMove.clear();
            _pointsBeforeMove.addAll(_newPositions);
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo here
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Move connection";
        }

        @Override
        public String getUndoPresentationName() {
            return "Move connection";
        }

        @Override
        public String getRedoPresentationName() {
            return "Move connection";
        }
    }
    
}
