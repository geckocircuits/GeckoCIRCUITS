package gecko.rest.service;

import gecko.core.signal.TimedSignal;
import gecko.core.signal.CharacteristicsCalculator;
import gecko.core.signal.FourierGUIless;
import gecko.core.GeckoInvalidArgumentException;
import gecko.rest.model.analysis.CharacteristicsResponse;
import gecko.rest.model.analysis.FourierResponse;
import gecko.rest.model.analysis.SignalAnalysisRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for signal analysis operations.
 * Provides characteristic analysis and Fourier decomposition for simulation signals.
 *
 * Uses classes from gecko-simulation-core for all calculations (GUI-free).
 */
@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    private final SimulationService simulationService;

    @Autowired
    public AnalysisService(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    private TimedSignal resolveInput(SignalAnalysisRequest request) {
        try {
            if (request.getData() != null && request.getData().length > 0) {
                return TimedSignal.evenlySampled(request.getData(), request.getSampleRate());
            }
            if (request.getSimulationId() != null && request.getSignalName() != null) {
                TimedSignal signal = simulationService.getTimedSignalData(request.getSimulationId(), request.getSignalName());
                if (signal == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Signal '" + request.getSignalName() + "' not found in simulation " + request.getSimulationId());
                }
                return signal.window(request.getStartTime(), request.getEndTime());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either supply 'data' with 'sampleRate', or provide 'simulationId' + 'signalName'");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Compute signal characteristics (AVG, RMS, THD, MIN, MAX, etc.).
     *
     * @param request Analysis request with raw data or simulation reference
     * @return CharacteristicsResponse with computed metrics
     * @throws ResponseStatusException on invalid request or calculation error
     */
    public CharacteristicsResponse computeCharacteristics(SignalAnalysisRequest request) {
        try {
            TimedSignal signal = resolveInput(request);
            var container = signal.toContainer();
            double duration = signal.duration();

            // Calculate characteristics using gecko-core
            try {
                CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
                    container,
                    0.0,  // start time
                    duration  // end time
                );

                // Extract metrics for signal 0
                double[] metrics = calc.getChannelCharacteristics(0);

                // Build response
                CharacteristicsResponse response = CharacteristicsResponse.of(
                    metrics,
                    signal.sampleCount(),
                    request.getSignalName()
                );

                return response;
            } catch (GeckoInvalidArgumentException e) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Characteristics calculation failed: " + e.getMessage(),
                    e
                );
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in computeCharacteristics", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Analysis failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Compute Fourier harmonic decomposition for a signal.
     *
     * @param request Analysis request with raw data or simulation reference
     * @param harmonics Number of harmonics to compute (0-based, so 10 = DC through 10th harmonic)
     * @return FourierResponse with An, Bn, Cn, Jn coefficients
     * @throws ResponseStatusException on invalid request or calculation error
     */
    public FourierResponse computeFourier(SignalAnalysisRequest request, int harmonics) {
        try {
            // Validate harmonics
            if (harmonics < 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Harmonics count must be >= 0"
                );
            }

            TimedSignal signal = resolveInput(request);
            var container = signal.toContainer();
            double duration = signal.duration();

            // Calculate base frequency
            double baseFreq = 1.0 / duration;

            // Perform Fourier analysis using gecko-core
            try {
                FourierGUIless fourier = new FourierGUIless(
                    container,
                    0.0,  // start time
                    duration,  // end time
                    harmonics
                );

                double[][][] result = fourier.doFourier();

                // Build response
                FourierResponse response = FourierResponse.of(
                    result,
                    0,  // signal index
                    baseFreq,
                    harmonics,
                    request.getSignalName()
                );

                return response;
            } catch (GeckoInvalidArgumentException e) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fourier analysis failed: " + e.getMessage(),
                    e
                );
            } catch (RuntimeException e) {
                // Catches OutOfMemory wrapped in RuntimeException
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fourier analysis failed (likely out of memory for large harmonics count): " + e.getMessage(),
                    e
                );
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in computeFourier", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Analysis failed: " + e.getMessage(),
                e
            );
        }
    }
}
