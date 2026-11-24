package gecko.core.io.importer;

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
        if (args.length != 2 && !(args.length == 4 && args[2].equals("--symbols") && args[0].toLowerCase(Locale.ROOT).endsWith(".asc"))) {
            errors.println("Usage: gecko.core.io.importer.Main <input.cir|input.asc|input.plecs> <output.ipes> [--symbols <stock-symbol-directory>]");
            return 1;
        }
        try {
            Path input = Path.of(args[0]);
            String inputName = input.toString();
            String lowerName = inputName.toLowerCase(Locale.ROOT);
            CircuitModel model;
            if (lowerName.endsWith(".asc")) {
                var importer = new LtspiceImporter();
                model = args.length == 2 ? importer.convert(input)
                        : importer.convert(input, Path.of(args[3]));
            } else {
                String source = Files.readString(input, StandardCharsets.UTF_8);
                model = lowerName.endsWith(".plecs")
                        ? new PlecsImporter().convert(source, inputName)
                        : new SpiceImporter().convert(source, inputName);
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
