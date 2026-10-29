package service;

import algorithms.SortAlgorithm;
import algorithms.SortingAlgorithms;
import model.PerformanceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BenchmarkService {
    private static final int MEASUREMENT_RUNS = 5;
    private final int warmupRuns;

    public BenchmarkService() { this(2); }
    public BenchmarkService(int warmupRuns) {
        if (warmupRuns < 0) throw new IllegalArgumentException("Invalid benchmark run count.");
        this.warmupRuns = warmupRuns;
    }

    public BenchmarkReport compare(List<List<String>> originalRows,
                                   Comparator<List<String>> comparator) {
        List<PerformanceResult> results = new ArrayList<>();
        List<List<String>> fastestRows = null;
        long fastestTime = Long.MAX_VALUE;
        int effectiveWarmupRuns = originalRows.size() > 10_000 ? 0 : warmupRuns;

        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            for (int i = 0; i < effectiveWarmupRuns; i++) {
                List<List<String>> warmup = copyRows(originalRows);
                SortingAlgorithms.sort(warmup, comparator, algorithm);
            }

            long[] measurements = new long[MEASUREMENT_RUNS];
            List<List<String>> measuredRows = null;
            for (int run = 0; run < MEASUREMENT_RUNS; run++) {
                List<List<String>> working = copyRows(originalRows);
                long start = System.nanoTime();
                SortingAlgorithms.sort(working, comparator, algorithm);
                measurements[run] = System.nanoTime() - start;
                if (!isSorted(working, comparator)) {
                    throw new IllegalStateException(algorithm.getDisplayName()
                            + " produced an incorrectly sorted result.");
                }
                measuredRows = working;
            }
            long elapsed = median(measurements);
            results.add(new PerformanceResult(algorithm.getDisplayName(), elapsed));
            if (elapsed < fastestTime) {
                fastestTime = elapsed;
                fastestRows = measuredRows;
            }
        }
        return new BenchmarkReport(results, fastestRows);
    }

    private static long median(long[] measurements) {
        Arrays.sort(measurements);
        return measurements[measurements.length / 2];
    }

    private static List<List<String>> copyRows(List<List<String>> source) {
        return new ArrayList<>(source);
    }

    private static boolean isSorted(List<List<String>> rows, Comparator<List<String>> comparator) {
        for (int i = 1; i < rows.size(); i++) if (comparator.compare(rows.get(i - 1), rows.get(i)) > 0) return false;
        return true;
    }

    public record BenchmarkReport(List<PerformanceResult> results, List<List<String>> fastestRows) { }
}
