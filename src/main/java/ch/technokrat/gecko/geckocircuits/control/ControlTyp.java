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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.control.javablock.ReglerJavaFunction;
import ch.technokrat.gecko.geckocircuits.nativec.ReglerNativeC;
import java.util.HashMap;
import java.util.Map;

public enum ControlTyp implements AbstractComponentTyp {

    C_VOLTMETER(1, ReglerVOLT.tinfo),
    C_AMPMETER(2, ReglerAmperemeter.tinfo),
    C_CONST(3, ReglerConstant.tinfo),
    C_SIGNALSOURCE(4, ReglerSignalSource.tinfo),
    C_SCOPE(5, ReglerOSZI.tinfo),
    C_SWITCH(6, ReglerGate.tinfo),
    C_GAIN(7, ReglerGain.tinfo),
    C_PT1(8, ReglerPT1.tinfo),
    C_PT2(9, ReglerPT2.tinfo),
    C_PI(10, ReglerPI.tinfo),
    C_HYS(11, ReglerHysteresis.tinfo),
    C_ADD(12, ReglerAdd.tinfo),
    C_SUB(13, ReglerSubtraction.tinfo),
    C_MUL(14, ReglerMUL.tinfo),
    C_DIV(15, ReglerDivision.tinfo),
    C_TEMP(16, ReglerTEMP.tinfo),
    C_FLOW(17, ReglerFlowMeter.tinfo),
    C_NOT(18, ReglerNOT.tinfo),
    C_AND(19, ReglerAnd.tinfo),
    C_OR(20, ReglerOr.tinfo),
    C_XOR(21, ReglerExclusiveOr.tinfo),
    C_TO_EXTERNAL(22, ReglerToEXTERNAL.tinfo),
    C_FROM_EXTERNAL(23, ReglerFromEXTERNAL.tinfo),
    C_DELAY(25, ReglerDelay.tinfo),
    C_SAMPLEHOLD(26, ReglerSampleHold.tinfo),
    C_LIMIT(27, ReglerLimit.tinfo),
    C_PD(29, ReglerPD.tinfo),
    C_ABS(32, ReglerAbsolutValue.tinfo),
    C_ROUND(33, ReglerRound.tinfo),
    C_SIN(34, ReglerSIN.tinfo),
    C_ASIN(35, ReglerAreaSine.tinfo),
    C_COS(36, ReglerCosine.tinfo),
    C_ACOS(37, ReglerAreaCosine.tinfo),
    C_TAN(38, ReglerTAN.tinfo),
    C_ATAN(39, ReglerAreaTangens.tinfo),
    C_EXP(40, ReglerExponential.tinfo),
    C_LN(41, ReglerLN.tinfo),
    C_SQR(42, ReglerSQR.TYPE_INFO),
    C_SQRT(43, ReglerSQRT.tinfo),
    C_POW(44, ReglerPOW.tinfo),
    C_GE(45, ReglerGreaterEqual.tinfo),
    C_GT(46, ReglerGreaterThan.tinfo),
    C_EQ(47, ReglerEqual.tinfo),
    C_NE(48, ReglerNE.tinfo),
    C_MIN(49, ReglerMIN.tinfo),
    C_MAX(50, ReglerMAX.tinfo),
    C_SIGN(51, ReglerSignum.tinfo),
    C_COUNTER(53, ReglerCounter.tinfo),
    C_TIME(58, ReglerTIME.tinfo),
    C_SPARSEMATRIX(59, ReglerSPARSEMATRIX.tinfo),
    C_CISPR16(60, ReglerCISPR16.tinfo),
    C_JAVA_FUNCTION(61, ReglerJavaFunction.tinfo),
    C_VIEWMOT(62, ReglerVIEWMOT.tinfo),
    C_SPACE_VECTOR(63, ReglerSpaceVector.tinfo),
    C_INT(64, ReglerIntegrator.tinfo),
    C_ABCDQ(65, ReglerABCDQ.tinfo),
    C_DQABC(66, ReglerDQABC.tinfo),
    // careful: this has been moved to SpecialTyp! S_TEXTFIELD(70)),
    C_THYR_CTRL(72, ReglerThyristorControl.tinfo),
    C_U_ZI(73, ReglerU_ZI.tinfo),
    C_TF(74, ReglerTransferFunction.tinfo),
    C_DATA_EXPORT(75, ReglerSaveData.tinfo),
    C_PMSM_CONTROL(76, ReglerPMSMCONTROL.tinfo),
    C_PMSM_MODULATOR(77, ReglerPMSM_Modulator.tinfo),
    C_TERMINAL(78, ReglerTERMINAL.tinfo),
    C_MMFMETER(79, ReglerMMF.tinfo),
    C_FLUXMETER(80, ReglerFluxMeter.tinfo),
    C_GLOBAL_TERMINAL(81, ControlGlobalTerminal.tinfo),
    C_SDFT(82, ReglerSlidingDFT.T_INFO),
    C_MUX(84, ReglerMUX.tinfo),
    C_DEMUX(85, ReglerDemux.tinfo),
    C_DEBUG(86, ReglerControlDebug.tinfo),
    C_SMALL_SIG(87, ReglerSmallSignalAnalysis.TYPE_INFO),
    C_NATIVE_C_FUNCTION(88, ReglerNativeC.tinfo),
    C_SOURCE_IMPORT_DATA(89, ReglerImportFromFile.tinfo) {

        @Override
        public int getTypeNumber() {
            return C_SIGNALSOURCE.getTypeNumber();
        }
        
    },
    C_SOURCE_RANDOM(90, ReglerRandomWalk.tinfo) {

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
    private static Map<Integer, ControlTyp> _backwardMap;

    public static ControlTyp getFromIntNumber(final int intNumber) {

        if (_backwardMap == null) {
            _backwardMap = new HashMap<Integer, ControlTyp>();
            for (ControlTyp typ : values()) {
                _backwardMap.put(typ._intValue, typ);
            }
        }

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