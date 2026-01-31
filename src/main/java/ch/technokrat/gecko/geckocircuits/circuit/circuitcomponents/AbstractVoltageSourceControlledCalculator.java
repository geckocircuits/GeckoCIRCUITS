package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

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
