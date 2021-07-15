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
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerMeanWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author andreas
 */
public final class DefinedMeanSignals{
  private final List<ScopeSignalMean> _scopeMeanSignals = new ArrayList<ScopeSignalMean>();
  private DataContainerMeanWrapper _meanWrapper;
  private GraferV4 _grafer;
  private final Stack<AbstractScopeSignal> _origScopeSignals;

  public DefinedMeanSignals(final Stack<AbstractScopeSignal> origScopeSignals){
    _origScopeSignals = origScopeSignals;
  }

  public void setGrafer(final GraferV4 grafer){
    _grafer = grafer;
  }

  public void exportIndividualCONTROL(final StringBuffer ascii){

    final int[] tmpSignalIndices = this.getSignalIndices();
    final double[] tmpSignalTimes = this.getSignalTimes();

    DatenSpeicher.appendAsString(ascii.append("\navgIndices"), tmpSignalIndices);
    DatenSpeicher.appendAsString(ascii.append("\navgValues"), tmpSignalTimes);
  }

  public void importIndividualCONTROL(final TokenMap tokenMap){

    if(tokenMap.containsToken("avgIndices[]")){
      final int[] tmpSignalIndices = tokenMap.readDataLine("avgIndices[]", new int[0]);
      final List<Double> tmpSignalTimes = tokenMap.readDataLineDoubleArray("avgValues[]");

      for(int i = 0; i < tmpSignalIndices.length; i++){
        ScopeSignalMean newSignal = new ScopeSignalMean(_origScopeSignals.get(tmpSignalIndices[i]), tmpSignalTimes.get(i));
        _scopeMeanSignals.add(newSignal);
        _grafer.getManager().defineNewMeanSignal(newSignal);
      }
    }

  }

  public Stack<AbstractScopeSignal> getOrigScopeSignals(){
    return _origScopeSignals;
  }

  public ScopeSignalMean findMeanSignal(final AbstractScopeSignal origSignal){
    for(ScopeSignalMean candidate : _scopeMeanSignals){
      if(candidate._connectedScopeSignal.equals(origSignal)){
        return candidate;
      }
    }
    return null;
  }

  public void registerIndices(final DataContainerMeanWrapper meanWrapper){
    meanWrapper.defineMeanSignals(_scopeMeanSignals);
    _meanWrapper = meanWrapper;
  }

  public void defineNewMeanSignal(final AbstractScopeSignal origSignal, final double value){

    final ScopeSignalMean newSignal = new ScopeSignalMean(origSignal, value);
    _grafer.getManager().defineNewMeanSignal(newSignal);


    if(_scopeMeanSignals.contains(newSignal)){
      _scopeMeanSignals.remove(newSignal);
    }

    // do a sorted insertion! ------------>
    for(int i = 0; i < _scopeMeanSignals.size(); i++){
      if(newSignal.getConnectedScopeInputIndex() > _scopeMeanSignals.get(i).getConnectedScopeInputIndex()){
        _scopeMeanSignals.add(i, newSignal);
        break;
      }
    }

    if(!_scopeMeanSignals.contains(newSignal)){
      _scopeMeanSignals.add(newSignal);
    }
    if(_meanWrapper != null){  // when no simulation was done before, this could be null!
      registerIndices(_meanWrapper);
    }


  }

  void unDefineMeanSignal(final ScopeSignalMean toDelete){
    _grafer.getManager().undefineMeanSignal(toDelete);
    _scopeMeanSignals.remove(toDelete);
    _meanWrapper.removeSignal(toDelete.getConnectedScopeInputIndex() + 1);
    registerIndices(_meanWrapper);
  }

  public int[] getSignalIndices(){
    int[] returnValue = new int[_scopeMeanSignals.size()];
    for(int i = 0; i < _scopeMeanSignals.size(); i++){
      returnValue[i] = _scopeMeanSignals.get(i).getConnectedScopeInputIndex();
    }
    return returnValue;
  }

  public double[] getSignalTimes(){
    double[] returnValue = new double[_scopeMeanSignals.size()];
    for(int i = 0; i < _scopeMeanSignals.size(); i++){
      returnValue[i] = _scopeMeanSignals.get(i).getAveragingTime();
    }
    return returnValue;
  }
}
