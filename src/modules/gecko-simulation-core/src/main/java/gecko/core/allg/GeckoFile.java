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

import gecko.core.circuit.ComponentIdentifiable;
import gecko.core.circuit.TokenMap;
import gecko.core.io.SerializationUtils;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
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
    /** The separator used in the stored paths. */
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
    private boolean _numericOwnerIds = true;
    private boolean _explicitInternalStorage = true;
    private final String _extension;
    private static final int HASH_CONST_BOUND = 10164;
    private static final Random RANDOM = new Random();
    private AbstractStorageStrategy _storageStrategy;
    private long _lastDiskModification = -1;
    private final ExternalStorageConverter _converter;

    public enum StorageType {
        // don't change the order, ordinal is used!

        INTERNAL, EXTERNAL;

        private static StorageType fromOrdinal(final int ord) {
            for (StorageType tmp : values()) {
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
        this(fileName, modelFileName, contents, null);
    }

    public static GeckoFile createEmbeddedPwl(File fileName, String modelFileName, byte[] contents)
            throws FileNotFoundException {
        var file = new GeckoFile(fileName, modelFileName, contents);
        file._sourceOwnedPwl = true;
        return file;
    }

    /**
     * create an initial internal file with custom external storage converter
     * @param fileName the file
     * @param modelFileName the model file name
     * @param contents the file contents
     * @param converter the external storage converter (null uses default)
     * @throws FileNotFoundException
     */
    public GeckoFile(File fileName, final String modelFileName, final byte[] contents,
                     ExternalStorageConverter converter) throws FileNotFoundException {
        this._converter = converter != null ? converter : new DefaultExternalStorageConverter();
        setStorageStrategy(StorageType.INTERNAL);
        _file = fileName;
        _separator = File.separator;
        _absolutePath = _file.getAbsolutePath();
        _relativePath = getRelativePath(_absolutePath, modelFileName);
        _extension = _absolutePath.substring(_absolutePath.lastIndexOf('.'));
        _fileContents = contents != null ? contents.clone() : null;
        _hash = generateHashCode();
    }

    //same as above, but with File object passed instead of path
    public GeckoFile(final File file, final StorageType storageType, final String modelFileName) throws FileNotFoundException {
        this(file, storageType, modelFileName, null);
    }

    /**
     * Primary constructor with custom external storage converter
     * @param file the file
     * @param storageType the storage type (INTERNAL or EXTERNAL)
     * @param modelFileName the model file name
     * @param converter the external storage converter (null uses default)
     * @throws FileNotFoundException
     */
    public GeckoFile(final File file, final StorageType storageType, final String modelFileName,
                     ExternalStorageConverter converter) throws FileNotFoundException {
        this._converter = converter != null ? converter : new DefaultExternalStorageConverter();
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
    public GeckoFile(final TokenMap tokenMap, final List<?> allComponents) throws FileNotFoundException {
        this(tokenMap, allComponents, null);
    }

    /**
     * constructor when GeckoFile object is reconstructed from ipes file with custom converter
     *
     * @param tokenMap the token map from .ipes file
     * @param allComponents list of components for resolving old-style string IDs
     * @param converter the external storage converter (null uses default)
     * @throws FileNotFoundException
     */
    public GeckoFile(final TokenMap tokenMap, final List<?> allComponents, ExternalStorageConverter converter) throws FileNotFoundException {
        this(tokenMap, allComponents, GlobalFilePathes.DATNAM, converter);
    }

    /** Reconstructs a resource relative to the circuit file that contains its saved data. */
    public GeckoFile(final TokenMap tokenMap, final List<?> allComponents, final String modelFileName,
                     ExternalStorageConverter converter) throws FileNotFoundException {
        this(tokenMap, allComponents, modelFileName, converter, false);
    }

    public GeckoFile(final TokenMap tokenMap, final List<?> allComponents, final String modelFileName,
                     ExternalStorageConverter converter, boolean pwlResource) throws FileNotFoundException {
        this(tokenMap, allComponents, modelFileName, converter,
                pwlResource ? gecko.core.io.SavedPwlResources.embedded(tokenMap.getLines()) : null);
    }

    private boolean _sourceOwnedPwl;

    public GeckoFile(final TokenMap tokenMap, final List<?> allComponents, final String modelFileName,
                     ExternalStorageConverter converter, gecko.core.io.SavedPwlResources.Embedded pwl)
            throws FileNotFoundException {
        _explicitInternalStorage = tokenMap.containsToken("isExternal")
                && tokenMap.getLineString("isExternal", "").strip().matches("isExternal\\s+0")
                && java.util.Arrays.stream(tokenMap.getLines()).map(String::stripLeading)
                        .filter(row -> row.matches("isExternal(?:\\s.*)?")).count() == 1;
        this._converter = converter != null ? converter : new DefaultExternalStorageConverter();
        final String savedSeparator = tokenMap.readDataLine("fileSep", File.separator);
        _separator = File.separator;
        _hash = pwl == null ? tokenMap.readDataLine("hashValue", -1L) : pwl.resourceId();


        setStorageStrategy(pwl == null ? StorageType.fromOrdinal(tokenMap.readDataLine(
                "isExternal", StorageType.EXTERNAL.ordinal())) : StorageType.INTERNAL);

        _absolutePath = tokenMap.readDataLine("absPath", _absolutePath).replace(savedSeparator, _separator);
        _relativePath = tokenMap.readDataLine("relPath", _relativePath).replace(savedSeparator, _separator);

        if (pwl != null) {
            _explicitInternalStorage = true;
            _sourceOwnedPwl = true;
            _numericOwnerIds = true;
            _usageList.add(pwl.ownerId());
            _fileContents = pwl.bytes().clone();
            _extension = ".pwl";
            _file = new File(_absolutePath);
            return;
        }

        String[] savedOwners = new String[0];
        int numericOwnerLines = 0;
        for (String row : tokenMap.getLines()) {
            if (row.stripLeading().matches("usageList\\[\\](?:\\s.*)?")) {
                numericOwnerLines++;
            }
        }
        _numericOwnerIds = numericOwnerLines == 1
                && tokenMap.getLineString("usageList[]", "").strip().matches("usageList\\[\\]\\s+/?-?[0-9]+");
        if (tokenMap.containsToken("usageList[]")) {
            savedOwners = tokenMap.readDataLine("usageList[]", savedOwners);
        } else {
            TokenMap ownerBlock = tokenMap.getBlockTokenMap("<usageList>");
            if (ownerBlock != null) savedOwners = ownerBlock.getLines();
        }
        for (String name : savedOwners) {
            try {
                long identifier = Long.parseLong(name.strip());
                _numericOwnerIds &= _usageList.add(identifier);
            } catch (NumberFormatException ex) { // this is due to a version change from 1.61 to 1.62
                _numericOwnerIds = false;
                for (Object comp : allComponents) {
                    if (comp instanceof ComponentIdentifiable) {
                        ComponentIdentifiable block = (ComponentIdentifiable) comp;
                        if (block.getStringID().equals(name)) {
                            _usageList.add(block.getUniqueObjectIdentifier());
                        }
                    }
                }

            }
        }

        if (_storageStrategy.getStorageType() == StorageType.INTERNAL) {
            String contentsKey = tokenMap.containsToken("fileContents[]") ? "fileContents[]" : "fileContents";
            _fileContents = tokenMap.readDataLine(contentsKey, _fileContents);
        }

        _extension = _absolutePath.substring(_absolutePath.lastIndexOf('.'));

        final File modelFile = modelFileName == null ? null : new File(modelFileName).getAbsoluteFile();
        final File modelDirectory = modelFile == null ? null : modelFile.getParentFile();
        final File relativeFile = modelDirectory == null ? null : new File(modelDirectory, _relativePath);
        if (relativeFile != null && relativeFile.exists()) {
            _file = relativeFile;
            if (_storageStrategy.getStorageType() == StorageType.EXTERNAL) {
                _absolutePath = relativeFile.toPath().toAbsolutePath().normalize().toString();
            }
        } else {
            _file = new File(_absolutePath);
            if (_file.exists() && modelDirectory != null) {
                _relativePath = getRelativePath(_file.getAbsolutePath(), modelFile.getPath());
            }
        }

        //if the file still does not exist, throw an exception if the file is specified as external
        if (!_file.exists() && _storageStrategy.getStorageType() == StorageType.EXTERNAL) {
            throw new FileNotFoundException("Error: Specified external file with relative path: "
                    + _relativePath + "\n and absolute path:" + _absolutePath + "\n NOT FOUND!");
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

        try (FileInputStream inStream = new FileInputStream(_file)) {
            int offset = 0;
            while (offset < fileContents.length) {
                final int numRead = inStream.read(fileContents, offset, fileContents.length - offset);
                if (numRead < 0) {
                    break;
                }
                offset += numRead;
            }
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
                + RANDOM.nextInt(HASH_CONST_BOUND);


        return code;
    }

    public long getHashValue() {
        return _hash;
    }

    public boolean hasExplicitInternalStorage() {
        return _explicitInternalStorage && getStorageType() == StorageType.INTERNAL;
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

    /** Returns the saved identifiers of the components that use this file. */
    public List<Long> getOwnerIds() {
        return List.copyOf(_usageList);
    }

    public boolean isSoleNumericOwner(final long ownerId) {
        return _numericOwnerIds && _usageList.size() == 1 && _usageList.contains(ownerId);
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
        exportASCII(ascii, null);
    }

    public void exportASCII(final StringBuffer ascii, String targetModelPath) {
        ascii.append("\n<GeckoFile>");
        //----------------------------------------
        SerializationUtils.appendAsString(ascii.append("\nhashValue"), _hash);
        SerializationUtils.appendAsString(ascii.append("\nabsPath"), _absolutePath);
        String path = targetModelPath == null ? _relativePath : getRelativePath(_absolutePath, targetModelPath);
        SerializationUtils.appendAsString(ascii.append("\nrelPath"), path);
        SerializationUtils.appendAsString(ascii.append("\nfileSep"), _separator);
        SerializationUtils.appendAsString(ascii.append("\nisExternal"), _storageStrategy.getStorageType().ordinal());
        if (_sourceOwnedPwl) {
            ascii.append("\nusageList[] ");
            for (Long userID : _usageList) {
                ascii.append('/').append(userID);
            }
        } else {
            SerializationUtils.appendAsString(ascii.append("\nusageList"), _usageList.toArray(new Long[0]));
            ascii.append("\n<usageList>");
            for (Long userID : _usageList) {
                ascii.append('\n');
                ascii.append(userID);
            }
            ascii.append("\n<\\usageList>");
        }

        if (_storageStrategy.getStorageType() == StorageType.INTERNAL) {
            SerializationUtils.appendAsString(ascii.append("\nfileContents[]"), _fileContents);
        }

        ascii.append("\n<\\GeckoFile>");
    }

    /**
     * for saving in applet mode
     *
     * @param ascii
     */

    /**
     * get the file contents as a String
     *
     * @return
     */
    public String getContentsString() {
        String fileContents;
        if (_storageStrategy.getStorageType() == StorageType.EXTERNAL) {
            fileContents = new String(readFileIntoMemory(), StandardCharsets.UTF_8);
        } else {
            fileContents = new String(_fileContents, StandardCharsets.UTF_8);
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
        return new InputStreamReader(getInputStream(), StandardCharsets.UTF_8);
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

    private final class InternalStrategy extends AbstractStorageStrategy {

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

    private final class ExternalStrategy extends AbstractStorageStrategy {

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

            final String result = _converter.promptForExternalPath(GeckoFile.this, originalContents);
            if (result == null || !new File(result).exists()) { // cancel pressed or external file is not existing!
                setStorageType(StorageType.INTERNAL);
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

    /**
     * Default implementation of ExternalStorageConverter that uses reflection to call
     * DialogMakeExternal from the GUI layer. This allows GeckoFile to work in both
     * GUI and headless environments.
     */
    private static class DefaultExternalStorageConverter implements ExternalStorageConverter {
        @Override
        public String promptForExternalPath(GeckoFile geckoFile, byte[] originalContents) {
            // Use reflection to avoid hard dependency on GUI class
            try {
                Class<?> dialogClass = Class.forName("gecko.geckocircuits.allg.DialogMakeExternal");
                Method factory = dialogClass.getMethod("dialogResultFabric", GeckoFile.class, byte[].class);
                return (String) factory.invoke(null, geckoFile, originalContents);
            } catch (Exception e) {
                throw new UnsupportedOperationException(
                    "GUI not available - provide ExternalStorageConverter implementation", e);
            }
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
