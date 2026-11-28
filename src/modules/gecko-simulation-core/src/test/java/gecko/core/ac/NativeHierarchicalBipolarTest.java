package gecko.core.ac;

import gecko.core.io.ArtifactDigest;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.LtspiceImporter;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class NativeHierarchicalBipolarTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory;

    @Test
    void bipolarChildCurrentsFollowThePhysicalBoundaryAfterReopening() throws Exception {
        Files.writeString(directory.resolve("root.asc"), """
                Version 4
                SHEET 1 880 680
                FLAG 0 16 collector
                FLAG 0 96 0
                FLAG 128 16 base
                FLAG 128 96 0
                FLAG 256 160 collector
                FLAG 256 208 base
                FLAG 256 256 0
                SYMBOL voltage 0 0 R0
                SYMATTR InstName Vc
                SYMATTR Value 5
                SYMBOL voltage 128 0 R0
                SYMATTR InstName Vb
                SYMATTR Value 0.65 AC 1
                SYMBOL stage 256 160 R0
                SYMATTR InstName X1
                TEXT 0 304 Left 2 !.ac dec 2 10 100k
                """);
        Files.writeString(directory.resolve("stage.asy"), """
                Version 4
                SymbolType BLOCK
                PIN 0 0 NONE 8
                PINATTR PinName C
                PINATTR SpiceOrder 1
                PIN 0 48 NONE 8
                PINATTR PinName B
                PINATTR SpiceOrder 2
                PIN 0 96 NONE 8
                PINATTR PinName E
                PINATTR SpiceOrder 3
                """);
        Files.writeString(directory.resolve("stage.asc"), """
                Version 4
                SHEET 1 880 680
                FLAG 64 0 C
                FLAG 0 48 B
                FLAG 64 96 E
                SYMBOL npn 0 0 R0
                SYMATTR InstName Q1
                SYMATTR Value local
                SYMATTR Value2 area=3
                """);
        Files.writeString(directory.resolve("npn.asy"), """
                Version 4
                SymbolType CELL
                SYMATTR Prefix QN
                SYMATTR Value NPN
                PIN 64 0 NONE 0
                PINATTR PinName C
                PINATTR SpiceOrder 1
                PIN 0 48 NONE 0
                PINATTR PinName B
                PINATTR SpiceOrder 2
                PIN 64 96 NONE 0
                PINATTR PinName E
                PINATTR SpiceOrder 3
                """);
        try (var symbol = getClass().getResourceAsStream("/ltspice-hierarchy/voltage.asy")) {
            Files.copy(java.util.Objects.requireNonNull(symbol), directory.resolve("voltage.asy"));
        }
        var cards = Files.writeString(directory.resolve("local.models"), ".model local NPN(Is=1e-14 Bf=100)\n");
        var imported = new LtspiceImporter(false, cards).convert(directory.resolve("root.asc"), directory, java.util.List.of(directory));
        assertTrue(imported.getImportIssues().isEmpty(), imported.getImportIssues().toString());
        var model = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(imported)), "hierarchy.ipes");
        assertEquals(ArtifactDigest.sha256(Files.readAllBytes(cards)), model.getAcProvenance().dependencySha256()
                .get("semiconductor-model-cards"));
        var law = model.getCircuitComponents().stream().filter(c -> "Q1".equals(c.getName())).findFirst().orElseThrow()
                .getAcComponent().law();
        assertEquals("3", assertInstanceOf(AcComponent.Bipolar.class, law).area());

        var response = new AcExecutionPlan(model).execute();
        var ports = response.hierarchy().ports().stream().collect(Collectors.toMap(trace -> trace.internal().instance(),
                AcResult.PortTrace::currentIntoChildA));
        var supply = response.currents().stream().filter(trace -> trace.instance().equals("Vc")).findFirst().orElseThrow();
        assertEquals(java.util.Set.of("C", "B", "E"), ports.keySet());
        for (int i = 0; i < response.frequencyHz().size(); i++) {
            assertTrue(ports.get("C").get(i).abs() > 1e-3);
            assertEquals(0, ports.get("C").get(i).add(supply.currentA().get(i)).abs(), 1e-12);
            assertEquals(0, ports.get("C").get(i).add(ports.get("B").get(i)).add(ports.get("E").get(i)).abs(), 1e-12);
        }
    }

    @Test
    void modelFilesCannotIntroduceSpiceDirectives() throws Exception {
        var cards = Files.writeString(directory.resolve("bad.models"), ".include hidden.lib\n");
        assertThrows(IllegalArgumentException.class, () -> new LtspiceImporter(false, cards));
    }
}
