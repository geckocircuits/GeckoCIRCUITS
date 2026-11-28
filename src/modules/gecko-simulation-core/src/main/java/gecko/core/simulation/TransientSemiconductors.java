package gecko.core.simulation;

import gecko.core.ac.AcComponent;
import gecko.core.ac.MosfetModel;
import gecko.core.ac.ShockleyDiode;
import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.INetList;
import gecko.core.io.CircuitModel;
import gecko.core.io.importer.SpiceValue;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.ArrayList;
import java.util.List;

/**
 * Solves selected nonlinear transient devices and behavioural sources (ADR-0033) in one Newton iteration,
 * and retains only accepted charge history.
 */
final class TransientSemiconductors {
    static final class Nonconvergence extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        Nonconvergence(double time, double step, double residual) {
            super("The transient semiconductor solve did not converge at " + time + " s with step " + step + " s and residual " + residual + ".");
        }
    }

    /** What the netlist stamps for a selected device: an open branch, so the iteration owns the whole law. */
    private static final double[] DEVICE_PLACEHOLDER = {1e12, 0, 1e-6, 1e12, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private record Device(int element, int gate, ShockleyDiode diode, MosfetModel mosfet) {
        double gateVoltage(double[] potential, int source) {
            return gate < 0 ? 0 : potential[gate] - potential[source];
        }
    }

    /**
     * A behavioural source. Probe k reads {@code u[plus[k]] - u[minus[k]]}, with -1 for none. A voltage source owns
     * the branch row {@code row}; a current source has row -1 and flows from x to y through the source, as in SPICE.
     */
    private record Source(int element, int row, SpiceValue.Behaviour law, int[] plus, int[] minus) {
        SpiceValue.Behaviour.Point at(double[] potential) {
            double[] values = new double[plus.length];
            for (int k = 0; k < values.length; k++) values[k] = potential[plus[k]] - (minus[k] < 0 ? 0 : potential[minus[k]]);
            return law.at(values);
        }

        /** The stamped linear model {@code J u - offset} at a solution. */
        double linear(double[] slopes, double offset, double[] potential) {
            double value = -offset;
            for (int k = 0; k < plus.length; k++) {
                value = Math.fma(slopes[k], potential[plus[k]] - (minus[k] < 0 ? 0 : potential[minus[k]]), value);
            }
            return value;
        }
    }

    private record Point(double current, double conductance, double gateSlope, double junctionVoltage,
                         double charge, double chargeCurrent) { }
    private record Trial(double[] potential, Point[] points, SpiceValue.Behaviour.Point[] sourcePoints, double error) {
        boolean sameBranches(Trial other) {
            for (int i = 0; i < sourcePoints.length; i++) {
                if (sourcePoints[i].branches() != other.sourcePoints[i].branches()) return false;
            }
            return true;
        }
    }
    /** The linear models currently stamped into the solver, which the residual corrects to the true laws. */
    private record Stamped(double[] conductances, double[] gateSlopes, double[] offsets, double[][] sourceSlopes,
                           double[] sourceOffsets) { }
    private final List<Device> devices;
    private final List<Source> sources;
    private final double[][] placeholders;
    private final double[] charge;
    private final double[] chargeCurrent;

    private TransientSemiconductors(List<Device> devices, List<Source> sources, double[][] placeholders) {
        this.devices = List.copyOf(devices);
        this.sources = List.copyOf(sources);
        this.placeholders = placeholders;
        charge = new double[devices.size()];
        chargeCurrent = new double[devices.size()];
    }

    static TransientSemiconductors from(CircuitModel model, CircuitNetlist netlist) {
        var devices = new ArrayList<Device>();
        var sources = new ArrayList<Source>();
        double[][] placeholders = new double[netlist.getElementCount()][];
        int[] order = netlist.getNumericalNodeOrder();
        for (var component : model.getCircuitComponents()) {
            if (component.getAcComponent() == null) continue;
            var law = component.getAcComponent().law();
            int index = netlist.getElementIndexByName(component.getName());
            if (index < 0) continue;
            if (law instanceof AcComponent.ExponentialDiode diode && diode.transientLaw()) {
                if (netlist.getType(index) != CircuitTypCore.LK_D) {
                    throw new IllegalArgumentException("The exponential diode law requires a diode element.");
                }
                devices.add(new Device(index, -1, diode.evaluate(model.rootParameterMap()), null));
                placeholders[index] = DEVICE_PLACEHOLDER;
            } else if (law instanceof AcComponent.Mosfet mosfet) {
                int gate = netlist.getMosfetGateNode(index);
                if (gate < 0 || netlist.getType(index) != CircuitTypCore.LK_MOSFET) {
                    throw new IllegalArgumentException("The Level 1 MOSFET law requires an electrical gate.");
                }
                devices.add(new Device(index, order.length == 0 ? gate : order[gate], null,
                        mosfet.evaluate(model.rootParameterMap())));
                placeholders[index] = DEVICE_PLACEHOLDER;
            } else if (law instanceof AcComponent.Behavioural behavioural) {
                sources.add(source(model, netlist, component.getName(), index, behavioural));
                // A 0 V or 0 A DC source: the iteration stamps the whole law.
                placeholders[index] = new double[21];
                placeholders[index][0] = SourceType.QUELLE_DC;
            }
        }
        return devices.isEmpty() && sources.isEmpty() ? null : new TransientSemiconductors(devices, sources, placeholders);
    }

    private static Source source(CircuitModel model, CircuitNetlist netlist, String name, int element,
            AcComponent.Behavioural law) {
        CircuitTypCore type = netlist.getType(element);
        if (type != (law.current() ? CircuitTypCore.LK_I : CircuitTypCore.LK_U)) {
            throw new IllegalArgumentException("The behavioural law of " + name + " requires a "
                    + (law.current() ? "current" : "voltage") + " source element.");
        }
        var compiled = law.compile(model.rootParameterMap());
        var probes = compiled.probes();
        int[] plus = new int[probes.size()], minus = new int[probes.size()];
        int[] order = netlist.getNumericalNodeOrder();
        for (int k = 0; k < plus.length; k++) {
            var probe = probes.get(k);
            if (probe.current()) {
                plus[k] = netlist.getNodeMax() + netlist.getVoltageSourceNumber(voltageSource(netlist, name, probe.first()));
                minus[k] = -1;
            } else {
                plus[k] = numerical(order, node(model, netlist, name, probe.first()));
                minus[k] = probe.second() == null ? -1 : numerical(order, node(model, netlist, name, probe.second()));
            }
        }
        int row = law.current() ? -1 : netlist.getNodeMax() + netlist.getVoltageSourceNumber(element);
        return new Source(element, row, compiled, plus, minus);
    }

    private static int numerical(int[] order, int node) {
        return order.length == 0 ? node : order[node];
    }

    /** Resolves a SPICE node name, without regard to case, through the terminal labels of the flat circuit. */
    private static int node(CircuitModel model, CircuitNetlist netlist, String source, String label) {
        if (label.equals("0") || label.equalsIgnoreCase("gnd")) return 0;
        for (var component : model.getCircuitComponents()) {
            int element = netlist.getElementIndexByName(component.getName());
            if (element < 0) continue;
            for (String terminal : component.getTerminalXLabels()) {
                if (label.equalsIgnoreCase(terminal)) return netlist.getNodeX(element);
            }
            for (String terminal : component.getTerminalYLabels()) {
                if (label.equalsIgnoreCase(terminal)) return netlist.getNodeY(element);
            }
        }
        throw new IllegalArgumentException("The behavioural source " + source + " references the unknown node " + label + ".");
    }

    private static int voltageSource(CircuitNetlist netlist, String source, String name) {
        for (var entry : netlist.getComponentNameToIndex().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name) && netlist.getType(entry.getValue()) == CircuitTypCore.LK_U) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("The behavioural source " + source + " reads the current of " + name
                + ", which is not a voltage source.");
    }

    boolean includes(int element) {
        return placeholders[element] != null;
    }

    /** @return the parameters the netlist stamps for a selected element, or null for an unselected one */
    double[] placeholder(int element) {
        return placeholders[element];
    }

    void solve(MatrixSolver solver, INetList netlist, double width, double time) {
        solver.buildMatrixA(netlist, width, time, false);
        solver.buildVectorB(netlist, width, time, false);
        double[] rhs = solver.getb().clone();
        var stamped = new Stamped(new double[devices.size()], new double[devices.size()], new double[devices.size()],
                new double[sources.size()][], new double[sources.size()]);
        for (int i = 0; i < devices.size(); i++) {
            stamped.conductances[i] = 1 / netlist.getParameter(devices.get(i).element)[0];
        }
        for (int i = 0; i < sources.size(); i++) stamped.sourceSlopes[i] = new double[sources.get(i).plus.length];
        Trial trial;
        try {
            trial = evaluate(solver, netlist, width, rhs, stamped, solver.getPALT().clone());
        } catch (IllegalArgumentException outOfRange) {
            if (sources.isEmpty()) throw outOfRange;
            // A law that is not finite at the last solution, such as 1/v(x) from 0 V, starts from the sources set to zero.
            solver.solve();
            trial = evaluate(solver, netlist, width, rhs, stamped, solver.getP().clone());
        }
        for (int iteration = 0; iteration < 50; iteration++) {
            System.arraycopy(rhs, 0, solver.getb(), 0, rhs.length);
            for (int i = 0; i < devices.size(); i++) {
                var device = devices.get(i);
                int x = netlist.getNodeX(device.element), y = netlist.getNodeY(device.element);
                var point = trial.points[i];
                double next = point.conductance();
                stampConductance(solver, x, y, next - stamped.conductances[i]);
                stamped.conductances[i] = next;
                if (device.gate >= 0) {
                    stampGate(solver, x, y, device.gate, point.gateSlope() - stamped.gateSlopes[i]);
                    stamped.gateSlopes[i] = point.gateSlope();
                }
                double terminal = trial.potential[x] - trial.potential[y];
                double offset = Math.fma(point.conductance(), terminal,
                        Math.fma(stamped.gateSlopes[i], device.gateVoltage(trial.potential, y), -point.current()));
                stamped.offsets[i] = offset;
                solver.getb()[x] += offset;
                solver.getb()[y] -= offset;
            }
            for (int i = 0; i < sources.size(); i++) stampSource(solver, netlist, i, trial, stamped);
            solver.setMatrixChanged();
            solver.solve();
            if (trial.error <= 1e-12 && solutionConverged(netlist.getNodeMax(), trial.potential, solver.getP())) {
                System.arraycopy(trial.potential, 0, solver.getP(), 0, rhs.length);
                return;
            }
            Trial accepted = null;
            double fraction = 1;
            for (int half = 0; half < 48; half++, fraction /= 2) {
                double[] candidate = new double[rhs.length];
                for (int row = 0; row < rhs.length; row++) {
                    candidate[row] = Math.fma(fraction, solver.getP()[row] - trial.potential[row], trial.potential[row]);
                }
                try {
                    var next = evaluate(solver, netlist, width, rhs, stamped, candidate);
                    // A comparison that changes its result is a jump, not a descent direction: take the full step.
                    if (next.error < trial.error || half == 0 && !next.sameBranches(trial)) {
                        accepted = next;
                        break;
                    }
                } catch (IllegalArgumentException outOfRange) {
                    // A smaller Newton step may stay within the finite junction range.
                }
            }
            if (accepted == null) break;
            trial = accepted;
        }
        throw new Nonconvergence(time, width, trial.error);
    }

    /** Replaces the stamped linear model of one source with its tangent at the trial solution. */
    private void stampSource(MatrixSolver solver, INetList netlist, int index, Trial trial, Stamped stamped) {
        var source = sources.get(index);
        var point = trial.sourcePoints[index];
        double[] old = stamped.sourceSlopes[index], slopes = point.gradient();
        int x = netlist.getNodeX(source.element), y = netlist.getNodeY(source.element);
        for (int k = 0; k < slopes.length; k++) {
            double change = slopes[k] - old[k];
            for (int column : new int[]{source.plus[k], source.minus[k]}) {
                if (column < 0) continue;
                if (source.row >= 0) {
                    solver.addCoefficient(source.row, column, -change);
                } else {
                    solver.addCoefficient(x, column, change);
                    solver.addCoefficient(y, column, -change);
                }
                change = -change;
            }
        }
        stamped.sourceSlopes[index] = slopes;
        double offset = source.linear(slopes, 0, trial.potential) - point.value();
        stamped.sourceOffsets[index] = offset;
        if (source.row >= 0) {
            solver.getb()[source.row] -= offset;
        } else {
            solver.getb()[x] += offset;
            solver.getb()[y] -= offset;
        }
    }

    private Trial evaluate(MatrixSolver solver, INetList netlist, double width, double[] rhs, Stamped stamped,
            double[] potential) {
        double[] residual = solver.residual(potential);
        var points = new Point[devices.size()];
        for (int i = 0; i < points.length; i++) {
            var device = devices.get(i);
            int x = netlist.getNodeX(device.element), y = netlist.getNodeY(device.element);
            double terminal = potential[x] - potential[y];
            double gateVoltage = device.gateVoltage(potential, y);
            points[i] = point(i, solver, width, terminal, gateVoltage);
            double predicted = Math.fma(stamped.conductances[i], terminal,
                    Math.fma(stamped.gateSlopes[i], gateVoltage, -stamped.offsets[i]));
            double difference = points[i].current() - predicted;
            residual[x] += difference;
            residual[y] -= difference;
        }
        var sourcePoints = new SpiceValue.Behaviour.Point[sources.size()];
        for (int i = 0; i < sourcePoints.length; i++) {
            var source = sources.get(i);
            sourcePoints[i] = source.at(potential);
            double difference = sourcePoints[i].value()
                    - source.linear(stamped.sourceSlopes[i], stamped.sourceOffsets[i], potential);
            if (source.row >= 0) {
                residual[source.row] -= difference;
            } else {
                residual[netlist.getNodeX(source.element)] += difference;
                residual[netlist.getNodeY(source.element)] -= difference;
            }
        }
        // A stiff row, such as one through a 1 uOhm resistor, cannot round below a few ulps of its largest terms.
        double[] scale = solver.residualScale(potential);
        double maximum = 0;
        for (int row = netlist.usesWireReference() ? 1 : 0; row < rhs.length; row++) {
            maximum = Math.max(maximum, Math.abs(residual[row]) / Math.max(Math.max(1, Math.abs(rhs[row])), 1e-2 * scale[row]));
        }
        return new Trial(potential, points, sourcePoints, maximum);
    }

    private static boolean solutionConverged(int voltageRows, double[] previous, double[] next) {
        for (int row = 0; row < previous.length; row++) {
            double floor = row < voltageRows ? 1e-10 : 1e-12;
            if (!(Math.abs(next[row] - previous[row]) <= floor + 1e-9 * Math.abs(previous[row]))) return false;
        }
        return true;
    }

    private Point point(int index, MatrixSolver solver, double width, double terminal, double gate) {
        var device = devices.get(index);
        if (device.mosfet != null) {
            var point = device.mosfet.at(terminal, gate);
            return new Point(point.current(), point.drainSlope(), point.gateSlope(), terminal, 0, 0);
        }
        boolean trap = solver.getSolverType() == SolverType.SOLVER_TRZ;
        var point = device.diode.transientAt(terminal, (trap ? 2 : 1) / width, charge[index], trap ? chargeCurrent[index] : 0);
        return new Point(point.current(), point.conductance(), 0, point.junctionVoltage(), point.charge(), point.chargeCurrent());
    }

    void commit(MatrixSolver solver, INetList netlist, double width, double[] currents) {
        for (int i = 0; i < devices.size(); i++) {
            int element = devices.get(i).element;
            int source = netlist.getNodeY(element);
            double terminal = solver.getP()[netlist.getNodeX(element)] - solver.getP()[source];
            var point = point(i, solver, width, terminal, devices.get(i).gateVoltage(solver.getP(), source));
            currents[element] = point.current();
            charge[i] = point.charge();
            chargeCurrent[i] = point.chargeCurrent();
        }
        for (var source : sources) {
            if (source.row < 0) currents[source.element] = source.at(solver.getP()).value();
        }
    }

    double adaptiveError(MatrixSolver used, MatrixSolver companion, INetList netlist, double width, double tolerance) {
        double worst = 0;
        for (int i = 0; i < devices.size(); i++) {
            int element = devices.get(i).element, x = netlist.getNodeX(element), y = netlist.getNodeY(element);
            var device = devices.get(i);
            var primary = point(i, used, width, used.getP()[x] - used.getP()[y], device.gateVoltage(used.getP(), y));
            var alternate = point(i, companion, width, companion.getP()[x] - companion.getP()[y],
                    device.gateVoltage(companion.getP(), y));
            if (device.mosfet != null) {
                double currentScale = 1e-9 + tolerance * Math.max(Math.abs(primary.current()), Math.abs(alternate.current()));
                worst = Math.max(worst, Math.abs(primary.current() - alternate.current()) / currentScale);
                continue;
            }
            double scale = 1e-6 + tolerance * Math.max(Math.abs(primary.junctionVoltage()), Math.abs(alternate.junctionVoltage()));
            worst = Math.max(worst, Math.abs(primary.junctionVoltage() - alternate.junctionVoltage()) / scale);
            double chargeCurrentScale = 1e-9 + tolerance * Math.max(Math.abs(primary.chargeCurrent()), Math.abs(chargeCurrent[i]));
            worst = Math.max(worst, Math.abs(primary.chargeCurrent() - alternate.chargeCurrent()) / chargeCurrentScale);
        }
        return worst;
    }

    private static void stampGate(MatrixSolver solver, int drain, int source, int gate, double value) {
        solver.addCoefficient(drain, gate, value);
        solver.addCoefficient(drain, source, -value);
        solver.addCoefficient(source, gate, -value);
        solver.addCoefficient(source, source, value);
    }

    private static void stampConductance(MatrixSolver solver, int x, int y, double value) {
        solver.addCoefficient(x, x, value);
        solver.addCoefficient(y, y, value);
        solver.addCoefficient(x, y, -value);
        solver.addCoefficient(y, x, -value);
    }
}
