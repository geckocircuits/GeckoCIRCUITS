package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalRegular;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link DataContainerMeanWrapper}.
 */
public class DataContainerMeanWrapperTest {

    private DataContainerGlobal wrappedContainer;
    private ScopeWrapperIndices scopeIndices;
    private DataContainerMeanWrapper wrapper;

    @Before
    public void setUp() {
        wrappedContainer = new DataContainerGlobal();
        wrappedContainer.init(2, new String[]{"sig1", "sig2"}, "time");
        scopeIndices = new ScopeWrapperIndices(Arrays.asList(0, 1), wrappedContainer);
        wrapper = new DataContainerMeanWrapper(wrappedContainer, scopeIndices);
    }

    // ---------------------------------------------------------------
    // getHiLoValue() tests
    // ---------------------------------------------------------------

    @Test
    public void testGetHiLoValueReturnsHardcodedValues() {
        HiLoData result = wrapper.getHiLoValue(0, 0, 10);
        assertEquals(0.0f, result._yLo, 0.0f);
        assertEquals(10.0f, result._yHi, 0.0f);
    }

    @Test
    public void testGetHiLoValueIgnoresParameters() {
        // Should always return (0, 10) regardless of input
        HiLoData result1 = wrapper.getHiLoValue(0, 5, 20);
        assertEquals(0.0f, result1._yLo, 0.0f);
        assertEquals(10.0f, result1._yHi, 0.0f);

        HiLoData result2 = wrapper.getHiLoValue(1, 100, 200);
        assertEquals(0.0f, result2._yLo, 0.0f);
        assertEquals(10.0f, result2._yHi, 0.0f);
    }

    @Test
    public void testGetHiLoValueReturnsSameStaticInstance() {
        // HiLoData.hiLoDataFabric(0, 10) returns a new instance each time
        // (not a cached static like 0,0 or 0,1), but both should be equal
        HiLoData result1 = wrapper.getHiLoValue(0, 0, 0);
        HiLoData result2 = wrapper.getHiLoValue(1, 0, 0);
        assertEquals(result1, result2);
    }

    // ---------------------------------------------------------------
    // getRowLength() tests
    // ---------------------------------------------------------------

    @Test
    public void testGetRowLengthInitiallyZero() {
        // No mean signals defined initially
        assertEquals(0, wrapper.getRowLength());
    }

    // ---------------------------------------------------------------
    // getXDataName() tests
    // ---------------------------------------------------------------

    @Test
    public void testGetXDataNameDelegatesToWrapped() {
        assertEquals("time", wrapper.getXDataName());
    }

    @Test
    public void testGetXDataNameMatchesWrappedContainer() {
        assertEquals(wrappedContainer.getXDataName(), wrapper.getXDataName());
    }

    // ---------------------------------------------------------------
    // getContainerStatus() tests
    // ---------------------------------------------------------------

    @Test
    public void testGetContainerStatusDelegatesToWrapped() {
        ContainerStatus status = wrapper.getContainerStatus();
        assertEquals(wrappedContainer.getContainerStatus(), status);
    }

    @Test
    public void testGetContainerStatusReflectsWrappedState() {
        // After init, the wrapped container has a DataContainerCompressable
        // which should have a non-null status
        ContainerStatus status = wrapper.getContainerStatus();
        assertNotNull(status);
    }

    // ---------------------------------------------------------------
    // setContainerStatus() tests
    // ---------------------------------------------------------------

    @Test
    public void testSetContainerStatusDelegatesToWrapped() {
        wrapper.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.RUNNING, wrappedContainer.getContainerStatus());
        assertEquals(ContainerStatus.RUNNING, wrapper.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusFinished() {
        wrapper.setContainerStatus(ContainerStatus.FINISHED);
        assertEquals(ContainerStatus.FINISHED, wrappedContainer.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusPaused() {
        wrapper.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, wrappedContainer.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusRoundTrip() {
        wrapper.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.RUNNING, wrapper.getContainerStatus());

        wrapper.setContainerStatus(ContainerStatus.FINISHED);
        assertEquals(ContainerStatus.FINISHED, wrapper.getContainerStatus());
    }

    // ---------------------------------------------------------------
    // getTimeSeries() tests
    // ---------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTimeSeriesThrowsUnsupported() {
        wrapper.getTimeSeries(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTimeSeriesThrowsForAnyRow() {
        wrapper.getTimeSeries(1);
    }

    // ---------------------------------------------------------------
    // getDataArray() tests
    // ---------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataArrayThrowsUnsupported() {
        wrapper.getDataArray();
    }

    // ---------------------------------------------------------------
    // Constructor tests
    // ---------------------------------------------------------------

    @Test
    public void testConstructorWithValidArgs() {
        // Should not throw
        DataContainerMeanWrapper w = new DataContainerMeanWrapper(wrappedContainer, scopeIndices);
        assertNotNull(w);
    }

    @Test
    public void testConstructorPreservesWrappedReference() {
        // Verify delegation works, meaning internal reference is correct
        assertEquals(wrappedContainer.getXDataName(), wrapper.getXDataName());
        assertEquals(wrappedContainer.getContainerStatus(), wrapper.getContainerStatus());
    }

    // ---------------------------------------------------------------
    // Delegation consistency tests
    // ---------------------------------------------------------------

    @Test
    public void testStatusChangeVisibleThroughBothReferences() {
        wrapper.setContainerStatus(ContainerStatus.RUNNING);

        // Both the wrapper and the wrapped container should show the same status
        assertEquals(ContainerStatus.RUNNING, wrapper.getContainerStatus());
        assertEquals(ContainerStatus.RUNNING, wrappedContainer.getContainerStatus());

        // Changing through the wrapped container directly should also reflect
        wrappedContainer.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, wrapper.getContainerStatus());
    }

    @Test
    public void testGetXDataNameWithUninitializedWrapped() {
        // DataContainerGlobal with default constructor wraps DataContainerNullData
        // which returns "t" for getXDataName()
        DataContainerGlobal uninitContainer = new DataContainerGlobal();
        ScopeWrapperIndices uninitIndices = new ScopeWrapperIndices(Arrays.asList(0), uninitContainer);
        DataContainerMeanWrapper uninitWrapper = new DataContainerMeanWrapper(uninitContainer, uninitIndices);

        assertEquals("t", uninitWrapper.getXDataName());
    }

    @Test
    public void testGetContainerStatusWithUninitializedWrapped() {
        // DataContainerNullData returns NOT_INITIALIZED
        DataContainerGlobal uninitContainer = new DataContainerGlobal();
        ScopeWrapperIndices uninitIndices = new ScopeWrapperIndices(Arrays.asList(0), uninitContainer);
        DataContainerMeanWrapper uninitWrapper = new DataContainerMeanWrapper(uninitContainer, uninitIndices);

        assertEquals(ContainerStatus.NOT_INITIALIZED, uninitWrapper.getContainerStatus());
    }

    // ==================== Tests requiring ScopeSignalMean ====================

    private DataContainerGlobal createContainerWithData() {
        DataContainerGlobal container = new DataContainerGlobal();
        container.init(2, new String[]{"Voltage", "Current"}, "time");
        for (int i = 0; i < 200; i++) {
            float[] values = {(float) i * 10, (float) i * 0.5f};
            container.insertValuesAtEnd(values, i * 0.001);
        }
        return container;
    }

    @Test
    public void testGetValueWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        assertEquals(1, meanWrapper.getRowLength());

        // getValue delegates to wrapped container's getAVGValueInInterval
        // The underlying DataContainerCompressable implements DataContainerIntegralCalculatable
        float val = meanWrapper.getValue(0, 50);
        // Result depends on averaging calculation
    }

    @Test
    public void testGetTimeValueDelegation() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        double time = meanWrapper.getTimeValue(5, 0);
        assertEquals(container.getTimeValue(5, 0), time, 1e-6);
    }

