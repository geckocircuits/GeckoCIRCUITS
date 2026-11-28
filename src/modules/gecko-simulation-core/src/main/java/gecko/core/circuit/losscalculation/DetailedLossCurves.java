/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.losscalculation;

import gecko.core.circuit.TokenMap;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * The measured loss curves of a DETAILED loss model, read from a saved loss file like the legacy desktop does:
 * gzip, then deflate, then plain text.
 *
 * @param conduction the conduction table
 * @param turnOn the turn-on energy table
 * @param turnOff the turn-off energy table
 */
public record DetailedLossCurves(DetailedLossLookupTable conduction, DetailedLossLookupTable turnOn,
        DetailedLossLookupTable turnOff) {

    /**
     * @param bytes the loss file contents
     * @return its curves
     * @throws IllegalArgumentException if no switching or conduction curve is found
     */
    public static DetailedLossCurves parse(byte[] bytes) {
        List<String> lines = decode(bytes);
        TokenMap tokens = new TokenMap(lines.toArray(new String[0]));
        List<SwitchingLossCurve> switching = new ArrayList<>();
        for (TokenMap block = tokens.getBlockTokenMap("<SchaltverlusteMesskurve>"); block != null;
                block = tokens.getBlockTokenMap("<SchaltverlusteMesskurve>")) {
            SwitchingLossCurve curve = new SwitchingLossCurve(-1, -1);
            curve.importASCII(block);
            switching.add(curve);
        }
        List<LeitverlusteMesskurve> conduction = new ArrayList<>();
        for (TokenMap block = tokens.getBlockTokenMap("<LeitverlusteMesskurve>"); block != null;
                block = tokens.getBlockTokenMap("<LeitverlusteMesskurve>")) {
            LeitverlusteMesskurve curve = new LeitverlusteMesskurve(-1);
            curve.importASCII(block);
            conduction.add(curve);
        }
        if (switching.isEmpty() || conduction.isEmpty()) {
            throw new IllegalArgumentException("The loss file has no switching or conduction curves.");
        }
        return new DetailedLossCurves(DetailedLossLookupTable.fabric(conduction, 1),
                DetailedLossLookupTable.fabric(switching, 1), DetailedLossLookupTable.fabric(switching, 2));
    }

    private static List<String> decode(byte[] bytes) {
        for (int format = 0; format < 3; format++) {
            try (InputStream raw = new ByteArrayInputStream(bytes);
                 InputStream input = format == 0 ? new GZIPInputStream(raw) : format == 1 ? new InflaterInputStream(raw) : raw;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                for (String line = reader.readLine(); line != null; line = reader.readLine()) lines.add(line);
                return lines;
            } catch (IOException | RuntimeException failure) {
                // Try the next format, like the legacy loader.
            }
        }
        throw new IllegalArgumentException("The loss file cannot be read.");
    }
}
