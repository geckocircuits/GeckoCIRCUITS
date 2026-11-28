package gecko.geckocircuits.allg;

import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.control.RegelBlock;
import gecko.geckocircuits.control.ReglerCISPR16;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.geckocircuits.newscope.ScopeFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/** Temporary frontend bridge for the legacy desktop kernel entry points. */
public final class LegacySimulationFrontend implements Consumer<Boolean> {
    private static final Logger LOGGER = Logger.getLogger(LegacySimulationFrontend.class.getName());
    // Only the event dispatch thread accesses these maps. Values do not retain the weak keys.
    private static final Map<RegelBlock, Object> OWNERS = new WeakHashMap<>();
    private static final Map<AbstractDataContainer, Object> RECORDING_OWNERS = new WeakHashMap<>();
    private volatile Binding binding;

    private record Target(RegelBlock block, ScopeFrame scope, AbstractDataContainer recording) { }
    private record Binding(List<Target> targets, Object generation, BooleanSupplier stillCurrent) { }

    /** Creates an unbound bridge. Call {@link #bind(NetListContainer)} before presentation updates. */
    public LegacySimulationFrontend() {
        this(() -> true);
    }

    /**
     * Creates a bridge whose initial widget updates belong to one execution request.
     *
     * @param stillCurrent identifies the request that owns widget presentation
     */
    public LegacySimulationFrontend(BooleanSupplier stillCurrent) {
        binding = new Binding(List.of(), new Object(), Objects.requireNonNull(stillCurrent));
    }

    /**
     * Reads the legacy desktop setting when the kernel initializes its matrices.
     *
     * @return the selected desktop solver
     */
    public SolverType selectedSolver() {
        return MainWindow._solverSettings.SOLVER_TYPE.getValue();
    }

    /**
     * Captures initialized targets and claims their presentation without changing widgets.
     *
     * @param netlist the originating initialized simulation netlist
     */
    public void bind(NetListContainer netlist) {
        prepare(netlist, binding.stillCurrent(), false).run();
    }

    /**
     * Prepares ownership and activation without changing Swing or waiting for the event thread.
     * Bind kernel completion after this call, then publish the execution before running the action.
     *
     * @param netlist the originating initialized simulation netlist
     * @param stillCurrent identifies this execution request
     * @param activate true to show active execution, false to claim targets only
     * @return the synchronous, ownership-checked presentation action
     */
    public Runnable prepare(NetListContainer netlist, BooleanSupplier stillCurrent, boolean activate) {
        if (!stillCurrent.getAsBoolean()) {
            return () -> { };
        }
        List<Target> targets = new ArrayList<>();
        for (RegelBlock block : netlist._nlControl._orderedControlBlocks) {
            if (block instanceof ReglerOSZI scope) {
                targets.add(new Target(block, scope._scopeFrame, null));
            } else if (block instanceof ReglerCISPR16 receiver) {
                targets.add(new Target(block, null, receiver.getZVDatenImRAM()));
            }
        }
        return prepareBinding(List.copyOf(targets), stillCurrent, activate);
    }

    private synchronized Runnable prepareBinding(List<Target> targets, BooleanSupplier stillCurrent, boolean activate) {
        if (!stillCurrent.getAsBoolean()) {
            return () -> { };
        }
        Binding next = new Binding(targets, new Object(), stillCurrent);
        binding = next;
        return () -> onEventThread(() -> {
            if (!next.stillCurrent().getAsBoolean()) {
                return;
            }
            for (Target target : next.targets()) {
                claim(target, next.generation());
            }
            if (activate) {
                deliver(next, true);
            }
        });
    }

    /**
     * Completes presentation and receiver finalization before the simulation caller returns.
     *
     * @param active true while execution is active, false when execution stops or pauses
     */
    @Override
    public void accept(Boolean active) {
        Binding origin = binding;
        if (active) {
            prepareBinding(origin.targets(), origin.stillCurrent(), true).run();
        } else {
            onEventThread(() -> deliver(origin, false));
        }
    }

    /**
     * Captures completion for the current activation before execution can be resumed or replaced.
     *
     * @return an action that finalizes only the originating activation
     */
    public Runnable captureCompletion() {
        Binding origin = binding;
        return () -> onEventThread(() -> deliver(origin, false));
    }

    private void deliver(Binding origin, boolean active) {
        if (!active) {
            for (Target target : origin.targets()) {
                if (target.recording() != null
                        && RECORDING_OWNERS.get(target.recording()) == origin.generation()) {
                    try {
                        target.recording().setContainerStatus(ContainerStatus.FINISHED);
                    } catch (RuntimeException error) {
                        LOGGER.log(Level.WARNING, "Could not finalize simulation recording", error);
                    }
                }
            }
        }
        for (Target target : origin.targets()) {
            if (!origin.stillCurrent().getAsBoolean() || OWNERS.get(target.block()) != origin.generation()) {
                continue;
            }
            try {
                BooleanSupplier owner = () -> origin.stillCurrent().getAsBoolean()
                        && OWNERS.get(target.block()) == origin.generation();
                if (target.scope() != null) {
                    target.scope().setScopeMenueEnabled(active, owner);
                }
                if (target.block() instanceof ReglerCISPR16 receiver) {
                    receiver.setSimulationPresentationActive(active, owner);
                }
            } catch (RuntimeException error) {
                LOGGER.log(Level.WARNING, "Could not update simulation presentation", error);
            }
        }
    }

    private static void claim(Target target, Object generation) {
        OWNERS.put(target.block(), generation);
        if (target.recording() != null) {
            RECORDING_OWNERS.put(target.recording(), generation);
        }
    }

    static void onEventThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        FutureTask<Void> task = new FutureTask<>(action, null);
        SwingUtilities.invokeLater(task);
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    task.get();
                    return;
                } catch (InterruptedException error) {
                    interrupted = true;
                } catch (ExecutionException error) {
                    throw new IllegalStateException("Could not update simulation presentation", error.getCause());
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}