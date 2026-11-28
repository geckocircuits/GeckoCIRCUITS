package gecko.geckocircuits.circuit;

import gecko.core.ac.AcComponent;
import gecko.core.ac.ParameterDefinition;
import gecko.core.io.CircuitHierarchy;
import gecko.core.io.importer.SpiceValue;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitTerminal;
import gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import gecko.geckocircuits.control.Point;
import gecko.geckocircuits.control.SubCircuitSheet;
import gecko.modelviewcontrol.AbstractUndoGenericModel;
import gecko.modelviewcontrol.GroupableUndoManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Applies edits to the native occurrences of a document-owned definition. */
final class HierarchyEditCoordinator {
    private final SchematicEditor2 editor;

    HierarchyEditCoordinator(SchematicEditor2 editor) { this.editor = editor; }

    Map<String, String> scope(AbstractCircuitSheetComponent member) {
        return scope(member.getParentCircuitSheet(), editor.getHierarchy(), null, null);
    }

    private Map<String, String> scope(CircuitSheet sheet, CircuitHierarchy document,
                                      SubcircuitBlock edited, CircuitHierarchy.Occurrence substitute) {
        if (sheet instanceof SubCircuitSheet child && child._subBlock.getHierarchyCall() != null) {
            var owner = child._subBlock;
            var call = owner == edited ? substitute : owner.getHierarchyCall();
            return document.definition(call.definitionId()).bind(
                    scope(owner.getParentCircuitSheet(), document, edited, substitute), call);
        }
        return editor.rootParameterMap();
    }

    void setSharedLaw(AbstractBlockInterface selected, AcComponent.Law law) {
        SchematicEditor2.requireEditThread();
        editor.inspectHierarchy();
        var peers = law instanceof AcComponent.IdealDiode diode
                ? idealDiodeModelPeers(selected, diode) : peers(selected, AbstractBlockInterface.class);
        for (var peer : peers) {
            var owned = peer.getAcComponentOrLiteral();
            if (owned == null || owned.law().getClass() != law.getClass()) {
                throw new IllegalArgumentException("Invalid AC law for " + peer.getStringID());
            }
            validateLaw(law, scope(peer));
        }
        group(() -> peers.forEach(peer -> peer.setAcLawLocal(law)));
    }

    private List<AbstractBlockInterface> idealDiodeModelPeers(AbstractBlockInterface selected, AcComponent.IdealDiode edited) {
        var original = selected.getAcComponent();
        if (original == null || !(original.law() instanceof AcComponent.IdealDiode diode)
                || !diode.modelName().equals(edited.modelName()) || diode.origin() != edited.origin()) {
            throw new IllegalArgumentException("Retain the local ideal diode model name and origin.");
        }
        String definition = definitionOf(selected);
        var peers = new ArrayList<AbstractBlockInterface>();
        for (var element : editor._circuitSheet.getAllElements()) {
            if (element instanceof AbstractBlockInterface block && block.getAcComponent() != null
                    && block.getAcComponent().law() instanceof AcComponent.IdealDiode law
                    && law.modelName().equals(diode.modelName()) && definitionOf(block).equals(definition)) {
                peers.add(block);
            }
        }
        return peers;
    }

    private static String definitionOf(AbstractBlockInterface component) {
        if (component.getParentCircuitSheet() instanceof SubCircuitSheet sheet
                && sheet._subBlock.getHierarchyCall() != null) {
            return sheet._subBlock.getHierarchyCall().definitionId();
        }
        return "";
    }

    void setParameters(SubcircuitBlock owner, List<ParameterDefinition> defaults,
                       List<ParameterDefinition> overrides) {
        SchematicEditor2.requireEditThread();
        editor.inspectHierarchy();
        var existing = owner.getHierarchyCall();
        var oldDocument = editor.getHierarchy();
        var oldDefinition = oldDocument.definition(existing.definitionId());
        var newDefinition = new CircuitHierarchy.Definition(oldDefinition.id(), oldDefinition.source(),
                oldDefinition.formals(), defaults, oldDefinition.requiredInputs());
        var newDocument = new CircuitHierarchy(oldDocument.definitions().stream()
                .map(definition -> definition.id().equals(newDefinition.id()) ? newDefinition : definition).toList());
        var newCall = new CircuitHierarchy.Occurrence(existing.definitionId(), existing.instance(), overrides);
        var scalarValues = new java.util.LinkedHashMap<AbstractBlockInterface, double[]>();
        for (var element : editor._circuitSheet.getAllElements()) {
            if (element instanceof SubcircuitBlock block && block.getHierarchyCall() != null) {
                scope(block._myCircuitSheet, newDocument, owner, newCall);
            }
            if (element instanceof AbstractBlockInterface block) {
                if (block.getAcComponent() != null) {
                    validateLaw(block.getAcComponent().law(), scope(block.getParentCircuitSheet(), newDocument, owner, newCall));
                }
                if (!block.scalarBindings().isEmpty()) {
                    scalarValues.put(block, block.scopedScalarValues(
                            scope(block.getParentCircuitSheet(), newDocument, owner, newCall)));
                }
            }
        }
        if (newDocument.equals(oldDocument) && newCall.equals(existing)) return;
        group(() -> {
            if (!newDocument.equals(oldDocument)) editor.setHierarchy(newDocument);
            if (!newCall.equals(existing)) owner.setHierarchyCall(newCall);
            scalarValues.forEach(AbstractBlockInterface::setParameter);
        });
    }

