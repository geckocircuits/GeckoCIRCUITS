package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioural sources linearised at the DC operating point (ADR-0033). Each deck carries 0 V and 0 A placeholders
 * that receive the law, as the importer will once it maps B elements.
 */
class BehaviouralAcTest {

    /** Archive id 438 in AC: the XFMR subcircuit, B1 {@code I=I(V1)*ratio} and B2 {@code V=V(1,2)*ratio}. */
    private static final String TRANSFORMER = """
            .ac dec 5 1k 1Meg
            .param k=0.995
            .param ratio={(1/k)*sqrt(37.5u/606u)}
            V2 in 0 AC 1
            VIV1 in n001 0
            R2 n002 n001 10m
            Lf n002 n003 {606u*(1-k**2)} Rser=1m
            Lm n003 0 {606u*k**2} Rser=1m
            IB1 n003 0 0
            VB2 x1n001 0 0
            V1 x1n001 x1n002 0
            RX1 out1 x1n002 1u
            RX2 n003 0 1Meg
            R1 out1 0 1k
            """;

    @Test
    void transformerMatchesTheIdealTransformerAnalytically() throws Exception {
        var model = withLaws(TRANSFORMER, nodes -> Map.of("IB1", new AcComponent.Behavioural("I(V1)*{ratio}", true),
                "VB2", new AcComponent.Behavioural("V(" + nodes.apply("Lm", 0) + ",0)*{ratio}", false)));
        String out1 = node(model, "R1", 0);
        var result = solve(roundTrip(model));
        double k = 0.995, ratio = (1 / k) * Math.sqrt(37.5e-6 / 606e-6), lf = 606e-6 * (1 - k * k), lm = 606e-6 * k * k;
        for (int i = 0; i < result.frequencyHz().size(); i++) {
            var s = new Complex(0, 2 * Math.PI * result.frequencyHz().get(i));
            var reflected = new Complex(ratio * ratio / (1e3 + 1e-6), 0);
            var shunt = Complex.ONE.divide(s.scale(lm).add(new Complex(1e-3, 0))).add(new Complex(1e-6, 0)).add(reflected);
            var series = s.scale(lf).add(new Complex(1e-3 + 10e-3, 0));
            var v3 = Complex.ONE.divide(Complex.ONE.add(series.multiply(shunt)));
            var out1Voltage = v3.scale(ratio * 1e3 / (1e3 + 1e-6));
            assertClose(out1Voltage, voltage(result, out1, i), 1e-9);
            assertClose(v3.multiply(reflected), current(result, "IB1", i), 1e-6);
        }
    }

    @Test
    void productsAndIfLineariseAtTheBias() {
        var model = withLaws("""
                .ac dec 1 1k 1k
                Va a 0 10
                Vd d 0 DC 0.4 AC 1
                Vs a s 0
                Rs s 0 5
                IB1 c 0 0
                Rc c 0 1
                VB2 o 0 0
                Ro o 0 1k
                VB3 e 0 0
                Re e 0 1k
                """, nodes -> {
                    String a = "V(" + nodes.apply("Va", 0) + ")", d = "V(" + nodes.apply("Vd", 0) + ")";
                    return Map.of("IB1", new AcComponent.Behavioural("I(Vs)*" + d, true),
                            "VB2", new AcComponent.Behavioural(a + "*" + d, false),
                            "VB3", new AcComponent.Behavioural("IF(" + d + "/2>0.99,0.99," + d + "/2)", false));
                });
        var result = solve(model);
        // d(I(Vs) V(d)) = 2 A * vd; d(V(a) V(d)) = 10 V * vd; the unclamped branch of IF has slope 1/2.
        assertClose(new Complex(2, 0), current(result, "IB1", 0), 1e-12);
        assertClose(new Complex(-2, 0), voltage(result, node(model, "Rc", 0), 0), 1e-12);
        assertClose(new Complex(10, 0), voltage(result, node(model, "Ro", 0), 0), 1e-12);
        assertClose(new Complex(0.5, 0), voltage(result, node(model, "Re", 0), 0), 1e-12);
    }

    @Test
    void rejectsAnUnknownNodeAndAMissingBranchCurrent() {
        for (String expression : new String[]{"V(nowhere)", "I(Rs)"}) {
            var model = withLaws("""
                    .ac dec 1 1k 1k
                    Va a 0 AC 1
                    Rs a 0 5
                    VB2 o 0 0
                    Ro o 0 1k
                    """, nodes -> Map.of("VB2", new AcComponent.Behavioural(expression, false)));
            var error = assertThrows(IllegalArgumentException.class, () -> new AcExecutionPlan(model));
            assertTrue(error.getMessage().toLowerCase().contains(expression.substring(2, expression.length() - 1).toLowerCase()),
                    error.getMessage());
        }
    }

    /** Selects the laws. The importer names nodes N1, N2, ..., so laws read node names from the imported terminals. */
    private static CircuitModel withLaws(String deck,
            Function<BiFunction<String, Integer, String>, Map<String, AcComponent.Behavioural>> select) {
        var model = new SpiceImporter().convert(deck, "behavioural.cir");
        var laws = select.apply((instance, port) -> node(model, instance, port));
        int replaced = 0;
        for (var component : model.getCircuitComponents()) {
            var owned = component.getAcComponent();
            var law = owned == null ? null : laws.get(owned.instance());
            if (law == null) continue;
            component.setAcComponent(new AcComponent(owned.instance(), owned.spiceOrders(), owned.aliases(), owned.mirrored(), law));
            replaced++;
        }
        assertEquals(laws.size(), replaced);
        return model;
    }

    private static String node(CircuitModel model, String instance, int port) {
        return model.getCircuitComponents().stream().map(CircuitModel.ComponentData::getAcComponent)
                .filter(owned -> owned != null && owned.instance().equals(instance)).findFirst().orElseThrow().aliases().get(port);
    }

    private static AcResult solve(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        return ready.execution().acPlan().execute();
    }

    private static CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "behavioural.ipes");
    }

    private static Complex voltage(AcResult result, String label, int index) {
        return result.groups().stream().filter(group -> group.aliases().stream().anyMatch(label::equalsIgnoreCase))
                .findFirst().orElseThrow(() -> new AssertionError("no node " + label)).voltageV().get(index);
    }

    private static Complex current(AcResult result, String instance, int index) {
        return result.currents().stream().filter(trace -> trace.instance().equalsIgnoreCase(instance))
                .findFirst().orElseThrow(() -> new AssertionError("no current " + instance)).currentA().get(index);
    }

    private static void assertClose(Complex expected, Complex actual, double relative) {
        double error = expected.subtract(actual).abs();
        assertTrue(error <= relative * Math.max(1e-12, expected.abs()), "expected " + expected + " but was " + actual);
    }
}
