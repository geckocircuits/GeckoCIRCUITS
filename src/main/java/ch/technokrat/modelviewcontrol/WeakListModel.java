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
import java.util.*;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public final class WeakListModel implements ListModel, Serializable{
  public static final long serialVersionUID = 582811111394392L;
  private final Map<ListDataListener, Object> _listenerList =
          Collections.synchronizedMap(new WeakHashMap<ListDataListener, Object>());
  private final Object _present = new Object();
  @SuppressWarnings("PMD")
  private final ArrayList<Object> _delegate = new ArrayList<Object>();

  @Override
  public int getSize(){
    return _delegate.size();
  }

  @Override
  public Object getElementAt(final int index){
    return _delegate.get(index);
  }

  public void trimToSize(){
    _delegate.trimToSize();
  }

  public void ensureCapacity(final int minCapacity){
    _delegate.ensureCapacity(minCapacity);
  }

  public int size(){
    return _delegate.size();
  }

  public boolean isEmpty(){
    return _delegate.isEmpty();
  }

  public Enumeration elements(){
    return Collections.enumeration(_delegate);
  }

  public boolean contains(final Object elem){
    return _delegate.contains(elem);
  }

  public int indexOf(final Object elem){
    return _delegate.indexOf(elem);
  }

  public int lastIndexOf(final Object elem){
    return _delegate.lastIndexOf(elem);
  }

  public Object elementAt(final int index){
    return _delegate.get(index);
  }

  public Object firstElement(){
    return _delegate.get(0);
  }

  public Object lastElement(){
    return _delegate.get(_delegate.size() - 1);
  }

  @Override
  public String toString(){
    return _delegate.toString();
  }

  public void setElementAt(final Object obj, final int index){
    _delegate.set(index, obj);
    fireContentsChanged(this, index, index);
  }

  public void removeElementAt(final int index){
    _delegate.remove(index);
    fireIntervalRemoved(this, index, index);
  }

  public void insertElementAt(final Object obj, final int index){
    _delegate.add(index, obj);
    fireIntervalAdded(this, index, index);
  }

  public void addElement(final Object obj){
    final int index = _delegate.size();
    _delegate.add(obj);
    fireIntervalAdded(this, index, index);
  }

  public boolean removeElement(final Object obj){
    final int index = indexOf(obj);
    final boolean couldRemove = _delegate.remove(obj);
    if(index >= 0){
      fireIntervalRemoved(this, index, index);
    }
    return couldRemove;
  }

  public void removeAllElements(){
    final int index1 = _delegate.size() - 1;
    _delegate.clear();
    if(index1 >= 0){
      fireIntervalRemoved(this, 0, index1);
    }
  }

  @Override
  public void addListDataListener(final ListDataListener dataListener){
    synchronized(this){
      _listenerList.put(dataListener, _present);
    }
  }

  @Override
  public void removeListDataListener(final ListDataListener dataListener){
    synchronized(this){
      _listenerList.remove(dataListener);
    }
  }

  public EventListener[] getListeners(final Class listenerType){
    final Set<ListDataListener> set = _listenerList.keySet();
    return set.toArray(new EventListener[set.size()]);
  }

  protected void fireContentsChanged(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set = new HashSet<ListDataListener>(_listenerList.keySet());
      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.CONTENTS_CHANGED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.contentsChanged(event);
      }
    }
  }

  protected void fireIntervalAdded(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set =
              new HashSet<ListDataListener>(_listenerList.keySet());
      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.INTERVAL_ADDED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.intervalAdded(event);
      }
    }
  }

  protected void fireIntervalRemoved(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set =
              new HashSet<ListDataListener>(_listenerList.keySet());

      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.INTERVAL_REMOVED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.intervalRemoved(event);
      }
    }
  }
}
