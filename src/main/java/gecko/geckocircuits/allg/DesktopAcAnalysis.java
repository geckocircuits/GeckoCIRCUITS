package gecko.geckocircuits.allg;

import gecko.core.ac.AcResult;
import gecko.core.ac.Complex;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.circuit.SolverSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;

/** Captures and presents AC runs from the desktop document. */
final class DesktopAcAnalysis {
    private record Trace(String label, String units, List<Complex> values) {
        @Override public String toString() { return label; }
    }

    private final MainWindow window;
    // Only the event dispatch thread changes ownership.
    private volatile SwingWorker<AcResult, Void> active;
    private volatile JDialog resultWindow;

    DesktopAcAnalysis(MainWindow window) { this.window = window; }

    static PreparationResult prepare(SchematicEditor2 editor, SolverSettings settings) throws Exception {
        var scripts = MainWindow._scripter;
        var sources = new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                scripts.getDeclarationCode(), scripts.getExtraFilesHashes());
        var snapshot = new EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(), settings,
                MainWindow.optimizerParameterData, MainWindow._fileManager, sources, MainWindow.getOpenFileName(),
                editor.getAcSettings(), editor.getAcProvenance(), editor.getConversionIssues(), editor.getHierarchy(),
                editor.getRootParameterDefinitions());
        return new CircuitPreparation().prepare(snapshot, PreparationRequest.defaults());
    }

    void start(SchematicEditor2 editor, SolverSettings settings, boolean async) throws Exception {
        if (!async) throw new IllegalArgumentException("Use the AC result interface for this circuit.");
        onEdt(() -> {
            begin(editor, settings);
            return null;
        });
    }

    private void begin(SchematicEditor2 editor, SolverSettings settings) throws Exception {
        invalidateRun();
        var preparation = prepare(editor, settings);
        if (!(preparation instanceof PreparationResult.Ready ready)) {
            throw new IllegalArgumentException(preparation.report().diagnostics().stream()
                    .map(diagnostic -> diagnostic.message()).collect(Collectors.joining("\n")));
        }
        var plan = ready.execution().acPlan();
        active = new SwingWorker<>() {
            @Override
            protected AcResult doInBackground() { return plan.execute(); }

            @Override
            protected void done() {
                if (active != this) return;
                active = null;
                window.setAcAnalysisRunning(false);
                try {
                    AcResult result = get();
                    window.setzeSTATUS("AC analysis complete.");
                    resultWindow = show(window, result);
                } catch (ExecutionException error) {
                    window.setzeSTATUS("AC analysis failed.");
                    JOptionPane.showMessageDialog(window, error.getCause().getMessage(), "AC analysis", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        window.setAcAnalysisRunning(true);
        window.setzeSTATUS("AC analysis running.");
        active.execute();
    }

    boolean stop() { return active != null && syncOnEdt(this::stopRun); }

    void invalidate() {
        if (active == null && resultWindow == null && SwingUtilities.isEventDispatchThread()) return;
        syncOnEdt(() -> {
            invalidateRun();
            return null;
        });
    }

    private void invalidateRun() {
        stopRun();
        if (resultWindow != null) {
            resultWindow.dispose();
            resultWindow = null;
        }
    }

    private boolean stopRun() {
        if (active == null) return false;
        var oldRun = active;
        active = null;
        oldRun.cancel(true);
        window.setAcAnalysisRunning(false);
        window.setzeSTATUS("AC analysis stopped.");
        return true;
    }

    private static <T> T syncOnEdt(Callable<T> operation) {
        try {
            return onEdt(operation);
        } catch (Exception error) {
            throw new IllegalStateException("The AC presentation update failed.", error);
        }
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return operation.call();
        var task = new FutureTask<>(operation);
        try {
            SwingUtilities.invokeAndWait(task);
            return task.get();
        } catch (InterruptedException error) {
            task.cancel(false);
            Thread.currentThread().interrupt();
            throw error;
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static JDialog show(MainWindow owner, AcResult result) {
        var dialog = new JDialog(owner, "AC frequency response", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        var traces = new ArrayList<Trace>();
        for (var group : result.groups()) {
            if (group.ground()) continue;
            String name = group.aliases().isEmpty() ? group.id() : String.join(", ", group.aliases());
            var terminal = group.members().getFirst();
            traces.add(new Trace("V(" + name + ") [" + terminal.instance() + ":" + terminal.spiceOrder() + "]",
                    "V", group.voltageV()));
        }
        for (var current : result.currents()) traces.add(new Trace("I(" + current.instance() + ")", "A", current.currentA()));
        if (result.hierarchy() != null) {
            for (var port : result.hierarchy().ports()) {
                var formal = port.internal();
                String label = String.join(":", formal.occurrencePath()) + ":" + formal.instance();
                traces.add(new Trace("Ix(" + label + ")", "A", port.currentIntoChildA()));
            }
        }
        var selector = new JComboBox<>(traces.toArray(Trace[]::new));
        var controls = new JPanel(new BorderLayout(8, 0));
        var label = new JLabel("Signal");
        label.setLabelFor(selector);
        controls.add(label, BorderLayout.WEST);
        controls.add(selector, BorderLayout.CENTER);
        var plots = new JPanel(new GridLayout(2, 1));
        var magnitude = new Plot(result.frequencyHz(), traces.getFirst(), false);
        var phase = new Plot(result.frequencyHz(), traces.getFirst(), true);
        plots.add(magnitude);
        plots.add(phase);
        selector.addActionListener(event -> {
            Trace selected = (Trace) selector.getSelectedItem();
            magnitude.trace = selected;
            phase.trace = selected;
            plots.repaint();
        });
        dialog.add(controls, BorderLayout.NORTH);
        dialog.add(plots, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog;
    }

    private static final class Plot extends JPanel {
        private final List<Double> frequencies;
        private final boolean phase;
        private Trace trace;

        Plot(List<Double> frequencies, Trace trace, boolean phase) {
            this.frequencies = frequencies;
            this.trace = trace;
            this.phase = phase;
            setPreferredSize(new Dimension(760, 220));
            setBackground(Color.WHITE);
        }

        private double value(Complex sample) {
            return phase ? Math.toDegrees(Math.atan2(sample.imaginary(), sample.real())) : sample.abs();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 82, right = getWidth() - 30, top = 28, bottom = getHeight() - 46;
            double low = phase ? -180 : 0;
            double high = phase ? 180 : trace.values.stream().mapToDouble(this::value).max().orElse(1);
            if (high == 0) high = 1;
            g.setColor(Color.DARK_GRAY);
            g.drawString(phase ? "Phase (degrees)" : "Magnitude (" + trace.units + ")", left, 17);
            for (int j = 0; j <= 4; j++) {
                int y = top + (bottom - top) * j / 4;
                g.setColor(new Color(220, 224, 229));
                g.drawLine(left, y, right, y);
                g.setColor(Color.DARK_GRAY);
                g.drawString(String.format(Locale.ROOT, "%.4g", high - (high - low) * j / 4), 10, y + 4);
            }
            double first = Math.log10(frequencies.getFirst()), last = Math.log10(frequencies.getLast());
            double width = last == first ? 1 : last - first;
            for (int j = 0; j <= 4; j++) {
                int x = left + (right - left) * j / 4;
                g.setColor(new Color(220, 224, 229));
                g.drawLine(x, top, x, bottom);
                g.setColor(Color.DARK_GRAY);
                if (last != first || j == 0) {
                    String tick = String.format(Locale.ROOT, "%.3g", Math.pow(10, first + (last - first) * j / 4));
                    int labelWidth = g.getFontMetrics().stringWidth(tick);
                    g.drawString(tick, Math.min(x - labelWidth / 2, getWidth() - labelWidth - 4), bottom + 18);
                }
            }
            g.setColor(Color.DARK_GRAY);
            g.drawString("Frequency (Hz, log scale)", (left + right) / 2 - 70, bottom + 37);
            g.setColor(new Color(16, 103, 190));
            int previousX = 0, previousY = 0;
            double previousValue = 0;
            for (int i = 0; i < frequencies.size(); i++) {
                double sample = value(trace.values.get(i));
                int x = left + (int) Math.round((right - left) * (Math.log10(frequencies.get(i)) - first) / width);
                int y = bottom - (int) Math.round((bottom - top) * (sample - low) / (high - low));
                if (i > 0 && (!phase || Math.abs(sample - previousValue) <= 180)) g.drawLine(previousX, previousY, x, y);
                else g.fillOval(x - 2, y - 2, 4, 4);
                previousX = x;
                previousY = y;
                previousValue = sample;
            }
            g.dispose();
        }
    }
}
