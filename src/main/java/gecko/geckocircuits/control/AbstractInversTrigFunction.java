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
package gecko.geckocircuits.control;

import gecko.i18n.resources.I18nKeys;
import java.awt.Window;

abstract class AbstractInversTrigFunction extends AbstractReglerSingleInputSingleOutput {
    @Override
    public final String[] getOutputNames() {
        return new String[]{getFixedIDString().toLowerCase()};
    }        

    @Override
    protected Window openDialogWindow() {
        return new DialogSimpleInfoMessage(this, I18nKeys.OUTPUT_MEASURED_IN_RAD.getTranslation());
    }                
}
