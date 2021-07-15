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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.IDStringDialog;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import java.io.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anstupar This class is for handling all extra files that are used
 * inside Gecko e.g. extra files for java block, loss model files, (in the
 * future) nonlinear characteristic files
 */
public final class GeckoFile {

    /**
     * the java File object for this file, i.e. the actual file
     */
    private File _file;
    /**
     * the absolute pathname as specified by the user when this file was created
     */
    private String _absolutePath;
    /**
     * the pathname of this file relative to the path of the model files (.ipes)
     * this file is associated with
     */
    private String _relativePath;
    /**
     * the file separator on the OS used to add this file to the model; used for
     * checking whether path stored in file differs from current OS
     */
    private String _separator;
    /**
     * byte array which contains the file contents if they have been read from
     * ipes file
     */
    private byte[] _fileContents;
    /**
     * we need a unique identifer to save to ipes file when different elements
     * point at a Gecko file
     */
    private final long _hash;
    /**
     * model blocks which use this file
     */
    private final Set<Long> _usageList = new LinkedHashSet<Long>();
    private final String _extension;
    private static final double HASH_CONST1 = 5.0;
    private static final double HASH_CONST2 = 2032.6;
    private static final String[] EMPTY_STRING = new String[0];
    private AbstractStorageStrategy _storageStrategy;
    private long _lastDiskModification = -1;

    public enum StorageType {
        // don't change the order, ordinal is used!

        INTERNAL, EXTERNAL;

        private static StorageType fromOrdinal(final int ord) {
            for (StorageType tmp : StorageType.values()) {
                if (tmp.ordinal() == ord) {
                    return tmp;
                }
            }
            assert false;
            return EXTERNAL;
        }
    }

    private void setStorageStrategy(final StorageType storageType) {
        switch (storageType) {
            case INTERNAL:
                _storageStrategy = new InternalStrategy();
                return;
            case EXTERNAL:
                _storageStrategy = new ExternalStrategy();
                return;
            default:
                assert false;
                break;
        }
    }
    
    /**
     * create an initial internal file (convertion from old GeckoCIRCUITS versions)
     * @param modelFileName
     * @throws FileNotFoundException 
     */
    public GeckoFile(File fileName, final String modelFileName, final byte[] contents) throws FileNotFoundException {
        setStorageStrategy(StorageType.INTERNAL);
        _file = fileName;
        _separator = File.separator;
        _absolutePath = _file.getAbsolutePath();        
        _relativePath = getRelativePath(_absolutePath, modelFileName);
        _extension = _absolutePath.substring(_absolutePath.lastIndexOf('.'));        
        _fileContents = contents;        
        _hash = generateHashCode();
    }

    //same as above, but with File object passed instead of path
    public GeckoFile(final File file, final StorageType storageType, final String modelFileName) throws FileNotFoundException {
        setStorageStrategy(storageType);
        _file = file;
        _separator = File.separator;

        if (!_file.exists()) {
            throw new FileNotFoundException("GeckoFile constructor: Supplied File with path:"
                    + _file.getAbsolutePath() + " does not exist.");
        }

        try {
            _absolutePath = _file.getCanonicalPath();
        } catch (Exception e) {
            final String errorMessage = e.toString() + "GeckoFile constructor: could not get canonical "
                    + "path of specified file. Using getAbsolutePath instead";
            Logger.getLogger(GeckoFile.class.getName()).log(Level.SEVERE, errorMessage);
            _absolutePath = _file.getAbsolutePath();
        }
        
        _relativePath = getRelativePath(_absolutePath, modelFileName); 
        _extension = _absolutePath.substring(_absolutePath.lastIndexOf('.'));

        if (storageType == StorageType.INTERNAL) {
            //read in file contents to byte array
            _fileContents = readFileIntoMemory();
        } else {
            _lastDiskModification = _file.lastModified();
        }

        _hash = generateHashCode();
    }