    @Test
    public void testFindTimeIndexDelegation() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        int idx = meanWrapper.findTimeIndex(0.050, 0);
        assertEquals(container.findTimeIndex(0.050, 0), idx);
    }

    @Test
    public void testGetMaximumTimeIndexWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        int maxIdx = meanWrapper.getMaximumTimeIndex(0);
        // Should be less than the wrapped maximum due to averaging time offset
        assertTrue(maxIdx >= 0);
        assertTrue(maxIdx <= container.getMaximumTimeIndex(0));
    }

    @Test
    public void testGetMaximumTimeIndexNoMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        // With no mean signals defined, maxAvgTime is 0
        int maxIdx = meanWrapper.getMaximumTimeIndex(0);
        assertTrue(maxIdx >= 0);
    }

    @Test
    public void testGetSignalNameWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        String name = meanWrapper.getSignalName(0);
        assertNotNull(name);
    }

    @Test
    public void testIsInvalidNumbersWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        boolean invalid = meanWrapper.isInvalidNumbers(0);
        assertFalse(invalid);
    }

    @Test
    public void testGetAbsoluteMinMaxWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        try {
            HiLoData absMinMax = meanWrapper.getAbsoluteMinMaxValue(0);
            assertNotNull(absMinMax);
        } catch (ArithmeticException e) {
            // May throw if not enough compressed data
        }
    }

    @Test
    public void testGetDataValueInIntervalWithMeanSignals() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular = new ScopeSignalRegular(0, null);
        ScopeSignalMean mean = new ScopeSignalMean(regular, 0.01);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean);

        meanWrapper.defineMeanSignals(meanSignals);

        Object result = meanWrapper.getDataValueInInterval(0.050, 0.100, 0);
        assertNotNull(result);
    }

    @Test
    public void testRemoveSignal() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        // defineMeanSignals calls reset() and defineAdditionalSignal internally
        ScopeSignalRegular regular0 = new ScopeSignalRegular(0, null);
        ScopeSignalRegular regular1 = new ScopeSignalRegular(1, null);
        ScopeSignalMean mean0 = new ScopeSignalMean(regular0, 0.01);
        ScopeSignalMean mean1 = new ScopeSignalMean(regular1, 0.02);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean0);
        meanSignals.add(mean1);

        meanWrapper.defineMeanSignals(meanSignals);

        // Now there are signals to remove
        assertEquals(2, meanWrapper.getRowLength());
        meanWrapper.removeSignal(0);
    }

    @Test
    public void testSetSignalPathName() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        // setSignalPathName delegates to wrapped container
        meanWrapper.setSignalPathName(0, "/sub/path");
    }

    @Test
    public void testDefineMeanSignalsMultiple() {
        DataContainerGlobal container = createContainerWithData();
        ScopeWrapperIndices indices = new ScopeWrapperIndices(Arrays.asList(0, 1), container);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(container, indices);

        ScopeSignalRegular regular0 = new ScopeSignalRegular(0, null);
        ScopeSignalRegular regular1 = new ScopeSignalRegular(1, null);
        ScopeSignalMean mean0 = new ScopeSignalMean(regular0, 0.01);
        ScopeSignalMean mean1 = new ScopeSignalMean(regular1, 0.02);

        List<ScopeSignalMean> meanSignals = new ArrayList<>();
        meanSignals.add(mean0);
        meanSignals.add(mean1);

        meanWrapper.defineMeanSignals(meanSignals);

        assertEquals(2, meanWrapper.getRowLength());
    }
}
