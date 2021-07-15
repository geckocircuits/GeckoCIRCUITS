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

import ch.technokrat.gecko.i18n.resources.I18nKeys;
import ch.technokrat.gecko.i18n.translationtoolbox.PopupListener;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public final class GuiFabric {    
    
    
    private GuiFabric() {
        //private contructor, since this is a pure static untility class.
    }    
    
    public static JMenu getJMenu(final I18nKeys key) {
        final JMenu returnValue = new JMenu(LangInit.transMap_single.getValue(key));        
        returnValue.addMouseListener(new PopupListener(key));
        return returnValue;        
    }
    
    public static JMenuItem getJMenuItem(final I18nKeys key) {
        final JMenuItem returnValue = new JMenuItem(LangInit.transMap_single.getValue(key));        
        returnValue.addMouseListener(new PopupListener(key));
        return returnValue;
    }
    
    public static JButton getJButton(final I18nKeys key) {
        final JButton returnValue = new JButton(LangInit.transMap_single.getValue(key));        
        returnValue.addMouseListener(new PopupListener(key));
        return returnValue;
    }
    
    
}
