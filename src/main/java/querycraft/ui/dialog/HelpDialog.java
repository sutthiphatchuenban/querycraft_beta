package querycraft.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;

/**
 * Help/Documentation dialog showing correct usage for each database type.
 */
public class HelpDialog extends Dialog<Void> {

    public HelpDialog() {
        setTitle("QueryCraft - Documentation");
        initModality(Modality.APPLICATION_MODAL);
        
        // Create tab pane for different sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefSize(700, 550);
        
        // Overview Tab
        Tab overviewTab = new Tab("Overview", createOverviewContent());
        overviewTab.setClosable(false);
        
        // MySQL Tab
        Tab mysqlTab = new Tab("MySQL", createMySqlContent());
        mysqlTab.setClosable(false);
        
        // PostgreSQL Tab
        Tab postgresTab = new Tab("PostgreSQL", createPostgreSqlContent());
        postgresTab.setClosable(false);
        
        // SQL Server Tab
        Tab mssqlTab = new Tab("SQL Server", createSqlServerContent());
        mssqlTab.setClosable(false);
        
        // CSV Tab
        Tab csvTab = new Tab("CSV (H2)", createCsvContent());
        csvTab.setClosable(false);
        
        tabPane.getTabs().addAll(overviewTab, mysqlTab, postgresTab, mssqlTab, csvTab);
        
        // Close button
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(closeButton);
        
        getDialogPane().setContent(tabPane);
        getDialogPane().setPrefSize(720, 600);
    }

    private ScrollPane createOverviewContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("help-content");
        
