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

import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

public final class GraferV4 extends JPanel {

    private static final long serialVersionUID = 364726123473711L;
    final ScopeSettings _scopeSettings;
    boolean _antialiasing = true;
    final XSliderValueDrawer _xSliderDrawer = new XSliderValueDrawer();
    final SliderContainer _sliderContainer = new SliderContainer(this, _xSliderDrawer);
    private final GridBagLayout _layout = new GridBagLayout();
    private AbstractDataContainer _dataContainer;
    private GridBagConstraints _contstraints;
    private static final double DIAG_WEIGHT_X = 0.5;
    private Thread updateThread = new Thread(new UpdateRunnable());
    private boolean _loadAndDraw;
    public final DiagramCurveSignalManager _manager = new DiagramCurveSignalManager(this);
    private int _sleepMillis = 400;
    public boolean _isNormalSimulationView = true;
    private boolean _isGlobalFitEnabled = true;
    public String xAxisLabel = "";

    public GraferV4(final ScopeSettings scopeSettings) {
        super();
        setLayout(new GridBagLayout());

        setOpaque(true);
        setBackground(Color.WHITE);
        _scopeSettings = scopeSettings;
        this.setLayout(_layout);
        refreshComponentPane();
        updateThread.setPriority(Thread.MIN_PRIORITY);
        updateThread.start();
        //this.add(_plotPanel);        
    }

    public DiagramCurveSignalManager getManager() {
        return _manager;
    }

    public void setNewXNames(final String xName, final String invXName) {
        _xSliderDrawer.xName = xName;
        _xSliderDrawer.xNameInv = invXName;
    }

    public void createInitialDiagram() {
        if (_manager.getNumberDiagrams() == 0) {
            _manager.updateCurveNumber(ReglerOSZI.DEF_TERM_NUMBER);
            final AbstractDiagram diag = new DiagramCurve(this);
            diag._diagramSettings.setNameDiagram("GRF " + _manager.getNumberDiagrams());

            _manager.addDiagram(diag);

            for (AbstractCurve curve : diag.getCurves()) {
                curve.setAxisConnection(AxisConnection.ZUORDNUNG_Y);
            }
        }
    }

    void addDiagramToPane(final AbstractDiagram diag) {
        if (_contstraints == null) {
            return;
        }
        _contstraints.weightx = DIAG_WEIGHT_X;
        _contstraints.weighty = diag.getDiagramYWeight();
        _contstraints.fill = GridBagConstraints.BOTH;
        _contstraints.gridx = 0;
        _contstraints.gridy = _manager.getDiagrams().indexOf(diag);

        add(diag, _contstraints);

        _contstraints.fill = GridBagConstraints.BOTH;
        _contstraints.weightx = 0.0;
        _contstraints.gridx = 1;
        add(diag.getLabelPanel(), _contstraints);
    }

    public void refreshComponentPane() {
        this.removeAll();
        _contstraints = new GridBagConstraints();
        //natural height, maximum width
        _contstraints.fill = GridBagConstraints.HORIZONTAL;
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            addDiagramToPane(diag);
        }
        if (_xSliderDrawer.isVisible()) {
            addSliderXDrawer();
        }

        // don't call "revalidate" here, since revalidate is not immediate!
        this.validate();
        this.repaint();

