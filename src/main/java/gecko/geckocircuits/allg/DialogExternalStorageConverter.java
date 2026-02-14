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
package gecko.geckocircuits.allg;

import gecko.core.allg.ExternalStorageConverter;
import gecko.core.allg.GeckoFile;

/**
 * GUI implementation of external storage conversion using DialogMakeExternal.
 * Delegates to existing dialog for user interaction.
 *
 * @since Sprint 4a - GeckoFile Migration
 */
public class DialogExternalStorageConverter implements ExternalStorageConverter {
    @Override
    public String promptForExternalPath(GeckoFile geckoFile, byte[] originalContents) {
        return DialogMakeExternal.dialogResultFabric(geckoFile, originalContents);
    }
}