    Collection<AbstractCircuitSheetComponent> expandRemoval(Collection<AbstractCircuitSheetComponent> selected) {
        SchematicEditor2.requireEditThread();
        editor.inspectHierarchy();
        var all = new LinkedHashSet<AbstractCircuitSheetComponent>();
        for (var component : selected) {
            if (component instanceof AbstractCircuitTerminal && component.getHierarchyMember() != null
                    || component instanceof SubcircuitBlock block && block.getHierarchyCall() != null) {
                throw new IllegalArgumentException("Retain the ASC definition interface and its occurrences.");
            }
            all.addAll(peers(component, AbstractCircuitSheetComponent.class));
        }
        return all;
    }

    void toggleEnable(Collection<AbstractCircuitSheetComponent> selected, boolean shortCommand) {
        var targets = expandRemoval(selected);
        group(() -> {
            for (var component : targets) {
                if (shortCommand && component instanceof Verbindung) continue;
                Enabled next = switch (component.isCircuitEnabled()) {
                    case ENABLED -> shortCommand ? Enabled.DISABLED_SHORT : Enabled.DISABLED;
                    case DISABLED, DISABLED_SHORT -> Enabled.ENABLED;
                };
                component.setCircuitEnabled(next);
            }
        });
    }

    void removeMembers(Collection<AbstractCircuitSheetComponent> selected) {
        var placements = expandRemoval(selected).stream()
                .map(element -> new Placement(element, element.getParentCircuitSheet())).toList();
        group(() -> {
            var edit = new GeckoUndoableEdit() {
                @Override public void undo() {
                    for (var placement : placements) placement.element().setParentCircuitSheet(placement.sheet());
                }
                @Override public void redo() {
                    for (var placement : placements.reversed()) placement.element().deleteComponent();
                }
                @Override public boolean canUndo() { return true; }
                @Override public boolean canRedo() { return true; }
                @Override public String getPresentationName() { return "Remove ASC members"; }
            };
            AbstractUndoGenericModel.undoManager.addEdit(new GeckoUndoableEditAdapter(edit));
            edit.redo();
            editor.inspectHierarchy();
        });
    }

    void requireSupportedCopies(Collection<? extends AbstractCircuitSheetComponent> selected) {
        SchematicEditor2.requireEditThread();
        for (var element : selected) {
            if (element instanceof Verbindung
                    || element instanceof AbstractBlockInterface block && !(block instanceof SubcircuitBlock)
                        && block.getAcComponentOrLiteral() != null) continue;
            throw new IllegalArgumentException("Copy only supported AC components and wires in this circuit.");
        }
    }

    private record Placement(AbstractCircuitSheetComponent element, CircuitSheet sheet) { }

    void commitAdditions(List<AbstractCircuitSheetComponent> previews) {
        SchematicEditor2.requireEditThread();
        var additions = new ArrayList<Placement>();
        var used = new java.util.HashSet<Long>();
        for (var element : editor._circuitSheet.getAllElements()) used.add(element.getUniqueObjectIdentifier());
        used.add(0L);
        try {
            requireSupportedCopies(previews);
            for (var source : previews) prepareAddition(source, additions, used);
        } catch (RuntimeException | Error error) {
            previews.forEach(AbstractCircuitSheetComponent::deleteComponent);
            throw error;
        }
        group(() -> {
            var edit = new GeckoUndoableEdit() {
                @Override
                public void undo() {
                    for (var placement : additions.reversed()) placement.element().deleteComponent();
                }

                @Override
                public void redo() {
                    for (var placement : additions) placement.element().setParentCircuitSheet(placement.sheet());
                }

                @Override public boolean canUndo() { return true; }
                @Override public boolean canRedo() { return true; }
                @Override public String getPresentationName() { return "Place ASC members"; }
            };
            AbstractUndoGenericModel.undoManager.addEdit(new GeckoUndoableEditAdapter(edit));
            for (var placement : additions) {
                var element = placement.element();
                element.setParentCircuitSheet(placement.sheet());
                if (element instanceof Verbindung wire) wire.initializeHierarchyPoints(List.copyOf(wire.getAllPointCoordinates()));
                else element.absetzenElement();
            }
            editor.inspectHierarchy();
        });
    }

