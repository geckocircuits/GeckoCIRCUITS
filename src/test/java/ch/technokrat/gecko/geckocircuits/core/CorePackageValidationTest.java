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
package ch.technokrat.gecko.geckocircuits.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates that designated "core" packages remain GUI-free.
 * 
 * This test suite enforces the architectural boundary between the simulation
 * core (backend) and the desktop GUI (frontend). It scans source files in
 * specified packages and verifies they don't import Swing/AWT classes.
 * 
 * GUI-Free Core Packages (Backend API candidates):
 * - circuit.matrix: Matrix stampers for circuit simulation
 * - circuit.netlist: Netlist building and parsing
 * - circuit.simulation: Simulation engine components
 * - math: Mathematical utilities (Matrix, LU decomposition, etc.)
 * 
 * Partial GUI-Free Packages (need cleanup):
 * - control.calculators: Signal calculators (some GUI leakage)
 * - circuit.losscalculation: Thermal calculations
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15
 */
@DisplayName("Core Package GUI-Free Validation")
class CorePackageValidationTest {

    private static final String SRC_MAIN = "src/main/java/ch/technokrat/gecko/geckocircuits/";
    
    // Packages that MUST remain GUI-free (hard boundary)
    private static final String[] CORE_PACKAGES = {
        "circuit/matrix",
        "circuit/netlist",
        "circuit/simulation",
        "math"
    };
    
    // Calculators that are allowed to have GUI dependencies (explicitly excluded)
    private static final Set<String> CALCULATORS_WITH_GUI_ALLOWED = Set.of(
        "DEMUXCalculator.java",      // Depends on ReglerDemux (GUI)
        "SpaceVectorCalculator.java"  // Depends on SpaceVectorDisplay (GUI)
    );
    
    // LossCalculation classes that are GUI panels (explicitly excluded)
    private static final Set<String> LOSSCALC_GUI_PANELS = Set.of(
        "DetailedConductionLossPanel.java",
        "DetailedSwitchingLossesPanel.java",
        "DetailledLossPanel.java",
        "DialogVerlusteDetail.java",
        "JPanelLossDataInterpolationSettings.java",
        "LossCurveTemperaturePanel.java"
    );
    
    // Circuit main package GUI classes (explicitly documented)
    private static final Set<String> CIRCUIT_GUI_CLASSES = Set.of(
        "AbstractBlockInterface.java",
        "AbstractCircuitSheetComponent.java",
        "AbstractTerminal.java",
        "AwtGraphicsAdapter.java",  // Bridge to AWT Graphics (intentional AWT dependency)
        "CircuitSheet.java",
        "DataTablePanel.java",
        "DataTablePanelParameters.java",
        "DialogCircuitComponent.java",
        "DialogGlobalTerminal.java",
        "DialogModule.java",
        "DialogNonLinearity.java",
        "GeckoUndoableEditAdapter.java",  // Bridge to Swing undo manager (intentional Swing dependency)
        "IDStringDialog.java",
        "KnotenLabel.java",
        "MyTableCellEditor.java",
        "MyTableCellRenderer.java",
        "NonLinearDialogPanel.java",
        "SchematicComponentSelection2.java",
        "SchematicEditor2.java",
        "SchematicTextInfo.java",
        "TerminalControl.java",
        "TerminalControlBidirectional.java",
        "TerminalControlInput.java",
        "TerminalControlOutput.java",
        "TerminalFixedPositionInvisible.java",
        "TerminalHiddenSubcircuit.java",
        "TerminalInterface.java",
        "TerminalRelativePositionReluctance.java",
        "TerminalSubCircuitBlock.java",
        "TerminalToWrap.java",
        "TerminalTwoPortComponent.java",
        "TerminalVerbindung.java",
        "ToolBar.java",
        "Verbindung.java",
        // GUI-free as of Sprint 15:
        // ComponentPositioner.java - NOW GUI-FREE (uses GridPoint)
        // ConnectorType.java - NOW GUI-FREE (uses int RGB)
        // SubCircuitTerminable.java - NOW GUI-FREE (uses int RGB)
        // InvisibleEdit.java - NOW GUI-FREE (uses GeckoUndoableEdit)
        // CircuitLabel.java - NOW GUI-FREE (uses GeckoUndoableEdit)
        // ComponentCoupling.java - NOW GUI-FREE (uses GeckoUndoableEdit)
        // PotentialCoupling.java - NOW GUI-FREE (uses GeckoUndoableEdit)
        // WirePathCalculator.java - NOW GUI-FREE (uses GridPoint)
        // GeckoGraphics.java - NOW GUI-FREE (pure interface)
        // Drawable.java - NOW GUI-FREE (pure interface)
        "WorksheetSize.java"
    );
    
