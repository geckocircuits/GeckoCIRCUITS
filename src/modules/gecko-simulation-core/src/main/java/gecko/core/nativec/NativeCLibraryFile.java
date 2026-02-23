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

package gecko.core.nativec;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Wrapper class for native library files with existence checking and timestamp tracking.
 *
 * <p>Manages native library file references, validates file existence, and tracks
 * file modification timestamps for revision control.
 *
 * @author DIEHL Controls Ricardo Richter
 * @since Core Module Extraction Sprint - Phase 2
 */
public final class NativeCLibraryFile {
    private long _timeStamp;
    private String _libPathName;
    private File _libFile;

    public NativeCLibraryFile () {
        // no-op
    }

    public NativeCLibraryFile (final String fileName) throws FileNotFoundException {
        setFile(fileName);
    }

    public NativeCLibraryFile (final File file) throws FileNotFoundException {
        setFile(file);
    }

    public String getFileName () {
        return _libPathName;
    }

    public void setFile (final File file) throws FileNotFoundException {
        if (file != null) {
            if (!file.exists()) {
                throw new FileNotFoundException("Could not find Library File " + file.getAbsolutePath());
            } else {
                _libFile = file;
                _libPathName = _libFile.getAbsolutePath();
                _timeStamp = _libFile.lastModified();
            }
        }
    }

    public void setFile (final String fileName) throws FileNotFoundException {
        _libFile = new File(fileName);
        if (!_libFile.exists()) {
            throw new FileNotFoundException("Could not find Library File " + fileName);
        } else {
            _libPathName = _libFile.getAbsolutePath();
            _timeStamp = _libFile.lastModified();
        }
    }

    public void setFile () {
        _libFile = null;
        _libPathName = null;
        _timeStamp = 0;
    }

    public File getFile() {
        return _libFile;
    }

    public File savegetFile () throws FileNotFoundException {
        if (_libFile == null || _libPathName == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            return _libFile;
        }
    }

    public String savegetFileName () throws FileNotFoundException {
        if (_libFile == null || _libPathName == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            return _libPathName;
        }
    }

    /**
     * The TimeStamp can be used in future as an easy implementation for
     * revision control.
     * @return timestamp of last file modification
     */
    public long getTimeStamp () {
        return _timeStamp;
    }

    public boolean updateTimeStamp () throws FileNotFoundException {
        long newTimeStamp;
        if (_libFile == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            if (!_libFile.exists()) {
                throw new FileNotFoundException("Could not find Library File " + _libPathName);
            }
            newTimeStamp = _libFile.lastModified();
            if (newTimeStamp != _timeStamp) {
                _timeStamp = newTimeStamp;
                return true;
            }
        }
        return false;
    }
}
