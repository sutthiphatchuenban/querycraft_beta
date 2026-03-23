# <img src="src/main/resources/images/logo.png" width="80" height="80" align="center"> QueryCraft - Database Query Tool

QueryCraft is a JavaFX-based desktop application for database querying with support for multiple database engines, result preview, CSV export with customizable options, and SQL INSERT statement generation.

## Features

### Database Support
- **MySQL** - MySQL 8.0+ compatible
- **PostgreSQL** - PostgreSQL 12+ compatible
- **Microsoft SQL Server** - SQL Server 2016+ compatible

### Query Operations
- **SELECT Queries** - Execute and preview results in a table view
- **DELETE Queries** - Execute with confirmation dialog and show affected rows
- **Safety Features** - Query validation and confirmation for destructive operations

### Export Capabilities
- **CSV Export** with options:
  - Format: Standard CSV or Excel-compatible
  - Encoding: UTF-8, UTF-8 with BOM, TIS-620, Windows-874
  - Delimiter: Comma, Semicolon, Tab, Pipe
  - Include/exclude headers
  - Quote handling options
  - Custom date format

- **SQL INSERT Generation**:
  - Generate executable INSERT statements from query results
  - Transaction wrapper support
  - Proper value escaping and quoting
  - NULL handling

## Project Structure (Refactored for OOP)

```
src/main/java/querycraft/
├── QueryCraftApp.java              # Application entry point
├── dialect/                        # Database-specific SQL dialects (Strategy Pattern)
│   ├── DatabaseDialect.java        # Dialect interface
│   ├── MySqlDialect.java           # MySQL implementation
│   ├── PostgreSqlDialect.java      # PostgreSQL implementation
│   └── SqlServerDialect.java       # SQL Server implementation
├── model/                          # Data models and Enums
│   ├── DatabaseType.java           # Refactored Enum using Dialects
│   ├── ConnectionInfo.java         # Connection parameters model
│   ├── ColumnInfo.java             # Column metadata model
│   ├── QueryResult.java            # Query result wrapper
│   └── ExportOptions.java          # CSV export options
├── service/                        # Business logic and services
│   ├── ConnectionObserver.java     # Observer interface for DB events
│   ├── DatabaseConnectionService.java # Refactored using Observer pattern
│   ├── QueryExecutorService.java      # Refactored using Handler pattern
│   └── handler/                    # SQL Command handlers (Command Pattern)
│       ├── QueryHandler.java       # Handler interface
│       ├── SelectHandler.java      # Handles SELECT/SHOW/DESC queries
│       ├── UpdateHandler.java      # Handles INSERT/UPDATE/DELETE queries
│       └── GenericHandler.java     # Fallback query handler
├── ui/                             # JavaFX UI layer
│   ├── MainController.java         # Main UI controller (Observer)
│   ├── ConnectionDialog.java       # Database connection dialog
│   ├── SqlEditor.java              # Enhanced SQL editor with highlighting
│   └── component/                  # Reusable UI components
│       ├── SidebarSection.java     # Refactored Sidebar (Observer)
│       ├── QueryEditorSection.java # Shared query editor component
│       └── ResultTableSection.java # Enhanced result table with Factory export
└── util/                           # Utilities and Patterns
    ├── DataExporter.java           # Exporter interface (Strategy)
    ├── ExporterFactory.java        # Factory for creating exporters
    ├── CsvExporter.java            # Refactored CSV export strategy
    └── SqlInsertGenerator.java     # Refactored SQL INSERT strategy
```

## Requirements

- Java 17 or higher
- Maven (for building)
- One of the supported databases:
  - MySQL Server
  - PostgreSQL Server
  - Microsoft SQL Server

## Building the Project

### Using Maven

```bash
mvn clean package
```

This will create an executable JAR file in the `target/` directory.

### Running the Application

#### Option 1: Using Maven
```bash
mvn javafx:run
```

#### Option 2: Using the JAR file
```bash
java -jar target/querycraft-1.0-SNAPSHOT.jar
```

#### Option 3: On Windows (run.bat)
```batch
run.bat
```

## Usage Guide

### 1. Connect to Database

1. Click the **"Connect..."** button
2. Select your database type (MySQL, PostgreSQL, or MSSQL)
3. Enter connection details:
   - Host: Database server address (e.g., `localhost`)
   - Port: Database server port (auto-filled based on database type)
   - Database: Database name
   - Username: Your database username
   - Password: Your database password
4. Click **"Test Connection"** to verify (optional)
5. Click **"Connect"** to establish the connection

### 2. Execute Queries

#### SELECT Query
1. Enter your SQL query in the text area
   ```sql
   SELECT * FROM users LIMIT 100;
   ```
