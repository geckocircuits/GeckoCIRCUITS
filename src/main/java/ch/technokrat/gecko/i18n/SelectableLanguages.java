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
package ch.technokrat.gecko.i18n;

import java.util.Locale;

public enum SelectableLanguages {

    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "français"),
    JAPANESE("ja", "日本語"),
    PORTUGESE("pt", "português"),
    ITALIAN("it", "italiano"),
    SPANISH("es", "español"),
    TURKISH("tr", "Türkçe"),
    HUNGARIAN("hu", "magyar"),
    POLISH("pl", "polski"),
    CHINESE("zh", "中文"),
    THAI("th", "ไทย"),
    ROMANIC("ro", "română"),
    DUTCH("nl", "Nederlands"),
    KOREAN("ko", "한국어"),
    RUSSIAN("ru", "русский"),
    LETTIAN("lv", "Latviešu"),
    HEBREW("iw", "עברית"),
    CROATIAN("hr", "hrvatski"),
    ESTNIC("et", "Eesti"),
    FINNISH("fi", "suomi"),
    BELARUS("be", "беларускі"),
    SWEDIAN("sv", "svenska"),
    ICELANDIC("is", "íslenska"),
    NORWEGIAN("no", "norsk"),
    LITAUIC("lt", "Lietuvių"),
    CZECK("cs", "čeština"),
    SERBIAN("sr", "Српски"),
    UCRAINE("uk", "українська"),
    VIETNAMESE("vi", "Tiếng Việt"),
    ALBANESE("sq", "shqipe"),
    ARABIC("ar", "العربية"),
    INDONESIAN("in", "Bahasa Indonesia"),
    SLOWENIAN("sl", "Slovenščina"),
    GAELIC("ga", "Gaeilge"),
    SLOWAKIAN("sk", "Slovenčina"),
    DAENISH("da", "Dansk"),
    MALTESIAN("mt", "Malti"),
    GREEK("el", "Ελληνικά"),
    MAKEDONIC("mk", "македонски"),
    BULGARIAN("bg", "български"),
    CATALAN("ca", "català"),
    MALAY("ms", "Bahasa Melayu");
    final String _foreignName;
    final String _langCode;
    final Locale _locale;

    SelectableLanguages(final String code, final String foreignName) {
        _locale = new Locale(code);
        _foreignName = foreignName;
        _langCode = code;

    }

    @Override
    public String toString() {
        return _foreignName;
    }

    public String getLanguageCode() {
        return _langCode;
    }

    public Locale getLocale() {
        return _locale;
    }
}