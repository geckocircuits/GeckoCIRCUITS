package gecko.core.simulation;

import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.PeriodicStateSource;
import gecko.core.control.StatelessCalculation;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.control.calculators.SignalCalculatorExternalWrapper;
import gecko.core.control.calculators.SignalCalculatorImport;
import gecko.core.control.calculators.SparseMatrixCalculator;
import gecko.core.control.calculators.TimeCalculator;
import gecko.core.control.calculators.SignalCalculatorRandom;
import gecko.core.control.calculators.SignalCalculatorSinus;
import java.util.ArrayList;
import java.util.List;

/** Checks that a circuit can reach a periodic state of the requested period, before any run state exists. */
final class PeriodicSteadyStateValidation {
    private static final double WHOLE = 1e-9;

    private PeriodicSteadyStateValidation() { }

    /**
     * Checks the circuit parts of the settings: sine sources and the state export of every CONTROL block.
     *
     * @param period the simulated period in seconds
     * @param netlist the electrical netlist, or null
     * @param calculators the assembled CONTROL calculators
     * @return one issue per violation, with its parameter path
     */
    static List<SimulationTiming.SettingIssue> circuitIssues(double period, CircuitNetlist netlist,
            List<AbstractControlCalculatable> calculators) {
        var issues = new ArrayList<SimulationTiming.SettingIssue>();
        if (netlist != null) {
            for (int element = 0; element < netlist.getElementCount(); element++) {
                CircuitTypCore type = netlist.getType(element);
                // Every source family that MatrixSolver stamps with an absolute-time sine.
                if (type != CircuitTypCore.LK_U && type != CircuitTypCore.LK_I && type != CircuitTypCore.TH_FLOW
                        && type != CircuitTypCore.TH_TEMP && type != CircuitTypCore.REL_MMF) continue;
                double[] parameters = netlist.getParameter(element);
                int kind = (int) parameters[0];
                if ((kind == SourceType.QUELLE_SIN || kind == SourceType.QUELLE_SIN_NEW) && !wholeCycles(period, parameters[2])) {
                    issues.add(new SimulationTiming.SettingIssue("periodicSteadyState.period",
                            "Electrical sine source " + element + " does not complete whole cycles in the period."));
                }
            }
        }
        for (int index = 0; index < calculators.size(); index++) {
            var calculator = calculators.get(index);
            String name = calculator.getClass().getSimpleName() + "#" + index;
            if (calculator instanceof SignalCalculatorRandom) {
                issues.add(unsupported(name, "draws from a hidden random generator, so its state cannot repeat"));
                continue;
            }
            if (calculator instanceof SparseMatrixCalculator) {
                issues.add(unsupported(name, "computes its phase from absolute time and an input frequency, which is not supported"));
                continue;
            }
            if (calculator instanceof SignalCalculatorExternalWrapper wrapper
                    && wrapper.wrapped() instanceof SignalCalculatorSinus) {
                issues.add(unsupported(name, "is a sine with an input frequency, so its absolute-time phase is not proven periodic"));
                continue;
            }
            if (calculator instanceof SignalCalculatorSinus sine && !wholeCycles(period, sine.frequency())) {
                issues.add(new SimulationTiming.SettingIssue("periodicSteadyState.period",
                        "CONTROL sine source " + name + " does not complete whole cycles in the period."));
            }
            if (calculator instanceof SignalCalculatorImport table && !table.repeatsEvery(period)) {
                issues.add(new SimulationTiming.SettingIssue("periodicSteadyState.period",
                        "CONTROL import " + name + " does not repeat in the period. It must start at time zero, and the period"
                                + " must be a whole multiple of its duration unless the table is constant."));
            }
            if (calculator instanceof TimeCalculator && consumedByLogic(calculators, calculator)) {
                issues.add(unsupported(name, "feeds another CONTROL block, so absolute time would act as a hidden state"));
            }
            if (!(calculator instanceof PeriodicStateSource) && !(calculator instanceof StatelessCalculation)) {
                issues.add(new SimulationTiming.SettingIssue("periodicSteadyState",
                        "CONTROL block " + name + " has no periodic state export, so convergence cannot be checked."));
            }
        }
        return issues;
    }

    private static SimulationTiming.SettingIssue unsupported(String name, String why) {
        return new SimulationTiming.SettingIssue("periodicSteadyState", "CONTROL block " + name + " " + why + ".");
    }

    /** A display sink reads the time output without acting on it. Every other reader is control logic. */
    private static boolean consumedByLogic(List<AbstractControlCalculatable> calculators, AbstractControlCalculatable time) {
        for (var other : calculators) {
            if (other == time || other instanceof NothingToDoCalculator) continue;
            for (double[] input : other._inputSignal) if (input == time._outputSignal[0]) return true;
        }
        return false;
    }

    /** A fixed frequency of exactly zero is constant. Any other must complete a whole, nonzero number of cycles. */
    private static boolean wholeCycles(double period, double frequency) {
        if (frequency == 0) return true;
        double cycles = period * frequency;
        return Double.isFinite(cycles) && Math.abs(Math.rint(cycles)) >= 1 && Math.abs(cycles - Math.rint(cycles)) <= WHOLE;
    }
}
