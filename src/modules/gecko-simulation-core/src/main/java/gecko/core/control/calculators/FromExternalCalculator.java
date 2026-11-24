package gecko.core.control.calculators;

import gecko.core.control.StatelessCalculation;


/** Supplies the latest external values as CONTROL outputs. */
public final class FromExternalCalculator extends AbstractControlCalculatable implements StatelessCalculation {
    private final double[] values;

    public FromExternalCalculator(int width) {
        super(0, width);
        if (width < 0) throw new IllegalArgumentException("External width must not be negative.");
        values = new double[width];
    }

    public void setValues(double[] next) {
        if (next == null || next.length != values.length) throw new IllegalArgumentException("External input width does not match.");
        System.arraycopy(next, 0, values, 0, values.length);
    }

    @Override public void berechneYOUT(double deltaT) {
        for (int i = 0; i < values.length; i++) _outputSignal[i][0] = values[i];
    }
}
