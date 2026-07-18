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

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cross-platform browser launcher utility.
 *
 * <p>Opens URLs in the system's default browser. Supports Windows (rundll32)
 * and Unix-like systems (firefox fallback).
 *
 * @since Core Module Extraction Sprint
 */
public final class LaunchBrowser {
    private static final Logger LOGGER = LogManager.getLogger(LaunchBrowser.class);


    private LaunchBrowser() {
        // pure utility class!
    }


    public static void launch(final String url) {
        final String fileUrl = url;
        try {
            if (isWindows()) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", fileUrl).start();
            } else {
                new ProcessBuilder("firefox", fileUrl).start();
            }
        } catch (IOException ioe) {
            LOGGER.error("Failed to launch browser for URL: " + fileUrl, ioe);
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
