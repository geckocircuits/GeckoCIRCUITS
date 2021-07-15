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
package ch.technokrat.modelviewcontrol;

import java.io.Serializable;

/**
 * class represents the MVC - Model of a Generic type value, which is undoable
 *
 * @param <T>
 * @author andy
 */
public class ModelMVC<T> extends AbstractUndoGenericModel<T>
        implements Serializable{
  private static final long serialVersionUID = 784635241326447L;
  private Object _descriptionObject = null;

  public ModelMVC(T initValue){
    super(initValue);
  }

  /**
   *
   * @param initValue initial Float value of model
   * @param description a string which tells what kind of object the object
   * represents. This is used, e.g. for an undo-event: "Undo dielectric
   * constant".
   */
  public ModelMVC(T initValue, Object descriptionObject){
    super(initValue);
    _descriptionObject = descriptionObject;
  }

  @Override
  public String toString(){
      if(_descriptionObject != null) {
          return _descriptionObject.toString();
      } else {
          return getClass().getName() + "_" + hashCode();
      }    
  }
    
}
