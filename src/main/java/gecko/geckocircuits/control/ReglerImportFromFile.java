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
package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.ControlSourceType;
import gecko.i18n.resources.I18nKeys;

/**
 *
 * @author andy
 */
public class ReglerImportFromFile  extends ReglerSignalSource {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerImportFromFile.class, "FILE", I18nKeys.DATA_FROM_FILE);    

    public ReglerImportFromFile() {    
        super();
        _typQuelle.setValueWithoutUndo(ControlSourceType.QUELLE_IMPORT);
    }            
}
