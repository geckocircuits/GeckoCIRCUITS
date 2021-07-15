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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author andy
 */
class MotorSmRoundDialog extends AbstractMotorDialog<MotorSmRound> {

    public MotorSmRoundDialog(MotorSmRound parent) {
        super(parent);
    }

    @Override
    List<UserParameter<Double>> getDialogSortedParameters() {
        return Arrays.asList(element._frictionParameter, element._inertiaParameter, element._polePairsParameter,
                element._statorResistancePar, element.statorLeakageInductance,
                element.unsaturatedMagnetizingInductanceD, element.saturatedMagnetizingInductance,
                element.fluxAtSaturationTransition, element.tightnessOfSaturationTransition,
                element.fieldResistance, element.fieldLeakageInductance, element.damperResistanceD, element.damperResistanceQ,
                element._damperResistanceQ2_par, element.damperLeakageInductanceD, element.damperLeakageInductanceQ, element._damperLeakageIndQ2Par,
                element._initialRotationalSpeed, element._initialRotorPosition, element.initialStatorCurrentA, element.initialStatorCurrentB,
                element.initialFieldCurrent, element._initDampCurrentPar, element.initialStatorFluxD, element.initialStatorFluxQ);
    }

    @Override
    List<UserParameter<Double>> getInitPanelParameters() {
        return Arrays.asList(element._initialRotationalSpeed, element._initialRotorPosition, element.initialStatorCurrentA, element.initialStatorCurrentB,
                element.initialFieldCurrent, element._initDampCurrentPar, element.initialStatorFluxD, element.initialStatorFluxQ);
    }

    @Override
    JPanel buildPanelInitParameter() {
        return super.buildPanelParameters(17, 25, new int[]{2}, false);
    }

    @Override
    JPanel buildPanelParameters() {
        return super.buildPanelParameters(0, 17, new int[]{3, 12, 19}, true);
    }
}
