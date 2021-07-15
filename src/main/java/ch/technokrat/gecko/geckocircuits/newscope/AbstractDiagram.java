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
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerNullData;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author andy
 */
public abstract class AbstractDiagram extends JPanel {

    public final DiagramSettings _diagramSettings;
    protected Axis _xAxis = new Axis(Axis.Direction.X, false, this);
    protected Axis _yAxis1 = new Axis(Axis.Direction.Y, false, this);
    protected Axis _yAxis2 = new Axis(Axis.Direction.Y, true, this);
    private List<AbstractCurve> _curves = Collections.unmodifiableList(new ArrayList<AbstractCurve>());
    protected int _ySpaceUpper = DY_IN_UPPER_SMALL;
    static final int DY_IN_UNTEN = 8;
    static final int DX_IN_LINKS = 70;
    private static final int DX_IN_RIGHT_DEF = 20;
    private static final int DX_IN_WITH_Y2 = 80;
    private static final int LABEL_WIDTH = 85;
    int _dxInRight = DX_IN_RIGHT_DEF;
    private static final int DY_IN_UPPER_SMALL = 8;
    private static final int DY_IN_UPPER_LARGE = 16;
    private static final int THREE = 3;
    GraferV4 _grafer;
    final ZoomWindow _zoomWindow = new ZoomWindow(this);
    private int _topOffset;
    private int _bottomOffset;
    static final int DEF_PREF_WIDTH = 3000;
    private static final int DEF_TOP_OFFSET = 7;
    private static final int TITLE_OFFSET = 10;
    private final HorizontalLevel _ylevel = new HorizontalLevel(Color.darkGray, this);
    private final TriggerPosition _triggerPrositon = new TriggerPosition(Color.darkGray, this);
    private boolean _isProbeUtilsEnabled = false;

    static AbstractDiagram fabricFromModelFile(final TokenMap diagramMap, final GraferV4 grafer) {
        String diagramTypeString = "";
        diagramTypeString = diagramMap.readDataLine("diagramType", diagramTypeString);

        AbstractDiagram returnValue;
        returnValue = new DiagramCurve(grafer); // default-diagram                               

        final TokenMap diagramSettingsMap = diagramMap.getBlockTokenMap("<diagramSettings>");
        if (diagramSettingsMap != null) {
            returnValue._diagramSettings.importASCII(diagramSettingsMap);
        }

        final TokenMap xAxisMap = diagramMap.getBlockTokenMap("<xAxis>");
        if (xAxisMap != null) {
            returnValue._xAxis.importASCII(xAxisMap);
        }

        final TokenMap yAxis1Map = diagramMap.getBlockTokenMap("<yAxis1>");
        if (yAxis1Map != null) {
            returnValue._yAxis1.importASCII(yAxis1Map);
        }

        final TokenMap yAxis2Map = diagramMap.getBlockTokenMap("<yAxis2>");
        if (yAxis2Map != null) {
            returnValue._yAxis2.importASCII(yAxis2Map);
        }

        if (diagramTypeString.equals(DiagramSignal.DIAGRAM_TYPE_STRING)) {
            returnValue = new DiagramSignal(returnValue);
        }
        return returnValue;
    }

    public void populateCurveProperties(final TokenMap diagramMap, final GraferV4 grafer) {
        int counter = 0;
        for (TokenMap curveMap = diagramMap.getBlockTokenMap("<CurveDiagram>"); curveMap != null;
                curveMap = diagramMap.getBlockTokenMap("<CurveDiagram>"), counter++) {
            _grafer.getManager().updateCurveNumber(Math.max(counter + 1, _curves.size()));
            _curves.get(counter).populatePropertiesFromModelFile(curveMap, this);
        }

    }

