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
import java.util.HashSet;
import java.util.Set;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Extension of the ModelMVCGeneric framework. Inheriting from this class, then
 * the undo/redo can be performed easily.
 *
 * @param <T>
 * @author Andreas Müsing
 */
public abstract class AbstractUndoGenericModel<T> extends ModelMVCGeneric<T> implements Serializable{
  private static final long serialVersionUID = 28474838273478583L;
  protected boolean _initialized = false;

  /**
   *
   */
  public AbstractUndoGenericModel(final T initValue){
    super(initValue);
  }
  /**
   * refer to this undomanager when connecting to GUI undo/redo actions
   */
  public final static UndoManager undoManager = new GroupableUndoManager();

  static{
    undoManager.setLimit(1000);
  }
  // history list
  /**
   * Caution: static variable, gives more or less global access to all events.
   * these Listeners are notified, whenever a UndoGenericModel changes its
   * value. Used for e.g. for the undo/redo jMenuItem entries which must be
   * updated whenever something happens
   */
  public final static Set<ActionListener> globalEventListeners = new HashSet<ActionListener>();

  /**
   * send a notification to all listeners that are in globalEventListeners.
   * Caution, globalEventListeners is static, that means similar to a global
   * variable
   */
  public void fireGlobalEvent(){
    for(ActionListener listener : globalEventListeners){
      listener.actionPerformed(new ActionEvent(this, 0, "global MVC action"));
    }
  }
  /**
   * Extension of ModelMVCPoints inner Property class. Here, the redo/undo work
   * and connection to all the listeners is done. Careful, this inner class has
   * a further inner class included.
   *
   * @param <T>
   */
  /**
   * flag is true during an undo or redo event, otherwise false. flag needed for
   * preventing an undo operation beeing inserted again into the undo-manage
   */
  protected boolean _redoUndoFlag = false;

  /**
   * overrides super method, to add the undo/redo functionality.
   */
  @Override
  public void setValue(final T value){     
    if(!value.equals(_value) && !_redoUndoFlag && _initialized){
      undoManager.addEdit(new UndoableAction(this, this._value, value));
    }
    _initialized = true;      
    super.setValue(value);
    fireGlobalEvent();
  }

  public void setValueWithoutUndo(final T value){
    _initialized = true;
    if(!value.equals(getValue())){
      super.setValue(value);
      fireGlobalEvent();
    }
  }

  /**
   * Inner class Here the undo/redo operation is performed. Und just copies an
   * old value into the source´s property, redo does the opposite.
   *
   * @param <T>
   * @author Andreas Müsing
   */
  protected final class UndoableAction<T> implements UndoableEdit{
    private boolean _canUndo = false;
    private boolean _canRedo = false;
    private AbstractUndoGenericModel<T> _source;
    private T _oldValue;
    private T _newValue;

    /**
     *
     * @param source object whose property value will be overwritten with an old
     * value during undo
     * @param oldValue reference to an old property value
     * @param newValue reference to the actual property value(needed for a redo)
     */
    public UndoableAction(final AbstractUndoGenericModel<T> source, final T oldValue, final T newValue){
      _source = source;
      _oldValue = oldValue;
      _newValue = newValue;

      if(_oldValue != null){
        _canUndo = true;
      }

      if(_newValue != null){
        _canRedo = true;
      }
    }

    /**
     * Perform the undo operation. The method simply copies the old value into
     * the property of source.
     */
    @Override
    public void undo(){
      assert _oldValue != null;
      _redoUndoFlag = true;
      _source.setValue(_oldValue);
      _redoUndoFlag = false;
    }

    /**
     * should only be invoked after an undo operation, to revert the undo. It
     * just copies the newes property value into source.
     */
    @Override
    public void redo(){
      assert _newValue != null;
      _redoUndoFlag = true;
      _source.setValue(_newValue);
      _redoUndoFlag = false;
    }

    /**
     *
     * @return in general return true, only at the initialisation this should be
     * false.
     */
    @Override
    public boolean canUndo(){
      return _canUndo;
    }

    /**
     *
     * @return true, if the undo-action of this class was invoked before
     */
    @Override
    public boolean canRedo(){
      return _canRedo;
    }

    /**
     * Remove all references that exist, so that they can be deleted. This is
     * usually called when the undomanager queue is full, and the Undo-Object is
     * removed from undomanager
     */
    @Override
    @SuppressWarnings("PMD")
    public void die(){
      _source = null;
      _oldValue = null;
      _newValue = null;
    }

    /**
     * Combine different edits, e.g. when they are to small or trivial not used
     * yet
     *
     * @param arg0
     * @return
     */
    @Override
    public boolean addEdit(final UndoableEdit arg0){
      return false;
    }

    @Override
    public boolean replaceEdit(final UndoableEdit arg0){
      return false;
    }

    /**
     * not used yet. Can be used to skip trivial actions from undoing
     *
     * @return true
     */
    @Override
    public boolean isSignificant(){
      return true;
    }

    /**
     *
     * @return text for logging, for instance
     */
    @Override
    public String getPresentationName(){
      return toString();
    }

    /**
     *
     * @return
     */
    @Override
    public String getUndoPresentationName(){
      return AbstractUndoGenericModel.this.toString() + " " + _oldValue + " >> " + _newValue;
    }

    /**
     * text that will be displayed in the undo JMenuItem entry, e.g redo delete
     * point
     *
     * @return
     */
    @Override
    public String getRedoPresentationName(){
      return AbstractUndoGenericModel.this.toString() + " " + _newValue + " >> " + _oldValue;
    }
  }
}
