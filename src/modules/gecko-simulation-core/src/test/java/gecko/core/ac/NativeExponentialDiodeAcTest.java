package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeExponentialDiodeAcTest {
    private static final String DECK = """
            * Active diffusion-charge witness at 27 °C
            I1 0 a DC 100u AC 1m
            D1 a 0 DOTA
            .model DOTA D(tt=100n Rs=10m N=100m)
            .ac dec 30 10k 10meg
            .end
            """;

    @Test
    void exponentialModelRequiresOptInAndCannotFallBackToATransientDiode() {
        assertTrue(new SpiceImporter().convert(DECK, "diode.cir").getImportIssues().stream()
                .anyMatch(issue -> issue.component().equals("D1")));
        var model = new SpiceImporter(true).convert(DECK, "diode.cir");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        model.setAcSettings(null);
        var prep = new CircuitPreparation();
        assertFalse(prep.prepare(prep.capture(model), PreparationRequest.defaults()).report().readyForHeadless());
    }

    @Test
    void persistedLawFindsForwardBiasAndStampsDiffusionChargeAndSeriesResistance() throws Exception {
        var source = new SpiceImporter(true).convert(DECK, "diode.cir");
        assertTrue(source.getImportIssues().isEmpty(), source.getImportIssues().toString());
        var model = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(source)), "diode.ipes");
        var prep = new CircuitPreparation();
        var result = prep.prepare(prep.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString());
        var plan = ready.execution().acPlan();
        var point = plan.diodeOperatingPoints().getFirst();
        assertEquals(1e-4, point.currentA(), 1e-12);
        assertEquals(0.0595571925, point.voltageV(), 1e-8);
        var sweep = plan.execute();
        var output = sweep.groups().stream().filter(group -> group.members().contains(new AcResult.Member("D1", 1))).findFirst().orElseThrow();
        var current = sweep.currents().stream().filter(trace -> trace.instance().equals("D1")).findFirst().orElseThrow();
        double gd = (1e-4 + 1e-14) / (0.1 * 1.380649e-23 * 300.15 / 1.602176634e-19);
        for (int sample = 0; sample < sweep.frequencyHz().size(); sample++) {
            double omega = 2 * Math.PI * sweep.frequencyHz().get(sample);
            var expected = new Complex(0.01, 0).add(Complex.ONE.divide(new Complex(gd, omega * 100e-9 * gd)))
                    .scale(1e-3);
            assertTrue(expected.subtract(output.voltageV().get(sample)).abs() < 1e-9);
            assertTrue(new Complex(1e-3, 0).subtract(current.currentA().get(sample)).abs() < 1e-12);
        }
    }

    @Test
    void subPicoampBiasMustConvergeInVoltageAsWellAsCurrent() {
        String deck = DECK.replace("100u AC 1m", "0.5p AC 1p").replace("D(tt=100n Rs=10m N=100m)", "D(Is=10f)");
        var plan = new AcExecutionPlan(new SpiceImporter(true).convert(deck, "small-bias.cir"));
        var point = plan.diodeOperatingPoints().getFirst();
        assertTrue(point.voltageV() > 0.07);
        assertEquals(0.5e-12, point.currentA(), 1e-19);
    }

    @Test
    void rejectsModelEffectsOutsideTheSupportedLaw() {
        var model = new SpiceImporter(true).convert(DECK.replace("tt=100n", "Bv=50"), "diode.cir");
        assertTrue(model.getImportIssues().stream().anyMatch(issue -> issue.component().equals("D1")));
    }
}
