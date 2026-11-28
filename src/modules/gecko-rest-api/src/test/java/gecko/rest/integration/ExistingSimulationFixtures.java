package gecko.rest.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ExistingSimulationFixtures {
    private ExistingSimulationFixtures() { }

    static String rc(Path directory) throws IOException {
        return write(directory, "rc.ipes", source("401 6")
                + element(1, "R1", "1000", "/1", "/2")
                + element(3, "C1", "1e-6", "/2", "/0"));
    }

    static String ac(Path directory) throws IOException {
        return write(directory, "ac.ipes", source("402 6 50 0 0")
                + element(1, "R1", "1000", "/1", "/0"));
    }

    static String thermal(Path directory) throws IOException {
        return write(directory, "thermal.ipes", element(45, "ambient", "401 25", "/1", "/0")
                + element(46, "RTH", "10", "/1", "/0"));
    }

    static String complex(Path directory) throws IOException {
        return write(directory, "complex.ipes", source("401 6")
                + element(1, "R1", "1000", "/1", "/2")
                + element(1, "R2", "2000", "/2", "/3")
                + element(2, "L1", "1e-3", "/3", "/4")
                + element(3, "C1", "1e-6", "/4", "/0")
                + element(3, "C2", "2e-6", "/4", "/0"));
    }

    private static String source(String parameters) {
        return element(4, "Vs", parameters, "/1", "/0");
    }

    private static String write(Path directory, String name, String elements) throws IOException {
        Path file = directory.resolve(name);
        Files.writeString(file, "dt 1e-6\ntDURATION 0.01\n" + elements, StandardCharsets.US_ASCII);
        return file.toString();
    }

    private static String element(int type, String name, String parameters, String inputs, String outputs) {
        return "e\n<ElementLK>\ntyp " + type + "\nidStringDialog " + name
                + "\nx 0\ny 0\norientierung 503\nparameterString[] /\nparameter[] " + parameters
                + "\nlabelAnfangsKnoten[] " + inputs + "\nlabelEndKnoten[] " + outputs
                + "\n<" + (char) 92 + "ElementLK>\n";
    }
}