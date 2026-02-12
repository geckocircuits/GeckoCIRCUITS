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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

/**
 *
 * @author andy
 */
class CurveLabel{
  protected int _labelYMin, _labelYMax;
  protected static final int SGN_NAME_OFFSET = 10;
  protected static final int DIST_SIG_NAMES = 16;
  protected static final double SIG_NAMES_RATIO = 0.75;
  protected static final int SELECTION_OFFSET = 3;
  protected final AbstractCurve _curve;
  static final Font FONT_LABEL = new Font("Arial", Font.PLAIN, 10);
  static final Font FONT_NUMBER_LABEL = new Font("Arial", Font.PLAIN, 9);
  static final int TXT_DX = 2;
  final SliderContainer _slider;
  final AbstractDiagram _diagram;
  int _labelIndex;

  public CurveLabel(final AbstractCurve curve){
    super();
    _curve = curve;
    _diagram = _curve._diagram;
    _slider = _diagram._grafer._sliderContainer;
  }

  public void setLabelIndex(final int labelIndex){
    _labelIndex = labelIndex;
  }

  public void drawLabel(final Graphics2D g2d){
    final CurveSettings curveSettings = _curve.getCurveSettings();
    drawCurveSelection(g2d, _diagram.getLabelPanel().getWidth());
    g2d.setColor(curveSettings._curveColor.getJavaColor());

    if(_slider.isSliderActive()){
      g2d.setFont(FONT_LABEL);
    }else{
      g2d.setFont(FONT_NUMBER_LABEL);
    }

  }

  /**
   * draw a red rectangle around the selected curve label
   *
   * @param g2d
   * @param width
   */
  public final void drawCurveSelection(final Graphics2D g2d, final int width){
    if(!_curve._isSelected){
      return;
    }
    final int posYMin = _labelYMin;
    final int posYMax = _labelYMax;
    g2d.setColor(Color.red);
    g2d.drawRect(0, posYMin - SELECTION_OFFSET + 1,
                 width - 1, posYMax - posYMin + 2 * SELECTION_OFFSET - 2);
  }

  public final boolean isInSelectionWindow(final MouseEvent mouseEvent){
    final int posY = mouseEvent.getY();
    return posY >= _labelYMin && posY <= _labelYMax;
  }
}
