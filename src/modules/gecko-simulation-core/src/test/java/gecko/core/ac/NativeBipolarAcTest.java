package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.SpiceImporter;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeBipolarAcTest {
    private static final String COMMON_EMITTER = """
            * Mirrored common-emitter stage with an ideal bias source
            Vcc supply 0 %s5
            Vb base 0 %s0.65 AC 1
            R1 supply out 1k
            Q1 out base 0 local area=%s
            .model local %s (Is=1e-14 Bf=300 Br=4 Cje=8p Cjc=4p Tf=350p)
            .ac dec 10 1k 10Meg
            """;

    @Test
    void bipolarImportIsOptInAndKeepsCollectorBaseEmitterOrderAcrossSave() throws Exception {
        String deck = COMMON_EMITTER.formatted("", "", "1", "NPN");
        var off = new SpiceImporter().convert(deck, "bjt.cir");
        assertTrue(off.getImportIssues().stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_ELEMENT")));

        var model = new SpiceImporter(false, true).convert(deck, "bjt.cir");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var saved = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "bjt.ipes");
        var transistor = saved.getCircuitComponents().stream().filter(component -> component.getName().equals("Q1"))
                .findFirst().orElseThrow().getAcComponent();
        assertEquals(java.util.List.of(1, 2, 3), transistor.spiceOrders());
        var bipolar = assertInstanceOf(AcComponent.Bipolar.class, transistor.law());
        assertFalse(bipolar.pnp());
        assertEquals("8p", bipolar.parameters().get("cje"));
        assertEquals(1, saved.getCircuitComponents().stream().filter(component -> component.getName().equals("Q1"))
                .findFirst().orElseThrow().getNumericParameters()[6]);
        double gm = 1e-14 * Math.exp(.65 / .025864925786328215) / .025864925786328215;
        assertEquals(-1000 * gm, output(new AcExecutionPlan(saved).execute()).real(), 1e-4);
    }

    @Test
    void aGroundedSubstrateKeepsTheSavedPnpSymbolPolarity() throws Exception {
        String deck = COMMON_EMITTER.formatted("-", "-", "1", "PNP").replace("base 0 local", "base 0 0 local");
        var model = new SpiceImporter(false, true).convert(deck, "pnp.cir");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var saved = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "pnp.ipes");
        var component = saved.getCircuitComponents().stream().filter(item -> item.getName().equals("Q1")).findFirst().orElseThrow();
        var bipolar = assertInstanceOf(AcComponent.Bipolar.class, component.getAcComponent().law());
        assertTrue(bipolar.pnp());
        assertEquals(0, component.getNumericParameters()[6]);
        assertTrue(output(new AcExecutionPlan(saved).execute()).real() < 0);
        var unsupported = new SpiceImporter(false, true).convert(deck.replace("base 0 0 local", "base 0 substrate local"),
                "pnp-floating-substrate.cir");
        assertTrue(unsupported.getImportIssues().stream().anyMatch(issue -> issue.component().equals("Q1")));
    }

    @Test
    void mirroredPolarityAndAreaPreserveTheExpectedGain() {
        var converter = new SpiceImporter(false, true);
        var npn = output(new AcExecutionPlan(converter.convert(COMMON_EMITTER.formatted("", "", "1", "NPN"),
                "npn.cir")).execute());
        var pnp = output(new AcExecutionPlan(converter.convert(COMMON_EMITTER.formatted("-", "-", "1", "PNP"),
                "pnp.cir")).execute());
        var doubled = output(new AcExecutionPlan(converter.convert(COMMON_EMITTER.formatted("", "", "2", "NPN"),
                "npn2.cir")).execute());
        assertEquals(npn.real(), pnp.real(), 1e-8);
        assertEquals(npn.imaginary(), pnp.imaginary(), 1e-8);
        assertEquals(2 * npn.real(), doubled.real(), 1e-7);
        assertTrue(Math.abs(doubled.imaginary()) > Math.abs(npn.imaginary()));
    }

    @Test
    void bipolarJacobianMatchesTerminalCurrentDifferencesAndConservesCurrent() {
        var law = new AcComponent.Bipolar("test", false, "3", Map.of("is", "1e-14", "bf", "300",
                "vaf", "100", "ikf", ".4", "cje", "8p", "tf", "350p"));
        var model = BipolarJunction.from(law, Map.of());
        double be = .73, bc = -.4, step = 1e-7;
        var point = model.at(be, bc);
        assertEquals(0, java.util.Arrays.stream(point.current()).sum(), 1e-16);
        for (int terminal = 0; terminal < 3; terminal++) {
            double dBe = (model.at(be + step, bc).current()[terminal] - model.at(be - step, bc).current()[terminal]) / (2 * step);
            double dBc = (model.at(be, bc + step).current()[terminal] - model.at(be, bc - step).current()[terminal]) / (2 * step);
            assertEquals(dBe, point.dBe()[terminal], 1e-9);
            assertEquals(dBc, point.dBc()[terminal], 1e-10);
        }
    }

    private static Complex output(AcResult result) {
        return result.groups().stream().filter(trace -> trace.members().contains(new AcResult.Member("Q1", 1)))
                .findFirst().orElseThrow().voltageV().getFirst();
    }
}
