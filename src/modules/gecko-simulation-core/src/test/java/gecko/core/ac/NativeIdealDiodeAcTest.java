package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeIdealDiodeAcTest {
    private static final String MODEL = ".model clamp D(Vfwd=.5 Ron=10 Roff=1meg)\n";

    @Test
    void findsForwardAndReverseBiasBeforeApplyingTheAcExcitation() throws Exception {
        for (double dc : List.of(2.0, -1.0)) {
            var model = roundTrip(importDeck("""
                    V1 rail 0 DC %s AC 1 27
                    R1 rail leaf 1k
                    D1 leaf 0 clamp
                    C1 leaf 0 10n
                    """.formatted(dc) + MODEL));
            var diode = model.getCircuitComponents().stream().filter(c -> c.getName().equals("D1")).findFirst().orElseThrow();
            var law = assertInstanceOf(AcComponent.IdealDiode.class, diode.getAcComponent().law());
            assertEquals("clamp", law.modelName());
            assertEquals("10", law.onResistance());
            var plan = new AcExecutionPlan(model);
            var point = plan.diodeOperatingPoints().getFirst();
            boolean forward = dc > 0;
            double slope = forward ? .1 : 1e-6;
            double intercept = forward ? (.000001 - .1) * .5 : 0;
            double expectedVoltage = (dc / 1000 - intercept) / (.001 + slope);
            assertEquals(forward, point.forward());
            assertEquals(slope, point.conductanceS());
            assertEquals(expectedVoltage, point.voltageV(), 1e-12);
            assertEquals((dc - expectedVoltage) / 1000, point.currentA(), 1e-12);
            var result = plan.execute();
            for (int i = 0; i < result.frequencyHz().size(); i++) {
                var admittance = new Complex(.001 + slope, 2 * Math.PI * 10e-9 * result.frequencyHz().get(i));
                var expected = Complex.polar(.001, Math.toRadians(27)).divide(admittance);
                near(expected, voltage(result, "D1", 1, i));
                near(expected.scale(slope), current(result, "D1", i));
                near(current(result, "R1", i), current(result, "D1", i).add(current(result, "C1", i)));
            }
        }
    }

    @Test
    void adjudicatesTwoOpposingClampsAndUsesDcCurrentAndStaticGain() {
        String deck = """
                Vlower low 0 .1
                Vupper rail 0 2
                I1 0 mid DC %s AC 1m %s
                R1 mid 0 1k
                E1 high 0 rail 0 2
                G1 mid 0 rail 0 10u
                D1 low mid clamp
                D2 mid high clamp
                """ + MODEL;
        for (double dc : List.of(-.005, .002, .008)) {
            var plan = new AcExecutionPlan(importDeck(deck.formatted(dc, "30")));
            var points = plan.diodeOperatingPoints();
            assertEquals(2, points.size());
            assertEquals(dc < 0, points.getFirst().forward());
            assertEquals(dc > .004, points.getLast().forward());
            assertEquals(points, new AcExecutionPlan(importDeck(deck.formatted(dc, "90"))).diodeOperatingPoints());
            double node = .1 - points.getFirst().voltageV();
            assertEquals(node - 4, points.getLast().voltageV(), 1e-12);
            assertEquals(dc + points.getFirst().currentA(), node / 1000 + 20e-6 + points.getLast().currentA(), 1e-12);
        }
    }

    @Test
    void rejectsKneesAndUnsupportedModelOptions() {
        // The ideal drive fixes precisely the diode threshold, regardless of its segment.
        var kink = importDeck("V1 n 0 .5 AC 1\nD1 n 0 clamp\n" + MODEL);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new AcExecutionPlan(kink))
                .getMessage().contains("nonsmooth threshold"));
        var reverse = new AcExecutionPlan(importDeck("V1 n 0 -2 AC 1\nD1 0 n clamp\n" + MODEL));
        assertTrue(reverse.diodeOperatingPoints().getFirst().forward());
        assertTrue(current(reverse.execute(), "D1", 0).real() < 0);
        for (String options : List.of("Is=1p", "Vfwd=.5 epsilon=1m", "Vfwd=.5 Vrev=10",
                "Vfwd=.5 Vfwd=.7", "Vfwd=.5 Ron=1meg Roff=10", "Vfwd=.5 Ron=1e-999 Roff=1meg")) {
            var model = importDeck("V1 n 0 1 AC 1\nD1 n 0 clamp\n.model clamp D(" + options + ")\n");
            assertTrue(model.getImportIssues().stream().anyMatch(issue -> issue.component().equals("D1")), options);
        }
    }

    @Test
    void refusesToSubstituteTheLegacyTransientDiodeForAnAcLaw() {
        var model = importDeck("V1 rail 0 1 AC 1\nR1 rail leaf 1k\nD1 leaf 0 clamp\n" + MODEL);
        assertTrue(model.getImportIssues().isEmpty());
        model.setAcSettings(null);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        assertFalse(result.report().readyForHeadless());
        assertTrue(result.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("UNSUPPORTED_COMPONENT")
                        && diagnostic.message().equals("Use AC analysis for this ideal diode law.")),
                result.report().toString());
    }

    @Test
    void rejectsDivergentReferencesToOneLocalModel() {
        var model = importDeck("V1 rail 0 2 AC 1\nR1 rail leaf 1k\nD1 leaf 0 clamp\nD2 leaf 0 CLAMP\n" + MODEL);
        assertTrue(model.getImportIssues().isEmpty());
        var diode = model.getCircuitComponents().getLast();
        var owned = diode.getAcComponent();
        var law = (AcComponent.IdealDiode) owned.law();
        assertEquals("clamp", law.modelName());
        diode.setAcComponent(new AcComponent(owned.instance(), owned.spiceOrders(), owned.aliases(), owned.mirrored(),
                new AcComponent.IdealDiode("clamp", "3", law.onResistance(), law.offResistance(), law.origin())));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(model))
                .getMessage().contains("Conflicting ideal diode model clamp"));
    }

    private static CircuitModel importDeck(String deck) {
        return new SpiceImporter().convert(".ac dec 1 10 100\n" + deck, "diode-test.cir");
    }

    private static CircuitModel roundTrip(CircuitModel model) throws Exception {
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        return new CircuitFileParser().parse(
                new ByteArrayInputStream(new CircuitFileWriter().write(model)), "diode-test.ipes");
    }

    private static Complex voltage(AcResult result, String name, int order, int sample) {
        return result.groups().stream().filter(group -> group.members().contains(new AcResult.Member(name, order)))
                .findFirst().orElseThrow().voltageV().get(sample);
    }

    private static Complex current(AcResult result, String name, int sample) {
        return result.currents().stream().filter(trace -> trace.instance().equals(name))
                .findFirst().orElseThrow().currentA().get(sample);
    }

    private static void near(Complex expected, Complex actual) {
        assertTrue(expected.subtract(actual).abs() <= 1e-12 + expected.abs() * 1e-9, expected + " != " + actual);
    }
}
