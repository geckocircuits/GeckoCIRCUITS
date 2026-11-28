package gecko.core.simulation;

import gecko.core.control.StateSink;
import java.util.Arrays;

/**
 * Compares the full periodic state at the end of each period. Each committed step exports every value
 * that the next step reads into {@link #sink()}. At each period boundary the monitor scales the change
 * inside each unit class and applies the contraction-corrected test of ADR-0017.
 * Discrete values compare exactly and any mismatch blocks a pass.
 */
final class PeriodicStateMonitor {
    /** The unit class of a state value. Values of different classes are never scaled together. */
    enum Unit {
        VOLT(1e-6), AMPERE(1e-9), CONTROL(1e-9), TIME(1e-12), DISCRETE(0);
        final double floor;
        Unit(double floor) { this.floor = floor; }
    }

    /** What the search does next. */
    enum Outcome { CONTINUE, CONVERGED, EXHAUSTED }

    private static final double RELATIVE_FLOOR = 1e-3;
    private static final int REQUIRED_PASSES = 2;
    private final int stepsPerPeriod;
    private final int maxPeriods;
    private final double tolerance;
    private double[] values = new double[64];
    private double[] reference = new double[64];
    private double[] peak = new double[64];
    private Unit[] units = new Unit[64];
    private String[] labels = new String[64];
    private int size = -1;
    private int cursor;
    private Unit unit = Unit.CONTROL;
    private String group = "";
    private int groupIndex;
    private boolean haveReference;
    private double previousResidual = Double.NaN;
    private int passes;
    private SteadyStateReport report = new SteadyStateReport(false, 0, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY, "", 0);

    PeriodicStateMonitor(double tolerance, int maxPeriods, int stepsPerPeriod) {
        this.tolerance = tolerance;
        this.maxPeriods = maxPeriods;
        this.stepsPerPeriod = stepsPerPeriod;
    }

    /** The sink of the state being exported. Call {@link #begin()} first, then {@link #commit} after the export. */
    final class Sink implements StateSink {
        /** Starts a group of values with one unit class and a description used in the worst-state report. */
        void group(Unit unit, String description) {
            PeriodicStateMonitor.this.unit = unit;
            group = description;
            groupIndex = 0;
        }

        /** A continuous value in the unit of the current group. A discrete group compares it exactly. */
        @Override public void value(double value) { put(value, unit); }

        /** A boolean, compared exactly whatever the group. */
        @Override public void flag(boolean value) { put(value ? 1 : 0, Unit.DISCRETE); }
    }

    private final Sink sink = new Sink();

    Sink sink() { return sink; }

    /** Starts the export of one committed step. */
    void begin() { cursor = 0; }

    private void put(double value, Unit valueUnit) {
        if (size >= 0 && cursor >= size) throw new IllegalStateException("The exported state changed size between steps.");
        if (cursor == values.length) grow();
        if (size < 0) {
            units[cursor] = valueUnit;
            labels[cursor] = group + "[" + groupIndex + "]";
        } else if (units[cursor] != valueUnit) {
            throw new IllegalStateException("The exported state changed its layout between steps.");
        }
        values[cursor++] = value;
        groupIndex++;
    }

    private void grow() {
        int length = values.length * 2;
        values = Arrays.copyOf(values, length);
        reference = Arrays.copyOf(reference, length);
        peak = Arrays.copyOf(peak, length);
        units = Arrays.copyOf(units, length);
        labels = Arrays.copyOf(labels, length);
    }

    /**
     * Ends the export of the step with the given committed count and decides the next action.
     *
     * @param committedSteps the number of steps committed so far, including this one
     * @param time the time of the step that was just committed
     * @return what the search does next
     */
    Outcome commit(int committedSteps, double time) {
        if (size < 0) size = cursor;
        else if (cursor != size) throw new IllegalStateException("The exported state changed size between steps.");
        for (int index = 0; index < size; index++) {
            if (units[index] != Unit.DISCRETE) peak[index] = Math.max(peak[index], Math.abs(values[index]));
        }
        if (committedSteps % stepsPerPeriod != 0) return Outcome.CONTINUE;
        int period = committedSteps / stepsPerPeriod;
        boolean pass = false;
        double residual = Double.POSITIVE_INFINITY;
        double estimate = Double.POSITIVE_INFINITY;
        String worst = "";
        if (haveReference) {
            double[] classPeak = new double[Unit.values().length];
            for (int index = 0; index < size; index++) {
                classPeak[units[index].ordinal()] = Math.max(classPeak[units[index].ordinal()], peak[index]);
            }
            residual = 0;
            int worstIndex = 0;
            for (int index = 0; index < size; index++) {
                double change = scaledChange(index, classPeak);
                if (!(change <= residual)) { residual = change; worstIndex = index; }
            }
            worst = labels[worstIndex];
            double ratio = Double.isFinite(previousResidual) && previousResidual > 0 ? residual / previousResidual : Double.POSITIVE_INFINITY;
            if (residual == 0) estimate = 0;
            else if (ratio < 1) estimate = residual * ratio / (1 - ratio);
            pass = residual <= tolerance && (residual == 0 || (ratio < 1 && estimate <= tolerance));
            previousResidual = residual;
        }
        passes = pass ? passes + 1 : 0;
        System.arraycopy(values, 0, reference, 0, size);
        Arrays.fill(peak, 0, size, 0);
        haveReference = true;
        boolean converged = passes >= REQUIRED_PASSES;
        report = new SteadyStateReport(converged, period, residual, estimate, worst, time);
        if (converged) return Outcome.CONVERGED;
        return period >= maxPeriods ? Outcome.EXHAUSTED : Outcome.CONTINUE;
    }

    private double scaledChange(int index, double[] classPeak) {
        double now = values[index];
        double before = reference[index];
        if (units[index] == Unit.DISCRETE) return now == before ? 0 : Double.POSITIVE_INFINITY;
        if (!Double.isFinite(now) || !Double.isFinite(before)) return Double.POSITIVE_INFINITY;
        Unit unitClass = units[index];
        double scale = Math.max(peak[index], Math.max(RELATIVE_FLOOR * classPeak[unitClass.ordinal()], unitClass.floor));
        return Math.abs(now - before) / scale;
    }

    /** @return the latest report, with converged false until two consecutive periods pass */
    SteadyStateReport report() { return report; }
}
