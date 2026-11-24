package gecko.core.ac;

import gecko.core.io.ArtifactDigest;
import gecko.core.io.BuildIdentity;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gecko.core.io.importer.Main.json;

/** Runs saved native AC analysis and writes its typed JSON result to stdout. */
public final class AcMain {
    private AcMain() { }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args, PrintStream out, PrintStream errors) {
        if (args.length != 1) {
            errors.println("Usage: gecko.core.ac.AcMain <circuit.ipes>");
            return 1;
        }
        try {
            BuildIdentity build = BuildIdentity.current();
            byte[] document = Files.readAllBytes(Path.of(args[0]));
            var model = new CircuitFileParser().parse(new ByteArrayInputStream(document), args[0]);
            if (model.getAcSettings() == null) throw new IllegalArgumentException("The circuit has no saved AC analysis.");
            var preparation = new CircuitPreparation();
            var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
            if (!(result instanceof PreparationResult.Ready ready)) {
                result.report().diagnostics().forEach(diagnostic -> errors.println(diagnostic.message()));
                return 2;
            }
            AcResult ac = ready.execution().acPlan().execute();
            out.println(format(model, ac, ArtifactDigest.sha256(document), build));
            return 0;
        } catch (IOException | CircuitFileParser.CircuitParseException | IllegalArgumentException error) {
            errors.println(error.getMessage());
            return 1;
        }
    }

    private static String format(CircuitModel model, AcResult result, String documentSha256, BuildIdentity build) {
        var source = model.getAcProvenance();
        var metadata = new StringBuilder("{\"ipesSha256\":").append(json(documentSha256))
                .append(",\"javaVersion\":").append(json(System.getProperty("java.runtime.version")))
                .append(",\"analysisId\":").append(json(model.getAcSettings().analysisId()))
                .append(",\"executionSourceDirty\":").append(build.dirty());
        if (!build.jarSha256().isEmpty()) metadata.append(",\"runtimeJarSha256\":").append(json(build.jarSha256()));
        if (!build.revision().isEmpty()) metadata.append(",\"executionRevision\":").append(json(build.revision()));
        if (!build.sourceSnapshotSha256().isEmpty()) {
            metadata.append(",\"executionSourceSnapshotSha256\":").append(json(build.sourceSnapshotSha256()));
        }
        if (source != null) {
            metadata.append(",\"sourceSha256\":").append(json(source.sourceSha256()))
                    .append(",\"importSourceDirty\":").append(source.importSourceDirty());
            if (!source.importRevision().isEmpty()) metadata.append(",\"importRevision\":").append(json(source.importRevision()));
            if (!source.importRuntimeJarSha256().isEmpty()) {
                metadata.append(",\"importRuntimeJarSha256\":").append(json(source.importRuntimeJarSha256()));
            }
            if (!source.importSourceSnapshotSha256().isEmpty()) {
                metadata.append(",\"importSourceSnapshotSha256\":").append(json(source.importSourceSnapshotSha256()));
            }
            metadata
                    .append(",\"dependencySha256\":").append(source.dependencySha256().entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry::getKey))
                            .map(entry -> json(entry.getKey()) + ":" + json(entry.getValue()))
                            .collect(Collectors.joining(",", "{", "}")));
        }
        metadata.append('}');
        int worst = 0;
        for (int i = 1; i < result.residualEta().size(); i++) {
            if (result.residualEta().get(i) > result.residualEta().get(worst)) worst = i;
        }
        String schema = result.hierarchy() == null ? "gecko-ac-result-v1" : "gecko-ac-result-v2";
        return "{\"schema\":" + json(schema) + ",\"metadata\":" + metadata
                + ",\"frequencyHz\":" + result.frequencyHz()
                + ",\"groups\":" + array(result.groups(), group -> "{\"id\":" + json(group.id())
                    + ",\"ground\":" + group.ground()
                    + ",\"members\":" + array(group.members(), member -> "{\"instance\":" + json(member.instance())
                        + ",\"spiceOrder\":" + member.spiceOrder() + "}")
                    + ",\"aliases\":" + array(group.aliases(), alias -> json(alias))
                    + ",\"voltageV\":" + array(group.voltageV(), AcMain::sample) + "}")
                + ",\"currents\":" + array(result.currents(), current -> "{\"instance\":" + json(current.instance())
                    + ",\"currentA\":" + array(current.currentA(), AcMain::sample) + "}")
                + ",\"complexUnknowns\":" + result.complexUnknowns()
                + ",\"residualEta\":" + result.residualEta()
                + ",\"worstResidualFrequencyHz\":" + result.frequencyHz().get(worst)
                + ",\"worstResidualEta\":" + result.residualEta().get(worst)
                + (result.hierarchy() == null ? "" : ",\"hierarchy\":" + hierarchy(result.hierarchy())) + "}";
    }

    private static String hierarchy(AcResult.HierarchyResult hierarchy) {
        return "{\"groups\":" + array(hierarchy.groups(), group -> "{\"voltageTraceId\":" + json(group.voltageTraceId())
                + ",\"ground\":" + group.ground() + ",\"members\":" + array(group.members(), AcMain::member) + "}")
                + ",\"localGroups\":" + array(hierarchy.localGroups(), AcMain::localGroup)
                + ",\"currents\":" + array(hierarchy.currents(), branch -> "{\"positive\":" + member(branch.positive())
                    + ",\"negative\":" + member(branch.negative())
                    + ",\"currentA\":" + array(branch.currentA(), AcMain::sample) + "}")
                + ",\"ports\":" + array(hierarchy.ports(), port -> "{\"external\":" + member(port.external())
                    + ",\"internal\":" + member(port.internal())
                    + ",\"externalGroup\":" + localGroup(port.externalGroup())
                    + ",\"internalGroup\":" + localGroup(port.internalGroup())
                    + ",\"currentIntoChildA\":" + array(port.currentIntoChildA(), AcMain::sample) + "}") + "}";
    }

    private static String localGroup(AcResult.LocalGroup group) {
        return "{\"occurrencePath\":" + array(group.occurrencePath(), value -> json(value)) + ",\"ground\":" + group.ground()
                + ",\"members\":" + array(group.members(), AcMain::member) + "}";
    }

    private static String member(AcResult.SourceMember member) {
        return "{\"occurrencePath\":" + array(member.occurrencePath(), value -> json(value))
                + ",\"localMemberKey\":" + json(member.localMemberKey()) + ",\"instance\":" + json(member.instance())
                + ",\"spiceOrder\":" + member.spiceOrder() + ",\"kind\":" + json(member.kind().name()) + "}";
    }

    private static <T> String array(List<T> values, Function<T, String> format) {
        return values.stream().map(format).collect(Collectors.joining(",", "[", "]"));
    }

    private static String sample(Complex value) {
        return "[" + value.real() + "," + value.imaginary() + "]";
    }
}
