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

import java.io.FileNotFoundException;
import java.util.*;

/**
 *
 * @author anstupar This class keeps track of all the GeckoFile objects added to a model
 */
public final class GeckoFileManager {

    public final Set<GeckoFile> _allAvailableFiles = new LinkedHashSet<GeckoFile>();
    
    //create an empty GeckoFileManager
    public GeckoFileManager() {
        // nothing todo for this constructor
    }

    //create a GeckoFileManager with a list of GeckoFiles - when loading from .ipes file
    public GeckoFileManager(final List<GeckoFile> files) {
        if (files != null && !files.isEmpty()) {
            for (GeckoFile file : files) {
                _allAvailableFiles.add(file);
            }
        }
    }

    //get a file based on hash value
    public GeckoFile getFile(final long hash) throws FileNotFoundException {                        
        for (GeckoFile tmpFile : _allAvailableFiles) {            
            if (tmpFile.getHashValue() == hash) {
                return tmpFile;
            }
        }
                
        throw new FileNotFoundException("Desired file not found!");
    }

    //return all files with a specific extension
    public List<GeckoFile> getFilesByExtension(final String extension) {
        final List<GeckoFile> filesOfThisExtension = new ArrayList<GeckoFile>();
        String fileExt;


        //iterate through the list and add those files with this extension to the returned list
        for (GeckoFile file : _allAvailableFiles) {
            fileExt = file.getExtension();
            if (fileExt.equals(extension)) {
                filesOfThisExtension.add(file);
            }
        }

        return filesOfThisExtension;
    }

    //check whether this file has any users left, and remove it if it doesn't
    public void maintain(final GeckoFile file) {
        maintain(file.getHashValue());
    }

    public void maintain(final long fileHash) {
        try {
            final GeckoFile file = getFile(fileHash);
            //otherwise it is already removed
            if (file != null && file.noOfUsers() == 0) {                
                _allAvailableFiles.remove(getFile(fileHash));                
            }
        } catch (Exception ex) {
            System.err.println("GeckoFile not found: " + ex.getMessage());            
        }
    }

    public void removeUnusedFiles() {
        for (GeckoFile file : _allAvailableFiles) {
            maintain(file.getHashValue());
        }

    }

    //write all the GeckoFiles to the .ipes file
    public void exportASCII(final StringBuffer ascii) {
        ascii.append("\nGeckoFileManager");
        ascii.append("\n<GeckoFileManager>");
        
        for (GeckoFile file : _allAvailableFiles) {
            file.exportASCII(ascii);
        }
        ascii.append("\n<\\GeckoFileManager>");
    }

    //add a file
    public void addFile(final GeckoFile file) {
        _allAvailableFiles.add(file);
    }

    //save all the files to be internal - for applet mode
    public void exportASCIIApplet(final StringBuffer ascii) {
        ascii.append("\nGeckoFileManager");
        ascii.append("\n<GeckoFileManager>");        
        for (GeckoFile file : _allAvailableFiles) {
            file.exportASCIIApplet(ascii);
        }
        ascii.append("\n<\\GeckoFileManager>");
    }

    //rework all relative paths (i.e. we change location of file with "save as" or save the file for the first time
    public void recomputeRelativePaths(final String absModelPath) {        
        for (GeckoFile file : _allAvailableFiles) {
            file.recomputeRelativePath(absModelPath);
        }
    }
}
