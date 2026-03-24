# <img src="src/main/resources/images/logo.png" width="80" height="80" align="center"> QueryCraft v1.0.0

QueryCraft is a high-performance, JavaFX-based desktop application designed for database querying, data exploration, and export. It provides a robust interface for interacting with multiple database engines, supporting large dataset handling through advanced features like dynamic row limiting and streaming mode.

## 🚀 Core Features

### 🔌 Database & Data Support
- **Multi-Engine Support**: Seamlessly connect to **MySQL**, **PostgreSQL**, and **Microsoft SQL Server**.
- **CSV Folder Connection**: Treat a directory of `.csv` files as a virtual database. Query files as tables using standard SQL syntax (powered by H2).
- **Secure Connection**: Robust connection management with real-time error reporting (e.g., Access Denied, Host Unreachable).

### 📊 Advanced Data Handling
- **Standard Mode**: Fast result preview for datasets up to **50,000 rows**.
- **Streaming Mode**: Specialized mode for massive datasets (100,000 to 1,000,000+ rows). Ensures stability and prevents $OOM$ (Out of Memory) errors by processing rows in batches.
- **Dynamic Row Limiting**: User-configurable row caps (configurable via Settings) to balance performance and memory usage.
- **Truncation Warning**: Explicit visual indicator in the UI when query results are capped by the configured limit.

### 🛡️ Security & Validation
- **SQL Validator**: Built-in protection that blocks destructive commands like `DROP` and `TRUNCATE` to prevent accidental data loss.
- **Safety Dialogs**: Mandatory confirmation dialogs for `DELETE` and `UPDATE` operations, showing the impact before execution.
- **Read-Only Detection**: Smart detection of SELECT-only queries to optimize execution paths.

### 📤 Export & Tools
- **Advanced CSV Export**:
  - Full control over character encoding (**UTF-8**, **UTF-8 with BOM**, **TIS-620**, **Windows-874**).
  - Customizable delimiters (Comma, Semicolon, Tab, Pipe).
  - Excel-compatible format options.
  - Date formatting and header toggling.
- **SQL INSERT Generator**:
  - Transform query results into executable `.sql` scripts.
  - Supports transaction wrappers and proper value escaping for various dialects.
- **SQL Formatter**: One-click SQL beautification for complex queries.

## 🏗️ Technical Architecture

QueryCraft follows a clean, modular architecture inspired by modern design patterns:
- **Strategy Pattern (Dialects)**: Abstracted database logic allowing easy addition of new SQL dialects.
- **Command/Handler Pattern**: Specialized execution logic for SELECT, UPDATE, and Generic commands.
- **Observer Pattern**: Decoupled UI and Service layers for real-time status updates and event handling.
- **Factory Pattern**: Centralized creation of exporters and data generators.

## ⚙️ Requirements
- **Java**: JDK 17 or higher.
- **Maven**: For building and dependency management.
- **Driver**: JDBC drivers are automatically managed via Maven.

## 🛠️ Getting Started

### Installation & Build
```bash
# Clone the repository
git clone https://github.com/sutthiphatchuenban/querycraft_beta.git
cd querycraft

# Build the project
mvn clean package

# Run the application
mvn javafx:run
```

### Running Tests
QueryCraft includes a comprehensive suite of **31+ tests** (Unit & Integration):
```bash
mvn test
```

## 📖 Usage Guide

### 1. Connecting
1. Click **Connect...** on the top bar.
2. Select your provider (**MySQL**, **Postgres**, **SQL Server**, or **CSV Folder**).
3. If using CSV, simply point to a folder. If using a database, provide the host and credentials.
4. Click **Connect**. The sidebar will automatically refresh with the list of tables/files.

### 2. Executing Queries
- Type your SQL in the editor.
- Press **Shortcut + Enter** to execute.
- For large exports, toggle **Streaming Mode: ON** before execution for maximum stability.

### 3. Configuring Limits
Go to **Settings** to adjust:
- **Max Rows Limit**: Set between 1,000 and 50,000.
- **Query Timeout**: Control how long the application waits for the server.

## 📝 Configuration Directory
User preferences and settings are stored locally using `java.util.prefs.Preferences` (Registry on Windows), ensuring your UI theme and row limits persist across launches.

## 🤝 Contributing
Contributions are welcome! If you encounter bugs or have feature requests, please open an issue or submit a pull request.

## ⚖️ License
This project is provided for development and educational use. See official license for further details.

---
**Developed by QueryCraft Team**
