package algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Member 2: sorting algorithm implementations. Sorts the supplied list in-place. */
public final class SortingAlgorithms {
    private SortingAlgorithms() { }

    public static <T> void sort(List<T> items, Comparator<? super T> comparator, SortAlgorithm algorithm) {
        switch (algorithm) {
            case INSERTION -> insertionSort(items, comparator);
            case SHELL -> shellSort(items, comparator);
            case MERGE -> mergeSort(items, comparator);
            case QUICK -> quickSort(items, comparator);
            case HEAP -> heapSort(items, comparator);
        }
    }

    public static <T> void insertionSort(List<T> a, Comparator<? super T> c) {
        for (int i = 1; i < a.size(); i++) {
            T key = a.get(i); int j = i - 1;
            while (j >= 0 && c.compare(a.get(j), key) > 0) { a.set(j + 1, a.get(j)); j--; }
            a.set(j + 1, key);
        }
    }

    public static <T> void shellSort(List<T> a, Comparator<? super T> c) {
        for (int gap = a.size() / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < a.size(); i++) {
                T value = a.get(i); int j = i;
                while (j >= gap && c.compare(a.get(j - gap), value) > 0) {
                    a.set(j, a.get(j - gap)); j -= gap;
                }
                a.set(j, value);
            }
        }
    }

    public static <T> void mergeSort(List<T> a, Comparator<? super T> c) {
        if (a.size() < 2) return;
        List<T> aux = new ArrayList<>(a);
        mergeSort(a, aux, 0, a.size() - 1, c);
    }

    private static <T> void mergeSort(List<T> a, List<T> aux, int left, int right, Comparator<? super T> c) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(a, aux, left, mid, c);
        mergeSort(a, aux, mid + 1, right, c);
        if (c.compare(a.get(mid), a.get(mid + 1)) <= 0) return;
        for (int i = left; i <= right; i++) aux.set(i, a.get(i));
        int i = left, j = mid + 1;
        for (int k = left; k <= right; k++) {
            if (i > mid) a.set(k, aux.get(j++));
            else if (j > right) a.set(k, aux.get(i++));
            else if (c.compare(aux.get(i), aux.get(j)) <= 0) a.set(k, aux.get(i++));
            else a.set(k, aux.get(j++));
        }
    }

    public static <T> void quickSort(List<T> a, Comparator<? super T> c) {
        if (a.size() > 1) quickSort(a, 0, a.size() - 1, c);
    }

    private static <T> void quickSort(List<T> a, int low, int high, Comparator<? super T> c) {
        while (low < high) {
            int pivot = partition(a, low, high, c);
            if (pivot - low < high - pivot) { quickSort(a, low, pivot - 1, c); low = pivot + 1; }
            else { quickSort(a, pivot + 1, high, c); high = pivot - 1; }
        }
    }

    private static <T> int partition(List<T> a, int low, int high, Comparator<? super T> c) {
        T pivot = a.get(high); int i = low;
        for (int j = low; j < high; j++) if (c.compare(a.get(j), pivot) <= 0) swap(a, i++, j);
        swap(a, i, high); return i;
    }

    public static <T> void heapSort(List<T> a, Comparator<? super T> c) {
        int n = a.size();
        for (int i = n / 2 - 1; i >= 0; i--) heapify(a, n, i, c);
        for (int end = n - 1; end > 0; end--) { swap(a, 0, end); heapify(a, end, 0, c); }
    }

    private static <T> void heapify(List<T> a, int size, int root, Comparator<? super T> c) {
        int largest = root, left = 2 * root + 1, right = 2 * root + 2;
        if (left < size && c.compare(a.get(left), a.get(largest)) > 0) largest = left;
        if (right < size && c.compare(a.get(right), a.get(largest)) > 0) largest = right;
        if (largest != root) { swap(a, root, largest); heapify(a, size, largest, c); }
    }

    private static <T> void swap(List<T> a, int i, int j) {
        if (i == j) return;
        T temp = a.get(i); a.set(i, a.get(j)); a.set(j, temp);
    }
}
