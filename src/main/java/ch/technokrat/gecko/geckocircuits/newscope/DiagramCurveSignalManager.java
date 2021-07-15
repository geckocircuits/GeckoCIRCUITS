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

import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * If the number of Curves is changed, all diagrams have to be updated. Adding a
 * Diagram to the scope requires to insert the correct number of curves into the
 * diagram. Therefore, this behavior is encapsulated in this Manager. All
 * Adding/removing/replacing of Diagrams or curves should be done via this
 * manager. Outside the manager, only non-modifiable lists should be available,
 * to prevent by accident any modification from outside.
 *
 * @author andreas
 */
public final class DiagramCurveSignalManager{
  private final List<AbstractDiagram> _diagrams = new ArrayList<AbstractDiagram>();
  
  private final GraferV4 _grafer;
  private Stack<AbstractScopeSignal> _inputSignals;
  private final List<AbstractScopeSignal> _allScopeSignals = new ArrayList<AbstractScopeSignal>();

  public DiagramCurveSignalManager(final GraferV4 grafer){
    _grafer = grafer;
  }

  /**
   * if new curves have to be created, do it here!
   *
   * @param value
   */
  public void updateCurveNumber(final int value){
    for(AbstractDiagram diag : _diagrams){
      final List<AbstractCurve> oldCurves = diag.getCurves();
      if(value == oldCurves.size()){
        return;
      }
      final List<AbstractCurve> newCurves = new ArrayList<AbstractCurve>();

      for(int i = 0; i < Math.min(oldCurves.size(), value); i++){
        newCurves.add(oldCurves.get(i));
      }

      while(newCurves.size() < value){
        AbstractCurve newCurve = diag.curveFabric();
        newCurves.add(newCurve);
      }

      diag.setCurves(Collections.unmodifiableList(newCurves));
    }

  }

  public List<AbstractScopeSignal> getAllScopeSignals(){
    return Collections.unmodifiableList(_allScopeSignals);
  }

  public int getNumberInputSignals(){
    return _allScopeSignals.size();
  }

  /**
   * Adding or removing diagrams should only be allowed through the
   * corresponding manager functions! Therefore, return unmodifiable list!
   *
   * @return
   */
  public List<AbstractDiagram> getDiagrams(){
    return Collections.unmodifiableList(_diagrams);
  }

  AbstractDiagram getDiagram(final int index){
    return _diagrams.get(index);
  }

  public void deleteDiagram(final AbstractDiagram toDelete){
    _diagrams.remove(toDelete);
    _grafer.refreshComponentPane();
  }

  void addDiagram(final int newIndex, final AbstractDiagram toAdd){
    _diagrams.add(newIndex, toAdd);
    _grafer.refreshComponentPane();
  }

  public int getNumberDiagrams(){
    return _diagrams.size();
  }

  public void addDiagram(final AbstractDiagram newDiagram){
    final List<AbstractCurve> newCurves = new ArrayList<AbstractCurve>();
    for(int i = 0; i < _allScopeSignals.size(); i++){
      newCurves.add(newDiagram.curveFabric());
    }

    newDiagram.setCurves(Collections.unmodifiableList(newCurves));
    if(!_diagrams.isEmpty()){
      HiLoData oldLimits = _diagrams.get(_diagrams.size() - 1)._xAxis._axisMinMax.getLimits();
      newDiagram._xAxis._axisMinMax.setGlobalAutoScaleValues(oldLimits);
    }

    _diagrams.add(newDiagram);      
    _grafer.refreshComponentPane();
  }

  void replaceDiagram(final AbstractDiagram oldDiag, final AbstractDiagram newDiag){
    assert _diagrams.contains(oldDiag);
    final int index = _diagrams.indexOf(oldDiag);
    _diagrams.set(index, newDiag);
    newDiag.setCurves(newDiag.getCurvesCopy(oldDiag.getCurves()));
    _grafer.refreshComponentPane();
  }

  void swapDiagrams(final AbstractDiagram swap1, final AbstractDiagram swap2){
    assert _diagrams.contains(swap1);
    assert _diagrams.contains(swap2);

    final int index1 = _diagrams.indexOf(swap1);
    final int index2 = _diagrams.indexOf(swap2);

    _diagrams.set(index1, swap2);
    _diagrams.set(index2, swap1);

    _grafer.refreshComponentPane();
  }

