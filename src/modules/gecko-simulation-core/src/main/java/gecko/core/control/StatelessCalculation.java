package gecko.core.control;

/**
 * Marks a calculator whose next output depends only on its inputs, on absolute time, and on its own output signal.
 * The periodic steady-state search then needs no private state from it. A calculator with other state
 * implements {@link PeriodicStateSource} instead.
 */
public interface StatelessCalculation { }
