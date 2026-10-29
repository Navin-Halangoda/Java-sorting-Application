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
import java.net.URL;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {
    private static final String APP_STYLESHEET = ".root { -fx-base: #f8fafc; -fx-background-color: #f8fafc; }\n"
            + ".table-view { -fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-table-cell-border-color: #f1f5f9; }\n"
            + ".table-view .column-header-background { -fx-background-color: #f1f5f9; -fx-background-radius: 6px 6px 0 0; }\n"
            + ".table-view .column-header { -fx-background-color: transparent; -fx-border-color: transparent #e2e8f0 #cbd5e1 transparent; -fx-border-width: 0 1 1 0; -fx-padding: 6px 8px; }\n"
            + ".table-view .column-header .label { -fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 13px; }\n"
            + ".table-view .filler { -fx-background-color: #f1f5f9; -fx-border-color: transparent transparent #cbd5e1 transparent; -fx-border-width: 0 0 1 0; }\n"
            + ".table-row-cell:odd { -fx-background-color: #f8fafc; }\n"
            + ".table-row-cell:even { -fx-background-color: #ffffff; }\n"
            + ".table-row-cell:selected { -fx-background-color: #e0f2fe; -fx-text-fill: #0f172a; }";

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
        uploadButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #1e293b; "
                + "-fx-font-weight: bold; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; "
                + "-fx-background-radius: 6px; -fx-padding: 6px 14px; -fx-cursor: hand;");
        uploadButton.setOnMouseEntered(event -> uploadButton.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; "
                + "-fx-font-weight: bold; -fx-border-color: #94a3b8; -fx-border-radius: 6px; "
                + "-fx-background-radius: 6px; -fx-padding: 6px 14px; -fx-cursor: hand;"));
        uploadButton.setOnMouseExited(event -> uploadButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #1e293b; "
                + "-fx-font-weight: bold; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; "
                + "-fx-background-radius: 6px; -fx-padding: 6px 14px; -fx-cursor: hand;"));

        fileLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        sortButton.setOnAction(event -> compareAlgorithms());
        sortButton.setDisable(true);
        updateSortButtonStyle();
        sortButton.disabledProperty().addListener((obs, oldVal, newVal) -> updateSortButtonStyle());
        sortButton.setOnMouseEntered(event -> {
            if (!sortButton.isDisabled()) {
                sortButton.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: #ffffff; "
                        + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 16px; -fx-cursor: hand;");
            }
        });
        sortButton.setOnMouseExited(event -> updateSortButtonStyle());

        String comboStyle = "-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; "
                + "-fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 13px;";
        columnCombo.setPromptText("Select numeric column");
        columnCombo.setPrefWidth(200);
        columnCombo.setStyle(comboStyle);
        columnCombo.setOnAction(event -> sortButton.setDisable(dataset == null || columnCombo.getValue() == null));
        orderCombo.getItems().addAll("Ascending", "Descending");
        orderCombo.setValue("Ascending");
        orderCombo.setStyle(comboStyle);
        comparisonProgress.setPrefSize(22, 22);
        comparisonProgress.setStyle("-fx-progress-color: #2563eb;");
        comparisonProgress.setVisible(false);
        comparisonProgress.setManaged(false);

        comparisonStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        HBox uploadRow = new HBox(12, uploadButton, fileLabel);
        uploadRow.setAlignment(Pos.CENTER_LEFT);

        Label columnLabel = new Label("Numeric column:");
        columnLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        Label orderLabel = new Label("Order:");
        orderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        HBox controls = new HBox(10, columnLabel, columnCombo,
                orderLabel, orderCombo, sortButton, comparisonProgress, comparisonStatusLabel);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label dataHeading = heading("Dataset Preview");
        Label performanceHeading = heading("Algorithm Performance");
        configurePerformanceTable();
        bestLabel.setStyle("-fx-font-size: 14px; "
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: #1e3a8a; "
                + "-fx-background-color: #e0f2fe; "
                + "-fx-padding: 8px 14px; "
                + "-fx-background-radius: 6px; "
                + "-fx-border-color: #93c5fd; "
                + "-fx-border-radius: 6px; "
                + "-fx-border-width: 1px;");
        VBox content = new VBox(12, uploadRow, controls, dataHeading, dataTable,
                performanceHeading, performanceTable, bestLabel);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #f8fafc;");
        VBox.setVgrow(dataTable, Priority.ALWAYS);
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label dataPlaceholder = new Label("Upload a CSV file to display its data.");
        dataPlaceholder.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        dataTable.setPlaceholder(dataPlaceholder);
        String tableStyle = "-fx-base: #f8fafc; -fx-background-color: #ffffff; "
                + "-fx-table-cell-border-color: #f1f5f9; -fx-border-color: #cbd5e1; "
                + "-fx-border-radius: 6px; -fx-background-radius: 6px;";
        dataTable.setStyle(tableStyle);
        performanceTable.setPrefHeight(180);
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        performanceTable.setStyle(tableStyle);
        Label perfPlaceholder = new Label("Run a comparison to see execution times.");
        perfPlaceholder.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        performanceTable.setPlaceholder(perfPlaceholder);
        Scene scene = new Scene(content, 1150, 760);
        applyStylesheet(scene);
        stage.setScene(scene);
        stage.show();
    }

    private void updateSortButtonStyle() {
        if (sortButton.isDisabled()) {
            sortButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 16px; -fx-cursor: default;");
        } else {
            sortButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 16px; -fx-cursor: hand;");
        }
    }

    private void applyStylesheet(Scene scene) {
        try {
            URL resource = getClass().getResource("/style.css");
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                File tempCss = File.createTempFile("csvsortlab-style", ".css");
                tempCss.deleteOnExit();
                java.nio.file.Files.writeString(tempCss.toPath(), APP_STYLESHEET);
                scene.getStylesheets().add(tempCss.toURI().toString());
            }
        } catch (Exception ignored) {
        }
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
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
            fileLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
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
        comparisonStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
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
            comparisonStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            showError("Sorting failed: " + (error == null ? "Unknown error" : error.getMessage()));
            sortButton.setDisable(false);
            comparisonProgress.setVisible(false);
            comparisonProgress.setManaged(false);
            comparisonStatusLabel.setText("Comparison failed.");
            comparisonStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
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
        TableColumn<PerformanceResult, String> timeCol = new TableColumn<>("Median execution time (ms)");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getMilliseconds()));
        performanceTable.getColumns().add(algorithmCol);
        performanceTable.getColumns().add(timeCol);
        Label placeholder = new Label("Run a comparison to see execution times.");
        placeholder.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        performanceTable.setPlaceholder(placeholder);
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
