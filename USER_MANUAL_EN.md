# QueryCraft v1.1.0 - Comprehensive User Manual

<img src="src/main/resources/images/logo.png" width="100" height="100" align="center">

## Table of Contents
1. [Introduction](#1-introduction)
2. [System Requirements](#2-system-requirements)
3. [Installation & Setup](#3-installation--setup)
4. [User Interface Overview](#4-user-interface-overview)
5. [Connecting to Databases](#5-connecting-to-databases)
6. [Writing & Executing Queries](#6-writing--executing-queries)
7. [Working with Results](#7-working-with-results)
8. [Data Export](#8-data-export)
9. [Batch Processing Mode](#9-batch-processing-mode)
10. [Settings & Configuration](#10-settings--configuration)
11. [Safety Features](#11-safety-features)
12. [Troubleshooting](#12-troubleshooting)
13. [Keyboard Shortcuts](#13-keyboard-shortcuts)

---

## 1. Introduction

QueryCraft is a high-performance, JavaFX-based desktop application designed for database querying, data exploration, and export. It provides a robust interface for interacting with multiple database engines, supporting large dataset handling through advanced features like dynamic row limiting and streaming mode.

### Key Capabilities
- **Multi-Database Support**: MySQL, PostgreSQL, Microsoft SQL Server, and CSV files
- **Advanced Query Execution**: Standard and streaming modes for handling datasets from hundreds to millions of rows
- **Data Export**: CSV export with customizable encoding and SQL INSERT statement generation
- **Batch Processing**: Automated "Select → Export → Delete" workflow for data archiving
- **Security**: Built-in SQL validation, confirmation dialogs for destructive operations

---

## 2. System Requirements

### Minimum Requirements
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **Java Runtime**: JDK 17 or higher
- **Memory**: 4 GB RAM (8 GB recommended for large datasets)
- **Disk Space**: 200 MB for application, additional space for exports
- **Display**: 1280x720 resolution or higher

### Recommended Requirements
- **Memory**: 8 GB RAM or more
- **Display**: 1920x1080 resolution
- **Internet**: Required for downloading JDBC drivers (first run)

---

## 3. Installation & Setup

### 3.1 Windows Installation

1. **Download** the QueryCraft distribution package
2. **Extract** to your desired location (e.g., `C:\Program Files\QueryCraft`)
3. **Run** `run.bat` to start the application
4. **Optional**: Create a desktop shortcut to `run.bat`

### 3.2 Building from Source

```bash
# Clone the repository
git clone https://github.com/sutthiphatchuenban/querycraft_beta.git
cd querycraft

# Build the project
mvn clean package

# Run the application
mvn javafx:run
```

### 3.3 First Launch

When you first start QueryCraft:
1. A splash screen appears while the application initializes
2. Database drivers are loaded automatically
3. The main window opens with default settings

---

## 4. User Interface Overview

The QueryCraft interface is divided into several key areas:

```
┌──────────────────────────────────────────────────────────────────┐
│  [Connect...] [Disconnect] [Help] [Settings]        Status: Ready │
├──────────┬───────────────────────────────────────────────────────┤
│          │  SQL Query Editor:                                     │
│  TABLES  │  ┌─────────────────────────────────────────────────┐ │
│  ──────  │  │ SELECT * FROM users WHERE status = 'active'     │ │
│  users   │  │                                                 │ │
│  orders  │  └─────────────────────────────────────────────────┘ │
│  ...     │  [Execute Query] [Clear All] [Format SQL]            │
│          │                                                        │
│  Recent  │  Results (150 rows):                                   │
│  Queries │  ┌─────────────────────────────────────────────────┐ │
│  ──────  │  │ id │ name  │ email          │ status │ created  │ │
│  Query 1 │  ├─────────────────────────────────────────────────┤ │
│  Query 2 │  │ 1  │ John  │ john@mail.com  │ active │ 2024-01  │ │
│  ...     │  └─────────────────────────────────────────────────┘ │
│          │  [Export CSV] [Generate SQL]                         │
└──────────┴───────────────────────────────────────────────────────┘
```

### 4.1 Top Bar
- **Connect...**: Opens the connection dialog
- **Disconnect**: Closes the current database connection
- **Help**: Opens the built-in documentation
- **Settings**: Configures application preferences
- **Status Display**: Shows connection info and current database

### 4.2 Left Sidebar
- **Tables List**: Shows all available tables in the connected database
- **Recent Queries**: History of executed queries (last 50)

### 4.3 Center Area
- **SQL Editor**: Write and edit SQL queries with syntax highlighting
- **Results Table**: Displays query results with pagination and filtering

### 4.4 Bottom Bar
- **Status Label**: Shows execution status and messages
- **Version Info**: Current application version

---

## 5. Connecting to Databases

### 5.1 Supported Database Types

| Database | Default Port | Driver | Special Features |
|----------|-------------|--------|------------------|
| MySQL | 3306 | com.mysql.cj.jdbc.Driver | LIMIT clause, backtick escaping |
| PostgreSQL | 5432 | org.postgresql.Driver | CTE support, ILIKE operator |
| SQL Server | 1433 | com.microsoft.sqlserver.jdbc.SQLServerDriver | TOP clause, Windows Auth |
| CSV (H2) | N/A | org.h2.Driver | File-based, auto-detect encoding |

### 5.2 Creating a Connection

1. Click **Connect...** in the top bar
2. Select your database type from the dropdown
3. Fill in the connection parameters:
   - **Host**: Server address (e.g., `localhost`, `192.168.1.100`)
   - **Port**: Server port (auto-filled with default)
   - **Database**: Database name
   - **Username**: Your database username
   - **Password**: Your database password
4. Optional: Check "Use SSL" for encrypted connections
5. Optional: Check "Remember Connection" to save settings
6. Click **Test Connection** to verify
7. Click **Connect** to establish the connection

### 5.3 MySQL Connection

**Standard Connection:**
- Host: `localhost` or your server IP
- Port: `3306` (default)
- Database: Your database name
- Username: Your MySQL username
- Password: Your MySQL password

**Cloud MySQL (Neon.tech, PlanetScale):**
- Enable **Use SSL** checkbox
- Use the hostname provided by your cloud provider
- Port is typically `3306` or specified by provider

**Sample Connection String:**
```
jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
```

### 5.4 PostgreSQL Connection

**Standard Connection:**
- Host: `localhost` or your server address
- Port: `5432` (default)
- Database: Your database name
- Username: Your PostgreSQL username
- Password: Your PostgreSQL password

**Connection Notes:**
- PostgreSQL is stricter with quoted identifiers (case-sensitive)
- Use `ILIKE` for case-insensitive text matching
- CTEs (Common Table Expressions) with `WITH` clause are supported

### 5.5 SQL Server Connection

**Standard Connection:**
- Host: `localhost` or your server address
- Port: `1433` (default)
- Database: Your database name
- Username: SQL Server authentication username
- Password: SQL Server authentication password

**Windows Authentication:**
- Check "Use Windows Authentication"
- Username and Password fields become disabled
- Requires `mssql-jdbc_auth-12.6.1.x64.dll` in application directory

**Named Pipes:**
- Check "Use Named Pipes"
- Enter Instance Name (e.g., `MSSQLSERVER`, `SQLEXPRESS`)
- Port is not used in Named Pipes mode

### 5.6 CSV File Connection

1. Select **CSV File (H2)** as database type
2. Click **Browse...** to select a folder containing CSV files
3. All `.csv` files in the folder will be loaded as tables
4. Click **Connect**

**Important CSV Rules:**
- **Always use double quotes** around table and column names
- File names become table names (e.g., `customers.csv` → `"customers"`)
- First row is assumed to be header (column names)
- H2 auto-detects UTF-8, TIS-620, Windows-874 encodings
- Supported delimiters: comma, semicolon, tab

**Example CSV Query:**
```sql
SELECT * FROM "customers" WHERE "country" = 'Thailand';

SELECT c."name", o."order_date"
FROM "customers" c
JOIN "orders" o ON c."id" = o."customer_id";
```

### 5.7 Managing Recent Connections

- Up to **20 recent connections** are saved
- Select from the "Recent" dropdown to auto-fill connection details
- Click **Delete** to remove a saved connection
- Passwords are obfuscated (Base64) for basic security

---

## 6. Writing & Executing Queries

### 6.1 SQL Editor Features

The SQL editor provides:
- **Syntax highlighting** for SQL keywords
- **Auto-completion** for table names and SQL keywords
- **Line numbers** for easier navigation
- **Multiple query support** (separate with semicolons)

### 6.2 Executing Queries

**Standard Execution:**
1. Type your SQL query in the editor
2. Press **Ctrl+Enter** (or Cmd+Enter on Mac)
3. Results appear in the table below

**With Parameters:**
1. Write SQL with named parameters using `:paramName` syntax
2. Press **Ctrl+Enter**
3. A parameter dialog appears asking for values
4. Enter values and click OK to execute

**Example Parameterized Query:**
```sql
SELECT * FROM customers 
WHERE country = :country 
  AND created_at >= :startDate;
```

### 6.3 Query Types

**SELECT Queries:**
- Display results in a paginated table
- Row count shown in status bar
- Results can be filtered, exported, or copied

**INSERT/UPDATE/DELETE Queries:**
- Show affected row count
- Confirmation dialog appears for DELETE operations
- No result table displayed

### 6.4 Streaming Mode

For large datasets (100,000+ rows):

1. Click the **Streaming: OFF** button to toggle to **Streaming: ON**
2. Execute your SELECT query
3. Results stream in progressively
4. Memory usage remains constant regardless of result size

**When to Use Streaming:**
- Querying tables with >50,000 rows
- Exporting large datasets
- Preventing OutOfMemory errors

### 6.5 SQL Formatting

Press **Ctrl+F** to auto-format your SQL:
```sql
-- Before formatting:
select id,name,email from users where status='active' and created_at>'2024-01-01';

-- After formatting:
SELECT id, name, email
FROM users
WHERE status = 'active'
  AND created_at > '2024-01-01';
```

### 6.6 Working with the Sidebar

**Table List:**
- **Double-click** a table: Generates `SELECT * FROM table LIMIT 100`
- **Right-click** → "Describe Structure": Shows column information
- **Right-click** → "SELECT * (Top 100)": Same as double-click

**Recent Queries:**
- **Double-click** a query: Loads it into the editor
- Keeps last 50 executed queries

---

## 7. Working with Results

### 7.1 Result Table Features

- **Pagination**: Navigate through large result sets (100 rows per page)
- **Column Sorting**: Click column headers to sort
- **Column Resizing**: Drag column borders to resize
- **Selection**: Click cells to select; use Ctrl+Click for multiple
- **Copy**: Select cells and press Ctrl+C to copy

### 7.2 Filtering Results

1. Type in the **Filter Results** field above the table
2. Filtering happens automatically after a short delay
3. Only rows containing the filter text are displayed
4. Filtering is case-insensitive and searches all columns

### 7.3 Pagination Controls

- **Previous**: Go to previous page
- **Next**: Go to next page
- Page info shows: `Showing X-Y of Z matches (Page N of M)`

### 7.4 Truncation Warning

If your query result exceeds the maximum row limit:
- A yellow warning banner appears: `[!] RESULTS TRUNCATED`
- Enable Streaming Mode to see all rows
- Or increase the Max Rows limit in Settings

---

## 8. Data Export

### 8.1 Export to CSV

1. Execute a SELECT query
2. Click **Export to CSV...** button
3. Configure export options:
   - **Format**: Standard CSV or Excel Compatible
   - **Encoding**: UTF-8, UTF-8 with BOM, TIS-620, Windows-874, etc.
   - **Delimiter**: Comma, Semicolon, Tab, Pipe, or Custom
   - **Include Header**: Include column names in first row
   - **Quote All Values**: Wrap all values in quotes
   - **Date Format**: Customize date/time formatting
4. Select destination file
5. Click **Export**

### 8.2 Generate SQL INSERT Statements

1. Execute a SELECT query
2. Click **Generate SQL INSERTs...** button
3. Enter the target table name (e.g., `archived_customers`)
4. Select destination file
5. Click **Save**

The generated SQL file includes:
- Header comments with metadata
- Transaction wrapper (BEGIN/COMMIT)
- Individual INSERT statements for each row
- Database-specific syntax adjustments

### 8.3 Export Options Reference

| Option | Values | Description |
|--------|--------|-------------|
| Format | Standard, Excel | Excel format adds BOM for Excel compatibility |
| Encoding | UTF-8, UTF-8 BOM, TIS-620, Windows-874, UTF-16LE, ASCII, ISO-8859-1 | Character encoding for output file |
| Delimiter | Comma, Semicolon, Tab, Pipe, Other | Field separator character |
| Include Header | On/Off | Include column names as first row |
| Quote All | On/Off | Wrap all values in quotes |
| Date Format | Pattern | Java SimpleDateFormat pattern |

---

## 9. Batch Processing Mode

Batch Processing automates the "Select → Export → Delete" workflow for data archiving.

### 9.1 Enabling Batch Mode

1. Click the **Batch Action Mode** toggle in the query editor
2. Two editors appear:
   - **Top**: SELECT query to identify records for archiving
   - **Bottom**: DELETE query to remove archived records

### 9.2 Batch Process Workflow

1. **Write SELECT Query**: Defines records to archive
   ```sql
   SELECT * FROM logs WHERE created_at < '2023-01-01';
   ```

2. **Write DELETE Query**: Defines records to remove
   ```sql
   DELETE FROM logs WHERE created_at < '2023-01-01';
   ```

3. **Click Process Batch** button
4. System estimates row count
5. Configure export (CSV file location)
6. For databases: Optionally generate SQL INSERT file
7. System exports data to CSV
8. System executes DELETE query
9. Summary dialog shows results

### 9.3 Batch Process Safety

- Row count estimation before execution
- Export must succeed before DELETE runs
- Confirmation dialog before starting
- Partial success handling if DELETE fails after export

---

## 10. Settings & Configuration

### 10.1 Query Settings Tab

**Query Timeout (seconds):**
- Range: 5-300 seconds
- Default: 30 seconds
- How long to wait for query execution before canceling

**Max Rows Limit:**
- Range: 100-50,000 rows
- Default: 10,000 rows
- Maximum rows to fetch per query (normal mode)

**Auto-format SQL on paste:**
- Automatically formats SQL when pasted into editor

**Confirm DELETE operations:**
- Show confirmation dialog before executing DELETE statements

### 10.2 Connection Settings Tab

**Max Pool Size:**
- Range: 2-50 connections
- Default: 10 connections
- Maximum connections in the connection pool

**Connection Timeout (seconds):**
- Range: 5-120 seconds
- Default: 30 seconds
- Timeout for establishing new connections

**Enable SSL by default:**
- Enable SSL for new connections by default

### 10.3 Saving Settings

Settings are automatically saved to:
- **Windows**: Windows Registry (`HKEY_CURRENT_USER\Software\JavaSoft\Prefs\querycraft`)
- **macOS**: `~/Library/Preferences/com.querycraft.plist`
- **Linux**: `~/.java/.userPrefs/querycraft/`

---

## 11. Safety Features

### 11.1 SQL Validation

QueryCraft validates all queries before execution and blocks:
- `DROP TABLE`, `DROP DATABASE`
- `TRUNCATE TABLE`
- `ALTER DATABASE`, `ALTER SYSTEM`
- `GRANT ALL`, `REVOKE ALL`
- `SHUTDOWN`, `KILL`

### 11.2 Confirmation Dialogs

**DELETE Operations:**
- Always shows confirmation dialog
- Warning: "This operation is a DELETE statement and will permanently modify data"

**UPDATE/INSERT Operations:**
- Shows confirmation for non-SELECT operations
- Warning: "This operation will modify data in the database"

### 11.3 Query Timeout

- Queries automatically timeout after configured duration
- Prevents runaway queries from hanging the application
- Streaming mode has separate timeout (60 seconds)

### 11.4 Row Limiting

- Default limit of 10,000 rows prevents accidental massive queries
- Truncation warning clearly displayed
- Streaming mode available for legitimate large queries

### 11.5 Password Security

- Passwords are **never saved to disk**
- Stored only in memory during session
- Recent connections save other details but require password re-entry

---

## 12. Troubleshooting

### 12.1 Connection Issues

**"Connection refused" Error:**
- Verify database server is running
- Check host and port are correct
- Verify firewall allows connection
- For cloud databases, check IP allow-list

**"Access denied" Error:**
- Check username and password
- Verify user has permission for the database
- For SQL Server, verify SQL authentication is enabled

**SSL Errors:**
- Enable "Use SSL" option for cloud databases
- Check server's SSL certificate is valid
- Try disabling SSL only for local development

### 12.2 Query Issues

**"Query timeout" Error:**
- Increase timeout in Settings
- Add LIMIT/TOP clause to reduce result size
- Optimize query with indexes

**"Out of Memory" Error:**
- Enable Streaming Mode for large results
- Reduce Max Rows limit
- Increase Java heap size: edit `run.bat` and add `-Xmx4G`

**"Invalid Query" Error:**
- Check SQL syntax for your database type
- Verify table and column names exist
- Check for reserved keywords

### 12.3 CSV Issues

**"Table not found" Error:**
- Verify CSV file exists in selected folder
- Use double quotes around table names: `"customers"`
- Check file extension is `.csv`

**"Garbled text" in CSV:**
- Verify source file encoding
- H2 auto-detects UTF-8, TIS-620, Windows-874
- Try converting file to UTF-8 before loading

### 12.4 Application Issues

**Application won't start:**
- Verify Java 17+ is installed: `java -version`
- Check `run.bat` exists and is executable
- Review console output for errors

**UI looks strange:**
- Ensure display scaling is set to 100% or 125%
- Update graphics drivers
- Try maximizing the window

---

## 13. Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+Enter | Execute current query |
| Ctrl+F | Format SQL |
| Ctrl+C | Copy selected cells |
| Escape | Clear selection |

---

## Appendix A: SQL Dialect Differences

| Feature | MySQL | PostgreSQL | SQL Server | CSV (H2) |
|---------|-------|------------|------------|----------|
| Row Limiting | `LIMIT n` | `LIMIT n` | `TOP n` | `LIMIT n` |
| Identifier Quotes | `` `name` `` | `"name"` | `[name]` | `"name"` |
| String Concatenation | `CONCAT()` | `\|\|` | `+` | `\|\|` |
| Current Date | `CURDATE()` | `CURRENT_DATE` | `GETDATE()` | `CURRENT_DATE()` |
| Case-Insensitive Like | `LIKE` | `ILIKE` | `LIKE` | `LIKE` |
| Boolean | `1/0` | `true/false` | `1/0` | `TRUE/FALSE` |

---

## Appendix B: Supported SQL Functions (CSV/H2 Mode)

**Aggregate Functions:**
- `COUNT()`, `SUM()`, `AVG()`, `MAX()`, `MIN()`

**String Functions:**
- `UPPER()`, `LOWER()`, `CONCAT()`, `SUBSTRING()`

**Date Functions:**
- `CURRENT_DATE()`, `CURRENT_TIMESTAMP()`

**Math Functions:**
- `ROUND()`, `ABS()`, `MOD()`

**Conditional Functions:**
- `COALESCE()`, `NULLIF()`, `CASE WHEN`

---

**Document Version**: 1.1.0  
**Last Updated**: March 2026  
**Application**: QueryCraft Database Query Tool
