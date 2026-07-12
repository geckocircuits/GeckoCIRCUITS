/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.geckocircuits.circuit;

import gecko.geckocircuits.circuit.circuitcomponents.ThermAmbient;
import gecko.geckocircuits.control.Point;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.*;

@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
        justification = "Public field for singularity indices shared with matrix operations")
public final class NetlistGeneral {



    private final List<PotentialArea> _potentialAreas = new ArrayList<PotentialArea>();
    final List<Connection> _connections = new ArrayList<Connection>();
    private final List<AbstractBlockInterface> _allElements;
    public int[] _singularityIndices = new int[0];

    public List<AbstractBlockInterface> getElemente() {
        return Collections.unmodifiableList(_allElements);
    }

    // // Result (used to create the netlist, i.e. linking the elements via the potentials):
    public PotentialArea[] getPotentiale() {
        return _potentialAreas.toArray(new PotentialArea[0]);
    }
    /**
     * Use the fabric methods for object creation!!!
     *
     * @param connectors
     * @param components
     */
    private NetlistGeneral(final Collection<Connection> connectors, final List<? extends AbstractBlockInterface> components) {
        this._connections.addAll(connectors);
        this._allElements = Collections.unmodifiableList(components);
    }

    /**
     * build the complete netlist, including connections via geometry and
     * identical labels.
     *
     * @param connectors
     * @param components
     * @return
     */
    public static NetlistGeneral fabricNetzlistComplete(final Collection<Connection> connectors,
            final List<? extends AbstractBlockInterface> components) {

        final NetlistGeneral returnValue = new NetlistGeneral(connectors, components);

        returnValue.createPotentialSheetConnectedGeometric();  // --> pot[]
        returnValue.mergePotentialAreasViaLabels();  // --> potUeberLabelsVerbunden[]

        returnValue.resetAllLabelPriorities();
        returnValue.connectSubCircuitInOutTerminals();

        returnValue.connectGlobalTerminals();
        returnValue.removeEmptyPotentials();

        return returnValue;
    }

    public static NetlistGeneral fabricNetzlistDisabledParentSubsRemoved(final Collection<Connection> connectors,
            final List<? extends AbstractBlockInterface> components) {

        final List<AbstractBlockInterface> eFilteredParentsEnabled = new ArrayList<AbstractBlockInterface>();

        for (AbstractBlockInterface elem : components) {
            if (elem.allParentSubcircuitsEnabled()) {
                eFilteredParentsEnabled.add(elem);
            }
        }

        final List<Connection> vFilteredParentsEnabled = new ArrayList<Connection>();

        for (Connection verb : connectors) {
            if (verb.allParentSubcircuitsEnabled()) {
                vFilteredParentsEnabled.add(verb);
            }
        }

        return fabricNetzlistComplete(vFilteredParentsEnabled, eFilteredParentsEnabled);
    }

    public static NetlistGeneral fabricNetzlistComponentLabelUpdate(AbstractBlockInterface element, ConnectorType terminalType) {

        CircuitSheet elementParent = element.getParentCircuitSheet();

        final List<? extends AbstractBlockInterface> elementsList = elementParent._se.getElementInterface(terminalType);
        final Set<Connection> verbindung = elementParent.getConnection(terminalType);

        List<AbstractBlockInterface> filteredList = new ArrayList<AbstractBlockInterface>();
        for (AbstractBlockInterface ab : elementsList) {
            if (java.util.Objects.equals(ab.getParentCircuitSheet(), elementParent)) {
                filteredList.add(ab);
            }
        }

        NetlistGeneral returnValue = new NetlistGeneral(verbindung, filteredList);
        returnValue.createPotentialSheetConnectedGeometric();  // --> pot[]
        returnValue.resetAllLabelPriorities();
        return returnValue;
    }

    /**
     * build a netlist, but do not make connections via labels (same name of
     * nodes or connectors).
     *
     * @param connectors
     * @param components
     * @return
     */
    public static NetlistGeneral fabricNetzlistLabelUpdate(final Collection<Connection> connectors,
            final List<? extends AbstractBlockInterface> components) {

        final NetlistGeneral returnValue = new NetlistGeneral(connectors, components);

        returnValue.createPotentialSheetConnectedGeometric();  // --> pot[]
        returnValue.resetAllLabelPriorities();

        return returnValue;
    }


