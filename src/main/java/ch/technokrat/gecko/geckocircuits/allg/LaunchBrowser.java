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

import java.io.IOException;

public final class LaunchBrowser {

    private LaunchBrowser() {
        // pure utility class!
    }
    

    public static void launch(final String url) {
        final String fileUrl = url;
        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler "
                        + fileUrl);
            } else {
                Runtime.getRuntime().exec("firefox " + fileUrl);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    private static boolean isWindows() {
        final String osString = System.getProperty("os.name");
        if(osString == null || osString.isEmpty()) {
            return false;
        }
        return osString.startsWith("Windows");
    }
}
