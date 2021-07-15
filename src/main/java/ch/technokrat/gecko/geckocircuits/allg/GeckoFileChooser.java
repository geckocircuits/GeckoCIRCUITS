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
package ch.technokrat.gecko.geckocircuits.allg;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class GeckoFileChooser {
    
    
    private final JFileChooser _fileChooser;
    private static File _lastUsedDirectory = null;
    private final String _fileEnding;
    private FileChooserResult _userResult = FileChooserResult.CANCEL;

    // private - use the fabric methods!
    private GeckoFileChooser(final String ending, final File currentDirectory,
            final String fileDescription) {
        _fileChooser = new JFileChooser(currentDirectory);
        
        if (ending != null) {
            assert ending.startsWith(".");

            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(final File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    if (file.getName().endsWith(ending)) {
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public String getDescription() {
                    return fileDescription;
                }
            };
            _fileChooser.setAcceptAllFileFilterUsed(false);            
            _fileChooser.addChoosableFileFilter(filter);

        }
        _fileChooser.setMultiSelectionEnabled(false);
        _fileEnding = ending;

    }

    public enum FileChooserResult {

        OK,
        CANCEL
    };

    public FileChooserResult getUserResult() {
        return _userResult;
    }

    public File getFileWithCheckedEnding() {
        if(_fileEnding == null) {
            return _fileChooser.getSelectedFile();
        }
        
        String fileName = _fileChooser.getSelectedFile().getAbsolutePath();
        if (!fileName.endsWith(_fileEnding)) {
            fileName += (_fileEnding);
        }
        return new File(fileName);
    }
    
    public static GeckoFileChooser createOpenFileChooser(final String ending,
            final String fileDescription, final Component windowParent, final File currentDirectory) {
        final File checkedCurrentDirectory = calculateCheckedCurrentDirectory(currentDirectory);                
        final GeckoFileChooser returnValue = new GeckoFileChooser(ending, checkedCurrentDirectory, fileDescription);
        final int result = returnValue._fileChooser.showOpenDialog(windowParent);
        returnValue.processResult(result);        
        return returnValue;
    }
    
    public static GeckoFileChooser createSimpleOpenFileChooser(final String ending,
            final Component windowParent) {
        return createOpenFileChooser(ending, ending, windowParent, null);        
    }

    public static GeckoFileChooser createSimpleSaveFileChooser(final String ending, final Component windowParent) {
        return createSaveFileChooser(ending, ending, windowParent, null);
    }

    public static GeckoFileChooser createSaveFileChooser(final String ending,
            final String fileDescription, final Component windowParent, final File currentDirectory) {        

        File checkedCurrentDirectory = calculateCheckedCurrentDirectory(currentDirectory);        
        final GeckoFileChooser returnValue = new GeckoFileChooser(ending, checkedCurrentDirectory, fileDescription);
        final int result = returnValue._fileChooser.showSaveDialog(windowParent);
        returnValue.processResult(result);        
        return returnValue;
    }
    
    public static File calculateCheckedCurrentDirectory(final File currentDirectory) {
        File checkedCurrentDirectory = currentDirectory;
        
        if (currentDirectory == null || !currentDirectory.exists()) {
            if (_lastUsedDirectory != null && _lastUsedDirectory.exists()) {
                checkedCurrentDirectory = _lastUsedDirectory;
            } else {
                checkedCurrentDirectory = new File(GlobalFilePathes.DATNAM);
            }
        }
        return checkedCurrentDirectory;
    }

    private void processResult(int result) {
        switch (result) {
            case JFileChooser.CANCEL_OPTION:
                _userResult = FileChooserResult.CANCEL;
                break;
            default:
                _userResult = FileChooserResult.OK;
                File possibleLastSavedParent = getFileWithCheckedEnding().getParentFile();
                if (possibleLastSavedParent != null && possibleLastSavedParent.isDirectory()) {
                    _lastUsedDirectory = possibleLastSavedParent;
                }
        }
    }
    
}
