package gecko.core.ac;

/** Double-precision complex value. */
public record Complex(double real, double imaginary) {
    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);

    public Complex add(Complex b) {
        return new Complex(real + b.real, imaginary + b.imaginary);
    }

    public Complex subtract(Complex b) {
        return new Complex(real - b.real, imaginary - b.imaginary);
    }

    public Complex scale(double factor) {
        return new Complex(real * factor, imaginary * factor);
    }

    public Complex multiply(Complex b) {
        return new Complex(real * b.real - imaginary * b.imaginary, real * b.imaginary + imaginary * b.real);
    }

    public Complex divide(Complex b) {
        double divisor = Math.max(Math.abs(b.real), Math.abs(b.imaginary));
        double c = b.real / divisor, d = b.imaginary / divisor;
        double a = real / divisor, i = imaginary / divisor;
        return new Complex((a * c + i * d) / (c * c + d * d), (i * c - a * d) / (c * c + d * d));
    }

    public double abs() { return Math.hypot(real, imaginary); }
    public boolean finite() { return Double.isFinite(real) && Double.isFinite(imaginary); }

    public static Complex polar(double magnitude, double phase) {
        return new Complex(magnitude * Math.cos(phase), magnitude * Math.sin(phase));
    }

    public Complex power(Complex exponent) {
        if (exponent.equals(ZERO)) return ONE;
        if (equals(ZERO) && exponent.imaginary == 0 && exponent.real > 0) return ZERO;
        return logarithm().multiply(exponent).exponential();
    }

    public Complex logarithm() { return new Complex(Math.log(abs()), Math.atan2(imaginary, real)); }
    public Complex exponential() { return polar(Math.exp(real), imaginary); }
}
