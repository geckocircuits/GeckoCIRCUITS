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
 * This class defines all constant parameters for the i18n toolkit.
 * -Note: Changing values here without changing the same respective values in 
 * the Admin Tool (class: admintool.InitParameters) will cause the i18n toolkit
 * to malfunction.
 */
package ch.technokrat.gecko.i18n;

import java.util.Locale;

public class InitParameters {
    
    private InitParameters(){} // prevents instantiation
    
    // **********   To Be Adjusted:    **********
    
    /**
     * Set this to "true" if you want database connection DEBUG messages to appear in the console
     */
    public static final boolean DEBUG_MODE = false;
    /**
     * URL of the Database Wiki
     */
    public static final String WIKI_URL = "http://www.translation.gecko-research.org/wiki/";
    /**
     * Download bot login
     */
    public static final String DLbot_LOGIN = "DLbot";
    /**
     * Download bot password
     */
    public static final String DLbot_PWORD = "download";
    /**
     * Upload bot login
     */
    public static final String UPbot_LOGIN = "UPbot";
    /**
     * Upload bot password
     */
    public static final String UPbot_PWORD = "upload";
    /**
     * Wiki page from which to get single-line translations
     */
    public static final String SINGLE_PAGE = "Presentation";
    /**
     * Wiki page from which to get multiple-line translations
     */
    public static final String MULTIPLE_PAGE = "PresentationMul";
    /**
     * Wiki page for single-line suggestions
     */
    public static final String SINGLE_SUGGESTIONS_PAGE = "GeckoSuggestions";
    /**
     * Wiki page for multiple-line suggestions
     */
    public static final String MULTIPLE_SUGGESTIONS_PAGE = "GeckoSuggestionsMul";
    
    // supported languages with their respective language codes                    
    
    /**
     * Method to get language code (if the language != English)
     * @return Language code of the chosen language
     */
    public static final String getCurrentLanguageCode() {        
        return LangInit.language.getLanguageCode();        
    }
    
    // **********   No Need To Change:    **********
    
    /**
     * Connection error message 1
     */
    public static final String DATABASE_ERROR_MESSAGEa = "Failed to communicate with database!!";
    /**
     * Connection error message 2
     */
    public static final String DATABASE_ERROR_MESSAGEb = "Please select English or establish connection.";
    /**
     * Connection error message 3
     */
    public static final String DATABASE_ERROR_MESSAGEc = "   Please re-establish connection and try again.";
    /**
     * used to indicate that a suggestion has no comment
     */
    public static final String NO_COMMENT = "NO_COMMENT";
    /**
     * Used to delimit suggestion lists
     */
    public static final String END_DELIMITER = "-END-";
    /**
     * Used to separate suggestions from comments pages 
     * IMPORTANT: This has to be a regex expression!)
     */
    public static final String SEPARATOR = "#::#:::##:";
    /**
     * Title of change language GUI
     */
    public static final String CHANGE_LANGUAGE = "Change Language";
    /**
     * Title of translation pop-up GUI
     */
    public static final String TRANS_TOOL = "Translation Tool";
    /**
     * Title of translation toolbox GUI
     */
    public static final String TRANS_TOOLS = "Translation Tools";
    /**
     * English Language name
     */
    public static final String ENGLISH = "English";
    /**
     * Information message while downloading
     */
    public static final String P_BAR_MESSAGE_DL = "Downloading Translations..";
    /**
     * Information message while uploading
     */
    public static final String P_BAR_MESSAGE_UP = "Uploading Suggestion..";
    /**
     * Dialog message in toolbox GUIs
     */
    public static final String CONFIRM_MESSAGE = "Please confirm a suggestion!";
    /**
     * Dialog message in toolbox GUI
     */
    public static final String CONFIRM_MESSAGE2 = "Please click the Done button first!";
    /**
     * Dialog message for empty confirms in toolbox GUIs
     */
    public static final String EMPTY_CONFIRM_MESSAGE = "Please give a suggestion!";
    /**
     * Open Translation Toolbox button identifier
     */
    public static final String OTT_BUTTON = "OTT";
    /**
     * Done button identifier
     */
    public static final String DONE_BUTTON = "Done";  
}
