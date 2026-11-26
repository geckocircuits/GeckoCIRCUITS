package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.ac.AcComponent;
import gecko.core.io.SpicePorts;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.List;

/** Native four-terminal voltage-controlled voltage source for linear AC. */
public final class AcControlledVoltageSource extends AcControlledSource {
    static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(AcControlledVoltageSource.class, "VCVS",
            I18nKeys.AC_CONTROLLED_VOLTAGE_SOURCE);

    public AcControlledVoltageSource() {
        super(SpicePorts.E);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new IllegalArgumentException("Use AC analysis for the voltage-controlled voltage source.");
    }

    @Override
    void drawForeground(Graphics2D graphics) {
        drawDiamond(graphics);
        graphics.drawString("+", -dpix / 4, 0);
        graphics.drawString("−", -dpix / 4, dpix / 2);
        var owned = getAcComponent();
        boolean laplace = owned != null && owned.law() instanceof AcComponent.Controlled law && law.laplace();
        graphics.drawString(laplace ? "H(s)" : "E", dpix, 0);
    }
}
