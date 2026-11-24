package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The thermal network inside the public session: step order, probes, continuation, and sub-step independence. */
class ThermalSessionTest {
    private static PreparationResult.Ready ready(CircuitModel model, SolverOverrides overrides) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(overrides, Map.of()));
        var report = switch (result) {
            case PreparationResult.Ready r -> r.report();
            case PreparationResult.Inspectable i -> i.report();
            case PreparationResult.Rejected r -> r.report();
        };
        assertInstanceOf(PreparationResult.Ready.class, result, report.diagnostics().toString());
        return (PreparationResult.Ready) result;
    }

    /** One backward-Euler step of an RC node that a loss source heats, with the parallel 10 kOhm of the loss source. */
    private static double nextTemperature(double previous, double heat, double dt, double resistance, double capacitance) {
        double conductance = 1 / resistance + 1 / 10000.0;
        return (capacitance / dt * previous + heat) / (capacitance / dt + conductance);
    }

    @Test
    void heatEntersTheThermalStepAfterTheStepThatComputedItAndProbesReadTheCurrentStep() throws Exception {
        var prepared = ready(ThermalFixture.model(), SolverOverrides.none());
        try (var session = SimulationSession.open(prepared.execution(), ExecutionOptions.defaults())) {
            var names = session.signalNames();
            int temperature = ThermalFixture.row(names, "TjS"), loss = ThermalFixture.row(names, "PS");
            double dt = session.status().stepWidth();
            double[] t = new double[400], p = new double[400];
            for (int step = 0; step < t.length; step++) {
                var sample = session.step();
                t[step] = sample.values()[temperature];
                p[step] = sample.values()[loss];
            }
            double previous = 0, peak = 0, maxLoss = 0;
            for (int step = 0; step < t.length; step++) {
                // Heat that step n-1 computed is stamped at step n. The first thermal step has none.
                double expected = nextTemperature(previous, step == 0 ? 0 : p[step - 1], dt,
                        ThermalFixture.SWITCH_R, ThermalFixture.SWITCH_C);
                assertEquals(expected, t[step], Math.max(1e-4, Math.abs(expected) * 2e-5), "step " + step);
                previous = expected;
                peak = Math.max(peak, t[step]);
                maxLoss = Math.max(maxLoss, p[step]);
            }
            assertTrue(maxLoss > 0, "the switch dissipates");
            assertTrue(peak > 0, "the junction heats");
            assertEquals(0, t[0], 0, "no heat has entered at the first step");
            assertTrue(p[0] > 0 || p[1] > 0 || maxLoss > 0);
            assertEquals(400, session.thermalSolves());
        }
    }

    @Test
    void thermalAdvancesOncePerGridStepWhateverTheElectricalSubSteps() throws Exception {
        var adaptive = new SolverFeatures(0, true, false, null, new AdaptiveStepSettings(1e-3, 1e-9, Double.NaN));
        var prepared = ready(ThermalFixture.model(), new SolverOverrides(null, null, null, adaptive));
        try (var session = SimulationSession.open(prepared.execution(), ExecutionOptions.defaults())) {
            session.run();
            int steps = session.status().committedSteps();
            assertTrue(session.adaptiveStepReport().accepted() > steps, "sub-steps exist: " + session.adaptiveStepReport());
            assertEquals(steps, session.thermalSolves());
        }
    }

    @Test
    void aStepChangeKeepsTheTemperatureContinuousAndRebuildsTheMatrix() throws Exception {
        var prepared = ready(ThermalFixture.model(), SolverOverrides.none());
        try (var session = SimulationSession.open(prepared.execution(), ExecutionOptions.defaults())) {
            var names = session.signalNames();
            int temperature = ThermalFixture.row(names, "TjS"), loss = ThermalFixture.row(names, "PS");
            double dt = session.status().stepWidth();
            double previous = 0, previousHeat = 0;
            for (int step = 0; step < 100; step++) {
                var sample = session.step();
                previous = nextTemperature(previous, previousHeat, dt, ThermalFixture.SWITCH_R, ThermalFixture.SWITCH_C);
                previousHeat = sample.values()[loss];
                assertEquals(previous, sample.values()[temperature], Math.max(1e-4, Math.abs(previous) * 2e-5));
            }
            double newStep = dt * 2;
            session.extend(session.status().endTime() + 100 * newStep, newStep);
            for (int step = 0; step < 100; step++) {
                var sample = session.step();
                previous = nextTemperature(previous, previousHeat, newStep, ThermalFixture.SWITCH_R, ThermalFixture.SWITCH_C);
                previousHeat = sample.values()[loss];
                assertEquals(previous, sample.values()[temperature], Math.max(1e-4, Math.abs(previous) * 2e-5),
                        "continued step " + step);
            }
            assertEquals(200, session.thermalSolves());
        }
    }

    @Test
    void conductionAndSwitchingProbesSumToTheTotalOfTheSameLossSource() throws Exception {
        // Give the switch real switching energies so that all three parts are nonzero.
        String text = ThermalFixture.buckText().replace("idStringDialog S.1\n",
                "idStringDialog S.1\n<Verluste>\nverlustTyp 1\nkON 3.0E-5\nkOFF 1.5E-5\nuSWnorm 48.0\n<" + (char) 92 + "Verluste>\n");
        String probes = ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.control(17, "FLOW.T", 6201, "", "PT", "/Pv.1/NIX_NIX_NIX/0",
                        "coupledReferenceID[] 5001 \nlosscomp " + gecko.core.circuit.losscalculation.LossComponent.TOTAL.getSaveString() + "\n")
                + ThermalFixture.control(17, "FLOW.C", 6202, "", "PC", "/Pv.1/NIX_NIX_NIX/0",
                        "coupledReferenceID[] 5001 \nlosscomp " + gecko.core.circuit.losscalculation.LossComponent.CONDUCTION.getSaveString() + "\n")
                + ThermalFixture.control(17, "FLOW.W", 6203, "", "PW", "/Pv.1/NIX_NIX_NIX/0",
                        "coupledReferenceID[] 5001 \nlosscomp " + gecko.core.circuit.losscalculation.LossComponent.SWITCHING.getSaveString() + "\n")
                + ThermalFixture.control(5, "SCOPE.L", 6204, "/PT/PC/PW", "", "/NIX_NIX_NIX", "");
        var model = new gecko.core.io.CircuitFileParser().parse(new java.io.BufferedReader(new java.io.StringReader(text + "\n" + probes)), "losscomp.ipes");
        var prepared = ready(model, SolverOverrides.none());
        var result = new HeadlessSimulationEngine().runSimulation(prepared.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        float[] total = result.getSignalData(ThermalFixture.row(result.getSignalNames(), "PT"));
        float[] conduction = result.getSignalData(ThermalFixture.row(result.getSignalNames(), "PC"));
        float[] switching = result.getSignalData(ThermalFixture.row(result.getSignalNames(), "PW"));
        float[] temperature = result.getSignalData(ThermalFixture.row(result.getSignalNames(), "TjS"));
        double maxConduction = 0, maxSwitching = 0, maxTemperature = 0;
        for (int step = 0; step < total.length; step++) {
            assertEquals(total[step], conduction[step] + switching[step], Math.max(1e-6, Math.abs(total[step]) * 1e-5), "step " + step);
            maxConduction = Math.max(maxConduction, conduction[step]);
            maxSwitching = Math.max(maxSwitching, switching[step]);
            maxTemperature = Math.max(maxTemperature, temperature[step]);
        }
        assertTrue(maxConduction > 0, "the switch conducts");
        assertTrue(maxSwitching > 0, "the switch turns on or off");
        assertTrue(maxTemperature > 0, "the junction heats");
    }

    @Test
    void aPeriodicSteadyStateSearchRejectsTheThermalNetwork() throws Exception {
        var search = new gecko.core.allg.PeriodicSteadyStateSettings(1e-4, 1e-6, 10);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(ThermalFixture.model()), new PreparationRequest(
                new SolverOverrides(null, null, null, new SolverFeatures(0, false, false, search, null)), Map.of()));
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, result);
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals("INVALID_SETTING")
                && "periodicSteadyState".equals(item.parameterPath()) && item.message().contains("THERMAL")),
                rejected.report().diagnostics().toString());
    }
}
