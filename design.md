# QueryCraft - Database Query Tool Design Document

## Overview
QueryCraft is a Java-based desktop application for database querying with support for multiple database engines, result preview, CSV export with customizable options, and SQL INSERT statement generation.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (JavaFX)                       │
│  ┌────────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ SidebarSection │  │ ResultTable  │  │ QueryEditor      │  │
│  └────────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                          │
│  ┌──────────────────────┐    ┌──────────────────────────┐    │
│  │ MainController       │    │ QueryExecutionController │    │
│  └──────────────────────┘    └──────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                               │
┌──────────────────┬───────────┴───────────┬──────────────────┐
│  connection/     │       query/          │     export/      │
│  ┌────────────┐  │  ┌─────────────────┐  │  ┌────────────┐  │
│  │ DB Service  │  │  │ Streaming/Batch │  │  │ Exporters  │  │
│  └────────────┘  │  └─────────────────┘  │  └────────────┘  │
└──────────────────┴───────────────────────┴──────────────────┘
�──────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Database  │  │   Query     │  │    Export Service   │  │
│  │  Connection │  │  Executor   │  │   (CSV/SQL Gen)     │  │
│  │   Service   │  │   Service   │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Data Access Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  JDBC MySQL │  │ JDBC Postgre│  │    JDBC MSSQL       │  │
│  │   Driver    │  │    SQL      │  │      Driver         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Features

### 1. Database Connection Management
- **Supported Databases:**
  - MySQL (com.mysql.cj.jdbc.Driver)
  - PostgreSQL (org.postgresql.Driver)
  - Microsoft SQL Server (com.microsoft.sqlserver.jdbc.SQLServerDriver)
- **Connection Parameters:**
  - Host/Server address
  - Port (auto-detected based on DB type)
  - Database name
  - Username/Password
  - Connection string (advanced mode)

### 2. Query Execution
- **Supported Operations:**
  - **SELECT:** Preview results in table view, show row count
  - **DELETE:** Show affected rows count with confirmation
- **Safety Features:**
  - Query validation before execution
  - Confirmation dialog for DELETE operations
  - Execution timeout protection
  - Row limit for SELECT queries (configurable)

### 3. Result Preview
- **Table View:**
  - Column headers from query result
  - Sortable columns
  - Paginated results for large datasets
  - Row count display
- **Column Type Display:**
  - Show data types for each column
  - Format dates and numbers appropriately

### 4. CSV Export
- **Format Options:**
  - Standard CSV
  - Excel-compatible CSV
- **Encoding Options:**
  - UTF-8
  - UTF-8 with BOM
  - TIS-620 (Thai Windows)
  - Windows-874
- **Delimiter Options:**
  - Comma (,)
  - Semicolon (;)
  - Tab (\t)
  - Pipe (|)
- **Additional Options:**
  - Include/exclude headers
  - Quote handling
  - Date format selection

### 5. SQL INSERT Generation
- **Features:**
  - Generate INSERT statements from SELECT results
  - Batch INSERT support (configurable batch size)
  - Proper value escaping and quoting
  - NULL handling
  - Date/Time formatting for SQL
- **Output:**
  - .sql file with executable INSERT statements
  - Optional table name specification
  - Transaction wrapper option

## Project Structure (Refactored v1.1)

```
src/main/java/querycraft/
├── QueryCraftApp.java             # Entry point
├── connection/                    # Connection Logic
│   ├── DatabaseConnectionService.java
│   └── PooledConnectionManager.java
├── query/                         # Query Execution Logic
│   ├── QueryExecutorService.java
│   └── StreamingQueryService.java
├── export/                        # Data Exporting
│   ├── DataExporter.java
│   └── ExporterFactory.java
├── ui/
│   ├── controller/                # App Flow Controllers
│   │   ├── MainController.java
│   │   └── QueryExecutionController.java
│   ├── dialog/                    # Specialized Dialogs
│   │   ├── ConnectionDialog.java
│   │   └── ExportDialog.java
│   └── component/                 # Reusable UI widgets
│       ├── SidebarSection.java
│       └── ResultTableSection.java
└── util/                          # Validation & Shared Helpers
```

### 6. Batch Action Mode (New)
- **Goal:** Automate the "Select -> Export -> Cleanup" workflow.
- **Components:**
  - Dual Editor UI
  - Sequential execution logic
  - Data integrity fallback (Archive before delete)

## Dependencies (pom.xml)

```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>
    
    <!-- JDBC Drivers -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.3.0</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.2</version>
    </dependency>
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <version>12.6.1.jre11</version>
    </dependency>
    
    <!-- CSV Processing -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-csv</artifactId>
        <version>1.10.0</version>
    </dependency>
</dependencies>
```

## UI Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  QueryCraft - Database Query Tool                    [Connect ▼] │
├──────────────────────────────────────────────────────────────────┤
│  Database: [MySQL ▼]  Status: [● Connected]  DB: my_database     │
├──────────────────────────────────────────────────────────────────┤
│  Query:                                                           │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ SELECT * FROM users WHERE created_at > '2024-01-01'        │  │
│  │                                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│  [Execute SELECT]  [Execute DELETE]                               │
├──────────────────────────────────────────────────────────────────┤
│  Results (150 rows):                                              │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ id │ username │ email          │ created_at     │ status  │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │ 1  │ john_doe │ john@mail.com  │ 2024-01-15     │ active  │  │
│  │ 2  │ jane_smith│ jane@mail.com │ 2024-02-01     │ active  │  │
│  │ ...                                                        │  │
│  └────────────────────────────────────────────────────────────┘  │
│  [< Prev] Page 1 of 15 [Next >]                                   │
├──────────────────────────────────────────────────────────────────┤
│  Export Options:                                                  │
│  Format: [CSV ▼]  Encoding: [UTF-8 ▼]  Delimiter: [Comma ▼]     │
│  [Export to CSV]  [Generate SQL INSERTs]                          │
└──────────────────────────────────────────────────────────────────┘
```

## Data Flow

1. **Connection:**
   ```
   User → Connection Dialog → ConnectionService → JDBC Driver → Database
   ```

2. **Query Execution:**
   ```
   User enters query → QueryService validates → JDBC executes → ResultSet → 
   QueryResult model → Display in TableView
   ```

3. **CSV Export:**
   ```
   QueryResult → CsvExporter (with options) → FileWriter → .csv file
   ```

4. **SQL Generation:**
   ```
   QueryResult → SqlInsertGenerator → .sql file with INSERT statements
   ```

## Security Considerations

- Passwords stored in memory only (not persisted)
- SQL injection prevention through PreparedStatement for parameterized queries
- Confirmation dialogs for destructive operations (DELETE)
- Query timeout to prevent long-running queries

## Error Handling

- Connection failures with detailed error messages
- Query syntax errors highlighting
- Export file permission errors
- Memory management for large result sets
