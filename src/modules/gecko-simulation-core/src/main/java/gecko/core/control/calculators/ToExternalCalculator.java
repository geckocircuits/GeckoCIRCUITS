package gecko.core.control.calculators;

import gecko.core.control.StatelessCalculation;

/** Captures CONTROL inputs for an external consumer. */
public final class ToExternalCalculator extends AbstractControlCalculatable implements StatelessCalculation {
    private final double[] values;

    public ToExternalCalculator(int width) {
        super(width, 0);
        if (width < 0) throw new IllegalArgumentException("External width must not be negative.");
        values = new double[width];
    }

    @Override public void berechneYOUT(double deltaT) {
        for (int i = 0; i < values.length; i++) values[i] = _inputSignal[i][0];
    }

    public double[] values() { return values.clone(); }
}