    /**
     * constructor when GeckoFile object is reconstructed from ipes file
     *
     * @param allComponents this parameter could probably be removed in the
     * future. At the moment, I think this is the only possiblity to get the old
     * GeckoFile stuff running in the new Software release.
     * @throws FileNotFoundException
     */
    public GeckoFile(final TokenMap tokenMap, final List<AbstractCircuitSheetComponent> allComponents) throws FileNotFoundException {
        _separator = tokenMap.readDataLine("fileSep", _separator);        
        _hash = tokenMap.readDataLine("hashValue", -1);
        
        
        setStorageStrategy(StorageType.fromOrdinal(tokenMap.readDataLine(
                "isExternal", StorageType.EXTERNAL.ordinal())));

        _absolutePath = tokenMap.readDataLine("absPath", _absolutePath);
        _relativePath = tokenMap.readDataLine("relPath", _relativePath);

        for (String name : tokenMap.readDataLine("usageList[]", new String[0])) {
            try {
                long identifier = Long.parseLong(name);
                _usageList.add(identifier);
            } catch (NumberFormatException ex) { // this is due to a version change from 1.61 to 1.62
                for (AbstractCircuitSheetComponent comp : allComponents) {
                    if (comp instanceof AbstractBlockInterface) {
                        AbstractBlockInterface block = (AbstractBlockInterface) comp;
                        if (block.getStringID().equals(name)) {
                            _usageList.add(block.getUniqueObjectIdentifier());
                        }
                    }
                }

            }
        }

        if (_storageStrategy.getStorageType() == StorageType.INTERNAL) {
            _fileContents = tokenMap.readDataLine("fileContents[]", _fileContents);
        }

        _extension = _absolutePath.substring(_absolutePath.lastIndexOf('.'));

        //check if we are in a different OS then the one in which the files were added to the model
        //Windows file names use backslashes - giving them "as is" to the replaceAll function causes an exception becuase regex uses backslashes as an escape character
        //therefore we must double the backslash - \\ - i.e. change from "\\" to "\\\\"
        String separatorSequenceToReplace;
        if (_separator.equals("\\")) {
            separatorSequenceToReplace = "\\\\";
        } else {
            separatorSequenceToReplace = _separator;
        }
        //we must do the same with the File.separator
        //forward slashes (UNIX filenames) should not be a problem
        String separatorSequenceToInsert;
        if (File.separator.equals("\\")) {
            separatorSequenceToInsert = "\\\\";
        } else {
            separatorSequenceToInsert = File.separator;
        }
        final String absPathToUse = _absolutePath.replaceAll(separatorSequenceToReplace, separatorSequenceToInsert);
        final String relPathToUse = _relativePath.replaceAll(separatorSequenceToReplace, separatorSequenceToInsert);
        final int lastSeparatorIndex = GlobalFilePathes.DATNAM.lastIndexOf(File.separator);
        //now first try the relative path
        if(lastSeparatorIndex >=0) {
            _file = new File(GlobalFilePathes.DATNAM.substring(0, lastSeparatorIndex) + File.separator + relPathToUse);
        } else {
            _file = new File("fileNotFound");
        }
        

        //if cannot find file, use the absolute path
        if (!_file.exists()) {
            _file = new File(absPathToUse);
            if (_file.exists()) {
                //file exists, recompute relative path
                _relativePath = getRelativePath(GlobalFilePathes.DATNAM, absPathToUse);
                //to keep things consistent (perhaps we changed file separator), set the stored _absolutePath field to the used absolute path
                _absolutePath = absPathToUse;
            }
        }

        //if the file still does not exist, throw an exception if the file is specified as external
        if (!(_file.exists()) && _storageStrategy.getStorageType() == StorageType.EXTERNAL) {
            throw new FileNotFoundException("Error: Specified external file with relative path: "
                    + relPathToUse + "\n and absolute path:" + absPathToUse + "\n NOT FOUND!");
        }
    }