    private void prepareAddition(AbstractCircuitSheetComponent source, List<Placement> additions, java.util.Set<Long> used) {
        var sheet = source.getParentCircuitSheet();
        additions.add(new Placement(source, sheet));
        if (source instanceof Verbindung wire) validatePoints(wire.getAllPointCoordinates());
        if (!(sheet instanceof SubCircuitSheet child) || child._subBlock.getHierarchyCall() == null) return;
        var owners = occurrences(child._subBlock.getHierarchyCall().definitionId());
        var key = new CircuitHierarchy.Member("member:edit:" + java.util.UUID.randomUUID());
        if (source instanceof AbstractBlockInterface block) {
            var law = block.getAcComponentOrLiteral();
            block.setAcComponentWithoutUndo(law);
            for (var owner : owners) {
                validateLaw(law.law(), scope(owner._myCircuitSheet, editor.getHierarchy(), null, null));
                if (owner._myCircuitSheet.getLocalSheetComponents().stream().anyMatch(component -> component != source
                        && component instanceof AbstractBlockInterface peer
                        && peer.getStringID().equalsIgnoreCase(block.getStringID()))) {
                    throw new IllegalArgumentException("Use a unique component name in the definition.");
                }
            }
        }
        source.setHierarchyMember(key);
        for (var owner : owners) {
            if (owner == child._subBlock) continue;
            AbstractCircuitSheetComponent peer;
            if (source instanceof Verbindung wire) {
                peer = wire.copyShape();
            } else {
                var block = (AbstractBlockInterface) source;
                var copy = (AbstractBlockInterface) block.copyFabric(0);
                copy.getIDStringDialog().setNameUnChecked(block.getStringID());
                copy.setAcComponentWithoutUndo(block.getAcComponentOrLiteral());
                peer = copy;
            }
            long previousOwner = peer.getUniqueObjectIdentifier();
            do {
                peer.getIdentifier().createNewIdentifier(0);
                peer.getIdentifier().createNewIdentifier();
            } while (!used.add(peer.getUniqueObjectIdentifier()));
            if (peer instanceof gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitSource sourcePeer) {
                sourcePeer.rebindPwlOwner(previousOwner);
            }
            peer.setHierarchyMember(key);
            additions.add(new Placement(peer, owner._myCircuitSheet));
        }
    }

    void editWireProperties(Verbindung wire, String label, Enabled enabled) {
        SchematicEditor2.requireEditThread();
        editor.inspectHierarchy();
        if (label.toLowerCase(java.util.Locale.ROOT).startsWith("$g_")) {
            throw new IllegalArgumentException("Use a sheet-local label or ground.");
        }
        var copies = peers(wire, Verbindung.class);
        group(() -> {
            for (var peer : copies) {
                String old = peer.getLabel();
                peer.setCircuitEnabled(enabled);
                peer.getLabelObject().setLabelFromUserDialog(label);
                peer.getParentCircuitSheet().updateRenamedLabel(old, label, peer.getSimulationDomain());
            }
        });
    }

    void placeSharedMember(AbstractBlockInterface selected) {
        SchematicEditor2.requireEditThread();
        Point previous = selected.getPositionVorVerschieben();
        Point next = selected.getSheetPosition();
        var previousDirection = selected.getOrientationBeforeMove();
        var nextDirection = selected.getComponentDirection();
        if (previous.equals(next) && previousDirection == nextDirection) {
            selected.absetzenElement();
            return;
        }
        var peers = peers(selected, AbstractBlockInterface.class);
        for (var peer : peers) {
            if (peer != selected && (!previous.equals(peer.getSheetPosition()) || previousDirection != peer.getComponentDirection())) {
                selected.deselectViaESCAPE();
                throw new IllegalArgumentException("The shared components have different positions.");
            }
        }
        selected.deselectViaESCAPE();
        group(() -> {
            for (var peer : peers) {
                peer.setOrientationBeforeMove(previousDirection);
                peer.setComponentDirection(nextDirection);
                peer.setComponentDirectionUndo();
                peer.setPositionVorVerschieben(previous);
                peer.setSheetPositionWithoutUndo(next);
                peer.setPositionWithUndo();
            }
        });
    }

