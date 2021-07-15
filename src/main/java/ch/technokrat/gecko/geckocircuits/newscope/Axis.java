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
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class Axis {

    private static final int HASH_CONSTANT1 = 3;
    private static final int HASH_CONSTANT2 = 7;
    private final boolean _invertTickDir;
    private static final int DEFAULT_SPACE = 3;
    private static final int AXIS_VISIBLE_SPACE = 5;
    private static final int THREE = 3;
    private static final int LONG_AXIS_THRES = 800;
    private static final int SHORT_AXIS_THRES = 50;
    private static final int POS_TICK_LABELS = 20;  // How far are the tick-labels away from the axis
    private static final int LOG_AXIS_BASE = 10;
    private static final int DIGITS_TO_SHOW = 3;
    private static final int ANZ_AUTO_TICKS = 5;
    private static final int MIN_LOG_TICKS = 3;
    private static final Font FONT_TICK_LABEL = GlobalFonts.foGRAFER;
    private static final TechFormat TECH_FORMAT = new TechFormat();
    private static final int DEF_TICK_SPACING = 100;
    /**
     * this constant is used so that also tick values at the interval border get
     * drawn.
     */
    private static final double MOUSE_WHEEL_ZOOM = 1.1;
    private HiLoData _panningOriginLimits;
    private final AbstractDiagram _diagram;

    public enum Direction {

        X, Y;
    }
    private final Direction _direction;
    final Point _axisOriginPixel = new Point();
    final AxisLimits _axisMinMax = new AxisLimits();
    //public double minimumValue;
    private AxisLinLog _axisType = AxisLinLog.ACHSE_LIN;
    final AxisDesignSettings _axisSettings = new AxisDesignSettings();
    final AxisGridSettings _axisGridSettings = new AxisGridSettings();
    final AxisTickSettings _axisTickSettings = new AxisTickSettings();
    private int _axisLengthPix;
    double _tickSpacing = DEF_TICK_SPACING;  // Abstand zwischen 2 Ticks, ausgehend von Null
    AbstractAxisScale _axisScale;

    /*
     * direction: x or y orthogonalAxis: if this is an x-Axis, give an y-axis.
     */
    public Axis(final Direction direction, final boolean invertTickDirection, final AbstractDiagram diagram) {
        _direction = direction;
        _invertTickDir = invertTickDirection;
        _axisScale = new AxisLin();
        _diagram = diagram;        
    }

    boolean hasIdenticalSettings(final Axis otherAxis) {
        if (_axisType != otherAxis._axisType) {
            return false;
        }

        if (!_axisMinMax.getLimits().equals(otherAxis._axisMinMax.getLimits())) {
            return false;
        }

        return true;
    }

    void copyAxisSettings(final Axis copyFrom) {
        _axisSettings.setStroke(copyFrom._axisSettings.getStroke());
        _axisTickSettings.setTickLengthMaj(copyFrom._axisTickSettings.getTickLengthMaj());
        _axisTickSettings.setTickLengthMin(copyFrom._axisTickSettings.getTickLengthMin());
        _axisTickSettings.setShowLabelsMaj(copyFrom._axisTickSettings.isShowLabelsMaj());
        _axisTickSettings.setShowLabelsMin(copyFrom._axisTickSettings.isShowLabelsMin());
    }

    public int getAxisLengthPixel() {
        return _axisLengthPix;
    }

    public void setAxisLengthPixel(final int positiveValue) {
        assert positiveValue > 0 : "Axis length Pixel should be positive: " + positiveValue;
        _axisLengthPix = positiveValue;
    }

    AxisLinLog getAxisType() {
        return _axisType;
    }

    void setAxisType(final AxisLinLog newType) {
        if (newType.equals(_axisType)) {
            return; // do nothing if no change!
        }
        _axisType = newType;
        switch (_axisType) {
            case ACHSE_LIN:
                _axisScale = new AxisLin();
                break;
            case ACHSE_LOG:
                _axisScale = new AxisLog();
                break;
            default:
                assert false;
                break;
        }
        _axisTickSettings.setAnzTicksMinor(_axisScale.getDefaultNumberMinorTicks());
    }

    abstract class AbstractAxisScale {

        protected HiLoData getLimits() {            
            if (_axisType == AxisLinLog.ACHSE_LOG) {
                _axisMinMax.setNiceScale(false);
                HiLoData accurateLimits = Axis.this._axisMinMax.getLimits();
                return HiLoData.hiLoDataFabric(accurateLimits._yLo, accurateLimits._yHi * 1.5f);
            } else {                                                
                return Axis.this._axisMinMax.getLimits();
            }
        }

        protected abstract double getPixelFromValue(final double value);

        protected abstract double getValueFromPixel(final int xPix);

        final void drawSingleAxis(final Graphics2D g2d, final Axis otherAxis) {
            final List<Tick> majTicks = drawMajorTicks(g2d, otherAxis);
            drawZeroLine(g2d, otherAxis);
            drawMinorTicks(g2d, otherAxis, majTicks);
        }

        protected void drawMinorTicks(final Graphics2D g2D, final Axis otherAxis, final List<Tick> majorTicks) {

            final HiLoData axisMinMax = _axisMinMax.getLimits();            
            final double yTickSpacingMinor = _tickSpacing / _axisTickSettings.getAnzTicksMinor();
            final int yMinorTicksAnzahl = (int) ((axisMinMax.getIntervalRange()) / yTickSpacingMinor) + 2;

            if (yMinorTicksAnzahl <= 0 || majorTicks.size() < 2) {
                return;
            }

            for (int i = 0; i < majorTicks.size() - 1; i++) {
                for (int j = 1; j < _axisTickSettings.getAnzTicksMinor(); j++) {
                    final double value = majorTicks.get(i)._wert + j * (majorTicks.get(i + 1)._wert
                            - majorTicks.get(i)._wert) / _axisTickSettings.getAnzTicksMinor();
                    final Tick minorTick = new Tick(value, _axisTickSettings.getTickLengthMin(), false);
                    minorTick.drawTick(g2D, TECH_FORMAT.formatENG(minorTick._wert, DIGITS_TO_SHOW), _axisTickSettings.isShowLabelsMin());
                    if (_axisGridSettings.isShowGridNormalMinor()) {
                        minorTick.drawGrid(g2D, otherAxis);
                    }
                }

            }
        }

        protected abstract List<Tick> drawMajorTicks(final Graphics2D g2d, final Axis otherAxis);

        protected abstract double getScaleFactor();

        abstract int getDefaultNumberMinorTicks();
    }
    
    static int counter = 0;
    
    private class AxisLog extends AbstractAxisScale {

        private static final int DEF_MIN_TICKS = 10;

        @Override
        protected double getPixelFromValue(final double value) {                        
            double positiveValue = value;
            if (positiveValue <= 0) {
                positiveValue = Float.MIN_VALUE;
            }
            
            return getSignDirection() * getScaleFactor() * Math.log10(positiveValue / getLimits()._yLo)
                    + getDirectionOrigin();
        }

                
        @Override
        protected double getValueFromPixel(final int xPix) {                                    
            return getLimits()._yLo * Math.pow(LOG_AXIS_BASE,
                    getSignDirection() * (xPix - getDirectionOrigin()) / getScaleFactor());
        }

        @Override
        protected void drawMinorTicks(Graphics2D g2d, Axis otherAxis, List<Tick> majorTicks) {
            if (majorTicks.size() > 2) {                
                if(minorTicksNotTooClose(majorTicks)) {
                    super.drawMinorTicks(g2d, otherAxis, majorTicks); 
                }                
            } else {
                final int noTicks = 5;
                double upperValue = getLimits()._yHi;
                double lowerValue = getLimits()._yLo;
                double distance = (upperValue - lowerValue) / (noTicks + 1);
                
                double tickValue = lowerValue;
                for(int i = 0; i < noTicks; i++) {
                    tickValue += distance;
                    Tick newTick = new Tick(tickValue, (int) _axisTickSettings.getTickLengthMin(), false);
                    newTick.drawTick(g2d, TECH_FORMAT.formatT(newTick._wert, "#.#E0"),
                                true);
                }
                
            }

        }

        @Override
        protected double getScaleFactor() {
            double yLo = getLimits()._yLo;
            if (yLo <= 0.0) {
                yLo = Float.MIN_VALUE;
            }
            return _axisLengthPix / Math.log10(getLimits()._yHi / yLo);
        }

        @Override
        int getDefaultNumberMinorTicks() {
            return DEF_MIN_TICKS;
        }

        protected List<Tick> drawMajorTicks(final Graphics2D g2d, final Axis otherAxis) {
            List<Tick> returnValue = new ArrayList<Tick>();
            int anzTicks = (int) Math.round(Math.log10(getLimits()._yHi / getLimits()._yLo)) + MIN_LOG_TICKS;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                final double wert = Math.pow(10, ((int) Math.log10(getLimits()._yLo) - 1 + i2));
                final int tick = (int) getPixelFromValue(wert);
                if ((getLimits()._yLo <= wert) && (wert <= getLimits()._yHi)) {
                    Tick newTick = new Tick(wert, tick, _axisTickSettings.getTickLengthMaj(), true);
                    returnValue.add(newTick);
                    newTick.drawTick(g2d, TECH_FORMAT.formatT(newTick._wert, "#.E0"),
                            _axisTickSettings.isShowLabelsMaj());
                    if (_axisGridSettings.isShowGridNormalMajor()) {
                        newTick.drawGrid(g2d, otherAxis);
                    }
                }
            }
            return returnValue;
        }

        private static final int MINOR_LOG_TICK_MIN_DIST = 25;
        
        private boolean minorTicksNotTooClose(List<Tick> majorTicks) {
            return majorTicks.size() < _axisLengthPix / MINOR_LOG_TICK_MIN_DIST;
        }
    }

    private class AxisLin extends AbstractAxisScale {

        @Override
        protected double getPixelFromValue(final double value) {
            double signalDirection = getSignDirection();
            HiLoData limits = getLimits();
            double scaleFactor = getScaleFactor();
            double directionOrigin = getDirectionOrigin();
            HiLoData newLimits = getLimits();
            double scaleFactor2 = getScaleFactor();
            if (!limits.equals(newLimits) || scaleFactor != scaleFactor2) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Axis.class.getName()).log(Level.SEVERE, null, ex);
                }
                //System.out.println("correktur. " + scaleFactor + " " + scaleFactor2 + " " + limits + " " + newLimits);
                double scaleFactor3 = getScaleFactor();
                HiLoData limits3 = getLimits();
                if (scaleFactor2 == scaleFactor3) {
                    scaleFactor = scaleFactor3;
                    limits = limits3;
                } else if (scaleFactor == scaleFactor3) {
                    scaleFactor = scaleFactor;
                    limits = limits;
                } else if (scaleFactor == scaleFactor2) {
                    scaleFactor = scaleFactor;
                    limits = limits;
                }
            }
            double returnValue = signalDirection * (value - limits._yLo) * scaleFactor + directionOrigin;
            return returnValue;
        }

        @Override
        protected double getValueFromPixel(final int xPix) {
            return getLimits()._yLo + getSignDirection() * (xPix - getDirectionOrigin()) / getScaleFactor();
        }

        @Override
        protected double getScaleFactor() {
            return _axisLengthPix / getLimits().getIntervalRange();
        }

        @Override
        int getDefaultNumberMinorTicks() {
            return 2;
        }

        protected List<Tick> drawMajorTicks(final Graphics2D g2D, final Axis otherAxis) {
            final HiLoData axisMinMax = _axisMinMax.getLimits();
            int anzTicks = (int) (axisMinMax._yHi / _tickSpacing) - (int) (axisMinMax._yLo / _tickSpacing) + 2;
            //int anzTicks = (int) (getLimits()._yHi / _tickSpacing) - (int) (getLimits()._yLo / _tickSpacing) + 2;
            List<Tick> majorTicks = new ArrayList<Tick>();
            for (int i2 = 0; i2 < anzTicks; i2++) {
                final double wert = _tickSpacing * (int) (axisMinMax._yLo / _tickSpacing) + i2 * _tickSpacing;
                final int tick = (int) getPixelFromValue(wert);
                if ((axisMinMax._yLo <= wert) && (wert <= axisMinMax._yHi)) {
                    Tick newTick = new Tick(wert, tick, _axisTickSettings.getTickLengthMaj(), true);
                    majorTicks.add(newTick);
                    newTick.drawTick(g2D, TECH_FORMAT.formatENG(newTick._wert, DIGITS_TO_SHOW), _axisTickSettings.isShowLabelsMaj());
                    if (_axisGridSettings.isShowGridNormalMajor()) {
                        newTick.drawGrid(g2D, otherAxis);
                    }
                }
            }

            return majorTicks;
        }
    }

    private double getDirectionOrigin() {
        switch (_direction) {
            case X:
                return _axisOriginPixel.x;
            case Y:
                return _axisOriginPixel.y;
            default:
                assert false;
                return 0;
        }
    }

    private double getSignDirection() {
        switch (_direction) {
            case X:
                return 1.0;
            case Y:
                return -1.0;
            default:
                assert false;
                return 0;
        }
    }

    protected double getPixelFromValue(final double value) {
        return _axisScale.getPixelFromValue(value);
    }

    protected double getValueFromPixel(final int xPix) {
        return _axisScale.getValueFromPixel(xPix);
    }

    public double getAutoTickSpacing() {
        if (_axisLengthPix < SHORT_AXIS_THRES) {
            return _axisMinMax.getLimits().getIntervalRange() / 2;
        }

        final double returnValue = (_axisMinMax.getLimits().getIntervalRange()) / ANZ_AUTO_TICKS;

        if (_axisLengthPix > LONG_AXIS_THRES) {
            return returnValue / 2;
        }
        return returnValue;
    }

    private void drawZeroLine(final Graphics2D g2d, final Axis otherAxis) {
        final HiLoData axisMinMax = _axisMinMax.getLimits();

        if (axisMinMax._yLo > 0 || axisMinMax._yHi < 0) {
            return;
        }
        g2d.setColor(AxisDesignSettings.ZERO_LINE_COL.getJavaColor());
        final Stroke oldStroke = g2d.getStroke();
        AxisDesignSettings.ZERO_LINE_STYLE.setStrokeStyle(g2d);
        GRL.reset();
        final int pixValue = (int) getPixelFromValue(0);


        if (_direction == Direction.X) {
            GRL.moveTo(pixValue, otherAxis._axisOriginPixel.y);
            GRL.lineTo(pixValue, otherAxis._axisOriginPixel.y - otherAxis._axisLengthPix);
        } else {
            GRL.moveTo(otherAxis._axisOriginPixel.x, pixValue);
            GRL.lineTo(otherAxis._axisOriginPixel.x + otherAxis._axisLengthPix, pixValue);
        }
        g2d.draw(GRL);
        g2d.setStroke(oldStroke);
    }

    public void drawAxis(final Graphics2D g2d, final boolean isSignalAxis, final Axis otherAxis) {
        _axisGridSettings.blendeEventuellGridLinienAus(_axisLengthPix);
        g2d.setFont(FONT_TICK_LABEL);
        if (_axisTickSettings.isAutoTickSpacing()) {
            _tickSpacing = getAutoTickSpacing();
        }

        if (isSignalAxis) {
            drawSingleSignalAxisY(g2d);
        } else {
            _axisScale.drawSingleAxis(g2d, otherAxis);
        }


        drawAxisLine(g2d);
        drawAxisLabel(g2d);
    }

    public long getAxisHash() {
        long returnValue = Double.doubleToLongBits(_axisScale.getScaleFactor()) * HASH_CONSTANT2;
        returnValue += _axisLengthPix;
        final HiLoData axisMinMax = _axisMinMax.getLimits();
        returnValue += Double.doubleToLongBits(axisMinMax._yLo)
                - HASH_CONSTANT1 * Double.doubleToLongBits(axisMinMax._yHi);
        return returnValue;
    }

    private void drawSingleSignalAxisY(final Graphics2D g2d) {
        _axisSettings.getStroke().setStrokeStyle(g2d);
        GRL.reset();
        GRL.moveTo(_axisOriginPixel.x, _axisOriginPixel.y);
        GRL.lineTo(_axisOriginPixel.x, _axisOriginPixel.y - _axisLengthPix);
        g2d.draw(GRL);
        g2d.setStroke(GeckoLineStyle.SOLID_PLAIN.stroke());
    }
    private static final GeneralPath GRL = new GeneralPath();

    private final class Tick {

        private final double _wert;
        private final int _pixelValue;
        private final int _tickLength;
        private final boolean _isMajor;

        public Tick(final double newValue, final int newPixel, final int tickLength, final boolean isMajor) {
            _wert = newValue;
            this._pixelValue = newPixel;
            _tickLength = tickLength;
            _isMajor = isMajor;
        }

        public Tick(final double newValue, final int tickLength, final boolean isMajor) {
            _wert = newValue;
            _pixelValue = (int) getPixelFromValue(_wert);
            _tickLength = tickLength;
            _isMajor = isMajor;
        }

        public void drawTick(final Graphics2D g2d, final String label, final boolean zeigeLabels) {
            g2d.setColor(_axisSettings.getColor().getJavaColor());
            int distance = _tickLength;
            if (!_invertTickDir) {
                distance = -distance;
            }
            if (_direction == Direction.Y) {
                g2d.drawLine(_axisOriginPixel.x, _pixelValue, _axisOriginPixel.x + distance, _pixelValue);
            } else {
                g2d.drawLine(_pixelValue, _axisOriginPixel.y, _pixelValue, _axisOriginPixel.y - distance);
            }


            if (zeigeLabels) {
                if (_direction == Direction.Y) {

                    if (_invertTickDir) {
                        g2d.drawString(label, _axisOriginPixel.x + (_axisTickSettings.getTickLengthMaj() + THREE),
                                _pixelValue + FONT_TICK_LABEL.getSize() / 2 - 2);
                    } else {
                        g2d.drawString(label, _axisOriginPixel.x - (_axisTickSettings.getTickLengthMaj() + THREE)
                                - (int) g2d.getFontMetrics().getStringBounds(label,
                                g2d).getWidth(), _pixelValue + FONT_TICK_LABEL.getSize() / 2 - 2);
                    }
                } else { // direction X
                    final int centerOffset = (int) g2d.getFontMetrics().getStringBounds(label, g2d).getWidth() / 2;
                    g2d.drawString(label, _pixelValue - centerOffset, _axisOriginPixel.y
                            + _axisTickSettings.getTickLengthMaj() + THREE + FONT_TICK_LABEL.getSize());
                }
            }
        }

        private void drawGrid(final Graphics2D g2d, final Axis otherAxis) {
            if (_isMajor) {
                g2d.setColor(_axisGridSettings.getColorGridMaj().getJavaColor());
                _axisGridSettings.getLinStyleMaj().setStrokeStyle(g2d);
            } else {
                g2d.setColor(_axisGridSettings.getColorGridMin().getJavaColor());
                _axisGridSettings.getLinStyleMin().setStrokeStyle(g2d);
            }
            GRL.reset();
            if (_direction == Direction.X) {
                GRL.moveTo(_pixelValue, otherAxis._axisOriginPixel.y);
                GRL.lineTo(_pixelValue, otherAxis._axisOriginPixel.y - otherAxis._axisLengthPix);
            } else {
                GRL.moveTo(otherAxis._axisOriginPixel.x, _pixelValue);
                GRL.lineTo(otherAxis._axisOriginPixel.x + otherAxis._axisLengthPix, _pixelValue);
            }
            g2d.draw(GRL);
        }
    };

    void drawAxisLabel(final Graphics2D g2d) {
        switch (_direction) {
            case X:
                g2d.drawString(_axisSettings.getAchseBeschriftung(), _axisOriginPixel.x
                        + _axisLengthPix / 2, _axisOriginPixel.y + POS_TICK_LABELS);
                break;
            case Y:
                g2d.drawString(_axisSettings.getAchseBeschriftung(), _axisOriginPixel.x - POS_TICK_LABELS,
                        _axisOriginPixel.y - _axisLengthPix / 2);
                break;
            default:
                assert false;
        }
    }

    void drawAxisLine(final Graphics2D g2d) {
        if (_axisSettings.getStroke() == GeckoLineStyle.INVISIBLE) {
            return;
        }

        final GeneralPath grL = new GeneralPath();
        g2d.setColor(_axisSettings.getColor().getJavaColor());

        _axisSettings.getStroke().setStrokeStyle(g2d);

        // jetzt die Linie ziehen:
        grL.reset();
        grL.moveTo(_axisOriginPixel.x, _axisOriginPixel.y);

        switch (_direction) {
            case X:
                grL.lineTo(_axisOriginPixel.x + _axisLengthPix, _axisOriginPixel.y);
                break;
            case Y:
                grL.lineTo(_axisOriginPixel.x, _axisOriginPixel.y - _axisLengthPix);
                break;
            default:
                assert false;
        }

        g2d.draw(grL);
    }

    /**
     * at the moment only used for x-Axis
     *
     * @return
     */
    int getRequiredAxisSpace() {
        int returnValue = DEFAULT_SPACE;
        if (this._axisTickSettings.isShowLabelsMaj()) {
            returnValue += _axisTickSettings.getTickLengthMaj();
            returnValue += FONT_TICK_LABEL.getSize();
            returnValue += AXIS_VISIBLE_SPACE;
        }
        return returnValue;
    }

    void setAxisInvisible() {
        _axisSettings.setStroke(GeckoLineStyle.INVISIBLE);
        _axisTickSettings.setTickLengthMaj(0);
        _axisTickSettings.setTickLengthMin(0);
        _axisTickSettings.setShowLabelsMaj(false);
        _axisTickSettings.setShowLabelsMin(false);
    }

    void importASCII(final TokenMap axisMap) {
        setAxisType(AxisLinLog.getFromCode(axisMap.readDataLine("axisType", _axisType.getCode())));
        _axisTickSettings.importASCII(axisMap);
        _axisGridSettings.importASCII(axisMap);
        _axisSettings.importASCII(axisMap);
        _axisMinMax.importASCII(axisMap);        
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\naxisType"), _axisType.getCode());
        _axisTickSettings.exportIndividualCONTROL(ascii);
        _axisGridSettings.exportIndividualCONTROL(ascii);
        _axisSettings.exportIndividualCONTROL(ascii);
        _axisMinMax.exportIndividualCONTROL(ascii);
    }

    void doMouseWheelZoom(final int mousePointCoordinate, final int wheelRotation) {
        final HiLoData oldRangeY = _axisMinMax.getLimits();
        final HiLoData dataAxisLimits = _axisMinMax.getAutoScaleGlobal();
        final double centerPoint = getValueFromPixel(mousePointCoordinate);

        double lowerValue = oldRangeY._yLo - centerPoint;
        double higherValue = oldRangeY._yHi - centerPoint;

        if (wheelRotation < 0) {
            lowerValue *= MOUSE_WHEEL_ZOOM;
            higherValue *= MOUSE_WHEEL_ZOOM;
        } else {
            lowerValue /= MOUSE_WHEEL_ZOOM;
            higherValue /= MOUSE_WHEEL_ZOOM;
        }

        lowerValue += centerPoint;
        higherValue += centerPoint;
        lowerValue = Math.max(lowerValue, dataAxisLimits._yLo);
        higherValue = Math.min(higherValue, dataAxisLimits._yHi);

        final HiLoData newRange = HiLoData.hiLoDataFabric((float) lowerValue, (float) higherValue);
        _axisMinMax.setZoomValues(newRange, false);
    }

    void setPanningOriginLimits() {
        _panningOriginLimits = _axisMinMax.getLimits();
    }

    void doPanning(final int mousePoint, final int panningStartPoint) {
        final HiLoData originLimits = _panningOriginLimits;
        final double mouseXValue = getValueFromPixel(mousePoint);
        final double originXValue = getValueFromPixel(panningStartPoint);
        final double shiftValue = mouseXValue - originXValue;
        final HiLoData newLimits = HiLoData.hiLoDataFabric(originLimits._yLo - (float) shiftValue,
                originLimits._yHi - (float) shiftValue);

        _axisMinMax.setZoomValues(newLimits, false);

    }
}
