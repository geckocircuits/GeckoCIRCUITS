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
package ch.technokrat.gecko.core.circuit.netlist;

import java.util.*;

/**
 * Registry for managing mutual inductance couplings between inductors.
 * Extracted from NetListLK.definiere_magnetischeKopplungen_im_LK for better testability.
 *
 * <p>Mutual inductance in circuit simulation:
 * <ul>
 *   <li>Two coupled inductors share magnetic flux</li>
 *   <li>Coupling coefficient k ranges from 0 (no coupling) to 1 (perfect coupling)</li>
 *   <li>Mutual inductance M = k * sqrt(L1 * L2)</li>
 *   <li>Affects the matrix stamping for inductor currents</li>
 * </ul>
 *
 * <p>Registry responsibilities:
 * <ul>
 *   <li>Track which inductors are coupled together</li>
 *   <li>Store coupling coefficients</li>
 *   <li>Calculate mutual inductance values</li>
 *   <li>Provide coupling partners for each inductor</li>
 * </ul>
 *
 * @author Extracted from NetListLK
 * @since Sprint 2 - Circuit Refactoring
 */
public class MutualCouplingRegistry {

    /**
     * Represents a coupling between two inductors.
     */
    public static class Coupling {
        private final int inductor1Index;
        private final int inductor2Index;
        private final double couplingCoefficient;
        private final double inductance1;
        private final double inductance2;
        private final double mutualInductance;

        /**
         * Creates a coupling between two inductors.
         *
         * @param inductor1Index index of first inductor
         * @param inductor2Index index of second inductor
         * @param couplingCoefficient k value (0 to 1)
         * @param inductance1 L1 value in Henries
         * @param inductance2 L2 value in Henries
         */
        public Coupling(int inductor1Index, int inductor2Index,
                       double couplingCoefficient,
                       double inductance1, double inductance2) {
            this.inductor1Index = inductor1Index;
            this.inductor2Index = inductor2Index;
            this.couplingCoefficient = couplingCoefficient;
            this.inductance1 = inductance1;
            this.inductance2 = inductance2;
            this.mutualInductance = couplingCoefficient * Math.sqrt(inductance1 * inductance2);
        }

        public int getInductor1Index() { return inductor1Index; }
        public int getInductor2Index() { return inductor2Index; }
        public double getCouplingCoefficient() { return couplingCoefficient; }
        public double getInductance1() { return inductance1; }
        public double getInductance2() { return inductance2; }
        public double getMutualInductance() { return mutualInductance; }

        /**
         * Checks if this coupling involves a specific inductor.
         *
         * @param inductorIndex the inductor to check
         * @return true if the inductor is part of this coupling
         */
        public boolean involves(int inductorIndex) {
            return inductor1Index == inductorIndex || inductor2Index == inductorIndex;
        }

        /**
         * Gets the partner inductor for a given inductor.
         *
         * @param inductorIndex the inductor to get partner for
         * @return partner index, or -1 if not part of this coupling
         */
        public int getPartner(int inductorIndex) {
            if (inductor1Index == inductorIndex) return inductor2Index;
            if (inductor2Index == inductorIndex) return inductor1Index;
            return -1;
        }

