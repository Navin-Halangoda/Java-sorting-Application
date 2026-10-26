package algorithms;

public enum SortAlgorithm {
    INSERTION("Insertion Sort"),
    SHELL("Shell Sort"),
    MERGE("Merge Sort"),
    QUICK("Quick Sort"),
    HEAP("Heap Sort");

    private final String displayName;
    SortAlgorithm(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
