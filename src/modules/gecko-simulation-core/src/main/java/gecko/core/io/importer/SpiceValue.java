package gecko.core.io.importer;

import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Evaluates scalar SPICE parameters without executing source text. */
final class SpiceValue {
    private final Map<String, String> parameters;
    private final Set<String> evaluating;
    private final String text;
    private int position;
    private int depth;

    private SpiceValue(String text, Map<String, String> parameters, Set<String> evaluating, int depth) {
        this.text = text.replace('{', '(').replace('}', ')').toLowerCase(Locale.ROOT);
        this.parameters = parameters;
        this.evaluating = evaluating;
        this.depth = depth;
    }

    static double evaluate(String text, Map<String, String> parameters) {
        return new SpiceValue(text, parameters, new HashSet<>(), 0).evaluate();
    }

    private double evaluate() {
        double value = sum();
        whitespace();
        if (position != text.length() || !Double.isFinite(value)) throw new IllegalArgumentException("Invalid scalar value: " + text);
        return value;
    }

    private double sum() {
        double result = product();
        while (true) {
            if (eat('+')) result += product();
            else if (eat('-')) result -= product();
            else return result;
        }
    }

    private double product() {
        double result = unary();
        while (true) {
            whitespace();
            if (text.startsWith("**", position)) return result;
            if (eat('*')) result *= unary();
            else if (eat('/')) result /= unary();
            else return result;
        }
    }

    private double unary() {
        if (depth >= 128) throw new IllegalArgumentException("The scalar expression is nested too deeply.");
        depth++;
        try {
            if (eat('+')) return unary();
            if (eat('-')) return -unary();
            double value = atom();
            whitespace();
            if (text.startsWith("**", position)) { position += 2; value = Math.pow(value, unary()); }
            else if (eat('^')) value = Math.pow(value, unary());
            return value;
        } finally {
            depth--;
        }
    }

    private double atom() {
        whitespace();
        if (eat('(')) {
            double value = sum();
            if (!eat(')')) throw new IllegalArgumentException("Close the scalar expression.");
            return value;
        }
        int start = position;
        if (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
            while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) position++;
            if (position < text.length() && text.charAt(position) == 'e') {
                position++;
                if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) position++;
                while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
            }
            double value = Double.parseDouble(text.substring(start, position));
            int suffix = position;
            while (position < text.length() && (Character.isLetter(text.charAt(position)) || text.charAt(position) == 'µ')) position++;
            String unit = text.substring(suffix, position);
            double multiplier = unit.startsWith("meg") ? 1e6 : unit.startsWith("mil") ? 25.4e-6 : unit.isEmpty() ? 1
                    : switch (unit.charAt(0)) {
                        case 't' -> 1e12; case 'g' -> 1e9; case 'k' -> 1e3; case 'm' -> 1e-3;
                        case 'u', 'µ' -> 1e-6; case 'n' -> 1e-9; case 'p' -> 1e-12; case 'f' -> 1e-15; default -> 1;
                    };
            return value * multiplier;
        }
        while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) position++;
        String name = text.substring(start, position);
        if (name.equals("pi")) return Math.PI;
        if (eat('(')) {
            double a = sum();
            if (!eat(')')) throw new IllegalArgumentException("Close the scalar function.");
            return switch (name) {
                case "sqrt" -> Math.sqrt(a); case "abs" -> Math.abs(a); case "sin" -> Math.sin(a);
                case "cos" -> Math.cos(a); case "tan" -> Math.tan(a); case "exp" -> Math.exp(a);
                case "ln", "log" -> Math.log(a); case "log10" -> Math.log10(a);
                default -> throw new IllegalArgumentException("Unsupported scalar function: " + name);
            };
        }
        if (!parameters.containsKey(name)) throw new IllegalArgumentException("Unknown scalar parameter: " + name);
        if (!evaluating.add(name)) throw new IllegalArgumentException("Circular scalar parameter: " + name);
        try { return new SpiceValue(parameters.get(name), parameters, evaluating, depth).evaluate(); }
        finally { evaluating.remove(name); }
    }

    private boolean eat(char expected) {
        whitespace();
        if (position < text.length() && text.charAt(position) == expected) { position++; return true; }
        return false;
    }

    private void whitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
    }
}
