package gecko.core.io.importer;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gecko.core.io.importer.Main.json;

/** Source geometry and native primitive identifiers recorded during ASC import. */
public record LtspiceImportReceipt(String sourceSha256, String profileId, String configurationSha256,
                                   List<Device> devices, List<Wire> wires, List<Flag> flags) {
    public LtspiceImportReceipt {
        devices = List.copyOf(devices);
        wires = List.copyOf(wires);
        flags = List.copyOf(flags);
    }

    public record Point(int x, int y) {
        String format() { return "[" + x + "," + y + "]"; }
    }

    public record Device(int sourceLine, String instance, String deviceKind, long nativeIdentifier,
                         Point origin, String transform, List<Pin> pins) {
        public Device { pins = List.copyOf(pins); }

        String format() {
            return "{\"sourceLine\":" + sourceLine + ",\"instance\":" + json(instance)
                    + ",\"deviceKind\":" + json(deviceKind) + ",\"nativeIdentifier\":" + nativeIdentifier
                    + ",\"origin\":" + origin.format() + ",\"transform\":" + json(transform)
                    + ",\"pins\":" + array(pins, Pin::format) + "}";
        }
    }

    public record Pin(int spiceOrder, Point symbol, Point transformed, Point grid, Point terminal, String side, int index) {
        String format() {
            return "{\"spiceOrder\":" + spiceOrder + ",\"symbol\":" + symbol.format()
                    + ",\"transformed\":" + transformed.format() + ",\"grid\":" + grid.format()
                    + ",\"terminal\":" + terminal.format() + ",\"side\":" + json(side) + ",\"index\":" + index + "}";
        }
    }

    public record Wire(Point start, Point end, Point gridStart, Point gridEnd) {
        String format() {
            return "{\"start\":" + start.format() + ",\"end\":" + end.format()
                    + ",\"gridStart\":" + gridStart.format() + ",\"gridEnd\":" + gridEnd.format() + "}";
        }
    }

    public record Flag(String label, Point source, Point grid) {
        String format() {
            return "{\"label\":" + json(label) + ",\"source\":" + source.format() + ",\"grid\":" + grid.format() + "}";
        }
    }

    public String format() {
        return "{\"schema\":\"gecko-ltspice-import-receipt-v1\",\"sourceSha256\":" + json(sourceSha256)
                + ",\"profileId\":" + json(profileId) + ",\"configurationSha256\":" + json(configurationSha256)
                + ",\"devices\":" + array(devices, Device::format) + ",\"wires\":" + array(wires, Wire::format)
                + ",\"flags\":" + array(flags, Flag::format) + "}";
    }

    private static <T> String array(List<T> items, Function<T, String> format) {
        return items.stream().map(format).collect(Collectors.joining(",", "[", "]"));
    }
}