    void moveWire(Verbindung selected, List<Point> from, List<Point> to) {
        SchematicEditor2.requireEditThread();
        var peers = peers(selected, Verbindung.class);
        try {
            validatePoints(to);
            for (var peer : peers) {
                if (peer != selected && !from.equals(peer.getAllPointCoordinates())) {
                    throw new IllegalArgumentException("The shared wires have different endpoints.");
                }
            }
        } catch (IllegalArgumentException error) {
            selected.initializeHierarchyPoints(from);
            throw error;
        }
        selected.initializeHierarchyPoints(from);
        group(() -> peers.forEach(peer -> peer.setHierarchyPointsWithUndo(from, to)));
    }

    private static void validatePoints(List<Point> points) {
        if (points.size() < 2 || points.getFirst().equals(points.getLast())) {
            throw new IllegalArgumentException("Use distinct wire endpoints.");
        }
    }

    private List<SubcircuitBlock> occurrences(String definitionId) {
        return editor._circuitSheet.getAllElements().stream().filter(SubcircuitBlock.class::isInstance)
                .map(SubcircuitBlock.class::cast).filter(block -> block.getHierarchyCall() != null
                        && block.getHierarchyCall().definitionId().equals(definitionId)).toList();
    }

    private <T extends AbstractCircuitSheetComponent> List<T> peers(AbstractCircuitSheetComponent selected, Class<T> type) {
        if (!(selected.getParentCircuitSheet() instanceof SubCircuitSheet sheet)
                || sheet._subBlock.getHierarchyCall() == null) return List.of(type.cast(selected));
        var key = selected.getHierarchyMember();
        if (key == null) throw new IllegalArgumentException("The ASC member has no definition key.");
        var result = new ArrayList<T>();
        for (var owner : occurrences(sheet._subBlock.getHierarchyCall().definitionId())) {
            var matches = owner._myCircuitSheet.getLocalSheetComponents().stream()
                    .filter(member -> key.equals(member.getHierarchyMember())).toList();
            if (matches.size() != 1 || !type.isInstance(matches.getFirst())) {
                throw new IllegalArgumentException("Missing shared ASC member " + key.key());
            }
            result.add(type.cast(matches.getFirst()));
        }
        return result;
    }

    private void group(Runnable apply) {
        var manager = (GroupableUndoManager) AbstractUndoGenericModel.undoManager;
        boolean dirty = editor.getZustandGeaendert();
        try {
            manager.atomicEdit(apply);
        } catch (GroupableUndoManager.RollbackFailure failure) {
            editor.retainFailedHierarchyEdit();
            throw failure;
        } catch (RuntimeException | Error failure) {
            if (!dirty) editor.resetModelModified();
            throw failure;
        }
        editor.setDirtyFlag();
        editor._visibleCircuitSheet.repaint();
    }

    private static void validateLaw(AcComponent.Law law, Map<String, String> scope) {
        switch (law) {
            case AcComponent.Passive passive -> requirePositive(SpiceValue.evaluate(passive.expression(), scope));
            case AcComponent.Inductance inductor -> inductor.evaluate(scope);
            case AcComponent.Voltage voltage -> validateSource(voltage.dc(), voltage.magnitude(), voltage.phaseDegrees(), scope);
            case AcComponent.Current current -> validateSource(current.dc(), current.magnitude(), current.phaseDegrees(), scope);
            case AcComponent.IdealDiode diode -> diode.evaluate(scope);
            case AcComponent.ExponentialDiode diode -> diode.validate(scope);
            case AcComponent.Mosfet transistor -> transistor.evaluate(scope);
            case AcComponent.Behavioural behavioural -> behavioural.compile(scope);
            case AcComponent.Bipolar transistor -> transistor.validate(scope);
            case AcComponent.Transconductance lawG -> SpiceValue.evaluate(lawG.expression(), scope);
            case AcComponent.Controlled controlled -> {
                if (controlled.laplace()) SpiceValue.transfer(controlled.expression(), scope);
                else SpiceValue.evaluate(controlled.expression(), scope);
            }
        }
    }

    private static void validateSource(String dc, String magnitude, String phase, Map<String, String> scope) {
        SpiceValue.evaluate(dc, scope);
        SpiceValue.evaluate(phase, scope);
        if (SpiceValue.evaluate(magnitude, scope) < 0) throw new IllegalArgumentException("Require a nonnegative AC magnitude.");
    }

    private static void requirePositive(double value) {
        if (value <= 0) throw new IllegalArgumentException("Require a positive component value.");
    }
}
