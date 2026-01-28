/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.core.circuit.circuitcomponents;

import java.util.HashMap;
import java.util.Map;

/**
 * Core circuit component types for headless simulation.
 *
 * This enum provides a GUI-free version of CircuitTyp, containing only the
 * component type identifiers and their integer values. Unlike the full
 * CircuitTyp enum in the main project, this version has no dependencies on
 * GUI-related TYPE_INFO objects.
 *
 * The integer values match those used in CircuitTyp for compatibility when
 * loading circuit files and communicating between core and GUI modules.
 *
 * Component categories:
 * - Electrical: LK_R, LK_L, LK_C, LK_U, LK_I (passive/sources)
 * - Semiconductor: LK_D, LK_S, LK_THYR, LK_IGBT, LK_MOSFET, LK_BJT (switches)
 * - Magnetic: LK_M, LK_LKOP2, REL_* (inductors/reluctance)
 * - Motors: LK_MOTOR_* (DC, PMSM, IM, SM variants)
 * - Thermal: TH_* (thermal simulation components)
 * - Special: LK_TERMINAL, LK_GLOBAL_TERMINAL (subcircuit terminals)
 *
 * @author GeckoCIRCUITS Team
 */
public enum CircuitTypCore {

    // Basic electrical components
    LK_R(1),           // Resistor
    LK_L(2),           // Inductor (without coupling)
    LK_C(3),           // Capacitor
    LK_U(4),           // Voltage source
    LK_I(5),           // Current source

    // Semiconductor switches
    LK_D(6),           // Diode
    LK_S(7),           // Ideal switch
    LK_THYR(8),        // Thyristor (SCR)
    LK_M(9),           // Mutual inductance
    LK_IGBT(10),       // IGBT
    LK_LKOP2(12),      // Inductor (coupable, matrix extension for stability)
    LK_LISN(13),       // LISN (Line Impedance Stabilization Network)

    // Motor components
    LK_MOTOR(14),      // DC Motor
    LK_MOTOR_PMSM(15), // Permanent Magnet Synchronous Motor
    LK_MOTOR_SMSALIENT(16), // Synchronous Motor (salient pole)
    LK_MOTOR_SMROUND(17),   // Synchronous Motor (round rotor)
    LK_MOTOR_IMA(18),  // Induction Motor (cage type A)
    LK_MOTOR_IMC(20),  // Induction Motor (generic)
    LK_MOTOR_IMSAT(21),// Induction Motor (with saturation)

    // Operational amplifier
    LK_OPV1(22),       // Operational Amplifier

    // Transformer
    LK_TRANS(23),      // Ideal Transformer

    // Reluctance network components
    REL_RELUCTANCE(24),    // Reluctance
    REL_INDUCTOR(25),      // Reluctance network inductor
    REL_MMF(26),           // Magnetomotive force source

    // MOSFET and terminals
    LK_MOSFET(28),     // MOSFET
    LK_TERMINAL(29),   // Subcircuit terminal
    REL_TERMINAL(30),  // Reluctance terminal
    LK_GLOBAL_TERMINAL(31),    // Global terminal
    REL_GLOBAL_TERMINAL(32),   // Reluctance global terminal

    // BJT
    LK_BJT(33),        // Bipolar Junction Transistor

    // Thermal components
    TH_PvCHIP(41),     // Thermal: Pv chip (power loss)
    TH_MODUL(42),      // Thermal: Module
    TH_FLOW(44),       // Thermal: Heat flow (current source)
    TH_TEMP(45),       // Thermal: Temperature (voltage source)
    TH_RTH(46),        // Thermal: Thermal resistance
    TH_CTH(47),        // Thermal: Thermal capacitance
    TH_AMBIENT(48),    // Thermal: Ambient temperature
    TH_TERMINAL(49),   // Thermal: Terminal
    TH_GLOBAL_TERMINAL(50),  // Thermal: Global terminal

    // Additional motor types
    LK_MOTOR_PERM(51), // Permanent magnet motor

    // Non-linear reluctance
    NONLIN_REL(52);    // Non-linear reluctance

    private final int typeNumber;

    private static Map<Integer, CircuitTypCore> lookupMap;

