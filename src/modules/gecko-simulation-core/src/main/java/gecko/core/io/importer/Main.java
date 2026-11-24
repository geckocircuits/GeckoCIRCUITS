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
        if (args.length != 2) { errors.println("Usage: gecko.core.io.importer.Main <input.cir|input.asc|input.plecs> <output.ipes>"); return 1; }
        try {
            Path input = Path.of(args[0]);
            String inputName = input.toString().toLowerCase(Locale.ROOT);
            CircuitModel model;
            if (inputName.endsWith(".asc")) {
                model = new LtspiceImporter().convert(input);
            } else {
                String source = Files.readString(input, StandardCharsets.UTF_8);
                model = inputName.endsWith(".plecs")
                        ? new PlecsImporter().convert(source, input.toString())
                        : new SpiceImporter().convert(source, input.toString());
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

    private static String json(String value) {
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
