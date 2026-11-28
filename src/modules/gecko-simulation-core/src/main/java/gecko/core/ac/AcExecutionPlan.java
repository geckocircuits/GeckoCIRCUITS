package gecko.core.ac;

import gecko.core.io.AcOriginMembership;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.SpicePorts;
import gecko.core.io.importer.SpiceValue;
import gecko.core.math.LUDecomposition;
import gecko.core.math.Matrix;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;

/** Prepared linear AC network. Each execution stamps and solves its own equations. */
public final class AcExecutionPlan {
    private record Node(CircuitTopology.Group group, int row, List<AcResult.Member> members, TreeSet<String> aliases) { }
    private record Branch(String instance, int positive, int negative, int sensePositive, int senseNegative,
                          int currentRow, SpiceValue.Transfer admittance, double capacitance,
                          SpiceValue.Transfer gain, Complex excitation, double dc) {
        Branch withExcitation(double value) {
            return new Branch(instance, positive, negative, sensePositive, senseNegative, currentRow,
                    admittance, capacitance, gain, new Complex(value, 0), dc);
        }

        Branch withConductance(double value) {
            return withAdmittance(ignored -> new Complex(value, 0));
        }

        Branch withAdmittance(SpiceValue.Transfer value) {
            return new Branch(instance, positive, negative, sensePositive, senseNegative, currentRow,
                    value, capacitance, gain, excitation, dc);
        }
    }

    private record BipolarDevice(int[] external, int[] intrinsic, double[] resistance, BipolarJunction model,
                                 List<AcResult.SourceMember> terminals) {
        BipolarJunction.Point at(Complex[] bias) {
            return model.at(voltage(bias, 2), voltage(bias, 0));
        }

        double voltage(Complex[] bias, int terminal) {
            return AcExecutionPlan.at(bias, intrinsic[1]).real() - AcExecutionPlan.at(bias, intrinsic[terminal]).real();
        }

        void stampSeries(Complex[][] matrix) {
            for (int terminal = 0; terminal < 3; terminal++) {
                if (resistance[terminal] == 0) continue;
                int p = external[terminal], n = intrinsic[terminal];
                var y = new Complex(1 / resistance[terminal], 0);
                add(matrix, p, p, y);
                add(matrix, p, n, y.scale(-1));
                add(matrix, n, p, y.scale(-1));
                add(matrix, n, n, y);
            }
        }

        void stampJunction(Complex[][] matrix, BipolarJunction.Point point, Complex frequency) {
            int collector = intrinsic[0], base = intrinsic[1], emitter = intrinsic[2];
            for (int i = 0; i < 3; i++) {
                int row = intrinsic[i];
                var slopes = slopes(point, frequency, i);
                var be = slopes.be();
                var bc = slopes.bc();
                add(matrix, row, base, be.add(bc));
                add(matrix, row, emitter, be.scale(-1));
                add(matrix, row, collector, bc.scale(-1));
            }
        }

        private record Slopes(Complex be, Complex bc) { }

        private static Slopes slopes(BipolarJunction.Point point, Complex frequency, int i) {
            var be = new Complex(point.dBe()[i], 0);
            var bc = new Complex(point.dBc()[i], 0);
            if (i == 0) bc = bc.subtract(frequency.scale(point.cBc()));
            if (i == 1) {
                be = be.add(frequency.scale(point.cBe()));
                bc = bc.add(frequency.scale(point.cBc() + point.cBeBc()));
            }
            if (i == 2) {
                be = be.subtract(frequency.scale(point.cBe()));
                bc = bc.subtract(frequency.scale(point.cBeBc()));
            }
            return new Slopes(be, bc);
        }

        Complex terminalCurrent(BipolarJunction.Point point, Complex frequency, Complex[] solved, int terminal) {
            if (resistance[terminal] > 0) {
                return AcExecutionPlan.at(solved, external[terminal]).subtract(AcExecutionPlan.at(solved, intrinsic[terminal]))
                        .scale(1 / resistance[terminal]);
            }
            var base = AcExecutionPlan.at(solved, intrinsic[1]);
            var be = base.subtract(AcExecutionPlan.at(solved, intrinsic[2]));
            var bc = base.subtract(AcExecutionPlan.at(solved, intrinsic[0]));
            var slope = slopes(point, frequency, terminal);
            return slope.be().multiply(be).add(slope.bc().multiply(bc));
        }

        void stampBias(Complex[][] matrix, Complex[] rhs, Complex[] bias) {
            var point = at(bias);
            stampJunction(matrix, point, Complex.ZERO);
            double be = voltage(bias, 2), bc = voltage(bias, 0);
            for (int i = 0; i < 3; i++) {
                double offset = point.current()[i] - point.dBe()[i] * be - point.dBc()[i] * bc;
                stampCurrent(rhs, intrinsic[i], -1, new Complex(offset, 0));
            }
        }
    }

    private record LinearBipolar(BipolarDevice device, BipolarJunction.Point point) {
        void stamp(Complex[][] matrix, Complex frequency) {
            device.stampSeries(matrix);
            device.stampJunction(matrix, point, frequency);
        }
    }

    /** A behavioural source before its probes resolve, which needs every branch current row. */
    private record PendingSource(int branch, SpiceValue.Behaviour law, long sheet, List<String> path) {
        BehaviouralDevice resolve(List<Branch> branches, CircuitTopology topology, Map<Integer, Node> groupNodes) {
            var probes = law.probes();
            String name = branches.get(branch).instance;
            int[] plus = new int[probes.size()], minus = new int[probes.size()];
            for (int k = 0; k < plus.length; k++) {
                var probe = probes.get(k);
                plus[k] = probe.current() ? currentRow(branches, name, probe.first()) : row(topology, groupNodes, name, probe.first());
                minus[k] = probe.second() == null ? -1 : row(topology, groupNodes, name, probe.second());
            }
            return new BehaviouralDevice(branch, law, plus, minus);
        }

        private int row(CircuitTopology topology, Map<Integer, Node> groupNodes, String name, String label) {
            if (label.equals("0") || label.equalsIgnoreCase("gnd")) return -1;
            var group = topology.spiceLabelGroup("LK", sheet, label);
            Node node = group == null ? null : groupNodes.get(group.id());
            if (node == null) throw new IllegalArgumentException("The behavioural source " + name + " references the unknown node " + label + ".");
            return node.row;
        }

