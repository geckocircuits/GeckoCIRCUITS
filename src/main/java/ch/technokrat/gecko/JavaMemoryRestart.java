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
package ch.technokrat.gecko;

import ch.technokrat.gecko.geckocircuits.allg.GetJarPath;
import java.io.*;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class JavaMemoryRestart {

    private static final int MINIMUM_MEM_MB = 128;
    private static final int MEGA_BYTE = 1098300;
    // sometimes, the get-runtime max-memory is smaller than the actual memory!
    private static final double MEM_FACTOR = 0.7;
    private static final int TIMEOUT = 100;
    private static final int TIMEOUT_REPEATS = 100;

    private JavaMemoryRestart() {
        // private constructor, since this is a pure utility class!
    }

    public static boolean isMemoryRestartRequired(final int userMemorySize) {
        final int memorySize = setLowerMemoryBound(userMemorySize);
        System.out.println("Requested memory size: " + memorySize + "MB.");
        final long jvmMemory = Runtime.getRuntime().maxMemory();
        System.out.println("Available JVM memory: " + jvmMemory / MEGA_BYTE + " MB.");
        return jvmMemory < MEM_FACTOR * memorySize * MEGA_BYTE;
    }

    /**
     * trys to restart the program with same command line options, but with more
     * memory. Function will only return true, when the following is fulfilled:
     * - initial memory size was smaller that default JVM Memory size - the
     * invoked process returned a "Starting"-String in its output -
     *
     * @param memorySize value in MB for the new memory
     * @return true if restart was successful
     */
    public static boolean startNewGeckoCIRCUITSJVM(final int memorySize, final String[] args, final String javaCommand) {
        final String jarPath = GetJarPath.getJarPath();
        String pathToJarFile = jarPath + "GeckoCIRCUITS.jar";

        // when run as applet, don't restart
        try {
            if (!new File(pathToJarFile).exists() && jarPath.endsWith("classes/")) {
                pathToJarFile = jarPath.substring(0, jarPath.length() - 15) + "/dist/GeckoCIRCUITS.jar";
            }
        } catch (AccessControlException ex) {
            System.err.println("Could not re-start GeckoCIRCUITS.jar with increased memory size,"
                    + "\nsince it was probably started as applet.");
            return false;
        }

        if (new File(pathToJarFile).exists()) {
            System.out.println("Starting GeckoCIRCUITS JVM with " + memorySize + " MB memory");

            final List<String> commands = createJVMCallCommands(javaCommand, memorySize, pathToJarFile, args);


            final ProcessBuilder procBuilder = new ProcessBuilder(commands);

            try {
                final Process proc = procBuilder.start();
                final InputStream inputStream = proc.getInputStream();
                final InputStream errInputStream = proc.getErrorStream();
                // check you have received an status code 200 to indicate ok
                // get the encoding from the Content-TYpe header
                final BufferedReader bufRead = new BufferedReader(new InputStreamReader(inputStream));
                final BufferedReader errBufRead = new BufferedReader(new InputStreamReader(errInputStream));
                return searchForReadyString(bufRead, errBufRead);

            } catch (IOException ex) {
                Logger.getLogger(JavaMemoryRestart.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private static boolean searchForReadyString(final BufferedReader stdBufRead, final BufferedReader errBufRead) {

        class SearchRunnable implements Runnable {

            public boolean readyStringFound = false;

            @Override
            public void run() {
                try {
                    for (String line = stdBufRead.readLine(); line != null; line = stdBufRead.readLine()) {
                        if (line.startsWith("GeckoCIRCUITS is ready")) {
                            readyStringFound = true;
                            return;
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(JavaMemoryRestart.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        SearchRunnable searchRunnable = new SearchRunnable();


        Runnable errPrintRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    for (String line = errBufRead.readLine(); line != null; line = errBufRead.readLine()) {
                        System.err.println(line);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(JavaMemoryRestart.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };


        final Thread searchThread = new Thread(searchRunnable);
        searchThread.start();

        final Thread errPrintThread = new Thread(errPrintRunnable);
        errPrintThread.start();

        try {
            for (int i = 0; i < TIMEOUT_REPEATS && !searchRunnable.readyStringFound; i++) {
                Thread.sleep(TIMEOUT);
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(JavaMemoryRestart.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!searchRunnable.readyStringFound) {
            System.err.println("Timeout, probably GeckoCIRCUITS was not started properly.");
            searchThread.stop();
        }
        errPrintThread.stop();
        return searchRunnable.readyStringFound;
    }

    /**
     * the memory should not be lower than a given threshold
     *
     * @param originalMemorySize in MB
     * @return corrected value in MB, that is larger than MINIMUM_MEM_MB
     */
    private static int setLowerMemoryBound(final int originalMemorySize) {
        if (originalMemorySize < MINIMUM_MEM_MB) {
            return MINIMUM_MEM_MB;
        } else {
            return originalMemorySize;
        }
    }

    private static List<String> createJVMCallCommands(final String javaCommand, final int memorySize,
            final String pathToJarFile, final String[] args) {
        final List<String> commands = new ArrayList<String>();
        commands.add(javaCommand);
        commands.add("-Xmx" + memorySize + "m");

        commands.add("-jar");
        commands.add(pathToJarFile);
        commands.addAll(Arrays.asList(args));
        return commands;
    }
};
