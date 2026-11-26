import gecko.core.io.CircuitFileParser;
import gecko.core.io.importer.Main;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportAndParse {
    public static void main(String[] args) {
        int status = Main.run(args, System.out, System.err);
        if (status == 0 || status == 2) {
            try (var stream = Files.newInputStream(Path.of(args[1]))) {
                var model = new CircuitFileParser().parse(stream, args[1]);
                System.out.println("{\"ipes_parse\":\"passed\",\"parsed_issue_count\":"
                        + model.getImportIssues().size() + "}");
            } catch (Exception failure) {
                System.err.println("{\"ipes_parse\":\"failed\",\"message\":"
                        + Main.json(failure.toString()) + "}");
            }
        }
        System.exit(status);
    }
}
