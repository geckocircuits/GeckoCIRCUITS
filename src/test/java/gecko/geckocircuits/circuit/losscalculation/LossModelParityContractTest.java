package gecko.geckocircuits.circuit.losscalculation;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.circuit.losscalculation.AbstractLossCalculator;
import gecko.core.circuit.losscalculation.LossCalculationSplittable;
import gecko.core.circuit.losscalculation.SemiconductorLossModel;
import gecko.core.simulation.ThermalLossSources;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.SemiconductorLossCalculatable;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The core loss model equals the legacy loss calculators bit for bit. The saved circuit is the NPC converter, whose
 * SIMPLE and DETAILED models come from its embedded loss files.
 */
@Tag("regression")
class LossModelParityContractTest {
    private static final double DT = 2e-6;
    // Current, voltage and temperature of each step. They contain turn-on and turn-off edges, sub-threshold currents,
    // a negative current, the first-step history of -1 and temperatures outside the curves.
    private static final double[] CURRENT = {0, 0, 5, 12, 12, 3, 0.005, 0, 40, -7, 20, 0.02, 0, 18};
    private static final double[] VOLTAGE = {300, 310, 40, 5, 6, 200, 350, 360, 20, 150, 10, 400, 380, 12};
    private static final double[] TEMPERATURE = {25, 30, 60, 90, 120, 125, 70, 50, 1.0E5, Double.NaN, 100, 15, 150, 80};

    @Test
    void everyResolvableSimpleAndDetailedLossModelMatchesTheLegacyCalculatorBitForBit() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[0]);
        try {
            int[] npc = compare("resources/articles/2011_10_NPC_optimization/NPCmodel.ipes");
            int[] buck = compare("src/test/resources/ipes/thermal/BuckBoost_thermal.ipes");
            System.out.println("LOSS_PARITY: npc simple=" + npc[0] + " detailed=" + npc[1] + ", buck simple=" + buck[0] + " detailed=" + buck[1]);
            assertTrue(buck[0] >= 2, "the BuckBoost has an IGBT and a diode with SIMPLE loss models");
            assertTrue(npc[1] >= 2, "the NPC converter has DETAILED loss models with embedded loss files");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (var window : java.awt.Window.getWindows()) window.dispose(); });
        }
    }

    /** @return the numbers of SIMPLE and DETAILED loss sources that were compared */
    private static int[] compare(String file) throws Exception {
        {
            String path = Path.of(file).toAbsolutePath().toString();
            GeckoExternal.openFile(path);
            var scripts = MainWindow._scripter;
            var model = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                    MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                    new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                            scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), path).toModel();
            int simple = 0, detailed = 0;
            for (var lossSource : model.getThermalComponents()) {
                if (lossSource.getType() != 41) continue;
                var resolution = ThermalLossSources.resolve(model, lossSource);
                if (resolution.spec() == null) continue;
                String coupledName = resolution.spec().coupled().getName();
                var legacyComponent = MainWindow._se._circuitSheet.getAllElements().stream()
                        .filter(element -> element instanceof AbstractCircuitBlockInterface block && coupledName.equals(block.getStringID()))
                        .map(element -> (AbstractCircuitBlockInterface) element).findFirst().orElseThrow();
                AbstractLossCalculator legacy = ((SemiconductorLossCalculatable) legacyComponent).getVerlustBerechnung().lossCalculatorFabric();
                SemiconductorLossModel core = resolution.spec().newModel().get();
                String type = resolution.spec().coupled().getSavedBlock().sections().get("Verluste").contains("verlustTyp 2") ? "DETAILED" : "SIMPLE";
                if (type.equals("DETAILED")) detailed++; else simple++;
                for (int step = 0; step < CURRENT.length; step++) {
                    legacyComponent._voltage = VOLTAGE[step];
                    legacy.calcLosses(CURRENT[step], TEMPERATURE[step], DT);
                    core.update(CURRENT[step], VOLTAGE[step], TEMPERATURE[step], DT);
                    String where = type + " " + coupledName + " step " + step;
                    assertEquals(Double.doubleToLongBits(legacy.getTotalLosses()), Double.doubleToLongBits(core.total()), where + " total");
                    if (legacy instanceof LossCalculationSplittable split) {
                        assertEquals(Double.doubleToLongBits(split.getConductionLoss()), Double.doubleToLongBits(core.conduction()), where + " conduction");
                        assertEquals(Double.doubleToLongBits(split.getSwitchingLoss()), Double.doubleToLongBits(core.switching()), where + " switching");
                    }
                }
            }
            return new int[]{simple, detailed};
        }
    }
}
