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
package gecko.geckocircuits.control;

import gecko.geckocircuits.general.UserParameter;
import gecko.geckocircuits.circuit.TerminalControlInput;
import gecko.geckocircuits.circuit.TerminalControlOutput;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.control.calculators.MUXControlCalculatable;
import gecko.i18n.resources.I18nKeys;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ControlMUX extends RegelBlock implements VariableTerminalNumber {
    private static final long serialVersionUID = 1L;

    private static final double WIDTH = 0.3;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlMUX.class, "MUX", I18nKeys.CONTROL_MUX);

    transient final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("tn", 3).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();


    public ControlMUX() {
        super(3, 0);
        YOUT.add(new TerminalControlOutput(this, 2, -1));
        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });
        setInputTerminalNumber(3);  // default: 3 Anschluss nach Aussen
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"output"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.VECTOR_SIGNAL};
    }


    @Override
    @SuppressFBWarnings(value = "UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR",
            justification = "Null check protects against uninitialized field access from superclass constructor")
    public void setInputTerminalNumber(final int number) {
        while (XIN.size() > number) {
            XIN.pop();
        }

        while (XIN.size() < number) {
            XIN.add(new TerminalControlInput(this, -2, -XIN.size()));
        }

        // Null check required because this method may be called from superclass constructor
        // before _inputTerminalNumber field is initialized (UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR)
        if (_inputTerminalNumber != null) {
            int newsize = XIN.size();
            if (_inputTerminalNumber.getValue() != newsize) {
                _inputTerminalNumber.setUserValue(newsize);
            }
        }
    }

    @Override
    public void setOutputTerminalNumber(final int number) {
        // here, we don't have output terminals
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new MUXControlCalculatable(XIN.size());
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int posX = getSheetPosition().x;
        final int posY = getSheetPosition().y;
        final Color origColor = graphics.getColor();
        // Klickbereich:
        xKlickMin = (int) (dpix * (posX - WIDTH));
        xKlickMax = (int) (dpix * (posX + WIDTH));
        yKlickMin = (int) (dpix * (posY - WIDTH));
        yKlickMax = (int) (dpix * (posY + XIN.size()));
        graphics.setColor(getBackgroundColor());

        graphics.fillRect((int) (dpix * (posX - WIDTH)), (int) (dpix * (posY - WIDTH)),
                (int) (dpix * 2 * WIDTH), (int) (dpix * XIN.size()));

        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (posX - WIDTH)), (int) (dpix * (posY - WIDTH)),
                (int) (dpix * 2 * WIDTH), (int) (dpix * XIN.size()));
        // Pfeil-Symbol:
        double pf = 1.4;  // Pfeilspitzen-X-Abstand
        double pfym = YOUT.get(0).getPosition().y;  // Pfeil-Y-Koordinate
        graphics.drawLine((int) (dpix * (posX + WIDTH)), (int) (dpix * pfym), (int) (dpix * (posX + pf)),
                (int) (dpix * pfym));  // zum Pfeil gehoerig
        graphics.setColor(Color.black);
        graphics.setColor(origColor);

    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    @Override
    protected Window openDialogWindow() {
        return new DialogMuxDemux(this);
    }
}
