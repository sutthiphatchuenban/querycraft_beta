package querycraft.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.connection.DatabaseConnectionService;
import querycraft.exception.QueryCraftException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Stack;

/**
 * Service for managing database transactions with savepoint support.
 */
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final DatabaseConnectionService connectionService;
    private Connection currentTransactionConnection;
    private final Stack<Savepoint> savepoints = new Stack<>();
    private boolean inTransaction = false;
    
    public TransactionService() {
        this.connectionService = DatabaseConnectionService.getInstance();
    }
    
    /**
     * Begin a new transaction.
     * @throws QueryCraftException if already in transaction or connection failed
     */
    public void beginTransaction() throws QueryCraftException {
        if (inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Already in a transaction. Commit or rollback first."
            );
        }
        
        connectionService.validateConnection();
        
        try {
            currentTransactionConnection = connectionService.getCurrentConnection();
            currentTransactionConnection.setAutoCommit(false);
            inTransaction = true;
            savepoints.clear();
            logger.info("Transaction started");
        } catch (SQLException e) {
            logger.error("Failed to begin transaction", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Failed to begin transaction: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Commit the current transaction.
     * @throws QueryCraftException if not in transaction or commit failed
     */
    public void commit() throws QueryCraftException {
        if (!inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Not in a transaction"
            );
        }
        
        try {
            currentTransactionConnection.commit();
            currentTransactionConnection.setAutoCommit(true);
            inTransaction = false;
            savepoints.clear();
            logger.info("Transaction committed");
        } catch (SQLException e) {
            logger.error("Failed to commit transaction", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Failed to commit transaction: " + e.getMessage(),
                e
            );
        } finally {
            closeTransactionConnection();
        }
    }
    
    /**
     * Rollback the entire transaction.
     * @throws QueryCraftException if not in transaction or rollback failed
     */
    public void rollback() throws QueryCraftException {
        if (!inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Not in a transaction"
            );
        }
        
        try {
            currentTransactionConnection.rollback();
            currentTransactionConnection.setAutoCommit(true);
            inTransaction = false;
            savepoints.clear();
            logger.info("Transaction rolled back");
        } catch (SQLException e) {
            logger.error("Failed to rollback transaction", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Failed to rollback transaction: " + e.getMessage(),
                e
            );
        } finally {
            closeTransactionConnection();
        }
    }
    
    /**
     * Create a savepoint within the current transaction.
     * @param name the savepoint name
     * @throws QueryCraftException if not in transaction or savepoint creation failed
     */
    public void createSavepoint(String name) throws QueryCraftException {
        if (!inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Not in a transaction"
            );
        }
        
        try {
            Savepoint sp = currentTransactionConnection.setSavepoint(name);
            savepoints.push(sp);
            logger.debug("Savepoint '{}' created", name);
        } catch (SQLException e) {
            logger.error("Failed to create savepoint: {}", name, e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Failed to create savepoint: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Rollback to the last savepoint.
     * @throws QueryCraftException if not in transaction or no savepoints
     */
    public void rollbackToSavepoint() throws QueryCraftException {
        if (!inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Not in a transaction"
            );
        }
        
        if (savepoints.isEmpty()) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "No savepoints to rollback to"
            );
        }
        
        try {
            Savepoint sp = savepoints.pop();
            currentTransactionConnection.rollback(sp);
            logger.debug("Rolled back to savepoint: {}", sp.getSavepointName());
        } catch (SQLException e) {
            logger.error("Failed to rollback to savepoint", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Failed to rollback to savepoint: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Check if currently in a transaction.
     */
    public boolean isInTransaction() {
        return inTransaction;
    }
    
    /**
     * Get the current transaction connection.
     * @throws QueryCraftException if not in transaction
     */
    public Connection getTransactionConnection() throws QueryCraftException {
        if (!inTransaction) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Not in a transaction"
            );
        }
        return currentTransactionConnection;
    }
    
    /**
     * Get the number of active savepoints.
     */
    public int getSavepointCount() {
        return savepoints.size();
    }
    
    private void closeTransactionConnection() {
        if (currentTransactionConnection != null) {
            try {
                currentTransactionConnection.close();
            } catch (SQLException e) {
                logger.warn("Failed to close transaction connection", e);
            }
            currentTransactionConnection = null;
        }
    }
    
    /**
     * Execute a Runnable within a transaction.
     * Automatically commits on success, rolls back on failure.
     * @param operation the operation to execute
     * @throws QueryCraftException if operation fails
     */
    public void executeInTransaction(TransactionOperation operation) throws QueryCraftException {
        beginTransaction();
        try {
            operation.execute();
            commit();
        } catch (Exception e) {
            try {
                rollback();
            } catch (QueryCraftException rollbackEx) {
                logger.error("Rollback failed after operation error", rollbackEx);
            }
            if (e instanceof QueryCraftException) {
                throw (QueryCraftException) e;
            }
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.TRANSACTION_FAILED,
                "Transaction operation failed: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Functional interface for transaction operations.
     */
    @FunctionalInterface
    public interface TransactionOperation {
        void execute() throws Exception;
    }
}
