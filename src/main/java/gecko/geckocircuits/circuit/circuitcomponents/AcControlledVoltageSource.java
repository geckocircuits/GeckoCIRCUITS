package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.ac.AcComponent;
import gecko.core.io.CircuitTopology.Side;
import gecko.core.io.SpicePorts;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.AbstractTerminal;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.AcEditorDialogs;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.geckocircuits.control.Point;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.List;

/** Native four-terminal voltage-controlled voltage source for linear AC. */
public final class AcControlledVoltageSource extends AbstractCircuitBlockInterface {
    static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(AcControlledVoltageSource.class, "VCVS",
            I18nKeys.AC_CONTROLLED_VOLTAGE_SOURCE);

    public AcControlledVoltageSource() {
        for (var pin : SpicePorts.E.ports()) {
            (pin.side() == Side.INPUT ? XIN : YOUT).add(new Pin(this, pin));
        }
        parameter[0] = 1;
    }

    @Override
    protected Window openDialogWindow() {
        return AcEditorDialogs.componentDialog(this);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new IllegalArgumentException("Use AC analysis for the voltage-controlled voltage source.");
    }

    @Override
    void drawConnectorLines(Graphics2D graphics) {
        graphics.drawLine(0, -2 * dpix, 0, 2 * dpix);
        int side = mirrored() ? 1 : -1;
        graphics.drawLine(side * 2 * dpix, -dpix, side * dpix, -dpix);
        graphics.drawLine(side * dpix, -dpix, side * dpix / 2, -dpix / 2);
        graphics.drawLine(side * 2 * dpix, dpix, side * dpix, dpix);
        graphics.drawLine(side * dpix, dpix, side * dpix / 2, dpix / 2);
    }

    @Override
    protected void drawBackground(Graphics2D graphics) {
        int width = 3 * dpix / 4;
        graphics.fillPolygon(new int[]{0, width, 0, -width}, new int[]{-dpix, 0, dpix, 0}, 4);
    }

    @Override
    void drawForeground(Graphics2D graphics) {
        int width = 3 * dpix / 4;
        graphics.drawPolygon(new int[]{0, width, 0, -width}, new int[]{-dpix, 0, dpix, 0}, 4);
        int side = mirrored() ? 1 : -1;
        graphics.drawString("+", -dpix / 4, 0);
        graphics.drawString("−", -dpix / 4, dpix / 2);
        graphics.drawString("+", side * 3 * dpix / 2, -dpix - 2);
        graphics.drawString("−", side * 3 * dpix / 2, dpix - 2);
        var owned = getAcComponent();
        boolean laplace = owned != null && owned.law() instanceof AcComponent.Controlled law && law.laplace();
        graphics.drawString(laplace ? "H(s)" : "E", dpix, 0);
    }

    private boolean mirrored() { return getAcComponent() != null && getAcComponent().mirrored(); }

    private static final class Pin extends AbstractTerminal {
        private final SpicePorts.PortSpec spec;

        Pin(AbstractBlockInterface parent, SpicePorts.PortSpec spec) {
            super(parent);
            this.spec = spec;
        }

        @Override
        public Point getPosition() {
            var offset = spec.nativeOffset(_parentElement.getComponentDirection().code(),
                    ((AcControlledVoltageSource) _parentElement).mirrored());
            var center = _parentElement.getSheetPosition();
            return new Point(center.x + offset.x(), center.y + offset.y());
        }

        @Override
        public AbstractTerminal createCopy(AbstractBlockInterface parent) {
            var copy = new Pin(parent, spec);
            copy.getLabelObject().setLabelWithoutUndo(getLabelObject().getLabelString());
            return copy;
        }
    }
}
