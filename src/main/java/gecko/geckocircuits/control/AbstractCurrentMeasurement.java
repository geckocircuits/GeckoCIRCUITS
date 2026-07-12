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
package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.*;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.Diode;
import gecko.geckocircuits.circuit.circuitcomponents.MOSFET;
import gecko.geckocircuits.circuit.circuitcomponents.ThermPvChip;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.control.calculators.NothingToDoCalculator;
import java.awt.Window;

public abstract class AbstractCurrentMeasurement extends ControlWithSingleReference {

    public AbstractCurrentMeasurement() {
        super(0, 1);
    }

    private static class CurrentCalculation extends AbstractControlCalculatable {
        private final AbstractCircuitBlockInterface _coupled;

        public CurrentCalculation(final AbstractCircuitBlockInterface coupled) {
            super(0, 1);
            _coupled = coupled;
        }

        @Override
        public void calculateYOUT(final double deltaT) {
            _outputSignal[0][0] = _coupled._currentInAmps;

        }
    }

    private static class MOSFETCurrentCalculation extends AbstractControlCalculatable {
        private final MOSFET _mosefet;
        private final Diode _antiParallelDiode;

        public MOSFETCurrentCalculation(final MOSFET mosfet) {
            super(0, 1);
            _mosefet = mosfet;
            _antiParallelDiode = mosfet.getAntiParallelDiode();
        }

        @Override
        public void calculateYOUT(final double deltaT) {
            // Bugfix in 171, release 49: current of freewheeling diode had wrong sign.
            // the diode is antiparallel to the Mosfet component, therefore a "-" is required.
            _outputSignal[0][0] = _mosefet._currentInAmps - _antiParallelDiode._currentInAmps;
        }
    }

    private static class ThermPvChipFlowCalculation extends AbstractControlCalculatable {
        private final ThermPvChip _lossSource;
        private final ControlFlowMeter _flowMeasurement;

        public ThermPvChipFlowCalculation(final ThermPvChip loss, final ControlFlowMeter measurement) {
            super(0,1);
            _lossSource = loss;
            _flowMeasurement = measurement;
        }

        @Override
        public void calculateYOUT(final double deltaT) {
            switch (_flowMeasurement.getLossComponentBeingMeasured()) {
                case TOTAL:
                    _outputSignal[0][0] = _lossSource.getTotalLosses();
                    break;
                case CONDUCTION:
                    _outputSignal[0][0] = _lossSource.getConductionLosses();
                    break;
                case SWITCHING:
                    _outputSignal[0][0] = _lossSource.getSwitchngLosses();
                    break;
                default:
                    _outputSignal[0][0] = _lossSource._currentInAmps;
                    break;
            }
        }
    }


    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        final AbstractBlockInterface coupled = (AbstractCircuitBlockInterface) _coupling._coupledElements[0];
        if (coupled != null) {
            if(coupled instanceof MOSFET) {
                return new MOSFETCurrentCalculation((MOSFET) _coupling._coupledElements[0]);
            } else if (coupled instanceof ThermPvChip && this instanceof ControlFlowMeter) {
                return new ThermPvChipFlowCalculation((ThermPvChip) _coupling._coupledElements[0], (ControlFlowMeter) this);
            } else {
                return new CurrentCalculation((AbstractCircuitBlockInterface) _coupling._coupledElements[0]);
            }
        } else { // no signal attached, nothing todo!
            return new NothingToDoCalculator(0, 1);
        }

    }

    abstract String getVariableForDisplay();

    @Override
    String getDisplayValueWithoutError() {
        AbstractCircuitBlockInterface coupledComponent = (AbstractCircuitBlockInterface) getComponentCoupling()._coupledElements[0];
        return getVariableForDisplay() + " [" + coupledComponent.getStringID() + "]";
    }

    @Override
    protected final Window openDialogWindow() {
        return new ControlAmpereMeterDialog(this);
    }


}
