package gecko.core.simulation.solver;

import gecko.core.circuit.netlist.INetList;

/** Resolves source controls through explicit element references. */
final class SourceControl {
    private SourceControl() { }

    static int mode(double[] parameters) {
        return switch ((int) parameters[0]) {
            case 401 -> 0;
            case 402 -> 1;
            case 400 -> 2;
            case 396 -> 3;
            case 397 -> 4;
            case 399 -> 5;
            case 398 -> 6;
            default -> (int) parameters[0];
        };
    }

    static int target(INetList netlist, int source) {
        int target = netlist.getControlElementIndex(source);
        if (target < 0 || target >= netlist.getElementCount()) {
            throw new IllegalArgumentException("Source " + source + " requires a controlling element");
        }
        return target;
    }

    static int branch(INetList netlist, int target) {
        int branch = netlist.getVoltageSourceNumber(target);
        if (branch <= 0 || branch > netlist.getVoltageSourceMax()) {
            throw new IllegalArgumentException("The controlling element must have a branch-current equation");
        }
        return netlist.getNodeMax() + branch;
    }
}
