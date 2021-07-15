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

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import java.awt.Graphics2D;

/**
 *
 * @author andy
 */
class CurveLabelRegular extends CurveLabel{
  private final TechFormat _cf = new TechFormat();
  private static final int DISP_NUM_DIGITS = 4;

  CurveLabelRegular(final AbstractCurve curve){
    super(curve);
    _cf.setMaximumDigits(DISP_NUM_DIGITS);
  }

  @Override
  public void drawLabel(final Graphics2D g2d){
    if(_labelIndex < 0){
      return;
    }
    super.drawLabel(g2d);
    final double yValue = _slider.getYSliderValueRealOrDiff(_diagram.getCurves().indexOf(_curve));
    final String wert = _cf.formatT(yValue, TechFormat.FORMAT_AUTO);

    final Axis yAxis = _diagram._yAxis1;
    final int yLinksObenKurve = yAxis._axisOriginPixel.y - yAxis.getAxisLengthPixel();

    if(_slider.isSliderActive()){
      final int yOben = yLinksObenKurve + g2d.getFont().getSize() + 2 * _labelIndex * DIST_SIG_NAMES;
      final int yUnten = yOben + (int)(DIST_SIG_NAMES * SIG_NAMES_RATIO);
      _labelYMin = yOben - g2d.getFont().getSize();
      _labelYMax = yUnten;
      g2d.drawString(_curve.getCurveName() + " =", TXT_DX, yOben);
      final String labelString = _slider.getLabelString(_curve) + " " + wert;
      g2d.drawString(labelString, TXT_DX,
                     yOben + Math.round(DIST_SIG_NAMES * SIG_NAMES_RATIO));
    }else{
      _labelYMin = yLinksObenKurve + _labelIndex * DIST_SIG_NAMES;
      _labelYMax = _labelYMin + g2d.getFont().getSize();
      g2d.drawString(_curve.getCurveName(), TXT_DX,
                     _labelYMax);
    }

  }
}
