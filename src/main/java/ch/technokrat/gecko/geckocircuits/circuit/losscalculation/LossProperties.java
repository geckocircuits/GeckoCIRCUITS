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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.SchematicTextInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSemiconductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.MOSFET;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SemiconductorLossCalculatable;
import ch.technokrat.modelviewcontrol.ModelMVC;

public final class LossProperties implements AbstractLossCalculatorFabric {

    public final ModelMVC<LossCalculationDetail> _lossType = new ModelMVC<LossCalculationDetail>(LossCalculationDetail.SIMPLE,
            "Loss calculation level: ");
    // Eigenschaften des Halbleiters:
    // falls detaillierte Halbleiter-Verlusteigenschaften spezifiziert sind -->
    //        
    private final AbstractCircuitBlockInterface _parent;    

    public LossProperties(final AbstractSemiconductor parent) {
        _lossCalculationDetailed = new VerlustBerechnungDetailed(parent, this);
        _lossCalculationSimple = new LossCalculationSimple(parent);
        _parent = parent;
    }
    private final LossCalculationSimple _lossCalculationSimple;
    public final VerlustBerechnungDetailed _lossCalculationDetailed;

    public VerlustBerechnungDetailed getDetailedLosses() {
        return _lossCalculationDetailed;
    }
    

    public void exportASCII(final StringBuffer ascii) {
        ascii.append("\n<Verluste>");

        _lossCalculationDetailed.exportASCII(ascii);
        DatenSpeicher.appendAsString(ascii.append("\nverlustTyp"), _lossType.getValue().getOldGeckoCIRCUITSOrdinal());
        ascii.append("\n<\\Verluste>");
    }

    public boolean importASCII(final TokenMap tokenMap) {
        _lossCalculationDetailed.importASCII(tokenMap);
        _lossType.setValue(LossCalculationDetail.getFromDeprecatedFileVersion(tokenMap.readDataLine("verlustTyp", 1)));
        return true;  // 'laden OK'--> true; 'Ladefehler'--> false
    }

    public void copyPropertiesFrom(final LossProperties origLosses) {
        _lossType.setValue(origLosses._lossType.getValue());
        _lossCalculationSimple.copyPropertiesFrom(origLosses._lossCalculationSimple);
        _lossCalculationDetailed.copyPropertiesFrom(origLosses._lossCalculationDetailed);
    }

    public void addTextInfoValue(final SchematicTextInfo textInfo) {

        // ist die Datei mit der Verlustbeschreibung ueberhaupt vorhanden?
        final boolean isLossFileOk = _lossCalculationDetailed.pruefeLinkAufHalbleiterDatei();
        if (_lossType.getValue() == LossCalculationDetail.DETAILED) {
            if (isLossFileOk) {
                textInfo.addParameter(_lossCalculationDetailed.lossFile.getName());
            } else {
                textInfo.addErrorValue("loss-file not found");
            }
        }
    }

    public void setLossType(final LossCalculationDetail lossCalculationDetail) {
        _lossType.setValue(lossCalculationDetail);
    }

    public LossCalculationDetail getLossType() {
        return _lossType.getValue();
    }

    private class LossCalculatorParallelWrapper implements AbstractLossCalculator {

        final AbstractLossCalculator _wrapped;
        private double _totalLosses;
        protected int _numberParalleled = 1;

        public LossCalculatorParallelWrapper(final AbstractLossCalculator toWrap, final int numberParalleled) {
            super();
            _wrapped = toWrap;
            _numberParalleled = numberParalleled;
        }

        @Override
        public void calcLosses(final double current, final double temperature, final double deltaT) {
            final double reducedCurrent = current / _numberParalleled;
            _wrapped.calcLosses(reducedCurrent, temperature, deltaT);
            _totalLosses = _wrapped.getTotalLosses() * _numberParalleled;
        }

        @Override
        public double getTotalLosses() {
            return _totalLosses;
        }
    }

    private class LossCalculatorParallelWrapperWithSplit extends LossCalculatorParallelWrapper
            implements LossCalculationSplittable {

        public LossCalculatorParallelWrapperWithSplit(final AbstractLossCalculator toWrap, final int numberParalleld) {
            super(toWrap, numberParalleld);
            assert toWrap instanceof LossCalculationSplittable;
        }

        @Override
        public double getSwitchingLoss() {
            return ((LossCalculationSplittable) _wrapped).getSwitchingLoss() * _numberParalleled;
        }

        @Override
        public double getConductionLoss() {
            return ((LossCalculationSplittable) _wrapped).getConductionLoss() * _numberParalleled;
        }

    }

    private final class LossCalculatorAdditionalDiode implements AbstractLossCalculator, LossCalculationSplittable {

        private final Diode _diode;
        private final AbstractLossCalculator _original;
        private final AbstractLossCalculator _diodeLosses;
        private final LossCalculationSplittable _splittable;
        private double _conductionLosses;
        private double _switchingLosses;

        public LossCalculatorAdditionalDiode(final AbstractLossCalculator original, final Diode diode) {
            super();
            _diode = diode;
            _original = original;
            assert original instanceof LossCalculationSplittable;
            _splittable = (LossCalculationSplittable) original;
            _diodeLosses = ((SemiconductorLossCalculatable) _diode).getVerlustBerechnung().lossCalculatorFabric();
        }

        @Override
        public void calcLosses(final double current, final double temperature, final double deltaT) {
            _original.calcLosses(current, temperature, deltaT);
            final double diodeCurrent = _diode._currentInAmps;  // aktueller Strom in Diode                                    
            _diodeLosses.calcLosses(diodeCurrent, temperature, deltaT);
            _conductionLosses = _splittable.getConductionLoss() + ((LossCalculationSplittable) _diodeLosses).getConductionLoss();
            _switchingLosses = _splittable.getSwitchingLoss() + ((LossCalculationSplittable) _diodeLosses).getSwitchingLoss();
        }

        @Override
        public double getTotalLosses() {
            return _switchingLosses + _conductionLosses;
        }

        @Override
        public double getSwitchingLoss() {
            return _switchingLosses;
        }

        @Override
        public double getConductionLoss() {
            return _conductionLosses;
        }
    }

    @Override
    public AbstractLossCalculator lossCalculatorFabric() {
        AbstractLossCalculator returnValue = null;

        switch (_lossType.getValue()) {
            case SIMPLE:
                returnValue = _lossCalculationSimple.lossCalculatorFabric();
                break;
            case DETAILED:
                returnValue = _lossCalculationDetailed.lossCalculatorFabric();
                break;
            default:
                assert false;
        }

        if (_parent instanceof MOSFET) {
            final Diode diodeElement = (Diode) ((MOSFET) _parent).getAntiParallelDiode();
            returnValue = new LossCalculatorAdditionalDiode(returnValue, diodeElement);
        }

        if (_parent instanceof AbstractSemiconductor) {
            int numberParalleled = ((AbstractSemiconductor) _parent).numberParalleled.getValue();
            if (numberParalleled > 1) {
                if (returnValue instanceof LossCalculationSplittable) {
                    return new LossCalculatorParallelWrapperWithSplit(returnValue, numberParalleled);
                } else {
                    return new LossCalculatorParallelWrapper(returnValue, numberParalleled);
                }
            }
        }
        return returnValue;

    }
}
