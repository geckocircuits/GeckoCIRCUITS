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
package gecko.geckocircuits.circuit.losscalculation;

import gecko.core.allg.GeckoFile;
import gecko.geckocircuits.allg.MainWindow;
import java.io.FileNotFoundException;
import java.util.List;

public final class MainWindowLossFileAccessor implements LossFileAccessor {
    @Override
    public GeckoFile getFile(long hash) throws FileNotFoundException {
        return MainWindow._fileManager.getFile(hash);
    }

    @Override
    public List<GeckoFile> getFilesByExtension(String extension) {
        return MainWindow._fileManager.getFilesByExtension(extension);
    }

    @Override
    public void maintain(GeckoFile file) {
        MainWindow._fileManager.maintain(file);
    }

    @Override
    public void addFile(GeckoFile file) {
        MainWindow._fileManager.addFile(file);
    }

    @Override
    public String getOpenFileName() {
        return MainWindow.getOpenFileName();
    }
}
