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
 * -Upload bot class.  This bot is used to add translation suggestions (along 
 * with their comments) from users to the Wiki database.  It has its own
 * dedicated login on the Wiki:
 * USER = "UPbot", PASSWORD = "upload"
 */
package ch.technokrat.gecko.i18n.bot;

import ch.technokrat.gecko.i18n.InitParameters;
import java.util.Random;
import java.lang.StringBuilder;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import ch.technokrat.gecko.i18n.translationtoolbox.TranslationDialog;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public class UPbot {
    
    private static boolean connected = false; // Applet-Wiki connection status indicator
    private static int progress = 0; // upload progress (percent)
    
    /*
     * Creates a new bot with UPbot credentials, logs in and returns it.
     * -Throws an exception if communication with the wiki failed.
     */
    private static MediaWikiBot initBot() throws Exception {
            // print DEBUG messages in console if DEBUG_MODE is turned on
            if (InitParameters.DEBUG_MODE) {
                org.apache.log4j.BasicConfigurator.configure(); // configure log4j
            }
            progress = Math.min(progress + 2, 99); // update progress
            MediaWikiBot b = new MediaWikiBot(InitParameters.WIKI_URL);
            progress = Math.min(progress + 2, 99); // update progress
            b.login(InitParameters.UPbot_LOGIN, InitParameters.UPbot_PWORD);
            progress = Math.min(progress + 23, 99); // update progress
            return b;        
    }

    /**
     * Adds a single-line suggestion along with a comment to the designated 
     * Suggestions page for the chosen language on the Wiki
     * @param key Key of the translation
     * @param newTranslation New suggestion to upload
     * @param comment Comment to upload ("" for no comment)
     */
    public static void addTranslationSuggestion_single(I18nKeys key, String newTranslation, String comment) {
        try {
            // make the change on the Wiki database

            MediaWikiBot b = initBot();
            
            String title = InitParameters.NO_COMMENT; // initialize title string
            
            if (!comment.isEmpty()) {
                progress = Math.min(progress + 4, 99); // update progress
                // create a new page for the comment
                SimpleArticle comm = new SimpleArticle();

                // create a random title string with 6 characters
                title = RandomString(6);
                progress = Math.min(progress + 1, 99); // update progress
                SimpleArticle sa1 = b.readData(title); // so that we can check if the page already exists
                progress = Math.min(progress + 12, 99); // update progress
                String wikiCode1 = sa1.getText();
                progress = Math.min(progress + 2, 99); // update progress
                
                // keep changing title until it is unique
                while (!wikiCode1.isEmpty()) {
                    title = title + "i";
                    sa1 = b.readData(title); // check again
                    wikiCode1 = sa1.getText();
                }

                comm.setTitle(title);
                comm.setText(comment);
                progress = Math.min(progress + 2, 99); // update progress
                b.writeContent(comm); // upload the comment to the Wiki
            }
            
            progress = Math.min(progress + 6, 99); // update progress
            SimpleArticle sa = b.readData(InitParameters.SINGLE_SUGGESTIONS_PAGE + "_" + InitParameters.getCurrentLanguageCode());
            progress = Math.min(progress + 6, 99); // update progress
            String wikiCode = sa.getText();
            progress = Math.min(progress + 1, 99); // update progress
            wikiCode = wikiCode.replace(InitParameters.END_DELIMITER + key, newTranslation + InitParameters.SEPARATOR + title + "\n" + InitParameters.END_DELIMITER + key);
            progress = Math.min(progress + 1, 99); // update progress
            sa.setText(wikiCode);
            progress = Math.min(progress + 4, 99); // update progress
            b.writeContent(sa);
            connected = true;
        } catch(Exception e) {
            new TranslationDialog(InitParameters.DATABASE_ERROR_MESSAGEa,InitParameters.DATABASE_ERROR_MESSAGEc).setVisible(true);
            connected = false;
        }       
    }
    
    /**
     * Adds a multiple-line suggestion along with a comment to the designated 
     * Suggestions page for the chosen language on the Wiki
     * @param key Key of the translation
     * @param newTranslation New suggestion to upload
     * @param comment Comment to upload ("" for no comment)
     */
    public static void addTranslationSuggestion_multiple(I18nKeys key, String newTranslation, String comment) {
        try {
            // make the change on the Wiki database
            
            MediaWikiBot b = initBot();
            
            // create a new page for the suggestion
            SimpleArticle sugg = new SimpleArticle();

            // create a random title string with 7 characters
            String suggestionTitle = RandomString(7);
            
            SimpleArticle sa1 = b.readData(suggestionTitle);
            String wikiCode1 = sa1.getText();
            // keep changing title until it is unique
            while (!wikiCode1.isEmpty()) {
                suggestionTitle = suggestionTitle + "i";
                sa1 = b.readData(suggestionTitle); // check again
                wikiCode1 = sa1.getText();
            }
            
            sugg.setTitle(suggestionTitle);
            sugg.setText(newTranslation);
            b.writeContent(sugg); // upload the suggestion
            
            String commentTitle = InitParameters.NO_COMMENT; // initialize comment page title
            
            if (!comment.isEmpty()) {
                // create a new page for the comment
                SimpleArticle comm = new SimpleArticle();
                
                // create a random title string with 6 characters
                commentTitle = RandomString(6);
                
                sa1 = b.readData(commentTitle);
                wikiCode1 = sa1.getText();
                // keep changing title until it is unique
                while (!wikiCode1.isEmpty()) {
                    commentTitle = commentTitle + "i";
                    sa1 = b.readData(commentTitle); // check again
                    wikiCode1 = sa1.getText();
                }
                
                comm.setTitle(commentTitle);
                comm.setText(comment);
                b.writeContent(comm); // upload the comment                
            }
            
            SimpleArticle sa = b.readData(InitParameters.MULTIPLE_SUGGESTIONS_PAGE + "_" + InitParameters.getCurrentLanguageCode());
            String wikiCode = sa.getText();
            wikiCode = wikiCode.replace(InitParameters.END_DELIMITER + key, suggestionTitle + InitParameters.SEPARATOR + commentTitle + "\n" + InitParameters.END_DELIMITER + key);
            sa.setText(wikiCode);
            b.writeContent(sa);
            connected = true;
        } catch (Exception e) {
            new TranslationDialog(InitParameters.DATABASE_ERROR_MESSAGEa,InitParameters.DATABASE_ERROR_MESSAGEc).setVisible(true);
            connected = false;
        }
    }
    
    /*
     * Returns a random string of lower-case letters.
     * -The parameter "length" is the length of the string to return
     */
    private static String RandomString(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < length; k++) {
            sb.append(alphabet.charAt(rand.nextInt(alphabet.length())));
        }
        return sb.toString();
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
    * Method to get upload progress
    * - To be called while uploading from a separate thread
    * @return Upload progress (percentage complete)
    */
   public static int getProgress() {
       return progress;
   }
   
    /**
     * Method to reinitialize progress
     * - To be called after completing upload instructions
     */
    public static void resetProgress() {
        progress = 0;
    }
}
