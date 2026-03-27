package querycraft.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection manager for CSV file connections using H2 database.
 * Each CSV folder gets its own in-memory H2 database with tables created from CSV files.
 */
public class CsvConnectionManager implements ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(CsvConnectionManager.class);

    private Connection currentConnection;
    private CsvConnectionInfo currentCsvInfo;

    @Override
    public Connection connect(ConnectionInfo connectionInfo) throws QueryCraftException {
        if (!(connectionInfo instanceof CsvConnectionInfo)) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "CsvConnectionManager only supports CSV connections"
            );
        }

        CsvConnectionInfo csvInfo = (CsvConnectionInfo) connectionInfo;

        // Disconnect any existing connection
        disconnect();

        // Load H2 driver
        loadDriver();

        try {
            // Create H2 connection (no pooling for CSV)
            String jdbcUrl = csvInfo.getJdbcUrl();
            Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");

            // Load all CSV files as tables
            loadCsvFiles(conn, csvInfo);

            this.currentConnection = conn;
            this.currentCsvInfo = csvInfo;

            logger.info("CSV connection established with {} files from {}",
                csvInfo.getCsvFileCount(), csvInfo.getCsvFolderPath());

            return conn;

        } catch (SQLException e) {
            disconnect();
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Failed to connect to CSV: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void disconnect() {
        if (currentConnection != null) {
            try {
                // SHUTDOWN command is required for H2 in-memory DBs with DB_CLOSE_DELAY=-1 to free memory
                try (Statement stmt = currentConnection.createStatement()) {
                    stmt.execute("SHUTDOWN");
                }
                currentConnection.close();
                logger.info("CSV connection closed and H2 database shut down");
            } catch (SQLException e) {
                logger.warn("Error closing CSV connection", e);
            }
            currentConnection = null;
        }
        currentCsvInfo = null;
    }

    @Override
    public Connection getConnection() throws QueryCraftException {
        if (currentCsvInfo != null) {
            // For CSV, return a new connection each time (H2 in-memory is cheap)
            try {
                return DriverManager.getConnection(currentCsvInfo.getJdbcUrl(), "sa", "");
            } catch (SQLException e) {
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.CONNECTION_FAILED,
                    "Failed to get CSV connection: " + e.getMessage(),
                    e
                );
            }
        }

        throw new QueryCraftException(
            QueryCraftException.ErrorCode.CONNECTION_FAILED,
            "No active CSV connection. Please connect first."
        );
    }

    @Override
    public boolean testConnection(ConnectionInfo connectionInfo) throws QueryCraftException {
        if (!(connectionInfo instanceof CsvConnectionInfo)) {
            return false;
        }

        CsvConnectionInfo csvInfo = (CsvConnectionInfo) connectionInfo;
        boolean valid = csvInfo.getCsvFileCount() > 0;
        logger.debug("CSV connection test: {} files found", csvInfo.getCsvFileCount());
        return valid;
    }

    @Override
    public boolean isConnected() {
        return currentCsvInfo != null;
    }

    @Override
    public boolean supports(ConnectionInfo connectionInfo) {
        return connectionInfo instanceof CsvConnectionInfo;
    }

    /**
     * Returns the current CSV connection info.
     */
    public CsvConnectionInfo getCurrentCsvInfo() {
        return currentCsvInfo;
    }

    private void loadDriver() throws QueryCraftException {
        try {
            Class.forName("org.h2.Driver");
            logger.debug("H2 driver loaded");
        } catch (ClassNotFoundException e) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.DRIVER_NOT_FOUND,
                "H2 driver not found. Please ensure H2 is included in dependencies.",
                e
            );
        }
    }

    private void loadCsvFiles(Connection conn, CsvConnectionInfo csvInfo) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (CsvConnectionInfo.CsvFileInfo csvFile : csvInfo.getCsvFiles()) {
                String createTableSql = csvInfo.getCreateTableSql(csvFile);
                logger.debug("Creating table from CSV: {}", csvFile.getTableName());
                stmt.execute(createTableSql);
            }
        } catch (SQLException e) {
            logger.error("Failed to load CSV files", e);
            throw e;
        }
    }
}
