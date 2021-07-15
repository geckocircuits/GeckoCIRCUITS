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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.newscope.SliderUtils.ExtremumType;
import ch.technokrat.gecko.geckocircuits.newscope.SliderUtils.FlankType;
import ch.technokrat.gecko.geckocircuits.newscope.SliderUtils.IterationDirection;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

final class SliderContainer{
  private boolean _sliderActive = false;
  private final List<SliderValues> _ySliderValues = new ArrayList<SliderValues>();
  private boolean _inDiffMode = false;
  private final GraferV4 _grafer;
  private final Slider _redSlider = new Slider(Color.RED);
  private final Slider _greenSlider = new Slider(Color.GREEN);
  private Slider _activeSlider = _redSlider;
  private final XSliderValueDrawer _xSliderDrawer;
  private static final String JOPTION_PANE_MESSAGE = "No curve selected! Please select\n"
          + "a curve by clicking on its label name.";
  private static final String JOPTION_PANE_MESSAGE_HEAD = "Warning";

  public SliderContainer(final GraferV4 grafer, final XSliderValueDrawer xSliderDrawer){
    _grafer = grafer;
    _xSliderDrawer = xSliderDrawer;
  }

  public boolean isSliderActive(){
    return _sliderActive;
  }

  public void setSliderActivity(final boolean value){
    _sliderActive = value;
  }

  public void setInDiffMode(final boolean isInDiffMode){
    _inDiffMode = isInDiffMode;
  }

  public double getYSliderValueRealOrDiff(final int curveID){
    if(_activeSlider.equals(_redSlider)){
      return getSliderValues(curveID).getYValue1();
    }else{
      return getSliderValues(curveID).getYValue2() - getSliderValues(curveID).getYValue1();
    }
  }

  public double getActiveSliderYValue(final int curveID){
    if(_ySliderValues.size() != _grafer.getDataContainer().getRowLength()){
      initYSliderValue();
    }
    if(_ySliderValues.size() != _grafer.getDataContainer().getRowLength()){
      System.out.println("ERROR");
    }
    if(_activeSlider.equals(_redSlider)){
      return getSliderValues(curveID).getYValue1();
    }else{
      return getSliderValues(curveID).getYValue2();
    }
  }

  public double getActiveSliderXValue(){
    if(_ySliderValues.size() != _grafer.getDataContainer().getRowLength()){
      initYSliderValue();
    }

    if(_activeSlider.equals(_redSlider)){
      return getSliderValues(0).getXValue1();
    }else{
      return getSliderValues(0).getXValue2();
    }
  }

  /**
   *
   * @param curveID the selected curve
   * @return a paix of x-y-Values
   */
  public SliderValues getSliderValues(final int curveID){
//        System.out.println("xxyxyx. " + _ySliderValues.size() +" " +  _grafer.getManager().getNumberInputSignals());
    if(_ySliderValues.size() != _grafer.getManager().getNumberInputSignals()){
      initYSliderValue();
    }

    try{
      if(curveID < _ySliderValues.size()){
        return _ySliderValues.get(curveID);
      }else{
      return new SliderValues(0, 0, 0, 0);
      }
    }catch(Exception ex){
      ex.printStackTrace();
      return new SliderValues(0, 0, 0, 0);
    }
  }

  public void initYSliderValue(){
    _ySliderValues.clear();

    for(int i = 0; i < _grafer.getManager().getNumberInputSignals(); i++){
      _ySliderValues.add(new SliderValues());
    }
  }

  public void calculateSliderFromXValues(){

    if(_ySliderValues.size() != _grafer.getDataContainer().getRowLength()){
      initYSliderValue();
    }

    final int index1 = _grafer.getDataContainer().findTimeIndex(_redSlider.getXSliderValue(), 0);
    final int index2 = _grafer.getDataContainer().findTimeIndex(_greenSlider.getXSliderValue(), 0);

    for(int i = 0; i < _ySliderValues.size(); i++){
      final double x1Value = _redSlider.getXSliderValue();
      final double y1Value = _grafer.getDataContainer().getValue(i, index1);

      final double x2Value = _greenSlider.getXSliderValue();
      final double y2Value = _grafer.getDataContainer().getValue(i, index2);
      _ySliderValues.set(i, new SliderValues(x1Value, y1Value, x2Value, y2Value));
    }
  }

  public void drawSlider(final Graphics graphics){
    if(!_sliderActive){
      return;
    }

    // changed here: don't use the pixel value for the slider, but
    // the x-Value, and re-calculate the pixel from that value
    // otherwise, zooming or changing the window size makes problems/does
    // not update correctly.
    final AbstractDiagram firstDiagram = _grafer.getManager().getDiagram(0);
    final Axis xAxis = firstDiagram._xAxis;
    final int startPaintY = firstDiagram.getTopOffset();
    final AbstractDiagram bottomDiagram = _grafer.getManager().getDiagram(_grafer.getManager().getNumberDiagrams() - 1);
    final int stopPaintY = bottomDiagram.getY() + bottomDiagram.getHeight() - bottomDiagram.getBottomOffset();

    _redSlider.paintComponent(graphics, startPaintY, stopPaintY, xAxis);
    _greenSlider.paintComponent(graphics, startPaintY, stopPaintY, xAxis);

  }

