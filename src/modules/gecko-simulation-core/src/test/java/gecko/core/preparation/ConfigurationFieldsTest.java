package gecko.core.preparation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFieldsTest {
    @Test
    void configuredFieldMetadataExposesOnlySupportedWritableFieldsAndOwnsItsLists() {
        var model = CircuitPreparationTest.resistorCircuit();
        model.addCircuitComponent(CircuitPreparationTest.component(7, "switch", new double[]{19999, 1, 19999}, "1", "0"));
        model.addControlComponent(CircuitPreparationTest.component(4, "signal", new double[]{400, 2, 1, 0, 0, 0.5}, "", "wave"));
        var preparation = new CircuitPreparation();
        var fields = preparation.configurationFields(preparation.capture(model));
        assertTrue(fields.stream().anyMatch(field -> field.domain().equals("LK") && field.component().equals("R.1")
                && field.name().equals("resistance") && field.indexedName().equals("param0")));
        assertEquals(List.of("onResistance", "offResistance"), fields.stream().filter(field -> field.component().equals("switch"))
                .map(ConfigurationField::name).toList());
        var phase = fields.stream().filter(field -> field.component().equals("signal") && field.name().equals("phaseDegrees")).findFirst().orElseThrow();
        assertEquals("param3", phase.indexedName());
        assertTrue(phase.aliases().contains("phase"));
        assertThrows(UnsupportedOperationException.class, () -> fields.add(phase));
        assertThrows(UnsupportedOperationException.class, () -> phase.aliases().add("runtime"));
    }
}
