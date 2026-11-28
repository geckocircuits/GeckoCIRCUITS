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
package gecko.core.simulation;

import gecko.core.control.calculators.AbstractControlCalculatable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Reads signal values from a wired SCOPE calculator's input signals.
 *
 * <p>Once the control chain is wired (via {@link ControlCalculatorFactory}),
 * the SCOPE calculator's {@code _inputSignal[i]} arrays are shared references
 * to the upstream calculators' output arrays. This extractor simply reads those
 * shared values — no copying or routing needed.</p>
 *
 * <p>This mirrors the GUI-side behavior in {@code NetzlisteCONTROL.writeData()},
 * which reads {@code _allSortedCalculatables[idx]._inputSignal[port][0]}.</p>
 */
public class ScopeDataExtractor {

    public record ScopeChannel(String scopeName, long scopeIdentifier, int channelIndex, String signalName, int row) { }

    private final String[] signalNames;
    private final int signalCount;
    private final List<ScopeChannel> channels;
    private final List<SignalReference> rows;
    private record SignalReference(AbstractControlCalculatable calculator, int port) { }

    /**
     * Creates a ScopeDataExtractor for the given SCOPE calculator.
     *
     * @param scopeCalculator the wired SCOPE calculator (NothingToDoCalculator with N inputs)
     * @param signalNames     the signal names corresponding to each input
     */
    public ScopeDataExtractor(AbstractControlCalculatable scopeCalculator, String[] signalNames) {
        this.signalNames = signalNames != null ? signalNames.clone() : new String[0];
        this.signalCount = this.signalNames.length;
        var result = new ArrayList<ScopeChannel>();
        var references = new ArrayList<SignalReference>();
        for (int index = 0; index < signalCount; index++) {
            result.add(new ScopeChannel("", 0, index, this.signalNames[index], index));
            references.add(new SignalReference(scopeCalculator, index));
        }
        channels = List.copyOf(result);
        rows = List.copyOf(references);
    }

    private ScopeDataExtractor(java.util.List<ControlBlockMapping.ScopeInfo> scopes) {
        var names = new ArrayList<String>();
        var references = new ArrayList<SignalReference>();
        var rowForSignal = new IdentityHashMap<double[], Integer>();
        // The desktop optimizer reverses equal-priority SCOPE sinks.
        for (int index = scopes.size() - 1; index >= 0; index--) {
            var scope = scopes.get(index);
            var labels = scope.getSignalNames();
            for (int port = 0; port < labels.length; port++) {
                var signal = scope.getCalculator()._inputSignal[port];
                if (!rowForSignal.containsKey(signal)) {
                    rowForSignal.put(signal, names.size());
                    names.add(labels[port]);
                    references.add(new SignalReference(scope.getCalculator(), port));
                }
            }
        }
        var result = new ArrayList<ScopeChannel>();
        for (var scope : scopes) {
            var labels = scope.getSignalNames();
            for (int port = 0; port < labels.length; port++) {
                result.add(new ScopeChannel(scope.getName(), scope.getIdentifier(), port, labels[port],
                        rowForSignal.get(scope.getCalculator()._inputSignal[port])));
            }
        }
        signalNames = names.toArray(String[]::new);
        signalCount = signalNames.length;
        channels = List.copyOf(result);
        rows = List.copyOf(references);
    }

    /**
     * Builds recorded rows and per-SCOPE port mappings from wired SCOPE inputs.
     * Shared signals use one recorded row. Disconnected inputs retain separate rows.
     *
     * @param scopes wired SCOPE entries in saved component order
     * @return the extractor, or null if the list is empty
     * @throws NullPointerException if scopes is null
     */
    public static ScopeDataExtractor fromScopes(java.util.List<ControlBlockMapping.ScopeInfo> scopes) {
        return scopes.isEmpty() ? null : new ScopeDataExtractor(scopes);
    }

    /**
     * Gets each saved SCOPE port and its shared recorded-row mapping.
     *
     * @return immutable channel mappings in saved SCOPE and port order
     */
    public java.util.List<ScopeChannel> getChannels() { return channels; }

    /**
     * Creates a ScopeDataExtractor from a {@link ControlBlockMapping.ScopeInfo}.
     *
     * @param scopeInfo the scope info from the factory build result
     * @return a new extractor, or null if scopeInfo is null
     */
    public static ScopeDataExtractor fromScopeInfo(ControlBlockMapping.ScopeInfo scopeInfo) {
        if (scopeInfo == null) {
            return null;
        }
        return new ScopeDataExtractor(scopeInfo.getCalculator(), scopeInfo.getSignalNames());
    }

    /**
     * Extracts the current values of all SCOPE input signals.
     *
     * <p>Each value is read from {@code scopeCalculator._inputSignal[i][0]},
     * which is a shared reference to the upstream calculator's output signal.</p>
     *
     * @return array of float values, one per signal
     */
    public float[] extractCurrentValues() {
        float[] values = new float[signalCount];
        for (int row = 0; row < rows.size(); row++) {
            var source = rows.get(row);
            var calculator = source.calculator();
            if (source.port() < calculator._inputSignal.length && calculator._inputSignal[source.port()] != null) {
                values[row] = (float) calculator._inputSignal[source.port()][0];
            }
        }
        return values;
    }

    /**
     * Returns the signal names corresponding to each SCOPE input.
     *
     * @return array of signal name strings
     */
    public String[] getSignalNames() {
        return signalNames.clone();
    }

    /**
     * Returns the number of signals in this SCOPE.
     *
     * @return the signal count
     */
    public int getSignalCount() {
        return signalCount;
    }
}
