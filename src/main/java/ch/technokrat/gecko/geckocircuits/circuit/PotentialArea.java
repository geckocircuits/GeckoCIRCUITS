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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.IdealTransformer;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ReluctanceInductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TerminalCircuit;
import ch.technokrat.gecko.geckocircuits.control.Point;
import java.util.*;

/**
 * class represents a connected (potential) region.
 *
 * @author andreas
 */
public final class PotentialArea {

    private final Set<Point> _pointsSchematic = new LinkedHashSet<Point>();
    private final Set<Point> _nodeTerminals = new LinkedHashSet<Point>();
    private String _potentialLabel = "";  // der zu diesem Potential gehoerige Label
    private final Set<Verbindung> _potentialConnections = new LinkedHashSet<Verbindung>();
    private final Set<ElementNodes> _elementNodeTerminals = new LinkedHashSet<ElementNodes>();
    public final Set<SubCircuitTerminable> _potentialTerminals = new LinkedHashSet<SubCircuitTerminable>();
    private final Set<AbstractBlockInterface> _globalTerminals = new LinkedHashSet<AbstractBlockInterface>();
    boolean _isShortConnector;
    private LabelPriority _highesPriority = LabelPriority.LOW;
    public CircuitSheet _potentialCircuitSheet;
    public ConnectorType _potentialTyp = ConnectorType.NONE;
    /**
     * these two fields are used for speeding up the potential mergin.
     */
    Point _upperLeftCorner;
    Point _lowerRightCorner;

    private Collection<? extends Verbindung> getConnections() {
        return Collections.unmodifiableCollection(_potentialConnections);
    }

    public Set<Verbindung> getAllConnections() {
        return Collections.unmodifiableSet(_potentialConnections);
    }

    public Collection<Point> getAllNodes() {
        return Collections.unmodifiableCollection(_nodeTerminals);
    }

    public AbstractBlockInterface[] getAllGlobalTerminals() {
        return _globalTerminals.toArray(new AbstractBlockInterface[_globalTerminals.size()]);
    }

    private PotentialArea() {
        // use static fabric methods for object construction!
    }

    public static PotentialArea fabricFromConnector(final Verbindung connector) {
        final PotentialArea returnValue = new PotentialArea();
        returnValue._potentialConnections.add(connector);
        returnValue._pointsSchematic.addAll(connector.getAllPointCoordinates());

        for (TerminalInterface term : connector.getAllTerminals()) {
            returnValue._nodeTerminals.add(term.getPosition());
        }
        returnValue._isShortConnector = connector instanceof VerbindungShortConnector;
        returnValue._potentialLabel = connector.getLabel();
        returnValue._highesPriority = connector.getLabelPriority();

        final int minX = Math.min(connector.getStartPoint().x, connector.getEndPoint().x);
        final int maxX = Math.max(connector.getStartPoint().x, connector.getEndPoint().x);
        final int minY = Math.min(connector.getStartPoint().y, connector.getEndPoint().y);
        final int maxY = Math.max(connector.getStartPoint().y, connector.getEndPoint().y);
        returnValue._upperLeftCorner = new Point(minX, minY);
        returnValue._lowerRightCorner = new Point(maxX, maxY);

        returnValue._potentialCircuitSheet = connector.getParentCircuitSheet();
        returnValue._potentialTyp = connector.getSimulationDomain();
        assert returnValue._potentialTyp != ConnectorType.LK_AND_RELUCTANCE : "mixed type not possible, here!";
        return returnValue;
    }

