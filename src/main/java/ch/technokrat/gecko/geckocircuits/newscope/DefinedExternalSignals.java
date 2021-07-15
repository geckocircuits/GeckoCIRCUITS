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
//package ch.technokrat.gecko.geckocircuits.newscope;
//
//import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerExternalWrapper;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Stack;
//
///**
// * Contains the currently defined external signals within the current data
// * container.
// */
//public final class DefinedExternalSignals{
//  private final List<ExternalSignal> _scopeExternalSignals = new ArrayList<ExternalSignal>();
//  private DataContainerExternalWrapper _externalWrapper;
//  private GraferV4 _grafer;
//  private final Stack<ScopeSignalRegular> _origScopeSignals;
//
//  /**
//   * Constructor saving the signals contained in the current scope before adding
//   * any external signals.
//   * @param origScopeSignals The original scope signals.
//   */
//  public DefinedExternalSignals(final Stack<ScopeSignalRegular> origScopeSignals){
//    this._origScopeSignals = origScopeSignals;
//  }
//
//  public void setGrafer(final GraferV4 grafer){
//    this._grafer = grafer;
//  }
//
//  /**
//   * Returns the signal at index.
//   * @param index The index of the external signal to return.
//   * @return The signal at index as an ExternalSignal object.
//   */
//  public ExternalSignal get(final int index){
//    return this._scopeExternalSignals.get(index);
//  }
//
//  public int size(){
//    return this._scopeExternalSignals.size();
//  }
//
//  public Stack<ScopeSignalRegular> getOrigScopeSignals(){
//    return _origScopeSignals;
//  }
//
//  /**
//   * Register all external signals in the external wrapper.
//   * @param externalWrapper The wrapper for which the external signals should be
//   * registered.
//   */
//  public void registerIndices(final DataContainerExternalWrapper externalWrapper){
//    externalWrapper.defineExternalSignals(this._scopeExternalSignals);
//    this._externalWrapper = externalWrapper;
//  }
//
//  /**
//   * Add an external signal to the list of defined external signals.
//   * @param index The signal to add.
//   */
//  public void defineNewExternalSignal(final ExternalSignal newSignal){
//    this._grafer.getManager().addExternalSignal(newSignal);
//    if(!this._scopeExternalSignals.contains(newSignal)){
//      this._scopeExternalSignals.add(newSignal);
//    }
//    if(this._externalWrapper != null){  // when no simulation was done before, this could be null!
//      this.registerIndices(this._externalWrapper);
//    }
//  }
//
//  /**
//   * Remove an external signal from the list of defined external signals.
//   * @param toDelete A reference to the external signal to delete.
//   */
//  public void unDefineExternalSignal(final ExternalSignal toDelete){
//    this._grafer.getManager().removeExternalSignal(toDelete);
//    this._externalWrapper.removeSignal(this._scopeExternalSignals.size() - this._scopeExternalSignals.indexOf(toDelete) - 1);
//    this._scopeExternalSignals.remove(toDelete);
//    this.registerIndices(this._externalWrapper);
//  }
//}
