package gecko.core.simulation;

import gecko.core.ac.AcComponent;
import gecko.core.ac.MosfetModel;
import gecko.core.ac.ShockleyDiode;
import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.INetList;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.ArrayList;
import java.util.List;

/** Solves selected nonlinear transient devices and retains only accepted charge history. */
final class TransientSemiconductors {
    static final class Nonconvergence extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        Nonconvergence(double time, double step, double residual) {
            super("The transient semiconductor solve did not converge at " + time + " s with step " + step + " s and residual " + residual + ".");
        }
    }

    private record Device(int element, int gate, ShockleyDiode diode, MosfetModel mosfet) {
        double gateVoltage(double[] potential, int source) {
            return gate < 0 ? 0 : potential[gate] - potential[source];
        }
    }
    private record Point(double current, double conductance, double gateSlope, double junctionVoltage,
                         double charge, double chargeCurrent) { }
    private record Trial(double[] potential, Point[] points, double error) { }
    private final List<Device> devices;
    private final boolean[] selectedElements;
    private final double[] charge;
    private final double[] chargeCurrent;

    private TransientSemiconductors(List<Device> devices, int elementCount) {
        this.devices = List.copyOf(devices);
        selectedElements = new boolean[elementCount];
        for (var device : devices) selectedElements[device.element] = true;
        charge = new double[devices.size()];
        chargeCurrent = new double[devices.size()];
    }

    static TransientSemiconductors from(CircuitModel model, CircuitNetlist netlist) {
        var devices = new ArrayList<Device>();
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
            } else if (law instanceof AcComponent.Mosfet mosfet) {
                int gate = netlist.getMosfetGateNode(index);
                if (gate < 0 || netlist.getType(index) != CircuitTypCore.LK_MOSFET) {
                    throw new IllegalArgumentException("The Level 1 MOSFET law requires an electrical gate.");
                }
                int[] order = netlist.getNumericalNodeOrder();
                devices.add(new Device(index, order.length == 0 ? gate : order[gate], null,
                        mosfet.evaluate(model.rootParameterMap())));
            }
        }
        return devices.isEmpty() ? null : new TransientSemiconductors(devices, netlist.getElementCount());
    }

    boolean includes(int element) {
        return selectedElements[element];
    }

    void solve(MatrixSolver solver, INetList netlist, double width, double time) {
        solver.buildMatrixA(netlist, width, time, false);
        solver.buildVectorB(netlist, width, time, false);
        double[] rhs = solver.getb().clone();
        double[] conductances = new double[devices.size()];
        double[] gateSlopes = new double[devices.size()];
        double[] offsets = new double[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            conductances[i] = 1 / netlist.getParameter(devices.get(i).element)[0];
        }
        var trial = evaluate(solver, netlist, width, rhs, conductances, gateSlopes, offsets, solver.getPALT().clone());
        for (int iteration = 0; iteration < 50; iteration++) {
            System.arraycopy(rhs, 0, solver.getb(), 0, rhs.length);
            for (int i = 0; i < devices.size(); i++) {
                var device = devices.get(i);
                int x = netlist.getNodeX(device.element), y = netlist.getNodeY(device.element);
                var point = trial.points[i];
                double next = point.conductance();
                stampConductance(solver, x, y, next - conductances[i]);
                conductances[i] = next;
                if (device.gate >= 0) {
                    stampGate(solver, x, y, device.gate, point.gateSlope() - gateSlopes[i]);
                    gateSlopes[i] = point.gateSlope();
                }
                double terminal = trial.potential[x] - trial.potential[y];
                double offset = Math.fma(point.conductance(), terminal,
                        Math.fma(gateSlopes[i], device.gateVoltage(trial.potential, y), -point.current()));
                offsets[i] = offset;
                solver.getb()[x] += offset;
                solver.getb()[y] -= offset;
            }
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
                    var next = evaluate(solver, netlist, width, rhs, conductances, gateSlopes, offsets, candidate);
                    if (next.error < trial.error) {
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

    private Trial evaluate(MatrixSolver solver, INetList netlist, double width, double[] rhs,
            double[] conductances, double[] gateSlopes, double[] offsets, double[] potential) {
        double[] residual = solver.residual(potential);
        var points = new Point[devices.size()];
        for (int i = 0; i < points.length; i++) {
            var device = devices.get(i);
            int x = netlist.getNodeX(device.element), y = netlist.getNodeY(device.element);
            double terminal = potential[x] - potential[y];
            double gateVoltage = device.gateVoltage(potential, y);
            points[i] = point(i, solver, width, terminal, gateVoltage);
            double predicted = Math.fma(conductances[i], terminal, Math.fma(gateSlopes[i], gateVoltage, -offsets[i]));
            double difference = points[i].current() - predicted;
            residual[x] += difference;
            residual[y] -= difference;
        }
        double maximum = 0;
        for (int row = netlist.usesWireReference() ? 1 : 0; row < rhs.length; row++) {
            maximum = Math.max(maximum, Math.abs(residual[row]) / Math.max(1, Math.abs(rhs[row])));
        }
        return new Trial(potential, points, maximum);
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
