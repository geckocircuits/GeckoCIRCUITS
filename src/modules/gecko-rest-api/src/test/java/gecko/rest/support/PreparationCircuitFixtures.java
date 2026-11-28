package gecko.rest.support;

import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * Supplies saved circuit data for tests at public preparation and REST interfaces.
 */
public final class PreparationCircuitFixtures {
    private PreparationCircuitFixtures() { }

    /**
     * Creates a six-volt resistor circuit with a current probe and SCOPE.
     *
     * @param resistance the configured resistance in ohms.
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] resistorCircuit(double resistance) throws IOException {
        String text = resistorElements(resistance);
        return gzip(text);
    }

    /**
     * Creates a resistor circuit with a configurable DC voltage.
     *
     * @param resistance the configured resistance in ohms.
     * @param voltage the DC source voltage in volts.
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] resistorCircuit(double resistance, double voltage) throws IOException {
        return gzip(resistorElements(resistance, "source", voltage));
    }

    private static String resistorElements(double resistance) {
        return resistorElements(resistance, "source");
    }

    private static String resistorElements(double resistance, String sourceName) {
        return resistorElements(resistance, sourceName, 6);
    }

    private static String resistorElements(double resistance, String sourceName, double voltage) {
        return "dt 0.125\ntDURATION 0.25\n"
                + element("LK", 4, sourceName, "401 " + voltage, "/1", "/0", "")
                + element("LK", 1, "R.1", Double.toString(resistance), "/1", "/0", "")
                + element("CONTROL", 2, "ammeter", "", "", "/current", "/R.1")
                + element("CONTROL", 5, "scope", "", "/current", "", "");
    }



    /**
     * Creates saved DC-source input for canonical inspection.
     *
     * @param unsupported whether to add an unsupported CONTROL block.
     * @return gzip data with the legacy source layout.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] canonicalInspectionCircuit(boolean unsupported) throws IOException {
        return gzip(resistorElements(10, "U.1")
                + (unsupported ? element("CONTROL", 777, "unknown", "1", "", "/unused", "") : ""));
    }

    /**
     * Creates a resistor circuit whose SCOPE samples current and scaled current.
     *
     * @param resistance the configured resistance in ohms.
     * @param gain the configured CONTROL gain.
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] resistorGainCircuit(double resistance, double gain) throws IOException {
        return gzip("dt 0.125\ntDURATION 0.25\n"
                + element("LK", 4, "source", "401 6", "/1", "/0", "")
                + element("LK", 1, "R.1", Double.toString(resistance), "/1", "/0", "")
                + element("CONTROL", 2, "ammeter", "", "", "/current", "/R.1")
                + element("CONTROL", 7, "gain", Double.toString(gain), "/current", "/scaled", "")
                + element("CONTROL", 5, "scope", "", "/current/scaled", "", ""));
    }


    /**
     * Creates a grounded circuit with parallel ideal voltage sources.
     *
     * @return gzip input for numerical failure cases.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] singularCircuit() throws IOException {
        return gzip(resistorElements(10)
                + element("LK", 4, "parallel-source", "401 6", "/1", "/0", ""));
    }

    /**
     * Creates a series resistor-capacitor circuit with a current probe.
     *
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] capacitorCircuit() throws IOException {
        return gzip("dt 0.1\ntDURATION 0.2\n"
                + element("LK", 4, "source", "401 6", "/1", "/0", "")
                + element("LK", 1, "R.1", "10", "/1", "/2", "")
                + element("LK", 3, "state", "0.02 0 0 0 0 0 0.02 0.02 0 0 0", "/2", "/0", "")
                + element("CONTROL", 2, "ammeter", "", "", "/current", "/R.1")
                + element("CONTROL", 5, "scope", "", "/current", "", ""));
    }

    /**
     * Creates a resistor whose nodes have no ground connection.
     *
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] floatingResistorCircuit() throws IOException {
        return gzip("dt 0.125\ntDURATION 0.25\n"
                + element("LK", 1, "R.1", "10", "/a", "/b", ""));
    }

    /**
     * Adds unsupported CONTROL input to a grounded resistor circuit.
     *
     * @param floating whether to include an additional floating resistor.
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] unsupportedCircuit(boolean floating) throws IOException {
        return gzip(resistorElements(10)
                + element("CONTROL", 777, "unknown", "1", "", "/unused", "")
                + (floating ? element("LK", 1, "floating", "10", "/a", "/b", "") : ""));
    }

    /**
     * Parses and prepares fixture data without executing time steps.
     *
     * @param content the plain or gzip circuit data.
     * @return the preparation result from the public core interface.
     * @throws Exception if fixture parsing or preparation fails.
     */
    public static PreparationResult prepare(byte[] content) throws Exception {
        var model = new CircuitFileParser().parse(new ByteArrayInputStream(content), "fixture.ipes");
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
    }

    /**
     * Adds a disconnected algebraic CONTROL cycle to an otherwise executable circuit.
     *
     * @return the gzip circuit data.
     * @throws IOException if gzip encoding fails.
     */
    public static byte[] algebraicControlCycle() throws IOException {
        return gzip(resistorElements(10)
                + element("CONTROL", 7, "first", "2", "/b", "/a", "")
                + element("CONTROL", 7, "second", "0.25", "/a", "/b", ""));
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static String element(String domain, int type, String name, String parameters,
            String inputs, String outputs, String references) {
        return (domain.equals("LK") ? "e" : "c") + "\n<Element" + domain + ">\ntyp " + type
                + "\nidStringDialog " + name + "\nx 0\ny 0\norientierung 503\nparameterString[] "
                + (references.isEmpty() ? "/" : references) + "\nparameter[] " + parameters
                + "\nlabelAnfangsKnoten[] " + inputs + "\nlabelEndKnoten[] " + outputs
                + "\n<" + (char) 92 + "Element" + domain + ">\n";
    }
}
