package querycraft.query;

import querycraft.connection.DatabaseConnectionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.ColumnInfo;
import querycraft.util.ResourceUtils;
import querycraft.util.ResultSetUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for executing streaming queries with row-by-row processing.
 * Prevents OutOfMemoryError for large result sets.
 *
 * <p>This service uses {@link ResultSetUtils} for column metadata extraction
 * and {@link ResourceUtils} for safe resource management.</p>
 */
public class StreamingQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingQueryService.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final DatabaseConnectionService connectionService;
    private final ExecutorService executor;
    private int fetchSize = DEFAULT_BATCH_SIZE;

    public StreamingQueryService() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("StreamingQuery-Thread");
            return t;
        });
    }

    /**
     * Set the fetch size for streaming results.
     * @param size number of rows to fetch at a time (default 1000)
     */
    public void setFetchSize(int size) {
        if (size < 100 || size > 10000) {
            throw new IllegalArgumentException("Fetch size must be between 100 and 10000");
        }
        this.fetchSize = size;
    }

    /**
     * Execute a query and stream results row by row.
     * @param sql the SQL query
     * @param metadataConsumer consumer for column metadata
     * @param rowConsumer consumer for each row
     * @param callback callback for completion or error
     */
    public void streamQuery(String sql, Consumer<List<ColumnInfo>> metadataConsumer, Consumer<Object[]> rowConsumer, StreamCallback callback) {
        try {
            connectionService.validateConnection();
        } catch (QueryCraftException e) {
            callback.onError(e);
            return;
        }

        executor.submit(() -> {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                try {
                    conn = connectionService.getCurrentConnection();
                } catch (QueryCraftException e) {
                    throw new SQLException("Failed to get connection", e);
                }

                // Create statement with streaming support
                stmt = conn.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
                );

                // Set fetch size for MySQL streaming
                String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
                if (dbName.contains("mysql")) {
                    stmt.setFetchSize(Integer.MIN_VALUE);
                } else if (fetchSize > 0) {
                    stmt.setFetchSize(fetchSize);
                }
                stmt.setQueryTimeout(DEFAULT_TIMEOUT_SECONDS);

                logger.debug("Executing streaming query: {}", sql.substring(0, Math.min(100, sql.length())));

                rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();

                // Use centralized column extraction
                List<ColumnInfo> columns = ResultSetUtils.extractColumns(metaData);
                int columnCount = columns.size();

                if (metadataConsumer != null) {
                    metadataConsumer.accept(columns);
                }

                long rowCount = 0;
                long startTime = System.currentTimeMillis();

                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rowConsumer.accept(row);
                    rowCount++;

                    // Log progress every 10k rows
                    if (rowCount % 10000 == 0) {
                        logger.debug("Streamed {} rows so far...", rowCount);
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Streaming query completed: {} rows in {} ms", rowCount, duration);

                callback.onComplete(rowCount, duration);

            } catch (SQLException e) {
                logger.error("Streaming query failed", e);
                callback.onError(new QueryCraftException(
                    QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                    "Streaming query failed: " + e.getMessage(),
                    e
                ));
            } finally {
                // Use centralized resource closing
                ResourceUtils.closeJdbcResources(rs, stmt, conn);
            }
        });
    }

    /**
     * Execute a query and return results in batches.
     * @param sql the SQL query
     * @param batchConsumer consumer for each batch of rows
     * @param batchSize number of rows per batch
     * @param callback callback for completion or error
     */
    public void streamQueryInBatches(String sql, Consumer<List<Object[]>> batchConsumer,
                                     int batchSize, StreamCallback callback) {
        List<Object[]> batch = new ArrayList<>(batchSize);

        streamQuery(sql, null, row -> {
            batch.add(row);
            if (batch.size() >= batchSize) {
                batchConsumer.accept(new ArrayList<>(batch));
                batch.clear();
            }
        }, new StreamCallback() {
            @Override
            public void onComplete(long totalRows, long durationMs) {
                // Process remaining rows
                if (!batch.isEmpty()) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
                callback.onComplete(totalRows, durationMs);
            }

            @Override
            public void onError(QueryCraftException e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Export large query results to file without loading into memory.
     * @param sql the SQL query
     * @param exporter the exporter to use
     * @param callback callback for completion or error
     */
    public void streamExportToFile(String sql, StreamingExporter exporter, StreamCallback callback) {
        try {
            connectionService.validateConnection();
        } catch (QueryCraftException e) {
            callback.onError(e);
            return;
        }

        executor.submit(() -> {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                try {
                    conn = connectionService.getCurrentConnection();
                } catch (QueryCraftException e) {
                    throw new SQLException("Failed to get connection", e);
                }

                stmt = conn.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
                );
                stmt.setFetchSize(fetchSize > 0 ? fetchSize : Integer.MIN_VALUE);
                stmt.setQueryTimeout(DEFAULT_TIMEOUT_SECONDS);

                rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();

                // Use centralized column extraction
                List<ColumnInfo> columns = ResultSetUtils.extractColumns(metaData);

                exporter.start(columns);

                long rowCount = 0;
                long startTime = System.currentTimeMillis();

                while (rs.next()) {
                    Object[] row = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    exporter.writeRow(row);
                    rowCount++;

                    if (rowCount % 10000 == 0) {
                        logger.debug("Exported {} rows...", rowCount);
                    }
                }

                exporter.finish();

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Stream export completed: {} rows in {} ms", rowCount, duration);

                callback.onComplete(rowCount, duration);

            } catch (Exception e) {
                logger.error("Stream export failed", e);
                try {
                    exporter.abort();
                } catch (Exception abortEx) {
                    logger.error("Failed to abort export", abortEx);
                }
                callback.onError(new QueryCraftException(
                    QueryCraftException.ErrorCode.EXPORT_FAILED,
                    "Export failed: " + e.getMessage(),
                    e
                ));
            } finally {
                // Use centralized resource closing
                ResourceUtils.closeJdbcResources(rs, stmt, conn);
            }
        });
    }

    /**
     * Get approximate row count for a query without executing full query.
     * Uses EXPLAIN or COUNT(*) depending on database.
     */
    public long estimateRowCount(String sql) throws QueryCraftException {
        connectionService.validateConnection();

        // Remove trailing semicolons to prevent syntax error in subquery
        String cleanSql = sql.trim().replaceAll(";+$", "");

        // Simple estimation - wrap in COUNT(*)
        String countSql = "SELECT COUNT(*) FROM (" + cleanSql + ") AS count_table";

        try (Connection conn = connectionService.getCurrentConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            logger.warn("Failed to estimate row count", e);
            return -1; // Unknown
        }
    }

    /**
     * Callback interface for streaming operations.
     */
    public interface StreamCallback {
        void onComplete(long totalRows, long durationMs);
        void onError(QueryCraftException e);
    }

    /**
     * Interface for streaming exporters.
     */
    public interface StreamingExporter {
        void start(List<ColumnInfo> columns) throws Exception;
        void writeRow(Object[] row) throws Exception;
        void finish() throws Exception;
        void abort();
    }

    /**
     * Shutdown the service.
     */
    public void shutdown() {
        logger.info("Shutting down StreamingQueryService");
        executor.shutdownNow();
    }
}
