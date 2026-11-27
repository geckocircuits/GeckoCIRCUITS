package gecko.core.io.importer;

import gecko.core.ac.AcInputOrigin;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Command-line circuit conversion. Diagnostics use one structured JSON object per line. */
public final class Main {
    private Main() { }

    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }

    /** Returns zero for a complete import, two for a partial import, or one for an input/output error. */
    public static int run(String[] args, PrintStream output, PrintStream errors) {
        boolean exponentialDiodes = false;
        boolean bipolarTransistors = false;
        boolean levelOneMosfets = false;
        int length = args.length;
        while (length > 2) {
            String flag = args[length - 1];
            if (flag.equals("--exponential-diodes")) exponentialDiodes = true;
            else if (flag.equals("--bipolar-transistors")) bipolarTransistors = true;
            else if (flag.equals("--level-one-mosfets")) levelOneMosfets = true;
            else break;
            length--;
        }
        if (length != args.length) args = java.util.Arrays.copyOf(args, length);
        boolean ascOptions = args.length >= 4 && args.length % 2 == 0 && args[2].equals("--symbols")
                && args[0].toLowerCase(Locale.ROOT).endsWith(".asc");
        if (args.length != 2 && !ascOptions) {
            errors.println("Usage: gecko.core.io.importer.Main <input.cir|input.asc|input.plecs> <output.ipes>"
                    + " [--symbols <stock-symbol-directory> [--source-root <source-directory>]... [--profile <id>]"
                    + " [--model-cards <file>]] [--exponential-diodes] [--bipolar-transistors] [--level-one-mosfets]");
            return 1;
        }
        try {
            Path input = Path.of(args[0]);
            String inputName = input.toString();
            String lowerName = inputName.toLowerCase(Locale.ROOT);
            if (bipolarTransistors && !lowerName.endsWith(".cir") && !ascOptions) {
                throw new IllegalArgumentException("Use the bipolar transistor option with SPICE AC input.");
            }
            if (levelOneMosfets && !lowerName.endsWith(".cir")) {
                throw new IllegalArgumentException("Use the Level 1 MOSFET option with flat transient SPICE input.");
            }
            CircuitModel model;
            if (lowerName.endsWith(".asc")) {
                var sourceRoots = new java.util.ArrayList<Path>();
                Path modelCards = null;
                AcInputOrigin origin = AcInputOrigin.UNSELECTED_ASC;
                for (int i = 4; i < args.length; i += 2) {
                    switch (args[i]) {
                        case "--source-root" -> sourceRoots.add(Path.of(args[i + 1]));
                        case "--model-cards" -> {
                            if (modelCards != null) throw new IllegalArgumentException("Select one semiconductor model file.");
                            modelCards = Path.of(args[i + 1]);
                        }
                        case "--profile" -> {
                            if (origin.tested()) throw new IllegalArgumentException("Select one ASC import profile.");
                            origin = AcInputOrigin.selectProfile(args[i + 1]);
                        }
                        default -> throw new IllegalArgumentException("Unknown import option " + args[i]);
                    }
                }
                if (bipolarTransistors != (modelCards != null)) {
                    throw new IllegalArgumentException("Select both the bipolar transistor option and a local model file for ASC input.");
                }
                if (exponentialDiodes && (origin.tested() || !sourceRoots.isEmpty() && modelCards == null)) {
                    throw new IllegalArgumentException("Use unselected ASC input with explicit models for hierarchical exponential diodes.");
                }
                var importer = modelCards == null ? new LtspiceImporter(exponentialDiodes)
                        : new LtspiceImporter(exponentialDiodes, modelCards);
                model = args.length == 2 ? importer.convert(input)
                        : importer.convert(input, Path.of(args[3]), sourceRoots, origin);
            } else {
                String source = Files.readString(input, StandardCharsets.UTF_8);
                model = lowerName.endsWith(".plecs")
                        ? new PlecsImporter().convert(source, inputName)
                        : new SpiceImporter(exponentialDiodes, bipolarTransistors, levelOneMosfets).convert(source, inputName);
            }
            if (bipolarTransistors && model.getAcSettings() == null) {
                throw new IllegalArgumentException("Use the bipolar transistor option with SPICE AC input.");
            }
            if (exponentialDiodes && model.getAcSettings() == null && model.getCircuitComponents().stream().noneMatch(
                    component -> component.getAcComponent() != null && component.getAcComponent().law()
                            instanceof gecko.core.ac.AcComponent.ExponentialDiode diode && diode.transientLaw())) {
                throw new IllegalArgumentException("Require an AC analysis or a transient exponential diode.");
            }
            new CircuitFileWriter().write(model, Path.of(args[1]));
            for (var issue : model.getImportIssues()) output.println("{\"code\":" + json(issue.code())
                    + ",\"domain\":" + json(issue.domain()) + ",\"component\":" + json(issue.component())
                    + ",\"message\":" + json(issue.message()) + ",\"sourceLocation\":" + json(issue.sourceLocation())
                    + ",\"unsupported\":" + issue.unsupported() + ",\"parameterPath\":" + json(issue.parameterPath()) + "}");
            return model.getImportIssues().isEmpty() ? 0 : 2;
        } catch (IOException | IllegalArgumentException | ArithmeticException error) {
            errors.println("{\"code\":\"CONVERSION_FAILED\",\"message\":" + json(error.getMessage()) + "}");
            return 1;
        }
    }

    public static String json(String value) {
        if (value == null) return "null";
        var result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') result.append('\\').append(c);
            else if (c < 32) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }
}
