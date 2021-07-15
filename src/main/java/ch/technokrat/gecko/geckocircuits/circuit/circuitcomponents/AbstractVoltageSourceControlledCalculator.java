package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

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