2. Click **"Execute SELECT"**
3. View results in the table below

#### DELETE Query
1. Enter your DELETE statement
   ```sql
   DELETE FROM users WHERE status = 'inactive';
   ```
2. Click **"Execute DELETE"**
3. Confirm the operation in the dialog
4. View the number of affected rows

### 3. Export Results

#### Export to CSV
1. Execute a SELECT query
2. Click **"Export to CSV..."**
3. Configure export options:
   - Format: Standard or Excel-compatible
   - Encoding: Choose appropriate character encoding
   - Delimiter: Select field separator
   - Date Format: Customize date display
   - Include Header: Toggle column names in output
   - Quote All Values: Force quotes on all fields
4. Select save location
5. Click **"Export"**

#### Generate SQL INSERTs
1. Execute a SELECT query
2. Click **"Generate SQL INSERTs..."**
3. Enter the target table name
4. Select save location for the `.sql` file
5. The generated file contains executable INSERT statements

## Sample Queries

```sql
-- Basic SELECT
SELECT * FROM customers;

-- SELECT with conditions
SELECT id, name, email FROM users WHERE active = 1;

-- DELETE with conditions
DELETE FROM logs WHERE created_at < '2024-01-01';

-- Join query
SELECT u.name, o.order_date, o.total
FROM users u
JOIN orders o ON u.id = o.user_id;
```

## Safety Features

- **Query Validation**: Basic validation before execution
- **DELETE Confirmation**: Confirmation dialog for DELETE operations
- **Connection Status**: Visual indicator of connection status
- **Row Limits**: SELECT queries limited to 10,000 rows for safety

## Troubleshooting

### Connection Issues
- Verify database server is running
- Check firewall settings
- Verify username/password
- Ensure database name is correct

### Driver Not Found
If you see "JDBC Driver not found" error, ensure the Maven dependencies are downloaded:
```bash
mvn dependency:resolve
```

### JavaFX Issues
If you encounter JavaFX-related errors, ensure you're using Java 17+ with JavaFX support.

## License

This project is provided as-is for educational and development purposes.

## Author

QueryCraft Development Team

---

## 🇹🇭 คู่มือการใช้งาน (Thai Guide)

### 1. การเตรียมความพร้อมก่อนใช้งาน
*   **Java Runtime:** เวอร์ชัน 17 ขึ้นไป
*   **JDBC Drivers:** หากรันผ่าน Source Code ให้รันไฟล์ `download-drivers.bat` ก่อน

### 2. วิธีเริ่มใช้งานโปรแกรม
*   **สำหรับผู้ใช้ทั่วไป:** แตกไฟล์ `QueryCraft.zip` และรันไฟล์ `QueryCraft.exe` ในโฟลเดอร์ `build_output/QueryCraft`
*   **รันผ่าน Script:** ดับเบิลคลิกไฟล์ `launcher-clean-build.bat`

### 3. การเชื่อมต่อฐานข้อมูล
1.  กดปุ่ม **"Connect..."**
2.  เลือกประเภทฐานข้อมูล (**MySQL, PostgreSQL, MS SQL Server**)
3.  กรอกรายละเอียด Host, Port, Database, Username และ Password
4.  กด **"Connect"** เพื่อเข้าสู่ระบบ

### 4. การรัน Query
*   **SELECT:** พิมพ์คำสั่งแล้วกด **"Execute SELECT"** เพื่อดูข้อมูลในตาราง
*   **DELETE:** พิมพ์คำสั่งลลข้อมูลแล้วกด **"Execute DELETE"** (ระบบจะถามยืนยันอีกครั้งเพื่อความปลอดภัย)

### 5. การส่งออกข้อมูล (Export)

#### **ส่งออกเป็น CSV**
1.  กดปุ่ม **"Export to CSV..."**
2.  ตั้งค่า **Encoding** (แนะนำ **TIS-620** หรือ **Windows-874** สำหรับ Excel ภาษาไทย)
3.  เลือกตัวคั่น (Delimiter) และที่เก็บไฟล์ แล้วกด **"Export"**

#### **สร้าง SQL INSERTs**
1.  กดปุ่ม **"Generate SQL INSERTs..."**
2.  ระบุชื่อตารางปลายทาง และเลือกที่เก็บไฟล์ `.sql`

### 6. ระบบความปลอดภัย
*   จำกัดการดึงข้อมูลสูงสุด 10,000 แถว
*   บล็อกคำสั่ง `DROP` และ `TRUNCATE` เพื่อป้องกันความผิดพลาด
*   มีระบบยืนยันตัวตนและการรันคำสั่งลบข้อมูล