    public static PotentialArea fabricElementTerminal(final AbstractBlockInterface element,
            final TerminalInterface terminal) {

        final PotentialArea returnValue = new PotentialArea();
        returnValue._upperLeftCorner = terminal.getPosition();
        returnValue._lowerRightCorner = terminal.getPosition();
        
        /*if(!(terminal instanceof TerminalHiddenSubcircuit)) {
            returnValue._pointsSchematic.add(terminal.getPosition());
            returnValue._nodeTerminals.add(terminal.getPosition());
        }*/               
                
        returnValue._pointsSchematic.add(terminal.getPosition());                        
        returnValue._nodeTerminals.add(terminal.getPosition());
        
        returnValue._potentialLabel = terminal.getLabelObject().getLabelString();
        final ElementNodes ens = new ElementNodes(element, terminal);
        returnValue._elementNodeTerminals.add(ens);

        if (element instanceof SubCircuitTerminable) {
            returnValue._potentialTerminals.add((SubCircuitTerminable) element);
        }
        if (element instanceof GlobalTerminable) {
            returnValue._globalTerminals.add((AbstractBlockInterface) element);
        }

        returnValue._highesPriority = terminal.getLabelObject().getLabelPriority();
        returnValue._potentialCircuitSheet = terminal.getCircuitSheet();
        returnValue._potentialTyp = terminal.getCategory();
        try {
            assert returnValue._potentialTyp != ConnectorType.LK_AND_RELUCTANCE : "mixed type not possible, here!";
        } catch (AssertionError err) {
            err.printStackTrace();
        }

        return returnValue;
    }

    public String getLabel() {
        return _potentialLabel;
    }

