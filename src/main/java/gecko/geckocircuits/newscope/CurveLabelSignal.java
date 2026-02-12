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

import java.awt.Graphics2D;

/**
 *
 * @author andy
 */
class CurveLabelSignal extends CurveLabel{
  public CurveLabelSignal(final AbstractCurve curve){
    super(curve);
  }

  @Override
  public void drawLabel(final Graphics2D g2d){
    super.drawLabel(g2d);
    final String name = _curve.getCurveName();
    final String onOffString = _slider.getLabelString(_curve);
    _labelYMax = ((CurveSignal)_curve)._yTranslation - 1;
    _labelYMin = _labelYMax - g2d.getFont().getSize();
    g2d.drawString(name + onOffString, TXT_DX, _labelYMax);

  }
}
