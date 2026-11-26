package gecko.geckocircuits.circuit;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcDocumentSettings;
import gecko.core.ac.AcSweep;
import gecko.core.ac.ParameterDefinition;
import gecko.core.io.importer.SpiceValue;
import gecko.geckocircuits.allg.MainWindow;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Desktop controls for the active AC settings and component laws. */
public final class AcEditorDialogs {
    private AcEditorDialogs() { }

    public static void editSettings(MainWindow window, SchematicEditor2 editor) {
        AcDocumentSettings current = editor.getAcSettings();
        var sweep = current == null ? new AcSweep(40, 10, 100000) : current.sweep();
        var panel = new JPanel(new BorderLayout(8, 8));
        var fields = new JPanel(new GridLayout(0, 2, 8, 4));
        var points = addField(fields, "Points per decade", Integer.toString(sweep.pointsPerDecade()));
        var start = addField(fields, "Start frequency (Hz)", Double.toString(sweep.startHz()));
        var stop = addField(fields, "Stop frequency (Hz)", Double.toString(sweep.stopHz()));
        fields.add(new JLabel("Scalar definitions (name = expression)"));
        panel.add(fields, BorderLayout.NORTH);
        var definitions = new JTextArea(10, 42);
        var rootParameters = current == null ? editor.getRootParameterDefinitions() : current.parameters();
        rootParameters.forEach(p -> definitions.append(p.name() + " = " + p.expression() + "\n"));
        panel.add(new JScrollPane(definitions), BorderLayout.CENTER);
        while (JOptionPane.showConfirmDialog(window, panel, "AC analysis settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                var parameters = new ArrayList<ParameterDefinition>();
                for (String row : definitions.getText().split("\\R")) {
                    if (row.isBlank()) continue;
                    String[] assignment = row.split("=", 2);
                    if (assignment.length != 2) throw new IllegalArgumentException("Enter one parameter assignment per line.");
                    parameters.add(new ParameterDefinition(assignment[0].strip(), assignment[1].strip()));
                }
                int density = Integer.parseInt(points.getText().strip());
                double first = SpiceValue.evaluate(start.getText(), Map.of());
                double last = SpiceValue.evaluate(stop.getText(), Map.of());
                var newSweep = new AcSweep(density, first, last);
                String analysis = current != null && current.sweep().equals(newSweep) ? current.analysisId()
                        : ".ac dec " + density + " " + start.getText().strip() + " " + stop.getText().strip();
                var settings = new AcDocumentSettings(newSweep, parameters, analysis);
                var scalarScope = settings.parameterMap();
                scalarScope.values().forEach(expression -> SpiceValue.evaluate(expression, scalarScope));
                editor.setAcSettings(settings);
                return;
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(window, error.getMessage(), "AC settings", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static Window componentDialog(AbstractBlockInterface block) {
        var current = block.getAcComponentOrLiteral();
        var settings = SchematicEditor2.Singleton.getAcSettings();
        var editor = SchematicEditor2.Singleton;
        Map<String, String> parameters = editor.getHierarchy() != null ? editor.hierarchyEdits.scope(block)
                : settings == null ? Map.of() : settings.parameterMap();
        var fields = new JPanel(new GridLayout(0, 2, 8, 4));
        java.util.function.Supplier<AcComponent.Law> updatedLaw;
        switch (current.law()) {
            case AcComponent.Passive law -> {
                var expression = addField(fields, "Value or expression", law.expression());
                fields.add(new JLabel("Evaluated value"));
                fields.add(new JLabel(displayScalar(law.expression(), parameters)));
                updatedLaw = () -> {
                    String text = expression.getText().strip();
                    if (SpiceValue.evaluate(text, parameters) <= 0) {
                        throw new IllegalArgumentException("Require a positive component value.");
                    }
                    return new AcComponent.Passive(text);
                };
            }
            case AcComponent.Voltage law -> {
                var dc = addField(fields, "DC value (V)", law.dc());
                var waveform = addField(fields, "Transient waveform", law.waveform());
                var magnitude = addField(fields, "AC magnitude (V)", law.magnitude());
                var phase = addField(fields, "AC phase (degrees)", law.phaseDegrees());
                updatedLaw = () -> {
                    SpiceValue.evaluate(dc.getText(), parameters);
                    if (SpiceValue.evaluate(magnitude.getText(), parameters) < 0) {
                        throw new IllegalArgumentException("Require a nonnegative AC magnitude.");
                    }
                    SpiceValue.evaluate(phase.getText(), parameters);
                    return new AcComponent.Voltage(dc.getText().strip(), waveform.getText().strip(),
                            magnitude.getText().strip(), phase.getText().strip());
                };
            }
            case AcComponent.Inductance law -> {
                var inductance = addField(fields, "Inductance (H)", law.expression());
                String expression = law.resistance() instanceof AcComponent.ExplicitResistance explicit
                        ? explicit.expression() : "";
                var resistance = addField(fields, "Series resistance (Ω)", expression);
                var restore = new JCheckBox("Use imported default", law.resistance() instanceof AcComponent.ImportedDefault);
                if (law.origin().tested()) {
                    fields.add(restore);
                    fields.add(new JLabel("0.001 Ω"));
                    resistance.setEnabled(!restore.isSelected());
                    restore.addActionListener(event -> resistance.setEnabled(!restore.isSelected()));
                }
                var effective = new JLabel();
                try {
                    var evaluated = law.evaluate(parameters);
                    effective.setText(evaluated.ohms() + " Ω (" + evaluated.mode().label() + ")");
                } catch (IllegalArgumentException error) {
                    effective.setText("Invalid value");
                    effective.setToolTipText(error.getMessage());
                }
                fields.add(new JLabel("Effective series resistance"));
                fields.add(effective);
                updatedLaw = () -> {
                    AcComponent.Resistance chosen = restore.isSelected() ? new AcComponent.ImportedDefault()
                            : new AcComponent.ExplicitResistance(resistance.getText().strip());
                    var updated = new AcComponent.Inductance(inductance.getText().strip(), chosen, law.origin());
                    updated.evaluate(parameters);
                    return updated;
                };
            }
            case AcComponent.Current law -> {
                var dc = addField(fields, "DC value (A)", law.dc());
                var magnitude = addField(fields, "AC magnitude (A)", law.magnitude());
                var phase = addField(fields, "AC phase (degrees)", law.phaseDegrees());
                updatedLaw = () -> {
                    SpiceValue.evaluate(dc.getText(), parameters);
                    if (SpiceValue.evaluate(magnitude.getText(), parameters) < 0) {
                        throw new IllegalArgumentException("Require a nonnegative AC magnitude.");
                    }
                    SpiceValue.evaluate(phase.getText(), parameters);
                    return new AcComponent.Current(dc.getText().strip(), magnitude.getText().strip(), phase.getText().strip());
                };
            }
            case AcComponent.Controlled law -> {
                var expression = addField(fields, "Voltage gain", law.expression());
                var laplace = new JCheckBox("Use Laplace variable s", law.laplace());
                fields.add(laplace);
                updatedLaw = () -> {
                    String text = expression.getText().strip();
                    if (laplace.isSelected()) {
                        double hz = settings == null ? 10 : settings.sweep().startHz();
                        SpiceValue.transfer(text, parameters).evaluate(new gecko.core.ac.Complex(0, 2 * Math.PI * hz));
                    } else {
                        SpiceValue.evaluate(text, parameters);
                    }
                    return new AcComponent.Controlled(text, laplace.isSelected());
                };
            }
        }
        var pane = new JOptionPane(fields, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog("AC component " + block.getStringID());
        pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, event -> {
            if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(event.getNewValue())) return;
            try {
                block.setAcLaw(updatedLaw.get());
            } catch (IllegalArgumentException error) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialog.getOwner(),
                        error.getMessage(), "AC component", JOptionPane.ERROR_MESSAGE));
            }
        });
        return dialog;
    }