    /**
     * we need two absolute paths here - otherwise doesn't work
     *
     * @param absFilePath
     * @param absModelPath
     * @return
     */
    private String getRelativePath(final String absFilePath, final String givenAbsPath) {
        String absModelPath = givenAbsPath;

        //if absModelPath is "Untitled" it is an unsaved file -> save with respect to current user directory
        if ("Untitled".equals(absModelPath)) {
            absModelPath = System.getProperty("user.dir");
        }

        //absModelPath points to a file - should be modified to point to a directory
        final int lastSeparatorIndex = absModelPath.lastIndexOf(File.separator);        
        absModelPath = absModelPath.substring(0, lastSeparatorIndex); 
        //System.out.println(absModelPath);

        //this is because separator '\' does strange things with String.split()
        String regexCharacter = File.separator;
        if (File.separatorChar == '\\') {
            regexCharacter = "\\\\";
        }

        final String[] fromSplit = absFilePath.split(regexCharacter);
        final String[] toSplit = absModelPath.split(regexCharacter); //relative file name is relative to the model (.ipes) path
        return findRelativePath(fromSplit, toSplit, absFilePath);
    }

    private static String findRelativePath(final String[] fromSplit, final String[] toSplit, final String absFilePath) {
        StringBuffer result = new StringBuffer();

        final int common = findCommonPath(toSplit, fromSplit);
        //first check if file path is subset of modelpath        
        if ((fromSplit.length >= toSplit.length) && (common == toSplit.length)) {
            for (int i = common; i < fromSplit.length; i++) {
                result.append(fromSplit[i]);
                if (i < fromSplit.length - 1) {
                    result.append(File.separatorChar);
                }
            }
        } else { //if not, then have to go back some directories

            //if there is nothing common, there is no way to construct a relative path - the relative path is then absolute
            if (common == 0) {
                return absFilePath;
            } else {
                result = new StringBuffer();

                //go up from the TO path to common ground (the path our relative path is relative TO)
                for (int i = common; i < toSplit.length; i++) {
                    if (i > common) {
                        result.append(File.separatorChar).append("..");
                    } else {
                        result.append("..");
                    }
                }

                //go down to the FROM path
                for (int i = common; i < fromSplit.length; i++) {
                    result.append(File.separatorChar).append(fromSplit[i]);
                }
            }
        }
        return result.toString();
    }

    private static int findCommonPath(final String[] toSplit, final String[] fromSplit) {
        int common = 0;
        boolean diverged = false;
        while (!diverged) {
            if ((common < fromSplit.length) && (common < toSplit.length)) {
                if (fromSplit[common].equals(toSplit[common])) {
                    common++;
                } else {
                    diverged = true;
                }
            } else {
                diverged = true;
            }
        }
        return common;
    }

    private byte[] readFileIntoMemory() {
        final byte[] fileContents = new byte[(int) _file.length()];
        _lastDiskModification = _file.lastModified();

        try {
            final FileInputStream inStream = new FileInputStream(_file);
            int offset = 0;
            while (offset < fileContents.length) {
                final int numRead = inStream.read(fileContents, offset, fileContents.length - offset);
                if (numRead < 0) {
                    break;
                }
                offset += numRead;
            }
            inStream.close();
        } catch (Exception e) {
            final String errorMessage = "GeckoFile read in file contents: cannot find file. " + e.toString();
            Logger.getLogger(GeckoFile.class.getName()).log(Level.SEVERE, errorMessage);
        }

        return fileContents;
    }

    /**
     * generate a unique identifier for this GeckoFile object which can be saved
     * to file
     *
     * @return
     */
    private long generateHashCode() {
        long code;

        code = _absolutePath.hashCode() + 2 * _relativePath.hashCode()
                + (int) (HASH_CONST1 * Math.random() * HASH_CONST2 * Math.random());
        
       
        return code;
    }

    public long getHashValue() {
        return _hash;
    }

    /**
     * function which sets a particular model block as a user of this file
     * CAREFUL - probably should be called along with removeUser when any block
     * is renamed
     *
     * @param blockID
     */
    public void setUser(final long uniqueObjectIdentifier) {
        _usageList.add(uniqueObjectIdentifier);
    }

    /**
     * function which removes a particular model block from the user list of
     * this file CAREFUL - probably should be called along with removeUser when
     * any block is renamed
     *
     * @param blockID
     */
    public void removeUser(final long blockID) {
        _usageList.remove((Long) blockID);
    }

    /**
     * function which gives the number of users of this file
     *
     * @return
     */
    public int noOfUsers() {
        return _usageList.size();
    }

