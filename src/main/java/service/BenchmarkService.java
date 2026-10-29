package service;

import algorithms.SortAlgorithm;
import algorithms.SortingAlgorithms;
import model.PerformanceResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Member 3: repeated timing and result verification. */
public class BenchmarkService {
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

            List<List<String>> working = copyRows(originalRows);
            long start = System.nanoTime();
            SortingAlgorithms.sort(working, comparator, algorithm);
            long elapsed = System.nanoTime() - start;
            if (!isSorted(working, comparator)) {
                throw new IllegalStateException(algorithm.getDisplayName() + " produced an incorrectly sorted result.");
            }
            results.add(new PerformanceResult(algorithm.getDisplayName(), elapsed));
            if (elapsed < fastestTime) {
                fastestTime = elapsed;
                fastestRows = working;
            }
        }
        return new BenchmarkReport(results, fastestRows);
    }

    private static List<List<String>> copyRows(List<List<String>> source) {
        // Sorting changes only row order, so copying the outer list is sufficient.
        return new ArrayList<>(source);
    }

    private static boolean isSorted(List<List<String>> rows, Comparator<List<String>> comparator) {
        for (int i = 1; i < rows.size(); i++) if (comparator.compare(rows.get(i - 1), rows.get(i)) > 0) return false;
        return true;
    }

    public record BenchmarkReport(List<PerformanceResult> results, List<List<String>> fastestRows) { }
}