    // Verbindungen und Element-Knoten werden in PotentialGebieten zusammengefasst --> pot[]
    private void createPotentialSheetConnectedGeometric() {
        //-----------------------------------
        // // Connections and element nodes are combined in potential areas --> pot[]
        // gleichlautenden Labels behaftete Knoten in sogenannte "Potentiale"
        // (--> PotentialGebiet) zusammengefasst :
        //
        // zuerst: pro Connection ein Potentialgebiet:

        for (Connection verb : _connections) {
            PotentialArea connectorPotential = PotentialArea.fabricFromConnector(verb);
            _potentialAreas.add(connectorPotential);
        }

        for (AbstractBlockInterface elem : _allElements) {
            for (TerminalInterface term : elem.getAllTerminals()) {
                _potentialAreas.add(PotentialArea.fabricElementTerminal(elem, term));
            }
        }

        boolean continueLoop = true;

        while (continueLoop) {
            continueLoop = testMergeConnection();
        }
    }

    // die PotentialGebiete werden nun auch ueber gleichlautende Labels verknuepft, wo keine
    // direkte (optische) Connection am Bildschirm besteht --> potUeberLabelsVerbunden[]
    // // the potential areas are now also linked via identical labels where there are none
    private void mergePotentialAreasViaLabels() {

        boolean continueLoop = true;

        while (continueLoop) {
            continueLoop = false;
            final PotentialArea[] array = _potentialAreas.toArray(new PotentialArea[0]);
            for (int i = 0; i < array.length; i++) {
                for (int j = i + 1; j < array.length; j++) {
                    final PotentialArea pot1 = array[i];
                    final PotentialArea pot2 = array[j];

                    if (PotentialArea.testForLabelConnection(pot1, pot2)) {
                        final PotentialArea merged = pot1.mergePotential(pot2, true);
                        _potentialAreas.remove(pot1);
                        _potentialAreas.remove(pot2);
                        _potentialAreas.add(merged);
                        continueLoop = true;
                    }
                }
            }
        }

        for (PotentialArea pot : _potentialAreas) {
            pot.checkForDoubleTerminalLabels();
        }
    }

    private void resetAllLabelPriorities() {
        for (PotentialArea pot : _potentialAreas) {
            pot.clearLabelPriorities();
        }
    }

    private void removeEmptyPotentials() {

        for (PotentialArea pot : _potentialAreas.toArray(new PotentialArea[0])) {
            if (pot.isEmptyPotential()) {
                _potentialAreas.remove(pot);
            }
        }

    }

    private void connectGlobalTerminals() {
        boolean continueLoop = true;
        while (continueLoop) {
            continueLoop = false;
            final PotentialArea[] compare1Array = _potentialAreas.toArray(new PotentialArea[0]);
            final PotentialArea[] compare2Array = _potentialAreas.toArray(new PotentialArea[0]);

            for (int i = 0; i < compare1Array.length; i++) {
                for (int j = i + 1; j < compare2Array.length; j++) {
                    PotentialArea pot1 = compare1Array[i];
                    PotentialArea pot2 = compare2Array[j];
                    for (AbstractBlockInterface term1 : pot1.getAllGlobalTerminals()) {
                        for (AbstractBlockInterface term2 : pot2.getAllGlobalTerminals()) {
                            if (term1.getStringID().equals(term2.getStringID())
                                    && term1.getSimulationDomain() == term2.getSimulationDomain()) {
                                final PotentialArea merged = pot1.mergePotential(pot2, false);
                                _potentialAreas.remove(pot1);
                                _potentialAreas.remove(pot2);
                                _potentialAreas.add(merged);
                                continueLoop = true;
                            }
                        }
                    }

                    // this part is for connections via the "global" ambient zero temperature
                    // in thermal networks.
                    for (Point point1 : pot1.getAllPointsOnSchematic()) {

                        if (!point1.equals(ThermAmbient.THERMAL_ZERO)) {
                            continue;
                        }
                        for (Point point2 : pot2.getAllPointsOnSchematic()) {
                            if (point2.equals(ThermAmbient.THERMAL_ZERO)) {
                                final PotentialArea merged = pot1.mergePotential(pot2, false);
                                _potentialAreas.remove(pot1);
                                _potentialAreas.remove(pot2);
                                _potentialAreas.add(merged);
                                continueLoop = true;
                            }
                        }
                    }

                }
            }
        }
    }