    // is used in SchematischeEingabe2 to get all nodes of elements for showing the connections --> 
    public List<Point> getAllElementKnotenXY(final List<? extends AbstractBlockInterface> elements,
            final CircuitSheet circuitSheet) {
        final List<Point> returnValue = new ArrayList<Point>();
        for (AbstractBlockInterface elem : elements) {
            if (elem.getParentCircuitSheet() == circuitSheet) {
                for (TerminalInterface term : elem.getAllTerminals()) {
                    if (isTerminalOnPotential(term)) {
                        returnValue.add(term.getPosition());
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * is there a geometric connection between potential 1 and potential 2?
     *
     * @param pot2
     * @return
     */
    public boolean geometricOnSamePotential(final PotentialArea pot2) {


        if (_potentialTyp != ConnectorType.NONE && pot2._potentialTyp != ConnectorType.NONE) {
            if (pot2._potentialTyp != this._potentialTyp) {
                return false;
            }
        }



        if (pot2._potentialCircuitSheet != _potentialCircuitSheet) {
            return false;
        }


        if (pot2._upperLeftCorner.x > this._lowerRightCorner.x) {
            return false;
        }

        if (this._upperLeftCorner.x > pot2._lowerRightCorner.x) {
            return false;
        }

        if (pot2._upperLeftCorner.y > this._lowerRightCorner.y) {
            return false;
        }
        if (this._upperLeftCorner.y > pot2._lowerRightCorner.y) {
            return false;
        }



        for (Point point : _pointsSchematic) {
            if (pot2._nodeTerminals.contains(point)) {
                doVerbindungChangeType(this, pot2);
                return true;
            }
        }

        for (Point point2 : pot2._pointsSchematic) {
            if (_nodeTerminals.contains(point2)) {
                doVerbindungChangeType(this, pot2);
                return true;
            }
        }



        return false;
    }
    static long counter = 0;

    // wenn das aktuelle Potential gleich pot2 ist, werden die beiden zu einem Potential verschmolzen
    // das neue Potentailgebiet (das nun beide enthaelt), wird zurueckgegeben -->
    public PotentialArea mergePotential(final PotentialArea pot2, final boolean doLabelConnectRename) {

        final PotentialArea pot1 = this;

        if (pot2._isShortConnector) {
            pot1._isShortConnector = true;
        }
        if (pot1._isShortConnector) {
            pot2._isShortConnector = true;
        }

        doVerbindungChangeType(pot1, pot2);

        final String label1 = pot1.getLabel().trim();
        final String label2 = pot2.getLabel().trim();

        _pointsSchematic.addAll(pot2._pointsSchematic);


        _nodeTerminals.addAll(pot2._nodeTerminals);
        _potentialConnections.addAll(pot2.getConnections());

        this._upperLeftCorner = new Point(Math.min(pot1._upperLeftCorner.x, pot2._upperLeftCorner.x),
                Math.min(pot1._upperLeftCorner.y, pot2._upperLeftCorner.y));

        this._lowerRightCorner = new Point(Math.max(pot1._lowerRightCorner.x, pot2._lowerRightCorner.x),
                Math.max(pot1._lowerRightCorner.y, pot2._lowerRightCorner.y));


        if (pot2._highesPriority.isBiggerThan(pot1._highesPriority)) {
            _potentialLabel = label2;
        } else {
            _potentialLabel = label1;
        }

        if (pot1._potentialCircuitSheet == pot2._potentialCircuitSheet) {
            _potentialCircuitSheet = pot1._potentialCircuitSheet;
        } else {
            _potentialCircuitSheet = null;
        }

        _highesPriority = LabelPriority.getHighesPriority(pot1._highesPriority, pot2._highesPriority);

        _elementNodeTerminals.addAll(pot2._elementNodeTerminals);
        _potentialTerminals.addAll(pot2._potentialTerminals);
        _globalTerminals.addAll(pot2._globalTerminals);

        if (doLabelConnectRename && !_isShortConnector) {
            for (ElementNodes tmp : _elementNodeTerminals) {
                for (TerminalInterface term : tmp._connectedTerminals) {
                    term.getLabelObject().setLabel(_potentialLabel);
                }
            }

            for (Verbindung verb : _potentialConnections) {
                verb.setLabel(_potentialLabel);
                verb.setLabelPriority(_highesPriority);
            }
        }
        //assert this._potentialTyp != ConnectorType.NONE && this._potentialTyp != ConnectorType.LK_AND_RELUCTANCE;        
        return this;
    }

    /*
     * returns true if some of the terminals in the potential areas have a connection via a component
     */
    public static boolean hasComponentConnection(final PotentialArea potArea1, final PotentialArea potArea2) {
        for (ElementNodes elNodes : potArea1._elementNodeTerminals) {
            for (ElementNodes elNodes2 : potArea2._elementNodeTerminals) {
                if (elNodes._element == elNodes2._element) {
                    AbstractBlockInterface element = elNodes._element;

                    if (element instanceof SubcircuitBlock) {
                        continue;
                    }

                    if (element instanceof ReluctanceInductor
                            || element instanceof IdealTransformer) {
                        List<TerminalInterface> terminals1 = elNodes._connectedTerminals;
                        List<TerminalInterface> terminals2 = elNodes2._connectedTerminals;


                        if (terminals1.contains(element.XIN.get(0)) && terminals2.contains(element.XIN.get(1))) {
                            return true;
                        }
                        if (terminals1.contains(element.YOUT.get(0)) && terminals2.contains(element.YOUT.get(1))) {
                            return true;
                        }

                        if (terminals1.contains(element.XIN.get(1)) && terminals2.contains(element.XIN.get(0))) {
                            return true;
                        }
                        if (terminals1.contains(element.YOUT.get(1)) && terminals2.contains(element.YOUT.get(0))) {
                            return true;
                        }

                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean containsConnector(final Verbindung verb) {
        return _potentialConnections.contains(verb);
    }

    // ein Label ist gerade an einer bestimmten Stelle gesetzt worden -->
    // dieses Label wird allen (auch den Element-Knoten) aufgezwungen
    public void updateLabel(final String label, final Collection<AbstractBlockInterface> element) {
        // Labels auf Verbindungen werden gesetzt:

        // Labels auf Element-Knoten werden gesetzt (aber nur, wenn dort schon ein String eingetragen wurde, dh. '... !="" ...')
        for (Verbindung verb : _potentialConnections) {
            if (!verb.getLabel().trim().isEmpty()) {
                verb.setLabel(label);
            }
        }


        for (ElementNodes elInfo : _elementNodeTerminals) {
            for (TerminalInterface term : elInfo._connectedTerminals) {
                if (!_isShortConnector) {
                    term.getLabelObject().setLabel(label);
                }
            }
        }

        this._potentialLabel = label;
    }

    // verschiedene Potentiale sind (eventuell) gerade miteinander verbunden worden -->
    // die Labels werden nun aktualisiert, ein dominantes Label ist nicht vorgegeben
    public void aktualisiereLabel(final List<Verbindung> connector, final Collection<AbstractBlockInterface> element) {
        // zuerst schaun wir, ob es Labels auf Element-Knoten des Potentials gibt:

        for (ElementNodes elInfo : _elementNodeTerminals) {
            for (TerminalInterface term : elInfo._connectedTerminals) {
                final String lab = term.getLabelObject().getLabelString();
                if (!lab.isEmpty()) {
                    this._potentialLabel = lab;
                }
            }
        }


        adaptLabelDueToConnector();

        // nun setzen wir das soeben ermittelte Potential-Label ueberall wo noetig:        
        this.updateLabel(_potentialLabel, element);
    }

    private void adaptLabelDueToConnector() {
        final List<Verbindung> allConnections = new ArrayList<Verbindung>();
        allConnections.addAll(_potentialConnections);

        if (_potentialLabel.isEmpty()) {
            // iterate backward:
            for (int i1 = allConnections.size() - 1; i1 >= 0; i1--) {
                final Verbindung verb = allConnections.get(i1);
                if (!verb.getLabel().isEmpty() && !(verb instanceof VerbindungShortConnector)) {
                    _potentialLabel = verb.getLabel();
                    break;
                }
            }

        }
    }

    public boolean isTerminalOnPotential(final TerminalInterface terminal) {
        if (_pointsSchematic.contains(terminal.getPosition())) {
            for (ElementNodes elNode : _elementNodeTerminals) {
                for (TerminalInterface term : elNode._connectedTerminals) {
                    if (term.equals(terminal)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public boolean isPointOnPotential(final Point testPoint) {

        return _pointsSchematic.contains(testPoint);
    }

    void clearLabelPriorities() {
        for (ElementNodes elNode : _elementNodeTerminals) {
            for (TerminalInterface term : elNode._element.getAllTerminals()) {
                term.getLabelObject().clearPriority();
            }
        }

        for (Verbindung verb : _potentialConnections) {
            verb.getLabelObject().clearPriority();
        }

        _highesPriority = LabelPriority.LOW;

    }

    public void checkForDoubleTerminalLabels() {

        for (ElementNodes nodes : _elementNodeTerminals) {
            for (TerminalInterface term1 : nodes._connectedTerminals) {
                if (term1 instanceof AbstractTerminal) {
                    ((AbstractTerminal) term1).setHasDoubleLabel(false);
                }
            }
        }

        for (ElementNodes nodes : _elementNodeTerminals) {
            for (ElementNodes nodes2 : _elementNodeTerminals) {
                if (nodes._element.equals(nodes2._element)) {
                    continue;
                } else {
                    for (TerminalInterface term1 : nodes._connectedTerminals) {
                        if (!(term1 instanceof AbstractTerminal)) {
                            continue;
                        }
                        for (TerminalInterface term2 : nodes2._connectedTerminals) {
                            if (!(term2 instanceof AbstractTerminal)) {
                                continue;
                            }
                            final boolean hasDoubleLabel = term1.getPosition().equals(term2.getPosition());
                            if (hasDoubleLabel) {
                                ((AbstractTerminal) term1).setHasDoubleLabel(hasDoubleLabel);
                                ((AbstractTerminal) term2).setHasDoubleLabel(hasDoubleLabel);
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * when no component is connected to this potential
     *
     * @return
     */
    public boolean isEmptyPotential() {

        if (_elementNodeTerminals.size() == 2) { // special case: when a terminal is defined, but nothing else connected
            // to it, then the potential can be considered as empty.
            Object compare = null;
            for (ElementNodes nodes : _elementNodeTerminals) {
                if (nodes._element.equals(compare)) {
                    return true;
                }
                compare = nodes._element;
            }

        } else {
//            for (ElementNodes nodes : _elementNodeTerminals) {
//                boolean allTerminalCircuits = true;
//                if( ! (nodes._element instanceof TerminalCircuit)) {
//                    allTerminalCircuits = false;
//                    break;
//                }
//                if(allTerminalCircuits) {
//                    System.out.println("removing empty potential! " + ((TerminalCircuit) nodes._element).getStringID() + " " + ((TerminalCircuit) nodes._element).getParentSheetIdentifier());
//                    return true;
//                }
//            }
        }

        return _elementNodeTerminals.isEmpty();
    }

    public Set<TerminalInterface> getAllTerminals() {
        Set<TerminalInterface> returnValue = new LinkedHashSet<TerminalInterface>();
        for (ElementNodes nodes : _elementNodeTerminals) {
            for (TerminalInterface term : nodes._connectedTerminals) {
                returnValue.add(term);
            }

        }
        return returnValue;
    }

    Iterable<Point> getAllPointsOnSchematic() {
        return Collections.unmodifiableSet(_pointsSchematic);
    }
    
    SubCircuitTerminable[] _memoryOptimization;

    SubCircuitTerminable[] getPotentialTerminals() {
        if (_memoryOptimization == null || _memoryOptimization.length != _potentialTerminals.size()) {
            _memoryOptimization = _potentialTerminals.toArray(new SubCircuitTerminable[_potentialTerminals.size()]);
        }
        assert validateMemoryOptimization();
        return _memoryOptimization;
    }
    
    private boolean validateMemoryOptimization() {
        if(_memoryOptimization.length != _potentialTerminals.size()) {
            return false;
        }
        for (int i = 0; i < _memoryOptimization.length; i++) {
            if(!_potentialTerminals.contains(_memoryOptimization[i])) {
                assert false : _potentialTerminals.contains(_memoryOptimization[i]);
                return false;
            }
        }
        return true;
    }

    private void doVerbindungChangeType(PotentialArea pot1, PotentialArea pot2) {
        if (pot1._potentialTyp == pot2._potentialTyp) {
            return;
        }
        if (pot1._potentialTyp == ConnectorType.NONE) {
            pot1.setNewPotentialTyp(pot2._potentialTyp);
        }

        if (pot2._potentialTyp == ConnectorType.NONE) {
            pot2.setNewPotentialTyp(pot1._potentialTyp);
        }
    }

    private void setNewPotentialTyp(ConnectorType newPotentialTyp) {
        _potentialTyp = newPotentialTyp;
        for (Verbindung verb : _potentialConnections) {
            verb.changeConnectorType(newPotentialTyp);
        }
    }

    public List<TerminalVerbindung> getTermConnectors() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<AbstractTerminal> getTermComponents() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void addTermConnector(TerminalVerbindung terminalVerbindung) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    

    private static class ElementNodes {

        /**
         * here, all the nodes are listed, which have a connection to this
         * potential area. Since a single component can have several terminals,
         * the following list only contains the node indices, which are really
         * connected to this potential area.
         */
        final AbstractBlockInterface _element;
        final List<TerminalInterface> _connectedTerminals = new ArrayList<TerminalInterface>();

        public ElementNodes(final AbstractBlockInterface element, final TerminalInterface connectedTerminal) {
            _element = element;
            _connectedTerminals.add(connectedTerminal);
        }

        @Override
        public String toString() {
            return _element.getStringID();
        }
    }

    @Override
    public String toString() {
        final StringBuffer returnValue = new StringBuffer("------------------\n");
        for (Point point : _pointsSchematic) {
            returnValue.append("point: ");
            returnValue.append(point.toString());
            returnValue.append('\n');
        }

        return returnValue.toString();
    }

    public static boolean testForLabelConnection(final PotentialArea pot1, final PotentialArea pot2) {
        if (pot2._potentialTyp != pot1._potentialTyp) {
            return false;
        }

        final String label1 = pot1.getLabel();
        final String label2 = pot2.getLabel();
        if (label1.isEmpty() || label2.isEmpty()) {
            return false;
        }
        if (pot1._potentialCircuitSheet == null || pot2._potentialCircuitSheet == null) {
            return false;
        }
        if (label1.equals(label2) && pot1._potentialCircuitSheet == pot2._potentialCircuitSheet) {
            return true;
        }
        return false;
    }
}
