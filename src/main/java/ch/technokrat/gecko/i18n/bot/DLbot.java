/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
/*
 * -Download bot class.  This bot is used to download current translations
 * off of the Wiki database.  It has its own dedicated login on the Wiki:
 * USER = "DLbot", PASSWORD = "download"
 */
package ch.technokrat.gecko.i18n.bot;

import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import ch.technokrat.gecko.i18n.translationtoolbox.TranslationDialog;
import ch.technokrat.gecko.i18n.DoubleMap;
import ch.technokrat.gecko.i18n.InitParameters;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public class DLbot {
        
    private static boolean connected = false; // Applet-Wiki connection status indicator
    private static int progress = 0; // download progress (percent)
   
    /*
     * Creates a new bot with DLbot credentials, logs in and returns it.
     * -Throws an exception if communication with the wiki failed.
     * -Updates progress.
     */
    private static MediaWikiBot initBot() throws Exception {
            // print DEBUG messages in console if DEBUG_MODE is turned on
            if (InitParameters.DEBUG_MODE) {
                org.apache.log4j.BasicConfigurator.configure(); // configure log4j
            }
            progress = Math.min(progress + 2, 99); // update progress
            MediaWikiBot b = new MediaWikiBot(InitParameters.WIKI_URL);
            progress = Math.min(progress + 3, 99); // update progress
            b.login(InitParameters.DLbot_LOGIN, InitParameters.DLbot_PWORD);
            progress = Math.min(progress + 22, 99); // update progress
            return b;        
    }
    
    /**
     * Downloads all current single-line translations for the chosen language 
     * (other than English) off of the Wiki database
     * @return DoubleMap containing all single-line translations for the language
     */
    public static DoubleMap getTranslations_single() {
        try {
            MediaWikiBot b = initBot();
            SimpleArticle sa = b.readData(InitParameters.SINGLE_PAGE + "/" + InitParameters.getCurrentLanguageCode());
            progress = Math.min(progress + 9, 99); // update progress
            String wikiCode = sa.getText();
            progress = Math.min(progress + 1, 99); // update progress
            DoubleMap dm = getDoubleMap(wikiCode);
            progress = Math.min(progress + 22, 99); // update progress
            connected = true;
            return dm;
        } catch(Exception e) {
            new TranslationDialog(InitParameters.DATABASE_ERROR_MESSAGEa,InitParameters.DATABASE_ERROR_MESSAGEb).setVisible(true);
            connected = false;
            return null;
        }
    }
    
    /**
     * Downloads all current multiple-line translations for the chosen language
     * off of the Wiki database
     * @return DoubleMap containing all multiple-line translations for the language
     */
    public static DoubleMap getTranslations_multiple() {
        try {
            MediaWikiBot b = initBot();
            DoubleMap dm = new DoubleMap();
            SimpleArticle sa;
            
            // Get all multiple-line translations
            for (I18nKeys key : LangInit.englishMap_multiple.getKeySet()) {
                sa = b.readData("Translations:" + InitParameters.MULTIPLE_PAGE + "/" + key + "/" + InitParameters.getCurrentLanguageCode());
                progress = Math.min(progress + 5, 99); // update progress
                String value = sa.getText();
                dm.insertPair(key, value);               
            }
            connected = true;
            return dm;
        } catch (Exception e) {
            new TranslationDialog(InitParameters.DATABASE_ERROR_MESSAGEa,InitParameters.DATABASE_ERROR_MESSAGEb).setVisible(true);
            connected = false;
            return null;
        }
    }
    
     /**
     * Method to get connection status
     * - To be called after connection attempts
     * @return True if connection succeeded, False otherwise
     */
    public static boolean getConnectionStatus() {
        return connected;
    }
    
    /**
     * Method to get download progress
     * - To be called while downloading from a separate thread
     * @return Download progress (percentage complete)
     */
    public static int getProgress() {
        return progress;
    }
    
    /**
     * Method to reinitialize progress
     * - To be called after completing download instructions
     */
    public static void resetProgress() {
        progress = 0;
    }
    
    /*
     * This method parses wikiCode to key-value pairs which get stored in a 
     * DoubleMap and returned.
     * -Updates progress
     */
    private static DoubleMap getDoubleMap(final String wikiCode) {
        DoubleMap dm = new DoubleMap(); // create the DoubleMap
        progress = Math.min(progress + 2, 99); // update progress
        String[] lines = wikiCode.split("\n");
        progress = Math.min(progress + 2, 99); // update progress
        // iterate through all lines
        for (int i=0; i<lines.length; i=i+2) {
            I18nKeys key = I18nKeys.fabricFromKeyString(lines[i].substring(0, lines[i].length()-2));            
            String value = lines[i+1];
            dm.insertPair(key, value);            
            progress = Math.min(progress + 1, 99); // keep increasing progress to 99%
        }       
        return dm;
    }
}
