package gecko.core.io;

import gecko.core.io.CircuitFileParser.CircuitParseException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Stores source conversion findings in a native circuit document. */
public final class ImportIssueCodec {
    public static final String TAG = "ImportIssues";

    private ImportIssueCodec() { }

    public static String encodeSection(List<ImportIssue> issues) {
        if (issues.isEmpty()) return "";
        var text = new StringBuilder("\n<").append(TAG).append(">\n");
        for (var issue : issues) {
            String[] fields = {issue.code(), issue.domain(), issue.component(), issue.message(),
                issue.sourceLocation(), Boolean.toString(issue.unsupported()), issue.parameterPath()};
            text.append("issue");
            for (String field : fields) {
                text.append(' ').append(field == null ? "-" : Base64.getEncoder()
                        .encodeToString(field.getBytes(StandardCharsets.UTF_8)));
            }
            text.append('\n');
        }
        return text.append("<\\").append(TAG).append(">\n").toString();
    }

    public static List<ImportIssue> decodeDocument(String[] lines) throws CircuitParseException {
        var issues = new ArrayList<ImportIssue>();
        for (int start = 0; start < lines.length; start++) {
            String line = lines[start].strip();
            if (!line.startsWith("<") || !line.endsWith(">") || line.startsWith("<\\")) continue;
            String tag = line.substring(1, line.length() - 1);
            int end = SavedCircuitBlock.closingLine(lines, start, tag);
            if (tag.equals(TAG)) {
                if (end == lines.length) throw new CircuitParseException("Close the saved import diagnostics section.");
                for (int index = start + 1; index < end; index++) {
                    if (!lines[index].isBlank()) issues.add(decodeIssue(lines[index]));
                }
            }
            start = end;
        }
        return List.copyOf(issues);
    }

    private static ImportIssue decodeIssue(String line) throws CircuitParseException {
        try {
            String[] fields = line.split(" ", -1);
            if (fields.length != 8 || !fields[0].equals("issue")) throw new IllegalArgumentException();
            for (int i = 1; i < fields.length; i++) {
                fields[i] = fields[i].equals("-") ? null
                        : new String(Base64.getDecoder().decode(fields[i]), StandardCharsets.UTF_8);
            }
            if (fields[1] == null || fields[1].isBlank() || fields[4] == null || fields[4].isBlank()
                    || !("true".equals(fields[6]) || "false".equals(fields[6]))) throw new IllegalArgumentException();
            return new ImportIssue(fields[1], fields[2], fields[3], fields[4],
                    fields[5], Boolean.parseBoolean(fields[6]), fields[7]);
        } catch (IllegalArgumentException error) {
            throw new CircuitParseException("The saved import diagnostic is invalid.", error);
        }
    }
}
