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
package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.allg.UserParameter;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

public final class MotorPermanent extends AbstractMotorDC {

    private static final double INITIAL_FLUX_VALUE = 0.01;
    private static final int OMEGA_INDEX = 1;
    private static final int DREHZAHL_INDEX = 2;
    private static final int EMK_INDEX = 3;
    private static final int ELECTRIC_TORQUE_INDEX = 4;
    private static final int THETA_M_INDEX = 5;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorPermanent.class, "PM-DC", I18nKeys.DC_MACHINE_PERM, I18nKeys.PERMANENT_MAGNET_DC_MACHINE);
    double _fluxLinkage;
    final UserParameter<Double> _psiParameter = UserParameter.Builder.
            <Double>start("flux_psi", INITIAL_FLUX_VALUE).
            longName(I18nKeys.PERMANENT_MAGNET_FLUX_LINKAGE).
            shortName("psi").
            unit("Wb").
            arrayIndex(this, -1).
            build();

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        // careful: _psi is calculated BEFORE the next steps...
        // since it is used in "calculateFieldCurrents".
        _fluxLinkage = _psiParameter.getDoubleValue();
        super.setzeParameterZustandswerteAufNULL();
        calculateEMK();
    }

    @Override
    public List<String> getParameterStringIntern() {
        return Arrays.asList("ia [A]", "omega", "n [rpm]", "emf [V]", "Tel [Nm]", "theta [rad]");
    }

    @Override
    double calculateElectricTorque() {
        return _fluxLinkage * _anchorCurrent;
    }

    @Override
    void calculateEMK() {
        _emk = _fluxLinkage * _omegaElectric;  // innere Spannung der Maschine         
    }    

    @Override
    protected Window openDialogWindow() {
        return new MotorPermanentDialog(this);
    }

    @Override
    int getEMKIndex() {
        return EMK_INDEX;
    }

    @Override
    int getOmegaIndex() {
        return OMEGA_INDEX;
    }

    @Override
    int getDrehzahlIndex() {
        return DREHZAHL_INDEX;
    }

    @Override
    int getElectricTorqueIndex() {
        return ELECTRIC_TORQUE_INDEX;
    }

    @Override
    int getThetaMIndex() {
        return THETA_M_INDEX;
    }
}
