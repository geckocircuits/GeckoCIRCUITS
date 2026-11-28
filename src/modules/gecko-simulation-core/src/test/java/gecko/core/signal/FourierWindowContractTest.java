package gecko.core.signal;

import gecko.core.GeckoInvalidArgumentException;
import gecko.core.datacontainer.DataContainerSimple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class FourierWindowContractTest {
    private DataContainerSimple constant(double origin, int intervals) {
        var data = DataContainerSimple.fabricArrayTimeSeries(1, intervals + 1);
        for (int i = 0; i <= intervals; i++) data.insertValuesAtEnd(new float[]{3}, origin + i / 32.0);
        return data;
    }

    @Test
    void includesTheFirstSampleAndUsesTheFullRequestedInterval() throws Exception {
        var data = DataContainerSimple.fabricArrayTimeSeries(1, 9);
        for (int i = 0; i <= 8; i++) data.insertValuesAtEnd(new float[]{i == 0 ? 8 : 0}, i / 8.0);
        double[][][] result = new FourierGUIless(data, 0, 1, 3).doFourier();
        assertEquals(1, result[2][0][0], 1e-6);
    }

    @Test
    void aLargeClockOriginDoesNotCollapseTheFourierDuration() throws Exception {
        double[][][] result = new FourierGUIless(constant(1e12, 32), 1e12, 1e12 + 1, 3).doFourier();
        assertEquals(3, result[2][0][0], 1e-6);
        assertEquals(0, result[2][0][1], 1e-6);
    }

    @Test
    void oneRecordedIntervalCanSupplyDcWithoutAnArrayFailure() throws Exception {
        double[][][] result = new FourierGUIless(constant(10, 1), 10, 10 + 1.0 / 32, 0).doFourier();
        assertEquals(3, result[2][0][0], 1e-6);
    }

    @Test
    void harmonicCountMustFitTheAvailableIntervals() {
        assertThrows(GeckoInvalidArgumentException.class,
                () -> new FourierGUIless(constant(0, 8), 0, 0.25, 4).doFourier());
    }

    @Test
    void boundsOutsideRecordedTimeRejectInsteadOfExtrapolating() {
        assertThrows(GeckoInvalidArgumentException.class,
                () -> new FourierGUIless(constant(10, 32), 9, 11, 3).doFourier());
        assertThrows(GeckoInvalidArgumentException.class,
                () -> new FourierGUIless(constant(10, 32), 10, 12, 3).doFourier());
    }
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
    void nonfiniteWindowBoundsReject(double bound) {
        assertThrows(GeckoInvalidArgumentException.class,
                () -> new FourierGUIless(constant(0, 32), bound, 1, 3).doFourier());
        assertThrows(GeckoInvalidArgumentException.class,
                () -> new FourierGUIless(constant(0, 32), 0, bound, 3).doFourier());
    }

    @Test
    void rejectsEmptyOrNonmonotonicRecordedClocks() {
        var empty = DataContainerSimple.fabricArrayTimeSeries(1, 2);
        assertThrows(GeckoInvalidArgumentException.class, () -> new FourierGUIless(empty, 0, 1, 0).doFourier());
        for (double second : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var data = DataContainerSimple.fabricArrayTimeSeries(1, 3);
            data.insertValuesAtEnd(new float[]{1}, 0);
            data.insertValuesAtEnd(new float[]{2}, second);
            data.insertValuesAtEnd(new float[]{3}, 1);
            assertThrows(GeckoInvalidArgumentException.class, () -> new FourierGUIless(data, 0, 1, 0).doFourier());
        }
    }

    @Test
    void nonuniformSamplesUseThePrecedingValueAtEachFourierSample() throws Exception {
        var data = DataContainerSimple.fabricArrayTimeSeries(1, 5);
        double[] time = {0, 0.125, 0.5, 0.75, 1};
        float[] value = {8, 0, 4, 0, 99};
        for (int i = 0; i < time.length; i++) data.insertValuesAtEnd(new float[]{value[i]}, time[i]);
        assertEquals(3, new FourierGUIless(data, 0, 1, 1).doFourier()[2][0][0], 1e-6);
    }
}
