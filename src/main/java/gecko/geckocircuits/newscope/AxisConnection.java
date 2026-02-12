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

public enum AxisConnection{
  ZUORDNUNG_X(51, "X"),
  ZUORDNUNG_Y(52, "Y"),
  ZUORDNUNG_Y2(53, "Y2"),
  ZUORDNUNG_SIGNAL(54, "sg"),
  ZUORDNUNG_NIX(55, "-");
  private String _displayString;
  private int _code;

  AxisConnection(final int code, final String displayString){
    _code = code;
    _displayString = displayString;
  }

  static AxisConnection getFromCode(final int code){
    for(AxisConnection val : AxisConnection.values()){
      if(val.getCode() == code){
        return val;
      }
    }
    return AxisConnection.ZUORDNUNG_NIX;
  }

  int getCode(){
    return _code;
  }

  @Override
  public String toString(){
    return _displayString;
  }

  public AxisConnection iterateNext(final boolean signal){

    if(signal){
      switch(this){
        case ZUORDNUNG_SIGNAL:
          return AxisConnection.ZUORDNUNG_NIX;
        case ZUORDNUNG_NIX:
          return AxisConnection.ZUORDNUNG_SIGNAL;
        default:
          assert false : this;
          break;
      }
    }


    switch(this){
      case ZUORDNUNG_NIX:
        return AxisConnection.ZUORDNUNG_Y;
      case ZUORDNUNG_Y:
        return AxisConnection.ZUORDNUNG_Y2;
      case ZUORDNUNG_Y2:
        return AxisConnection.ZUORDNUNG_NIX;
      default:
        assert false;
        break;

    }
    return null;
  }
};
