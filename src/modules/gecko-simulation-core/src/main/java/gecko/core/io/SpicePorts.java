package gecko.core.io;

import gecko.core.io.CircuitTopology.Side;
import java.util.Arrays;
import java.util.List;

/** Original SPICE pin order and native terminal geometry. */
public enum SpicePorts {
    RES(1, 'R', two(16, 16, 16, 96)),
    IND(2, 'L', two(16, 16, 16, 96)),
    CAP(3, 'C', two(16, 0, 16, 64)),
    VOLTAGE(4, 'V', two(0, 16, 0, 96)),
    CURRENT(5, 'I', two(0, 0, 0, 80)),
    DIODE(6, 'D', two(16, 0, 16, 64)),
    SW(7, 'S', switchPorts()),
    MOSFET(28, 'M', List.of(
            new PortSpec(1, Side.INPUT, 0, new Point(48, 0), new Point(0, -2)),
            new PortSpec(2, Side.INPUT, 1, new Point(0, 48), new Point(-2, 0)),
            new PortSpec(3, Side.OUTPUT, 0, new Point(48, 96), new Point(0, 2)))),
    BJT(33, 'Q', List.of(
            new PortSpec(1, Side.INPUT, 0, new Point(64, 0), new Point(0, -2)),
            new PortSpec(2, Side.INPUT, 1, new Point(0, 48), new Point(-2, 0)),
            new PortSpec(3, Side.OUTPUT, 0, new Point(64, 96), new Point(0, 2)))),
    E(53, 'E', List.of(
            new PortSpec(1, Side.OUTPUT, 0, new Point(0, 16), new Point(0, -2)),
            new PortSpec(2, Side.OUTPUT, 1, new Point(0, 96), new Point(0, 2)),
            new PortSpec(3, Side.INPUT, 0, new Point(-48, 32), new Point(-2, -1)),
            new PortSpec(4, Side.INPUT, 1, new Point(-48, 80), new Point(-2, 1)))),
    G(54, 'G', List.of(
            new PortSpec(1, Side.OUTPUT, 0, new Point(0, 96), new Point(0, 2)),
            new PortSpec(2, Side.OUTPUT, 1, new Point(0, 16), new Point(0, -2)),
            new PortSpec(3, Side.INPUT, 0, new Point(-48, 32), new Point(-2, -1)),
            new PortSpec(4, Side.INPUT, 1, new Point(-48, 80), new Point(-2, 1))));

    public record Point(int x, int y) { }
    public record PortSpec(int order, Side side, int port, Point sourceLocalPoint, Point nativeLocalPoint) {
        public Point nativeOffset(int orientation, boolean mirrored) {
            if (nativeLocalPoint == null) throw new IllegalStateException("This source pin has no native terminal.");
            int x = mirrored ? -nativeLocalPoint.x : nativeLocalPoint.x;
            int y = nativeLocalPoint.y;
            return switch (orientation) {
                case 501 -> new Point(-x, -y);
                case 502 -> new Point(y, -x);
                case 504 -> new Point(-y, x);
                default -> new Point(x, y);
            };
        }
    }

    private final int type;
    private final char designator;
    private final List<PortSpec> ports;

    SpicePorts(int type, char designator, List<PortSpec> ports) {
        this.type = type;
        this.designator = designator;
        this.ports = ports;
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).order() != i + 1) throw new IllegalArgumentException("Invalid SPICE pin order.");
        }
    }

    public int type() { return type; }
    public char designator() { return designator; }
    public List<PortSpec> ports() { return ports; }
    public List<PortSpec> nativePorts() { return ports.stream().filter(p -> p.nativeLocalPoint() != null).toList(); }

    public static SpicePorts find(String domain, int type) {
        if (!domain.equals("LK")) return null;
        return Arrays.stream(values()).filter(value -> value.type == type).findFirst().orElse(null);
    }

    public static SpicePorts find(char designator) {
        return Arrays.stream(values()).filter(value -> value.designator == designator).findFirst().orElse(null);
    }

    private static List<PortSpec> two(int x1, int y1, int x2, int y2) {
        return List.of(new PortSpec(1, Side.INPUT, 0, new Point(x1, y1), new Point(0, -2)),
                new PortSpec(2, Side.OUTPUT, 0, new Point(x2, y2), new Point(0, 2)));
    }

    private static List<PortSpec> switchPorts() {
        var conduction = two(0, 16, 0, 96);
        return List.of(conduction.getFirst(), conduction.getLast(),
                new PortSpec(3, Side.INPUT, 1, new Point(-48, 80), null),
                new PortSpec(4, Side.INPUT, 2, new Point(-48, 32), null));
    }
}
