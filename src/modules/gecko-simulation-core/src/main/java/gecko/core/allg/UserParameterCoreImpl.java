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
package gecko.core.allg;

import gecko.core.circuit.TokenMap;
import gecko.core.io.SerializationUtils;

/**
 * Concrete implementation of UserParameterCore for headless simulation contexts.
 *
 * Supports Double, Integer, Boolean, and String parameter types.
 * Uses Builder pattern matching the main project's UserParameter API for familiar usage.
 *
 * Example usage:
 * <pre>
 * UserParameterCore&lt;Double&gt; tj = UserParameterCoreImpl.Builder
 *     .start("tj", 25.0)
 *     .shortName("Tj")
 *     .longName("Junction Temperature")
 *     .unit("°C")
 *     .build();
 * </pre>
 */
public final class UserParameterCoreImpl<T> implements UserParameterCore<T> {

    private T _value;
    private final String _identifier;
    private final String _shortName;
    private final String _longName;
    private final String _unit;

    private UserParameterCoreImpl(String identifier, T initialValue, String shortName, String longName, String unit) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter identifier cannot be null or empty");
        }
        if (initialValue == null) {
            throw new IllegalArgumentException("Initial value cannot be null");
        }

        this._identifier = identifier;
        this._value = initialValue;
        this._shortName = shortName != null ? shortName : identifier;
        this._longName = longName != null ? longName : identifier;
        this._unit = unit != null ? unit : "";
    }

    @Override
    public T getValue() {
        return _value;
    }

    @Override
    public double getDoubleValue() {
        if (_value instanceof Number) {
            return ((Number) _value).doubleValue();
        }
        throw new ClassCastException("Cannot convert " + _value.getClass().getSimpleName() + " to double");
    }

    @Override
    public void setValueWithoutUndo(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter value cannot be null");
        }
        this._value = value;
    }

    @Override
    public void readFromTokenMap(TokenMap tokenMap) {
        if (_value instanceof Double) {
            @SuppressWarnings("unchecked")
            T newValue = (T) Double.valueOf(tokenMap.readDataLine(_identifier, (Double) _value));
            _value = newValue;
        } else if (_value instanceof Integer) {
            @SuppressWarnings("unchecked")
            T newValue = (T) Integer.valueOf(tokenMap.readDataLine(_identifier, (Integer) _value));
            _value = newValue;
        } else if (_value instanceof Boolean) {
            @SuppressWarnings("unchecked")
            T newValue = (T) Boolean.valueOf(tokenMap.readDataLine(_identifier, (Boolean) _value));
            _value = newValue;
        } else if (_value instanceof String) {
            @SuppressWarnings("unchecked")
            T newValue = (T) tokenMap.readDataLine(_identifier, (String) _value);
            _value = newValue;
        } else {
            throw new UnsupportedOperationException("Unsupported parameter type: " + _value.getClass().getSimpleName());
        }
    }

    @Override
    public void writeXMLToFile(StringBuffer ascii) {
        SerializationUtils.appendAsString(ascii, _identifier, _value);
    }

    @Override
    public String getSaveIdentifier() {
        return _identifier;
    }

    @Override
    public String getShortName() {
        return _shortName;
    }

    @Override
    public String getUnit() {
        return _unit;
    }

    @Override
    public String getLongName() {
        return _longName;
    }

    /**
     * Builder for creating UserParameterCoreImpl instances.
     * Provides fluent API matching main project UserParameter conventions.
     */
    public static final class Builder<T> {
        private final String identifier;
        private final T initialValue;
        private String shortName;
        private String longName;
        private String unit;

        private Builder(String identifier, T initialValue) {
            this.identifier = identifier;
            this.initialValue = initialValue;
        }

        /**
         * Start building a new parameter.
         * @param identifier Unique save identifier (e.g., "tj", "uBlock")
         * @param initialValue Initial parameter value
         * @param <T> Parameter type (Double, Integer, Boolean, String)
         * @return Builder instance
         */
        public static <T> Builder<T> start(String identifier, T initialValue) {
            return new Builder<>(identifier, initialValue);
        }

        /**
         * Set short display name (e.g., "Tj", "Ub").
         * @param name Short name
         * @return Builder instance for chaining
         */
        public Builder<T> shortName(String name) {
            this.shortName = name;
            return this;
        }

        /**
         * Set long descriptive name (e.g., "Junction Temperature").
         * @param name Long description
         * @return Builder instance for chaining
         */
        public Builder<T> longName(String name) {
            this.longName = name;
            return this;
        }

        /**
         * Set measurement unit (e.g., "°C", "V", "A").
         * @param unit Unit string
         * @return Builder instance for chaining
         */
        public Builder<T> unit(String unit) {
            this.unit = unit;
            return this;
        }

        /**
         * Build the UserParameterCoreImpl instance.
         * @return New UserParameterCoreImpl instance
         */
        public UserParameterCoreImpl<T> build() {
            return new UserParameterCoreImpl<>(identifier, initialValue, shortName, longName, unit);
        }
    }
}
