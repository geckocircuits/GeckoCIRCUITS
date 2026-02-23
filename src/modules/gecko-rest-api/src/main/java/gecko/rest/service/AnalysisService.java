package gecko.rest.service;

import gecko.core.datacontainer.DataContainerSimple;
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

    /**
     * Resolve signal data from either raw input or server-side simulation storage.
     *
     * @param request The analysis request containing data or simulation reference
     * @return double[] signal data
     * @throws ResponseStatusException if data cannot be resolved
     */
    private double[] resolveData(SignalAnalysisRequest request) {
        // Priority: raw data
        if (request.getData() != null && request.getData().length > 0) {
            return request.getData();
        }

        // Fallback: server-side simulation lookup
        if (request.getSimulationId() != null && request.getSignalName() != null) {
            double[] data = simulationService.getSignalData(request.getSimulationId(), request.getSignalName());
            if (data == null || data.length == 0) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Signal '" + request.getSignalName() + "' not found in simulation " + request.getSimulationId()
                );
            }
            return data;
        }

        // No data source
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Either supply 'data' with 'sampleRate', or provide 'simulationId' + 'signalName'"
        );
    }

    /**
     * Build a DataContainerSimple from raw signal data with constant sample rate.
     *
     * @param data signal values
     * @param sampleRate samples per second
     * @return DataContainerSimple initialized with time series
     */
    private DataContainerSimple buildContainer(double[] data, double sampleRate) {
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, data.length);
        double dt = 1.0 / sampleRate;
        for (int i = 0; i < data.length; i++) {
            container.insertValuesAtEnd(new float[]{(float) data[i]}, i * dt);
        }
        return container;
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
            // Resolve data source
            double[] data = resolveData(request);

            if (data == null || data.length < 2) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient data points for analysis (minimum 2 required)"
                );
            }

            // Determine sample rate
            Double sampleRate = request.getSampleRate();
            if (sampleRate == null || sampleRate <= 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid sampleRate > 0 is required for raw data analysis"
                );
            }

            // Build data container
            DataContainerSimple container = buildContainer(data, sampleRate);

            // Calculate time range
            double duration = (data.length - 1) / sampleRate;

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
                    data.length,
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

            // Resolve data source
            double[] data = resolveData(request);

            if (data == null || data.length < 2) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient data points for Fourier analysis (minimum 2 required)"
                );
            }

            // Determine sample rate
            Double sampleRate = request.getSampleRate();
            if (sampleRate == null || sampleRate <= 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid sampleRate > 0 is required for raw data analysis"
                );
            }

            // Build data container
            DataContainerSimple container = buildContainer(data, sampleRate);

            // Calculate time range
            double duration = (data.length - 1) / sampleRate;

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
