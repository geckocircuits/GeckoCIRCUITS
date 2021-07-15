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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * this is based on the old "typ-Numbers from Uwe... with enums much cleaner!
 *
 * @author andy
 */
public enum CircuitTyp implements AbstractComponentTyp {

    LK_R(1, ResistorCircuit.TYPE_INFO),
    LK_L(2, InductorWOCoupling.TYPE_INFO),
    LK_C(3, CapacitorCircuit.TYPE_INFO),
    LK_U(4, VoltageSourceElectric.TYPE_INFO),
    LK_I(5, CurrentSourceCircuit.TYPE_INFO),
    LK_D(6, Diode.TYPE_INFO),
    LK_S(7, IdealSwitch.TYPE_INFO),
    LK_THYR(8, Thyristor.TYPE_INFO),
    LK_M(9, MutualInductance.TYPE_INFO),
    LK_IGBT(10, IGBT.TYPE_INFO),
    // bei Verwendung dieser Induktivitaet wird die Matrixgleichung um die Induktivitaetsstroeme 
    // erweitert --> bessere numerische Stabilitaet
    LK_LKOP2(12, InductorCoupable.TYPE_INFO),
    LK_LISN(13, LISN.TYPE_INFO),
    LK_MOTOR(14, MotorDC.TYPE_INFO),     
    LK_MOTOR_PMSM(15, MotorPMSM.TYPE_INFO),
    LK_MOTOR_SMSALIENT(16, MotorSmSalient.TYPE_INFO),
    LK_MOTOR_SMROUND(17, MotorSmRound.TYPE_INFO),
    LK_MOTOR_IMA(18, MotorImCage.TYPE_INFO),
    LK_MOTOR_IMC(20, MotorInductionMachine.TYPE_INFO),
    LK_MOTOR_IMSAT(21, MotorImSat.TYPE_INFO),
    LK_OPV1(22, OperationalAmplifier.TYPE_INFO ),
    LK_TRANS(23, IdealTransformer.TYPE_INFO),
    REL_RELUCTANCE(24, ResistorReluctance.TYPE_INFO),
    REL_INDUCTOR(25, ReluctanceInductor.TYPE_INFO),
    REL_MMF(26, VoltageSourceReluctanceMMF.TYPE_INFO),
    // this was moved to SpecialTyp!!! : SUBCIRCUIT(27),
    LK_MOSFET(28, MOSFET.TYPE_INFO),
    LK_TERMINAL(29, TerminalCircuit.TYPE_INFO ),
    REL_TERMINAL(30, RelTerminal.TYPE_INFO),
    LK_GLOBAL_TERMINAL(31, CircuitGlobalTerminal.TYPE_INFO),
    REL_GLOBAL_TERMINAL(32, ReluctanceGlobalTerminal.TYPE_INFO),
    LK_BJT(33, BJT.TYPE_INFO),
    TH_PvCHIP(41, ThermPvChip.TYPE_INFO),
    TH_MODUL(42, ThermMODUL.TYPE_INFO),
    //TH_KUEHLER(43, THERMAL),
    TH_FLOW(44, HeatFlowCurrentSource.TYPE_INFO),
    TH_TEMP(45, VoltageSourceThermalTemperature.TYPE_INFO),
    TH_RTH(46, ResistorThermal.TYPE_INFO),
    TH_CTH(47, CapacitorThermal.TYPE_INFO),
    TH_AMBIENT(48, ThermAmbient.TYPE_INFO),
    TH_TERMINAL(49, ThTerminal.TYPE_INFO),
    TH_GLOBAL_TERMINAL(50, ThGlobalTerminal.TYPE_INFO),
    LK_MOTOR_PERM(51, MotorPermanent.TYPE_INFO),
    NONLIN_REL(52, NonLinearReluctance.TYPE_INFO);
    
    private final int _intValue;
    private final AbstractTypeInfo _tInfo;

    CircuitTyp(final int initValue, final AbstractTypeInfo typeInfo) {
        _intValue = initValue;
        _tInfo = typeInfo;
        _tInfo.addParentEnum(this);
    }

    @Override
    public int getTypeNumber() {
        return _intValue;
    }
    private static Map<Integer, CircuitTyp> _backwardMap;

    public static CircuitTyp getFromIntNumber(final int intNumber) {

        if (_backwardMap == null) {
            _backwardMap = new HashMap<Integer, CircuitTyp>();
            for (CircuitTyp typ : values()) {
                _backwardMap.put(typ._intValue, typ);
            }
        }

        if (_backwardMap.containsKey(intNumber)) {
            return _backwardMap.get(intNumber);
        }
        throw new IllegalArgumentException("Type with identifier: " + intNumber + " is not known!");
    }

    @Override
    public AbstractTypeInfo getTypeInfo() {
        return _tInfo;
    }        
}
