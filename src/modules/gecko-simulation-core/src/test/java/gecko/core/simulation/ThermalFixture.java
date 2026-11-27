package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * The saved buck converter with a thermal network appended: two loss sources on its switch and diode, one RC chain
 * each, and TEMP and FLOW probes. Switch S.1 and diode D.1 use their SIMPLE loss model.
 */
final class ThermalFixture {
    static final double SWITCH_R = 0.5, SWITCH_C = 2e-4, DIODE_R = 1.0, DIODE_C = 1e-4;
    static final long SWITCH_ID = -1014213050L, DIODE_ID = -1779394816L;
    private static final char BACKSLASH = 92;

    private ThermalFixture() { }

    static String buckText() throws Exception {
        try (var in = new GZIPInputStream(new ByteArrayInputStream(Objects.requireNonNull(
                ThermalFixture.class.getResourceAsStream("/ipes/buck_simple.ipes")).readAllBytes()))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String thermal(int type, String name, long id, String from, String to, String parameters, String extra) {
        return "eTH\n<ElementTHERM>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\nlabelAnfangsKnoten[] /" + from
                + "\nlabelEndKnoten[] /" + to + "\n" + extra + "<" + BACKSLASH + "ElementTHERM>\n";
    }

    static String control(int type, String name, long id, String in, String out, String strings, String extra) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] 0.0\nparameterString[] "
                + strings + "\nlabelAnfangsKnoten[] " + in + "\nlabelEndKnoten[] " + (out.isEmpty() ? "" : "/" + out) + "\n" + extra
                + "<" + BACKSLASH + "ElementCONTROL>\n";
    }

    private static String chain(String prefix, long base, String node, double resistance, double capacitance) {
        return thermal(46, "Rth." + prefix, base, node, "0", resistance + " 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "")
                + thermal(47, "Cth." + prefix, base + 1, node, "0",
                capacitance + " 0.0 0.0 0.0 0.0 0.0 " + capacitance + " 0.0 0.0 0.0 0.0", "");
    }

    /** The thermal blocks and probes, ready to append to the buck text. */
    static String thermalBlocks(long switchCoupling, long diodeCoupling) {
        return thermal(41, "Pv.1", 5001, "", "tjS", "", "coupledReferenceID[] " + switchCoupling + " \n")
                + thermal(41, "Pv.2", 5002, "", "tjD", "", "coupledReferenceID[] " + diodeCoupling + " \n")
                + chain("S", 5010, "tjS", SWITCH_R, SWITCH_C) + chain("D", 5020, "tjD", DIODE_R, DIODE_C)
                + thermal(48, "TREF.1", 5030, "0", "0", "0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "")
                + control(16, "TEMP.1", 6001, "", "TjS", "/tjS/0/0", "coupledReferenceID[] 0 \n")
                + control(17, "FLOW.1", 6002, "", "PS", "/Pv.1/NIX_NIX_NIX/0", "coupledReferenceID[] 5001 \n")
                + control(17, "FLOW.2", 6003, "", "PD", "/Pv.2/NIX_NIX_NIX/0", "coupledReferenceID[] 5002 \nlosscomp conduction\n")
                + control(5, "SCOPE.T", 6004, "/TjS/PS/PD", "", "/NIX_NIX_NIX", "");
    }

    static CircuitModel model(String extraBlocks) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(buckText() + "\n" + extraBlocks)), "thermal-buck.ipes");
    }

    static CircuitModel model() throws Exception {
        return model(thermalBlocks(SWITCH_ID, DIODE_ID));
    }

    /** Row of the scope that records the named signal. */
    static int row(String[] names, String name) {
        for (int index = 0; index < names.length; index++) if (names[index].equals(name)) return index;
        throw new IllegalArgumentException(name + " is not recorded: " + String.join(",", names));
    }
}
