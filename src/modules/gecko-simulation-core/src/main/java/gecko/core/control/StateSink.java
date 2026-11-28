package gecko.core.control;

/** Receives the private state values of one calculator in a fixed order. */
public interface StateSink {
    /** @param value one continuous state value, compared after scaling */
    void value(double value);

    /** @param value one discrete state value, compared exactly */
    void flag(boolean value);
}
