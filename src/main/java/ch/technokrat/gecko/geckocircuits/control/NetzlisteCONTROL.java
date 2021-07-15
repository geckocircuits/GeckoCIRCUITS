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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.control.calculators.InitializableAtSimulationStart;
import ch.technokrat.gecko.SystemOutputRedirect;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerGlobal;
import ch.technokrat.gecko.geckocircuits.circuit.NetzlisteAllg;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialArea;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public final class NetzlisteCONTROL {

    public static final DataContainerGlobal globalData = new DataContainerGlobal();
    private List<Integer> potIndex; // contains the potential indices for potentials connected with a scope
    private int[][] potIndexArray;
    //-------------------------------------------
    private AbstractControlCalculatable[] reglerCalc;
    private final List<RegelBlock> elementsControl = new ArrayList<RegelBlock>();
    private PotentialArea[] potLab;
    private String[] labelListeReglerKnoten;  // index ist Knotennummer und String-Eintrag ist zugehoeriger Label
    public RegelBlock[] _orderedControlBlocks;
    public final Map<Integer, IndexConnection> _scopePotentialMap = new HashMap<Integer, IndexConnection>();
    public final Map<CircuitSheet, HashMap<String, IndexConnection>> _labelNodeMap = new HashMap<CircuitSheet, HashMap<String, IndexConnection>>();
    public final List<String> _inPortsStringWithoutConnection = new ArrayList<String>();
    public final List<String> _outPortsStringWithoutConnection = new ArrayList<String>();
    public final List<AbstractTerminal> _inPortsWithoutConnection = new ArrayList<AbstractTerminal>();
    public final List<AbstractTerminal> _outPortsWithoutConnection = new ArrayList<AbstractTerminal>();
    private AbstractControlCalculatable[] _sortedControlBlocksNew;
    private AbstractControlCalculatable[] _allSortedCalculatables;
    public AbstractControlCalculatable[] _allUnSortedControlCalculators;
    private RegelBlock[] _sortedReglerWONonCalculatables;

    /**
     * use static factory method for object construction!
     */
    private NetzlisteCONTROL() {
    }

    public static NetzlisteCONTROL FabricContinueSimulation(NetzlisteCONTROL previousNetList) {
        final NetzlisteCONTROL returnValue = new NetzlisteCONTROL();
        returnValue.reglerCalc = previousNetList.reglerCalc;
        returnValue._labelNodeMap.putAll(previousNetList._labelNodeMap);
        returnValue._scopePotentialMap.putAll(previousNetList._scopePotentialMap);
        returnValue.potLab = previousNetList.potLab;
        returnValue._orderedControlBlocks = previousNetList._orderedControlBlocks;
        returnValue._sortedControlBlocksNew = previousNetList._sortedControlBlocksNew;
        returnValue.elementsControl.addAll(previousNetList.elementsControl);
        returnValue._allSortedCalculatables = previousNetList._allSortedCalculatables;
        returnValue._allUnSortedControlCalculators = previousNetList._allUnSortedControlCalculators;
        returnValue.setGlobalDatacontainer();
        return returnValue;
    }

    public static NetzlisteCONTROL FabricUpdateGui(final NetzlisteAllg nlC) {
        final NetzlisteCONTROL returnValue = new NetzlisteCONTROL();
        returnValue.connectPotentialLabels(nlC);
        return returnValue;
    }

    public static NetzlisteCONTROL FabricRunSimulation(final NetzlisteAllg nlC) {
        SystemOutputRedirect.reset();
        final NetzlisteCONTROL returnValue = new NetzlisteCONTROL();
        returnValue.connectPotentialLabels(nlC);
        returnValue.removeMuxAndDemux();
        returnValue.optimiereAbarbeitungsListe();
        returnValue.setGlobalDatacontainer();
        returnValue.createControlCalculators();
        returnValue.initializeConnections();
        return returnValue;
    }

    public static List<RegelBlock> getOptimizedList(final NetzlisteAllg nlA) {
        final NetzlisteCONTROL nlC = new NetzlisteCONTROL();
        nlC.connectPotentialLabels(nlA);
        nlC.optimiereAbarbeitungsListe();
        List<RegelBlock> optimizedList = new ArrayList<RegelBlock>();
        return Arrays.asList(nlC._orderedControlBlocks);
    }

    public RegelBlock[] getElementCONTROL() {
        return elementsControl.toArray(new RegelBlock[0]);
    }

    public RegelBlock[] getEnabledElementCONTROL() {
        List<RegelBlock> enabledControls = new ArrayList<RegelBlock>();
        for (RegelBlock ctrl : elementsControl) {
            if (ctrl.isCircuitEnabled() == Enabled.ENABLED) {
                enabledControls.add(ctrl);
            }
        }
        return enabledControls.toArray(new RegelBlock[0]);
    }

    public String[] getLabelListeReglerKnoten() {
        return labelListeReglerKnoten;
    }

    private void connectPotentialLabels(final NetzlisteAllg nlA) {

        potLab = nlA.getPotentiale();

        for (AbstractBlockInterface elem : nlA.getElemente()) {
            if (elem != null && elem.isCircuitEnabled() != Enabled.DISABLED && !(elem instanceof ReglerTERMINAL)) {
                elementsControl.add((RegelBlock) elem);
            }
        }

        _labelNodeMap.clear();
        for (int i1 = 0; i1 < elementsControl.size(); i1++) {
            // Anfangsknoten:
            List<AbstractTerminal> startNodes = elementsControl.get(i1).XIN;
            for (int i2 = 0; i2 < startNodes.size(); i2++) {
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(startNodes.get(i2))) {
                        ((ControlTerminable) elementsControl.get(i1).XIN.get(i2)).setNodeNumber(i3);
                    }
                }
            }
            // Endknoten:
            List<AbstractTerminal> endNodes = elementsControl.get(i1).YOUT;  // x-Werte (ScematicEntry) der Endknoten
            for (int i2 = 0; i2 < endNodes.size(); i2++) {
                AbstractTerminal endNode = endNodes.get(i2);
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(endNode)) {
                        ((ControlTerminable) endNode).setNodeNumber(i3);
                        String label = potLab[i3].getLabel();
                        if (!label.isEmpty() && !_labelNodeMap.containsKey(label)) {
                            CircuitSheet circuitSheet = elementsControl.get(i1).getParentCircuitSheet();
                            HashMap<String, IndexConnection> map = null;
                            if (_labelNodeMap.containsKey(circuitSheet)) {
                                map = _labelNodeMap.get(circuitSheet);
                            } else {
                                map = new HashMap<String, IndexConnection>();
                                _labelNodeMap.put(circuitSheet, map);
                            }
                            map.put(label, new IndexConnection(i1, i2));
                        }
                    }
                }
            }
        }

        labelListeReglerKnoten = new String[potLab.length];
        for (int i1 = 0; i1 < potLab.length; i1++) {
            labelListeReglerKnoten[i1] = potLab[i1].getLabel();
        }
    }

    public void initializeConnections() {
        _outPortsStringWithoutConnection.clear();
        _outPortsWithoutConnection.clear();

        for (int ii = 0; ii < elementsControl.size(); ii++) {
            RegelBlock reg = elementsControl.get(ii);
            for (int i = 0; i < reg.YOUT.size(); i++) {
                boolean connectionDone = false;
                for (RegelBlock reg2 : elementsControl) {
                    for (int j = 0; j < reg2.XIN.size(); j++) {
                        if (((ControlTerminable) reg.YOUT.get(i)).getNodeNumber() == ((ControlTerminable) reg2.XIN.get(j)).getNodeNumber()) {
                            reg2.setInputSignal(j, reg, i);
                            connectionDone = true;
                        }
                    }
                }

                if (!connectionDone) {
                    AbstractTerminal term = elementsControl.get(ii).YOUT.get(i);
                    String labelString = term.getLabelObject().getLabelString();
                    if (labelString.isEmpty()) {
                        labelString = "empty";
                    }
                    _outPortsStringWithoutConnection.add(reg.getStringID() + " Port no. " + i + " label: "
                            + labelString);
                    _outPortsWithoutConnection.add(term);
                }
            }

        }

        _inPortsStringWithoutConnection.clear();
        _inPortsWithoutConnection.clear();
        for (RegelBlock reg : elementsControl) {
            for (int i = 0; i < reg.XIN.size(); i++) {
                if (reg.checkInputWithoutConnectionAndFillInput(i)) {
                    _inPortsStringWithoutConnection.add(reg.getStringID() + " Port no. " + i + " label: " + reg.XIN.get(i).getLabelObject().getLabelString());
                    _inPortsWithoutConnection.add(reg.XIN.get(i));
                }
            }

        }
    }

    public IndexConnection getIndexConnection(CircuitSheet searchCircuitSheet, String searchLabel) {
        HashMap<String, IndexConnection> map = _labelNodeMap.get(searchCircuitSheet);
        if (map != null) {
            return map.get(searchLabel);
        } else {

            return null;
        }
    }

    public void initializeAtSimulationStart(final double dt) {
        for (AbstractControlCalculatable calculatable : _sortedControlBlocksNew) {
            if (calculatable instanceof InitializableAtSimulationStart) {
                ((InitializableAtSimulationStart) calculatable).initializeAtSimulationStart(dt);
            }
        }
    }

    public void doMemorInits(final double deltaT) {
        for (AbstractControlCalculatable calculatable : _allSortedCalculatables) {
            if (calculatable instanceof MemoryInitializable) {
                ((MemoryInitializable) calculatable).doInit(deltaT);
            }
        }
    }

    public void doDtChangeInit(double deltaT) {
        for (AbstractControlCalculatable calculatable : _sortedControlBlocksNew) {
            if (calculatable instanceof IsDtChangeSensitive) {
                ((IsDtChangeSensitive) calculatable).initWithNewDt(deltaT);
            }
        }
    }

    public AbstractControlCalculatable[] getSortedControlCalculators() {
        return _allSortedCalculatables;
    }

    /**
     * mux and demuxes could lead to control loops, which are not real loops,
     * since the mux/demux just forwards the signals. This block removes all
     * muxes and demuxes, and corrects the input/output indices of the remaining
     * blocks.
     */
    private void removeMuxAndDemux() {
        Map<Integer, ArrayList<ReglerDemux>> demuxes = new LinkedHashMap<Integer, ArrayList<ReglerDemux>>();
        Map<Integer, ReglerMUX> muxes = new LinkedHashMap<Integer, ReglerMUX>();
        for (RegelBlock block : elementsControl.toArray(new RegelBlock[0])) {
            if (block instanceof ReglerMUX) {
                if (block.XIN.size() > 1) {
                    int nodeIndex = ((TerminalControl) block.YOUT.get(0)).getNodeNumber();
                    assert !muxes.containsKey(nodeIndex);
                    muxes.put(nodeIndex, (ReglerMUX) block);
                }
            }

            if (block instanceof ReglerDemux) {
                if (block.YOUT.size() > 1) {
                    int nodeIndex = ((TerminalControl) block.XIN.get(0)).getNodeNumber();
                    ArrayList<ReglerDemux> insertList = null;
                    if (demuxes.containsKey(nodeIndex)) {
                        insertList = demuxes.get(nodeIndex);
                    } else {
                        insertList = new ArrayList<ReglerDemux>();
                        demuxes.put(nodeIndex, insertList);
                    }
                    insertList.add((ReglerDemux) block);
                }
            }
        }
        Random randomIndex = new Random();

        for (Entry<Integer, ReglerMUX> muxEntry : muxes.entrySet()) {
            ReglerMUX mux = muxEntry.getValue();

            if (demuxes.containsKey(muxEntry.getKey())) {
                //elementsControl.remove(mux);
                for (ReglerDemux demux : demuxes.get(muxEntry.getKey())) {
                    //elementsControl.remove(demux);
                    //System.out.print(" " + demux.getStringID() + " ");
                    assert demux.YOUT.size() == mux.XIN.size();
                    for (int i = 0; i < mux.XIN.size(); i++) {
                        int muxInputIndex = ((ControlTerminable) mux.XIN.get(i)).getNodeNumber();
                        int demuxOutputIndex = ((ControlTerminable) demux.YOUT.get(i)).getNodeNumber();

                        for (RegelBlock regelblock : elementsControl) {
                            for (AbstractTerminal term : regelblock.XIN) {
                                ControlTerminable cTerm = (ControlTerminable) term;
                                if (cTerm.getNodeNumber() == demuxOutputIndex) {
                                    cTerm.setNodeNumber(muxInputIndex);
                                    ((ControlTerminable) demux.YOUT.get(i)).setNodeNumber(10000 + randomIndex.nextInt());
                                }
                            }
                        }
                    }
                }
            } else {
                //System.out.println("mux without input:  " + muxEntry.getValue().getStringID());
            }
        }
    }

    public class IndexConnection { // to avoid the Integer-Object usage

        public final int _elementIndex;
        public final int _inBlockIndex_outputIndex;

        public IndexConnection(final int elementIndex, final int inBlockIndex) {
            _elementIndex = elementIndex;
            _inBlockIndex_outputIndex = inBlockIndex;

        }
    }

    public void berechneZeitschritt(final double deltaT, final double time) {
        AbstractControlCalculatable.setTime(time);

////        if (!initDone) {
////            initDone = true;
////                try {
////                    bufReader = new BufferedReader(new FileReader(compareFile));
////                    
////                } catch (IOException ex) {
////                    Logger.getLogger(NetzlisteCONTROL.class.getName()).log(Level.SEVERE, null, ex);
////                }
////        }
////        try {
////            String readLine = bufReader.readLine();
////            String timeCompare = "    simulation time: " + time;
////            assert readLine.equals(timeCompare) : readLine + " xxx " + timeCompare;
//        counter++;
//        if (counter == 2) {
//            for(AbstractControlCalculatable calc : _sortedControlBlocksNew) {
//                System.out.print(calc.getClass().getName() + " ");
//                for(int i = 0; i < calc._inputSignal.length; i++) {
//                    System.out.print(calc._inputSignal[i][0] + " ");
//                }
//                System.out.print("     xxxx   ");
//                for(int i = 0; i < calc._outputSignal.length; i++) {
//                    System.out.print(calc._outputSignal[i][0] + " ");
//                }
//                System.out.println("");
//            }                        
//            System.exit(3);
//        }
        for (int i = 0; i < _sortedControlBlocksNew.length; i++) {
            _sortedControlBlocksNew[i].berechneYOUT(deltaT);
        }        

//        for(AbstractControlCalculatable calc : _sortedControlBlocksNew) {
//            for(int i =0 ;i < calc._outputSignal.length; i++) {
//                for(int j = 0; j < calc._outputSignal[i].length; j++) {
//                    double value = calc._outputSignal[i][j];
//                    if(value != value) {
//                        for(int k = 0; k < _sortedReglerWONonCalculatables.length; k++) {
//                            if(_sortedReglerWONonCalculatables[k]._calculator == calc) {
//                                System.out.println("calculator " +  _sortedReglerWONonCalculatables[k].getStringID() + " " + i + " " + j);
//                            }
//                        }                        
//                    }
//                    
//                }
//            }
//        }
        writeData(time);
    }

    static int counter = 0;

    // Wird erst in 'SimulationsKern' aufgerufen, und zwar dort, wo die Simulation wirklich gestartet wird 
    // ... damit wird vermieden, dass beim Aufbau der Regelung im SchematicEntry jedesmal, wenn ein Element oder ein Parameter 
    // geaendert wird, und die NetzlisteCONTROL neu gebaut wird, auch die (bei grossen Regelstrukturen) sehr zeitintensive 
    // Berechnung der Bauelement-Optimierung durchgefuehrt wird und das SchematicEntry entsprechend traege auf den User reagiert 
    // 
    public void optimiereAbarbeitungsListe() {
        //BlockOrderOptimizerNew blockOrderOtimizerNew = new BlockOrderOptimizerNew(elementsControl);
        //_orderedControlBlocks = blockOrderOtimizerNew.getOptimierteAbarbeitungsListe().toArray(new RegelBlock[0]);

        BlockOrderOptimizer3 blockOrderOtimizer3 = new BlockOrderOptimizer3(elementsControl);
        _orderedControlBlocks = blockOrderOtimizer3.getOptimierteAbarbeitungsListe().toArray(new RegelBlock[0]);
                
        
//        for(int i = 0; i < _orderedControlBlocks.length; i++) {
//            System.out.println("rb " + i + " " + _orderedControlBlocks[i]);
//        }        
    }

    public void createControlCalculators() {
        _sortedControlBlocksNew = new AbstractControlCalculatable[_orderedControlBlocks.length];
        final List<AbstractControlCalculatable> _sortedControlBlocksTmp = new ArrayList<AbstractControlCalculatable>();
        _allSortedCalculatables = new AbstractControlCalculatable[elementsControl.size()];

        _allUnSortedControlCalculators = new AbstractControlCalculatable[elementsControl.size()];

        List<RegelBlock> sortedWONonCalculators = new ArrayList<RegelBlock>();

        for (int i = 0; i < _orderedControlBlocks.length; i++) {
            try {
                AbstractControlCalculatable calc = _orderedControlBlocks[i].getInternalControlCalculatableForSimulationStart();
                _allSortedCalculatables[i] = calc;
                _orderedControlBlocks[i].setActiveCalculator(calc);

                _allUnSortedControlCalculators[elementsControl.indexOf(_orderedControlBlocks[i])] = calc;
                if (calc != null && !(calc instanceof NotCalculateableMarker)) {
                    _sortedControlBlocksTmp.add(calc);
                    sortedWONonCalculators.add(_orderedControlBlocks[i]);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Control block in " + _orderedControlBlocks[i].getStringID()
                        + ":\n" + ex.getMessage());
            }

        }
        _sortedReglerWONonCalculatables = sortedWONonCalculators.toArray(new RegelBlock[0]);
        _sortedControlBlocksNew = _sortedControlBlocksTmp.toArray(new AbstractControlCalculatable[0]);
    }

    private void setGlobalDatacontainer() {
        // contains coordinates of all inputs, from all scopes.
        // [0 for x, 1 for y coord][inputID]
        List<List<AbstractTerminal>> inputCoords = new ArrayList<List<AbstractTerminal>>();
        List<Integer> scopeIndices = new ArrayList<Integer>();
        potIndex = new ArrayList<Integer>();
        int numRows = 0;

        int maxNumOfInput = 0;
        for (int i = 0; i < _orderedControlBlocks.length; i++) {
            final RegelBlock regler = _orderedControlBlocks[i];
            if (regler instanceof ReglerOSZI) {
                Collection<AbstractTerminal> inputs = regler.XIN;
                inputCoords.add(new ArrayList<AbstractTerminal>(inputs));
                scopeIndices.add(i);
                maxNumOfInput = Math.max(maxNumOfInput, inputs.size());
            }
        }

        _scopePotentialMap.clear();
        // iterate over all scopes
        for (int scopeInd = 0; scopeInd < inputCoords.size(); scopeInd++) {
            List<AbstractTerminal> scopeInput = inputCoords.get(scopeInd);
            int[] dataIndex = new int[scopeInput.size()];
            // iterate over all scope inputs
            for (int inputInd = 0; inputInd < scopeInput.size(); inputInd++) {
                // iterate over all potentials
                for (int potGebInd = 0; potGebInd < potLab.length; potGebInd++) {
                    PotentialArea potArea = potLab[potGebInd];
                    Set<TerminalInterface> potentialTerminals = potArea.getAllTerminals();
                    final AbstractTerminal scopeTerminal = scopeInput.get(inputInd);
                    if (potentialTerminals.contains(scopeTerminal)) {
                        if (!potIndex.contains(potGebInd)) {
                            _scopePotentialMap.put(potGebInd, new IndexConnection(scopeIndices.get(scopeInd), inputInd));
                            potIndex.add(potGebInd);
                            numRows++;
                        }
                        dataIndex[inputInd] = potIndex.indexOf(potGebInd);
                    }
                }
            }
            // TIBOR System.out.println(Arrays.toString(dataIndex));
            ((ReglerOSZI) (_orderedControlBlocks[scopeIndices.get(scopeInd)])).setDataContainerIndices(dataIndex);
        }

        String[] dataNames = new String[potIndex.size()];
        int counter = 0;
        potIndexArray = new int[potIndex.size()][2];
        for (int index : potIndex) {
            dataNames[counter] = potLab[index].getLabel();
            potIndexArray[counter][0] = _scopePotentialMap.get(index)._elementIndex;
            potIndexArray[counter][1] = _scopePotentialMap.get(index)._inBlockIndex_outputIndex;
            counter++;
        }

        globalData.clear();
        ((DataContainerGlobal) globalData).init(potIndex.size(), dataNames, "t");

        for (RegelBlock reg : elementsControl) {
            if (reg != null && reg instanceof ReglerCISPR16) {
                ((ReglerCISPR16) reg).loescheZVDatenImRAM();
            }
        }
    }

    /**
     * write the selected scope data to the global data container. Be careful,
     * we use an "array-cache", so that we don't have to re-create the arrays,
     * which is a problem for the garbage-collection.
     *
     * @param time
     */
    private void writeData(final double time) {
        try {
            float[] scopeData = globalData.getDataArray();
            for (int i = 0; i < potIndexArray.length; i++) {
                scopeData[i] = (float) _allSortedCalculatables[potIndexArray[i][0]]._inputSignal[potIndexArray[i][1]][0];
            }

            globalData.insertValuesAtEnd(scopeData, time);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gives every Controlblock the chance to free its resources if the
     * simulation is paused
     */
    public void tearDownOnPause() {
        for (int i = 0; i < _sortedControlBlocksNew.length; i++) {
            _sortedControlBlocksNew[i].tearDownOnPause();
        }
    }
}
