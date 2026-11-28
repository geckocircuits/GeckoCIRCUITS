package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.core.io.CircuitTopology.Side;
import gecko.core.io.SpicePorts;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.AbstractTerminal;
import gecko.geckocircuits.circuit.AcEditorDialogs;
import gecko.geckocircuits.control.Point;
import java.awt.Graphics2D;
import java.awt.Window;

abstract class AcControlledSource extends AbstractCircuitBlockInterface {
    AcControlledSource(SpicePorts ports) {
        for (var pin : ports.ports()) {
            (pin.side() == Side.INPUT ? XIN : YOUT).add(new Pin(this, pin));
        }
        parameter[0] = 1;
    }

    @Override
    protected Window openDialogWindow() {
        return AcEditorDialogs.componentDialog(this);
    }

    final void drawDiamond(Graphics2D graphics) {
        int width = 3 * dpix / 4;
        graphics.drawPolygon(new int[]{0, width, 0, -width}, new int[]{-dpix, 0, dpix, 0}, 4);
        int side = mirrored() ? 1 : -1;
        graphics.drawString("+", side * 3 * dpix / 2, -dpix - 2);
        graphics.drawString("−", side * 3 * dpix / 2, dpix - 2);
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

    final boolean mirrored() { return getAcComponent() != null && getAcComponent().mirrored(); }

    private static final class Pin extends AbstractTerminal {
        private final SpicePorts.PortSpec spec;

        Pin(AbstractBlockInterface parent, SpicePorts.PortSpec spec) {
            super(parent);
            this.spec = spec;
        }

        @Override
        public Point getPosition() {
            var offset = spec.nativeOffset(_parentElement.getComponentDirection().code(),
                    ((AcControlledSource) _parentElement).mirrored());
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
