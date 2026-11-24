package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/** Runs the saved THERMAL network through the core step, like ADR-0023 describes. */
class ThermalNetworkTest {
    private static final double DT = 1e-3;

    private static String thermal(int type, String name, long id, String from, String to, String parameters, String extra) {
        return "eTH\n<ElementTHERM>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\nlabelAnfangsKnoten[] /" + from
                + "\nlabelEndKnoten[] /" + to + "\n" + extra + "<" + (char) 92 + "ElementTHERM>\n";
    }

    private static String electrical(int type, String name, long id, String from, String to, String parameters) {
        return "e\n<ElementLK>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\nlabelAnfangsKnoten[] /" + from
                + "\nlabelEndKnoten[] /" + to + "\n<" + (char) 92 + "ElementLK>\n";
    }

    private static CircuitModel parse(String blocks) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader("dt 0.001\ntDURATION 1\n" + blocks)), "thermal.ipes");
    }

    private static final String ZERO = "0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
    private static final String DC_SOURCE = "401.0 %s 50.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.0 1.0 -1.7976931348623157E308 1.7976931348623157E308 0.0";

    /** Heat source P into node a, Rth and Cth from a to the ground reference. */
    private static String rcFixture(double heat, double resistance, double capacitance) {
        return thermal(44, "P", 1, "0", "a", "401.0 " + heat + " 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "")
                + thermal(46, "Rth", 2, "a", "0", resistance + " " + ZERO, "")
                + thermal(47, "Cth", 3, "a", "0", capacitance + " 0.0 0.0 0.0 0.0 0.0 " + capacitance + " 0.0 0.0 0.0 0.0", "");
    }

    private static double temperature(ThermalNetwork network, String element) {
        CircuitNetlist netlist = network.netlist();
        int index = netlist.getElementIndexByName(element);
        double[] potentials = netlist.getLastNodeVoltagesRef();
        return potentials[netlist.getNodeX(index)] - potentials[netlist.getNodeY(index)];
    }

    /** The loss source runs from the thermal zero to its output, so the junction is the reverse potential drop. */
    private static double junction(ThermalNetwork network) {
        return -temperature(network, "Pv#Rth");
    }

    @Test
    void anRcNetworkFollowsTheAnalyticStepResponseForBothSolvers() throws Exception {
        double heat = 5, resistance = 2, capacitance = 0.5, tau = resistance * capacitance;
        for (SolverType solver : new SolverType[]{SolverType.SOLVER_BE, SolverType.SOLVER_TRZ}) {
            var network = ThermalNetwork.build(parse(rcFixture(heat, resistance, capacitance)), null, solver);
            assertNotNull(network);
            double worst = 0;
            for (int step = 0; step < 2000; step++) {
                double time = step * DT;
                network.advance(DT, time, null);
                if (step >= 1) {
                    double expected = heat * resistance * (1 - Math.exp(-(time + DT) / tau));
                    worst = Math.max(worst, Math.abs(temperature(network, "Rth") - expected) / (heat * resistance));
                }
            }
            assertEquals(2000, network.committedSteps());
            // The trapezoid still carries its start error of dt/(2 tau) from the stepped heat source.
            assertTrue(worst < (solver == SolverType.SOLVER_BE ? 2e-3 : 6e-4), solver + " deviation " + worst);

            assertEquals(heat * resistance * (1 - Math.exp(-2.0 / tau)), temperature(network, "Rth"), 0.05);
        }
    }

    @Test
    void aLossSourceHeatsAtTheNextStepWithTheCommittedLoss() throws Exception {
        // U = 10 V across R = 5 Ohm dissipates 20 W. The thermal zero is tied to the reference by an ambient element.
        String blocks = electrical(4, "U", 10, "in", "0", String.format(DC_SOURCE, "10.0"))
                + electrical(1, "R", 77, "in", "0", "5.0 " + ZERO)
                + thermal(41, "Pv", 20, "", "tj", "", "coupledReferenceID[] 77 \n")
                + thermal(46, "Rth", 21, "tj", "0", "2.0 " + ZERO, "")
                + thermal(47, "Cth", 22, "tj", "0", "0.5 0.0 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0", "")
                + thermal(48, "TREF", 23, "0", "0", "0.0 " + ZERO, "");
        var model = parse(blocks);
        var circuit = NetlistBuilder.buildFromCircuitModel(model);
        var electricalStep = new ElectricalTimeStep(circuit, SolverType.SOLVER_BE);
        var network = ThermalNetwork.build(model, circuit, SolverType.SOLVER_BE);
        assertNotNull(network);
        circuit.setSimulationTime(0);
        electricalStep.advance(DT, 0, null);
        network.advance(DT, 0, circuit);
        // The first thermal solve stamps no heat. Its losses are computed after it.
        assertEquals(0, junction(network), 0);
        assertEquals(20, network.loss("Pv").total(), 1e-9);
        double afterFirst = junction(network);
        circuit.setSimulationTime(DT);
        electricalStep.advance(DT, DT, null);
        network.advance(DT, DT, circuit);
        assertTrue(junction(network) > afterFirst, "the heat of step 0 enters step 1");
        double previous = 0;
        for (int step = 2; step < 12000; step++) {
            electricalStep.advance(DT, step * DT, null);
            network.advance(DT, step * DT, circuit);
            previous = junction(network);
        }
        double steady = 20 * (1.0 / (1.0 / 2.0 + 1.0 / 10000.0));
        assertEquals(steady, previous, steady * 0.01);
    }

    @Test
    void aNetworkWithoutThermalElementsIsNotCreated() throws Exception {
        var model = parse(electrical(1, "R", 1, "a", "0", "1.0 " + ZERO));
        assertNull(ThermalNetwork.build(model, NetlistBuilder.buildFromCircuitModel(model), SolverType.SOLVER_BE));
    }

    @Test
    void anUnresolvedLossCouplingFailsInsteadOfFabricatingZeroHeat() throws Exception {
        String blocks = electrical(1, "R", 77, "in", "0", "5.0 " + ZERO)
                + thermal(41, "Pv", 20, "", "tj", "", "coupledReferenceID[] 999 \n")
                + thermal(46, "Rth", 21, "tj", "0", "2.0 " + ZERO, "")
                + thermal(48, "TREF", 23, "0", "0", "0.0 " + ZERO, "");
        var model = parse(blocks);
        var error = assertThrows(IllegalArgumentException.class,
                () -> ThermalNetwork.build(model, NetlistBuilder.buildFromCircuitModel(model), SolverType.SOLVER_BE));
        assertTrue(error.getMessage().contains("Pv"), error.getMessage());
    }
}
