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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.*;

/**
 * A generic MVC framework. This seems to be very usefull when the MVC paradigma
 * is used extensively. The class does all the listener registration stuff,
 * notification... Detailed documentation, see
 * http://www.onjava.com/pub/a/onjava/2004/07/07/genericmvc.html?page=1 Be
 * aware: weak references are used for the listeners. Therefore, after Observer
 * deletion garbage collection is able to remove the Observer object, even if a
 * reference from the Listener-List still exists.
 *
 */
public abstract class ModelMVCGeneric<T> implements Serializable{
  // transient private Set<ActionListener> listeners = new HashSet<ActionListener>();
  transient public WeakListModel listeners = new WeakListModel();
  private static final long serialVersionUID = 784638463745367L;

  /**
   * register Listener in list. Careful: the listener is not updated during
   * registration, that means the listener must aquire its initialisation data
   * manually. But thats ok, since we have a reference to the values, anyway.
   * this saves some events.
   *
   * @param listener instance of ListenerMVCGeneric
   */
  public void addModelListener(final ActionListener listener){

    if(listeners == null){
      //listeners = new HashSet<ActionListener>();
      listeners = new WeakListModel();
    }

    //this.listeners.add(listener);
    this.listeners.addElement(listener);
  }

  /**
   * remove listener from List
   *
   * @param listener instance of ListenerMVCGeneric
   */
  public void removeModelListener(final ActionListener listener){
    //this.listeners.remove(listener);
    listeners.removeElement(listener);
  }

  /**
   * send a notify event to all registered listeners.
   *
   * @param source inner Property class which is sending the event
   */
  public void notifyModelListeners(Object source){
    if(listeners == null){
      return;
    }
    for(int i = 0; i < listeners.getSize(); i++){
      notifyModelListener((ActionListener)listeners.getElementAt(i), source);
    }

//        for (final ActionListener listener : this.listeners) {
//            notifyModelListener(listener, source);
//        }
  }

  /**
   * Send a notif event to a specifig Listener. Caution, the listener does not
   * have to be in the register list!
   *
   * @param listener an instance of ListenerMVCGeneric
   */
  protected void notifyModelListener(final ActionListener listener, Object source){
    listener.actionPerformed(new ActionEvent(source, 0, "mvc change"));
  }
  /**
   * value is the "model data" to be stored in the private class. To use the
   * framework, create a Member variable ot Property<T>, e.g. public final
   * Property <Boolean> selected in the subclass. Everytime the value is updated
   * by the setValue() method, the attached listeners are informed. Be careful,
   * value is still mutable, if other access than setValue() is used (by
   * reference for instance).
   */
  protected T _value;

  /**
   * initialisation konstructor. Please consider that the write access to the
   * value should be done throug setValue() method.
   *
   * @param initialValue
   */
  public ModelMVCGeneric(final T initialValue){
    this._value = initialValue;
  }

  /**
   * writes the new value and sends an update notification to all listeners
   *
   * @param value
   */
  public void setValue(final T value){
    this._value = value;
    if(value instanceof Double) {
        double dVal = (Double) value;
        if (dVal != dVal) {            
            this._value = (T) new Double(1);
        }
    }      
    notifyModelListeners(this);

  }

  /**
   *
   * @return the stored value
   */
  public T getValue(){
    return this._value;
  }
}