        private int currentRow(List<Branch> branches, String name, String source) {
            String instance = path.isEmpty() ? source : String.join(":", path) + ":" + source;
            for (var candidate : branches) {
                if (candidate.instance.equalsIgnoreCase(instance) && candidate.currentRow >= 0) return candidate.currentRow;
            }
            throw new IllegalArgumentException("The behavioural source " + name + " reads the current of " + instance
                    + ", which has no branch current.");
        }
    }

    /** A behavioural source (ADR-0033). Probe k reads row {@code plus[k]} minus row {@code minus[k]}; -1 is ground. */
    private record BehaviouralDevice(int branch, SpiceValue.Behaviour law, int[] plus, int[] minus) {
        SpiceValue.Behaviour.Point at(Complex[] bias) {
            double[] values = new double[plus.length];
            for (int k = 0; k < values.length; k++) values[k] = AcExecutionPlan.at(bias, plus[k]).real() - AcExecutionPlan.at(bias, minus[k]).real();
            return law.at(values);
        }

        /** Stamps the tangent at the bias: the gradient and the constant of {@code f(u) ~ J u + c}. */
        void stampBias(Complex[][] matrix, Complex[] rhs, Branch source, Complex[] bias) {
            var point = at(bias);
            stampSlopes(matrix, source, plus, minus, point.gradient());
            double constant = point.value();
            for (int k = 0; k < plus.length; k++) {
                constant -= point.gradient()[k] * (AcExecutionPlan.at(bias, plus[k]).real() - AcExecutionPlan.at(bias, minus[k]).real());
            }
            if (source.currentRow >= 0) rhs[source.currentRow] = rhs[source.currentRow].add(new Complex(constant, 0));
            else stampCurrent(rhs, source.positive, source.negative, new Complex(constant, 0));
        }
    }

    private static void stampSlopes(Complex[][] matrix, Branch source, int[] plus, int[] minus, double[] slopes) {
        for (int k = 0; k < slopes.length; k++) {
            for (int side = 0; side < 2; side++) {
                int column = side == 0 ? plus[k] : minus[k];
                var slope = new Complex(side == 0 ? slopes[k] : -slopes[k], 0);
                if (source.currentRow >= 0) {
                    add(matrix, source.currentRow, column, slope.scale(-1));
                } else {
                    add(matrix, source.positive, column, slope);
                    add(matrix, source.negative, column, slope.scale(-1));
                }
            }
        }
    }

    /** A behavioural source linearised at the DC operating point: real controlled-source coefficients. */
    private record LinearBehavioural(BehaviouralDevice device, Branch source, double[] slopes) {
        void stamp(Complex[][] matrix) { stampSlopes(matrix, source, device.plus, device.minus, slopes); }

        Complex current(Complex[] x) {
            Complex value = Complex.ZERO;
            for (int k = 0; k < slopes.length; k++) {
                value = value.add(at(x, device.plus[k]).subtract(at(x, device.minus[k])).scale(slopes[k]));
            }
            return value;
        }
    }

    public record DiodeOperatingPoint(String instance, boolean forward, double voltageV,
                                      double currentA, double conductanceS) { }

    private final double[] frequencies;
    private final List<Node> nodes;
    private final List<Branch> branches;
    private final int size;
    private final List<LinearBipolar> bipolarStamps;
    private final List<LinearBehavioural> behaviouralStamps;
    private final AcHierarchyTraces traceBindings;
    private final boolean hierarchical;
    private final List<DiodeOperatingPoint> diodeOperatingPoints;

