package gecko.geckocircuits.circuit;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicZoomTest {

    @Test
    public void wheelUpZoomsIn() {
        assertEquals(18, SchematicEditor2.nextZoomLevel(16, -1));
    }

    @Test
    public void wheelDownZoomsOut() {
        assertEquals(14, SchematicEditor2.nextZoomLevel(16, 1));
    }

    @Test
    public void clampsAtMinimum() {
        assertEquals(10, SchematicEditor2.nextZoomLevel(10, 1));
    }

    @Test
    public void clampsAtMaximum() {
        assertEquals(18, SchematicEditor2.nextZoomLevel(18, -1));
    }

    @Test
    public void offGridValueMovesToNextLevelInWheelDirection() {
        assertEquals(16, SchematicEditor2.nextZoomLevel(15, -1));
        assertEquals(14, SchematicEditor2.nextZoomLevel(15, 1));
    }

    @Test
    public void eachWheelNotchStepsOneLevel() {
        assertEquals(16, SchematicEditor2.nextZoomLevel(10, -3));
        assertEquals(12, SchematicEditor2.nextZoomLevel(18, 3));
    }

    @Test
    public void zeroRotationKeepsCurrentValue() {
        assertEquals(15, SchematicEditor2.nextZoomLevel(15, 0));
    }

    @Test
    public void largeRotationsClampWithoutOverflow() {
        assertEquals(18, SchematicEditor2.nextZoomLevel(14, Integer.MIN_VALUE));
        assertEquals(10, SchematicEditor2.nextZoomLevel(14, Integer.MAX_VALUE));
    }

    @Test
    public void plainWheelScrollsWithoutZooming() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel sheet = new JPanel();
            sheet.setPreferredSize(new Dimension(2000, 2000));
            JScrollPane pane = new JScrollPane(sheet);
            pane.setSize(200, 200);
            pane.doLayout();
            pane.getViewport().doLayout();
            sheet.addMouseWheelListener(SchematicEditor2::handleMouseWheel);
            int originalScale = AbstractCircuitSheetComponent.dpix;
            int originalScroll = pane.getVerticalScrollBar().getValue();
            MouseWheelEvent event = wheelEvent(sheet, 0, 1);

            sheet.dispatchEvent(event);

            assertEquals(originalScale, AbstractCircuitSheetComponent.dpix);
            assertFalse(event.isConsumed());
            assertTrue(pane.getVerticalScrollBar().getValue() > originalScroll);
        });
    }

    @Test
    public void controlWheelUsesRenderedScaleAndDoesNotScroll() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int previousScale = AbstractCircuitSheetComponent.dpix;
            int previousModel = AbstractCircuitSheetComponent.dpixValue.getValue();
            SchematicEditor2 previousEditor = SchematicEditor2.Singleton;
            try {
                SchematicEditor2.Singleton = null;
                AbstractCircuitSheetComponent.dpixValue.setValueWithoutUndo(10);
                AbstractCircuitSheetComponent.dpix = 16;
                JPanel sheet = new JPanel();
                JScrollPane pane = new JScrollPane(sheet);
                sheet.addMouseWheelListener(SchematicEditor2::handleMouseWheel);
                MouseWheelEvent event = wheelEvent(sheet, InputEvent.CTRL_DOWN_MASK, -1);

                sheet.dispatchEvent(event);

                assertEquals(Integer.valueOf(18), AbstractCircuitSheetComponent.dpixValue.getValue());
                assertEquals(18, AbstractCircuitSheetComponent.dpix);
                assertTrue(event.isConsumed());
                assertEquals(0, pane.getVerticalScrollBar().getValue());
            } finally {
                AbstractCircuitSheetComponent.dpixValue.setValueWithoutUndo(previousModel);
                AbstractCircuitSheetComponent.dpix = previousScale;
                SchematicEditor2.Singleton = previousEditor;
            }
        });
    }

    private static MouseWheelEvent wheelEvent(JPanel sheet, int modifiers, int rotation) {
        return new MouseWheelEvent(sheet, MouseEvent.MOUSE_WHEEL, 1L, modifiers,
                20, 20, 0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation);
    }
}
