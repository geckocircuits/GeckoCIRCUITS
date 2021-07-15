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
public final class AxisGridSettings{
  private boolean _autoGrids = true;
  private boolean _userShowGridMin = false;
  private boolean _userShowGridMaj = true;
  private GeckoColor _farbeGridNormal = GeckoColor.LIGHTGRAY;
  private GeckoColor _farbeGridNormalMinor = GeckoColor.LIGHTGRAY;
  private GeckoLineStyle _linStilGridNormal = GeckoLineStyle.SOLID_THIN;
  private GeckoLineStyle _linStilGridMinor = GeckoLineStyle.SOLID_THIN;
  /**
   * which gridlines should be drawn. This is not set by the user, but can be
   * disabled when gridline-distance is too small.
   */
  private boolean _showGridMaj, _showGridMin;
  private static final double PX1 = 230, PX2 = 100, PXR = 2.5;

  private class MajorSettable implements LineSettable{
    @Override
    public void setStroke(final GeckoLineStyle stroke){
      setLinStyleMaj(stroke);
    }

    @Override
    public GeckoLineStyle getStroke(){
      return getLinStyleMaj();
    }

    @Override
    public void setTransparency(final float value){
      // grid transparency cannot be set
    }

    @Override
    public float getTransparency(){
      return 1f;
    }

    @Override
    public GeckoColor getColor(){
      return getColorGridMaj();
    }

    @Override
    public void setColor(final GeckoColor color){
      setColorGridMaj(color);
    }
  }

  private class MinorSettable implements LineSettable{
    @Override
    public void setStroke(final GeckoLineStyle stroke){
      setLinStyleMin(stroke);
    }

    @Override
    public GeckoLineStyle getStroke(){
      return getLinStyleMin();
    }

    @Override
    public void setTransparency(final float value){
      // grid transparency cannot be set
    }

    @Override
    public float getTransparency(){
      return 1f;
    }

    @Override
    public GeckoColor getColor(){
      return getColorGridMin();
    }

    @Override
    public void setColor(final GeckoColor color){
      setColorGridMin(color);
    }
  }

  public LineSettable getSettableMaj(){
    return new MajorSettable();
  }

  public LineSettable getSettableMin(){
    return new MinorSettable();
  }

  public GeckoLineStyle getLinStyleMaj(){
    return _linStilGridNormal;
  }

  public GeckoLineStyle getLinStyleMin(){
    return _linStilGridMinor;
  }

  public void setLinStyleMaj(final GeckoLineStyle lineStyle){
    _linStilGridNormal = lineStyle;
  }

  public void setLinStyleMin(final GeckoLineStyle lineStyle){
    _linStilGridMinor = lineStyle;
  }

  public GeckoColor getColorGridMaj(){
    return _farbeGridNormal;
  }

  public GeckoColor getColorGridMin(){
    return _farbeGridNormalMinor;
  }

  public void setColorGridMaj(final GeckoColor newColor){
    _farbeGridNormal = newColor;
  }

  public void setColorGridMin(final GeckoColor newColor){
    _farbeGridNormalMinor = newColor;
  }

  public boolean isAutoGrids(){
    return _autoGrids;
  }

  public void setAutoGrids(final boolean value){
    _autoGrids = value;
  }

  public boolean isShowGridNormalMajor(){
    return _showGridMaj;
  }

  public boolean isShowGridNormalMinor(){
    return _showGridMin;
  }

  public void setUserShowGridMin(final boolean value){
    _userShowGridMin = value;
  }

  public boolean isUserShowGridMin(){
    return _userShowGridMin;
  }

  public void setUserShowGridMaj(final boolean value){
    _userShowGridMaj = value;
  }

  public boolean isUserShowGridMaj(){
    return _userShowGridMaj;
  }

  /**
   * wenn die Diagramme in einem sehr kleinen Fenster gezeichnet werden, dann
   * muessen eventuell die Grid-Linien ausgeblendet werden, um eine gewisse
   * Uebersichtlichkeit zu wahren -->
   */
  public void blendeEventuellGridLinienAus(final int axisLengthPix){
    if(_autoGrids){
      if(isUserShowGridMaj() && axisLengthPix > PX2 * PXR){
        _showGridMaj = true;
      }else{
        _showGridMaj = false;
      }

      if(isUserShowGridMin() && axisLengthPix > PX1 * PXR){
        _showGridMin = true;
      }else{
        _showGridMin = false;
      }
    }else{
      _showGridMaj = _userShowGridMaj;
      _showGridMin = _userShowGridMin;
    }
  }

  void exportIndividualCONTROL(final StringBuffer ascii){
    DatenSpeicher.appendAsString(ascii.append("\nuserShowGridMaj"), _userShowGridMaj);
    DatenSpeicher.appendAsString(ascii.append("\nuserShowGridMin"), _userShowGridMin);
    DatenSpeicher.appendAsString(ascii.append("\nlinStilGridNormal"), _linStilGridNormal.code());
    DatenSpeicher.appendAsString(ascii.append("\nlinStilGridMinor"), _linStilGridMinor.code());
    DatenSpeicher.appendAsString(ascii.append("\ncolorGridNormal"), _farbeGridNormal.code());
    DatenSpeicher.appendAsString(ascii.append("\ncolorGridMinor"), _farbeGridNormalMinor.code());
  }

  void importASCII(final TokenMap axisMap){
    _userShowGridMaj = axisMap.readDataLine("userShowGridMaj", _userShowGridMaj);
    _userShowGridMin = axisMap.readDataLine("userShowGridMin", _userShowGridMin);
    _linStilGridNormal = GeckoLineStyle.getFromCode(axisMap.readDataLine("linStilGridNormal", _linStilGridNormal.code()));
    _linStilGridMinor = GeckoLineStyle.getFromCode(axisMap.readDataLine("linStilGridMinor", _linStilGridMinor.code()));
    _farbeGridNormal = GeckoColor.getFromCode(axisMap.readDataLine("colorGridNormal", _farbeGridNormal.code()));
    _farbeGridNormalMinor = GeckoColor.getFromCode(axisMap.readDataLine("colorGridMinor", _farbeGridNormalMinor.code()));
  }
}