  /**
   * insert a new mean signal curve after the selected curve, for which the mean
   * is calculated.
   *
   * @param newSignal
   */
  void defineNewMeanSignal(final ScopeSignalMean newSignal){
    final int insertionIndex = _allScopeSignals.indexOf(newSignal._connectedScopeSignal) + 1;
    _allScopeSignals.add(insertionIndex, newSignal);

    for(AbstractDiagram diag : getDiagrams()){
      final List<AbstractCurve> newCurveList = new ArrayList<AbstractCurve>();
      newCurveList.addAll(diag.getCurves());


      final AbstractCurve newCurve = new CurveRegular(diag);
      newCurveList.add(insertionIndex, newCurve);

      final AbstractCurve connectedCurve = diag.getCurve(insertionIndex - 1);

      if(!(connectedCurve.getAxisConnection() == AxisConnection.ZUORDNUNG_NIX)
              && !(connectedCurve.getAxisConnection() == AxisConnection.ZUORDNUNG_SIGNAL)){
        newCurve.setAxisConnection(connectedCurve.getAxisConnection());
      }

      diag.setCurves(newCurveList);
    }
  }

  /**
   * Appends an external signal the the list of scope signals.
   * @param externalSignal The external signal to be added to the list of scope
   * signals.
   */
  public void addExternalSignal(final ExternalSignal externalSignal){
    this._allScopeSignals.add(externalSignal);
    for(final AbstractDiagram diag : this.getDiagrams()){
      final List<AbstractCurve> newCurveList = new ArrayList<AbstractCurve>();
      newCurveList.addAll(diag.getCurves());
      final AbstractCurve newCurve = new CurveRegular(diag);
      newCurveList.add(newCurve);
      diag.setCurves(newCurveList);
    }
  }

  /**
   * Removes an external Signal from the list of scope signals
   * @param externalSignal The external signal to be removed from the list of
   * scope signals .
   */
  public void removeExternalSignal(final ExternalSignal externalSignal){
    List<AbstractCurve> newCurveList;
    for(AbstractDiagram diag : this.getDiagrams()){
      newCurveList = new ArrayList<AbstractCurve>();
      newCurveList.addAll(diag.getCurves());
      newCurveList.remove(this._allScopeSignals.indexOf(externalSignal));
      diag.setCurves(newCurveList);
    }
    this._allScopeSignals.remove(externalSignal);
  }

  public void setInputSignals(final Stack<AbstractScopeSignal> scopeInputSignals){            
    _inputSignals = scopeInputSignals;
    _allScopeSignals.clear();
    _allScopeSignals.addAll(scopeInputSignals);
    updateCurveNumber(_inputSignals.size());
  }

  void undefineMeanSignal(final ScopeSignalMean deleteSignal){
    for(AbstractDiagram diag : getDiagrams()){
      final List<AbstractCurve> newCurveList = new ArrayList<AbstractCurve>();
      newCurveList.addAll(diag.getCurves());
      final int deleteIndex = _allScopeSignals.indexOf(deleteSignal);
      newCurveList.remove(deleteIndex);
      diag.setCurves(newCurveList);
    }
    _allScopeSignals.remove(deleteSignal);
  }

  public void defineNewSignalNumber(final ReglerOSZI regler, final int newTerminalNumber, final DefinedMeanSignals meanSigs){
    assert _inputSignals != null;
    // increasing terminal number
    while(_inputSignals.size() < newTerminalNumber){
      final AbstractScopeSignal newSignal = new ScopeSignalRegular(_inputSignals.size(), regler);
      _inputSignals.add(newSignal);
      _allScopeSignals.add(newSignal);
    }

    // decreasing terminal number
    while(_inputSignals.size() > newTerminalNumber){
      final AbstractScopeSignal toRemoveSignal = _allScopeSignals.get(_allScopeSignals.size() - 1);
      if(toRemoveSignal instanceof ScopeSignalMean){
        meanSigs.unDefineMeanSignal((ScopeSignalMean)toRemoveSignal);
      }else{
        _allScopeSignals.remove(toRemoveSignal);
        _inputSignals.remove((ScopeSignalRegular)toRemoveSignal);
      }
    }

    updateCurveNumber(_allScopeSignals.size());
  }
    
}
