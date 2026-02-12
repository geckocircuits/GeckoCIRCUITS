/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
/*
 * -This class defines two HashMaps needed for assigning keys to values and
 * values to keys for a specific language.
 * -A new instance of this class needs to be defined for each language used.
 */
package gecko.i18n;

import gecko.i18n.resources.I18nKeys;
import java.util.HashMap;
import java.util.Set;

public final class DoubleMap {
    
    /**
     * maps keys to values, e.g. "GeckoFrame.jMenuItem17.text" to "Export"
     */
    private HashMap<I18nKeys,String> _keytoValue = new HashMap<I18nKeys, String>();
    
    /**
     * maps values to keys, e.g. "Export" to "GeckoFrame.jMenuItem17.text"
     */
    private HashMap<String, I18nKeys> _valuetoKey = new HashMap<String,I18nKeys>();        
   
    
    public void insertPair(final I18nKeys englishKey, final String value) {
        _keytoValue.put(englishKey, value);
        _valuetoKey.put(value, englishKey);
    }
    
    public void removePair(final I18nKeys key, final String value) {
        _keytoValue.remove(key);
        _valuetoKey.remove(value);
    }
    
    public String getValue(final I18nKeys key) {
        return _keytoValue.get(key);
    }
    
    public I18nKeys getKey(final String value) {
        return _valuetoKey.get(value);
    }

    public Set<I18nKeys> getKeySet() {
        return _keytoValue.keySet();
    }
    
    public int getSize() {
        return _keytoValue.size();
    }
    
}