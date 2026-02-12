/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

/**
 * Represents a detected zero-crossing event where a signal crosses a threshold.
 * Used by {@link ZeroCrossingDetector} to report threshold crossings for switch events,
 * gate signals, and other discontinuities in power electronics simulation.
 */
public final class ZeroCrossingEvent {

    /**
     * Direction of the threshold crossing.
     */
    public enum Direction {
        /** Signal crossed threshold from below (rising edge) */
        RISING,
        /** Signal crossed threshold from above (falling edge) */
        FALLING
    }

    private final int signalIndex;
    private final double crossingTime;
    private final Direction direction;
    private final double valueBefore;
    private final double valueAfter;

    /**
     * Creates a new zero-crossing event.
     *
     * @param signalIndex  index of the signal that crossed the threshold
     * @param crossingTime estimated time of the crossing
     * @param direction    whether the crossing was rising or falling
     * @param valueBefore  signal value at the previous time step
     * @param valueAfter   signal value at the current time step
     */
    public ZeroCrossingEvent(int signalIndex, double crossingTime, Direction direction,
                             double valueBefore, double valueAfter) {
        this.signalIndex = signalIndex;
        this.crossingTime = crossingTime;
        this.direction = direction;
        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
    }

    public int getSignalIndex() {
        return signalIndex;
    }

    public double getCrossingTime() {
        return crossingTime;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getValueBefore() {
        return valueBefore;
    }

    public double getValueAfter() {
        return valueAfter;
    }

    @Override
    public String toString() {
        return String.format("ZeroCrossingEvent[signal=%d, t=%.6e, dir=%s, before=%.4f, after=%.4f]",
                signalIndex, crossingTime, direction, valueBefore, valueAfter);
    }
}
