package gecko.core.io;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcInputOrigin;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks profiled inductor membership against native component and occurrence identity. */
public final class AcOriginMembership {
    private AcOriginMembership() { }

    public static Long nativeIdentifier(CircuitModel.ComponentData component) {
        if (component.getSavedBlock() != null) {
            long id = component.getSavedBlock().uniqueObjectIdentifier();
            return id > 0 ? id : null;
        }
        String value = component.getImportTokens().get("uniqueObjectIdentifier");
        if (value == null) return null;
        long id = Long.parseLong(value);
        return id > 0 ? id : null;
    }

    public static void validate(CircuitModel model, HierarchyIndex hierarchy) {
        var provenance = model.getAcProvenance();
        if (provenance == null || !provenance.origin().tested()) {
            if (model.getCircuitComponents().stream().anyMatch(AcOriginMembership::profiledInductor)) {
                throw new IllegalArgumentException("Require the imported ASC origin for the inductor law.");
            }
            return;
        }
        List<CircuitHierarchy.Definition> definitions = model.getHierarchy() == null
                ? List.of() : model.getHierarchy().definitions();
        var definitionIds = definitions.stream().map(CircuitHierarchy.Definition::id)
                .collect(java.util.stream.Collectors.toSet());
        if (model.getAcSettings() == null || !model.getConversionIssues().isEmpty()
                || !provenance.definitionSourceSha256().keySet().equals(definitionIds)
                || !provenance.memberSha256().equals(bind(model, hierarchy, provenance.origin(),
                        provenance.sourceSha256(), provenance.definitionSourceSha256()))) {
            throw new IllegalArgumentException("The ASC origin does not match native component membership.");
        }
    }

    public static Map<Long, String> bind(CircuitModel model, HierarchyIndex hierarchy, AcInputOrigin origin,
                                         String sourceSha256, Map<String, String> definitions) {
        var ids = new java.util.HashSet<Long>();
        var result = new LinkedHashMap<Long, String>();
        for (var component : model.getCircuitComponents()) {
            Long id = nativeIdentifier(component);
            if (id == null || !ids.add(id)) {
                throw new IllegalArgumentException("Require unique native identifiers for the ASC profile.");
            }
            if (!profiledInductor(component)) continue;
            var owned = component.getAcComponent();
            var law = (AcComponent.Inductance) owned.law();
            if (law.origin() != origin || component.getType() != SpicePorts.IND.type()
                    || !owned.instance().equals(component.getName())) {
                throw new IllegalArgumentException("Invalid profiled ASC inductor identity.");
            }
            var saved = component.getSavedBlock();
            long parent = HierarchyIndex.parent(saved);
            String definition = parent == 0 ? "" : hierarchy.sheet(parent).definition().id();
            String source = parent == 0 ? sourceSha256 : definitions.get(definition);
            if (source == null) throw new IllegalArgumentException("Missing ASC inductor source digest.");
            String key = HierarchyIndex.key(saved);
            var fields = new ArrayList<>(List.of(source, origin.profileId(), origin.configurationSha256(),
                    id.toString(), definition, key == null ? "" : key, owned.instance()));
            fields.addAll(hierarchy.path(parent));
            result.put(id, ArtifactDigest.sha256(AcMetadataCodec.encode(fields).getBytes(StandardCharsets.UTF_8)));
        }
        return result;
    }

    private static boolean profiledInductor(CircuitModel.ComponentData component) {
        return component.getAcComponent() != null
                && component.getAcComponent().law() instanceof AcComponent.Inductance law && law.origin().tested();
    }
}
