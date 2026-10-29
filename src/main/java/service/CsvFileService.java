package service;

import model.CsvDataset;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Service for CSV import and validation. */
public class CsvFileService {
    public CsvDataset load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            List<String> headers = new ArrayList<>(parser.getHeaderNames());
            if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
                throw new IOException("The CSV file must contain a header row.");
            }

            List<List<String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (int col = 0; col < headers.size(); col++) {
                    row.add(col < record.size() ? record.get(col).trim() : "");
                }
                if (record.size() > headers.size()) {
                    throw new IOException("A row contains more values than the header has columns (record "
                            + record.getRecordNumber() + ").");
                }
                rows.add(row);
            }
            if (rows.isEmpty()) throw new IOException("The CSV file has headers but no data rows.");
            return new CsvDataset(headers, rows);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid CSV structure: " + ex.getMessage(), ex);
        }
    }

}