    // GUI import patterns to detect
    private static final Pattern GUI_IMPORT_PATTERN = Pattern.compile(
        "^\\s*import\\s+(java\\.awt|javax\\.swing|java\\.applet)",
        Pattern.MULTILINE
    );
    
    // Specific GUI classes (even if indirectly imported via *)
    private static final String[] GUI_CLASS_MARKERS = {
        "JFrame", "JPanel", "JButton", "JLabel", "JTextField",
        "Graphics", "Graphics2D", "Component", "Container",
        "ActionListener", "MouseListener", "KeyListener",
        "Color", "Font", "Dimension", "Point", "Rectangle",
        "BufferedImage", "ImageIcon"
    };

    @Test
    @DisplayName("circuit.matrix package has no GUI imports")
    void matrixPackageIsGuiFree() throws IOException {
        assertPackageIsGuiFree("circuit/matrix");
    }
    
    @Test
    @DisplayName("circuit.netlist package has no GUI imports")
    void netlistPackageIsGuiFree() throws IOException {
        assertPackageIsGuiFree("circuit/netlist");
    }
    
    @Test
    @DisplayName("circuit.simulation package has no GUI imports")
    void simulationPackageIsGuiFree() throws IOException {
        assertPackageIsGuiFree("circuit/simulation");
    }
    
    @Test
    @DisplayName("math package has no GUI imports")
    void mathPackageIsGuiFree() throws IOException {
        assertPackageIsGuiFree("math");
    }
    
    @Test
    @DisplayName("control.calculators package is mostly GUI-free (71/73)")
    void calculatorsPackageIsMostlyGuiFree() throws IOException {
        // Verify the calculators package except for known GUI-coupled classes
        List<String> violations = getGuiViolationsExcluding(
            "control/calculators", 
            CALCULATORS_WITH_GUI_ALLOWED
        );
        
        if (!violations.isEmpty()) {
            fail("Unexpected GUI imports in calculators package:\n" + String.join("\n", violations));
        }
    }
    
    @Test
    @DisplayName("calculators package has expected file count (73 classes)")
    void calculatorsPackageFileCount() throws IOException {
        assertPackageFileCount("control/calculators", 70, 76);  // 73 ± 3
    }
    
    @Test
    @DisplayName("circuit.losscalculation package is mostly GUI-free (18/24)")
    void losscalculationPackageIsMostlyGuiFree() throws IOException {
        // Verify the losscalculation package except for GUI panel classes
        List<String> violations = getGuiViolationsExcluding(
            "circuit/losscalculation", 
            LOSSCALC_GUI_PANELS
        );
        
        if (!violations.isEmpty()) {
            fail("Unexpected GUI imports in losscalculation package:\n" + String.join("\n", violations));
        }
    }
    
    @Test
    @DisplayName("losscalculation package has expected file count (24 classes)")
    void losscalculationPackageFileCount() throws IOException {
        assertPackageFileCount("circuit/losscalculation", 22, 26);  // 24 ± 2
    }
    
    @Test
    @DisplayName("circuit main package GUI-free classes remain GUI-free (63/96)")
    void circuitMainPackageGuiFreeClassesRemainGuiFree() throws IOException {
        // Verify the circuit package's GUI-free classes don't gain GUI imports
        // Updated Sprint 15: Added 6 GUI-free classes (ConnectorType, SubCircuitTerminable, 
        // InvisibleEdit, CircuitLabel, ComponentCoupling, PotentialCoupling)
        // Total GUI-free: 63/96 (65.6%)
        List<String> violations = getGuiViolationsExcluding(
            "circuit", 
            CIRCUIT_GUI_CLASSES
        );
        
        if (!violations.isEmpty()) {
            fail("Unexpected GUI imports in circuit package (should be GUI-free):\n" + 
                 String.join("\n", violations));
        }
    }
    
