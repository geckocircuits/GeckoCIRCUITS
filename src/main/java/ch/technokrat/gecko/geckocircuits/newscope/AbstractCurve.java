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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
 * A Curve class represents a single curve within a diagram, please note that
 * there can be several curves in one diagram, or different curves with the same
 * data can be plotted in different Diagrams!
 *
 * @author andy
 */
public abstract class AbstractCurve implements LineSettable, SymbolSettable {

    protected static final int DY_IN_OBEN = 8, DY_IN_UNTEN = 8;
    protected final Axis _xAxis;
    protected Axis _yAxis;
    protected static final GeneralPath GPATH = new GeneralPath();
    private AxisConnection _axisConnection = AxisConnection.ZUORDNUNG_NIX;
    protected final AbstractDiagram _diagram;
    protected int _noCuvePoints;
    protected final CurveSettings _curveSettings;
    private final GraferV4 _grafer;
    public AbstractCurvePainter _curvePainter;
    boolean _isSelected;    

    AbstractCurve(final AbstractDiagram diagram) {
        this._axisConnection = AxisConnection.ZUORDNUNG_NIX;
        _diagram = diagram;
        _xAxis = diagram._xAxis;
        _yAxis = diagram._yAxis1;
        _curveSettings = new CurveSettings();
        _grafer = diagram._grafer;
    }

    /**
     * clone diagram from given curve
     *
     * @param diagram
     * @param dataIndex
     */
    AbstractCurve(final AbstractCurve origCurve, final AbstractDiagram diagram) {
        this._axisConnection = AxisConnection.ZUORDNUNG_NIX;
        _diagram = diagram;
        _xAxis = origCurve._xAxis;
        _yAxis = origCurve._yAxis;
        _curveSettings = origCurve._curveSettings;
        _grafer = diagram._grafer;
    }

    public void populatePropertiesFromModelFile(final TokenMap curveMap, final AbstractDiagram diagram) {
        _axisConnection = AxisConnection.getFromCode(
                curveMap.readDataLine("axisConnection", _axisConnection.getCode()));
        _curveSettings.importASCII(curveMap);
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        _curveSettings.exportIndividualCONTROL(ascii);
        DatenSpeicher.appendAsString(ascii.append("\naxisConnection"), _axisConnection.getCode());
    }

    public AxisConnection getAxisConnection() {
        return _axisConnection;
    }

    public void setAxisConnection(final AxisConnection newConnection) {
        if (!_axisConnection.equals(newConnection)) {            
            _axisConnection = newConnection;
            AbstractDataContainer container = _grafer.getDataContainer();
            _diagram.fitYRangesFromGlobalData(container, true);
            _diagram.loadDataFromContainer(container);
        }
    }

    public int getValueDataIndex() {
        return _diagram.getCurves().indexOf(this);
    }

    public CurveSettings getCurveSettings() {
        return _curveSettings;
    }

    abstract void drawCurve(final Graphics2D g2d, final SliderContainer slider);

    @Override
    public void setStroke(final GeckoLineStyle stroke) {
        _curveSettings._curveLineStyle = stroke;
    }

    @Override
    public GeckoLineStyle getStroke() {
        return _curveSettings._curveLineStyle;
    }

    @Override
    public void setTransparency(final float value) {
        _curveSettings._crvTransparency = value;
    }

    @Override
    public float getTransparency() {
        return (float) _curveSettings._crvTransparency;
    }

    @Override
    public GeckoColor getColor() {
        return _curveSettings._curveColor;
    }

    @Override
    public void setColor(final GeckoColor color) {
        _curveSettings._curveColor = color;
    }

    @Override
    public GeckoSymbol getSymbol() {
        return _curveSettings._crvSymbShape;
    }

    @Override
    public void setSymbol(final GeckoSymbol symbol) {
        _curveSettings._crvSymbShape = symbol;
    }

    @Override
    public void setSymbolColor(final GeckoColor color) {
        _curveSettings._crvSymbFarbe = color;
    }

    @Override
    public GeckoColor getSymbolColor() {
        return _curveSettings._crvSymbFarbe;
    }

    @Override
    public void setSymbolEnabled(final boolean value) {
        _curveSettings._curveShowPtSymbols = value;
    }

    @Override
    public boolean isSymbolEnabled() {
        return _curveSettings._curveShowPtSymbols;
    }

    @Override
    public int getSkipNumber() {
        return _curveSettings._crvSymbFrequ;
    }

    @Override
    public void setSkipNumber(final int value) {
        _curveSettings._crvSymbFrequ = value;
    }

//    protected void drawCurveSymbols(final Graphics2D g2d) {
//        if (_curveSettings._kurvenPunktSymbolAnzeigen) {
//            g2d.setColor(_curveSettings._crvSymbFarbe.getJavaColor());
//            for (int i2 = _startIndex; i2 < _noCuvePoints; i2++) {
//                if (i2 % _curveSettings._crvSymbFrequ == 0) {
//                    _curveSettings._crvSymbShape.drawSymbol(g2d, _xPix[i2], _yPix[i2]);
//                }
//            }
//
//            final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
//            g2d.setComposite(alphaComposite);
//
//        }
//    }
    public abstract CurveLabel getCurveLabel();

    String getCurveName() {
        if (_grafer == null || _grafer.getDataContainer() == null) {
            return "no name defined";
        } else {
            return _grafer.getDataContainer().getSignalName(getValueDataIndex());
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + getCurveName();
    }

    void setSelected(final boolean value) {
        _isSelected = value;
    }

    GeckoLineType getLineType() {
        return _curveSettings._lineType;
    }
    
    void setLineType(final GeckoLineType newValue) {
        _curveSettings._lineType = newValue;
    }
}