        if (this.getWidth() > 0) {
            for (AbstractDiagram diag : _manager.getDiagrams()) {
                diag.validate();
                triggerRedraw();
                diag.repaint();
            }
            setAxisPositions();
        }
    }

    public int getNumberInputSignals() {
        return _dataContainer.getRowLength();
    }

    private boolean amIVisible() {
        Container c = getParent();
        while (c != null) {
            if (!c.isVisible()) {
                return false;
            } else {
                c = c.getParent();
            }
        }
        return true;
    }

    public void triggerRedraw() {
        _loadAndDraw = true;
    }

    public void setInitalCurveConnection(final int terminalNumber) {
        if(_manager.getDiagram(0) instanceof DiagramSignal) {
            _manager.getDiagram(0).getCurve(terminalNumber - 1).setAxisConnection(AxisConnection.ZUORDNUNG_SIGNAL);
        } else {
            _manager.getDiagram(0).getCurve(terminalNumber - 1).setAxisConnection(AxisConnection.ZUORDNUNG_Y);
        }
        
    }

    void doMouseWheelAction(final MouseWheelEvent event, final AbstractDiagram wheeledDiagram) {
        switch (_mausModus) {
            case ZOOM_WINDOW:
                mouseWheelZoom(event, wheeledDiagram);
                break;
            case SLIDER:
                _sliderContainer.mouseWheelSlider(event, wheeledDiagram);
                break;
            case NONE:
                break;
            default:
                assert false;
        }
    }

    private void mouseWheelZoom(final MouseWheelEvent event, final AbstractDiagram wheeledDiagram) {
        final Point mousePoint = event.getPoint();
        final Set<AbstractDiagram> diagramsXUpdate = getDiagramsForXUpdate(event, wheeledDiagram);

        if (!event.isShiftDown()) {
            for (AbstractDiagram diag : diagramsXUpdate) {
                diag._xAxis.doMouseWheelZoom(mousePoint.x, event.getWheelRotation());
            }
        }


        if (!event.isControlDown()) {
            wheeledDiagram._yAxis1.doMouseWheelZoom(mousePoint.y, event.getWheelRotation());
        }

        for (AbstractDiagram diag : diagramsXUpdate) {
            diag.loadDataFromContainer(getDataContainer());
            diag.repaint();
        }


    }

    public Set<AbstractDiagram> getDiagramsForXUpdate(final MouseEvent event, final AbstractDiagram clickedDiagram) {
        Set<AbstractDiagram> selection = new HashSet<AbstractDiagram>();
        selection.add(clickedDiagram);

        if (!event.isShiftDown() || (event.isShiftDown() && event.isControlDown())) {
            selection.addAll(_manager.getDiagrams());
        }
        return selection;
    }

    public void setUpdateSleep(final int sleepMillis) {
        _sleepMillis = sleepMillis;
    }

    public void createInitialAndSingleDiagram(final boolean xAxisLog, final boolean yAxisLog, final int numberCurves) {
        createInitialDiagram();
        _manager.updateCurveNumber(numberCurves);
        AbstractDiagram diag = _manager.getDiagram(0);
        GeckoColor color = GeckoColor.RED;
        for (int i = 0; i < numberCurves; i++) {
            AbstractCurve curve = diag.getCurve(i);
            curve.setSymbolEnabled(true);
            curve.setSymbol(GeckoSymbol.RECT_FILLED);
            curve.setAxisConnection(AxisConnection.ZUORDNUNG_Y);
            curve.setColor(color);
            color = GeckoColor.getNextColor(color);
        }


        if (yAxisLog) {
            diag._yAxis1.setAxisType(AxisLinLog.ACHSE_LOG);
        }

        if (xAxisLog) {
            diag._xAxis.setAxisType(AxisLinLog.ACHSE_LOG);
        }

        final Stack<AbstractScopeSignal> inputSignals = new Stack<AbstractScopeSignal>();
        for (int i = 0; i < numberCurves; i++) {
            inputSignals.add(new ScopeSignalRegular(0, null));
        }

        _manager.setInputSignals(inputSignals);
    }

    public void setSymbolsInCurveEnabled(final int[] indices) {
        AbstractDiagram diag = _manager.getDiagram(0);

        for (int index : indices) {
            AbstractCurve curve = diag.getCurve(index);
            curve.setSymbolEnabled(true);
            curve.setSymbol(GeckoSymbol.RECT_FILLED);
        }
    }

    public void createInitialDiagramCISPR16(final boolean xAxisLog, final boolean yAxisLog, final int numberCurves) {
        createInitialDiagram();
        _manager.updateCurveNumber(numberCurves);
        AbstractDiagram diag = _manager.getDiagram(0);
        GeckoColor color = GeckoColor.RED;
        for (int i = 0; i < numberCurves; i++) {
            AbstractCurve curve = diag.getCurve(i);
            curve.setSymbol(GeckoSymbol.RECT_FILLED);
            curve.setAxisConnection(AxisConnection.ZUORDNUNG_Y);
            curve.setColor(color);
            color = GeckoColor.getNextColor(color);
        }


        if (yAxisLog) {
            diag._yAxis1.setAxisType(AxisLinLog.ACHSE_LOG);
        }

        if (xAxisLog) {
            diag._xAxis.setAxisType(AxisLinLog.ACHSE_LOG);
        }

        final Stack<AbstractScopeSignal> inputSignals = new Stack<AbstractScopeSignal>();
        for (int i = 0; i < numberCurves; i++) {
            inputSignals.add(new ScopeSignalRegular(0, null));
        }

        _manager.setInputSignals(inputSignals);
        _manager.getDiagram(0).getCurve(0)._curveSettings._curveShowPtSymbols = false;
        _manager.getDiagram(0).getCurve(1)._curveSettings._curveShowPtSymbols = false;

    }

    private class UpdateRunnable implements Runnable {

        /**
         * Please note: I want to enshure, that the update of the curves is done
         * from one (this) and only this thread.
         */
        private boolean _isVisible;
        private AbstractDataContainer _lastDrawPausedContainer = null;

        @Override
        public void run() {
            while (true) {
                try {
                    _isVisible = amIVisible();
                    AbstractDataContainer dataContainer = _dataContainer;
                    if (!_isVisible) { // if not visible, don't redraw, but enshure that when component made visible, we redraw.
                        _loadAndDraw = true;
                        Thread.sleep(2 * _sleepMillis);
                        continue;
                    }

                    if (dataContainer == null) {
                        Thread.sleep(2 * _sleepMillis);
                        continue;
                    }

                    if (dataContainer.getContainerStatus() == ContainerStatus.RUNNING) {
                        _loadAndDraw = true;
                    }

                    if (dataContainer.getContainerStatus() == ContainerStatus.PAUSED && dataContainer != _lastDrawPausedContainer) {
                        _lastDrawPausedContainer = dataContainer;
                        _loadAndDraw = true;
                        double startTime = dataContainer.getTimeValue(0, 0);
                        double endTime = dataContainer.getTimeValue(dataContainer.getMaximumTimeIndex(0), 0);
                        for (int i = 1; i < dataContainer.getRowLength(); i++) {
                            endTime = Math.max(dataContainer.getTimeValue(dataContainer.getMaximumTimeIndex(i), i), endTime);
                        }
                        NiceScale xNiceScale = new NiceScale(HiLoData.hiLoDataFabric((float) startTime, (float) endTime), true);
                        HiLoData niceLimits = xNiceScale.getNiceLimits();

                        if (Math.abs(SimulationsKern.tEND - endTime) / Math.abs(SimulationsKern.tEND + endTime) > 0.01) {
                            setSimulationTimeBoundaries(startTime, niceLimits._yHi);
                        } else {
                            setSimulationTimeBoundaries(startTime, endTime);
                        }


                    }
                    if (_loadAndDraw) {
                        Thread.sleep(_sleepMillis);
                        reloadDataAndPaint(dataContainer);
                        _loadAndDraw = false;
                    }
                    Thread.sleep(_sleepMillis);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GraferV4.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        private void reloadDataAndPaint(final AbstractDataContainer container) throws InterruptedException {
            for (AbstractDiagram diag : _manager.getDiagrams()) {
                diag.loadDataFromContainer(container);
                repaint();
            }
        }
    }

    public void setDataContainer(final AbstractDataContainer dataContainer) {
        _dataContainer = dataContainer;
        if (_dataContainer.getContainerStatus() != ContainerStatus.PAUSED) {
            for (AbstractDiagram diag : _manager.getDiagrams()) {
                diag.loadDataFromContainer(_dataContainer);
                repaint();
            }
        }
    }

    public AbstractDataContainer getDataContainer() {
        return _dataContainer;
    }

    void saveCurrentZoom() {
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._yAxis1._axisMinMax.saveValues();
            diag._yAxis2._axisMinMax.saveValues();
            diag._xAxis._axisMinMax.saveValues();
        }
    }

    void loadSavedZoom() {
        final AbstractDataContainer container = getDataContainer();
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._yAxis1._axisMinMax.loadFromSaved();
            diag._yAxis2._axisMinMax.loadFromSaved();
            diag._xAxis._axisMinMax.loadFromSaved();
            diag.loadDataFromContainer(container);
        }
        repaint();

    }

    void setAutoScaleModus(final boolean value) {
        _isGlobalFitEnabled = value;
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._yAxis1._axisMinMax.setAutoEnabled(value);
            diag._yAxis2._axisMinMax.setAutoEnabled(value);
            diag._xAxis._axisMinMax.setAutoEnabled(value);
        }
    }

    void setSelectedCurve(final AbstractCurve newSelection) {
        if (this._curveSelected != null) {
            this._curveSelected.setSelected(false);
        }
        _curveSelected = newSelection;
        if (newSelection != null) {
            newSelection.setSelected(true);
        }
        repaint();
    }

    public void exportIndividualCONTROL(final StringBuffer ascii) {

        ascii.append("isAntiAliasing ");
        ascii.append(_antialiasing);
        ascii.append('\n');

        for (AbstractDiagram diag : _manager.getDiagrams()) {
            ascii.append("<Diagram>\n");
            diag.exportIndividualCONTROL(ascii);
            ascii.append("<\\Diagram>\n");
        }
    }

    public void plotConnections() {
        System.out.println("----+++------------");
        for (AbstractDiagram diag : getManager().getDiagrams()) {
            for (AbstractCurve crv : diag.getCurves()) {
                System.out.print(crv.getAxisConnection());
            }
            System.out.println("");
        }
    }

    public void importIndividualCONTROL(final TokenMap settingsMap) {
        if (settingsMap.containsToken("isAntiAliasing")) {
            _antialiasing = settingsMap.readDataLine("isAntiAliasing", _antialiasing);
        }

        if (!_manager.getDiagrams().isEmpty()) {

            for (AbstractDiagram diag : _manager.getDiagrams().toArray(new AbstractDiagram[0])) {
                _manager.deleteDiagram(diag);
            }
        }

        for (TokenMap diagramMap = settingsMap.getBlockTokenMap("<Diagram>"); diagramMap != null;
                diagramMap = settingsMap.getBlockTokenMap("<Diagram>")) {
            AbstractDiagram newDiagram = AbstractDiagram.fabricFromModelFile(diagramMap, this);
            _manager.addDiagram(newDiagram);
            newDiagram.populateCurveProperties(diagramMap, this);
        }
        refreshComponentPane();
    }

    public enum MausModus {

        NONE, ZOOM_WINDOW, SLIDER;
    }
    MausModus _mausModus = MausModus.ZOOM_WINDOW;  // default --> Maus deaktiviert
    //    
    private HiLoData _tLimitsScope = HiLoData.hiLoDataFabric(0, 1);
    private AbstractCurve _curveSelected = null;


    /*
     * return selected curve, or null if there is no selection.
     */
    AbstractCurve getSelectedCurve() {
        return _curveSelected;
    }

    public void setAntiAliasing(final boolean value) {
        _antialiasing = value;
    }

    public boolean isAntiAliasing() {
        return _antialiasing;
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        super.paintComponent(graphics);

        final Graphics2D g2d = (Graphics2D) graphics;
        if (_antialiasing) {
            ((Graphics2D) g2d).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            ((Graphics2D) g2d).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }

    @Override
    public void paint(final Graphics graphics) {
        super.paint(graphics);
        // this is done in the paint-routine (and not paintComponent!), since I want to enshure that the
        // slider is drawn on top of all other stuff!
        _sliderContainer.drawSlider(graphics);
    }

    public void doZoomAutoFit() {
        final AbstractDataContainer container = _dataContainer;
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._yAxis1._axisMinMax.globalFit();
            diag._yAxis2._axisMinMax.globalFit();
            diag._xAxis._axisMinMax.globalFit();
            diag.loadDataFromContainer(container);
        }
        repaint();
    }

    private void definiereAchsenbegrenzungenNumerischeSimulation(final HiLoData startStopTimes) {
        for (AbstractDiagram diag : _manager.getDiagrams()) {   // geht durch die Zeilen
            final Axis xAxis = diag._xAxis;
            xAxis._axisMinMax.setGlobalAutoScaleValues(startStopTimes);
        }
    }

    public void setAxisPositions() {
        if (getWidth() == 0) {
            return;
        }

        boolean hasAnyY2Axis = false;
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            if (diag.checkForY2Axis()) {
                hasAnyY2Axis = true;
            }
        }

        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag.setAxisPositions(hasAnyY2Axis);
        }
    }

    public void setMausModus(final MausModus mausModus) {
        this._mausModus = mausModus;  // in den neuen Zustand gehen
        //--------------------------
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._zoomWindow.setMausModus(mausModus);
        }

        switch (mausModus) {
            case NONE:
                _sliderContainer.setSliderActivity(false);  // aktives Ausschalten des Schiebers
                this.remove(_xSliderDrawer);
                _xSliderDrawer.setVisible(false);
                this.validate();
                this.setSelectedCurve(null);
                this.setAxisPositions();
                this.repaint();
                break;
            case ZOOM_WINDOW:
                break;
            case SLIDER:
                addSliderXDrawer();
                if (!_sliderContainer.isSliderActive()) {
                    _sliderContainer.setSliderActivity(true);
                    // x-Schieber wird an den Anfang gesetzt: gleich fuer alle Diagramme, in GraferV3 definiert
                    this.setAxisPositions();
                    this.repaint();
                }
                refreshComponentPane();
                break;
            default:
                assert false;
        }
    }

    private void addSliderXDrawer() {
        _contstraints.fill = GridBagConstraints.HORIZONTAL;
        _contstraints.weightx = 0.0;
        _contstraints.weighty = 0.0;
        _contstraints.gridx = 0;
        _contstraints.gridy = _manager.getNumberDiagrams();
        _contstraints.gridwidth = 2;
        this.add(_xSliderDrawer, _contstraints);
        _xSliderDrawer.setVisible(true);
        this.validate();
    }

    protected void undoZoom() {
        final AbstractDataContainer container = _dataContainer;
        for (AbstractDiagram diag : _manager.getDiagrams()) {
            diag._xAxis._axisMinMax.popHistoryStack();
            diag._yAxis1._axisMinMax.popHistoryStack();
            diag._yAxis2._axisMinMax.popHistoryStack();
            diag.loadDataFromContainer(container);
        }
        repaint();
    }

    public double getXValueFromPixel(final int xPix) {
        final Axis xAxis = _manager.getDiagram(0)._xAxis;
        return xAxis.getValueFromPixel(xPix);
    }

    public int getXPixFromValue(final double value) {
        final Axis xAxis = _manager.getDiagram(0)._xAxis;
        return (int) xAxis.getPixelFromValue(value);
    }

    public void setSimulationTimeBoundaries(final double t1SCOPE, final double t2SCOPE) {
        _tLimitsScope = HiLoData.hiLoDataFabric((float) t1SCOPE, (float) t2SCOPE);        
        definiereAchsenbegrenzungenNumerischeSimulation(_tLimitsScope);

        for (AbstractDiagram diag : _manager.getDiagrams()) {
            if (_isGlobalFitEnabled) {
                if (diag._xAxis._axisMinMax.isAutoEnabled()) {
                    diag._xAxis._axisMinMax.globalFit();
                }

                if (diag._yAxis1._axisMinMax.isAutoEnabled()) {
                    diag._yAxis1._axisMinMax.globalFit();
                }

                if (diag._yAxis2._axisMinMax.isAutoEnabled()) {
                    diag._yAxis2._axisMinMax.globalFit();
                }
            }

        }

    }
}
