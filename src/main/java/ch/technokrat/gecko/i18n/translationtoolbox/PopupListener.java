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
 * -Pop-up mouse listener class.
 */
package ch.technokrat.gecko.i18n.translationtoolbox;

import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.SelectableLanguages;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public final class PopupListener implements MouseListener {

    private final I18nKeys _key; // used to determine which GUI element triggered event

    /**
     * Constructor
     *
     * @param key Key of the pop-up triggering element text
     */
    public PopupListener(final I18nKeys key) {
        this._key = key;
    }

    @Override
    public void mousePressed(final MouseEvent evt) {
        listenForPopup(_key, evt);
    }

    /**
     * Checks if pop-up triggering command is given and opens translation pop-up
     * if the key is found in one of the translated maps
     *
     * @param key Key of the pop-up triggering element text
     * @param evt MouseEvent which triggered the listener method
     */
    public void listenForPopup(final I18nKeys key, final MouseEvent evt) {
        if (evt.isControlDown() && evt.isShiftDown() && !LangInit.language.equals(SelectableLanguages.ENGLISH)) {
            if (LangInit.transMap_single.getValue(key) != null) {
                new TranslationPopupSingle(key).setVisible(true); // open single-line pop-up
            } else if (LangInit.transMap_multiple.getValue(key) != null) {
                new TranslationPopupMultiple(key).setVisible(true); // open multiple-line pop-up
            }
        }
    }

    @Override
    public void mouseExited(final MouseEvent evt) {
        // nothing todo
    }

    @Override
    public void mouseEntered(final MouseEvent evt) {
        // nothing todo
    }

    @Override
    public void mouseReleased(final MouseEvent evt) {
        // nothing todo
    }

    @Override
    public void mouseClicked(final MouseEvent evt) {
        // nothing todo
    }
}
