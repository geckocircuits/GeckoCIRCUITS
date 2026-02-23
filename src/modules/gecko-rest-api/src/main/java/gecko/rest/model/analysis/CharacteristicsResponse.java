package gecko.rest.model.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Signal characteristic metrics computed from a time-series signal.
 *
 * <p>All values are computed over the requested time window.</p>
 */
@Schema(description = "Signal characteristic metrics")
public class CharacteristicsResponse {

    @Schema(description = "Average (DC) value", example = "0.0")
    private double average;

    @Schema(description = "Root Mean Square value", example = "0.707")
    private double rms;

    @Schema(description = "Total Harmonic Distortion [%]", example = "5.2")
    private double thd;

    @Schema(description = "Minimum value", example = "-1.0")
    private double min;

    @Schema(description = "Maximum value", example = "1.0")
    private double max;

    @Schema(description = "Peak-to-peak amplitude", example = "2.0")
    private double peakToPeak;

    @Schema(description = "Ripple factor", example = "0.01")
    private double ripple;

    @Schema(description = "Klirr (distortion factor)", example = "0.05")
    private double klirr;

    @Schema(description = "Shape factor (RMS / rectified average)", example = "1.11")
    private double shapeFactor;

    @Schema(description = "Number of samples analysed")
    private int sampleCount;

    @Schema(description = "Signal name (if derived from simulation)")
    private String signalName;

    // factory
    public static CharacteristicsResponse of(double[] metrics, int sampleCount, String signalName) {
        CharacteristicsResponse r = new CharacteristicsResponse();
        // metrics order from CharacteristicsCalculator:
        // AVG=0, RMS=1, THD=2, MIN=3, MAX=4, RIPPLE=5, KLIRR=6, SHAPE=7, PEAK_PEAK=8
        r.average = metrics[0];
        r.rms = metrics[1];
        r.thd = metrics[2];
        r.min = metrics[3];
        r.max = metrics[4];
        r.ripple = metrics[5];
        r.klirr = metrics[6];
        r.shapeFactor = metrics[7];
        r.peakToPeak = metrics[8];
        r.sampleCount = sampleCount;
        r.signalName = signalName;
        return r;
    }

    // getters / setters
    public double getAverage() { return average; }
    public void setAverage(double average) { this.average = average; }
    public double getRms() { return rms; }
    public void setRms(double rms) { this.rms = rms; }
    public double getThd() { return thd; }
    public void setThd(double thd) { this.thd = thd; }
    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }
    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
    public double getPeakToPeak() { return peakToPeak; }
    public void setPeakToPeak(double peakToPeak) { this.peakToPeak = peakToPeak; }
    public double getRipple() { return ripple; }
    public void setRipple(double ripple) { this.ripple = ripple; }
    public double getKlirr() { return klirr; }
    public void setKlirr(double klirr) { this.klirr = klirr; }
    public double getShapeFactor() { return shapeFactor; }
    public void setShapeFactor(double shapeFactor) { this.shapeFactor = shapeFactor; }
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }
}
