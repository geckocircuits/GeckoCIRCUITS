package gecko.core.ac;

import java.util.List;

/** Source identity and active constitutive law owned by one component. */
public record AcComponent(String instance, List<Integer> spiceOrders, List<String> aliases, boolean mirrored, Law law) {
    public AcComponent {
        if (instance.isBlank() || law == null) throw new IllegalArgumentException("Require an instance and its law.");
        spiceOrders = List.copyOf(spiceOrders);
        var expectedOrders = law instanceof Controlled ? List.of(1, 2, 3, 4) : List.of(1, 2);
        if (!spiceOrders.equals(expectedOrders)) throw new IllegalArgumentException("Invalid source terminal order.");
        aliases = List.copyOf(aliases);
    }

    public AcComponent(String instance, List<String> aliases, boolean mirrored, Law law) {
        this(instance, law instanceof Controlled ? List.of(1, 2, 3, 4) : List.of(1, 2), aliases, mirrored, law);
    }

    public sealed interface Law permits Passive, Inductance, Voltage, Current, Controlled { }

    public record Passive(String expression) implements Law { }
    public record Inductance(String expression, String seriesResistance) implements Law { }
    public record Voltage(String dc, String waveform, String magnitude, String phaseDegrees) implements Law { }
    public record Current(String dc, String magnitude, String phaseDegrees) implements Law { }
    public record Controlled(String expression, boolean laplace) implements Law { }
}
