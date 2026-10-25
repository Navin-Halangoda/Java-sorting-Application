package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds a CSV header and its complete rows. */
public final class CsvDataset {
    private final List<String> headers;
    private final List<List<String>> rows;

    public CsvDataset(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("CSV must contain at least one header.");
        }
        this.headers = List.copyOf(headers);
        List<List<String>> copiedRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                normalized.add(row != null && i < row.size() && row.get(i) != null
                        ? row.get(i) : "");
            }
            copiedRows.add(Collections.unmodifiableList(normalized));
        }
        this.rows = Collections.unmodifiableList(copiedRows);
    }

    public List<String> getHeaders() { return headers; }
    public List<List<String>> getRows() { return rows; }
    public int getRowCount() { return rows.size(); }
    public int getColumnCount() { return headers.size(); }

    /** Return column names whose non-empty values are all finite numbers. */
    public List<String> getNumericColumnNames() {
        List<String> numeric = new ArrayList<>();
        for (int col = 0; col < headers.size(); col++) {
            boolean hasNumber = false;
            boolean valid = true;
            for (List<String> row : rows) {
                String value = row.get(col).trim();
                if (value.isEmpty()) continue;
                try {
                    double number = Double.parseDouble(value);
                    if (!Double.isFinite(number)) { valid = false; break; }
                    hasNumber = true;
                } catch (NumberFormatException ex) {
                    valid = false;
                    break;
                }
            }
            if (valid && hasNumber) numeric.add(headers.get(col));
        }
        return numeric;
    }

    public List<List<String>> copyRows() {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> row : rows) copy.add(new ArrayList<>(row));
        return copy;
    }
}
