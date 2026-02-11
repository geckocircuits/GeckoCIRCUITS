/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.core.control.calculators;

import ch.technokrat.gecko.core.control.FrequencyDataCore;
import ch.technokrat.gecko.core.control.OutputDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SlidingDFTCalculatorTest {

    @Test
    void averageSpanSmallerThanStepWidth_doesNotCrashOnInitOrDtChange() {
        SlidingDFTCalculator calculator = new SlidingDFTCalculator(
                1, 1e-6, List.of(new FrequencyDataCore(50, OutputDataType.ABS)));
        calculator._inputSignal[0] = new double[1];

        double dt = 1e-3;
        calculator.initializeAtSimulationStart(dt);
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(dt);

        calculator.initWithNewDt(2 * dt);
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(2 * dt);

        assertFalse(Double.isNaN(calculator._outputSignal[0][0]));
        assertFalse(Double.isInfinite(calculator._outputSignal[0][0]));
    }
}
