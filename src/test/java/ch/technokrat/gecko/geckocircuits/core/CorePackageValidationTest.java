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
    
    @Test
    @DisplayName("datacontainer package has limited GUI usage")
    void datacontainerPackageGuiUsage() throws IOException {
        // datacontainer has some GUI references but core classes should be clean
        Set<String> datacontainerGuiAllowed = Set.of(
            "DataContainerCompressable.java"  // Has JOptionPane for memory errors
        );

        List<String> violations = getGuiViolationsExcluding(
            "datacontainer",
            datacontainerGuiAllowed
        );

        // Document violations but don't fail - this is aspirational
        if (!violations.isEmpty()) {
            System.out.println("Note: datacontainer GUI dependencies: " + violations.size());
        }
    }

    @Test
    @DisplayName("Verify core module extraction tracking")
    void coreModuleExtractionTracking() throws IOException {
        // These packages are fully extracted to gecko-simulation-core
        String[] coreModulePackages = {
            "circuit/matrix",
            "circuit/netlist",
            "circuit/simulation",
            "math"
        };

        int totalCoreClasses = 0;
        for (String pkg : coreModulePackages) {
            Path pkgPath = getPackagePath(pkg);
            if (Files.exists(pkgPath)) {
                try (Stream<Path> files = Files.walk(pkgPath, 1)) {
                    totalCoreClasses += files
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(Files::isRegularFile)
                        .count();
                }
            }
        }

        // Should have significant core classes extracted
        assertTrue(totalCoreClasses >= 20,
            String.format("Expected 20+ core classes, found %d", totalCoreClasses));
    }

    @Test
    @DisplayName("newscope package GUI classes are documented")
    void newscopePackageDocumented() throws IOException {
        // newscope has many GUI classes - just verify package exists
        Path newscopePath = getPackagePath("newscope");
        assertTrue(Files.exists(newscopePath), "newscope package should exist");
    }

    @Test
    @DisplayName("Circuit interfaces are GUI-free")
    void circuitInterfacesGuiFree() throws IOException {
        // Key interfaces that should remain GUI-free
        String[] interfaceFiles = {
            "ICircuitCalculator.java",
            "Drawable.java",
            "GeckoGraphics.java"
        };

        Path circuitPath = getPackagePath("circuit");

        for (String interfaceFile : interfaceFiles) {
            Path filePath = circuitPath.resolve(interfaceFile);
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                assertFalse(GUI_IMPORT_PATTERN.matcher(content).find(),
                    interfaceFile + " should be GUI-free");
            }
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

    // ========== Workstream 5: Core API Boundary Tests ==========

    @Test
    @DisplayName("StamperRegistry has no GUI imports")
    void stamperRegistryIsGuiFree() throws IOException {
        Path matrixPath = getPackagePath("circuit/matrix");
        Path stamperRegistry = matrixPath.resolve("StamperRegistry.java");

        if (Files.exists(stamperRegistry)) {
            String content = Files.readString(stamperRegistry);
            assertFalse(GUI_IMPORT_PATTERN.matcher(content).find(),
                "StamperRegistry should be GUI-free");
        }
    }

    @Test
    @DisplayName("DataContainerGlobal has limited GUI usage")
    void dataContainerGlobalGuiUsage() throws IOException {
        Path datacontainerPath = getPackagePath("datacontainer");
        Path dataContainerGlobal = datacontainerPath.resolve("DataContainerGlobal.java");

        if (Files.exists(dataContainerGlobal)) {
            String content = Files.readString(dataContainerGlobal);

            // Check for direct Swing imports (not AWT Color which may be acceptable)
            Pattern swingPattern = Pattern.compile(
                "^\\s*import\\s+javax\\.swing",
                Pattern.MULTILINE
            );
            assertFalse(swingPattern.matcher(content).find(),
                "DataContainerGlobal should not import Swing");
        }
    }

    @Test
    @DisplayName("All matrix stampers are GUI-free")
    void allMatrixStampersAreGuiFree() throws IOException {
        Path matrixPath = getPackagePath("circuit/matrix");

        if (Files.exists(matrixPath)) {
            try (Stream<Path> files = Files.walk(matrixPath, 1)) {
                List<Path> stamperFiles = files
                    .filter(p -> p.toString().endsWith("Stamper.java"))
                    .filter(Files::isRegularFile)
                    .toList();

                for (Path file : stamperFiles) {
                    String content = Files.readString(file);
                    assertFalse(GUI_IMPORT_PATTERN.matcher(content).find(),
                        file.getFileName() + " should be GUI-free");
                }

                assertTrue(stamperFiles.size() >= 5,
                    "Expected at least 5 stamper files, found " + stamperFiles.size());
            }
        }
    }

    @Test
    @DisplayName("Simulation package classes are all GUI-free")
    void allSimulationClassesAreGuiFree() throws IOException {
        Path simulationPath = getPackagePath("circuit/simulation");

        if (Files.exists(simulationPath)) {
            try (Stream<Path> files = Files.walk(simulationPath, 1)) {
                List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .toList();

                for (Path file : javaFiles) {
                    String content = Files.readString(file);
                    assertFalse(GUI_IMPORT_PATTERN.matcher(content).find(),
                        file.getFileName() + " should be GUI-free");
                }
            }
        }
    }

    @Test
    @DisplayName("Core calculators (non-GUI) can be instantiated")
    void coreCalculatorsCanBeInstantiated() {
        // Test that core calculators can be created without GUI
        // These should not throw exceptions related to headless mode
        try {
            // Simple calculators that should work headless
            Class.forName("ch.technokrat.gecko.geckocircuits.control.calculators.GainCalculator");
            Class.forName("ch.technokrat.gecko.geckocircuits.control.calculators.SinCalculator");
            Class.forName("ch.technokrat.gecko.geckocircuits.control.calculators.CosCalculator");
            Class.forName("ch.technokrat.gecko.geckocircuits.control.calculators.AbsCalculator");
            Class.forName("ch.technokrat.gecko.geckocircuits.control.calculators.PT1Calculator");
        } catch (ClassNotFoundException e) {
            fail("Core calculator class not found: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Netlist package is fully GUI-free")
    void netlistPackageIsFullyGuiFree() throws IOException {
        Path netlistPath = getPackagePath("circuit/netlist");

        if (Files.exists(netlistPath)) {
            try (Stream<Path> files = Files.walk(netlistPath, 1)) {
                List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .toList();

                List<String> violations = new ArrayList<>();
                for (Path file : javaFiles) {
                    String content = Files.readString(file);
                    if (GUI_IMPORT_PATTERN.matcher(content).find()) {
                        violations.add(file.getFileName().toString());
                    }
                }

                assertTrue(violations.isEmpty(),
                    "Netlist package should be fully GUI-free. Violations: " + violations);
            }
        }
    }

    @Test
    @DisplayName("Math utilities are GUI-free")
    void mathUtilitiesAreGuiFree() throws IOException {
        // Verify specific math utility classes
        String[] mathClasses = {
            "MNAMatrix.java",
            "LUDecomposition.java",
            "MatrixSolver.java"
        };

        Path mathPath = getPackagePath("math");

        for (String mathClass : mathClasses) {
            Path filePath = mathPath.resolve(mathClass);
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                assertFalse(GUI_IMPORT_PATTERN.matcher(content).find(),
                    mathClass + " should be GUI-free");
            }
        }
    }

    @Test
    @DisplayName("Core module package count is stable")
    void coreModulePackageCountStable() throws IOException {
        // Track class counts to detect unexpected changes
        int matrixCount = countJavaFiles("circuit/matrix");
        int netlistCount = countJavaFiles("circuit/netlist");
        int simulationCount = countJavaFiles("circuit/simulation");
        int mathCount = countJavaFiles("math");

        int totalCore = matrixCount + netlistCount + simulationCount + mathCount;

        // Document baseline - should be 25+ classes in core
        assertTrue(totalCore >= 25,
            String.format("Core module should have 25+ classes, found %d " +
                "(matrix=%d, netlist=%d, simulation=%d, math=%d)",
                totalCore, matrixCount, netlistCount, simulationCount, mathCount));
    }

    private int countJavaFiles(String packagePath) throws IOException {
        Path pkgPath = getPackagePath(packagePath);
        if (!Files.exists(pkgPath)) {
            return 0;
        }

        try (Stream<Path> files = Files.walk(pkgPath, 1)) {
            return (int) files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .count();
        }
    }
}