    private static JTextField addField(JPanel panel, String label, String value) {
        var field = new JTextField(value, 30);
        var caption = new JLabel(label);
        caption.setLabelFor(field);
        panel.add(caption);
        panel.add(field);
        return field;
    }

    public static void editHierarchyParameters(Window window, SchematicEditor2 editor) {
        if (!(editor._visibleCircuitSheet instanceof gecko.geckocircuits.control.SubCircuitSheet child)
                || child._subBlock.getHierarchyCall() == null) {
            JOptionPane.showMessageDialog(window, "Open an imported ASC block first.", "ASC parameters",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        var owner = child._subBlock;
        var call = owner.getHierarchyCall();
        var definition = editor.getHierarchy().definition(call.definitionId());
        var defaults = new javax.swing.JTextArea(parameterText(definition.defaults()), 5, 30);
        var overrides = new javax.swing.JTextArea(parameterText(call.overrides()), 5, 30);
        var fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.add(new JLabel("Definition defaults"));
        fields.add(new javax.swing.JScrollPane(defaults));
        fields.add(new JLabel("Overrides for " + call.instance()));
        fields.add(new javax.swing.JScrollPane(overrides));
        while (JOptionPane.showConfirmDialog(window, fields, "ASC parameters", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            try {
                editor.hierarchyEdits.setParameters(owner, readParameters(defaults.getText()), readParameters(overrides.getText()));
                return;
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(window, error.getMessage(), "ASC parameters", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String parameterText(List<ParameterDefinition> definitions) {
        return String.join("\n", definitions.stream().map(definition -> definition.name() + "=" + definition.expression()).toList());
    }

    private static List<ParameterDefinition> readParameters(String text) {
        return text.lines().filter(line -> !line.isBlank()).map(line -> {
            String[] assignment = line.split("=", 2);
            if (assignment.length != 2) throw new IllegalArgumentException("Enter one parameter assignment per line.");
            return new ParameterDefinition(assignment[0].strip(), assignment[1].strip());
        }).toList();
    }

    private static String displayScalar(String text, Map<String, String> parameters) {
        try {
            return Double.toString(SpiceValue.evaluate(text, parameters));
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
    }
}
