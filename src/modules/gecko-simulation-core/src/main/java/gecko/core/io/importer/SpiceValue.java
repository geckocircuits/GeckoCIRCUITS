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

        /** Real value and exact gradient over the probes of a behavioural law. */
        Dual dual(Scope scope, Inputs inputs, int depth);
    }

    private record Constant(double value) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            return finite(new Complex(value, 0), Double.toString(value));
        }

        public Dual dual(Scope scope, Inputs inputs, int depth) { return inputs.constant(value); }
    }

    private record Reference(String name) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            if (name.equals("pi")) return new Complex(Math.PI, 0);
            if (name.equals("s") && s != null) return s;
            return scope.parameter(name, depth);
        }

        public Dual dual(Scope scope, Inputs inputs, int depth) {
            return inputs.constant(evaluate(scope, null, depth).real());
        }
    }

    private record Binary(char operator, Node left, Node right) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            checkDepth(depth);
            Complex a = left.evaluate(scope, s, depth + 1);
            Complex b = right.evaluate(scope, s, depth + 1);
            return calculate(operator, a, b, s);
        }

        public Dual dual(Scope scope, Inputs inputs, int depth) {
            checkDepth(depth);
            return combine(operator, left.dual(scope, inputs, depth + 1), right.dual(scope, inputs, depth + 1), inputs);
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

        public Dual dual(Scope scope, Inputs inputs, int depth) {
            checkDepth(depth);
            Dual result = first.dual(scope, inputs, depth + 1);
            for (var term : following) {
                result = combine(term.operator, result, term.operand.dual(scope, inputs, depth + 1), inputs);
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
                default -> truth(compare(operator, a.real(), b.real()));
            }, 0), Character.toString(operator));
        }
        if (LOGICAL.indexOf(operator) >= 0) throw new IllegalArgumentException("Compare only real values.");
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
                    case "atan" -> Math.atan(x);
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

        public Dual dual(Scope scope, Inputs inputs, int depth) {
            checkDepth(depth);
            Dual a = argument.dual(scope, inputs, depth + 1);
            double x = a.value();
            double value = evaluate(new Constant(x), name);
            double slope = switch (name) {
                case "sqrt" -> 0.5 / value;
                case "abs" -> Math.signum(x);
                case "sin" -> Math.cos(x);
                case "cos" -> -Math.sin(x);
                case "tan" -> 1 + value * value;
                case "atan" -> 1 / (1 + x * x);
                case "exp" -> value;
                case "ln", "log" -> 1 / x;
                default -> 1 / (x * Math.log(10));
            };
            return inputs.chain(value, slope, a);
        }

        private static double evaluate(Constant argument, String name) {
            return new Function(name, argument).evaluate(null, null, 0).real();
        }
    }

    /** A function of several arguments. {@code if} evaluates only its selected branch. */
    private record Call(String name, List<Node> arguments) implements Node {
        Call {
            if (!name.equals("if") && !name.equals("atan2")) throw unsupported(name);
            int expected = name.equals("if") ? 3 : 2;
            if (arguments.size() != expected) throw new IllegalArgumentException("Use " + expected + " arguments in " + name + ".");
            arguments = List.copyOf(arguments);
        }

        public Complex evaluate(Scope scope, Complex s, int depth) {
            checkDepth(depth);
            if (name.equals("if")) {
                Complex condition = arguments.get(0).evaluate(scope, s, depth + 1);
                if (condition.imaginary() != 0) throw new IllegalArgumentException("Compare only real values.");
                return arguments.get(condition.real() > 0.5 ? 1 : 2).evaluate(scope, s, depth + 1);
            }
            Complex y = arguments.get(0).evaluate(scope, s, depth + 1), x = arguments.get(1).evaluate(scope, s, depth + 1);
            if (y.imaginary() != 0 || x.imaginary() != 0) throw new IllegalArgumentException("Use atan2 with real values.");
            return finite(new Complex(Math.atan2(y.real(), x.real()), 0), name);
        }

        public Dual dual(Scope scope, Inputs inputs, int depth) {
            checkDepth(depth);
            if (name.equals("if")) {
                boolean selected = inputs.branch(arguments.get(0).dual(scope, inputs, depth + 1).value() > 0.5);
                return arguments.get(selected ? 1 : 2).dual(scope, inputs, depth + 1);
            }
            Dual y = arguments.get(0).dual(scope, inputs, depth + 1), x = arguments.get(1).dual(scope, inputs, depth + 1);
            double squared = x.value() * x.value() + y.value() * y.value();
            double[] gradient = new double[inputs.size()];
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] = (x.value() * y.gradient()[i] - y.value() * x.gradient()[i]) / squared;
            }
            return inputs.checked(Math.atan2(y.value(), x.value()), gradient, name);
        }
    }

    /** A node voltage, a node-pair voltage or a voltage-source branch current of a behavioural law. */
    public record Probe(boolean current, String first, String second) {
        public Probe {
            if (first.isBlank() || second != null && (current || second.isBlank())) {
                throw new IllegalArgumentException("Use v(node), v(node,node) or i(source).");
            }
        }
    }

    private record ProbeReference(int index) implements Node {
        public Complex evaluate(Scope scope, Complex s, int depth) {
            throw new IllegalArgumentException("Use v() and i() only in a behavioural source.");
        }

        public Dual dual(Scope scope, Inputs inputs, int depth) { return inputs.probe(index); }
    }

    /** Probe values of one evaluation, and the outcomes of its comparisons. */
    private static final class Inputs {
        private final double[] values;
        private long branches = 1;

        Inputs(double[] values) { this.values = values; }

        int size() { return values.length; }

        Dual constant(double value) { return new Dual(value, new double[values.length]); }

        Dual probe(int index) {
            var gradient = new double[values.length];
            gradient[index] = 1;
            return new Dual(values[index], gradient);
        }

        Dual chain(double value, double slope, Dual argument) {
            var gradient = new double[values.length];
            for (int i = 0; i < gradient.length; i++) gradient[i] = argument.gradient()[i] == 0 ? 0 : slope * argument.gradient()[i];
            return checked(value, gradient, "function");
        }

        Dual checked(double value, double[] gradient, String expression) {
            finite(new Complex(value, 0), expression);
            for (double slope : gradient) finite(new Complex(slope, 0), expression);
            return new Dual(value, gradient);
        }

        /** Records a comparison outcome. The code changes when any outcome of the expression changes. */
        boolean branch(boolean outcome) {
            // ponytail: a rolling code, not a list; a collision only keeps the line search for one Newton step.
            branches = branches * 3 + (outcome ? 1 : 2);
            return outcome;
        }
    }

    private record Dual(double value, double[] gradient) { }

    /** Comparisons and Boolean operators; {@code l} and {@code g} stand for {@code <=} and {@code >=}. */
    private static final String LOGICAL = "<>lg&|";

    /** LTspice converts an operand to Boolean as true above 0.5. */
    private static boolean compare(char operator, double a, double b) {
        return switch (operator) {
            case '<' -> a < b;
            case '>' -> a > b;
            case 'l' -> a <= b;
            case 'g' -> a >= b;
            case '&' -> a > 0.5 && b > 0.5;
            case '|' -> a > 0.5 || b > 0.5;
            default -> throw new IllegalStateException();
        };
    }

    private static double truth(boolean value) { return value ? 1 : 0; }

    private static Dual combine(char operator, Dual a, Dual b, Inputs inputs) {
        if (LOGICAL.indexOf(operator) >= 0) return inputs.constant(truth(inputs.branch(compare(operator, a.value(), b.value()))));
        double x = a.value(), y = b.value();
        double value = calculate(operator, new Complex(x, 0), new Complex(y, 0), null).real();
        double[] gradient = new double[inputs.size()];
        for (int i = 0; i < gradient.length; i++) {
            double da = a.gradient()[i], db = b.gradient()[i];
            gradient[i] = switch (operator) {
                case '+' -> da + db;
                case '-' -> da - db;
                case '*' -> da * y + x * db;
                case '/' -> (da - value * db) / y;
                // d(x^y) = y x^(y-1) dx + x^y ln(x) dy, each term only where its input varies.
                case '^' -> (da == 0 ? 0 : y * Math.pow(x, y - 1) * da) + (db == 0 ? 0 : value * Math.log(x) * db);
                default -> throw new IllegalStateException();
            };
        }
        return inputs.checked(value, gradient, Character.toString(operator));
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
    /** Probes in first-use order, or null where probes are not allowed. */
    private Map<Probe, Integer> probes;

    private SpiceValue(String text) {
        this.text = text.replace('{', '(').replace('}', ')').toLowerCase(Locale.ROOT);
    }

    public static double evaluate(String text, Map<String, String> parameters) {
        return finite(parse(text).evaluate(new Scope(parameters), null, 0), text).real();
    }

    public static double requirePositivePassive(double value, String member) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Require a finite positive passive component value on " + member + ".");
        }
        return value;
    }

    public static Transfer transfer(String text, Map<String, String> parameters) {
        Node node = parse(text);
        var scope = new Scope(parameters);
        scope.parameters.keySet().forEach(name -> scope.parameter(name, 0));
        // Each solve uses its own precomputed scalar bindings.
        return s -> finite(node.evaluate(scope, s, 0), text);
    }

    /** A compiled behavioural law: a real expression of probes with an exact gradient. */
    public static final class Behaviour {
        /** The value, its gradient over {@link #probes()}, and a code of the comparison outcomes that selected it. */
        public record Point(double value, double[] gradient, long branches) { }

        private final Node node;
        private final Scope scope;
        private final List<Probe> probes;

        private Behaviour(Node node, Scope scope, List<Probe> probes) {
            this.node = node;
            this.scope = scope;
            this.probes = probes;
        }

        public List<Probe> probes() { return probes; }

        /** @throws IllegalArgumentException when the value or a slope is not finite */
        public Point at(double[] probeValues) {
            if (probeValues.length != probes.size()) throw new IllegalArgumentException("Supply one value per probe.");
            var inputs = new Inputs(probeValues);
            Dual result = node.dual(scope, inputs, 0);
            return new Point(result.value(), result.gradient(), inputs.branches);
        }
    }

    /** Compiles a behavioural expression. Scalar parameters bind on first use and stay bound for the life of the law. */
    public static Behaviour behaviour(String text, Map<String, String> parameters) {
        var parser = new SpiceValue(text);
        parser.probes = new LinkedHashMap<>();
        Node node = parser.complete();
        return new Behaviour(node, new Scope(parameters), List.copyOf(parser.probes.keySet()));
    }

    private static Complex finite(Complex value, String expression) {
        if (!value.finite()) throw new IllegalArgumentException("The expression is nonfinite: " + expression);
        return value;
    }

    private static void checkDepth(int depth) {
        if (depth >= 128) throw new IllegalArgumentException("The SPICE expression is nested too deeply.");
    }

    private static Node parse(String text) {
        return new SpiceValue(text).complete();
    }

    private Node complete() {
        Node result = logical();
        whitespace();
        if (position != text.length()) throw new IllegalArgumentException("Invalid SPICE expression: " + text);
        return result;
    }

    /** Boolean operators bind loosest: {@code |} below {@code &}, both below the comparisons. */
    private Node logical() {
        Node result = conjunction();
        while (eat('|')) result = new Binary('|', result, conjunction());
        return result;
    }

    private Node conjunction() {
        Node result = comparison();
        while (eat('&')) result = new Binary('&', result, comparison());
        return result;
    }

    private Node comparison() {
        Node result = sum();
        while (true) {
            whitespace();
            if (position >= text.length() || text.charAt(position) != '<' && text.charAt(position) != '>') return result;
            char operator = text.charAt(position++);
            if (eat('=')) operator = operator == '<' ? 'l' : 'g';
            result = new Binary(operator, result, sum());
        }
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
            Node value = logical();
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
            if ((name.equals("v") || name.equals("i")) && probes != null) return probe(name.equals("i"));
            var arguments = new ArrayList<Node>(List.of(logical()));
            while (eat(',')) arguments.add(logical());
            if (!eat(')')) throw new IllegalArgumentException("Close the scalar function.");
            return arguments.size() == 1 && !name.equals("if") ? new Function(name, arguments.getFirst()) : new Call(name, arguments);
        }
        return new Reference(name);
    }

    /** Reads the raw names of a probe, so that node names such as {@code 1} or {@code n001} stay names. */
    private Node probe(boolean current) {
        int end = text.indexOf(')', position);
        if (end < 0) throw new IllegalArgumentException("Close the probe.");
        String[] names = text.substring(position, end).split(",", -1);
        position = end + 1;
        if (names.length > 2) throw new IllegalArgumentException("Use v(node), v(node,node) or i(source).");
        var probe = new Probe(current, names[0].strip(), names.length == 2 ? names[1].strip() : null);
        return new ProbeReference(probes.computeIfAbsent(probe, ignored -> probes.size()));
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
