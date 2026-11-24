package gecko.core.io.importer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the text structure used by PLECS Standalone. See the Introduction to
 * PLECS Standalone tutorial at https://www.plexim.com/support/tutorials.
 */
final class PlecsText {
    private enum Kind { WORD, STRING, VECTOR, OPEN, CLOSE, COMMA, END }

    private static final Set<String> STRING_LIST_FIELDS = Set.of("Signals", "PossibleValues");

    private record Token(Kind kind, String value, int line) { }

    record Block(String name, int line, Map<String, String> fields, Map<String, List<String>> lists, List<Block> children) {
        String get(String key) { return fields.get(key); }

        List<Block> children(String key) {
            return children.stream().filter(block -> block.name().equals(key)).toList();
        }
    }

    static final class SyntaxError extends IllegalArgumentException {
        private final int line;

        SyntaxError(int line, String message) {
            super(message);
            this.line = line;
        }

        int line() { return line; }
    }

    private final String input;
    private int offset;
    private int line = 1;
    private Token token;

    private PlecsText(String input) {
        this.input = input;
        if (input.startsWith("\ufeff")) offset++;
        token = next();
    }

    static Block parse(String source) {
        var reader = new PlecsText(source);
        Token root = reader.expect(Kind.WORD, "Start the file with a Plecs block.");
        if (!root.value().equals("Plecs")) throw new SyntaxError(root.line(), "Start the file with a Plecs block.");
        reader.expect(Kind.OPEN, "Open the Plecs block with a brace.");
        Block document = reader.block(root, 0);
        if (reader.token.kind() == Kind.WORD && reader.token.value().equals("DemoSignature")) {
            reader.advance();
            reader.stringValue();
        }
        reader.expect(Kind.END, "Remove the text after the Plecs block.");
        return document;
    }

    private Block block(Token name, int depth) {
        if (depth > 128) throw new SyntaxError(name.line(), "Reduce the block nesting depth.");
        var fields = new LinkedHashMap<String, String>();
        var lists = new LinkedHashMap<String, List<String>>();
        var children = new ArrayList<Block>();
        while (token.kind() != Kind.CLOSE) {
            Token field = expect(Kind.WORD, "Specify a field name or close the block.");
            switch (token.kind()) {
                case OPEN -> {
                    advance();
                    if (token.kind() == Kind.STRING || STRING_LIST_FIELDS.contains(field.value())) {
                        put(lists, field, stringList());
                    } else {
                        children.add(block(field, depth + 1));
                    }
                }
                case STRING, WORD, VECTOR -> {
                    String value = scalarValue();
                    if (name.value().equals("Configurations") && field.value().equals("Name")) {
                        // Configurable subsystems repeat each configuration name before its schematic.
                        fields.putIfAbsent(field.value(), value);
                    } else {
                        put(fields, field, value);
                    }
                }
                default -> throw new SyntaxError(token.line(), "Specify a value for " + field.value() + ".");
            }
        }
        advance();
        return new Block(name.value(), name.line(), fields, lists, children);
    }

    private String scalarValue() {
        if (token.kind() == Kind.STRING) return stringValue();
        Token value = advance();
        if (value.kind() != Kind.WORD || !value.value().equals("base64") || token.kind() != Kind.STRING) return value.value();
        String encoded = stringValue();
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            throw new SyntaxError(value.line(), "Specify valid base64 text.");
        }
    }

    private static <T> void put(Map<String, T> fields, Token field, T value) {
        if (fields.putIfAbsent(field.value(), value) != null)
            throw new SyntaxError(field.line(), "Specify " + field.value() + " once per block.");
    }

    private List<String> stringList() {
        var values = new ArrayList<String>();
        if (token.kind() != Kind.CLOSE) {
            values.add(stringValue());
            while (token.kind() == Kind.COMMA) {
                advance();
                values.add(stringValue());
            }
        }
        expect(Kind.CLOSE, "Separate quoted list items with a comma and close the list.");
        return List.copyOf(values);
    }

    private String stringValue() {
        var value = new StringBuilder(expect(Kind.STRING, "Use a quoted string.").value());
        while (token.kind() == Kind.STRING) value.append(advance().value());
        return value.toString();
    }

    private Token expect(Kind kind, String message) {
        if (token.kind() != kind) throw new SyntaxError(token.line(), message);
        return advance();
    }

    private Token advance() {
        Token previous = token;
        token = next();
        return previous;
    }

    private Token next() {
        skipWhitespaceAndComments();
        if (offset == input.length()) return new Token(Kind.END, "", line);
        int start = offset, startLine = line;
        char c = take();
        return switch (c) {
            case '{' -> new Token(Kind.OPEN, "{", startLine);
            case '}' -> new Token(Kind.CLOSE, "}", startLine);
            case ',' -> new Token(Kind.COMMA, ",", startLine);
            case '"' -> new Token(Kind.STRING, quoted(startLine), startLine);
            case '[' -> new Token(Kind.VECTOR, vector(startLine), startLine);
            case ']' -> throw new SyntaxError(startLine, "Remove the extra closing bracket.");
            default -> {
                while (offset < input.length() && !Character.isWhitespace(input.charAt(offset))
                        && "{}[],\"".indexOf(input.charAt(offset)) < 0 && !startsComment()) take();
                yield new Token(Kind.WORD, input.substring(start, offset), startLine);
            }
        };
    }

    private String quoted(int startLine) {
        var value = new StringBuilder();
        while (offset < input.length()) {
            char c = take();
            if (c == '"') return value.toString();
            if (c == '\\' && offset < input.length()) {
                char escaped = take();
                switch (escaped) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '\\', '"' -> value.append(escaped);
                    default -> value.append('\\').append(escaped);
                }
            } else {
                value.append(c);
            }
        }
        throw new SyntaxError(startLine, "Close the quoted string.");
    }

    private String vector(int startLine) {
        var value = new StringBuilder("[");
        while (offset < input.length()) {
            if (startsComment()) {
                skipWhitespaceAndComments();
                value.append(' ');
                continue;
            }
            char c = take();
            value.append(c);
            if (c == ']') return value.toString();
            if ("[{}\"".indexOf(c) >= 0) break;
        }
        throw new SyntaxError(startLine, "Close the vector with a bracket.");
    }

    private void skipWhitespaceAndComments() {
        while (offset < input.length()) {
            if (Character.isWhitespace(input.charAt(offset))) {
                take();
            } else if (input.startsWith("//", offset) || input.charAt(offset) == '#') {
                while (offset < input.length() && input.charAt(offset) != '\n' && input.charAt(offset) != '\r') take();
            } else if (input.startsWith("/*", offset)) {
                int startLine = line;
                offset += 2;
                while (offset < input.length() && !input.startsWith("*/", offset)) take();
                if (offset == input.length()) throw new SyntaxError(startLine, "Close the block comment.");
                offset += 2;
            } else {
                return;
            }
        }
    }

    private boolean startsComment() {
        return input.charAt(offset) == '#' || input.startsWith("//", offset) || input.startsWith("/*", offset);
    }

    private char take() {
        char c = input.charAt(offset++);
        if (c == '\n' || c == '\r' && (offset == input.length() || input.charAt(offset) != '\n')) line++;
        return c;
    }
}
