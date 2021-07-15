/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.technokrat.gecko.geckocircuits.nativec;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Ignore;
import org.junit.rules.ExpectedException;


/**
 * Comment: The Native C Test Libraries were compiled on a Windows x86_64 machine 
 *          with GCC. To use them, you need to run the JUnit test under Windows 64bit.
 *          Otherwise, please recompile them and if necessary edit the fileName.
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCTest {
    private static final double DELTA_T = 5e-4;
    private static final double END_TIME = 1;
    private NativeCBlock _nativeCBlock;
    private NativeCLibraryFile _libFile;
    String _libFilePath, _libName;
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    /**
     * 
     * @param fileName  Name of Native Library
     * @return the absolute Path to the Native Library in the test directory
     */
    public String constructAbsolutPath(String fileName) {
        String absPath = new File(".").getAbsolutePath(); // get current directory
        if (absPath.endsWith(".")) {    // check for "."
            absPath = absPath.substring(0, absPath.length() - 1);
        }
        if (absPath.endsWith(File.separator)) {    // check for path ending (with "\" or without)
            absPath = absPath.substring(0, absPath.length() - 1);
        }
        absPath = absPath + File.separator
                + "src" + File.separator
                + "test" + File.separator
                + "java" + File.separator
                + "ch" + File.separator
                + "technokrat" + File.separator
                + "gecko" + File.separator
                + "geckocircuits" + File.separator
                + "nativec" + File.separator
                + "testJNI_DLL" + File.separator
                + fileName;
        return absPath;
    }
    
    @Before
    public void setUp() {
        try {
            _libName = "libtestJNI_DLL.dll";
            // construct an absolute file path to the test library
            // this is needed for the System.load() to work
            _libFilePath = constructAbsolutPath(_libName);
            _libFile = new NativeCLibraryFile(_libFilePath);
        } catch (Exception exc) {
            Assert.fail(exc.getMessage());
        }
    }
    
    @Test
    public void testNativeCLibraryFile_NotFound() throws FileNotFoundException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("Could not find Library File");
        NativeCLibraryFile testLibFile = new NativeCLibraryFile( "..\\.dll");
    }
    
    @Test
    public void testNativeCLibraryFile_Found() {
        NativeCLibraryFile testLibFile;
        try {
            testLibFile = new NativeCLibraryFile(_libFilePath);
            Assert.assertNotNull(testLibFile);
            Assert.assertNotNull(testLibFile.getFile());
            Assert.assertNotNull(testLibFile.getFileName());
        } catch (FileNotFoundException exc) {
            Assert.fail("Test File was not found!");
        }
    }
    
    @Test
    @Ignore
    public void testLoadAndExecuteNativeLibrary() {
        _nativeCBlock = new NativeCBlock();
        double[][] testInput = {{1, 2, 3, 4, 5}};
        double[][] testOutput = {{0, 0, 0}};
        Assert.assertNotNull(_nativeCBlock); 
        try {
            _nativeCBlock.loadLibraries(_libFilePath);
            for (double time = 0; time < END_TIME; time+=DELTA_T) {
                _nativeCBlock.calculateYOUT(time, DELTA_T, testInput, testOutput);
                double tmpOut = 0;
                for (int i = 0; i < testOutput.length; i++) {
                    tmpOut = tmpOut + testInput[0][i];
                    Assert.assertEquals(testOutput[0][i], tmpOut, 1e-6);
                }
            }
            _nativeCBlock.unloadLibraries();
            Assert.assertNull(_nativeCBlock._customCClassLoader);
            _nativeCBlock = null;
            Assert.assertNull(_nativeCBlock);
        } catch (Exception exc) {
            Assert.fail(exc.getMessage());
        }
    }
    
    
    @Test
    @Ignore
    public void testLoadAndExecuteNLAgain() {
        // execute again with same library
        testLoadAndExecuteNativeLibrary();
        // test with different library
        String testLib2 = "libtestJNI_DLL2.dll";
        _libFilePath = constructAbsolutPath(testLib2);
        Assert.assertNotNull(_libFilePath);
        testLoadAndExecuteNativeLibrary();
    }
}
