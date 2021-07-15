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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


/**
 *
 * @author andreas
 */
public final class SystemOutputRedirect {
    private static final PrintStream ORIG_ERR_STREAM = System.err;
    private static final PrintStream ORIG_OUT_STREAM = System.out;
    private static final OutputWarningStream WARNING_STREAM = 
            new OutputWarningStream(new ByteArrayOutputStream(), ORIG_OUT_STREAM);
    private static final OutputWarningStream WARNING_ERR_STREAM = 
            new OutputWarningStream(new ByteArrayOutputStream(), ORIG_ERR_STREAM);
    

    
    
    private SystemOutputRedirect() {
        // this is a "static" behavior class! -> private constructor
    }
    
    public static void init() {        
        System.setOut(new PrintStream(WARNING_STREAM));
        System.setErr(new PrintStream(WARNING_ERR_STREAM));
    }       
    
    public static void setAlternativeOutput(final StringBuffer stringBuffer, final String description) {
        WARNING_STREAM.setAlternativeOutput(stringBuffer, description);
        WARNING_ERR_STREAM.setAlternativeOutput(stringBuffer, description);
    }
    
    public static void setOriginalOutput() {
        WARNING_STREAM.setOriginalOutput();
        WARNING_ERR_STREAM.setOriginalOutput();
    }
    
    public static void setConsoleOutput(final String sourceDescription) {
        WARNING_STREAM.setConsoleOutput(sourceDescription);
        WARNING_ERR_STREAM.setConsoleOutput(sourceDescription);
    }
    
    public static void reset() {
        WARNING_ERR_STREAM.reset();
        WARNING_STREAM.reset();
    }
    
    
}
