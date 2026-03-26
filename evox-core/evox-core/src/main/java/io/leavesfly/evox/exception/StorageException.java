package io.leavesfly.evox.exception;

/**
 * Storage 相关异常
 * 用于存储操作过程中的错误情况
 */
public class StorageException extends EvoXException {

    private static final int STORAGE_ERROR_CODE = 4000;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(int errorCode, String message) {
        super(errorCode, message);
    }

    public StorageException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建存储操作异常
     */
    public static StorageException operationError(String operation, String reason) {
        return new StorageException(STORAGE_ERROR_CODE + 1, 
            String.format("存储操作 [%s] 失败: %s", operation, reason));
    }

    /**
     * 创建存储操作异常（带原因）
     */
    public static StorageException operationError(String operation, String reason, Throwable cause) {
        return new StorageException(STORAGE_ERROR_CODE + 1, 
            String.format("存储操作 [%s] 失败: %s", operation, reason), cause);
    }

    /**
     * 创建数据库连接异常
     */
    public static StorageException connectionError(String reason) {
        return new StorageException(STORAGE_ERROR_CODE + 2, 
            String.format("数据库连接失败: %s", reason));
    }

    /**
     * 创建数据库连接异常（带原因）
     */
    public static StorageException connectionError(String reason, Throwable cause) {
        return new StorageException(STORAGE_ERROR_CODE + 2, 
            String.format("数据库连接失败: %s", reason), cause);
    }

    /**
     * 创建数据未找到异常
     */
    public static StorageException notFound(String resourceType, String identifier) {
        return new StorageException(STORAGE_ERROR_CODE + 3, 
            String.format("%s 未找到: %s", resourceType, identifier));
    }

    /**
     * 创建向量存储异常
     */
    public static StorageException vectorStoreError(String operation, String reason) {
        return new StorageException(STORAGE_ERROR_CODE + 4, 
            String.format("向量存储操作 [%s] 失败: %s", operation, reason));
    }

    /**
     * 创建向量存储异常（带原因）
     */
    public static StorageException vectorStoreError(String operation, String reason, Throwable cause) {
        return new StorageException(STORAGE_ERROR_CODE + 4, 
            String.format("向量存储操作 [%s] 失败: %s", operation, reason), cause);
    }
}
