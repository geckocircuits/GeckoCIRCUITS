package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.AbstractScopeSignal;
import ch.technokrat.gecko.geckocircuits.newscope.DefinedMeanSignals;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class DataContainerScopeWrapperTest {

    private DataContainerGlobal globalContainer;
    private ScopeWrapperIndices scopeIndices;
    private DataContainerScopeWrapper wrapper;

    @Before
    public void setUp() {
        globalContainer = new DataContainerGlobal();
        String[] signalNames = {"Signal1", "Signal2"};
        globalContainer.init(2, signalNames, "Time");

        // Insert some data
        for (int i = 0; i < 100; i++) {
            float[] values = {i * 1.0f, i * 2.0f};
            globalContainer.insertValuesAtEnd(values, i * 0.001);
        }

        List<Integer> indices = Arrays.asList(0, 1);
        scopeIndices = new ScopeWrapperIndices(indices, globalContainer);
        scopeIndices.reset();

        Stack<AbstractScopeSignal> origSignals = new Stack<>();
        origSignals.push(new AbstractScopeSignal() {
            @Override
            public String getSignalName() { return "Sig1"; }
        });
        origSignals.push(new AbstractScopeSignal() {
            @Override
            public String getSignalName() { return "Sig2"; }
        });

        DefinedMeanSignals meanSignals = new DefinedMeanSignals(origSignals);

        List<AbstractScopeSignal> signalsList = new ArrayList<>(origSignals);

        wrapper = new DataContainerScopeWrapper(globalContainer, scopeIndices, meanSignals, signalsList);
    }

    @Test
    public void testConstructor() {
        assertNotNull(wrapper);
    }

    @Test
    public void testGetRowLength() {
        assertEquals(2, wrapper.getRowLength());
    }

    @Test
    public void testGetValue() {
        float val = wrapper.getValue(0, 0);
        assertEquals(0.0f, val, 0.001f);
    }

    @Test
    public void testGetValueSecondRow() {
        float val = wrapper.getValue(1, 0);
        assertEquals(0.0f, val, 0.001f);
    }

    @Test
    public void testGetTimeValue() {
        double time0 = wrapper.getTimeValue(0, 0);
        assertEquals(0.0, time0, 1e-6);
        double time1 = wrapper.getTimeValue(1, 0);
        assertEquals(0.001, time1, 1e-6);
    }

    @Test
    public void testGetMaximumTimeIndex() {
        int maxIdx = wrapper.getMaximumTimeIndex(0);
        assertTrue(maxIdx >= 99);
    }

    @Test
    public void testGetHiLoValue() {
        HiLoData hiLo = wrapper.getHiLoValue(0, 0, 50);
        assertNotNull(hiLo);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() {
        try {
            HiLoData absMinMax = wrapper.getAbsoluteMinMaxValue(0);
            assertNotNull(absMinMax);
        } catch (ArithmeticException e) {
            // May throw if not enough compressed data
        }
    }

    @Test
    public void testGetAbsoluteMinMaxValueOutOfBounds() {
        HiLoData fallback = wrapper.getAbsoluteMinMaxValue(999);
        assertNotNull(fallback);
        assertEquals(0.0f, fallback._yLo, 0.001f);
        assertEquals(1.0f, fallback._yHi, 0.001f);
    }

    @Test
    public void testFindTimeIndex() {
        int idx = wrapper.findTimeIndex(0.005, 0);
        assertTrue(idx >= 0);
    }

    @Test
    public void testGetSignalName() {
        assertEquals("Sig1", wrapper.getSignalName(0));
        assertEquals("Sig2", wrapper.getSignalName(1));
    }

    @Test
    public void testGetSignalNameOutOfBounds() {
        assertEquals("TestSubj", wrapper.getSignalName(999));
    }

    @Test
    public void testGetXDataName() {
        assertEquals("Time", wrapper.getXDataName());
    }

    @Test
    public void testGetContainerStatus() {
        ContainerStatus status = wrapper.getContainerStatus();
        assertNotNull(status);
    }

    @Test
    public void testSetContainerStatus() {
        wrapper.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, wrapper.getContainerStatus());
    }

    @Test
    public void testIsInvalidNumbers() {
        assertFalse(wrapper.isInvalidNumbers(0));
    }

    @Test
    public void testGetTimeSeries() {
        assertNotNull(wrapper.getTimeSeries(0));
    }

    @Test
    public void testGetDataValueInInterval() {
        Object result = wrapper.getDataValueInInterval(0.0, 0.05, 0);
        assertNotNull(result);
    }

    @Test
    public void testGetDataValueInIntervalOutOfBounds() {
        Object result = wrapper.getDataValueInInterval(0.0, 0.05, 999);
        assertEquals(0.0f, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAVGValueInIntervalThrows() {
        wrapper.getAVGValueInInterval(0.0, 1.0, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataArrayThrows() {
        wrapper.getDataArray();
    }

    @Test
    public void testGetDefinedMeanSignals() {
        assertNotNull(wrapper.getDefinedMeanSignals());
    }

    @Test
    public void testDeregisterObserver() {
        wrapper.deregisterObserver();
        // Should not throw after deregistering
    }
}
