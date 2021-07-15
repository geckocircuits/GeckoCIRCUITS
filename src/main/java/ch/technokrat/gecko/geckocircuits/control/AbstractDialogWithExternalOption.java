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
package ch.technokrat.gecko.geckocircuits.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

public abstract class AbstractDialogWithExternalOption<T extends RegelBlock> extends DialogElementCONTROL<T> {

    final JCheckBox _jCheckBoxUseExternal = new JCheckBox("Use external parameters");
    private final ControlInputTwoTerminalStateable _externable;

    public AbstractDialogWithExternalOption(final T element) {
        super(element);
        _externable = (ControlInputTwoTerminalStateable) element;
        assert element instanceof ControlInputTwoTerminalStateable;
        _jCheckBoxUseExternal.setSelected(_externable.isExternalSet());

        _jCheckBoxUseExternal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                for (JComponent comp : getComponentsDisabledExternal()) {
                    comp.setEnabled(!_jCheckBoxUseExternal.isSelected());
                }
            }
        });

    }

    @Override
    protected void processInputs() {
        super.processInputs();
        _externable.setExternalUsed(_jCheckBoxUseExternal.isSelected());
    }

    abstract JComponent[] getComponentsDisabledExternal();
}
