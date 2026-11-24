package gecko.core.simulation;

import static gecko.core.simulation.ControlSubsystemRateTest.block;
import static gecko.core.simulation.ControlSubsystemRateTest.parse;
import static gecko.core.simulation.ControlSubsystemRateTest.sheet;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitModel;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Each subsystem group keeps the #61 clock arithmetic. A grouped run exports, for every group, the same sample phase
 * and flag, bit for bit, as a global-only run with that group's period. Any drift of the running time sum that #61
 * already has shows up in both runs alike, so this test observes it and does not hide it.
 */
class ControlSubsystemClockParityTest {
    private static final long FAST = 101;
    private static final long SLOW = 202;

    private static CircuitModel grouped() throws Exception {
        return parse(sheet(FAST, 0, "fast") + sheet(SLOW, 0, "slow")
                + block(58, "fastTime", 1, FAST, "", "", "/fastclock") + block(5, "fastScope", 2, FAST, "", "/fastclock", "")
                + block(58, "slowTime", 3, SLOW, "", "", "/slowclock") + block(5, "slowScope", 4, SLOW, "", "/slowclock", ""));
    }

    private static CircuitModel flat() throws Exception {
        return parse(block(58, "rootTime", 1, 0, "", "", "/rootclock") + block(5, "rootScope", 2, 0, "", "/rootclock", ""));
    }

    /** The exported sample phases and flags by label, in raw bits, read from the monitor after one export. */
    private static Map<String, Long> export(ControlRuntime runtime, double time) throws Exception {
        var monitor = new PeriodicStateMonitor(1e-3, 3, 100);
        monitor.begin();
        runtime.exportPeriodicState(new CircuitNetlist(), time, monitor.sink());
        monitor.commit(1, time);
        int cursor = field("cursor").getInt(monitor);
        var values = (double[]) field("values").get(monitor);
        var labels = (String[]) field("labels").get(monitor);
        var result = new java.util.LinkedHashMap<String, Long>();
        for (int index = 0; index < cursor; index++) {
            if (labels[index] != null && labels[index].startsWith("sample phase")) result.put(labels[index], Double.doubleToRawLongBits(values[index]));
        }
        return result;
    }

    private static java.lang.reflect.Field field(String name) throws Exception {
        var field = PeriodicStateMonitor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static long only(Map<String, Long> exported, String prefix, int index) {
        var matches = new ArrayList<Long>();
        exported.forEach((label, bits) -> { if (label.startsWith(prefix) && label.endsWith("[" + index + "]")) matches.add(bits); });
        assertEquals(1, matches.size(), prefix + "[" + index + "] in " + exported.keySet());
        return matches.get(0);
    }

    /** Runs one grouped and two global runtimes and compares the phase bits of each group at every step. */
    private static void compare(double dt, double fast, double slow, int steps, int changeAt, double changedStep) throws Exception {
        var periods = Map.of("fast", fast, "slow", slow);
        try (var group = ControlRuntime.fromDeferred(grouped(), dt, null, 0, null, periods);
                var fastOnly = ControlRuntime.fromDeferred(flat(), dt, null, fast, null, Map.of());
                var slowOnly = ControlRuntime.fromDeferred(flat(), dt, null, slow, null, Map.of())) {
            var netlist = new CircuitNetlist();
            double time = 0;
            double step = dt;
            for (int index = 0; index < steps; index++) {
                if (changeAt > 0 && index == changeAt) {
                    step = changedStep;
                    group.prepareTimeStepChange(step).run();
                    fastOnly.prepareTimeStepChange(step).run();
                    slowOnly.prepareTimeStepChange(step).run();
                }
                group.advanceAfterElectricalStep(netlist, time);
                fastOnly.advanceAfterElectricalStep(netlist, time);
                slowOnly.advanceAfterElectricalStep(netlist, time);
                var exported = export(group, time);
                assertEquals(only(export(fastOnly, time), "sample phase", 0), only(exported, "sample phase of CONTROL group 1", 0),
                        "fast phase bits at step " + index);
                assertEquals(only(export(fastOnly, time), "sample phase", 1), only(exported, "sample phase of CONTROL group 1", 1),
                        "fast flag at step " + index);
                assertEquals(only(export(slowOnly, time), "sample phase", 0), only(exported, "sample phase of CONTROL group 2", 0),
                        "slow phase bits at step " + index);
                assertEquals(only(export(slowOnly, time), "sample phase", 1), only(exported, "sample phase of CONTROL group 2", 1),
                        "slow flag at step " + index);
                time = SimulationTiming.nextTime(time, step);
            }
        }
    }

    @Test
    void commensurateClocksExportTheGlobalRunPhasesBitForBitBeyondTenThousandSteps() throws Exception {
        compare(1e-6, 10e-6, 100e-6, 12000, 0, 0);
    }

    @Test
    void aNonIntegerRatioExportsTheGlobalRunPhasesBitForBit() throws Exception {
        compare(3e-6, 10e-6, 25e-6, 4000, 0, 0);
    }

    @Test
    void aStepChangeKeepsEveryGroupOnTheGlobalRunClock() throws Exception {
        compare(1e-6, 10e-6, 100e-6, 3000, 1500, 2e-6);
        compare(4e-6, 10e-6, 100e-6, 1500, 700, 20e-6);
    }
}
