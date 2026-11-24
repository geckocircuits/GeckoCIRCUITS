/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.losscalculation.DetailedLossCurves;
import gecko.core.circuit.losscalculation.SemiconductorLossModel;
import gecko.core.io.CircuitModel;
import java.util.function.Supplier;

/**
 * Resolves what a saved loss source (type 41) measures and which saved loss model computes its heat.
 * Parameters follow the legacy import order: the {@code <Verluste>} tokens override the parameter array.
 */
public final class ThermalLossSources {
    private static final double DEFAULT_BLOCKING_VOLTAGE = 400.0;
    private static final int SIMPLE = 1;
    private static final int DETAILED = 2;

    /**
     * @param coupled the coupled electrical component
     * @param newModel creates a fresh loss model, because each run owns its history
     */
    public record Spec(CircuitModel.ComponentData coupled, Supplier<SemiconductorLossModel> newModel) { }

    /**
     * @param spec the resolved loss source, or null when it cannot run in the core
     * @param code the diagnostic code, or null when it runs
     * @param message the diagnostic message, or null when it runs
     */
    public record Resolution(Spec spec, String code, String message) {
        static Resolution unsupported(String code, String message) { return new Resolution(null, code, message); }
    }

    private ThermalLossSources() { }

    /**
     * @param model the circuit model
     * @param lossSource a type 41 thermal component
     * @return the spec, or the capability diagnostic that keeps the circuit on the legacy kernel
     */
    public static Resolution resolve(CircuitModel model, CircuitModel.ComponentData lossSource) {
        CircuitModel.ComponentData coupled = coupledComponent(model, lossSource);
        if (coupled == null) {
            return Resolution.unsupported("UNSUPPORTED_LOSS_COMPONENT", "The loss source has no resolvable coupled component.");
        }
        int type = coupled.getType();
        if (type == CircuitTypCore.LK_R.getTypeNumber()) {
            return new Resolution(new Spec(coupled, SemiconductorLossModel::resistive), null, null);
        }
        boolean forwardDrop = type == CircuitTypCore.LK_D.getTypeNumber() || type == CircuitTypCore.LK_THYR.getTypeNumber()
                || type == CircuitTypCore.LK_IGBT.getTypeNumber();
        if (!forwardDrop && type != CircuitTypCore.LK_S.getTypeNumber()) {
            return Resolution.unsupported("UNSUPPORTED_LOSS_COMPONENT",
                    "The core cannot compute the losses of component type " + type + ".");
        }
        int lossType = (int) Math.rint(token(coupled, "verlustTyp", SIMPLE));
        if (lossType == DETAILED) {
            long hash = (long) token(coupled, "lossFileHashValue", 0);
            var resource = model.getResources().get(hash);
            if (resource == null) {
                return Resolution.unsupported("UNSUPPORTED_LOSS_MODEL", "The DETAILED loss file is not available.");
            }
            DetailedLossCurves curves;
            try { curves = DetailedLossCurves.parse(resource.bytes()); }
            catch (IllegalArgumentException error) {
                return Resolution.unsupported("UNSUPPORTED_LOSS_MODEL", "The DETAILED loss file cannot be read: " + error.getMessage());
            }
            return new Resolution(new Spec(coupled, () -> SemiconductorLossModel.detailed(curves)), null, null);
        }
        if (lossType != SIMPLE) {
            return Resolution.unsupported("UNSUPPORTED_LOSS_MODEL", "The loss model type " + lossType + " is not supported.");
        }
        double uf = forwardDrop ? parameter(coupled, 1) : 0;
        double rOn = parameter(coupled, type == CircuitTypCore.LK_S.getTypeNumber() ? 1 : 2);
        double kOn = token(coupled, "kON", parameter(coupled, 6));
        double kOff = token(coupled, "kOFF", parameter(coupled, 7));
        double blocking = token(coupled, "uSWnorm", DEFAULT_BLOCKING_VOLTAGE);
        if (!Double.isFinite(uf) || !Double.isFinite(rOn) || !Double.isFinite(kOn) || !Double.isFinite(kOff)
                || !Double.isFinite(blocking) || blocking == 0) {
            return Resolution.unsupported("UNSUPPORTED_LOSS_MODEL", "The SIMPLE loss parameters are not finite and nonzero.");
        }
        return new Resolution(new Spec(coupled, () -> SemiconductorLossModel.simple(uf, rOn, kOn, kOff, blocking)), null, null);
    }

    private static CircuitModel.ComponentData coupledComponent(CircuitModel model, CircuitModel.ComponentData lossSource) {
        String reference = lossSource.getImportTokens().get("coupledReferenceID[]");
        if (reference != null && !reference.isBlank()) {
            try {
                long identifier = Long.parseLong(reference.trim().split("\\s+")[0]);
                for (var component : model.getCircuitComponents()) {
                    if (component.getParameters().get("uniqueObjectIdentifier") instanceof Number number
                            && number.longValue() == identifier) return component;
                }
            } catch (NumberFormatException ignored) {
                // Fall back to the saved coupled name.
            }
            return null;
        }
        String[] names = lossSource.getParameterStrings();
        if (names.length > 0) {
            for (var component : model.getCircuitComponents()) if (names[0].equals(component.getName())) return component;
        }
        return null;
    }

    private static double parameter(CircuitModel.ComponentData component, int index) {
        double[] values = component.getNumericParameters();
        return index < values.length ? values[index] : 0;
    }

    private static double token(CircuitModel.ComponentData component, String name, double fallback) {
        String text = component.getImportTokens().get(name);
        if (text == null || text.isBlank()) return fallback;
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException error) { return Double.NaN; }
    }
}
