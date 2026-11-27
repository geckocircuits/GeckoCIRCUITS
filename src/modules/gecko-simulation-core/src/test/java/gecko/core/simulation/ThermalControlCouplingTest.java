package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/** The CONTROL↔THERMAL exchange of the core: signal-controlled thermal sources and TEMP and FLOW probes. */
class ThermalControlCouplingTest {
    private static final char BACKSLASH = 92;
    private static final double RTH = 2, CTH = 0.01, HEAT = 5;

    private static String thermal(int type, String name, long id, String from, String to, String parameters, String strings, String extra) {
        return "eTH\n<ElementTHERM>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] " + strings + "\nlabelAnfangsKnoten[] /" + from + "\nlabelEndKnoten[] /" + to + "\n" + extra
                + "<" + BACKSLASH + "ElementTHERM>\n";
    }

    private static String control(int type, String name, long id, String in, String out, String parameters, String strings, String extra) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] " + strings + "\nlabelAnfangsKnoten[] " + (in.isEmpty() ? "" : "/" + in)
                + "\nlabelEndKnoten[] " + (out.isEmpty() ? "" : "/" + out) + "\n" + extra + "<" + BACKSLASH + "ElementCONTROL>\n";
    }

    private static final String NONE = "/NIX_NIX_NIX/NIX_NIX_NIX/0";
    private static final String PASSIVE = " 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";

    /** Heat from a constant CONTROL signal into an RC node, one unfed source, one pinned temperature, and every probe kind. */
    private static String circuit(String probes) {
        return thermal(44, "Q.1", 1, "0", "a", "400.0 0.0" + PASSIVE, "/Q/NIX_NIX_NIX/0", "")
                + thermal(46, "Rth.1", 2, "a", "0", RTH + PASSIVE, NONE, "")
                + thermal(47, "Cth.1", 3, "a", "0", CTH + " 0.0 0.0 0.0 0.0 0.0 " + CTH + " 0.0 0.0 0.0 0.0", NONE, "")
                + thermal(44, "Q.2", 4, "0", "b", "400.0 0.0" + PASSIVE, "/unfed/NIX_NIX_NIX/0", "")
                + thermal(46, "Rth.2", 5, "b", "0", RTH + PASSIVE, NONE, "")
                + thermal(45, "Tset", 6, "t", "0", "400.0 0.0" + PASSIVE, "/Ts/NIX_NIX_NIX/0", "")
                + thermal(48, "TREF.1", 7, "0", "0", "0.0" + PASSIVE, NONE, "")
                + control(3, "CONST.Q", 100, "", "Q", HEAT + "", NONE, "")
                + control(3, "CONST.T", 101, "", "Ts", "30.0", NONE, "")
                + probes;
    }

    private static CircuitModel parse(String blocks) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader("dt 0.001\ntDURATION 0.2\n" + blocks)), "coupling.ipes");
    }

    private static String temp(String name, long id, String out, String labels, long reference) {
        return control(16, name, id, "", out, "0.0", labels, "coupledReferenceID[] " + reference + " \n");
    }

    private static String flow(String name, long id, String out, long reference, String extra) {
        return control(17, name, id, "", out, "0.0", NONE, "coupledReferenceID[] " + reference + " \n" + extra);
    }

    private static PreparationResult prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
    }

    private static ReadinessReport report(PreparationResult result) {
        return result.report();
    }

    @Test
    void controlSignalsDriveThermalSourcesAndEveryProbeKindReadsTheThermalStep() throws Exception {
        String probes = temp("TEMP.A", 200, "Ta", "/a/0/0", 0) + temp("TEMP.B", 201, "Tb", NONE, 2)
                + temp("TEMP.C", 202, "Tu", "/b/0/0", 0) + temp("TEMP.D", 203, "Tp", "/t/0/0", 0)
                + flow("FLOW.R", 210, "Fr", 2, "") + flow("FLOW.C", 211, "Fc", 3, "") + flow("FLOW.Q", 212, "Fq", 1, "")
                + flow("FLOW.A", 213, "Fa", 7, "")
                + control(5, "SCOPE.1", 300, "Ta/Tb/Tu/Tp/Fr/Fc/Fq/Fa", "", "0.0", NONE, "");
        var prepared = prepare(parse(circuit(probes)));
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, report(prepared).diagnostics().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        var names = result.getSignalNames();
        float[] ta = result.getSignalData(ThermalFixture.row(names, "Ta")), tb = result.getSignalData(ThermalFixture.row(names, "Tb"));
        float[] tu = result.getSignalData(ThermalFixture.row(names, "Tu")), tp = result.getSignalData(ThermalFixture.row(names, "Tp"));
        float[] fr = result.getSignalData(ThermalFixture.row(names, "Fr")), fc = result.getSignalData(ThermalFixture.row(names, "Fc"));
        float[] fq = result.getSignalData(ThermalFixture.row(names, "Fq"));
        int last = ta.length - 1;
        // The control signal heats the node to Q*R. The probe by label and by coupled element read the same difference.
        assertEquals(HEAT * RTH, ta[last], 0.05);
        for (int step = 0; step <= last; step++) assertEquals(ta[step], tb[step], 1e-6f, "step " + step);
        // A source whose signal has no producer holds zero.
        assertEquals(0, tu[last], 1e-6);
        // A signal-controlled temperature source pins its node.
        assertEquals(30, tp[last], 1e-3);
        // The heat flow through the resistor is T/R. The capacitor takes the difference and ends at zero.
        assertEquals(ta[last] / RTH, fr[last], 1e-3);
        assertTrue(fc[0] > 0, "the cold capacitor takes heat first");
        assertEquals(0, fc[last], 1e-2);
        assertEquals(HEAT, Math.abs(fq[last]), 1e-2, "the source delivers its signal");
    }

    @Test
    void anUnresolvedLossCouplingFallsBackAndUnresolvedProbeReferencesAreInvalid() throws Exception {
        String loss = thermal(41, "Pv.1", 8, "", "c", "", NONE, "coupledReferenceID[] 999 \n");
        var onLossSource = report(prepare(parse(circuit(loss + temp("TEMP.P", 220, "Tx", NONE, 8)))));
        assertTrue(onLossSource.diagnostics().stream().anyMatch(item -> item.code().equals("UNSUPPORTED_LOSS_COMPONENT")));
        String unresolved = temp("TEMP.U", 221, "Tx", NONE, 424242);
        var invalid = report(prepare(parse(circuit(unresolved)))).diagnostics().stream()
                .filter(item -> item.code().equals("INVALID_THERMAL_PROBE")).findFirst().orElseThrow();
        assertEquals("TEMP.U", invalid.component());
        var flowInvalid = report(prepare(parse(circuit(flow("FLOW.U", 222, "Fx", 424242, "")))));
        assertEquals("FLOW.U", flowInvalid.diagnostics().stream().filter(item -> item.code().equals("INVALID_THERMAL_PROBE"))
                .findFirst().orElseThrow().component());
    }

    @Test
    void aTemperatureProbeCoupledDirectlyToALossSourceIsACapabilityGap() throws Exception {
        var fixture = ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.control(16, "TEMP.PV", 6300, "", "Tpv", "/NIX_NIX_NIX/NIX_NIX_NIX/0", "coupledReferenceID[] 5001 \n");
        var item = report(prepare(ThermalFixture.model(fixture))).diagnostics().stream()
                .filter(d -> d.code().equals("UNSUPPORTED_CONTROL_COMPONENT")).findFirst().orElseThrow();
        assertEquals("TEMP.PV", item.component());
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
    }
}