        content.getChildren().addAll(
            createTitle("QueryCraft - Database Query Tool"),
            createParagraph("QueryCraft is a desktop application for querying databases with support for multiple database engines, " +
                "result preview, CSV export, and SQL generation."),
            createParagraph("This help section is intended to be practical, not just descriptive. It explains how to connect, how to write queries safely, how each database behaves, what QueryCraft can and cannot do, and what patterns you should follow to avoid common errors during use."),
            createParagraph("If you are new to the system, read this Overview tab first. This page explains the whole system in business terms: what the application is for, what each major area of the screen does, how data flows from connection to result view, what tools are built into the application, and when each feature should be used."),
             
            createSubtitle("Supported Database Types"),
            createBulletList(
                "MySQL 8.0+ - MySQL Community Server or MariaDB",
                "PostgreSQL 12+ - Open source relational database",
                "Microsoft SQL Server 2016+ - Enterprise database",
                "CSV Files (H2) - Query CSV files using SQL syntax"
            ),

            createSubtitle("System Overview"),
            createParagraph("QueryCraft is essentially a lightweight SQL workbench for day-to-day data work. The system is designed for people who need to connect to a data source, inspect tables, write queries, review results quickly, and export those results in a usable format. It is intentionally focused on practical querying rather than deep administration tasks such as schema migration, database tuning, or production operations."),

            createSubtitle("What the System Contains"),
            createDetailedList(
                "Connection System", "Used to connect the application to a database server or to a folder of CSV files. This is the starting point of the whole workflow. Without a connection, query execution and table browsing are disabled.",
                "Table Explorer", "Shows discovered tables on the left side. It helps users understand what data exists before writing SQL manually. Tables can be opened quickly or their structure can be inspected.",
                "SQL Editor", "The main workspace where users write queries. It supports syntax highlighting, simple autocomplete, formatting assistance, and parameterized query usage.",
                "Query Execution Engine", "Receives SQL from the editor, validates it for safety, selects the proper execution strategy, and returns either rows or affected-row counts.",
                "Prepared Parameter System", "Supports named parameters such as :customerId or :startDate so users can reuse SQL more safely and fill in values through a dialog instead of editing the SQL every time.",
                "Result Viewer", "Displays returned rows in a table, supports paging, filtering, selection copying, and acts as the central place for reviewing query output before export.",
                "Export System", "Takes query results and writes them out either as CSV files or as generated SQL INSERT scripts for migration, backup, or reuse in another environment.",
                "Settings System", "Lets users control timeout behavior and connection-related runtime settings so the application can be tuned to different workloads.",
                "Documentation System", "The help dialog itself is part of the product. It documents supported platforms, query examples, safe usage patterns, and special notes for each data source type."
            ),

            createSubtitle("What QueryCraft Is Best For"),
            createBulletList(
                "Exploring tables and previewing data quickly",
                "Running SELECT statements for inspection and reporting",
                "Testing filters, joins, aggregations, and export workflows",
                "Working with CSV files using SQL without importing them into a full database first",
                "Generating SQL INSERT scripts from result sets for migration or seeding"
            ),

            createSubtitle("What QueryCraft Is Not Designed For"),
            createBulletList(
                "Heavy database administration tasks",
                "Schema design or migration management",
                "Large-scale ETL workloads",
                "Editing millions of rows interactively",
                "Replacing a full database IDE for advanced debugging, profiling, or execution plan analysis"
            ),
            
            createSubtitle("Key Features"),
            createBulletList(
                "Execute SELECT queries with result preview in table view",
                "Execute DELETE queries with confirmation dialog",
                "Export results to CSV with customizable encoding (UTF-8, TIS-620, Windows-874)",
                "Generate SQL INSERT statements from query results",
                "Syntax highlighting SQL editor with autocomplete",
                "JOIN multiple CSV files as if they were database tables"
            ),

            createSubtitle("What Each Major Screen Area Does"),
            createDetailedList(
                "Top Bar", "Contains the main system actions such as connect, disconnect, help, and settings. It also shows a compact connection summary so the user always knows which source is currently active.",
                "Left Sidebar", "Contains two practical sections: a table list and recent query history. The table list is used for discovery and quick query generation. The history list is used to revisit previously executed SQL without rewriting it.",
                "Editor Area", "This is the command center of the application. Users type SQL here, review syntax coloring, format statements, and run SELECT or DELETE-oriented actions from the action buttons.",
                "Result Area", "Displays the result of the most recent execution. For SELECT queries this means rows and columns. For write operations this means the number of affected rows and execution feedback.",
                "Filter and Pagination Controls", "These controls make large results easier to inspect. Instead of scrolling blindly through everything, users can search within loaded results and move through them page by page.",
                "Export Buttons", "These actions convert the current result set into a file. They should be used only after you have verified that the result data is correct."
            ),
             
            createSubtitle("Safety Features"),
            createBulletList(
                "Query validation before execution",
                "DROP and TRUNCATE operations are blocked",
                "Confirmation dialog for DELETE operations",
                "Row limit for SELECT queries (10,000 rows max)"
            ),

            createSubtitle("How the System Works End to End"),
            createDetailedList(
                "Step 1 - Connect", "The user opens the connection dialog, chooses a source type, fills in the connection information, optionally tests the connection, and then connects.",
                "Step 2 - Discover Data", "After connection, the system loads available tables into the sidebar. This gives the user a quick map of the connected data source.",
                "Step 3 - Write SQL", "The user writes or edits SQL in the editor. This may be a normal SELECT statement, a filtered query, a join, an aggregate, or a parameterized statement.",
                "Step 4 - Validate and Execute", "Before running the SQL, the system checks for risky patterns and determines the proper execution path. Queries are then executed through the relevant service layer.",
                "Step 5 - Review Results", "Returned rows are shown in the result section. Users can inspect columns, search within the result set, copy selections, and confirm that the output matches expectations.",
                "Step 6 - Export or Reuse", "Once the output is correct, users can export it to CSV, generate SQL INSERT statements, or reuse the SQL later from the recent history section."
            ),

            createSubtitle("Detailed Feature Explanation"),
            createDetailedList(
                "Connection Testing", "Lets users verify server reachability and credentials before opening a full working session. This reduces confusion when connection settings are wrong.",
                "Recent Connections", "Stores a small list of previously used connection definitions so users can reconnect faster. This saves time in repeated operational workflows.",
                "Recent Query History", "Remembers previously executed SQL. This is useful for repeated checks, troubleshooting, audits, or building up queries iteratively.",
                "Syntax Highlighting", "Improves readability of SQL by visually separating keywords, strings, comments, numbers, and operators. This helps reduce mistakes in longer statements.",
                "Autocomplete", "Suggests SQL keywords and known table names while typing. This speeds up writing and reduces simple typing errors.",
                "SQL Formatting", "Rearranges SQL into a cleaner structure so complex statements are easier to review before execution or sharing.",
                "Parameterized Queries", "Allows value input to be separated from SQL structure. This makes queries easier to reuse and safer for repeated manual execution.",
                "Streaming Mode", "Intended for handling larger results with a streaming execution path so the application can work more safely with heavier data loads.",
                "Client-side Result Filtering", "Lets users search within already returned data without sending another query to the database.",
                "Copy Selection", "Makes it easy to transfer selected result cells into spreadsheets, chat, tickets, or documentation.",
                "CSV Export", "Creates a file that can be used in Excel, imports, integrations, or reporting workflows. Encoding and delimiter options are provided to fit local language and tool requirements.",
                "SQL INSERT Generation", "Turns result rows into SQL INSERT statements so the same data can be moved into another environment, used for seeding, or attached to deployment tasks."
            ),

            createSubtitle("Recommended Workflow"),
            createBulletList(
                "Connect to the target database first",
                "Inspect available tables from the left sidebar",
                "Start with a small SELECT query before attempting larger joins",
                "Use LIMIT or TOP whenever possible during exploration",
                "Check results in the preview table before exporting",
                "Use the Settings dialog to control timeout and other behavior when needed"
            ),

            createSubtitle("Prepared Parameters"),
            createParagraph("QueryCraft supports named parameters using the :parameterName style. When your SQL contains named parameters, the application opens a parameter dialog and asks you to enter values before execution. This is useful when you want safer and more reusable SQL."),
            createCodeBlock(
                "SELECT *\n" +
                "FROM customers\n" +
                "WHERE customer_id = :id;\n\n" +
                "SELECT *\n" +
                "FROM orders\n" +
                "WHERE order_date >= :startDate\n" +
                "  AND order_date <= :endDate;"
            ),

            createSubtitle("When to Use Which Feature"),
            createDetailedList(
                "Use table browsing", "When you do not yet know what tables exist or when you want to start from schema discovery before writing custom SQL.",
                "Use manual SQL editing", "When you need exact control over joins, filters, ordering, aggregations, or vendor-specific syntax.",
                "Use named parameters", "When the same query structure will be reused with different values such as dates, IDs, statuses, or categories.",
                "Use result filtering", "When the query result is already loaded and you only need a quick visual search instead of issuing another database request.",
                "Use CSV export", "When the next step is spreadsheet analysis, manual review, business sharing, or import into another tool.",
                "Use SQL INSERT generation", "When the next step is moving the data into another database, preparing sample data, or creating a repeatable script artifact."
            ),

            createSubtitle("Timeout Settings"),
            createParagraph("Use the Settings button in the top bar to configure query timeout, result limits, and other runtime behavior. If a query takes too long, increase timeout carefully or optimize the SQL rather than setting extremely large timeouts immediately."),
             
            createSubtitle("Security Notes"),
            createParagraph("Passwords are stored in memory only and are not persisted to disk. " +
                "Use the 'Remember Connection' feature to save connection details (without password) for quick reconnection."),

            createSubtitle("Operational Notes and Expectations"),
            createBulletList(
                "This application is best used as a focused data access tool rather than a full database administration suite",
                "Large data exploration should begin with limited queries before exporting everything",
                "For CSV mode, think of the folder as a temporary in-memory database session rather than permanent storage",
                "Always verify target rows carefully before executing DELETE statements",
                "Always review exported results before sending them onward to business or technical consumers"
            )
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("help-scroll");
        return scrollPane;
    }

