package gecko.core.simulation;

import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.GainCalculator;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.control.calculators.SubtractionTwoParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayedControlFeedbackContractTest {
    @ParameterizedTest
    @MethodSource("bufferedOrders")
    void bufferedFeedbackDoesNotAddSchedulingLatency(double delayTime, int position, double[] expected) throws Exception {
        var one = new ConstantCalculator(1);
        var delay = new DelayCalculator(delayTime);
        var feedback = new GainCalculator(-0.5);
        var sum = new SubtractionTwoParameter();
        var scope = new NothingToDoCalculator(1, 0);
        feedback.setInputSignal(0, delay, 0);
        sum.setInputSignal(0, one, 0);
        sum.setInputSignal(1, feedback, 0);
        delay.setInputSignal(0, sum, 0);
        scope.setInputSignal(0, sum, 0);
        delay.initializeAtSimulationStart(0.1);
        var calculators = new ArrayList<AbstractControlCalculatable>(List.of(one, feedback, sum, scope));
        calculators.add(position, delay);
        var netlist = ControlNetlist.fromOrderedCalculators(calculators);
        var output = new ScopeDataExtractor(scope, new String[]{"feedback"});

        // The recurrence is y[n] = 1 + 0.5 * y[n-delaySteps], with zero initial history.
        for (int step = 0; step < expected.length; step++) {
            netlist.executeTimeStep(0.1, step * 0.1);
            assertEquals(expected[step], output.extractCurrentValues()[0], 1e-6, "step " + step);
        }
    }

    static Stream<Arguments> bufferedOrders() {
        return IntStream.rangeClosed(0, 4).boxed().flatMap(position -> Stream.of(
                Arguments.of(0.1, position, new double[]{1, 1.5, 1.75, 1.875, 1.9375}),
                Arguments.of(0.2, position, new double[]{1, 1, 1.5, 1.5, 1.75, 1.75})));
    }

    @Test
    void emptySlotsAndNoncalculatingBlocksPreserveExecution() throws Exception {
        var source = new ConstantCalculator(2);
        var gain = new GainCalculator(3);
        gain.setInputSignal(0, source, 0);
        var netlist = ControlNetlist.fromOrderedCalculators(
                Arrays.asList(null, source, new NothingToDoCalculator(0, 0), gain, null));
        netlist.executeTimeStep(0.1, 0.2);
        assertEquals(6, gain._outputSignal[0][0]);
        assertEquals(0.2, netlist.getSimulationTime());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.05})
    void zeroAndSubstepDelaysKeepDirectFeedthrough(double delayTime) throws Exception {
        var source = new ConstantCalculator(7);
        var delay = new DelayCalculator(delayTime);
        var gain = new GainCalculator(3);
        delay.setInputSignal(0, source, 0);
        gain.setInputSignal(0, delay, 0);
        delay.initializeAtSimulationStart(0.1);
        var netlist = ControlNetlist.fromOrderedCalculators(List.of(source, delay, gain));
        var output = new ScopeDataExtractor(gain, new String[]{"input"});
        for (int step = 0; step < 3; step++) {
            netlist.executeTimeStep(0.1, step * 0.1);
            assertEquals(7, output.extractCurrentValues()[0], 1e-6);
            assertEquals(21, gain._outputSignal[0][0], 1e-6);
        }
    }

    @Test
    void timestepChangesCanSwitchBufferedDelaysToDirectFeedthrough() throws Exception {
        var source = new ConstantCalculator(7);
        var delay = new DelayCalculator(0.2);
        var gain = new GainCalculator(2);
        delay.setInputSignal(0, source, 0);
        gain.setInputSignal(0, delay, 0);
        delay.initializeAtSimulationStart(0.1);
        var netlist = ControlNetlist.fromOrderedCalculators(List.of(source, delay, gain));
        netlist.executeTimeStep(0.1, 0);
        netlist.executeTimeStep(0.1, 0.1);
        assertEquals(0, gain._outputSignal[0][0]);
        delay.initWithNewDt(0.25);
        netlist.executeTimeStep(0.25, 0.35);
        assertEquals(14, gain._outputSignal[0][0]);
        delay.setDelayTime(0.1);
        delay.initializeAtSimulationStart(0.1);
        netlist.executeTimeStep(0.1, 0.45);
        assertEquals(0, gain._outputSignal[0][0]);
        netlist.executeTimeStep(0.1, 0.55);
        assertEquals(14, gain._outputSignal[0][0]);
    }

    @Test
    void multipleDelaysPublishHistoryBeforeEitherCapturesAnInput() throws Exception {
        var one = new ConstantCalculator(1);
        var first = new DelayCalculator(0.1);
        var second = new DelayCalculator(0.1);
        var feedback = new GainCalculator(-0.5);
        var sum = new SubtractionTwoParameter();
        first.setInputSignal(0, sum, 0);
        second.setInputSignal(0, first, 0);
        feedback.setInputSignal(0, second, 0);
        sum.setInputSignal(0, one, 0);
        sum.setInputSignal(1, feedback, 0);
        first.initializeAtSimulationStart(0.1);
        second.initializeAtSimulationStart(0.1);
        var netlist = ControlNetlist.fromOrderedCalculators(List.of(one, second, feedback, sum, first));
        double[] expected = {1, 1, 1.5, 1.5, 1.75, 1.75};
        for (int step = 0; step < expected.length; step++) {
            netlist.executeTimeStep(0.1, step * 0.1);
            assertEquals(expected[step], sum._outputSignal[0][0], 1e-6);
        }
    }
}
