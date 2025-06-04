# Data Processor Project

This project provides tools for data anonymization and processing. It generates two primary executable JARs:
- `data-processor-main.jar`: The main application which uses compile-time embedded paths for data files (including KYU scores) and runtime arguments for query parameters.
- `data-processor-mock-runner.jar`: A test runner that takes all file paths as command-line arguments.

## Building and Running the Project

### Building the JARs

To build the executable JAR files, navigate to the project's root directory (where `pom.xml` is located).

The build process for `data-processor-main.jar` embeds paths for the main data CSV, attributes CSV, sensitivity results Excel file, and the KYU score Excel file directly into the JAR from Maven properties provided at build time.

Run the following Maven command, replacing placeholder paths with actual paths to your files:
```bash
mvn clean package -Ddata.df.path=./path/to/your/Data_2019-20.csv \
                  -Dattributes.path=./path/to/your/Attributes.csv \
                  -Dsensitivity.results.path=./path/to/your/Sensitivity_Results.xlsx \
                  -Dkyu.score.path=./path/to/your/KYU_Score.xlsx
```

**Explanation of Build-Time Properties for `data-processor-main.jar`**:
- `-Ddata.df.path`: Specifies the path to the main data CSV file (e.g., `Data_2019-20.csv`).
- `-Dattributes.path`: Specifies the path to the attributes definition CSV file (e.g., `Attributes.csv`). (Note: This path is loaded but not actively used by the `Main` class's current direct logic, but is configured for potential future use).
- `-Dsensitivity.results.path`: Specifies the path to the sensitivity results Excel file (e.g., `Sensitivity_Results.xlsx`).
- `-Dkyu.score.path`: Specifies the path to the KYU score Excel file (e.g., `KYU_Score.xlsx`).

If these properties are not provided during the build, they will default to `"path-not-set"`, which will likely cause runtime errors when `data-processor-main.jar` is executed as the application will not find the necessary data files.

This command will compile the code, run tests (if any), and package the application into two JAR files located in the `target/` directory:
- `target/data-processor-main.jar` (with all four data file paths embedded)
- `target/data-processor-mock-runner.jar`

### Running `data-processor-main.jar`

This JAR is the main application for data processing and anonymization.
- Paths for all necessary data files (main data, attributes, sensitivity results, and KYU scores) are embedded at build time.
- It requires three command-line arguments at runtime: the user ID (for KYU score lookup), the column ID to query from the main data file, and the filter value for that column.

**Usage:**
```bash
java -jar target/data-processor-main.jar <user_id> <column_id> <filter_value>
```

**Arguments:**
- `<user_id>`: The ID of the user. This ID is used to look up their KYU score from the KYU score file (whose path was embedded at build time).
- `<column_id>`: The ID/name of the column to be queried from the main data file.
- `<filter_value>`: The value to filter by in the specified `<column_id>`.

**Example:**
```bash
java -jar target/data-processor-main.jar "2" "user_id_column_in_data_df" "some_filter_value_for_data_df"
```
Ensure you replace `"2"`, `"user_id_column_in_data_df"`, and `"some_filter_value_for_data_df"` with your desired user ID, column ID, and filter value.

### Running `data-processor-mock-runner.jar`

This JAR runs a predefined mock data test scenario using the provided data files. It requires all three primary data file paths as command-line arguments.

**Usage:**
```bash
java -jar target/data-processor-mock-runner.jar <data_df_path> <sensitivity_results_path> <kyu_score_path>
```

**Arguments:**
- `<data_df_path>`: Path to the main data CSV file.
- `<sensitivity_results_path>`: Path to the sensitivity results Excel file.
- `<kyu_score_path>`: Path to the KYU score Excel file.

**Example:**
```bash
java -jar target/data-processor-mock-runner.jar path/to/your/Data_2019-20.csv path/to/your/Sensitivity_Results.xlsx path/to/your/KYU_Score.xlsx
```
Ensure you replace the placeholder paths with the actual paths to your files.

## Using as a Library / Dependency

The `data-processor-main.jar`, once built with its embedded paths (by providing the `-D` properties during `mvn clean package`), can potentially be included as a dependency in other Maven projects if you need to call its functionalities programmatically.

If the JAR is available locally, you might consider installing it to your local Maven repository (`mvn install`) or using a system-scoped dependency.

**Example of System-Scoped Dependency (for local JAR):**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>data-processor-main</artifactId> <!-- Note: artifactId might need adjustment if using classifiers -->
    <version>1.0-SNAPSHOT</version> <!-- Or the version built -->
    <scope>system</scope>
    <systemPath>${project.basedir}/path/to/data-processor-main.jar</systemPath>
</dependency>
```
**Note:** Using system scope has limitations and is generally not recommended for multi-module projects or wider distribution. Installing the JAR to a local or remote Maven repository is a more robust approach.

When used as a library, you could invoke the `main` method of `com.example.anonymization.Main` class, providing the three runtime arguments (`user_id`, `column_id`, `filter_value`) programmatically. Alternatively, you might refactor the core logic into separate, more easily callable public methods if direct `main` invocation is not suitable.
The paths for all data files (including KYU scores) would be used as configured during its build.
```
