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
package gecko.geckocircuits.general;

import gecko.core.allg.UserParameterCore;
import gecko.core.circuit.TokenMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Adapter that wraps the main project's GUI-enabled UserParameter class
 * to expose the UserParameterCore interface for use with core module classes.
 *
 * <p>This enables GUI-based components to pass UserParameter instances to
 * core module classes (like loss curves) that only depend on the minimal
 * UserParameterCore interface.
 *
 * <p>Design pattern: Adapter pattern following LossFileAccessor precedent.
 *
 * <p>Usage example:
 * <pre>
 * UserParameter&lt;Double&gt; guiParam = UserParameter.Builder.start("tj", 25.0).build();
 * UserParameterCore&lt;Double&gt; coreParam = new UserParameterGUIAdapter&lt;&gt;(guiParam);
 * lossCurve.setTemperatureParameter(coreParam);
 * </pre>
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Intentional adapter wrapper pattern where delegation is the primary purpose")
public final class UserParameterGUIAdapter<T> implements UserParameterCore<T> {

    private final UserParameter<T> _delegate;

    /**
     * Create an adapter wrapping a GUI-enabled UserParameter.
     *
     * @param delegate The UserParameter instance to wrap
     * @throws IllegalArgumentException if delegate is null
     */
    public UserParameterGUIAdapter(UserParameter<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("UserParameter delegate cannot be null");
        }
        this._delegate = delegate;
    }

    @Override
    public T getValue() {
        return _delegate.getValue();
    }

    @Override
    public double getDoubleValue() {
        return _delegate.getDoubleValue();
    }

    @Override
    public void setValueWithoutUndo(T value) {
        _delegate.setValueWithoutUndo(value);
    }

    @Override
    public void readFromTokenMap(TokenMap tokenMap) {
        _delegate.readFromTokenMap(tokenMap);
    }

    @Override
    public void writeXMLToFile(StringBuffer ascii) {
        _delegate.writeXMLToFile(ascii);
    }

    @Override
    public String getSaveIdentifier() {
        return _delegate.getSaveIdentifier();
    }

    @Override
    public String getShortName() {
        return _delegate.getShortName();
    }

    @Override
    public String getUnit() {
        return _delegate.getUnit();
    }

    @Override
    public String getLongName() {
        return _delegate.getLongName();
    }

    /**
     * Get the wrapped UserParameter instance.
     * Useful for GUI code that needs access to GUI-specific features.
     *
     * @return The underlying UserParameter instance
     */
    public UserParameter<T> getDelegate() {
        return _delegate;
    }
}