        @Override
        public String toString() {
            return String.format("Coupling[L%d-L%d, k=%.4f, M=%.6e]",
                inductor1Index, inductor2Index, couplingCoefficient, mutualInductance);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Coupling)) return false;
            Coupling other = (Coupling) obj;
            // Couplings are equal if they connect same inductors (order doesn't matter)
            return (inductor1Index == other.inductor1Index && inductor2Index == other.inductor2Index) ||
                   (inductor1Index == other.inductor2Index && inductor2Index == other.inductor1Index);
        }

        @Override
        public int hashCode() {
            // Order-independent hash
            return Integer.hashCode(Math.min(inductor1Index, inductor2Index)) * 31 +
                   Integer.hashCode(Math.max(inductor1Index, inductor2Index));
        }
    }

    /** All registered couplings */
    private final List<Coupling> couplings;

    /** Map from inductor index to its couplings */
    private final Map<Integer, List<Coupling>> inductorToCouplings;

    /** Set of all coupled inductor indices */
    private final Set<Integer> coupledInductors;

    /**
     * Creates an empty coupling registry.
     */
    public MutualCouplingRegistry() {
        this.couplings = new ArrayList<>();
        this.inductorToCouplings = new HashMap<>();
        this.coupledInductors = new HashSet<>();
    }

    /**
     * Registers a coupling between two inductors.
     *
     * @param inductor1Index index of first inductor in netlist
     * @param inductor2Index index of second inductor in netlist
     * @param couplingCoefficient k value (typically 0 to 1)
     * @param inductance1 L1 value in Henries
     * @param inductance2 L2 value in Henries
     * @return the created Coupling object
     * @throws IllegalArgumentException if indices are same or coefficient is invalid
     */
    public Coupling registerCoupling(int inductor1Index, int inductor2Index,
                                     double couplingCoefficient,
                                     double inductance1, double inductance2) {
        if (inductor1Index == inductor2Index) {
            throw new IllegalArgumentException("Cannot couple an inductor with itself");
        }
        if (inductor1Index < 0 || inductor2Index < 0) {
            throw new IllegalArgumentException("Inductor indices must be non-negative");
        }
        if (Double.isNaN(couplingCoefficient) || Double.isInfinite(couplingCoefficient)) {
            throw new IllegalArgumentException("Invalid coupling coefficient: " + couplingCoefficient);
        }
        if (inductance1 <= 0 || inductance2 <= 0) {
            throw new IllegalArgumentException("Inductance values must be positive");
        }

        Coupling coupling = new Coupling(inductor1Index, inductor2Index,
                                         couplingCoefficient, inductance1, inductance2);

        // Check for duplicate coupling
        if (couplings.contains(coupling)) {
            throw new IllegalArgumentException("Coupling between inductors " +
                inductor1Index + " and " + inductor2Index + " already exists");
        }

        couplings.add(coupling);

        // Update index mappings
        inductorToCouplings.computeIfAbsent(inductor1Index, k -> new ArrayList<>()).add(coupling);
        inductorToCouplings.computeIfAbsent(inductor2Index, k -> new ArrayList<>()).add(coupling);

        coupledInductors.add(inductor1Index);
        coupledInductors.add(inductor2Index);

        return coupling;
    }

    /**
     * Gets all couplings involving a specific inductor.
     *
     * @param inductorIndex the inductor index
     * @return list of couplings (empty if none)
     */
    public List<Coupling> getCouplingsFor(int inductorIndex) {
        List<Coupling> result = inductorToCouplings.get(inductorIndex);
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    /**
     * Gets all coupling partners for an inductor.
     *
     * @param inductorIndex the inductor index
     * @return list of partner inductor indices
     */
    public List<Integer> getCouplingPartners(int inductorIndex) {
        List<Integer> partners = new ArrayList<>();
        List<Coupling> inductorCouplings = inductorToCouplings.get(inductorIndex);
        if (inductorCouplings != null) {
            for (Coupling c : inductorCouplings) {
                partners.add(c.getPartner(inductorIndex));
            }
        }
        return partners;
    }

    /**
     * Gets the mutual inductance between two inductors.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return mutual inductance M, or 0 if not coupled
     */
    public double getMutualInductance(int inductor1Index, int inductor2Index) {
        List<Coupling> inductorCouplings = inductorToCouplings.get(inductor1Index);
        if (inductorCouplings != null) {
            for (Coupling c : inductorCouplings) {
                if (c.involves(inductor2Index)) {
                    return c.getMutualInductance();
                }
            }
        }
        return 0.0;
    }

    /**
     * Gets the coupling coefficient between two inductors.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return coupling coefficient k, or 0 if not coupled
     */
    public double getCouplingCoefficient(int inductor1Index, int inductor2Index) {
        List<Coupling> inductorCouplings = inductorToCouplings.get(inductor1Index);
        if (inductorCouplings != null) {
            for (Coupling c : inductorCouplings) {
                if (c.involves(inductor2Index)) {
                    return c.getCouplingCoefficient();
                }
            }
        }
        return 0.0;
    }

    /**
     * Checks if two inductors are coupled.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return true if coupled
     */
    public boolean areCoupled(int inductor1Index, int inductor2Index) {
        List<Coupling> inductorCouplings = inductorToCouplings.get(inductor1Index);
        if (inductorCouplings != null) {
            for (Coupling c : inductorCouplings) {
                if (c.involves(inductor2Index)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if an inductor is coupled to any other inductor.
     *
     * @param inductorIndex the inductor to check
     * @return true if has at least one coupling
     */
    public boolean isCoupled(int inductorIndex) {
        return coupledInductors.contains(inductorIndex);
    }

    /**
     * Gets all coupled inductor indices.
     *
     * @return unmodifiable set of inductor indices that have couplings
     */
    public Set<Integer> getAllCoupledInductors() {
        return Collections.unmodifiableSet(coupledInductors);
    }

    /**
     * Gets all couplings.
     *
     * @return unmodifiable list of all couplings
     */
    public List<Coupling> getAllCouplings() {
        return Collections.unmodifiableList(couplings);
    }

    /**
     * Gets the total number of couplings.
     *
     * @return coupling count
     */
    public int getCouplingCount() {
        return couplings.size();
    }

    /**
     * Removes a coupling between two inductors.
     *
     * @param inductor1Index first inductor
     * @param inductor2Index second inductor
     * @return true if coupling was removed
     */
    public boolean removeCoupling(int inductor1Index, int inductor2Index) {
        Coupling toRemove = null;
        for (Coupling c : couplings) {
            if (c.involves(inductor1Index) && c.involves(inductor2Index)) {
                toRemove = c;
                break;
            }
        }

        if (toRemove != null) {
            couplings.remove(toRemove);

            // Update inductor mappings
            List<Coupling> list1 = inductorToCouplings.get(inductor1Index);
            if (list1 != null) {
                list1.remove(toRemove);
                if (list1.isEmpty()) {
                    inductorToCouplings.remove(inductor1Index);
                    coupledInductors.remove(inductor1Index);
                }
            }

            List<Coupling> list2 = inductorToCouplings.get(inductor2Index);
            if (list2 != null) {
                list2.remove(toRemove);
                if (list2.isEmpty()) {
                    inductorToCouplings.remove(inductor2Index);
                    coupledInductors.remove(inductor2Index);
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Clears all couplings.
     */
    public void clear() {
        couplings.clear();
        inductorToCouplings.clear();
        coupledInductors.clear();
    }

    /**
     * Builds coupling arrays for LKMatrices compatibility.
     * Returns arrays in format: [spgQuelleNr[], kWerte[]] for each coupled inductor.
     *
     * @param voltageSourceNumbers map from inductor index to voltage source number
     * @return triple: [zuLKOP2gehoerigeM_spgQnr, zuLKOP2gehoerigeM_kWerte]
     */
    public double[][][] buildCouplingArrays(int[] voltageSourceNumbers) {
        int maxIndex = 0;
        for (int idx : coupledInductors) {
            maxIndex = Math.max(maxIndex, idx);
        }

        double[][] spgQnr = new double[maxIndex + 1][];
        double[][] kWerte = new double[maxIndex + 1][];

        for (int inductorIdx : coupledInductors) {
            List<Coupling> inductorCouplings = inductorToCouplings.get(inductorIdx);
            if (inductorCouplings != null && !inductorCouplings.isEmpty()) {
                double[] partnerSpgQnr = new double[inductorCouplings.size()];
                double[] partnerKWerte = new double[inductorCouplings.size()];

                for (int i = 0; i < inductorCouplings.size(); i++) {
                    Coupling c = inductorCouplings.get(i);
                    int partnerIdx = c.getPartner(inductorIdx);
                    partnerSpgQnr[i] = voltageSourceNumbers[partnerIdx];
                    partnerKWerte[i] = c.getMutualInductance();
                }

                spgQnr[inductorIdx] = partnerSpgQnr;
                kWerte[inductorIdx] = partnerKWerte;
            }
        }

        return new double[][][] { spgQnr, kWerte };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MutualCouplingRegistry[count=").append(couplings.size()).append("]\n");
        for (Coupling c : couplings) {
            sb.append("  ").append(c).append("\n");
        }
        return sb.toString();
    }
}
