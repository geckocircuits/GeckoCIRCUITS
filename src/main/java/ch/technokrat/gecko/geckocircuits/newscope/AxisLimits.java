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
import java.util.Stack;

/**
 *
 * @author andy
 */
final class AxisLimits {

    /**
     * this are the global values that actually determine the current axis view
     */
    private HiLoData _autoScaleGlobal = HiLoData.hiLoDataFabric(0, 1f);
    /**
     * when zooming one diagram, the view for the other diagrams could be
     * autoscaled depending on the local view data.
     */
    private HiLoData _autoScaleLocal;
    private Clipping _clipping = Clipping.GLOBAL_AUTO;
    /**
     * user can save and load some settings!
     */
    private HiLoData _savedLimits;
    /**
     * user can zoom....
     */
    private HiLoData _zoomedLimits;
    /*
     * these limits are shown when autoscale is disabled. I did that on purpose that here we don't use the user-saved values.
     * ValueScaleLocal is the value currently visible in the scope. When somebode switches from auto-scale to non-autoscale,
     * this value is used.
     */
    private HiLoData _valueScaleLocal = HiLoData.hiLoDataFabric(0, 1);
    private HiLoData _userScale = HiLoData.hiLoDataFabric(0, 0);
    private boolean _isAutoEnabled = true;
    private boolean _niceScale = false;
    private boolean _isCommonZero = false;
    /**
     * storage for "undo-zoom" /zoom back
     */
    private final Stack<HiLoData> _HistoryStack = new Stack<HiLoData>();

    public Clipping getClipping() {
        return _clipping;
    }

    public boolean isAutoEnabled() {
        return _isAutoEnabled;
    }

    public void setAutoEnabled(final boolean value) {

        if (!value) {
            if (_userScale == HiLoData.hiLoDataFabric(0, 0)) {
                _valueScaleLocal = getLimits();
            } else {
                _valueScaleLocal = _userScale;
            }
        }
        _isAutoEnabled = value;
    }

    public HiLoData getLimits() {
        if (!_isAutoEnabled) {
            return _valueScaleLocal;
        }

        switch (_clipping) {
            case GLOBAL_AUTO:
                if (_niceScale) {
                    final NiceScale niceScaleGen = new NiceScale(_autoScaleGlobal);
                    return niceScaleGen.getNiceLimits();

                } else {
                    return _autoScaleGlobal;
                }
            case LOCAL_AUTO:
                if (_niceScale) {
                    final NiceScale niceScaleGen = new NiceScale(_autoScaleLocal);
                    return niceScaleGen.getNiceLimits();
                } else {
                    return _autoScaleLocal;
                }

            case ZOOMED:
                return _zoomedLimits;
            default:
                assert false;
                break;
        }
        assert false;
        return null;
    }

    public HiLoData getAutoScaleGlobal() {
        return _autoScaleGlobal;
    }

    private void pushHistoryStack() {
        // check if we are not pushing identical values:
        if (!_HistoryStack.isEmpty()) {
            final HiLoData oldLimits = _HistoryStack.pop();
            _HistoryStack.push(oldLimits);
            if (getLimits().compare(oldLimits)) {
                return;
            }
        }

        _HistoryStack.push(getLimits());
    }

    public void popHistoryStack() {
        if (_HistoryStack.isEmpty()) {
            return;
        }

        _zoomedLimits = _HistoryStack.pop();
        _valueScaleLocal = _zoomedLimits;
        _clipping = Clipping.ZOOMED;
    }

    public void saveValues() {
        _savedLimits = getLimits();
    }

    public void loadFromSaved() {
        pushHistoryStack();
        _zoomedLimits = _savedLimits;
        _valueScaleLocal = _zoomedLimits;
        _clipping = Clipping.ZOOMED;
    }

    public void setZoomValues(final HiLoData minMax, final boolean niceScale) {
        final HiLoData newLimits = minMax;
        // --- this is just done for pushing
        _zoomedLimits = getLimits();

        pushHistoryStack();
        _clipping = Clipping.ZOOMED;
        // ---

        if (niceScale) {
            final NiceScale niceScaleGen = new NiceScale(newLimits);
            _zoomedLimits = niceScaleGen.getNiceLimits();
        } else {
            _zoomedLimits = newLimits;
        }

        _valueScaleLocal = _zoomedLimits;
    }

    public void setNiceScale(final boolean value) {
        _niceScale = value;
    }

    /*
     * Set the minimum-maximum values of the axis. This function only sets the number,
     * but does not immediately do the "global fit"
     */
    public void setGlobalAutoScaleValues(final HiLoData newGlobAutoScale) {                        
        try {
            assert newGlobAutoScale.isValidNumber() : newGlobAutoScale;
        } catch (AssertionError err) {
            err.printStackTrace();
        }

        if (_isCommonZero) {
            float maxLimit = Math.max(Math.abs(newGlobAutoScale._yLo), Math.abs(newGlobAutoScale._yHi));
            _autoScaleGlobal = HiLoData.hiLoDataFabric(-maxLimit, maxLimit);
        } else {
            _autoScaleGlobal = newGlobAutoScale;
        }
                
    }

    public void setLocalAutoScaleValues(final HiLoData newAutoScale) {
        _autoScaleLocal = newAutoScale;
    }

    public void setValueScaleLocal(final HiLoData newValueScale) {
        _valueScaleLocal = newValueScale;
        _userScale = newValueScale;
    }

    public void setUserScale(final HiLoData newUserScale) {
        _userScale = newUserScale;
    }

    @Override
    public String toString() {
        return "show values: " + _clipping + " " + getLimits()
                + " autoscale: " + _autoScaleGlobal + " " + _autoScaleLocal;

    }

    public void globalFit() {
        _valueScaleLocal = _autoScaleGlobal;
        _clipping = Clipping.GLOBAL_AUTO;
    }

    public void setLocalFit() {
        _clipping = Clipping.LOCAL_AUTO;
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\nisAutoEnabled"), _isAutoEnabled);
        DatenSpeicher.appendAsString(ascii.append("\nisUserScale"), _userScale);
        DatenSpeicher.appendAsString(ascii.append("\nisSymmetricZero"), _isCommonZero);
        DatenSpeicher.appendAsString(ascii.append("\nvalueScale"), _valueScaleLocal);
    }

    void importASCII(final TokenMap axisMap) {
        _isAutoEnabled = axisMap.readDataLine("isAutoEnabled", _isAutoEnabled);
        _userScale = axisMap.readDataLine("isUserScale", _userScale);
        _valueScaleLocal = axisMap.readDataLine("valueScale", _valueScaleLocal);

//        if(!_isAutoEnabled)  {
//            if(_userScale._yHi != _userScale._yLo) {
//                _valueScaleLocal = _userScale;
//            }            
//        }
        if (axisMap.containsToken("isSymmetricZero")) {
            _isCommonZero = axisMap.readDataLine("isSymmetricZero", _isCommonZero);
        }
    }

    boolean isCommonZero() {
        return _isCommonZero;
    }

    void setCommonZero(boolean value) {
        _isCommonZero = value;
    }
}
