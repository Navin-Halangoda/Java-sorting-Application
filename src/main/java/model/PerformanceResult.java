package model;

import java.util.Locale;

public final class PerformanceResult {
    private final String algorithm;
    private final long elapsedNanos;

    public PerformanceResult(String algorithm, long elapsedNanos) {
        this.algorithm = algorithm;
        this.elapsedNanos = elapsedNanos;
    }
    public String getAlgorithm() { return algorithm; }
    public long getElapsedNanos() { return elapsedNanos; }
    public String getMilliseconds() { return String.format(Locale.US, "%.3f", elapsedNanos / 1_000_000.0); }
}
