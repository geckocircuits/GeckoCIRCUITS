package gecko.core.ac;

/** Logarithmic frequency sweep with an inclusive start. */
public record AcSweep(int pointsPerDecade, double startHz, double stopHz) {
    private static final int MAX_POINTS = 100_000;

    public AcSweep {
        if (pointsPerDecade < 1 || !Double.isFinite(startHz) || !Double.isFinite(stopHz)
                || startHz <= 0 || stopHz < startHz
                || pointsPerDecade * (Math.log10(stopHz) - Math.log10(startHz)) >= MAX_POINTS) {
            throw new IllegalArgumentException("Require a finite positive AC frequency range and at most 100,000 samples.");
        }
    }

    public double[] frequencies() {
        return frequencies(false);
    }

    /** Gives the LTspice ASC endpoint convention for an imported hierarchy. */
    public double[] frequencies(boolean exactStop) {
        double intervals = pointsPerDecade * (Math.log10(stopHz) - Math.log10(startHz));
        int count = 1 + (int) Math.floor(intervals + 8 * Math.ulp(intervals));
        double last = startHz * Math.pow(10, (double) (count - 1) / pointsPerDecade);
        boolean appendStop = exactStop && stopHz - last > 8 * Math.ulp(stopHz);
        if (appendStop && count == MAX_POINTS) throw new IllegalArgumentException("Use at most 100,000 AC samples.");
        double[] axis = new double[appendStop ? count + 1 : count];
        for (int k = 0; k < count; k++) {
            axis[k] = startHz * Math.pow(10, (double) k / pointsPerDecade);
            if (!Double.isFinite(axis[k])) throw new IllegalArgumentException("The AC frequency is nonfinite.");
        }
        if (exactStop) axis[axis.length - 1] = stopHz;
        return axis;
    }
}
