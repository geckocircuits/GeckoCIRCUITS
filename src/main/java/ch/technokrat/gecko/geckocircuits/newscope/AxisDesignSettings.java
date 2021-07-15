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

/**
 *
 * @author andy
 */
public final class AxisDesignSettings implements LineSettable{
  private GeckoColor _axisColor = GeckoColor.BLACK;
  private GeckoLineStyle _axisStyle = GeckoLineStyle.SOLID_PLAIN;
  private String _axisCaption = "";
  public static final GeckoLineStyle ZERO_LINE_STYLE = GeckoLineStyle.SOLID_THIN;
  public static final GeckoColor ZERO_LINE_COL = GeckoColor.LIGHTGRAY;

  public String getAchseBeschriftung(){
    return _axisCaption;
  }

  public void setAchseBeschriftung(final String newName){
    _axisCaption = newName;
  }

  @Override
  public void setStroke(final GeckoLineStyle stroke){
    _axisStyle = stroke;
  }

  @Override
  public GeckoLineStyle getStroke(){
    return _axisStyle;
  }

  @Override
  public void setTransparency(final float value){
    // Axis transparency cannot be set
  }

  @Override
  public float getTransparency(){
    return 1f;
  }

  @Override
  public GeckoColor getColor(){
    return _axisColor;
  }

  @Override
  public void setColor(final GeckoColor color){
    _axisColor = color;
  }

  void importASCII(final TokenMap axisMap){
    _axisColor = GeckoColor.getFromCode(axisMap.readDataLine("axisColor", _axisColor.code()));
    _axisStyle = GeckoLineStyle.getFromCode(axisMap.readDataLine("axisStroke", _axisStyle.code()));
    _axisCaption = axisMap.readDataLine("axisCaption", _axisCaption);
  }

  void exportIndividualCONTROL(final StringBuffer ascii){
    DatenSpeicher.appendAsString(ascii.append("\naxisColor"), _axisColor.code());
    DatenSpeicher.appendAsString(ascii.append("\naxisStroke"), _axisStyle.code());
    DatenSpeicher.appendAsString(ascii.append("\naxisCaption"), _axisCaption);
  }
}