  protected void goToZeroOrEqual(final IterationDirection direction, final boolean isZeroTransition,
                                 final GraferV4 grafer) throws NoCurveSelectedException{

    final AbstractDataContainer data = grafer.getDataContainer();
    final AbstractCurve selectedCurve = grafer.getSelectedCurve();

    checkCurveSelection(selectedCurve);
    final double oldSliderXValue = getActiveSliderXValue();
    double transitionValue = 0;

    if(!isZeroTransition){
      transitionValue = getActiveSliderYValue(selectedCurve.getValueDataIndex());
    }

    final double newXValue = SliderUtils.nextZeroOrEqual(oldSliderXValue,
                                                         selectedCurve, direction, data, transitionValue);


    if(isZeroTransition){
      _activeSlider.setXSliderValue(newXValue);
    }else{
      getNonActiveSlider().setXSliderValue(newXValue);
    }

    calculateSliderFromXValues();
    _grafer.repaint();
  }

  Slider getNonActiveSlider(){
    if(_activeSlider.equals(_redSlider)){
      return _greenSlider;
    }else{
      return _redSlider;
    }
  }

  protected void goToExtrema(final IterationDirection direction, final ExtremumType maxMin, final GraferV4 grafer)
          throws NoCurveSelectedException{

    final AbstractDataContainer data = grafer.getDataContainer();
    final AbstractCurve curveSelected = grafer.getSelectedCurve();

    checkCurveSelection(curveSelected);
    final double oldSliderXValue = getActiveSliderXValue();
    final double newXValue = SliderUtils.nextExtrema(oldSliderXValue, curveSelected, direction, maxMin, data);      

    _activeSlider.setXSliderValue(newXValue);
    calculateSliderFromXValues();
    grafer.repaint();
  }

  protected void goToSignalFlank(final IterationDirection direction, final FlankType flankType,
                                 final GraferV4 grafer) throws NoCurveSelectedException{

    final AbstractDataContainer data = grafer.getDataContainer();
    final AbstractCurve curveSelected = grafer.getSelectedCurve();

    checkCurveSelection(curveSelected);

    final double oldSliderXValue = getActiveSliderXValue();
    final double newXValue = SliderUtils.nextSIGNALFlank(oldSliderXValue, curveSelected, direction, flankType, data);
      
    _activeSlider.setXSliderValue(newXValue);
    calculateSliderFromXValues();
    grafer.repaint();
  }

  protected String getLabelString(final AbstractCurve curve){
    String returnValue = "";

    if(curve.getAxisConnection() == AxisConnection.ZUORDNUNG_SIGNAL){
      if(isSliderActive()){
        double value;
        if(_activeSlider.equals(_redSlider)){
          value = getSliderValues(curve.getValueDataIndex()).getYValue1();
        }else{
          value = getSliderValues(curve.getValueDataIndex()).getYValue2();
        }

        if(value < CurvePainterSignal.SGN_THRESHOLD){
          return " = off";
        }else{
          return " = on";
        }
      }
      return returnValue;
    }else{
      if(_inDiffMode){
        returnValue = "diff";
      }
      return returnValue;
    }

  }

  protected void doMouseAction(final MouseEvent mouseEvent, final AbstractDiagram diag){
    if(_grafer._mausModus != GraferV4.MausModus.SLIDER){
      return;
    }

    if(mouseEvent.getID() != MouseEvent.NOBUTTON && isSliderActive()){
      final Axis xAxis = _grafer.getManager().getDiagram(0)._xAxis;
      if((mouseEvent.getModifiers() & MouseEvent.BUTTON1_MASK) == 0 || mouseEvent.isControlDown()){
        setInDiffMode(true);
        _activeSlider = _greenSlider;

      }else{
        setInDiffMode(false);
        _activeSlider = _redSlider;
      }

      _activeSlider.setXSliderPix(mouseEvent.getX(), xAxis, _grafer);
      _xSliderDrawer.setDisplayRanges(getSliderXRange());
      calculateSliderFromXValues();
    }
  }

  double[] getSliderXRange(){
    return new double[]{_redSlider.getXSliderValue(), _greenSlider.getXSliderValue()};
  }

  private void checkCurveSelection(final AbstractCurve curve) throws NoCurveSelectedException{
    if(curve == null){
      JOptionPane.showMessageDialog(GeckoSim._win, JOPTION_PANE_MESSAGE,
                                    JOPTION_PANE_MESSAGE_HEAD, JOptionPane.WARNING_MESSAGE);
      throw new NoCurveSelectedException("Please select a curve by clicking on the curve name.");
    }
  }

  void mouseWheelSlider(final MouseWheelEvent event, final AbstractDiagram wheeledDiagram){
    final int oldXPixel = _activeSlider.getXSliderPix();

    final Axis xAxis = _grafer.getManager().getDiagram(0)._xAxis;
    if(event.getWheelRotation() > 0){
      _activeSlider.setXSliderPix(oldXPixel + 1, xAxis, _grafer);
    }else{
      _activeSlider.setXSliderPix(oldXPixel - 1, xAxis, _grafer);
    }
    calculateSliderFromXValues();
    _xSliderDrawer.setDisplayRanges(getSliderXRange());
    _grafer.repaint();
  }
}
