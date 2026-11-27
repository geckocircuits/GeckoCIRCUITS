package gecko.core.simulation;

import static gecko.core.simulation.ControlSubsystemRateTest.block;
import static gecko.core.simulation.ControlSubsystemRateTest.open;
import static gecko.core.simulation.ControlSubsystemRateTest.parse;
import static gecko.core.simulation.ControlSubsystemRateTest.row;
import static gecko.core.simulation.ControlSubsystemRateTest.settings;
import static gecko.core.simulation.ControlSubsystemRateTest.sheet;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Signal-controlled sources fed from CONTROL subsystems with their own clocks, end to end through preparation,
 * factory, runtime and solver. A producer that samples at step s publishes to its source in the next step,
 * and the following solve applies it, so the source response lags the sample by two grid steps.
 */
class ControlSubsystemSourceTest {
    private static final long FAST = 101;
    private static final long SLOW = 202;
    private static final Map<String, Double> PERIODS = Map.of("fast", 10e-6, "slow", 100e-6);

    private static String lk(int type, String name, long id, String parameters, String strings, String from, String to) {
        return "e\n<ElementLK>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\n" + (strings.isEmpty() ? "" : "parameterString[] " + strings + "\n")
                + "labelAnfangsKnoten[] /" + from + "\nlabelEndKnoten[] /" + to + "\n<" + (char) 92 + "ElementLK>\n";
    }

    /** The producer of a ramp that equals the sample time in whole microseconds, published to the root sheet. */
    private static String producer(long sheetId, String name, long id, String root) {
        return block(58, name + "Time", id, sheetId, "", "", "/" + name + "clock")
                + block(7, name + "Gain", id + 1, sheetId, "1000000", "/" + name + "clock", "/" + name + "ramp")
                + block(5, name + "Scope", id + 2, sheetId, "", "/" + name + "ramp", "")
                + block(78, name + "Out", id + 3, sheetId, "", "/" + name + "ramp", "/" + root);
    }

    private static String sheets() { return sheet(FAST, 0, "fast") + sheet(SLOW, 0, "slow"); }

    private static void assertLags(float[] ramp, float[] response, int period, String what) {
        for (int step = 0; step < response.length; step++) {
            assertEquals(step < 2 ? 0 : ramp[step - 2], response[step], 1e-3, what + " at step " + step);
        }
        for (int step = 0; step < ramp.length; step++) {
            assertEquals(period * (step / period), ramp[step], 1e-3, "producer " + what + " " + step);
        }
    }

    @Test
    void anElectricalSourceFollowsOnlyTheSamplesOfItsProducersGroup() throws Exception {
        String signal = "400.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
        var model = parse(sheets()
                + lk(4, "Uf", 1001, signal, "/vf/NIX_NIX_NIX/0", "1", "0") + lk(1, "Rf", 1002, "10.0", "", "1", "0")
                + lk(4, "Us", 1003, signal, "/vs/NIX_NIX_NIX/0", "2", "0") + lk(1, "Rs", 1004, "10.0", "", "2", "0")
                + producer(FAST, "fast", 10, "vf") + producer(SLOW, "slow", 20, "vs")
                + block(1, "probeF", 30, 0, "", "/1/0", "", "/pf") + block(1, "probeS", 31, 0, "", "/2/0", "", "/ps")
                + block(5, "rootScope", 32, 0, "", "/pf/ps", ""));
        try (var session = open(model, settings(1e-6, 4e-4, 0, PERIODS))) {
            session.run();
            assertLags(row(session, "fastramp"), row(session, "pf"), 10, "fast source voltage");
            assertLags(row(session, "slowramp"), row(session, "ps"), 100, "slow source voltage");
            assertEquals(300, row(session, "ps")[302], 1e-3);
            assertEquals(200, row(session, "ps")[301], 1e-3, "one slow sample earlier at step 301");
        }
    }

    private static String thermalBlock(int type, String name, long id, String from, String to, String parameters, String strings) {
        return "eTH\n<ElementTHERM>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] " + strings + "\nlabelAnfangsKnoten[] /" + from + "\nlabelEndKnoten[] /" + to
                + "\n<" + (char) 92 + "ElementTHERM>\n";
    }

    private static String heatPath(String node, long id, String signal) {
        return thermalBlock(44, "Q" + node, id, "0", node, "400.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
                "/" + signal + "/NIX_NIX_NIX/0")
                + thermalBlock(46, "R" + node, id + 1, node, "0", "2.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "/NIX_NIX_NIX/NIX_NIX_NIX/0")
                + ThermalFixture.control(16, "TEMP." + node, id + 2, "", "T" + node, "/" + node + "/0/0",
                        "coupledReferenceID[] 0 \n");
    }

    @Test
    void aThermalSourceFollowsOnlyTheSamplesOfItsProducersGroup() throws Exception {
        var model = parse(sheets()
                + thermalBlock(48, "TREF.1", 8000, "0", "0", "0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "/NIX_NIX_NIX/NIX_NIX_NIX/0")
                + heatPath("tf", 8010, "pf") + heatPath("ts", 8020, "ps")
                + producer(FAST, "fast", 10, "pf") + producer(SLOW, "slow", 20, "ps")
                + block(5, "rootScope", 32, 0, "", "/Ttf/Tts", ""));
        try (var session = open(model, settings(1e-6, 4e-4, 0, PERIODS))) {
            session.run();
            float[] fast = row(session, "Ttf");
            float[] slow = row(session, "Tts");
            float[] fastRamp = row(session, "fastramp");
            float[] slowRamp = row(session, "slowramp");
            for (int step = 0; step < fast.length; step++) {
                // Two kelvin per watt, and the same two-step publication lag as the electrical source.
                assertEquals(step < 2 ? 0 : 2 * fastRamp[step - 2], fast[step], 1e-3, "fast " + step);
                assertEquals(step < 2 ? 0 : 2 * slowRamp[step - 2], slow[step], 1e-3, "slow " + step);
            }
            assertEquals(600, slow[302], 1e-3);
            assertEquals(400, slow[301], 1e-3, "held until the next slow sample reaches the source");
        }
    }
}
