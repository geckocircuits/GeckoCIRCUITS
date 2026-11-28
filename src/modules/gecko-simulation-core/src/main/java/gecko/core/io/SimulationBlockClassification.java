package gecko.core.io;

/** Classifies saved and programmatic simulation content through one type schema. */
public final class SimulationBlockClassification {
    private enum Kind { EXECUTION_DOMAIN, CONTAINER, METADATA, UNSUPPORTED }

    private SimulationBlockClassification() { }

    private static Kind classify(String tag, int type) {
        return switch (tag) {
            case "ElementLK", "ElementCONTROL", "ElementTHERM" -> Kind.EXECUTION_DOMAIN;
            case "ElementSPECIAL" -> switch (type) {
                case 27 -> Kind.CONTAINER;
                case 70 -> Kind.METADATA;
                default -> Kind.UNSUPPORTED;
            };
            default -> Kind.UNSUPPORTED;
        };
    }

    // Saved containers retain their historical import evidence until hierarchy projection resolves them.
    static ImportIssue savedBlockIssue(String tag, int type, String name, String sourceLocation) {
        var kind = classify(tag, type);
        if (kind == Kind.EXECUTION_DOMAIN || kind == Kind.METADATA) return null;
        return unsupportedIssue(tag.substring("Element".length()), name, sourceLocation);
    }

    /**
     * Retains unsupported SPECIAL evidence without changing source components or duplicating saved evidence.
     * Disabled owners remain inspectable. The execution projection filters their diagnostics.
     *
     * @param model the owned circuit description to classify
     */
    public static void retainSpecialComponentIssues(CircuitModel model) {
        for (var component : model.getSpecialComponents()) {
            if (classify("ElementSPECIAL", component.getType()) != Kind.UNSUPPORTED) continue;
            var issue = unsupportedIssue("SPECIAL", component.getName(), component.getSourceLocation());
            if (model.getImportIssues().stream().noneMatch(existing -> existing.code().equals(issue.code())
                    && java.util.Objects.equals(existing.domain(), issue.domain())
                    && java.util.Objects.equals(existing.component(), issue.component())
                    && java.util.Objects.equals(existing.sourceLocation(), issue.sourceLocation()))) {
                model.getImportIssues().add(issue);
            }
        }
    }

    private static ImportIssue unsupportedIssue(String domain, String name, String sourceLocation) {
        return new ImportIssue("UNSUPPORTED_SIMULATION_BLOCK", domain, name,
                "The simulation block is unsupported by the headless engine.", sourceLocation, true);
    }
}