    CircuitTypCore(int typeNumber) {
        this.typeNumber = typeNumber;
    }

    /**
     * Gets the integer type number for this component type.
     * These values match the original CircuitTyp enum for compatibility.
     *
     * @return the type number identifier
     */
    public int getTypeNumber() {
        return typeNumber;
    }

    /**
     * Gets the CircuitTypCore enum value from an integer type number.
     *
     * @param typeNumber the integer type identifier
     * @return the corresponding enum value
     * @throws IllegalArgumentException if no type exists for the given number
     */
    public static CircuitTypCore fromTypeNumber(int typeNumber) {
        if (lookupMap == null) {
            lookupMap = new HashMap<>();
            for (CircuitTypCore type : values()) {
                lookupMap.put(type.typeNumber, type);
            }
        }

        CircuitTypCore result = lookupMap.get(typeNumber);
        if (result == null) {
            throw new IllegalArgumentException("Unknown circuit type number: " + typeNumber);
        }
        return result;
    }

    /**
     * Checks if a type number is a valid CircuitTypCore value.
     *
     * @param typeNumber the integer type identifier
     * @return true if the type number corresponds to a known component type
     */
    public static boolean isValidTypeNumber(int typeNumber) {
        if (lookupMap == null) {
            fromTypeNumber(1); // Initialize the map
        }
        return lookupMap.containsKey(typeNumber);
    }

    /**
     * Checks if this type is a basic passive component (R, L, C).
     *
     * @return true if resistor, inductor, or capacitor
     */
    public boolean isPassive() {
        return this == LK_R || this == LK_L || this == LK_C || this == LK_LKOP2;
    }

    /**
     * Checks if this type is a source component (voltage or current).
     *
     * @return true if voltage or current source
     */
    public boolean isSource() {
        return this == LK_U || this == LK_I;
    }

    /**
     * Checks if this type is a switching semiconductor.
     *
     * @return true if diode, switch, thyristor, IGBT, MOSFET, or BJT
     */
    public boolean isSemiconductor() {
        return this == LK_D || this == LK_S || this == LK_THYR ||
               this == LK_IGBT || this == LK_MOSFET || this == LK_BJT;
    }

    /**
     * Checks if this type is a motor component.
     *
     * @return true if any motor type
     */
    public boolean isMotor() {
        return this == LK_MOTOR || this == LK_MOTOR_PMSM ||
               this == LK_MOTOR_SMSALIENT || this == LK_MOTOR_SMROUND ||
               this == LK_MOTOR_IMA || this == LK_MOTOR_IMC ||
               this == LK_MOTOR_IMSAT || this == LK_MOTOR_PERM;
    }

    /**
     * Checks if this type is a thermal component.
     *
     * @return true if any thermal type (TH_*)
     */
    public boolean isThermal() {
        return this == TH_PvCHIP || this == TH_MODUL || this == TH_FLOW ||
               this == TH_TEMP || this == TH_RTH || this == TH_CTH ||
               this == TH_AMBIENT || this == TH_TERMINAL || this == TH_GLOBAL_TERMINAL;
    }

    /**
     * Checks if this type is a reluctance network component.
     *
     * @return true if any reluctance type (REL_*)
     */
    public boolean isReluctance() {
        return this == REL_RELUCTANCE || this == REL_INDUCTOR ||
               this == REL_MMF || this == REL_TERMINAL || this == REL_GLOBAL_TERMINAL ||
               this == NONLIN_REL;
    }

    /**
     * Checks if this type is a terminal (local or global).
     *
     * @return true if any terminal type
     */
    public boolean isTerminal() {
        return this == LK_TERMINAL || this == LK_GLOBAL_TERMINAL ||
               this == REL_TERMINAL || this == REL_GLOBAL_TERMINAL ||
               this == TH_TERMINAL || this == TH_GLOBAL_TERMINAL;
    }

    /**
     * Checks if this component type requires voltage source treatment in MNA.
     * Components that add rows to the matrix for current variables.
     *
     * @return true if this type requires voltage source handling
     */
    public boolean requiresVoltageSourceHandling() {
        return this == LK_U || this == LK_LKOP2 || this == LK_TRANS ||
               this == REL_MMF || this == TH_TEMP;
    }
}
