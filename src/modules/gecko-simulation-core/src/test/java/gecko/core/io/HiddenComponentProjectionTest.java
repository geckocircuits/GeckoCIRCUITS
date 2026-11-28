package gecko.core.io;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenComponentProjectionTest {
    @Test
    void hiddenTransformerRolesShareParentGroupsWithoutUsingHistoricalIdentifiersAsIdentity() {
        var model = new CircuitModel();
        var transformer = component(23, "T", 9007199254740993L,
                new String[]{"primary", "0"}, new String[]{"secondary", "0"});
        var aliasCollision = component(1, "T prim", 9007199254740994L,
                new String[]{"unrelated"}, new String[]{"0"});
        model.addCircuitComponent(transformer);
        model.addCircuitComponent(aliasCollision);
        var original = CircuitTopology.resolve(model);
        var parent = original.identity(transformer);
        var primary = parent.child("primaryVoltageSource");
        var secondary = parent.child("secondaryVoltageSource");
        var projected = original.projectHiddenComponents(List.of(
                new CircuitTopology.HiddenComponent(parent, "primaryVoltageSource",
                        new CircuitTopology.HiddenPrimitive(4, "T prim", 9007199254740994L,
                                List.of(399.0, -2.0)),
                        new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 0),
                        new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 1),
                        "secondaryVoltageSource"),
                new CircuitTopology.HiddenComponent(parent, "secondaryVoltageSource",
                        new CircuitTopology.HiddenPrimitive(4, "T sec", 9007199254740995L,
                                List.of(398.0, -2.0)),
                        new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.OUTPUT, 0),
                        new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.OUTPUT, 1),
                        "primaryVoltageSource")));
        assertEquals(9007199254740993L, primary.parent().uniqueIdentifier());
        assertNotEquals(original.identity(aliasCollision), primary);
        assertEquals(List.of(primary, secondary, original.identity(aliasCollision)), projected.activeComponents());
        assertEquals(secondary, projected.controlTarget(primary));
        assertEquals(primary, projected.controlTarget(secondary));
        assertEquals(original.group(transformer, CircuitTopology.Side.INPUT, 0).id(),
                projected.terminalGroups().get(new CircuitTopology.TerminalIdentity(primary, CircuitTopology.Side.INPUT, 0)));
        assertEquals(original.group(transformer, CircuitTopology.Side.OUTPUT, 0).id(),
                projected.terminalGroups().get(new CircuitTopology.TerminalIdentity(secondary, CircuitTopology.Side.INPUT, 0)));
        assertEquals(original.group(transformer, CircuitTopology.Side.INPUT, 1).id(),
                projected.terminalGroups().get(new CircuitTopology.TerminalIdentity(primary, CircuitTopology.Side.OUTPUT, 0)));
        assertEquals(original.group(transformer, CircuitTopology.Side.OUTPUT, 1).id(),
                projected.terminalGroups().get(new CircuitTopology.TerminalIdentity(secondary, CircuitTopology.Side.OUTPUT, 0)));
        assertTrue(projected.groups().values().stream().flatMap(group -> group.terminals().stream())
                .anyMatch(terminal -> terminal.component().equals(primary)));
        assertEquals(399.0, projected.hiddenComponents().getFirst().primitive().numericParameters().getFirst());
        assertEquals(398.0, projected.hiddenComponents().getLast().primitive().numericParameters().getFirst());
        assertEquals(2, model.getCircuitComponents().size());
        assertEquals(23, model.getCircuitComponents().getFirst().getType());
        assertTrue(original.activeComponents().contains(parent));
        assertEquals(0, original.hiddenComponents().size());
        projected.terminalGroups().forEach((terminal, group) ->
                assertTrue(projected.groups().get(group).terminals().contains(terminal)));
        projected.groups().values().forEach(group -> group.terminals().forEach(terminal ->
                assertEquals(group.id(), projected.terminalGroups().get(terminal))));
    }

    @Test
    void unknownControlRoleCannotResolveThroughAHistoricalName() {
        var graph = graph();
        var parent = graph.activeComponents().getFirst();
        var child = child(parent, "source", "historicalSource",
                new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 0));
        var error = assertThrows(IllegalArgumentException.class,
                () -> graph.projectHiddenComponents(List.of(child)));
        assertTrue(error.getMessage().contains("Unknown hidden control target role"));
        assertEquals(0, graph.hiddenComponents().size());
    }

    @Test
    void duplicateCanonicalRolesCannotBeDistinguishedByHistoricalAliases() {
        var graph = graph();
        var parent = graph.activeComponents().getFirst();
        var first = child(parent, "source", "source",
                new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 0));
        var second = new CircuitTopology.HiddenComponent(parent, "source",
                new CircuitTopology.HiddenPrimitive(4, "differentAlias", 777, List.of(398.0)),
                first.input(), first.output(), "source");
        var error = assertThrows(IllegalArgumentException.class,
                () -> graph.projectHiddenComponents(List.of(first, second)));
        assertTrue(error.getMessage().contains("Duplicate hidden component role"));
    }

    @Test
    void aHiddenPortCannotUseAnotherComponentsExistingTerminal() {
        var graph = graph();
        var parent = graph.activeComponents().getFirst();
        var other = graph.activeComponents().getLast();
        var child = child(parent, "source", "source",
                new CircuitTopology.TerminalIdentity(other, CircuitTopology.Side.INPUT, 0));
        var error = assertThrows(IllegalArgumentException.class,
                () -> graph.projectHiddenComponents(List.of(child)));
        assertTrue(error.getMessage().contains("existing parent terminal"));
    }

    @Test
    void primitiveParametersAndPublishedProjectionCollectionsOwnTheirInput() {
        var graph = graph();
        var parent = graph.activeComponents().getFirst();
        var values = new java.util.ArrayList<>(List.of(399.0, -2.0));
        var primitive = new CircuitTopology.HiddenPrimitive(4, "historicalSource", 42, values);
        var child = new CircuitTopology.HiddenComponent(parent, "source", primitive,
                new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 0),
                new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 1), "source");
        values.set(0, 401.0);
        var projected = graph.projectHiddenComponents(List.of(child));
        assertEquals(List.of(399.0, -2.0), projected.hiddenComponents().getFirst().primitive().numericParameters());
        assertThrows(UnsupportedOperationException.class, () -> primitive.numericParameters().set(0, 402.0));
        assertThrows(UnsupportedOperationException.class, () -> projected.hiddenComponents().clear());
        assertThrows(UnsupportedOperationException.class, () -> projected.activeComponents().clear());
        assertThrows(UnsupportedOperationException.class, () -> projected.terminalGroups().clear());
        assertThrows(UnsupportedOperationException.class, () -> projected.groups().values().iterator().next().terminals().clear());
    }

    private static CircuitTopology graph() {
        var model = new CircuitModel();
        model.addCircuitComponent(component(23, "T", 123, new String[]{"primary", "0"}, new String[]{"secondary", "0"}));
        model.addCircuitComponent(component(1, "R", 124, new String[]{"other"}, new String[]{"0"}));
        return CircuitTopology.resolve(model);
    }

    private static CircuitTopology.HiddenComponent child(CircuitTopology.ComponentIdentity parent,
            String role, String target, CircuitTopology.TerminalIdentity input) {
        return new CircuitTopology.HiddenComponent(parent, role,
                new CircuitTopology.HiddenPrimitive(4, "historicalSource", 42, List.of(399.0)), input,
                new CircuitTopology.TerminalIdentity(parent, CircuitTopology.Side.INPUT, 1), target);
    }

    private static CircuitModel.ComponentData component(int type, String name, long identifier,
            String[] inputs, String[] outputs) {
        var component = new CircuitModel.ComponentData(type, name);
        component.setSavedBlock(SavedCircuitBlock.parse("ElementLK",
                new String[]{"uniqueObjectIdentifier " + identifier, "enabledShorted 1"}));
        component.setTerminalXLabels(inputs);
        component.setTerminalYLabels(outputs);
        return component;
    }
}
