package gecko.rest.service;

import gecko.core.circuit.losscalculation.DetailedLossLookupTable;
import gecko.core.circuit.losscalculation.LeitverlusteMesskurve;
import gecko.core.circuit.losscalculation.SwitchingLossCurve;
import gecko.rest.model.loss.ConductionLossRequest;
import gecko.rest.model.loss.DetailedLossRequest;
import gecko.rest.model.loss.LossCurveData;
import gecko.rest.model.loss.LossResponse;
import gecko.rest.model.loss.SwitchingLossRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for calculating semiconductor power losses.
 * Uses loss calculation classes from gecko-simulation-core.
 */
@Service
public class LossCalculationService {

    /**
     * Calculate switching losses using simple voltage/energy scaling.
     *
     * Method: Scales reference energy by voltage ratio
     * Formula: E_switching = (E_on + E_off) * (V_applied / V_ref)
     *
     * @param request Switching loss calculation parameters
     * @return Loss response with switching energy
     */
    public LossResponse calculateSwitchingLoss(SwitchingLossRequest request) {
        // Voltage scaling factor
        double voltageFactor = request.voltage() / request.referenceVoltage();

        // Scale energies by voltage
        double turnOnEnergy = request.turnOnEnergy() * voltageFactor;
        double turnOffEnergy = request.turnOffEnergy() * voltageFactor;
        double totalSwitchingEnergy = turnOnEnergy + turnOffEnergy;

        return new LossResponse(
            totalSwitchingEnergy,
            totalSwitchingEnergy,
            0.0,
            "simple_switching"
        );
    }

    /**
     * Calculate conduction losses using resistance + threshold model.
     *
     * Method: P = I * (Vth + I * Ron)
     * Where:
     * - Vth = threshold voltage (diode forward voltage or MOSFET threshold)
     * - Ron = on-state resistance
     *
     * @param request Conduction loss calculation parameters
     * @return Loss response with conduction power
     */
    public LossResponse calculateConductionLoss(ConductionLossRequest request) {
        // Voltage drop: V = Vth + I * Ron
        double voltageDrop = request.thresholdVoltage() +
                            request.current() * request.onResistance();

        // Power: P = I * V
        double power = request.current() * voltageDrop;

        return new LossResponse(
            power,
            null,
            power,
            "simple_conduction"
        );
    }

    /**
     * Calculate detailed losses using temperature-dependent curve interpolation.
     *
     * Method: Bilinear interpolation from manufacturer datasheet curves
     * - Interpolates switching energy vs current at given temperature
     * - Interpolates conduction voltage vs current at given temperature
     *
     * @param request Detailed loss calculation with multiple temperature curves
     * @return Loss response with interpolated switching and conduction losses
     */
    public LossResponse calculateDetailedLoss(DetailedLossRequest request) {
        // Create SwitchingLossCurve instances from request data
        List<SwitchingLossCurve> switchingCurves = new ArrayList<>();
        for (LossCurveData curveData : request.switchingCurves()) {
            double blockingVoltage = curveData.blockingVoltage() != null ?
                                    curveData.blockingVoltage() : 600.0; // default

            SwitchingLossCurve curve = new SwitchingLossCurve(
                curveData.temperature(),
                blockingVoltage
            );
            curve.setCurveData(curveData.data());
            switchingCurves.add(curve);
        }

        // Create LeitverlusteMesskurve instances from request data
        List<LeitverlusteMesskurve> conductionCurves = new ArrayList<>();
        for (LossCurveData curveData : request.conductionCurves()) {
            LeitverlusteMesskurve curve = new LeitverlusteMesskurve(
                curveData.temperature()
            );
            curve.setCurveData(curveData.data());
            conductionCurves.add(curve);
        }

        // Create lookup tables with bilinear interpolation
        // fabric() parameters: curves list, row index (1=turn-on, 2=turn-off for switching)
        DetailedLossLookupTable onLosses = DetailedLossLookupTable.fabric(switchingCurves, 1);
        DetailedLossLookupTable offLosses = DetailedLossLookupTable.fabric(switchingCurves, 2);
        DetailedLossLookupTable conduction = DetailedLossLookupTable.fabric(conductionCurves, 1);

        // Interpolate at requested temperature/current
        // Note: For switching curves, energies are normalized per-volt in the lookup table
        // We need to multiply by blocking voltage to get actual energy
        double blockingVoltage = request.switchingCurves().get(0).blockingVoltage() != null ?
                                request.switchingCurves().get(0).blockingVoltage() : 600.0;

        double turnOnEnergyPerVolt = onLosses.getInterpolatedYValue(request.temperature(), request.current());
        double turnOffEnergyPerVolt = offLosses.getInterpolatedYValue(request.temperature(), request.current());
        double turnOnEnergy = turnOnEnergyPerVolt * blockingVoltage;
        double turnOffEnergy = turnOffEnergyPerVolt * blockingVoltage;

        // For conduction: interpolate voltage at given current
        double conductionVoltage = conduction.getInterpolatedXValue(request.temperature(), request.current());

        // Calculate losses
        double switchingEnergy = turnOnEnergy + turnOffEnergy; // Energy per switching event [J]
        double conductionPower = conductionVoltage * request.current(); // Continuous power [W]

        // Note: For proper power calculation, switching energy needs frequency
        // Here we return energy; client should multiply by switching frequency for average power
        return new LossResponse(
            switchingEnergy + conductionPower,
            switchingEnergy,
            conductionPower,
            "detailed_interpolation"
        );
    }
}
