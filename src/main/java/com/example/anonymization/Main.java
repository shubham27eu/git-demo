package com.example.anonymization;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static final String DATA_DF_PATH = "Data_2019-20.csv";
    private static final String ATTRIBUTES_PATH = "Attributes.csv";
    private static final String SENSITIVITY_RESULTS_PATH = "Sensitivity_Results.xlsx";
    private static final String KYU_SCORE_PATH = "KYU Score.xlsx";

    public static void createTableFromSimpleDataFrame(Connection conn, SimpleDataFrame sdf, String tableName) throws SQLException {
        if (sdf == null || sdf.getColumnCount() == 0) {
            System.err.println("Skipping table creation for empty SimpleDataFrame: " + tableName);
            return;
        }
        List<String> headers = sdf.getColumnHeaders();
        String columnsWithType = headers.stream().map(header -> "\"" + header + "\" TEXT").collect(Collectors.joining(", "));

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
            stmt.execute("CREATE TABLE \"" + tableName + "\" (" + columnsWithType + ")");

            String insertSQL = "INSERT INTO \"" + tableName + "\" (" +
                    headers.stream().map(h -> "\"" + h + "\"").collect(Collectors.joining(", ")) +
                    ") VALUES (" +
                    headers.stream().map(h -> "?").collect(Collectors.joining(", ")) + ")";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                for (Map<String, Object> row : sdf.getRows()) {
                    for (int i = 0; i < headers.size(); i++) {
                        pstmt.setString(i + 1, row.get(headers.get(i)) != null ? String.valueOf(row.get(headers.get(i))) : null);
                    }
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    public static SimpleDataFrame executeSqlQueryToSimpleDataFrame(Connection conn, String query) throws SQLException {
        List<String> headers = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                headers.add(metaData.getColumnLabel(i));
            }
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (String header : headers) {
                    row.put(header, rs.getObject(header));
                }
                rows.add(row);
            }
        }

        SimpleDataFrame sdf = new SimpleDataFrame(headers);
        for (Map<String, Object> row : rows) {
            sdf.addRow(row);
        }
        return sdf;
    }

    public static void printSimpleDataFrame(SimpleDataFrame sdf, int maxRows) {
        if (sdf == null) {
            System.out.println("DataFrame is null.");
            return;
        }
        System.out.println("Columns: " + sdf.getColumnHeaders());
        System.out.println("Total Rows: " + sdf.getRowCount());
        List<Map<String, Object>> rowsToPrint = sdf.getRows().subList(0, Math.min(maxRows, sdf.getRowCount()));
        for (int i = 0; i < rowsToPrint.size(); i++) {
            System.out.println("Row " + i + ": " + rowsToPrint.get(i));
        }
        if (sdf.getRowCount() > maxRows) {
            System.out.println("... and " + (sdf.getRowCount() - maxRows) + " more rows.");
        }
        System.out.println("--------------------");
    }

    public static void main(String[] args) {
        System.out.println("Starting Anonymization Process...");
        if (args.length != 3) {
    System.err.println("Usage: java Main <column_id> <filter_value> <query_user_id>");
    return;
}
String userIdColumn = args[0];
String userValue = args[1];
String queryUserId = args[2];

        try {
            SimpleDataFrame dataDf = DataLoader.loadDataDf(DATA_DF_PATH, ';');
            List<SensitivityResult> sensitivityResultsList = DataLoader.loadSensitivityResults(SENSITIVITY_RESULTS_PATH, null);
            List<KyuScore> kyuScoresList = DataLoader.loadKyuScores(KYU_SCORE_PATH, null);

            System.out.println("Data initialized. dataDf rows: " + dataDf.getRowCount());

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                System.out.println("In-memory SQLite DB connected.");
                createTableFromSimpleDataFrame(conn, dataDf, "data_df");
                System.out.println("'data_df' table created and populated in SQLite.");
                String query = String.format("SELECT * FROM data_df WHERE \"%s\" = '%s'", userIdColumn, userValue);
                System.out.println("Executing query: " + query);
                SimpleDataFrame resultSDF = executeSqlQueryToSimpleDataFrame(conn, query);

System.out.println("Query resultSDF rows: " + resultSDF.getRowCount());

                if (resultSDF.getRowCount() == 0) {
                    System.err.println("Query returned no results. Check query, data, and file contents.");
                    return;
                }

                String resultType = DataProcessor.determineQueryResultType(resultSDF);
                System.out.println("Result Type ------ " + resultType);

                String kyuScoreString = kyuScoresList.stream()
                        .filter(ks -> queryUserId.equals(ks.getUserId()))
                        .map(ks -> ks.getKyuScore().toLowerCase())
                        .findFirst().orElse("low");
                System.out.println("KYU Score ------ " + kyuScoreString);

                String sensitivityLevelString;
                if (resultSDF.getColumnCount() == 0) {
                    sensitivityLevelString = "Low";
                    System.err.println("Query result has no columns. Defaulting sensitivity to Low.");
                } else if ("cell".equals(resultType)) {
                    String cellColumnName = resultSDF.getColumnHeaders().get(0);
                    sensitivityLevelString = sensitivityResultsList.stream()
                            .filter(sr -> cellColumnName.equals(String.valueOf(sr.getAttributeId())))
                            .map(SensitivityResult::getSensitivityLevel)
                            .findFirst().orElse("Low");
                } else {
                    sensitivityLevelString = DataProcessor.getMaxSensitivityLevel(resultSDF, sensitivityResultsList);
                }
                System.out.println("Sensitivity Level ------ " + sensitivityLevelString);

                List<String> selectedStrategies = StrategySelector.getStrategies(resultType, sensitivityLevelString.toLowerCase(), kyuScoreString.toLowerCase());
                System.out.println("Selected Strategies ----- " + selectedStrategies);

                SimpleDataFrame originalResultSdf = resultSDF.copy();
                AnonymizationResult anonymizationOutput = AnonymizationService.anonymizeBySensitivity(resultSDF, selectedStrategies, resultType, kyuScoreString);
                SimpleDataFrame anonymizedSdf = anonymizationOutput.getAnonymizedDataFrame();
                String appliedStrategy = anonymizationOutput.getAppliedStrategy();
                System.out.println("Applied Strategy --- " + appliedStrategy);

                if (appliedStrategy == null && !selectedStrategies.isEmpty() &&
                        !(selectedStrategies.size() == 1 && "no_transformation".equalsIgnoreCase(selectedStrategies.get(0)))) {
                    System.err.println("Warning: Strategies were selected but none was applied.");
                }

                AnonymizationScore scores = ScoreCalculator.calculateScore(originalResultSdf, anonymizedSdf);
                System.out.println("Anonymization Score (Composite) ------ " + scores.getScore());
                System.out.println("Utility Retained ------ " + scores.getUtilityRetained());

                System.out.println("\nOriginal Result DataFrame (first 5 rows):");
                printSimpleDataFrame(originalResultSdf, 5);
                System.out.println("\nAnonymized Result DataFrame (first 5 rows):");
                printSimpleDataFrame(anonymizedSdf, 5);

            }
        } catch (IOException | SQLException e) {
            System.err.println("Critical error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nAnonymization Process Completed.");
    }
}
