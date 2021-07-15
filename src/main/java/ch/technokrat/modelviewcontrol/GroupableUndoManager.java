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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public final class GroupableUndoManager extends UndoManager{
  @Override
  public synchronized void undo() throws CannotUndoException{
    super.undo();
  }

  @Override
  public synchronized boolean addEdit(final UndoableEdit anEdit){
        //System.out.println("adding edit: " + anEdit.getUndoPresentationName());
    return super.addEdit(anEdit);
  }

  @Override
  public synchronized void redo(){
    super.redo();
  }

  public static final class GroupUndoStart implements UndoableEdit{
    private final List<UndoableEdit> _mergedEdits = new ArrayList<UndoableEdit>();
    private boolean otherEditsAccepted = true;

    @Override
    public void undo(){
      // nothing todo here - the stop event contains all operations!
    }

    @Override
    public boolean canUndo(){
      return true;
    }

    @Override
    public void redo(){
      // nothing todo - operations done by stop event.            
    }

    @Override
    public boolean canRedo(){
      return true;
    }

    @Override
    public void die(){
      _mergedEdits.clear();
    }

    @Override
    public boolean addEdit(final UndoableEdit anEdit){
      if(anEdit instanceof GroupUndoStop){
        final GroupUndoStop stop = (GroupUndoStop)anEdit;
        if(stop._matchingStart == this){
          stop.addGroupOfOperations(_mergedEdits);
          otherEditsAccepted = false; // "close" the merging functionality!
          return false;
        }
      }
      _mergedEdits.add(anEdit);
      return otherEditsAccepted;
    }

    @Override
    public boolean replaceEdit(final UndoableEdit anEdit){
      return false;
    }

    @Override
    public boolean isSignificant(){
      return false;
    }

    @Override
    public String getPresentationName(){
      return "not available";
    }

    @Override
    public String getUndoPresentationName(){
      return "not available";
    }

    @Override
    public String getRedoPresentationName(){
      return "not available";
    }
  }

  public final static class GroupUndoStop implements UndoableEdit{
    private final GroupUndoStart _matchingStart;
    private List<UndoableEdit> _editList;
    private UndoableEdit _parentEdit;

    public GroupUndoStop(final GroupUndoStart matchingStart){
      _matchingStart = matchingStart;
    }

    public void setParentEditForInfo(final UndoableEdit edit){
      _parentEdit = edit;
    }

    @Override
    public void undo() throws CannotUndoException{

      for(int i = 0; i < _editList.size(); i++){
        _editList.get(i).undo();
      }
    }

    @Override
    public boolean canUndo(){
      return true;
    }

    void addGroupOfOperations(final List<UndoableEdit> editList){
      _editList = editList;
    }

    @Override
    public void redo() throws CannotRedoException{
      for(int i = _editList.size() - 1; i >= 0; i--){
        _editList.get(i).redo();
      }
    }

    @Override
    public boolean canRedo(){
      return true;
    }

    @Override
    public void die(){
      _editList.clear();
    }

    @Override
    public boolean addEdit(final UndoableEdit anEdit){
      return false;
    }

    @Override
    public boolean replaceEdit(final UndoableEdit anEdit){
      return false;
    }

    @Override
    public boolean isSignificant(){
      return true;
    }

    @Override
    public String getPresentationName(){
      if(_parentEdit != null){
        return _parentEdit.getPresentationName();
      }else{
        return "Multi operation";
      }
    }

    @Override
    public String getUndoPresentationName(){
      if(_parentEdit != null){
        return _parentEdit.getUndoPresentationName();
      }else{
        return "Operation on multiple components";
      }
    }

    @Override
    public String getRedoPresentationName(){
      if(_parentEdit != null){
        return _parentEdit.getRedoPresentationName();
      }else{
        return "Operation on multiple components";
      }
    }
  }
}