    private void connectSubCircuitInOutTerminals() {
        boolean continueLoop = true;
        while (continueLoop) {
            continueLoop = false;
            final PotentialArea[] compare1Array = _potentialAreas.toArray(new PotentialArea[0]);
            final PotentialArea[] compare2Array = _potentialAreas.toArray(new PotentialArea[0]);

            for (int i = 0; i < compare1Array.length; i++) {
                for (int j = i + 1; j < compare2Array.length; j++) {
                    final PotentialArea pot1 = compare1Array[i];
                    final PotentialArea pot2 = compare2Array[j];
                    for (SubCircuitTerminable term1 : pot1.getPotentialTerminals()) {
                        for (SubCircuitTerminable term2 : pot2.getPotentialTerminals()) {
                            if (term1.equals(term2) && term1.getBlockTerminal().getCategory() == term2.getBlockTerminal().getCategory()) {
                                final PotentialArea merged = pot1.mergePotential(pot2, false);
                                _potentialAreas.remove(pot1);
                                _potentialAreas.remove(pot2);
                                _potentialAreas.add(merged);
                                continueLoop = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean testMergeConnection() {
        boolean returnValue = false;
        final PotentialArea[] array = _potentialAreas.toArray(new PotentialArea[0]);
        final Set<PotentialArea> alreadyConnected = new LinkedHashSet<PotentialArea>();

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                final PotentialArea pot1 = array[i];
                final PotentialArea pot2 = array[j];

                if (!alreadyConnected.contains(pot2)) {
                    if (pot1.geometricOnSamePotential(pot2)) {
                        final PotentialArea merged = pot1.mergePotential(pot2, true);
                        alreadyConnected.add(pot2);

                        /**
                         * something is weird here! merged is equal to pot1, but
                         * just removing pot2 from the list, and not deleting
                         * pot1 and adding it again makes problems? this has
                         * probably to do with the order of the potentials in
                         * the container. -> fix later!
                         */
                        _potentialAreas.remove(pot1);
                        _potentialAreas.remove(pot2);
                        _potentialAreas.add(merged);
                        returnValue = true;
                    }
                }
            }
        }
        return returnValue;
    }

    private static class GraphEdge {

        private final PotentialArea _pot1;
        private final PotentialArea _pot2;
        public boolean isVisited = false;

        public GraphEdge(final PotentialArea pot1, final PotentialArea pot2) {
            _pot1 = pot1;
            _pot2 = pot2;
        }

        private void traverse(Map<PotentialArea, ArrayList<GraphEdge>> graphMap, LinkedHashSet<GraphEdge> connectedEdges) {
            isVisited = true;
            connectedEdges.add(this);
            for (GraphEdge nextEdge : graphMap.get(_pot1)) {
                if (!nextEdge.isVisited) {
                    nextEdge.traverse(graphMap, connectedEdges);
                }
            }
            for (GraphEdge nextEdge : graphMap.get(_pot2)) {
                if (!nextEdge.isVisited) {
                    nextEdge.traverse(graphMap, connectedEdges);
                }
            }
        }
    }

    void deSingularizeIsolatedPotentials() {

        List<GraphEdge> connectionGraph = new ArrayList<GraphEdge>();
        Map<PotentialArea, ArrayList<GraphEdge>> graphMap = new LinkedHashMap<PotentialArea, ArrayList<GraphEdge>>();

        for (int i = 0; i < _potentialAreas.size(); i++) {
            PotentialArea pot1 = _potentialAreas.get(i);
            for (int j = i + 1; j < _potentialAreas.size(); j++) {
                PotentialArea pot2 = _potentialAreas.get(j);
                if (PotentialArea.hasComponentConnection(pot1, pot2)) {
                    GraphEdge newGraphEdge = new GraphEdge(pot1, pot2);
                    connectionGraph.add(newGraphEdge);
                    if (!graphMap.containsKey(pot1)) {
                        graphMap.put(pot1, new ArrayList<GraphEdge>());
                    }
                    graphMap.get(pot1).add(newGraphEdge);

                    if (!graphMap.containsKey(pot2)) {
                        graphMap.put(pot2, new ArrayList<GraphEdge>());
                    }

                    graphMap.get(pot2).add(newGraphEdge);
                }
            }
        }

        ArrayList<LinkedHashSet<PotentialArea>> potentialAreasGroups = new ArrayList<LinkedHashSet<PotentialArea>>();

        while (!connectionGraph.isEmpty()) {
            LinkedHashSet<GraphEdge> connectedEdges = new LinkedHashSet<GraphEdge>();
            LinkedHashSet<PotentialArea> connectedPotentials = new LinkedHashSet<PotentialArea>();
            GraphEdge firstEdge = connectionGraph.iterator().next();

            firstEdge.traverse(graphMap, connectedEdges);

            for (GraphEdge edge : connectedEdges) {
                connectedPotentials.add(edge._pot1);
                connectedPotentials.add(edge._pot2);
            }
            connectionGraph.removeAll(connectedEdges);
            potentialAreasGroups.add(connectedPotentials);
        }

        _singularityIndices = new int[potentialAreasGroups.size()];
        for (Set<PotentialArea> group : potentialAreasGroups) {
            PotentialArea pot = group.iterator().next();
            _singularityIndices[potentialAreasGroups.indexOf(group)] = _potentialAreas.indexOf(pot);
        }
    }
}
