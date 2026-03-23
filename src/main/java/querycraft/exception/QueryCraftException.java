package querycraft.exception;

/**
 * Base exception for all QueryCraft errors.
 */
public class QueryCraftException extends Exception {
    
    private final ErrorCode errorCode;
    
    public enum ErrorCode {
        CONNECTION_FAILED,
        QUERY_EXECUTION_FAILED,
        QUERY_VALIDATION_FAILED,
        QUERY_TIMEOUT,
        TRANSACTION_FAILED,
        EXPORT_FAILED,
        DRIVER_NOT_FOUND,
        INVALID_SQL,
        PERMISSION_DENIED
    }
    
    public QueryCraftException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    public QueryCraftException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    public QueryCraftException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public QueryCraftException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return errorCode == ErrorCode.CONNECTION_FAILED || 
               errorCode == ErrorCode.QUERY_TIMEOUT;
    }
}
