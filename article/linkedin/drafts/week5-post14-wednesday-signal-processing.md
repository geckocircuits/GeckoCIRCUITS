# LinkedIn Post #14 (Wednesday) - Week 5

**Publishing Date:** Week 5, Wednesday 8am
**Topic:** Signal Processing (FFT, THD, CISPR16)
**Target:** Power electronics engineers interested in analysis tools
**Goal:** Show signal processing features, demonstrate GeckoCIRCUITS advantages
**Word Count:** 496 words

---

## Post Content

Circuit simulation isn't just about solving A·x = b. It's about analyzing waveforms. FFT, THD, CISPR16 - here's how GeckoCIRCUITS does real-time frequency analysis.

You've simulated your power converter. Got voltage and current waveforms. Now what?

**Time Domain → Frequency Domain**

Power electronics needs frequency analysis:
- Switch-mode converters: harmonics from PWM
- Motor drives: torque ripple from current harmonics
- EMC compliance: CISPR16 conducted emissions limits
- Power quality: THD requirements (IEEE 519)

GeckoCIRCUITS has this built-in. Let me show you.

**FFT (Fast Fourier Transform):**

```java
// From: Cispr16Fft.java (simplified)
public class Cispr16Fft {

    /**
     * Compute FFT with CISPR16-compliant windowing and averaging.
     *
     * CISPR16 is the EMC standard for conducted emissions measurement.
     * Requires:
     * - Specific frequency bins (9 kHz - 30 MHz)
     * - RMS averaging over multiple samples
     * - Quasi-peak detector simulation
     */
    public double[] computeSpectrum(double[] timeSeries, double sampleRate) {
        int N = timeSeries.length;

        // Step 1: Apply Hanning window (reduce spectral leakage)
        double[] windowed = applyHanningWindow(timeSeries);

        // Step 2: Zero-padding to next power of 2 (FFT efficiency)
        int paddedLength = nextPowerOfTwo(N);
        double[] padded = zeroPad(windowed, paddedLength);

        // Step 3: Compute FFT using Cooley-Tukey algorithm
        Complex[] spectrum = fft(padded);

        // Step 4: Convert to magnitude (dBµV for CISPR16)
        double[] magnitude = new double[paddedLength / 2];
        for (int i = 0; i < magnitude.length; i++) {
            double mag = spectrum[i].abs();
            magnitude[i] = 20 * Math.log10(mag / 1e-6);  // Convert to dBµV
        }

        return magnitude;
    }

    /**
     * Compute Total Harmonic Distortion (THD)
     *
     * THD = sqrt(sum(V_harmonic^2)) / V_fundamental
     */
    public double computeTHD(double[] spectrum, double fundamentalFreq) {
        int fundamentalBin = (int)(fundamentalFreq / binWidth);

        double fundamental = spectrum[fundamentalBin];
        double harmonicsPower = 0.0;

        // Sum power of harmonics (2f, 3f, 4f, ...)
        for (int n = 2; n <= MAX_HARMONIC; n++) {
            int harmonicBin = n * fundamentalBin;
            if (harmonicBin < spectrum.length) {
                harmonicsPower += spectrum[harmonicBin] * spectrum[harmonicBin];
            }
        }

        return Math.sqrt(harmonicsPower) / fundamental;
    }
}
```

**Why This Is Powerful:**

**EMC Compliance:**
- CISPR16 limits: 66-56 dBµV (150kHz - 30MHz)
- GeckoCIRCUITS computes spectrum in real-time
- See if your design passes **before building hardware**

**Power Quality:**
- IEEE 519 THD limits: <5% for voltage, <20% for current
- Compute THD directly from simulation
- Optimize filter design to meet standards

**Harmonic Analysis:**
- Identify which harmonics dominate
- Trace them back to switching frequency, dead-time, etc.
- Fix the root cause, not just symptoms

**Real-Time Updates (Week 1 Bonus):**

Remember Phase 1 REST API from the strategic roadmap?

```java
// WebSocket endpoint for real-time spectrum updates
@MessageMapping("/spectrum")
public void streamSpectrum(SimulationState state) {
    double[] spectrum = cispr16Fft.computeSpectrum(
        state.getVoltageWaveform(),
        state.getSampleRate()
    );

    messagingTemplate.convertAndSend("/topic/spectrum", spectrum);
}
```

Browser gets live FFT updates while simulation runs. PLECS can't do this.

**What You Can Build:**

- Automated EMC pre-compliance testing
- Parametric sweeps with THD optimization
- Filter design with real-time feedback
- Teaching tools for power quality courses

This is where GeckoCIRCUITS shines. Not just simulation - **analysis at scale**.

**Friday:**

Series finale. 5-week journey recap. Poll on what to cover next (AC analysis? Optimization? Multi-physics?).

Thank you for following along.

Source: github.com/geckocircuits/GeckoCIRCUITS
File: Cispr16Fft.java:50-200

---

**Hashtags:**
#SignalProcessing #FFT #EMC #CISPR16 #PowerQuality #THD

**CTA:** Follow for Friday series finale

**Source File References:**
- Cispr16Fft.java:50-200
- THD calculation utilities
- WebSocket spectrum streaming

**Metrics:**
- Word count: 496
- Reading time: 2.6 min
- Code snippets: 2 (FFT + THD)
- Standards: CISPR16, IEEE 519
