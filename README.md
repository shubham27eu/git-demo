# Data Processor Project

This project provides tools for data anonymization and processing.

## Building and Running the Project

### Building the JARs

To build the executable JAR files, navigate to the project's root directory (where the `pom.xml` is located) and run the following Maven command:

```bash
mvn clean package
```
This command will compile the code, run tests, and package the application into two JAR files located in the `target/` directory:
- `data-processor-main.jar`
- `data-processor-mock-runner.jar`

### Running `data-processor-main.jar`

This JAR is the main application for data processing and anonymization. It requires six command-line arguments: paths to the data CSV file, attributes CSV file, sensitivity results Excel file, KYU score Excel file, the column ID to query, and the filter value for that column.

**Usage:**
```bash
java -jar target/data-processor-main.jar <data_df_path> <attributes_path> <sensitivity_results_path> <kyu_score_path> <column_id> <filter_value>
```

**Example:**
```bash
java -jar target/data-processor-main.jar path/to/your/Data_2019-20.csv path/to/your/Attributes.csv path/to/your/Sensitivity_Results.xlsx path/to/your/KYU_Score.xlsx "2" "SomeValue"
```
Ensure you replace the placeholder paths and values with the actual paths to your files and the desired query parameters.

### Running `data-processor-mock-runner.jar`

This JAR runs a predefined mock data test scenario using the provided data files. It requires three command-line arguments: paths to the data CSV file, sensitivity results Excel file, and KYU score Excel file.

**Usage:**
```bash
java -jar target/data-processor-mock-runner.jar <data_df_path> <sensitivity_results_path> <kyu_score_path>
```

**Example:**
```bash
java -jar target/data-processor-mock-runner.jar path/to/your/Data_2019-20.csv path/to/your/Sensitivity_Results.xlsx path/to/your/KYU_Score.xlsx
```
Ensure you replace the placeholder paths with the actual paths to your files.

**Note:** All file paths provided as command-line arguments must be valid paths to your CSV and Excel data files. The application reads these files to perform its operations.