    AbstractDiagram(final GraferV4 grafer, final DiagramSettings diagramSettings) {
        super();        
        this.setBorder(null);
        _diagramSettings = diagramSettings;
        _yAxis1._axisMinMax.setNiceScale(true);
        _yAxis2._axisMinMax.setNiceScale(true);
        _grafer = grafer;

        _labelPanel.setOpaque(false);
        addMouseListeners();
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent evt) {                
                loadDataFromContainer(_grafer.getDataContainer());
            }
        });

        this.setOpaque(false);
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {

        DatenSpeicher.appendAsString(ascii.append("\ndiagramType"), getDiagramTypeString());
        ascii.append("\n<xAxis>");
        _xAxis.exportIndividualCONTROL(ascii);
        ascii.append("\n<\\xAxis>\n");
        ascii.append("<yAxis1>\n");
        _yAxis1.exportIndividualCONTROL(ascii);
        ascii.append("\n<\\yAxis1>\n");

        ascii.append("<yAxis2>\n");
        _yAxis2.exportIndividualCONTROL(ascii);
        ascii.append("\n<\\yAxis2>\n");

        ascii.append("<diagramSettings>\n");
        _diagramSettings.exportIndividualCONTROL(ascii);
        ascii.append("\n<\\diagramSettings>\n");

        for (AbstractCurve curve : _curves) {
            ascii.append("<CurveDiagram>\n");
            curve.exportIndividualCONTROL(ascii);
            ascii.append("\n<\\CurveDiagram>\n");
        }

    }            

    public void setAllCurvesWithBars(final int[] indices) {
        for(int index : indices) {
            AbstractCurve curve = _curves.get(index);            
            curve._curveSettings._lineType = GeckoLineType.BAR;
        }
    }
    

    private class LabelPanel extends JPanel {

        LabelPanel() {
            super();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent mouseEvent) {
                    for (AbstractCurve curve : _curves) {
                        if (curve.getAxisConnection() != AxisConnection.ZUORDNUNG_NIX
                                && curve.getCurveLabel().isInSelectionWindow(mouseEvent)) {
                            _grafer.setSelectedCurve(curve);
                        }
                    }
                }
            });

        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(LABEL_WIDTH, 0);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(LABEL_WIDTH, Integer.MAX_VALUE);
        }

        @Override
        protected void paintComponent(final Graphics graphics) {            
            final Graphics2D g2d = (Graphics2D) graphics;
            super.paintComponent(graphics);

            for (AbstractCurve curve : _curves) {
                if (curve.getAxisConnection() != AxisConnection.ZUORDNUNG_NIX) {
                    curve.getCurveLabel().drawLabel(g2d);
                }
            }
            
            graphics.setFont(GlobalFonts.foGRAFER);
            graphics.setColor(_xAxis._axisSettings.getColor().getJavaColor());
            if(_grafer._manager.getDiagrams().get(_grafer._manager.getDiagrams().size()-1) == AbstractDiagram.this) {
                graphics.drawString(_grafer.xAxisLabel, 0, this.getHeight() - graphics.getFontMetrics().getHeight()/2+1);
            }
            
        }
    };

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DEF_PREF_WIDTH, getHeight());
    }
    private final LabelPanel _labelPanel = new LabelPanel();

    public JPanel getLabelPanel() {
        return _labelPanel;
    }

    private void drawBorder(final Graphics2D graphics) {
        graphics.setColor(Color.lightGray);
        graphics.drawRect(_xAxis._axisOriginPixel.x, _xAxis._axisOriginPixel.y - _yAxis1.getAxisLengthPixel(),
                _xAxis.getAxisLengthPixel(), _yAxis1.getAxisLengthPixel());
    }

    /**
     *
     * @param worksheetDaten
     * @param niceScale
     * @return value can be used to detect if a redraw has to be performed or
     * not, e.g. when the axis limits where not changed, the axis does not have
     * to be redrawn.
     */
    public boolean fitYRangesFromGlobalData(final AbstractDataContainer worksheetDaten, final boolean niceScale) {
        final long hash = _yAxis1.getAxisHash() + _xAxis.getAxisHash() + _yAxis2.getAxisHash();
        calculateAutoScaleYBothYAxis(worksheetDaten);
        _yAxis1._axisMinMax.setNiceScale(niceScale);
        _yAxis2._axisMinMax.setNiceScale(niceScale);
        _yAxis1._axisMinMax.globalFit();
        _yAxis2._axisMinMax.globalFit();

        return hash != _yAxis1.getAxisHash() + _xAxis.getAxisHash() + _yAxis2.getAxisHash();
    }

    private void drawCoordinateAxis(final Graphics2D g2d) {
        _xAxis.drawAxis(g2d, false, _yAxis1);
        _yAxis1.drawAxis(g2d, this instanceof DiagramSignal, _xAxis);

        if (checkForY2Axis()) {
            _yAxis2._axisOriginPixel.x = _yAxis1._axisOriginPixel.x + _xAxis.getAxisLengthPixel() - 1;
            _yAxis2.drawAxis(g2d, this instanceof DiagramSignal, _xAxis);
        }

        g2d.setStroke(GeckoLineStyle.SOLID_PLAIN.stroke());  // wieder auf 'default' setzen                                    
    }

    public boolean checkForY2Axis() {
        boolean hasAnyY2Axis = false;
        for (AbstractCurve curve : _curves) {
            if (curve.getAxisConnection() == AxisConnection.ZUORDNUNG_Y2) {
                hasAnyY2Axis = true;
            }
        }
        return hasAnyY2Axis;
    }

    @Override
    public String getName() {
        if (_diagramSettings == null) {
            return "no name defined";
        } else {
            return _diagramSettings.getNameDiagram();
        }
    }

    private void drawProbeUtilities(Graphics2D g2d) {
        if (_isProbeUtilsEnabled) {
            _ylevel.paintComponent(g2d, _xAxis, _yAxis1);
            _triggerPrositon.paintComponent(g2d, _xAxis, _yAxis1);
            _triggerPrositon.setXPos(0.0);
        }
    }

    public void setLevelEnabled(final boolean state) {
        _isProbeUtilsEnabled = state;
    }

    public boolean isLevelEnabled() {
        return _isProbeUtilsEnabled;
    }

    public HorizontalLevel getHorizontalLevel() {
        return _ylevel;
    }

    public List<AbstractCurve> getCurves() {
        return _curves;
    }

    public AbstractCurve getCurve(final int index) {
        return _curves.get(index);
    }

    public void setCurves(final List<AbstractCurve> newCurves) {        
        _curves = newCurves;                
    }

    public void setAxisPositions(final boolean anyY2Axis) {                
        if (getWidth() == 0) {            
            return;
        }

        if (_diagramSettings.getNameDiagram().startsWith("GRF") || _diagramSettings.getNameDiagram().isEmpty()) {
            _ySpaceUpper = DY_IN_UPPER_SMALL;
        } else {
            _ySpaceUpper = DY_IN_UPPER_LARGE;
        }

        if (anyY2Axis) {
            _dxInRight = DX_IN_WITH_Y2;
        } else {
            _dxInRight = DX_IN_RIGHT_DEF;
        }
        
        _xAxis.setAxisLengthPixel(getWidth() - (DX_IN_LINKS + _dxInRight));

        _xAxis._axisOriginPixel.x = DX_IN_LINKS;
        _yAxis1._axisOriginPixel.x = DX_IN_LINKS;
        final int yPixelValue = getHeight() - _xAxis.getRequiredAxisSpace();
        _yAxis1._axisOriginPixel.y = yPixelValue;
        _yAxis2._axisOriginPixel.y = yPixelValue;
        _xAxis._axisOriginPixel.y = yPixelValue;



        _bottomOffset = getHeight() - _yAxis1._axisOriginPixel.y;
        _topOffset = DEF_TOP_OFFSET;
        if (isDrawTitle()) {
            _topOffset += TITLE_OFFSET;
        }
        _yAxis1.setAxisLengthPixel(getHeight() - _bottomOffset - _topOffset);
        _yAxis2.setAxisLengthPixel(_yAxis1.getAxisLengthPixel());

    }

    public int getTopOffset() {
        return _topOffset;
    }

    public int getBottomOffset() {
        return _bottomOffset;
    }

    private HiLoData calculateAutoScaleYMinMax(final AxisConnection axisConnection,
            final AbstractDataContainer worksheetDaten) {
        
        if (worksheetDaten == null) {
            return HiLoData.hiLoDataFabric(-1, 1);
        }
        HiLoData minMaxValue = null;
        for (AbstractCurve curve : _curves) {
            final int index = _curves.indexOf(curve);
            if (curve.getAxisConnection() == axisConnection) {
                final HiLoData addValue = worksheetDaten.getAbsoluteMinMaxValue(index);
                assert addValue != null : index;                
                minMaxValue = HiLoData.merge(addValue, minMaxValue);                
            }
        }
        return minMaxValue;
    }

    private void calculateAutoScaleYBothYAxis(final AbstractDataContainer worksheetDaten) {
        final HiLoData minMaxValue1 = calculateAutoScaleYMinMax(AxisConnection.ZUORDNUNG_Y, worksheetDaten);
        final HiLoData minMaxValue2 = calculateAutoScaleYMinMax(AxisConnection.ZUORDNUNG_Y2, worksheetDaten);

        if (minMaxValue1 != null) {            
            _yAxis1._axisMinMax.setGlobalAutoScaleValues(minMaxValue1);
        }

        if (minMaxValue2 != null) {
            _yAxis2._axisMinMax.setGlobalAutoScaleValues(minMaxValue2);                        
        }

    }
       

    public void loadDataFromContainer(final AbstractDataContainer container) {
        boolean forceLoad;
        if (container == null || container instanceof DataContainerNullData) {
            return;
        }

        if (container.getMaximumTimeIndex(0) > 0) {
            calculateAutoScaleYBothYAxis(container);
        }
        
        for (int i = 0; i < this._curves.size(); i++) {
            AbstractCurve curve = this._curves.get(i);
            if (curve.getAxisConnection() != AxisConnection.ZUORDNUNG_NIX) {
                forceLoad = (this._grafer._manager.getAllScopeSignals().get(curve.getValueDataIndex()) instanceof ExternalSignal);
                try {                    
                    curve._curvePainter.loadRequiredData(container, forceLoad);                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void paintComponent(final Graphics graphics) {        
        super.paintComponent(graphics);
        final Graphics2D g2d = (Graphics2D) graphics;
        drawBorder(g2d);
        drawCoordinateAxis(g2d);
        drawDiagramName(g2d);
        drawCurves(g2d, _grafer._sliderContainer);
        _zoomWindow.paintComponent(g2d);
        drawProbeUtilities(g2d);
    }

    protected void setClipping(final Graphics2D g2d) {
        final int clipXmin = Math.max(g2d.getClipBounds().x, _xAxis._axisOriginPixel.x + 1);
        final int clipXMax = _xAxis._axisOriginPixel.x + _xAxis.getAxisLengthPixel() + 2;
        final int clipXwidth = clipXMax - clipXmin;
        final int yAxis1Lenth = _yAxis1.getAxisLengthPixel();
        g2d.setClip(clipXmin, _xAxis._axisOriginPixel.y - yAxis1Lenth + 1, clipXwidth, yAxis1Lenth - 1);

    }

    private void drawCurves(final Graphics2D g2d, final SliderContainer slider) {        
        int labelIndex = 0;
        for (AbstractCurve curve : _curves) {
            if (curve.getAxisConnection() == AxisConnection.ZUORDNUNG_NIX) {
                curve.getCurveLabel().setLabelIndex(-1);
            } else {
                final java.awt.Shape oldClip = g2d.getClip();
                setClipping(g2d);
                curve.drawCurve(g2d, slider);
                g2d.setClip(oldClip);
                curve.getCurveLabel().setLabelIndex(labelIndex);
                labelIndex++;
            }
        }
    }

    private void drawDiagramName(final Graphics2D g2d) {
        if (isDrawTitle()) {
            final int stringWidth = g2d.getFontMetrics().stringWidth(_diagramSettings.getNameDiagram());
            final int yTitle = _yAxis1._axisOriginPixel.y - _yAxis1.getAxisLengthPixel() - g2d.getFont().getSize() / THREE;
            g2d.drawString(_diagramSettings.getNameDiagram(), (getWidth() - stringWidth - _dxInRight / 2) / 2, yTitle);
        }
    }

    boolean isDrawTitle() {
        final String diagramName = _diagramSettings.getNameDiagram();
        return !diagramName.isEmpty() && !diagramName.startsWith("GRF");
    }

    private void addMouseListeners() {

        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseEntered(final MouseEvent mouseEvent) {
                // nothing to do here!
            }

            @Override
            public void mouseExited(final MouseEvent mouseEvent) {
                // nothing to do here!
            }

            @Override
            public void mouseClicked(final MouseEvent mouseEvent) {
                _grafer._sliderContainer.doMouseAction(mouseEvent, AbstractDiagram.this);
            }

            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                _grafer._sliderContainer.doMouseAction(mouseEvent, AbstractDiagram.this);
            }

            @Override
            public void mouseReleased(final MouseEvent mouseEvent) {
                _grafer._sliderContainer.doMouseAction(mouseEvent, AbstractDiagram.this);
            }
        });

        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(final MouseEvent mouseEvent) {
                _grafer._sliderContainer.doMouseAction(mouseEvent, AbstractDiagram.this);
            }

            @Override
            public void mouseMoved(final MouseEvent mouseEvent) {
                // do nothing when mouse is moved
            }
        });

        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent event) {
                _grafer.doMouseWheelAction(event, AbstractDiagram.this);
            }
        });
        this.addMouseMotionListener(_zoomWindow._mouseMotionLsnr);
        this.addMouseListener(_zoomWindow._mouseListener);
    }

    abstract double getDiagramYWeight();

    abstract String getDiagramTypeString();

    /**
     * this method creates new curves of corresponding type, when an old set of
     * curves (probably of other type) is given.
     *
     * @param oldCurves
     * @return
     */
    abstract List<AbstractCurve> getCurvesCopy(final List<AbstractCurve> oldCurves);

    abstract AbstractCurve curveFabric();
}
