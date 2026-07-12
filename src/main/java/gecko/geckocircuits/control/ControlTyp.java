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

import gecko.geckocircuits.general.AbstractComponentType;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.control.javablock.ControlJavaFunction;
import gecko.geckocircuits.nativec.ControlNativeC;
import java.util.HashMap;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Enum exposes type info for component configuration")
public enum ControlTyp implements AbstractComponentType {

    C_VOLTMETER(1, ControlVOLT.tinfo),
    C_AMPMETER(2, ControlAmperemeter.tinfo),
    C_CONST(3, ControlConstant.tinfo),
    C_SIGNALSOURCE(4, ControlSignalSource.tinfo),
    C_SCOPE(5, ControlOSZI.tinfo),
    C_SWITCH(6, ControlGate.tinfo),
    C_GAIN(7, ControlGain.tinfo),
    C_PT1(8, ControlPT1.tinfo),
    C_PT2(9, ControlPT2.tinfo),
    C_PI(10, ControlPI.tinfo),
    C_HYS(11, ControlHysteresis.tinfo),
    C_ADD(12, ControlAdd.tinfo),
    C_SUB(13, ControlSubtraction.tinfo),
    C_MUL(14, ControlMUL.tinfo),
    C_DIV(15, ControlDivision.tinfo),
    C_TEMP(16, ControlTEMP.tinfo),
    C_FLOW(17, ControlFlowMeter.tinfo),
    C_NOT(18, ControlNOT.tinfo),
    C_AND(19, ControlAnd.tinfo),
    C_OR(20, ControlOr.tinfo),
    C_XOR(21, ControlExclusiveOr.tinfo),
    C_TO_EXTERNAL(22, ControlToEXTERNAL.tinfo),
    C_FROM_EXTERNAL(23, ControlFromEXTERNAL.tinfo),
    C_DELAY(25, ControlDelay.tinfo),
    C_SAMPLEHOLD(26, ControlSampleHold.tinfo),
    C_LIMIT(27, ControlLimit.tinfo),
    C_PD(29, ControlPD.tinfo),
    C_ABS(32, ControlAbsoluteValue.tinfo),
    C_ROUND(33, ControlRound.tinfo),
    C_SIN(34, ControlSIN.tinfo),
    C_ASIN(35, ControlAreaSine.tinfo),
    C_COS(36, ControlCosine.tinfo),
    C_ACOS(37, ControlAreaCosine.tinfo),
    C_TAN(38, ControlTAN.tinfo),
    C_ATAN(39, ControlAreaTangens.tinfo),
    C_EXP(40, ControlExponential.tinfo),
    C_LN(41, ControlLN.tinfo),
    C_SQR(42, ControlSQR.TYPE_INFO),
    C_SQRT(43, ControlSQRT.tinfo),
    C_POW(44, ControlPOW.tinfo),
    C_GE(45, ControlGreaterEqual.tinfo),
    C_GT(46, ControlGreaterThan.tinfo),
    C_EQ(47, ControlEqual.tinfo),
    C_NE(48, ControlNE.tinfo),
    C_MIN(49, ControlMIN.tinfo),
    C_MAX(50, ControlMAX.tinfo),
    C_SIGN(51, ControlSignum.tinfo),
    C_COUNTER(53, ControlCounter.tinfo),
    C_TIME(58, ControlTIME.tinfo),
    C_SPARSEMATRIX(59, ControlSPARSEMATRIX.tinfo),
    C_CISPR16(60, ControlCISPR16.tinfo),
    C_JAVA_FUNCTION(61, ControlJavaFunction.tinfo),
    C_VIEWMOT(62, ControlVIEWMOT.tinfo),
    C_SPACE_VECTOR(63, ControlSpaceVector.tinfo),
    C_INT(64, ControlIntegrator.tinfo),
    C_ABCDQ(65, ControlABCDQ.tinfo),
    C_DQABC(66, ControlDQABC.tinfo),
    // careful: this has been moved to SpecialType! S_TEXTFIELD(70)),
    C_THYR_CTRL(72, ControlThyristorControl.tinfo),
    C_U_ZI(73, ControlU_ZI.tinfo),
    C_TF(74, ControlTransferFunction.tinfo),
    C_DATA_EXPORT(75, ControlSaveData.tinfo),
    C_PMSM_CONTROL(76, ControlPMSMCONTROL.tinfo),
    C_PMSM_MODULATOR(77, ControlPMSM_Modulator.tinfo),
    C_TERMINAL(78, ControlTERMINAL.tinfo),
    C_MMFMETER(79, ControlMMF.tinfo),
    C_FLUXMETER(80, ControlFluxMeter.tinfo),
    C_GLOBAL_TERMINAL(81, ControlGlobalTerminal.tinfo),
    C_SDFT(82, ControlSlidingDFT.T_INFO),
    C_MUX(84, ControlMUX.tinfo),
    C_DEMUX(85, ControlDemux.tinfo),
    C_DEBUG(86, ControlControlDebug.tinfo),
    C_SMALL_SIG(87, ControlSmallSignalAnalysis.TYPE_INFO),
    C_NATIVE_C_FUNCTION(88, ControlNativeC.tinfo),
    C_SOURCE_IMPORT_DATA(89, ControlImportFromFile.tinfo) {

        @Override
        public int getTypeNumber() {
            return C_SIGNALSOURCE.getTypeNumber();
        }

    },
    C_SOURCE_RANDOM(90, ControlRandomWalk.tinfo) {

        @Override
        public int getTypeNumber() {
            return C_SIGNALSOURCE.getTypeNumber();
        }

    },;
    private int _intValue;
    private AbstractTypeInfo _typeInfo;

    ControlTyp(final int initValue, final AbstractTypeInfo typeInfo) {
        _intValue = initValue;
        _typeInfo = typeInfo;
        assert _typeInfo != null;
        _typeInfo.addParentEnum(this);
    }
    private static final Map<Integer, ControlTyp> _backwardMap;

    static {
        _backwardMap = new HashMap<Integer, ControlTyp>();
        for (ControlTyp typ : values()) {
            _backwardMap.put(typ._intValue, typ);
        }
    }

    public static ControlTyp getFromIntNumber(final int intNumber) {
        if (_backwardMap.containsKey(intNumber)) {
            return _backwardMap.get(intNumber);
        }
        throw new IllegalArgumentException("Type with identifier: " + intNumber + " is not known!");
    }

    @Override
    public int getTypeNumber() {
        return _intValue;
    }

    @Override
    public AbstractTypeInfo getTypeInfo() {
        return _typeInfo;
    }

}