    public AcExecutionPlan(CircuitModel model) {
        var settings = model.getAcSettings();
        hierarchical = model.getHierarchy() != null;
        frequencies = settings.sweep().frequencies(hierarchical);
        Map<String, String> rootParameters = settings.parameterMap();
        rootParameters.values().forEach(expression -> SpiceValue.evaluate(expression, rootParameters));
        var topology = CircuitTopology.resolve(model);
        var hierarchy = new HierarchyIndex(model);
        AcOriginMembership.validate(model, hierarchy);
        var scopes = hierarchy.parameterScopes(rootParameters);
        traceBindings = new AcHierarchyTraces(topology, hierarchy);
        topology.validatePhysicalConnections();
        var active = new HashSet<>(topology.activeComponents());
        if (active.stream().anyMatch(identity -> !identity.domain().equals("LK"))) {
            throw new IllegalArgumentException("AC supports electrical components only.");
        }
        var groupNodes = new LinkedHashMap<Integer, Node>();
        int voltageRows = 0;
        for (var group : topology.groups().values().stream().sorted(Comparator.comparingInt(CircuitTopology.Group::id)).toList()) {
            if (!group.domain().equals("LK") || group.terminals().stream().noneMatch(terminal -> active.contains(terminal.component()))) continue;
            int row = group.canonicalLabel().equals("0") ? -1 : voltageRows++;
            var aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            // Anonymous canonical labels generated by topology are display identifiers.
            if (!group.canonicalLabel().isEmpty()) aliases.add(group.canonicalLabel());
            groupNodes.put(group.id(), new Node(group, row, new ArrayList<>(), aliases));
        }
        if (groupNodes.values().stream().noneMatch(node -> node.row == -1)) {
            throw new IllegalArgumentException("Connect the AC circuit to ground.");
        }
        var seen = new HashSet<List<String>>();
        var prepared = new ArrayList<Branch>();
        var diodes = new LinkedHashMap<Integer, AcComponent.EvaluatedDiode>();
        var exponentialDiodes = new LinkedHashMap<Integer, ShockleyDiode>();
        var inductors = new HashSet<Integer>();
        var bipolars = new ArrayList<BipolarDevice>();
        var pendingSources = new ArrayList<PendingSource>();
        boolean frequencyDependentGain = false;
        int nextCurrent = voltageRows;
        for (var component : model.getCircuitComponents()) {
            var componentIdentity = topology.identity(component);
            if (!active.contains(componentIdentity)) continue;
            long sheet = HierarchyIndex.parent(component.getSavedBlock());
            var path = hierarchy.path(sheet);
            Map<String, String> parameters = scopes.getOrDefault(sheet, rootParameters);
            SpicePorts type = SpicePorts.find("LK", component.getType());
            if (type != SpicePorts.RES && type != SpicePorts.CAP && type != SpicePorts.IND
                    && type != SpicePorts.VOLTAGE && type != SpicePorts.CURRENT && type != SpicePorts.E && type != SpicePorts.G
                    && type != SpicePorts.DIODE && type != SpicePorts.BJT) {
                throw new IllegalArgumentException("Unsupported AC component: " + component.getName());
            }
            AcComponent owned = component.getAcComponent();
            if (owned == null) owned = legacy(component, type);
            String instance = path.isEmpty() ? owned.instance() : String.join(":", path) + ":" + owned.instance();
            var identityPath = new ArrayList<>(path);
            identityPath.add(owned.instance());
            if (!seen.add(identityPath.stream().map(segment -> segment.toLowerCase(Locale.ROOT)).toList())) {
                throw new IllegalArgumentException("Duplicate AC instance: " + owned.instance());
            }
            int[] ports = new int[type.ports().size()];
            var terminalIds = new CircuitTopology.TerminalIdentity[ports.length];
            String memberKey = HierarchyIndex.key(component.getSavedBlock());
            Long identifier = AcOriginMembership.nativeIdentifier(component);
            if (memberKey == null && identifier != null) memberKey = "native:" + identifier;
            String definitionId = sheet == 0 ? "" : hierarchy.sheet(sheet).definition().id();
            if (ports.length != owned.spiceOrders().size()) throw wrongLaw(owned);
            for (var port : type.ports()) {
                var group = topology.group(component, port.side(), port.port());
                Node node = group == null ? null : groupNodes.get(group.id());
                if (node == null) throw new IllegalArgumentException("Missing AC terminal on " + owned.instance());
                int order = port.order();
                ports[order - 1] = node.row;
                node.members.add(new AcResult.Member(instance, owned.spiceOrders().get(order - 1)));
                var terminal = new CircuitTopology.TerminalIdentity(componentIdentity, port.side(), port.port());
                terminalIds[order - 1] = terminal;
                traceBindings.addPrimitive(terminal, new AcResult.SourceMember(path, memberKey, owned.instance(),
                        owned.spiceOrders().get(order - 1), AcResult.TerminalKind.PRIMITIVE,
                        identifier, definitionId, type.name()));
                if (owned.aliases().size() >= order && !owned.aliases().get(order - 1).isEmpty()) {
                    node.aliases.add(owned.aliases().get(order - 1));
                }
            }
            switch (owned.law()) {
                case AcComponent.Passive law -> {
                    if (type != SpicePorts.RES && type != SpicePorts.CAP) throw wrongLaw(owned);
                    double value = SpiceValue.requirePositivePassive(
                            SpiceValue.evaluate(law.expression(), parameters), owned.instance());
                    if (type == SpicePorts.CAP) {
                        prepared.add(new Branch(instance, ports[0], ports[1], -1, -1,
                                nextCurrent++, null, value, null, Complex.ZERO, 0));
                    } else {
                        prepared.add(new Branch(instance, ports[0], ports[1], -1, -1,
                                -1, s -> new Complex(1 / value, 0), 0, null, Complex.ZERO, 0));
                    }
                }
                case AcComponent.Voltage law -> {
                    if (type != SpicePorts.VOLTAGE) throw wrongLaw(owned);
                    double dc = SpiceValue.evaluate(law.dc(), parameters);
                    double magnitude = SpiceValue.evaluate(law.magnitude(), parameters);
                    double phase = SpiceValue.evaluate(law.phaseDegrees(), parameters);
                    if (magnitude < 0) throw new IllegalArgumentException("Require a nonnegative AC source magnitude.");
                    Complex excitation = Complex.polar(magnitude, Math.toRadians(phase));
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1,
                            nextCurrent++, null, 0, null, excitation, dc));
                }
                case AcComponent.Inductance law -> {
                    if (type != SpicePorts.IND) throw wrongLaw(owned);
                    var value = law.evaluate(parameters);
                    SpiceValue.Transfer admittance = s -> Complex.ONE.divide(
                            s.scale(value.henries()).add(new Complex(value.ohms(), 0)));
                    if (value.ohms() == 0) inductors.add(prepared.size());
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1, -1, admittance, 0, null, Complex.ZERO, 0));
                }
                case AcComponent.Current law -> {
                    if (type != SpicePorts.CURRENT) throw wrongLaw(owned);
                    double dc = SpiceValue.evaluate(law.dc(), parameters);
                    double magnitude = SpiceValue.evaluate(law.magnitude(), parameters);
                    double phase = SpiceValue.evaluate(law.phaseDegrees(), parameters);
                    if (magnitude < 0) throw new IllegalArgumentException("Require a nonnegative AC source magnitude.");
                    Complex excitation = Complex.polar(magnitude, Math.toRadians(phase));
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1, -1, null, 0, null, excitation, dc));
                }
                case AcComponent.Transconductance law -> {
                    if (type != SpicePorts.G) throw wrongLaw(owned);
                    var transconductance = new Complex(SpiceValue.evaluate(law.expression(), parameters), 0);
                    prepared.add(new Branch(instance, ports[0], ports[1], ports[2], ports[3],
                            -1, null, 0, ignored -> transconductance, Complex.ZERO, 0));
                }
                case AcComponent.IdealDiode law -> {
                    if (type != SpicePorts.DIODE) throw wrongLaw(owned);
                    if (law.offResistance().isBlank() && (model.getAcProvenance() == null
                            || model.getAcProvenance().origin() != law.origin())) {
                        throw new IllegalArgumentException("Require the selected LTspice Gmin for " + instance + ".");
                    }
                    diodes.put(prepared.size(), law.evaluate(parameters));
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1,
                            -1, null, 0, null, Complex.ZERO, 0));
                }
                case AcComponent.ExponentialDiode law -> {
                    if (type != SpicePorts.DIODE || law.transientLaw() || model.getAcProvenance() != null
                            && model.getAcProvenance().origin().tested()) throw wrongLaw(owned);
                    exponentialDiodes.put(prepared.size(), law.evaluate(parameters));
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1,
                            -1, null, 0, null, Complex.ZERO, 0));
                }
                case AcComponent.Mosfet law -> throw wrongLaw(owned);
                case AcComponent.Behavioural law -> {
                    if (type != (law.current() ? SpicePorts.CURRENT : SpicePorts.VOLTAGE)) throw wrongLaw(owned);
                    pendingSources.add(new PendingSource(prepared.size(), law.compile(parameters), sheet, path));
                    prepared.add(new Branch(instance, ports[0], ports[1], -1, -1, law.current() ? -1 : nextCurrent++,
                            null, 0, null, Complex.ZERO, 0));
                }
                case AcComponent.Bipolar law -> {
                    if (type != SpicePorts.BJT || model.getAcProvenance() != null
                            && model.getAcProvenance().origin().tested()) throw wrongLaw(owned);
                    var device = BipolarJunction.from(law, parameters);
                    double[] resistance = device.seriesResistance();
                    int[] internal = ports.clone();
                    for (int i = 0; i < internal.length; i++) {
                        if (resistance[i] > 0) internal[i] = nextCurrent++;
                    }
                    var members = traceBindings.sourceMembers(Arrays.asList(terminalIds));
                    bipolars.add(new BipolarDevice(ports, internal, resistance, device, members));
                    continue;
                }
                case AcComponent.Controlled law -> {
                    if (type != SpicePorts.E) throw wrongLaw(owned);
                    SpiceValue.Transfer gain;
                    if (law.laplace()) {
                        frequencyDependentGain = true;
                        gain = SpiceValue.transfer(law.expression(), parameters);
                        gain.evaluate(new Complex(0, 2 * Math.PI * frequencies[0]));
                    } else {
                        double value = SpiceValue.evaluate(law.expression(), parameters);
                        gain = s -> new Complex(value, 0);
                    }
                    prepared.add(new Branch(instance, ports[0], ports[1], ports[2], ports[3],
                            nextCurrent++, null, 0, gain, Complex.ZERO, 0));
                }
            }
            traceBindings.addBranch(terminalIds[0], terminalIds[1]);
        }
        if (hierarchical) traceBindings.resolve();
        if (prepared.isEmpty() && bipolars.isEmpty()) throw new IllegalArgumentException("Add an AC circuit component.");
        var sources = new ArrayList<BehaviouralDevice>();
        for (var pending : pendingSources) sources.add(pending.resolve(prepared, topology, groupNodes));
        if ((!diodes.isEmpty() || !exponentialDiodes.isEmpty() || !bipolars.isEmpty() || !sources.isEmpty())
                && (!inductors.isEmpty() || frequencyDependentGain)) {
            throw new IllegalArgumentException("Nonlinear DC bias requires static gains and nonzero inductor series resistance.");
        }
        if (!exponentialDiodes.isEmpty() && !diodes.isEmpty()) {
            throw new IllegalArgumentException("Use one diode law in an AC bias solve.");
        }
        if (!sources.isEmpty() && !diodes.isEmpty()) {
            throw new IllegalArgumentException("Use exponential diodes with AC behavioural sources.");
        }
        Complex[] bias = null;
        if (bipolars.isEmpty()) {
            bipolarStamps = List.of();
            if (sources.isEmpty()) {
                diodeOperatingPoints = exponentialDiodes.isEmpty()
                        ? (diodes.isEmpty() ? List.of() : linearizeDiodes(prepared, diodes, nextCurrent))
                        : commitDiodeBias(prepared, exponentialDiodes,
                                solveExponentialBias(prepared, exponentialDiodes, sources, nextCurrent));
            } else {
                bias = solveExponentialBias(prepared, exponentialDiodes, sources, nextCurrent);
                diodeOperatingPoints = commitDiodeBias(prepared, exponentialDiodes, bias);
            }
        } else {
            if (!diodes.isEmpty()) throw new IllegalArgumentException("Use exponential diodes with AC bipolar transistors.");
            Complex[] solved = solveBipolarBias(prepared, exponentialDiodes, bipolars, sources, nextCurrent);
            bias = solved;
            bipolarStamps = bipolars.stream().map(device -> new LinearBipolar(device, device.at(solved))).toList();
            diodeOperatingPoints = commitDiodeBias(prepared, exponentialDiodes, bias);
        }
        var linearSources = new ArrayList<LinearBehavioural>();
        for (var source : sources) linearSources.add(new LinearBehavioural(source, prepared.get(source.branch), source.at(bias).gradient()));
        behaviouralStamps = List.copyOf(linearSources);
        // A feedback VCCS can constrain a node without a galvanic path. Use matrix rank for this circuit.
        if (bipolars.isEmpty() && sources.isEmpty() && prepared.stream().noneMatch(branch -> branch.currentRow < 0 && branch.gain != null)) {
            validateGroundPaths(groupNodes.values().stream().toList(), prepared, voltageRows);
        }
        nodes = List.copyOf(groupNodes.values());
        branches = List.copyOf(prepared);
        size = nextCurrent;
    }

    public List<DiodeOperatingPoint> diodeOperatingPoints() { return diodeOperatingPoints; }

    private static List<DiodeOperatingPoint> linearizeDiodes(List<Branch> branches,
            Map<Integer, AcComponent.EvaluatedDiode> diodes, int size) {
        if (diodes.size() > 2) {
            throw new IllegalArgumentException("Use at most two ideal diodes in the bounded AC operating point.");
        }
        var indices = List.copyOf(diodes.keySet());
        List<DiodeOperatingPoint> accepted = null;
        boolean closeToThreshold = false;
        for (int mode = 0; mode < 1 << indices.size(); mode++) {
            Complex[][] matrix = new Complex[size][size];
            for (var row : matrix) Arrays.fill(row, Complex.ZERO);
            Complex[] rhs = new Complex[size];
            Arrays.fill(rhs, Complex.ZERO);
            for (int i = 0; i < branches.size(); i++) {
                var branch = branches.get(i);
                var diode = diodes.get(i);
                if (diode != null) {
                    boolean forward = (mode & 1 << indices.indexOf(i)) != 0;
                    double conductance = diode.conductance(forward);
                    double offset = forward
                            ? (diode.conductance(false) - conductance) * diode.forwardVoltage() : 0;
                    stamp(matrix, rhs, branch.withConductance(conductance), Complex.ZERO, 0);
                    stampCurrent(rhs, branch.positive, branch.negative, new Complex(offset, 0));
                } else if (branch.capacitance > 0) {
                    // A capacitor is open at DC. Its retained current row enforces I = 0.
                    add(matrix, branch.currentRow, branch.currentRow, Complex.ONE);
                } else {
                    stamp(matrix, rhs, branch.withExcitation(branch.dc), Complex.ZERO, 0);
                }
            }
            var bias = solve(matrix, rhs);
            if (residual(matrix, rhs, bias) > 1e-12) {
                throw new IllegalArgumentException("The DC diode bias backward residual exceeds 1e-12.");
            }
            var defect = rhs.clone();
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    defect[row] = defect[row].subtract(matrix[row][col].multiply(bias[col]));
                }
            }
            var correction = solve(matrix, defect);
            var candidate = new ArrayList<DiodeOperatingPoint>();
            boolean consistent = true, near = false;
            for (int bit = 0; bit < indices.size(); bit++) {
                int i = indices.get(bit);
                var branch = branches.get(i);
                var diode = diodes.get(i);
                boolean forward = (mode & 1 << bit) != 0;
                double va = at(bias, branch.positive).real(), vk = at(bias, branch.negative).real();
                double voltage = va - vk;
                double threshold = diode.forwardVoltage();
                double uncertainty = voltageUncertainty(va, vk, threshold)
                        + 8 * (at(correction, branch.positive).abs() + at(correction, branch.negative).abs());
                double distance = forward ? voltage - threshold : threshold - voltage;
                consistent &= distance >= -uncertainty;
                near |= Math.abs(distance) <= uncertainty;
                double slope = diode.conductance(forward);
                double current = forward ? Math.fma(slope, voltage - threshold, diode.conductance(false) * threshold)
                        : slope * voltage;
                checkFinite(new Complex(current, 0));
                candidate.add(new DiodeOperatingPoint(branch.instance, forward, voltage, current, slope));
            }
            if (!consistent) continue;
            if (near) {
                closeToThreshold = true;
                continue;
            }
            verifyBiasLaws(branches, indices, candidate, bias, correction);
            if (accepted != null) throw new IllegalArgumentException("The DC diode operating point is ambiguous.");
            accepted = List.copyOf(candidate);
        }
        if (closeToThreshold) throw new IllegalArgumentException("The DC diode bias is at a nonsmooth threshold.");
        if (accepted == null) throw new IllegalArgumentException("No consistent DC diode operating point.");
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            branches.set(index, branches.get(index).withConductance(accepted.get(i).conductanceS));
        }
        return accepted;
    }

    private record BiasEquations(Complex[][] matrix, Complex[] rhs) {
        double residualAt(Complex[] bias, Complex[] sourceRhs) {
            double maximum = 0;
            for (int row = 0; row < rhs.length; row++) {
                Complex error = rhs[row];
                for (int column = 0; column < bias.length; column++) {
                    error = error.subtract(matrix[row][column].multiply(bias[column]));
                }
                // Use fixed source magnitudes. A large trial tangent must not make a bad trial look better.
                maximum = Math.max(maximum, error.abs() / Math.max(1, sourceRhs[row].abs()));
            }
            return maximum;
        }
    }

    /** Newton bias of exponential diodes and behavioural sources (ADR-0033). */
    private static Complex[] solveExponentialBias(List<Branch> branches, Map<Integer, ShockleyDiode> diodes,
            List<BehaviouralDevice> sources, int size) {
        Complex[] bias = new Complex[size];
        Arrays.fill(bias, Complex.ZERO);
        BiasEquations fixed = linearBiasEquations(branches, diodes, size);
        BiasEquations equations = exponentialBiasEquations(branches, diodes, sources, fixed, bias);
        double error = equations.residualAt(bias, fixed.rhs);
        for (int iteration = 0; iteration <= 50; iteration++) {
            Complex[] next = solve(equations.matrix, equations.rhs);
            boolean diodeVoltagesConverged = true;
            for (int index : diodes.keySet()) {
                var branch = branches.get(index);
                double voltage = branchVoltage(bias, branch);
                double correction = Math.abs(branchVoltage(next, branch) - voltage);
                diodeVoltagesConverged &= correction <= 1e-10 + 1e-9 * Math.abs(voltage);
            }
            for (var source : sources) {
                for (int k = 0; k < source.plus.length; k++) {
                    double value = at(bias, source.plus[k]).real() - at(bias, source.minus[k]).real();
                    double correction = Math.abs(at(next, source.plus[k]).real() - at(next, source.minus[k]).real() - value);
                    diodeVoltagesConverged &= correction <= 1e-10 + 1e-9 * Math.abs(value);
                }
            }
            if (error <= 1e-12 && diodeVoltagesConverged) {
                return bias;
            }
            if (iteration == 50) break;
            double scale = 1;
            boolean accepted = false;
            for (int half = 0; half < 48; half++, scale /= 2) {
                var trial = new Complex[size];
                for (int row = 0; row < size; row++) {
                    trial[row] = bias[row].add(next[row].subtract(bias[row]).scale(scale));
                }
                BiasEquations trialEquations;
                try {
                    trialEquations = exponentialBiasEquations(branches, diodes, sources, fixed, trial);
                } catch (IllegalArgumentException outOfRange) {
                    continue;
                }
                double trialError = trialEquations.residualAt(trial, fixed.rhs);
                // A comparison that changes its result is a jump, not a descent direction: take the full step.
                if (trialError < error || half == 0 && !sameBranches(sources, bias, trial)) {
                    bias = trial;
                    equations = trialEquations;
                    error = trialError;
                    accepted = true;
                    break;
                }
            }
            if (!accepted) throw new IllegalArgumentException("The " + biasName(sources) + " DC bias cannot reduce its residual.");
        }
        throw new IllegalArgumentException("The " + biasName(sources) + " DC bias did not converge.");
    }

    private static String biasName(List<BehaviouralDevice> sources) {
        return sources.isEmpty() ? "exponential diode" : "nonlinear";
    }

    private static boolean sameBranches(List<BehaviouralDevice> sources, Complex[] bias, Complex[] trial) {
        for (var source : sources) {
            try {
                if (source.at(bias).branches() != source.at(trial).branches()) return false;
            } catch (IllegalArgumentException outOfRange) {
                return true;
            }
        }
        return true;
    }

    private static Complex[] solveBipolarBias(List<Branch> branches, Map<Integer, ShockleyDiode> diodes,
            List<BipolarDevice> bipolars, List<BehaviouralDevice> sources, int size) {
        double[] weights = new double[size];
        Arrays.fill(weights, 1e3);
        for (var branch : branches) {
            if (branch.currentRow >= 0) weights[branch.currentRow] = 1;
        }
        return BipolarBias.solve(size, weights, fraction -> {
            var fixed = linearBiasEquations(branches, diodes, size, fraction);
            bipolars.forEach(device -> device.stampSeries(fixed.matrix));
            return bias -> {
                var equations = exponentialBiasEquations(branches, diodes, sources, fixed, bias);
                bipolars.forEach(device -> device.stampBias(equations.matrix, equations.rhs, bias));
                return new BipolarBias.Equations(equations.matrix, equations.rhs);
            };
        });
    }

    private static List<DiodeOperatingPoint> commitDiodeBias(List<Branch> branches,
            Map<Integer, ShockleyDiode> diodes, Complex[] bias) {
        var points = new ArrayList<DiodeOperatingPoint>();
        for (var entry : diodes.entrySet()) {
            int index = entry.getKey();
            var branch = branches.get(index);
            double voltage = branchVoltage(bias, branch);
            var diode = entry.getValue().at(voltage);
            points.add(new DiodeOperatingPoint(branch.instance, voltage > 0, voltage,
                    diode.current(), diode.conductance()));
            branches.set(index, branch.withAdmittance(diode::admittance));
        }
        return List.copyOf(points);
    }

    private static BiasEquations linearBiasEquations(List<Branch> branches, Map<Integer, ShockleyDiode> diodes, int size) {
        return linearBiasEquations(branches, diodes, size, 1);
    }

    private static BiasEquations linearBiasEquations(List<Branch> branches, Map<Integer, ShockleyDiode> diodes,
            int size, double sourceScale) {
        Complex[][] matrix = new Complex[size][size];
        for (var row : matrix) Arrays.fill(row, Complex.ZERO);
        Complex[] rhs = new Complex[size];
        Arrays.fill(rhs, Complex.ZERO);
        for (int i = 0; i < branches.size(); i++) {
            if (diodes.containsKey(i)) continue;
            var branch = branches.get(i);
            if (branch.capacitance > 0) {
                add(matrix, branch.currentRow, branch.currentRow, Complex.ONE);
            } else {
                stamp(matrix, rhs, branch.withExcitation(sourceScale * branch.dc), Complex.ZERO, 0);
            }
        }
        return new BiasEquations(matrix, rhs);
    }

    private static BiasEquations exponentialBiasEquations(List<Branch> branches, Map<Integer, ShockleyDiode> diodes,
            List<BehaviouralDevice> sources, BiasEquations fixed, Complex[] bias) {
        var matrix = new Complex[fixed.matrix.length][];
        for (int row = 0; row < matrix.length; row++) matrix[row] = fixed.matrix[row].clone();
        var rhs = fixed.rhs.clone();
        for (var entry : diodes.entrySet()) {
            var branch = branches.get(entry.getKey());
            double voltage = branchVoltage(bias, branch);
            var point = entry.getValue().at(voltage);
            double conductance = point.conductance();
            double offset = Math.fma(-conductance, voltage, point.current());
            stamp(matrix, rhs, branch.withConductance(conductance), Complex.ZERO, 0);
            stampCurrent(rhs, branch.positive, branch.negative, new Complex(offset, 0));
        }
        for (var source : sources) source.stampBias(matrix, rhs, branches.get(source.branch), bias);
        return new BiasEquations(matrix, rhs);
    }

    private static double branchVoltage(Complex[] bias, Branch branch) {
        return at(bias, branch.positive).real() - at(bias, branch.negative).real();
    }

    private static double voltageUncertainty(double a, double b, double reference) {
        return 128 * Math.ulp(Math.max(1, Math.max(Math.abs(reference), Math.max(Math.abs(a), Math.abs(b)))));
    }

    private static void verifyBiasLaws(List<Branch> branches, List<Integer> indices,
            List<DiodeOperatingPoint> diodes, Complex[] bias, Complex[] correction) {
        double[] sums = new double[bias.length], bounds = new double[bias.length];
        for (int i = 0; i < branches.size(); i++) {
            var branch = branches.get(i);
            int diode = indices.indexOf(i);
            double vp = at(bias, branch.positive).real(), vn = at(bias, branch.negative).real();
            double error = voltageUncertainty(vp, vn, branch.dc)
                    + 8 * (at(correction, branch.positive).abs() + at(correction, branch.negative).abs());
            double current;
            if (diode >= 0) {
                var point = diodes.get(diode);
                current = point.currentA;
                error *= point.conductanceS;
            } else if (branch.capacitance > 0) {
                current = at(bias, branch.currentRow).real();
                if (current != 0) throw new IllegalArgumentException("The DC capacitor current is not zero.");
            } else {
                current = branchCurrent(bias, Complex.ZERO, branch.withExcitation(branch.dc)).real();
                if (branch.currentRow >= 0) {
                    double drive = branch.dc;
                    if (branch.gain != null) {
                        drive += branch.gain.evaluate(Complex.ZERO).real()
                                * (at(bias, branch.sensePositive).real() - at(bias, branch.senseNegative).real());
                    }
                    if (Math.abs((vp - vn) - drive) > error) {
                        throw new IllegalArgumentException("DC voltage equation failed on " + branch.instance + ".");
                    }
                    error = 128 * Math.ulp(Math.max(1, Math.abs(current))) + 8 * at(correction, branch.currentRow).abs();
                } else if (branch.admittance != null) {
                    error *= branch.admittance.evaluate(Complex.ZERO).abs();
                } else if (branch.gain != null) {
                    double cp = at(bias, branch.sensePositive).real(), cn = at(bias, branch.senseNegative).real();
                    error = branch.gain.evaluate(Complex.ZERO).abs() * (voltageUncertainty(cp, cn, 0)
                            + 8 * (at(correction, branch.sensePositive).abs() + at(correction, branch.senseNegative).abs()));
                }
            }
            checkFinite(new Complex(current, error));
            accumulateBiasCurrent(sums, bounds, branch.positive, current, error);
            accumulateBiasCurrent(sums, bounds, branch.negative, -current, error);
        }
        for (int i = 0; i < sums.length; i++) {
            if (Math.abs(sums[i]) > bounds[i]) {
                throw new IllegalArgumentException("DC diode bias failed current balance.");
            }
        }
    }

    private static void accumulateBiasCurrent(double[] sums, double[] bounds, int row, double current, double error) {
        if (row < 0) return;
        sums[row] += current;
        bounds[row] += error;
    }

    private static void validateGroundPaths(List<Node> nodes, List<Branch> branches, int voltageRows) {
        // Only the two current-carrying terminals connect a branch to ground.
        var neighbors = new ArrayList<List<Integer>>(voltageRows + 1);
        for (int i = 0; i <= voltageRows; i++) neighbors.add(new ArrayList<>());
        for (var branch : branches) {
            if (branch.currentRow < 0 && branch.admittance == null) continue;
            int positive = branch.positive + 1, negative = branch.negative + 1;
            neighbors.get(positive).add(negative);
            neighbors.get(negative).add(positive);
        }
        var pending = new ArrayDeque<Integer>();
        var reachable = new boolean[voltageRows + 1];
        pending.add(0);
        reachable[0] = true;
        while (!pending.isEmpty()) {
            for (int next : neighbors.get(pending.removeFirst())) {
                if (!reachable[next]) {
                    reachable[next] = true;
                    pending.add(next);
                }
            }
        }
        for (var node : nodes) {
            if (!reachable[node.row + 1]) {
                throw new IllegalArgumentException("Connect the floating AC group " + node.group.id() + " to ground.");
            }
        }
    }

    private static IllegalArgumentException wrongLaw(AcComponent component) {
        return new IllegalArgumentException("Invalid AC law for " + component.instance());
    }

    private static AcComponent legacy(CircuitModel.ComponentData component, SpicePorts type) {
        double[] values = component.getNumericParameters();
        AcComponent.Law law;
        if (type == SpicePorts.VOLTAGE && values.length > 1) {
            law = new AcComponent.Voltage(Double.toString(values[1]), "", "0", "0");
        } else if ((type == SpicePorts.RES || type == SpicePorts.CAP) && values.length > 0) {
            law = new AcComponent.Passive(Double.toString(values[0]));
        } else {
            throw new IllegalArgumentException("Missing AC law for " + component.getName());
        }
        return new AcComponent(component.getName(), List.of(), false, law);
    }

    public AcResult execute() {
        return execute(java.util.function.IntUnaryOperator.identity(), java.util.function.IntUnaryOperator.identity());
    }

    // A test can redirect a complete acquired producer while keeping the requested trace label.
    AcResult execute(java.util.function.IntUnaryOperator voltageProducer, java.util.function.IntUnaryOperator currentProducer) {
        checkCancellation();
        var voltage = samples(nodes.size());
        var current = samples(branches.size());
        var terminalCurrent = new LinkedHashMap<AcResult.SourceMember, List<Complex>>();
        if (hierarchical) {
            for (var bipolar : bipolarStamps) {
                for (var terminal : bipolar.device.terminals) terminalCurrent.put(terminal, new ArrayList<>());
            }
        }
        var residuals = new ArrayList<Double>();
        for (double frequency : frequencies) {
            checkCancellation();
            try {
                var s = new Complex(0, 2 * Math.PI * frequency);
                Complex[][] matrix = new Complex[size][size];
                for (var row : matrix) Arrays.fill(row, Complex.ZERO);
                Complex[] rhs = new Complex[size];
                Arrays.fill(rhs, Complex.ZERO);
                for (var branch : branches) stamp(matrix, rhs, branch, s, frequency);
                for (var bipolar : bipolarStamps) bipolar.stamp(matrix, s);
                for (var source : behaviouralStamps) source.stamp(matrix);
                Complex[] x = solve(matrix, rhs);
                double eta = residual(matrix, rhs, x);
                if (eta > 1e-12) throw new IllegalArgumentException("The AC backward residual exceeds 1e-12.");
                residuals.add(eta);
                for (int i = 0; i < nodes.size(); i++) voltage.get(i).add(at(x, nodes.get(i).row));
                for (int i = 0; i < branches.size(); i++) {
                    var branch = branches.get(i);
                    Complex value = behaviouralCurrent(i, x);
                    if (value == null) value = branchCurrent(x, s, branch);
                    checkFinite(value);
                    current.get(i).add(value);
                }
                if (hierarchical) {
                    for (var bipolar : bipolarStamps) {
                        for (int terminal = 0; terminal < 3; terminal++) {
                            var value = bipolar.device.terminalCurrent(bipolar.point, s, x, terminal);
                            checkFinite(value);
                            terminalCurrent.get(bipolar.device.terminals.get(terminal)).add(value);
                        }
                    }
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("AC at " + frequency + " Hz: " + error.getMessage(), error);
            }
        }
        checkCancellation();
        var voltages = new ArrayList<AcResult.VoltageTrace>();
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            if (node.members.isEmpty()) continue;
            var members = node.members.stream().sorted(Comparator.comparing(AcResult.Member::instance)
                    .thenComparingInt(AcResult.Member::spiceOrder)).toList();
            int acquiredIndex = voltageProducer.applyAsInt(i);
            var acquired = nodes.get(acquiredIndex);
            voltages.add(new AcResult.VoltageTrace("runtime-group-" + node.group.id(), node.row == -1, members,
                    List.copyOf(node.aliases), voltage.get(acquiredIndex), traceBindings.sourceMembers(acquired.group.terminals()),
                    acquired.group.canonicalLabel(), "runtime-group-" + acquired.group.id()));
        }
        var currents = new ArrayList<AcResult.CurrentTrace>();
        for (int i = 0; i < branches.size(); i++) {
            int acquiredIndex = currentProducer.applyAsInt(i);
            currents.add(new AcResult.CurrentTrace(branches.get(i).instance, current.get(acquiredIndex),
                    traceBindings.positive(acquiredIndex), traceBindings.negative(acquiredIndex),
                    branches.get(acquiredIndex).instance));
        }
        return new AcResult(Arrays.stream(frequencies).boxed().toList(), voltages, currents, residuals, size,
                hierarchical ? traceBindings.result(current, terminalCurrent, frequencies.length) : null);
    }

    /** @return the small-signal current of a behavioural current source, or null for any other branch */
    private Complex behaviouralCurrent(int branch, Complex[] x) {
        for (var source : behaviouralStamps) {
            if (source.device.branch == branch && source.source.currentRow < 0) return source.current(x);
        }
        return null;
    }

    private static void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("The AC analysis was stopped.");
    }

    private static List<List<Complex>> samples(int size) {
        var samples = new ArrayList<List<Complex>>(size);
        for (int i = 0; i < size; i++) samples.add(new ArrayList<>());
        return samples;
    }

    private static Complex branchCurrent(Complex[] x, Complex s, Branch branch) {
        if (branch.currentRow >= 0) return x[branch.currentRow];
        if (branch.gain != null) return branch.gain.evaluate(s).multiply(
                at(x, branch.sensePositive).subtract(at(x, branch.senseNegative)));
        if (branch.admittance != null) return branch.admittance.evaluate(s).multiply(
                at(x, branch.positive).subtract(at(x, branch.negative)));
        return branch.excitation;
    }

    private static void stamp(Complex[][] a, Complex[] b, Branch branch, Complex s, double frequency) {
        int p = branch.positive, n = branch.negative, i = branch.currentRow;
        if (i < 0 && branch.gain != null) {
            Complex gm = branch.gain.evaluate(s);
            add(a, p, branch.sensePositive, gm);
            add(a, p, branch.senseNegative, gm.scale(-1));
            add(a, n, branch.sensePositive, gm.scale(-1));
            add(a, n, branch.senseNegative, gm);
        } else if (i < 0 && branch.admittance == null) {
            stampCurrent(b, p, n, branch.excitation);
        } else if (i < 0) {
            Complex y = branch.admittance.evaluate(s);
            add(a, p, p, y);
            add(a, n, n, y);
            add(a, p, n, y.scale(-1));
            add(a, n, p, y.scale(-1));
        } else {
            add(a, p, i, Complex.ONE);
            add(a, n, i, Complex.ONE.scale(-1));
            add(a, i, p, Complex.ONE);
            add(a, i, n, Complex.ONE.scale(-1));
            if (branch.capacitance > 0) {
                // Vp - Vn = I / (j * omega * C). Keep I as a solved unknown.
                add(a, i, i, new Complex(0, capacitiveReactance(frequency, branch)));
            }
            if (branch.gain != null) {
                Complex h = branch.gain.evaluate(s);
                add(a, i, branch.sensePositive, h.scale(-1));
                add(a, i, branch.senseNegative, h);
            }
            b[i] = branch.excitation;
        }
    }

    private static void stampCurrent(Complex[] rhs, int positive, int negative, Complex excitation) {
        if (positive >= 0) rhs[positive] = rhs[positive].subtract(excitation);
        if (negative >= 0) rhs[negative] = rhs[negative].add(excitation);
    }

    private static double capacitiveReactance(double frequency, Branch branch) {
        // Scale before multiplication so that even a subnormal reciprocal can be represented.
        int frequencyExponent = Math.getExponent(frequency);
        int capacitanceExponent = Math.getExponent(branch.capacitance);
        double scaledFrequency = Math.scalb(frequency, -frequencyExponent);
        double scaledCapacitance = Math.scalb(branch.capacitance, -capacitanceExponent);
        double reactance = Math.scalb(1 / (2 * Math.PI * scaledFrequency * scaledCapacitance),
                -frequencyExponent - capacitanceExponent);
        if (!Double.isFinite(reactance) || reactance == 0) {
            throw new IllegalArgumentException("The AC capacitor reactance is outside numeric range on " + branch.instance);
        }
        return reactance;
    }

    private static void add(Complex[][] a, int row, int col, Complex value) {
        if (row >= 0 && col >= 0) a[row][col] = a[row][col].add(value);
    }

    private static Complex at(Complex[] vector, int row) { return row < 0 ? Complex.ZERO : vector[row]; }

    private static Complex[] solve(Complex[][] a, Complex[] b) {
        int n = b.length;
        double[][] block = new double[2 * n][2 * n];
        double[] realRhs = new double[2 * n];
        for (int i = 0; i < n; i++) {
            checkFinite(b[i]);
            realRhs[i] = b[i].real();
            realRhs[i + n] = b[i].imaginary();
            for (int j = 0; j < n; j++) {
                checkFinite(a[i][j]);
                block[i][j] = block[i + n][j + n] = a[i][j].real();
                block[i + n][j] = a[i][j].imaginary();
                block[i][j + n] = -a[i][j].imaginary();
            }
        }
        EquilibratedRank.requireFullRank(block);
        checkCancellation();
        var factor = new LUDecomposition(new Matrix(block));
        if (!factor.isNonsingular()) throw new IllegalArgumentException("The AC network is singular.");
        for (var triangular : List.of(factor.getL(), factor.getU())) {
            for (var row : triangular.getArray()) {
                for (double entry : row) {
                    if (!Double.isFinite(entry)) throw new IllegalArgumentException("The AC factorization is nonfinite.");
                }
            }
        }
        double[] solved = factor.solve(new Matrix(realRhs, 2 * n)).getColumnPackedCopy();
        Complex[] x = new Complex[n];
        for (int i = 0; i < n; i++) {
            x[i] = new Complex(solved[i], solved[i + n]);
            checkFinite(x[i]);
        }
        return x;
    }

    static double residual(Complex[][] a, Complex[] b, Complex[] x) {
        double aNorm = 0, bNorm = 0, xNorm = 0, error = 0;
        for (int i = 0; i < b.length; i++) {
            double rowSum = 0;
            Complex value = Complex.ZERO;
            for (int j = 0; j < b.length; j++) {
                rowSum += a[i][j].abs();
                value = value.add(a[i][j].multiply(x[j]));
            }
            aNorm = Math.max(aNorm, rowSum);
            bNorm = Math.max(bNorm, b[i].abs());
            xNorm = Math.max(xNorm, x[i].abs());
            error = Math.max(error, value.subtract(b[i]).abs());
        }
        double denominator = aNorm * xNorm + bNorm;
        if (!Double.isFinite(denominator) || !Double.isFinite(error) || denominator == 0 && error != 0) {
            throw new IllegalArgumentException("The AC residual is invalid.");
        }
        return denominator == 0 ? 0 : error / denominator;
    }

    private static void checkFinite(Complex value) {
        if (!value.finite()) throw new IllegalArgumentException("The AC calculation is nonfinite.");
    }
}
