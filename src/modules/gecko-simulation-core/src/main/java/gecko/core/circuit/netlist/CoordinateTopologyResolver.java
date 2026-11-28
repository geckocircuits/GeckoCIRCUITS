/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.netlist;

import gecko.core.io.CircuitModel;
import java.util.*;

/** Resolves missing electrical labels using the desktop's grid-point and endpoint junction rules. */
final class CoordinateTopologyResolver {
    private CoordinateTopologyResolver() {
    }

    private record Position(int x, int y) {
    }

    static String[][] resolve(List<CircuitModel.ComponentData> components,
                              List<CircuitModel.ConnectionData> connections, String[][] labels) {
        List<CircuitModel.ConnectionData> wires = connections.stream()
                .filter(connection -> "LK".equals(connection.getType())).toList();
        int terminalCount = 2 * components.size();
        int size = terminalCount + wires.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
        Map<Position, List<Integer>> pointsToWires = new HashMap<>();
        Map<Position, List<Integer>> terminals = new HashMap<>();
        Map<String, Integer> named = new HashMap<>();
        String[] names = new String[size];
        for (int i = 0; i < components.size(); i++) {
            for (int end = 0; end < 2; end++) {
                int terminal = 2 * i + end;
                Position point = terminalPosition(components.get(i), end);
                terminals.computeIfAbsent(point, key -> new ArrayList<>()).add(terminal);
                names[terminal] = labels[i][end];
                connectLabel(parent, named, names[terminal], terminal);
            }
        }
        for (int i = 0; i < wires.size(); i++) {
            CircuitModel.ConnectionData wire = wires.get(i);
            int node = terminalCount + i;
            int[][] points = wire.getPoints();
            if (points == null || points.length < 2) {
                throw new IllegalArgumentException("Electrical wire requires at least two grid points");
            }
            for (int[] point : points) {
                if (point == null || point.length != 2) {
                    throw new IllegalArgumentException("Electrical wire requires x/y grid points");
                }
                pointsToWires.computeIfAbsent(new Position(point[0], point[1]),
                        key -> new ArrayList<>()).add(node);
            }
            names[node] = wire.getLabel() == null ? "" : NetlistBuilder.normalizeLabel(wire.getLabel());
            connectLabel(parent, named, names[node], node);
        }
        // Interior crossings alone do not join wires. A wire endpoint or component terminal is required.
        for (int i = 0; i < wires.size(); i++) {
            int[][] points = wires.get(i).getPoints();
            for (int end : new int[]{0, points.length - 1}) {
                Position point = new Position(points[end][0], points[end][1]);
                for (int other : pointsToWires.get(point)) {
                    union(parent, terminalCount + i, other);
                }
            }
        }
        for (var entry : terminals.entrySet()) {
            List<Integer> atPoint = entry.getValue();
            List<Integer> touchingWires = pointsToWires.getOrDefault(entry.getKey(), List.of());
            for (int terminal : atPoint) {
                union(parent, terminal, atPoint.get(0));
                for (int wire : touchingWires) {
                    union(parent, terminal, wire);
                }
                if (names[terminal].isEmpty() && atPoint.size() == 1 && touchingWires.isEmpty()) {
                    throw new IllegalArgumentException("Component '" + components.get(terminal / 2).getName()
                            + "' has an unconnected " + (terminal % 2 == 0 ? "input" : "output") + " terminal");
                }
            }
        }
        Map<Integer, String> rootLabels = new HashMap<>();
        Set<String> used = new HashSet<>(named.keySet());
        for (int i = 0; i < size; i++) {
            if (!names[i].isEmpty()) {
                int root = find(parent, i);
                if (names[i].equals("0") || !rootLabels.containsKey(root)) {
                    rootLabels.put(root, names[i]);
                }
            }
        }
        String[][] resolved = new String[components.size()][2];
        for (int i = 0; i < terminalCount; i++) {
            int root = find(parent, i);
            if (!rootLabels.containsKey(root)) {
                String generated = "@wire:" + root;
                while (!used.add(generated)) {
                    generated += ":";
                }
                rootLabels.put(root, generated);
            }
            resolved[i / 2][i % 2] = rootLabels.get(root);
        }
        return resolved;
    }

    private static Position terminalPosition(CircuitModel.ComponentData component, int end) {
        int distance = end == 0 ? -2 : 2;
        int x = component.getPosition()[0];
        int y = component.getPosition()[1];
        // Codes and offsets match desktop ComponentDirection and TerminalTwoPortComponent.
        return switch (component.getOrientation()) {
            case 501 -> new Position(x, y - distance);
            case 502 -> new Position(x + distance, y);
            case 504 -> new Position(x - distance, y);
            default -> new Position(x, y + distance);
        };
    }

    private static void connectLabel(int[] parent, Map<String, Integer> labels, String label, int node) {
        if (!label.isEmpty()) {
            Integer previous = labels.putIfAbsent(label, node);
            if (previous != null) {
                union(parent, node, previous);
            }
        }
    }

    private static int find(int[] parent, int node) {
        while (parent[node] != node) {
            parent[node] = parent[parent[node]];
            node = parent[node];
        }
        return node;
    }

    private static void union(int[] parent, int first, int second) {
        parent[find(parent, first)] = find(parent, second);
    }
}
