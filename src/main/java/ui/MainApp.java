package ui;

import model.CsvDataset;
import model.PerformanceResult;
import service.BenchmarkService;
import service.CsvFileService;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Member 3: JavaFX user interface and integration of the services. */
public class MainApp extends Application {
    private final CsvFileService csvService = new CsvFileService();
    private final BenchmarkService benchmarkService = new BenchmarkService();
    private final TableView<ObservableList<String>> dataTable = new TableView<>();
    private final ComboBox<String> columnCombo = new ComboBox<>();
    private final ComboBox<String> orderCombo = new ComboBox<>();
    private final Button sortButton = new Button("Compare all algorithms");
    private final Label fileLabel = new Label("No CSV file loaded");
    private final Label bestLabel = new Label("Fastest algorithm: —");
    private final Label comparisonStatusLabel = new Label();
    private final ProgressIndicator comparisonProgress = new ProgressIndicator();
    private final TableView<PerformanceResult> performanceTable = new TableView<>();
    private CsvDataset dataset;

    @Override
    public void start(Stage stage) {
        stage.setTitle("CSV SortLab - Numeric Sorting Performance Analyzer");
        Button uploadButton = new Button("Upload CSV");
        uploadButton.setOnAction(event -> uploadCsv(stage));
        sortButton.setOnAction(event -> compareAlgorithms());
        sortButton.setDisable(true);
        columnCombo.setPromptText("Select numeric column");
        columnCombo.setPrefWidth(200);
        columnCombo.setOnAction(event -> sortButton.setDisable(dataset == null || columnCombo.getValue() == null));
        orderCombo.getItems().addAll("Ascending", "Descending");
        orderCombo.setValue("Ascending");
        comparisonProgress.setPrefSize(22, 22);
        comparisonProgress.setVisible(false);
        comparisonProgress.setManaged(false);

        HBox uploadRow = new HBox(12, uploadButton, fileLabel);
        uploadRow.setAlignment(Pos.CENTER_LEFT);
        HBox controls = new HBox(10, new Label("Numeric column:"), columnCombo,
                new Label("Order:"), orderCombo, sortButton, comparisonProgress, comparisonStatusLabel);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label dataHeading = heading("Dataset Preview");
        Label performanceHeading = heading("Algorithm Performance");
        configurePerformanceTable();
        VBox content = new VBox(12, uploadRow, controls, dataHeading, dataTable,
                performanceHeading, performanceTable, bestLabel);
        content.setPadding(new Insets(16));
        VBox.setVgrow(dataTable, Priority.ALWAYS);
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        dataTable.setPlaceholder(new Label("Upload a CSV file to display its data."));
        performanceTable.setPrefHeight(180);
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Scene scene = new Scene(content, 1150, 760);
        stage.setScene(scene);
        stage.show();
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    private void uploadCsv(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a CSV dataset");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            CsvDataset loaded = csvService.load(file.toPath());
            dataset = loaded;
            fileLabel.setText(file.getName() + " (" + dataset.getRowCount() + " rows)");
            columnCombo.getItems().setAll(dataset.getNumericColumnNames());
            if (!columnCombo.getItems().isEmpty()) columnCombo.getSelectionModel().selectFirst();
            showRows(dataset.copyRows());
            performanceTable.getItems().clear();
            bestLabel.setText("Fastest algorithm: —");
            sortButton.setDisable(columnCombo.getValue() == null);
            if (columnCombo.getItems().isEmpty()) showInfo("No numeric columns found. Use a CSV containing at least one numeric column.");
        } catch (Exception ex) {
            showError("Could not load CSV: " + ex.getMessage());
        }
    }

    private void compareAlgorithms() {
        if (dataset == null || columnCombo.getValue() == null) return;
        int columnIndex = dataset.getHeaders().indexOf(columnCombo.getValue());
        boolean ascending = "Ascending".equals(orderCombo.getValue());
        List<List<String>> snapshot = dataset.copyRows();
        Comparator<List<String>> comparator = numericComparator(columnIndex, ascending);
        sortButton.setDisable(true);
        comparisonProgress.setVisible(true);
        comparisonProgress.setManaged(true);
        comparisonStatusLabel.setText("Comparing algorithms...");
        Task<BenchmarkService.BenchmarkReport> task = new Task<>() {
            @Override protected BenchmarkService.BenchmarkReport call() {
                return benchmarkService.compare(snapshot, comparator);
            }
        };
        task.setOnSucceeded(event -> {
            BenchmarkService.BenchmarkReport report = task.getValue();
            performanceTable.setItems(FXCollections.observableArrayList(report.results()));
            showRows(report.fastestRows());
            PerformanceResult fastest = report.results().stream()
                    .min(Comparator.comparingLong(PerformanceResult::getElapsedNanos)).orElseThrow();
            bestLabel.setText("Fastest algorithm: " + fastest.getAlgorithm()
                    + " (" + fastest.getMilliseconds() + " ms)");
            sortButton.setDisable(false);
            comparisonProgress.setVisible(false);
            comparisonProgress.setManaged(false);
            comparisonStatusLabel.setText("Comparison complete.");
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            showError("Sorting failed: " + (error == null ? "Unknown error" : error.getMessage()));
            sortButton.setDisable(false);
            comparisonProgress.setVisible(false);
            comparisonProgress.setManaged(false);
            comparisonStatusLabel.setText("Comparison failed.");
        });
        Thread thread = new Thread(task, "csv-sort-benchmark");
        thread.setDaemon(true);
        thread.start();
    }

    private Comparator<List<String>> numericComparator(int column, boolean ascending) {
        Map<String, Double> parsedValues = new HashMap<>();
        return (first, second) -> {
            String a = column < first.size() ? first.get(column).trim() : "";
            String b = column < second.size() ? second.get(column).trim() : "";
            boolean blankA = a.isEmpty(), blankB = b.isEmpty();
            if (blankA || blankB) {
                if (blankA && blankB) return 0;
                return blankA ? 1 : -1; // blank values always stay last
            }
            double valueA = parsedValues.computeIfAbsent(a, Double::parseDouble);
            double valueB = parsedValues.computeIfAbsent(b, Double::parseDouble);
            int comparison = Double.compare(valueA, valueB);
            return ascending ? comparison : -comparison;
        };
    }

    private void showRows(List<List<String>> rows) {
        dataTable.getColumns().clear();
        for (int col = 0; col < dataset.getColumnCount(); col++) {
            final int index = col;
            TableColumn<ObservableList<String>, String> tableColumn = new TableColumn<>(dataset.getHeaders().get(col));
            tableColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                    index < data.getValue().size() ? data.getValue().get(index) : ""));
            tableColumn.setPrefWidth(140);
            dataTable.getColumns().add(tableColumn);
        }
        ObservableList<ObservableList<String>> tableRows = FXCollections.observableArrayList();
        for (List<String> row : rows) tableRows.add(FXCollections.observableArrayList(row));
        dataTable.setItems(tableRows);
    }

    private void configurePerformanceTable() {
        TableColumn<PerformanceResult, String> algorithmCol = new TableColumn<>("Algorithm");
        algorithmCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getAlgorithm()));
        TableColumn<PerformanceResult, String> timeCol = new TableColumn<>("Execution time (ms)");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getMilliseconds()));
        performanceTable.getColumns().add(algorithmCol);
        performanceTable.getColumns().add(timeCol);
        performanceTable.setPlaceholder(new Label("Run a comparison to see execution times."));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("CSV SortLab"); alert.setHeaderText("Something went wrong"); alert.showAndWait();
    }
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("CSV SortLab"); alert.setHeaderText(null); alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
