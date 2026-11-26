package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.io.SpicePorts;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.List;

/** Native four-terminal voltage-controlled current source for linear AC. */
public final class AcControlledCurrentSource extends AcControlledSource {
    static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(AcControlledCurrentSource.class, "VCCS",
            I18nKeys.AC_CONTROLLED_CURRENT_SOURCE);

    public AcControlledCurrentSource() {
        super(SpicePorts.G);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new IllegalArgumentException("Use AC analysis for the voltage-controlled current source.");
    }

    @Override
    void drawForeground(Graphics2D graphics) {
        drawDiamond(graphics);
        int head = dpix / 3;
        graphics.drawLine(0, dpix / 2, 0, -head);
        graphics.drawLine(0, -head, -head / 2, 0);
        graphics.drawLine(0, -head, head / 2, 0);
        graphics.drawString("G", dpix, 0);
    }
}
