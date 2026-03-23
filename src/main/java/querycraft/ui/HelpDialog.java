package querycraft.ui;

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
            
            createSubtitle("Supported Database Types"),
            createBulletList(
                "MySQL 8.0+ - MySQL Community Server or MariaDB",
                "PostgreSQL 12+ - Open source relational database",
                "Microsoft SQL Server 2016+ - Enterprise database",
                "CSV Files (H2) - Query CSV files using SQL syntax"
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
            
            createSubtitle("Safety Features"),
            createBulletList(
                "Query validation before execution",
                "DROP and TRUNCATE operations are blocked",
                "Confirmation dialog for DELETE operations",
                "Row limit for SELECT queries (10,000 rows max)"
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

            createSubtitle("Timeout Settings"),
            createParagraph("Use the Settings button in the top bar to configure query timeout, result limits, and other runtime behavior. If a query takes too long, increase timeout carefully or optimize the SQL rather than setting extremely large timeouts immediately."),
            
            createSubtitle("Security Notes"),
            createParagraph("Passwords are stored in memory only and are not persisted to disk. " +
                "Use the 'Remember Connection' feature to save connection details (without password) for quick reconnection.")
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