    /**
     * function to write GeckoFile object to .ipes file
     *
     * @param ascii
     */
    public void exportASCII(final StringBuffer ascii) {
        ascii.append("\n<GeckoFile>");
        //----------------------------------------
        DatenSpeicher.appendAsString(ascii.append("\nhashValue"), _hash);
        DatenSpeicher.appendAsString(ascii.append("\nabsPath"), _absolutePath);
        DatenSpeicher.appendAsString(ascii.append("\nrelPath"), _relativePath);
        DatenSpeicher.appendAsString(ascii.append("\nfileSep"), _separator);
        DatenSpeicher.appendAsString(ascii.append("\nisExternal"), _storageStrategy.getStorageType().ordinal());
        DatenSpeicher.appendAsString(ascii.append("\nusageList"), _usageList.toArray(new Long[_usageList.size()]));

        ascii.append("\n<usageList>");
        for (Long userID : _usageList) {
            ascii.append('\n');
            ascii.append(userID);
            //System.out.println("user: " + user);
        }
        ascii.append("\n<\\usageList>");

        if (_storageStrategy.getStorageType() == StorageType.INTERNAL) {
            DatenSpeicher.appendAsString(ascii.append("\nfileContents"), _fileContents);
        }

        ascii.append("\n<\\GeckoFile>");
    }

    /**
     * for saving in applet mode
     *
     * @param ascii
     */
    public void exportASCIIApplet(final StringBuffer ascii) {
        final AbstractStorageStrategy oldIsExternalValue = _storageStrategy;
        _storageStrategy = new InternalStrategy();
        if (oldIsExternalValue.getStorageType() == StorageType.EXTERNAL) {
            _fileContents = readFileIntoMemory();            
        }        
        exportASCII(ascii);
        _storageStrategy = oldIsExternalValue;
    }

    /**
     * get the file contents as a String
     *
     * @return
     */
    public String getContentsString() {
        String fileContents;
        if (_storageStrategy.getStorageType() == StorageType.EXTERNAL) {
            fileContents = new String(readFileIntoMemory());
        } else {
            fileContents = new String(_fileContents);
        }
        return fileContents;
    }

    /**
     * get the file contents as a byte array CAREFUL - not a copy like above -
     * editing return variable edits the _fileContents field in this object
     * (only applies to files stored internally)
     *
     * @return
     */
    public byte[] getContentsByte() {        
        return _storageStrategy.getContentsByte();
    }

    /**
     * get a copy of the file contents as a byte array
     *
     * @return
     */
    public byte[] getContentsByteCopy() {
        return _storageStrategy.getContentsByteCopy();
    }

    /**
     * method to set whether file is external or internal
     *
     * @param newStorageType
     * @throws FileNotFoundException
     */
    public void setStorageType(final StorageType newStorageType) throws FileNotFoundException {
        if (newStorageType == _storageStrategy.getStorageType()) { // nothing changed, so do nothing!            
            return;
        }
        byte[] originalContents = getContentsByte();
        setStorageStrategy(newStorageType);
        _storageStrategy.switchToNewType(originalContents);
    }

    /**
     * return an input stream for this file - pointing either to the actual
     * file, or the internally stored contents
     */
    public InputStream getInputStream() {
        return _storageStrategy.getInputStream();
    }

    public InputStreamReader getInputStreamReader() {
        return new InputStreamReader(getInputStream());
    }

    public BufferedReader getBufferedReader() {
        return new BufferedReader(getInputStreamReader());
    }

    public String getExtension() {
        return _extension;
    }

    /**
     * get file name (without file path)
     */
    public String getName() {
        return _absolutePath.substring(_absolutePath.lastIndexOf(_separator) + 1);
    }

    /**
     * function to recompute relative path with respect to .ipes file - e.g. if
     * file is "saved as" to another location or if the file is saved for the
     * first time
     *
     * @param absModelPath
     */
    public void recomputeRelativePath(final String absModelPath) {
        _relativePath = getRelativePath(_absolutePath, absModelPath);
    }

    @Override
    public String toString() {
        return _storageStrategy.toString();
    }

    public StorageType getStorageType() {
        return _storageStrategy.getStorageType();
    }

