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
            
            createSubtitle("Supported Database Types"),
            createBulletList(
                "MySQL 8.0+ - MySQL Community Server or MariaDB",
                "PostgreSQL 12+ - Open source relational database",
                "Microsoft SQL Server 2016+ - Enterprise database",
                "CSV Files (H2) - Query CSV files using SQL syntax"
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
            
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: MySQL server address (e.g., localhost, 192.168.1.100)",
                "Port: 3306 (default for MySQL)",
                "Database: Database name to connect to",
                "Username: MySQL user with appropriate privileges",
                "Password: User password (not saved)",
                "SSL: Enable for cloud databases (Neon, PlanetScale, etc.)"
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
            
            createSubtitle("MySQL-Specific Features"),
            createBulletList(
                "Uses SHOW TABLES to list tables",
                "Uses DESCRIBE table_name for structure",
                "Supports LIMIT clause",
                "MySQL backtick (`) identifier escaping"
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
            
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: PostgreSQL server address (e.g., localhost, db.example.com)",
                "Port: 5432 (default for PostgreSQL)",
                "Database: Database name",
                "Username: PostgreSQL user",
                "Password: User password (not saved)",
                "SSL: Enable for cloud databases"
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
            
            createSubtitle("PostgreSQL-Specific Features"),
            createBulletList(
                "Uses information_schema for table listing",
                "Supports CTE (WITH clauses)",
                "Uses double quote (\") identifier escaping",
                "Supports LIMIT and OFFSET"
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
            
            createSubtitle("Connection Parameters"),
            createBulletList(
                "Host: SQL Server address (e.g., localhost, server.example.com)",
                "Port: 1433 (default for SQL Server)",
                "Database: Database name",
                "Username: SQL Server authentication user",
                "Password: User password (not saved)",
                "SSL: Enable for encrypted connections"
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
            
            createSubtitle("SQL Server-Specific Features"),
            createBulletList(
                "Uses sys.objects for table listing",
                "Supports TOP clause (instead of LIMIT)",
                "Uses square bracket ([ ]) identifier escaping",
                "Supports T-SQL functions like GETDATE(), DATEADD()"
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
