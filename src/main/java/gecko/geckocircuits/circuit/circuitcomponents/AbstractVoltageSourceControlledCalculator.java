package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.circuit.calculator.AStampable;
import gecko.core.circuit.calculator.BStampable;
import gecko.core.circuit.calculator.DirectCurrentCalculatable;
import gecko.core.circuit.calculator.HistoryUpdatable;
import gecko.core.circuit.calculator.PostProcessable;
import gecko.core.circuit.calculator.BVector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Stores current control component reference for dependent source calculation")
public abstract class AbstractVoltageSourceControlledCalculator extends AbstractVoltageSourceCalculator {
    protected double _gain = 1;
    protected DirectCurrentCalculatable _currentControl;

   public AbstractVoltageSourceControlledCalculator(final AbstractVoltageSource parent) {
       super(parent);
   }

    public final void setGain(final double value) {
        _gain = value;
    }

    public final void setCurrentControlComponent(final DirectCurrentCalculatable value) {
        _currentControl = value;
    }

}
