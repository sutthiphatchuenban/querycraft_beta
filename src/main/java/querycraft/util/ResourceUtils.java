package querycraft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for safe resource management.
 * Provides methods for closing resources without throwing exceptions,
 * eliminating the need for repetitive try-catch blocks in finally blocks.
 */
public final class ResourceUtils {

    private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);

    private ResourceUtils() {
        // Prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Quietly closes an AutoCloseable resource, logging any exceptions at debug level.
     * This method never throws an exception.
     *
     * @param closeable the resource to close, may be null
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.debug("Failed to close resource: {}", closeable.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Quietly closes a Connection, logging any SQLException at debug level.
     * This method never throws an exception.
     *
     * @param connection the connection to close, may be null
     */
    public static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.debug("Failed to close connection", e);
            }
        }
    }

    /**
     * Quietly closes a Statement, logging any SQLException at debug level.
     * This method never throws an exception.
     *
     * @param statement the statement to close, may be null
     */
    public static void closeQuietly(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.debug("Failed to close statement", e);
            }
        }
    }

    /**
     * Quietly closes a ResultSet, logging any SQLException at debug level.
     * This method never throws an exception.
     *
     * @param resultSet the result set to close, may be null
     */
    public static void closeQuietly(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.debug("Failed to close result set", e);
            }
        }
    }

    /**
     * Closes multiple resources in order. Resources are closed even if earlier
     * closures throw exceptions.
     *
     * @param closeables the resources to close
     */
    public static void closeAll(AutoCloseable... closeables) {
        if (closeables == null) {
            return;
        }
        for (AutoCloseable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Closes JDBC resources in the correct order: ResultSet, Statement, Connection.
     * This is the recommended order to avoid resource leaks.
     *
     * @param resultSet the result set to close (may be null)
     * @param statement the statement to close (may be null)
     * @param connection the connection to close (may be null)
     */
    public static void closeJdbcResources(ResultSet resultSet, Statement statement, Connection connection) {
        closeQuietly(resultSet);
        closeQuietly(statement);
        closeQuietly(connection);
    }

    /**
     * Closes Statement and Connection resources.
     *
     * @param statement the statement to close (may be null)
     * @param connection the connection to close (may be null)
     */
    public static void closeJdbcResources(Statement statement, Connection connection) {
        closeQuietly(statement);
        closeQuietly(connection);
    }

    /**
     * Executes a Runnable and ensures the closeable is closed afterwards,
     * similar to try-with-resources but for use cases where try-with-resources
     * cannot be used.
     *
     * @param closeable the resource to manage
     * @param action the action to execute
     * @param <T> the type of AutoCloseable
     * @throws RuntimeException if action throws an exception
     */
    public static <T extends AutoCloseable> void withResource(T closeable, ResourceConsumer<T> action) {
        try {
            action.accept(closeable);
        } catch (Exception e) {
            throw new RuntimeException("Resource operation failed", e);
        } finally {
            closeQuietly(closeable);
        }
    }

    /**
     * Functional interface for resource operations that may throw exceptions.
     *
     * @param <T> the resource type
     */
    @FunctionalInterface
    public interface ResourceConsumer<T extends AutoCloseable> {
        void accept(T resource) throws Exception;
    }
}
