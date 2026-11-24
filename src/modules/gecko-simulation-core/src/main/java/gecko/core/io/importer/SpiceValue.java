package gecko.core.io.importer;

import gecko.core.ac.Complex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Parses a safe expression shared by scalar values and AC transfer laws. */
public final class SpiceValue {
    private interface Node {
        Complex evaluate(Scope scope, Complex s, int depth);
    }

    private record Constant(double value) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            return finite(new Complex(value, 0), Double.toString(value));
        }
    }

    private record Reference(String name) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            if (name.equals("pi")) return new Complex(Math.PI, 0);
            if (name.equals("s") && s != null) return s;
            return scope.parameter(name, depth);
        }
    }

    private record Binary(char operator, Node left, Node right) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            checkDepth(depth);
            Complex a = left.evaluate(scope, s, depth + 1);
            Complex b = right.evaluate(scope, s, depth + 1);
            return calculate(operator, a, b, s);
        }
    }

    private record Term(char operator, Node operand) { }

    private record Chain(Node first, List<Term> following) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            checkDepth(depth);
            Complex result = first.evaluate(scope, s, depth + 1);
            for (var term : following) {
                result = calculate(term.operator, result, term.operand.evaluate(scope, s, depth + 1), s);
            }
            return result;
        }
    }

    private static Complex calculate(char operator, Complex a, Complex b, Complex s) {
        if (a.imaginary() == 0 && b.imaginary() == 0 && (s == null || operator != '^' || a.real() >= 0 || b.real() == Math.rint(b.real()))) {
            return finite(new Complex(switch (operator) {
                case '+' -> a.real() + b.real();
                case '-' -> a.real() - b.real();
                case '*' -> a.real() * b.real();
                case '/' -> a.real() / b.real();
                case '^' -> Math.pow(a.real(), b.real());
                default -> throw new IllegalStateException();
            }, 0), Character.toString(operator));
        }
        return finite(switch (operator) {
            case '+' -> a.add(b);
            case '-' -> a.subtract(b);
            case '*' -> a.multiply(b);
            case '/' -> a.divide(b);
            case '^' -> a.power(b);
            default -> throw new IllegalStateException();
        }, Character.toString(operator));
    }

    private record Function(String name, Node argument) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            checkDepth(depth);
            Complex a = argument.evaluate(scope, s, depth + 1);
            double x = a.real(), y = a.imaginary();
            if (s == null) {
                return finite(new Complex(switch (name) {
                    case "sqrt" -> Math.sqrt(x);
                    case "abs" -> Math.abs(x);
                    case "sin" -> Math.sin(x);
                    case "cos" -> Math.cos(x);
                    case "tan" -> Math.tan(x);
                    case "exp" -> Math.exp(x);
                    case "ln", "log" -> Math.log(x);
                    case "log10" -> Math.log10(x);
                    default -> throw unsupported(name);
                }, 0), name);
            }
            return finite(switch (name) {
                case "sqrt" -> a.power(new Complex(0.5, 0));
                case "abs" -> new Complex(a.abs(), 0);
                case "sin" -> new Complex(Math.sin(x) * Math.cosh(y), Math.cos(x) * Math.sinh(y));
                case "cos" -> new Complex(Math.cos(x) * Math.cosh(y), -Math.sin(x) * Math.sinh(y));
                case "tan" -> new Complex(Math.sin(2 * x), Math.sinh(2 * y))
                        .scale(1 / (Math.cos(2 * x) + Math.cosh(2 * y)));
                case "exp" -> a.exponential();
                case "ln", "log" -> a.logarithm();
                case "log10" -> a.logarithm().scale(1 / Math.log(10));
                default -> throw unsupported(name);
            }, name);
        }
    }

    private static IllegalArgumentException unsupported(String name) {
        return new IllegalArgumentException("Unsupported SPICE function: " + name);
    }

    private static final class Scope {
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private final Map<String, Complex> values = new LinkedHashMap<>();
        private final Set<String> evaluating = new HashSet<>();

        Scope(Map<String, String> source) {
            source.forEach((name, value) -> parameters.put(name.toLowerCase(Locale.ROOT), value));
        }

        Complex parameter(String name, int depth) {
            checkDepth(depth);
            Complex value = values.get(name);
            if (value != null) return value;
            String expression = parameters.get(name);
            if (expression == null) throw new IllegalArgumentException("Unknown scalar parameter: " + name);
            if (!evaluating.add(name)) throw new IllegalArgumentException("Circular scalar parameter: " + name);
            try {
                value = finite(parse(expression).evaluate(this, null, depth + 1), expression);
                values.put(name, value);
                return value;
            } finally {
                evaluating.remove(name);
            }
        }
    }

    @FunctionalInterface
    public interface Transfer {
        Complex evaluate(Complex s);
    }

    private final String text;
    private int position;
    private int depth;

    private SpiceValue(String text) {
        this.text = text.replace('{', '(').replace('}', ')').toLowerCase(Locale.ROOT);
    }

    public static double evaluate(String text, Map<String, String> parameters) {
        return finite(parse(text).evaluate(new Scope(parameters), null, 0), text).real();
    }

    public static Transfer transfer(String text, Map<String, String> parameters) {
        Node node = parse(text);
        var scope = new Scope(parameters);
        scope.parameters.keySet().forEach(name -> scope.parameter(name, 0));
        // Each solve uses its own precomputed scalar bindings.
        return s -> finite(node.evaluate(scope, s, 0), text);
    }

    private static Complex finite(Complex value, String expression) {
        if (!value.finite()) throw new IllegalArgumentException("The expression is nonfinite: " + expression);
        return value;
    }

    private static void checkDepth(int depth) {
        if (depth >= 128) throw new IllegalArgumentException("The SPICE expression is nested too deeply.");
    }

    private static Node parse(String text) {
        var parser = new SpiceValue(text);
        Node result = parser.sum();
        parser.whitespace();
        if (parser.position != parser.text.length()) throw new IllegalArgumentException("Invalid SPICE expression: " + text);
        return result;
    }

    private Node sum() {
        Node first = product();
        var following = new ArrayList<Term>();
        while (true) {
            if (eat('+')) following.add(new Term('+', product()));
            else if (eat('-')) following.add(new Term('-', product()));
            else return following.isEmpty() ? first : new Chain(first, List.copyOf(following));
        }
    }

    private Node product() {
        Node first = unary();
        var following = new ArrayList<Term>();
        while (true) {
            whitespace();
            if (text.startsWith("**", position)) break;
            if (eat('*')) following.add(new Term('*', unary()));
            else if (eat('/')) following.add(new Term('/', unary()));
            else break;
        }
        return following.isEmpty() ? first : new Chain(first, List.copyOf(following));
    }

    private Node unary() {
        checkDepth(depth);
        depth++;
        try {
            if (eat('+')) return unary();
            if (eat('-')) return new Binary('*', new Constant(-1), unary());
            Node value = atom();
            whitespace();
            if (text.startsWith("**", position)) {
                position += 2;
                value = new Binary('^', value, unary());
            } else if (eat('^')) value = new Binary('^', value, unary());
            return value;
        } finally {
            depth--;
        }
    }

    private Node atom() {
        whitespace();
        if (eat('(')) {
            Node value = sum();
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
            return new Constant(value * multiplier);
        }
        while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) position++;
        String name = text.substring(start, position);
        if (name.isEmpty()) throw new IllegalArgumentException("Require a SPICE expression.");
        if (eat('(')) {
            Node a = sum();
            if (!eat(')')) throw new IllegalArgumentException("Close the scalar function.");
            return new Function(name, a);
        }
        return new Reference(name);
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
