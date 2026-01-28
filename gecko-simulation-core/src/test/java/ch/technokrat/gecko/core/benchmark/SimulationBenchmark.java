/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.core.benchmark;

import ch.technokrat.gecko.core.allg.SolverType;
import ch.technokrat.gecko.core.simulation.HeadlessSimulationEngine;
import ch.technokrat.gecko.core.simulation.SimulationConfig;
import ch.technokrat.gecko.core.simulation.SimulationResult;
import org.junit.jupiter.api.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for the HeadlessSimulationEngine.
 *
 * <p>These tests measure:</p>
 * <ul>
 *   <li>Simulation execution time vs. circuit complexity</li>
 *   <li>Memory usage during simulation</li>
 *   <li>Throughput for concurrent simulations</li>
 * </ul>
 *
 * <p>Run with: mvn test -Dtest=SimulationBenchmark</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimulationBenchmark {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    private HeadlessSimulationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HeadlessSimulationEngine();
        // Force garbage collection before each test
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== Execution Time Benchmarks ====================

    @Test
    @Order(1)
    @DisplayName("Benchmark: Short simulation (1ms)")
    void benchmark_shortSimulation() {
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_short.ipes")
                .simulationDuration(1e-3)
                .stepWidth(1e-6)
                .solverType(SolverType.SOLVER_BE)
                .build();

        BenchmarkResult result = runBenchmark(config, 5);

        System.out.println("\n=== Short Simulation (1ms, dt=1us) ===");
        System.out.printf("Expected steps: %d%n", (int) (1e-3 / 1e-6));
        printBenchmarkResult(result);

        assertTrue(result.averageTimeMs < 1000, "Short simulation should complete in under 1 second");
    }

    @Test
    @Order(2)
    @DisplayName("Benchmark: Medium simulation (10ms)")
    void benchmark_mediumSimulation() {
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_medium.ipes")
                .simulationDuration(10e-3)
                .stepWidth(1e-6)
                .solverType(SolverType.SOLVER_BE)
                .build();

        BenchmarkResult result = runBenchmark(config, 3);

        System.out.println("\n=== Medium Simulation (10ms, dt=1us) ===");
        System.out.printf("Expected steps: %d%n", (int) (10e-3 / 1e-6));
        printBenchmarkResult(result);

        assertTrue(result.averageTimeMs < 5000, "Medium simulation should complete in under 5 seconds");
    }

    @Test
    @Order(3)
    @DisplayName("Benchmark: Long simulation (100ms)")
    void benchmark_longSimulation() {
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_long.ipes")
                .simulationDuration(100e-3)
                .stepWidth(10e-6)  // Larger time step for longer simulations
                .solverType(SolverType.SOLVER_BE)
                .build();

        BenchmarkResult result = runBenchmark(config, 2);

        System.out.println("\n=== Long Simulation (100ms, dt=10us) ===");
        System.out.printf("Expected steps: %d%n", (int) (100e-3 / 10e-6));
        printBenchmarkResult(result);

        assertTrue(result.averageTimeMs < 30000, "Long simulation should complete in under 30 seconds");
    }

    // ==================== Time Step Sensitivity Benchmarks ====================

    @Test
    @Order(4)
    @DisplayName("Benchmark: Time step sensitivity")
    void benchmark_timeStepSensitivity() {
        System.out.println("\n=== Time Step Sensitivity ===");
        double duration = 10e-3;
        double[] timeSteps = {10e-6, 5e-6, 2e-6, 1e-6};

        for (double dt : timeSteps) {
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile("benchmark_dt.ipes")
                    .simulationDuration(duration)
                    .stepWidth(dt)
                    .solverType(SolverType.SOLVER_BE)
                    .build();

            BenchmarkResult result = runBenchmark(config, 3);
            int expectedSteps = (int) (duration / dt);

            System.out.printf("dt=%s: steps=%d, time=%dms, throughput=%.1f steps/ms%n",
                    formatTime(dt), expectedSteps, result.averageTimeMs,
                    expectedSteps / (double) result.averageTimeMs);
        }
    }

    // ==================== Solver Type Benchmarks ====================

    @Test
    @Order(5)
    @DisplayName("Benchmark: Solver type comparison")
    void benchmark_solverTypes() {
        System.out.println("\n=== Solver Type Comparison ===");

        SolverType[] solvers = {SolverType.SOLVER_BE, SolverType.SOLVER_TRZ, SolverType.SOLVER_GS};
        double duration = 5e-3;
        double dt = 1e-6;

        for (SolverType solver : solvers) {
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile("benchmark_solver.ipes")
                    .simulationDuration(duration)
                    .stepWidth(dt)
                    .solverType(solver)
                    .build();

            BenchmarkResult result = runBenchmark(config, 3);

            System.out.printf("Solver %s: avg=%dms, min=%dms, max=%dms%n",
                    solver.toString(), result.averageTimeMs, result.minTimeMs, result.maxTimeMs);
        }
    }

    // ==================== Memory Benchmarks ====================

    @Test
    @Order(6)
    @DisplayName("Benchmark: Memory usage")
    void benchmark_memoryUsage() {
        System.out.println("\n=== Memory Usage Benchmark ===");

        // Force GC and get baseline
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        MemoryUsage beforeHeap = memoryBean.getHeapMemoryUsage();
        long baselineMemory = beforeHeap.getUsed();

        // Run simulation
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_memory.ipes")
                .simulationDuration(20e-3)
                .stepWidth(1e-6)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        MemoryUsage afterHeap = memoryBean.getHeapMemoryUsage();
        long peakMemory = afterHeap.getUsed();
        long memoryUsed = peakMemory - baselineMemory;

        System.out.printf("Baseline memory: %s%n", formatBytes(baselineMemory));
        System.out.printf("Peak memory: %s%n", formatBytes(peakMemory));
        System.out.printf("Memory used: %s%n", formatBytes(memoryUsed));
        System.out.printf("Steps completed: %d%n", result.getTotalTimeSteps());

        if (result.getTotalTimeSteps() > 0) {
            System.out.printf("Memory per 1000 steps: %s%n",
                    formatBytes(memoryUsed * 1000 / result.getTotalTimeSteps()));
        }

        assertTrue(result.isSuccess(), "Simulation should succeed");
    }

    // ==================== Concurrent Simulation Benchmarks ====================

    @Test
    @Order(7)
    @DisplayName("Benchmark: Sequential vs parallel simulations")
    void benchmark_concurrentSimulations() throws InterruptedException {
        System.out.println("\n=== Concurrent Simulations Benchmark ===");

        int numSimulations = 4;
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_concurrent.ipes")
                .simulationDuration(5e-3)
                .stepWidth(1e-6)
                .solverType(SolverType.SOLVER_BE)
                .build();

        // Sequential execution
        long sequentialStart = System.currentTimeMillis();
        for (int i = 0; i < numSimulations; i++) {
            HeadlessSimulationEngine eng = new HeadlessSimulationEngine();
            SimulationResult result = eng.runSimulation(config);
            assertTrue(result.isSuccess());
        }
        long sequentialTime = System.currentTimeMillis() - sequentialStart;

        // Parallel execution
        List<Thread> threads = new ArrayList<>();
        List<SimulationResult> results = new ArrayList<>();
        Object lock = new Object();

        long parallelStart = System.currentTimeMillis();
        for (int i = 0; i < numSimulations; i++) {
            Thread t = new Thread(() -> {
                HeadlessSimulationEngine eng = new HeadlessSimulationEngine();
                SimulationResult result = eng.runSimulation(config);
                synchronized (lock) {
                    results.add(result);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
        long parallelTime = System.currentTimeMillis() - parallelStart;

        // Verify all parallel simulations succeeded
        assertEquals(numSimulations, results.size());
        for (SimulationResult r : results) {
            assertTrue(r.isSuccess(), "All parallel simulations should succeed");
        }

        double speedup = (double) sequentialTime / parallelTime;

        System.out.printf("Sequential execution: %dms%n", sequentialTime);
        System.out.printf("Parallel execution: %dms%n", parallelTime);
        System.out.printf("Speedup: %.2fx%n", speedup);
        System.out.printf("Efficiency: %.1f%%%n", speedup / numSimulations * 100);
    }

    // ==================== Throughput Benchmark ====================

    @Test
    @Order(8)
    @DisplayName("Benchmark: Throughput measurement")
    void benchmark_throughput() {
        System.out.println("\n=== Throughput Benchmark ===");

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("benchmark_throughput.ipes")
                .simulationDuration(10e-3)
                .stepWidth(1e-6)
                .solverType(SolverType.SOLVER_BE)
                .build();

        // Warm up
        engine.runSimulation(config);

        // Measure
        int iterations = 10;
        long totalTime = 0;
        long totalSteps = 0;

        for (int i = 0; i < iterations; i++) {
            HeadlessSimulationEngine eng = new HeadlessSimulationEngine();
            long start = System.currentTimeMillis();
            SimulationResult result = eng.runSimulation(config);
            long elapsed = System.currentTimeMillis() - start;

            totalTime += elapsed;
            totalSteps += result.getTotalTimeSteps();
        }

        double avgTimeMs = totalTime / (double) iterations;
        double avgSteps = totalSteps / (double) iterations;
        double stepsPerSecond = avgSteps / (avgTimeMs / 1000.0);

        System.out.printf("Iterations: %d%n", iterations);
        System.out.printf("Average time: %.1fms%n", avgTimeMs);
        System.out.printf("Average steps: %.0f%n", avgSteps);
        System.out.printf("Throughput: %.0f steps/second%n", stepsPerSecond);
    }

    // ==================== Helper Methods ====================

    private BenchmarkResult runBenchmark(SimulationConfig config, int iterations) {
        List<Long> times = new ArrayList<>();

        // Warm-up run
        engine.runSimulation(config);

        // Measured runs
        for (int i = 0; i < iterations; i++) {
            HeadlessSimulationEngine eng = new HeadlessSimulationEngine();
            long start = System.currentTimeMillis();
            SimulationResult result = eng.runSimulation(config);
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            assertTrue(result.isSuccess(), "Simulation should succeed");
        }

        return new BenchmarkResult(times);
    }

    private void printBenchmarkResult(BenchmarkResult result) {
        System.out.printf("Iterations: %d%n", result.iterations);
        System.out.printf("Average: %dms%n", result.averageTimeMs);
        System.out.printf("Min: %dms%n", result.minTimeMs);
        System.out.printf("Max: %dms%n", result.maxTimeMs);
        System.out.printf("Std Dev: %.1fms%n", result.stdDevMs);
    }

    private static String formatTime(double seconds) {
        if (seconds >= 1.0) {
            return String.format("%.3fs", seconds);
        } else if (seconds >= 1e-3) {
            return String.format("%.3fms", seconds * 1000);
        } else if (seconds >= 1e-6) {
            return String.format("%.3fus", seconds * 1e6);
        } else {
            return String.format("%.3fns", seconds * 1e9);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) {
            return String.format("%.2f GB", bytes / 1_000_000_000.0);
        } else if (bytes >= 1_000_000) {
            return String.format("%.2f MB", bytes / 1_000_000.0);
        } else if (bytes >= 1_000) {
            return String.format("%.2f KB", bytes / 1_000.0);
        } else {
            return String.format("%d bytes", bytes);
        }
    }

    /**
     * Holds benchmark results.
     */
    private static class BenchmarkResult {
        final int iterations;
        final long averageTimeMs;
        final long minTimeMs;
        final long maxTimeMs;
        final double stdDevMs;

        BenchmarkResult(List<Long> times) {
            this.iterations = times.size();
            this.minTimeMs = times.stream().mapToLong(Long::longValue).min().orElse(0);
            this.maxTimeMs = times.stream().mapToLong(Long::longValue).max().orElse(0);
            this.averageTimeMs = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);

            // Calculate standard deviation
            double mean = times.stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = times.stream()
                    .mapToDouble(t -> Math.pow(t - mean, 2))
                    .average()
                    .orElse(0);
            this.stdDevMs = Math.sqrt(variance);
        }
    }
}
