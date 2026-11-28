package gecko.core.io;

import gecko.core.io.importer.SpiceValue;
import java.util.Map;

/** Source expressions for native numeric slots in a hierarchy. */
public final class ScalarParameterBindings {
    private ScalarParameterBindings() { }

    public static void validateSlots(int type, double[] values, Map<Integer, String> bindings) {
        for (var binding : bindings.entrySet()) {
            int slot = binding.getKey();
            var component = SpicePorts.find("LK", type);
            boolean supported = component != null && switch (component) {
                case RES -> slot == 0;
                case CAP, IND -> slot == 0 || slot == 1;
                case VOLTAGE, CURRENT -> slot == 1 && values.length > 0 && values[0] == 401;
                default -> false;
            };
            if (!supported || slot >= values.length || binding.getValue().isBlank()) {
                throw new IllegalArgumentException("Invalid native scalar parameter slot " + slot + ".");
            }
        }
    }

    public static double[] values(int type, double[] saved, Map<Integer, String> bindings, Map<String, String> scope) {
        if (bindings.isEmpty()) return saved;
        validateSlots(type, saved, bindings);
        double[] values = saved.clone();
        bindings.forEach((slot, expression) -> {
            double value = SpiceValue.evaluate(expression, scope);
            if (slot == 0 && value <= 0) {
                throw new IllegalArgumentException("Require a positive passive component value.");
            }
            values[slot] = value;
        });
        return values;
    }
}
