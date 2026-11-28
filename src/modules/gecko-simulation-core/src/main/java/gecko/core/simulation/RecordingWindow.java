package gecko.core.simulation;

/** The lower bound for retained samples on the simulation's absolute time grid. */
public record RecordingWindow(double start) {
    public boolean includes(double time) {
        return start == 0 || time >= start;
    }
}