    @Test
    @DisplayName("circuit main package has expected file count (112 classes)")
    void circuitMainPackageFileCount() throws IOException {
        // Updated Phase 4 Option D: Added 9 Core classes (ICircuitCalculator, 
        // CircuitComponentCore, AbstractResistorCore, AbstractInductorCore, 
        // AbstractCapacitorCore, AbstractCurrentSourceCore, AbstractVoltageSourceCore, 
        // AbstractSwitchCore, AbstractMotorCore)
        // Previous: 99-105 files, Now: 112 files
        assertPackageFileCount("circuit", 110, 115);  // 112 ± 3
    }
    
    @Test
    @DisplayName("All core packages collectively GUI-free")
    void allCorePackagesAreGuiFree() throws IOException {
        List<String> violations = new ArrayList<>();
        
        for (String pkg : CORE_PACKAGES) {
            List<String> pkgViolations = getGuiViolations(pkg);
            violations.addAll(pkgViolations);
        }
        
        if (!violations.isEmpty()) {
            fail("GUI imports found in core packages:\n" + String.join("\n", violations));
        }
    }
    
    @Test
    @DisplayName("Core packages have reasonable file count")
    void corePackagesHaveExpectedFileCount() throws IOException {
        // Document expected sizes (catches accidental file additions/deletions)
        assertPackageFileCount("circuit/matrix", 14, 16);  // 15 ± 1
        assertPackageFileCount("circuit/netlist", 3, 5);   // 4 ± 1  
        assertPackageFileCount("circuit/simulation", 1, 3); // 2 ± 1
        assertPackageFileCount("math", 6, 8);              // 7 ± 1
    }
    
    @Test
    @DisplayName("Matrix package classes are documented")
    void matrixPackageHasDocumentation() throws IOException {
        Path packagePath = getPackagePath("circuit/matrix");
        
        try (Stream<Path> files = Files.walk(packagePath, 1)) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .toList();
            
            int documented = 0;
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                // Check for Javadoc on class
                if (content.contains("/**") && content.contains("*/")) {
                    documented++;
                }
            }
            
            double docRatio = (double) documented / javaFiles.size();
            assertTrue(docRatio >= 0.8, 
                String.format("Matrix package documentation ratio too low: %.0f%% (expected ≥80%%)", 
                    docRatio * 100));
        }
    }

    // ========== Helper Methods ==========
    
    private void assertPackageIsGuiFree(String packagePath) throws IOException {
        List<String> violations = getGuiViolations(packagePath);
        
        if (!violations.isEmpty()) {
            fail("GUI imports found in " + packagePath + ":\n" + String.join("\n", violations));
        }
    }
    
    private List<String> getGuiViolations(String packagePath) throws IOException {
        return getGuiViolationsExcluding(packagePath, Set.of());
    }
    
    private List<String> getGuiViolationsExcluding(String packagePath, Set<String> excludedFiles) throws IOException {
        List<String> violations = new ArrayList<>();
        Path pkgPath = getPackagePath(packagePath);
        
        if (!Files.exists(pkgPath)) {
            return violations; // Skip non-existent packages
        }
        
        try (Stream<Path> files = Files.walk(pkgPath, 1)) {
            List<Path> javaFiles = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .filter(p -> !excludedFiles.contains(p.getFileName().toString()))
                .toList();
            
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                
                // Check explicit GUI imports
                Matcher matcher = GUI_IMPORT_PATTERN.matcher(content);
                while (matcher.find()) {
                    violations.add(String.format("  %s: %s", 
                        file.getFileName(), matcher.group().trim()));
                }
            }
        }
        
        return violations;
    }
    
    private void assertPackageFileCount(String packagePath, int minCount, int maxCount) throws IOException {
        Path pkgPath = getPackagePath(packagePath);
        
        if (!Files.exists(pkgPath)) {
            fail("Package not found: " + packagePath);
            return;
        }
        
        try (Stream<Path> files = Files.walk(pkgPath, 1)) {
            long count = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .count();
            
            assertTrue(count >= minCount && count <= maxCount,
                String.format("Package %s has %d files (expected %d-%d)", 
                    packagePath, count, minCount, maxCount));
        }
    }
    
    private Path getPackagePath(String packagePath) {
        // Try to find project root
        Path currentDir = Path.of(System.getProperty("user.dir"));
        Path srcPath = currentDir.resolve(SRC_MAIN + packagePath);
        
        if (!Files.exists(srcPath)) {
            // Try parent directory (in case tests run from subdir)
            srcPath = currentDir.getParent().resolve(SRC_MAIN + packagePath);
        }
        
        return srcPath;
    }
}
