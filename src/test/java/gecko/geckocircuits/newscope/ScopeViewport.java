package gecko.geckocircuits.newscope;

/** Copies the plot axes for a desktop test capture. */
public record ScopeViewport(AxisState horizontal, AxisState left, AxisState right,
                            int startPixel, int endPixel, int topOffset, int plotBottom) {
    public static ScopeViewport read(AbstractDiagram diagram, double start, double end) {
        var horizontal = diagram._xAxis;
        return new ScopeViewport(AxisState.read(horizontal), AxisState.read(diagram._yAxis1),
                diagram.checkForY2Axis() ? AxisState.read(diagram._yAxis2) : null,
                (int) horizontal.getPixelFromValue(start), (int) horizontal.getPixelFromValue(end),
                diagram.getTopOffset(), diagram.getHeight() - diagram.getBottomOffset());
    }

    public boolean matchesTimeRange(double start, double end) {
        // setSimulationTimeBoundaries stores the requested limits as floats.
        return horizontal.low() == (float) start && horizontal.high() == (float) end;
    }

    private record AxisState(float low, float high, int x, int y, int length, AxisLinLog type) {
        private static AxisState read(Axis axis) {
            var limits = axis._axisMinMax.getLimits();
            return new AxisState(limits._yLo, limits._yHi, axis._axisOriginPixel.x, axis._axisOriginPixel.y,
                    axis.getAxisLengthPixel(), axis.getAxisType());
        }
    }
}
