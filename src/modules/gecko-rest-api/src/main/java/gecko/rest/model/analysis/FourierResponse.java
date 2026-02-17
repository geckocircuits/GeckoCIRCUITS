package gecko.rest.model.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Fourier harmonic decomposition result.
 *
 * <p>For each signal, provides cosine (An), sine (Bn), amplitude (Cn), and phase (Jn)
 * coefficients for harmonics 0 through N.</p>
 */
@Schema(description = "Fourier harmonic analysis result")
public class FourierResponse {

    @Schema(description = "Base frequency [Hz]", example = "50.0")
    private double baseFrequency;

    @Schema(description = "Number of harmonics computed", example = "10")
    private int harmonics;

    @Schema(description = "Signal name (if derived from simulation)")
    private String signalName;

    @Schema(description = "Cosine coefficients An per harmonic (0=DC, 1=fundamental, 2=2nd, ...)")
    private double[] anCoefficients;

    @Schema(description = "Sine coefficients Bn per harmonic")
    private double[] bnCoefficients;

    @Schema(description = "Amplitude Cn = sqrt(An^2 + Bn^2) per harmonic")
    private double[] cnAmplitudes;

    @Schema(description = "Phase Jn = atan2(Bn, An) in radians per harmonic")
    private double[] jnPhases;

    @Schema(description = "DC component (harmonic 0 amplitude)", example = "0.5")
    private double dcComponent;

    @Schema(description = "Fundamental amplitude (harmonic 1 Cn)", example = "1.0")
    private double fundamentalAmplitude;

    @Schema(description = "Fundamental phase in degrees", example = "0.0")
    private double fundamentalPhaseDegrees;

    public static FourierResponse of(double[][][] result, int signalIdx, double baseFrequency,
                                      int harmonics, String signalName) {
        FourierResponse r = new FourierResponse();
        r.baseFrequency = baseFrequency;
        r.harmonics = harmonics;
        r.signalName = signalName;

        int len = harmonics + 1;
        r.anCoefficients = new double[len];
        r.bnCoefficients = new double[len];
        r.cnAmplitudes = new double[len];
        r.jnPhases = new double[len];

        for (int h = 0; h < len; h++) {
            r.anCoefficients[h] = result[0][signalIdx][h];
            r.bnCoefficients[h] = result[1][signalIdx][h];
            r.cnAmplitudes[h] = result[2][signalIdx][h];
            r.jnPhases[h] = result[3][signalIdx][h];
        }

        r.dcComponent = r.cnAmplitudes[0];
        if (len > 1) {
            r.fundamentalAmplitude = r.cnAmplitudes[1];
            r.fundamentalPhaseDegrees = Math.toDegrees(r.jnPhases[1]);
        }
        return r;
    }

    // getters
    public double getBaseFrequency() { return baseFrequency; }
    public void setBaseFrequency(double baseFrequency) { this.baseFrequency = baseFrequency; }
    public int getHarmonics() { return harmonics; }
    public void setHarmonics(int harmonics) { this.harmonics = harmonics; }
    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }
    public double[] getAnCoefficients() { return anCoefficients; }
    public void setAnCoefficients(double[] anCoefficients) { this.anCoefficients = anCoefficients; }
    public double[] getBnCoefficients() { return bnCoefficients; }
    public void setBnCoefficients(double[] bnCoefficients) { this.bnCoefficients = bnCoefficients; }
    public double[] getCnAmplitudes() { return cnAmplitudes; }
    public void setCnAmplitudes(double[] cnAmplitudes) { this.cnAmplitudes = cnAmplitudes; }
    public double[] getJnPhases() { return jnPhases; }
    public void setJnPhases(double[] jnPhases) { this.jnPhases = jnPhases; }
    public double getDcComponent() { return dcComponent; }
    public void setDcComponent(double dcComponent) { this.dcComponent = dcComponent; }
    public double getFundamentalAmplitude() { return fundamentalAmplitude; }
    public void setFundamentalAmplitude(double fundamentalAmplitude) { this.fundamentalAmplitude = fundamentalAmplitude; }
    public double getFundamentalPhaseDegrees() { return fundamentalPhaseDegrees; }
    public void setFundamentalPhaseDegrees(double fundamentalPhaseDegrees) { this.fundamentalPhaseDegrees = fundamentalPhaseDegrees; }
}