    private ScrollPane createMySqlContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createTitle("MySQL / MariaDB Connection"),
            createParagraph("Use this section when connecting to MySQL-compatible servers such as local MySQL, MariaDB, or managed cloud services that provide MySQL wire protocol support."),
             
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: MySQL server address (e.g., localhost, 192.168.1.100)",
                "Port: 3306 (default for MySQL)",
                "Database: Database name to connect to",
                "Username: MySQL user with appropriate privileges",
                "Password: User password (not saved)",
                "SSL: Enable for cloud databases (Neon, PlanetScale, etc.)"
            ),

            createSubtitle("Typical Connection Examples"),
            createBulletList(
                "Local development server: localhost:3306",
                "Docker container exposed to host: 127.0.0.1:3306",
                "Remote VM or LAN server: 192.168.x.x:3306",
                "Cloud-hosted database: hostname provided by your DB vendor, often with SSL enabled"
            ),

            createSubtitle("Before You Connect"),
            createBulletList(
                "Verify that the MySQL service is running",
                "Verify the user has permission to access the selected database",
                "Confirm whether SSL is required by the server",
                "If using a cloud host, confirm firewall or allow-list rules"
            ),
            
            createSubtitle("Sample Queries"),
            createCodeBlock(
                "-- Basic SELECT\n" +
                "SELECT * FROM users LIMIT 100;\n\n" +
                "-- SELECT with conditions\n" +
                "SELECT id, name, email FROM users WHERE active = 1;\n\n" +
                "-- JOIN example\n" +
                "SELECT u.name, o.order_date, o.total\n" +
                "FROM users u\n" +
                "JOIN orders o ON u.id = o.user_id;\n\n" +
                "-- DELETE with confirmation\n" +
                "DELETE FROM logs WHERE created_at < '2024-01-01';"
            ),

            createSubtitle("Practical MySQL Query Tips"),
            createBulletList(
                "Use LIMIT during exploration to avoid loading too many rows",
                "Use ORDER BY with LIMIT if you need predictable result order",
                "If column names contain spaces or reserved words, wrap them with backticks",
                "For text search, consider LIKE '%keyword%' but remember it may be slow on large tables"
            ),
             
            createSubtitle("MySQL-Specific Features"),
            createBulletList(
                "Uses SHOW TABLES to list tables",
                "Uses DESCRIBE table_name for structure",
                "Supports LIMIT clause",
                "MySQL backtick (`) identifier escaping"
            ),

            createSubtitle("Common Problems"),
            createBulletList(
                "Access denied - wrong username/password or missing privileges",
                "Unknown database - database name is incorrect",
                "Communications link failure - host/port unreachable or service down",
                "SSL errors - server requires SSL but the option is disabled"
            ),
             
            createNote("For cloud MySQL (Neon.tech, PlanetScale), check 'Use SSL' option.")
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private ScrollPane createPostgreSqlContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createTitle("PostgreSQL Connection"),
            createParagraph("Use this section for PostgreSQL servers running locally, on a VM, in Docker, or via a cloud database provider. PostgreSQL supports rich SQL features and is often stricter than MySQL in syntax and typing."),
             
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: PostgreSQL server address (e.g., localhost, db.example.com)",
                "Port: 5432 (default for PostgreSQL)",
                "Database: Database name",
                "Username: PostgreSQL user",
                "Password: User password (not saved)",
                "SSL: Enable for cloud databases"
            ),

            createSubtitle("Before You Connect"),
            createBulletList(
                "Make sure PostgreSQL is listening on the target host and port",
                "Check pg_hba.conf or provider access rules if connection is rejected",
                "Verify the selected database exists",
                "Use SSL for managed providers when required"
            ),
             
            createSubtitle("Sample Queries"),
            createCodeBlock(
                "-- Basic SELECT\n" +
                "SELECT * FROM customers LIMIT 100;\n\n" +
                "-- SELECT with ILIKE (case-insensitive)\n" +
                "SELECT * FROM customers WHERE name ILIKE '%john%';\n\n" +
                "-- JOIN example\n" +
                "SELECT c.name, COUNT(o.id) as order_count\n" +
                "FROM customers c\n" +
                "LEFT JOIN orders o ON c.id = o.customer_id\n" +
                "GROUP BY c.id, c.name;\n\n" +
                "-- Common Table Expression (CTE)\n" +
                "WITH recent_orders AS (\n" +
                "  SELECT * FROM orders WHERE created_at > CURRENT_DATE - INTERVAL '30 days'\n" +
                ")\n" +
                "SELECT * FROM recent_orders;"
            ),

            createSubtitle("Practical PostgreSQL Query Tips"),
            createBulletList(
                "Use ILIKE for case-insensitive text matching",
                "Use CTEs to organize complex logic step by step",
                "Be careful with quoted identifiers because they become case-sensitive",
                "Use LIMIT and OFFSET for paging during analysis"
            ),
             
            createSubtitle("PostgreSQL-Specific Features"),
            createBulletList(
                "Uses information_schema for table listing",
                "Supports CTE (WITH clauses)",
                "Uses double quote (\") identifier escaping",
                "Supports LIMIT and OFFSET"
            ),

            createSubtitle("Common Problems"),
            createBulletList(
                "Password authentication failed - wrong credentials",
                "Database does not exist - incorrect database name",
                "No pg_hba.conf entry - host/user not allowed",
                "Connection timeout - network or firewall issue"
            ),
             
            createNote("PostgreSQL identifiers are case-sensitive when quoted.")
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private ScrollPane createSqlServerContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createTitle("Microsoft SQL Server Connection"),
            createParagraph("Use this section for Microsoft SQL Server instances running locally, inside your network, or through hosted SQL Server environments. SQL Server syntax differs from MySQL and PostgreSQL in several important ways."),
             
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: SQL Server address (e.g., localhost, server.example.com)",
                "Port: 1433 (default for SQL Server)",
                "Database: Database name",
                "Username: SQL Server authentication user",
                "Password: User password (not saved)",
                "SSL: Enable for encrypted connections"
            ),

            createSubtitle("Before You Connect"),
            createBulletList(
                "Confirm SQL Server service is running",
                "Ensure TCP/IP is enabled in SQL Server configuration if needed",
                "Verify SQL authentication is enabled if not using Windows-integrated access",
                "Confirm firewall allows traffic on the configured port"
            ),
             
            createSubtitle("Sample Queries"),
            createCodeBlock(
                "-- Basic SELECT with TOP\n" +
                "SELECT TOP 100 * FROM employees;\n\n" +
                "-- SELECT with conditions\n" +
                "SELECT id, name, salary FROM employees WHERE department = 'Sales';\n\n" +
                "-- JOIN example\n" +
                "SELECT e.name, d.department_name\n" +
                "FROM employees e\n" +
                "INNER JOIN departments d ON e.dept_id = d.id;\n\n" +
                "-- DELETE with confirmation\n" +
                "DELETE FROM temp_logs WHERE created_date < DATEADD(day, -30, GETDATE());"
            ),

            createSubtitle("Practical SQL Server Query Tips"),
            createBulletList(
                "Use TOP instead of LIMIT",
                "Use square brackets for reserved names or names with spaces",
                "Common date functions include GETDATE(), DATEADD(), DATEDIFF()",
                "If you need deterministic TOP results, combine TOP with ORDER BY"
            ),
             
            createSubtitle("SQL Server-Specific Features"),
            createBulletList(
                "Uses sys.objects for table listing",
                "Supports TOP clause (instead of LIMIT)",
                "Uses square bracket ([ ]) identifier escaping",
                "Supports T-SQL functions like GETDATE(), DATEADD()"
            ),

            createSubtitle("Common Problems"),
            createBulletList(
                "Login failed for user - wrong credentials or SQL auth disabled",
                "Connection refused - SQL Server not reachable or TCP disabled",
                "Database unavailable - wrong database name or lack of permission",
                "Encryption/trust errors - SSL settings may need adjustment"
            ),
             
            createSubtitle("How to Enable TCP/IP for SQL Server"),
            createParagraph("By default, SQL Server may only listen on Shared Memory or Named Pipes. " +
                "To connect from QueryCraft using Host/Port (TCP/IP mode), you must enable TCP/IP in SQL Server Configuration Manager."),
            createCodeBlock(
                "Step-by-step:\n\n" +
                "1. Open 'SQL Server Configuration Manager'\n" +
                "   - Search 'SQL Server Configuration' in Windows Start\n" +
                "   - Or run: SQLServerManager16.msc (for SQL 2022)\n" +
                "                SQLServerManager15.msc (for SQL 2019)\n" +
                "                SQLServerManager14.msc (for SQL 2017)\n\n" +
                "2. Go to: SQL Server Network Configuration > Protocols for [INSTANCE]\n\n" +
                "3. Right-click 'TCP/IP' > Enable\n\n" +
                "4. Double-click 'TCP/IP' > IP Addresses tab\n" +
                "   - Scroll to 'IPAll' section\n" +
                "   - Set 'TCP Port' = 1433 (or your preferred port)\n" +
                "   - Clear 'TCP Dynamic Ports' if you want a fixed port\n\n" +
                "5. Restart SQL Server service:\n" +
                "   - In SQL Server Configuration Manager > SQL Server Services\n" +
                "   - Right-click 'SQL Server (INSTANCE)' > Restart"
            ),

            createSubtitle("How to Check if Port is Open (PowerShell)"),
            createParagraph("Use these PowerShell commands to verify SQL Server is listening on the expected port:"),
            createCodeBlock(
                "# Check if SQL Server is listening on port 1433\n" +
                "Test-NetConnection -ComputerName localhost -Port 1433\n\n" +
                "# Check a remote server\n" +
                "Test-NetConnection -ComputerName 192.168.1.100 -Port 1433\n\n" +
                "# List all ports SQL Server is using\n" +
                "Get-NetTCPConnection -OwningProcess (Get-Process sqlservr).Id |\n" +
                "  Select-Object LocalPort, State | Sort-Object LocalPort\n\n" +
                "# Quick check from CMD\n" +
                "telnet localhost 1433"
            ),

            createSubtitle("Windows Authentication Setup"),
            createParagraph("To use 'Use Windows Authentication' checkbox in QueryCraft, no username/password is needed. " +
                "The application uses your current Windows login. Requirements:"),
            createBulletList(
                "SQL Server must have 'Windows Authentication mode' or 'Mixed Mode' enabled",
                "Your Windows user must have a login mapped in SQL Server",
                "The mssql-jdbc_auth DLL must be present (auto-downloaded to lib/ folder)",
                "TCP/IP must be enabled (Windows Auth over TCP/IP is the most stable method)"
            ),
            createCodeBlock(
                "-- To check/add your Windows login in SQL Server:\n" +
                "-- Run in SSMS or sqlcmd:\n\n" +
                "-- Check if your login exists\n" +
                "SELECT name FROM sys.server_principals WHERE type = 'U';\n\n" +
                "-- Add your Windows login (run as admin)\n" +
                "CREATE LOGIN [DOMAIN\\\\username] FROM WINDOWS;\n" +
                "USE [your_database];\n" +
                "CREATE USER [DOMAIN\\\\username] FOR LOGIN [DOMAIN\\\\username];\n" +
                "ALTER ROLE db_datareader ADD MEMBER [DOMAIN\\\\username];"
            ),

            createSubtitle("Named Pipes Setup"),
            createParagraph("If TCP/IP cannot be enabled (e.g., corporate policy), you can use Named Pipes mode. " +
                "Check 'Use Named Pipes (no TCP/IP required)' in the connection dialog. This bypasses port configuration entirely."),
            createBulletList(
                "Named Pipes must be enabled in SQL Server Configuration Manager",
                "Works only for local or same-network connections",
                "Enter the Instance Name (e.g., SQLEXPRESS, MSSQLSERVER) instead of port",
                "Combined with Windows Authentication, this requires zero password setup"
            ),
            createCodeBlock(
                "Enable Named Pipes:\n\n" +
                "1. Open SQL Server Configuration Manager\n" +
                "2. Go to: SQL Server Network Configuration > Protocols for [INSTANCE]\n" +
                "3. Right-click 'Named Pipes' > Enable\n" +
                "4. Restart SQL Server service"
            ),

            createSubtitle("Firewall Configuration"),
            createParagraph("If connecting from another machine, you must open the SQL Server port in Windows Firewall:"),
            createCodeBlock(
                "# PowerShell (Run as Administrator)\n\n" +
                "# Open port 1433 for SQL Server\n" +
                "New-NetFirewallRule -DisplayName 'SQL Server Port 1433' `\n" +
                "  -Direction Inbound -Protocol TCP -LocalPort 1433 -Action Allow\n\n" +
                "# Verify the rule was created\n" +
                "Get-NetFirewallRule -DisplayName 'SQL Server*' | Format-Table"
            ),

            createNote("SQL Server uses TOP instead of LIMIT. Use TOP 100 for row limiting.")
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private ScrollPane createCsvContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createTitle("CSV File Query (via H2 Database)"),
            createParagraph("This mode lets you query CSV files as if they were database tables. It is ideal for quick ad hoc exploration, joins between multiple CSV files, and exporting transformed data without setting up a full database server."),
             
            createSubtitle("How It Works"),
            createParagraph("CSV files are loaded into an in-memory H2 database. Each CSV file becomes a table " +
                "with the same name as the file (without .csv extension). H2 automatically detects character encoding " +
                "and delimiter from the file content."),
            
            createSubtitle("Connection Steps"),
            createBulletList(
                "Select 'CSV File (H2)' from Database Type dropdown",
                "Browse to select a FOLDER containing CSV files",
                "All .csv files in the folder will be loaded as tables",
                "Connect to start querying"
            ),

            createSubtitle("Recommended Folder Structure"),
            createBulletList(
                "Keep related CSV files in a single folder",
                "Use meaningful file names because they become table names",
                "Prefer simple file names without unusual symbols when possible",
                "Make sure the first row contains column headers"
            ),
             
            createSubtitle("Example CSV Files"),
            createCodeBlock(
                "Folder: C:\\Data\\\n" +
                "  customers.csv  → Table: \"customers\"\n" +
                "  orders.csv     → Table: \"orders\"\n" +
                "  products.csv   → Table: \"products\""
            ),
            
            createSubtitle("Query Examples"),
            createCodeBlock(
                "-- Simple SELECT from CSV\n" +
                "SELECT * FROM \"customers\";\n\n" +
                "-- SELECT with WHERE\n" +
                "SELECT * FROM \"customers\" WHERE \"country\" = 'Thailand';\n\n" +
                "-- JOIN between CSV files\n" +
                "SELECT c.\"name\", o.\"order_date\", o.\"total\"\n" +
                "FROM \"customers\" c\n" +
                "JOIN \"orders\" o ON c.\"id\" = o.\"customer_id\";\n\n" +
                "-- Aggregation\n" +
                "SELECT \"country\", COUNT(*) as count\n" +
                "FROM \"customers\"\n" +
                "GROUP BY \"country\";\n\n" +
                "-- Multiple JOINs\n" +
                "SELECT c.\"name\", p.\"product_name\", oi.\"quantity\"\n" +
                "FROM \"customers\" c\n" +
                "JOIN \"orders\" o ON c.\"id\" = o.\"customer_id\"\n" +
                "JOIN \"order_items\" oi ON o.\"id\" = oi.\"order_id\"\n" +
                "JOIN \"products\" p ON oi.\"product_id\" = p.\"id\";"
            ),

            createSubtitle("Typical Use Cases"),
            createBulletList(
                "Join customer and order exports from another system",
                "Preview CSV content before importing into a real database",
                "Filter and export only selected rows",
                "Create SQL INSERT scripts from CSV-based query results"
            ),
             
            createSubtitle("Important Rules"),
            createBulletList(
                "ALWAYS use double quotes around table and column names",
                "Table names come from file names (customers.csv → \"customers\")",
                "H2 auto-detects: UTF-8, TIS-620, Windows-874, comma/semicolon/tab delimiters",
                "First row is assumed to be header (column names)",
                "SELECT queries only - INSERT/UPDATE/DELETE won't save back to CSV"
            ),
            
            createSubtitle("Supported JOIN Types"),
            createBulletList(
                "INNER JOIN - matching rows only",
                "LEFT JOIN / LEFT OUTER JOIN - all from left, matching from right",
                "RIGHT JOIN / RIGHT OUTER JOIN - all from right, matching from left",
                "FULL JOIN / FULL OUTER JOIN - all rows from both tables",
                "CROSS JOIN - cartesian product"
            ),
            
            createSubtitle("Supported SQL Functions"),
            createBulletList(
                "Aggregate: COUNT(), SUM(), AVG(), MAX(), MIN()",
                "String: UPPER(), LOWER(), CONCAT(), SUBSTRING()",
                "Date: CURRENT_DATE(), CURRENT_TIMESTAMP()",
                "Math: ROUND(), ABS(), MOD()",
                "Conditional: COALESCE(), NULLIF(), CASE WHEN"
            ),

            createSubtitle("Limitations"),
            createBulletList(
                "Data is loaded into memory, so very large files may consume significant RAM",
                "Changes are not written back to CSV files",
                "Type detection depends on file content and may not always match your expectations exactly",
                "Complex production-scale transformations may be better done in a proper database engine"
            ),

            createSubtitle("Troubleshooting CSV Queries"),
            createBulletList(
                "If a table is not found, verify the CSV file exists in the selected folder",
                "If column names behave strangely, inspect the header row in the file",
                "If text looks garbled, verify the source file encoding",
                "If results are slow, reduce file size or split very large CSV files"
            ),
             
            createWarning("Note: Data is loaded into memory. Very large CSV files (>100MB) may cause memory issues.")
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    // Helper methods
    private Label createTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("help-title");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        return label;
    }

    private Label createSubtitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("help-subtitle");
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 10 0 5 0;");
        return label;
    }

    private Label createParagraph(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 1.5;");
        return label;
    }

    private VBox createBulletList(String... items) {
        VBox box = new VBox(5);
        for (String item : items) {
            Label label = new Label("• " + item);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-padding: 0 0 0 10;");
            box.getChildren().add(label);
        }
        return box;
    }

    private VBox createDetailedList(String... items) {
        VBox box = new VBox(8);
        for (int i = 0; i < items.length; i += 2) {
            VBox itemBox = new VBox(2);
            Label title = new Label("• " + items[i]);
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 0 0 0 10;");

            String bodyText = i + 1 < items.length ? items[i + 1] : "";
            Label body = new Label(bodyText);
            body.setWrapText(true);
            body.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-padding: 0 0 0 24;");

            itemBox.getChildren().addAll(title, body);
            box.getChildren().add(itemBox);
        }
        return box;
    }

    private TextArea createCodeBlock(String code) {
        TextArea area = new TextArea(code);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount((int) code.lines().count() + 1);
        area.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                      "-fx-font-size: 12px; " +
                      "-fx-background-color: #F1F5F9; " +
                      "-fx-border-color: #E2E8F0; " +
                      "-fx-border-radius: 4px;");
        return area;
    }

    private Label createNote(String text) {
        Label label = new Label("ℹ️ " + text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #0369A1; " +
                       "-fx-background-color: #E0F2FE; " +
                       "-fx-padding: 10; " +
                       "-fx-background-radius: 4px;");
        return label;
    }

    private Label createWarning(String text) {
        Label label = new Label("⚠️ " + text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #B45309; " +
                       "-fx-background-color: #FEF3C7; " +
                       "-fx-padding: 10; " +
                       "-fx-background-radius: 4px;");
        return label;
    }
}