    public String getCurrentAbsolutePath() {
        return _storageStrategy.getCurrentAbsolutePath();
    }

    /**
     * update a file's contents without overwriting the GeckoFile object
     *
     * @param newFile
     * @param storageType
     *
     */
    public void update(final File newFile) {
        _file = newFile;
        
        switch(getStorageType()) {
            case INTERNAL:
                _fileContents = readFileIntoMemory();
                break;
            case EXTERNAL:
                _lastDiskModification = _file.lastModified();
                break;
            default:
                assert false;
        }                
    }

    /**
     * Get the last stored time stamp for file modification. This does not
     * update the time stamp - only returns the value from the last time it was
     * updated.
     *
     * @return the time stamp
     */
    public long getModificationTimeStamp() {
        return _lastDiskModification;
    }

    /**
     * Update the time stamp indicating when the file was last modified.
     *
     * @return the updated time stamp
     */
    public long checkModificationTimeStamp() {
        if (_storageStrategy.getStorageType() == StorageType.INTERNAL) {
            return _lastDiskModification;
        } else {
            _lastDiskModification = _file.lastModified();
            return _lastDiskModification;
        }
    }

    private abstract static class AbstractStorageStrategy {

        abstract StorageType getStorageType();

        abstract String getCurrentAbsolutePath();

        abstract InputStream getInputStream();

        abstract void switchToNewType(final byte[] originalContents) throws FileNotFoundException;

        abstract byte[] getContentsByte();

        abstract byte[] getContentsByteCopy();
    }

    private class InternalStrategy extends AbstractStorageStrategy {

        @Override
        StorageType getStorageType() {
            return StorageType.INTERNAL;
        }

        @Override
        String getCurrentAbsolutePath() {
            return _absolutePath;
        }

        @Override
        public String toString() {
            return _file.getName() + " [INTERNAL]";
        }

        @Override
        InputStream getInputStream() {
            return new ByteArrayInputStream(_fileContents);
        }

        @Override
        void switchToNewType(final byte[] originalContents) {
            _fileContents = readFileIntoMemory();
        }

        @Override
        byte[] getContentsByte() {
            return _fileContents;
        }

        @Override
        byte[] getContentsByteCopy() {
            final byte[] fileContentsCopy = new byte[_fileContents.length];
            System.arraycopy(_fileContents, 0, fileContentsCopy, 0, _fileContents.length);
            return fileContentsCopy;
        }
    }

    private class ExternalStrategy extends AbstractStorageStrategy {

        @Override
        StorageType getStorageType() {
            return StorageType.EXTERNAL;
        }

        @Override
        String getCurrentAbsolutePath() {
            return _file.getAbsolutePath();
        }

        @Override
        public String toString() {
            return _relativePath + " [EXTERNAL]";
        }

        @Override
        InputStream getInputStream() {
            try {
                return new FileInputStream(_file);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GeckoFile.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        @Override
        void switchToNewType(final byte[] originalContents) throws FileNotFoundException {

            final String result = DialogMakeExternal.dialogResultFabric(GeckoFile.this, originalContents);
            if (result == null || !(new File(result)).exists()) { // cancel pressed or external file is not existing!
                setStorageType(StorageType.INTERNAL);
                return;
            }            
        }

        @Override
        byte[] getContentsByte() {
            return readFileIntoMemory();
        }

        @Override
        byte[] getContentsByteCopy() {
            return readFileIntoMemory();
        }
    }
//    // test routine, do not remove!
//    public static void main(String[] args) {
//        try {
//            GeckoFile geckoFile = new GeckoFile(new File("/home/andreas/testFile.txt"), 
//                StorageType.EXTERNAL, "/home/andreas/test.ipes");
//            System.out.println("relative: " + geckoFile._relativePath);
//            geckoFile.setStorageType(StorageType.INTERNAL);
//            System.out.println("---------------");
//            geckoFile.setStorageType(StorageType.EXTERNAL);
//            System.out.println("relativeNew: " + geckoFile._relativePath);
//            System.out.println("finally storage type: " + geckoFile.getStorageType());
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(GeckoFile.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
}
