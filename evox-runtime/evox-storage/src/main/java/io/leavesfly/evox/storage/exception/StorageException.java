package io.leavesfly.evox.storage.exception;

import io.leavesfly.evox.core.exception.EvoXException;

/**
 * 存储异常
 * 用于数据存储、持久化相关的异常
 *
 * @author EvoX Team
 */
public class StorageException extends EvoXException {

    public StorageException(String message) {
        super("STORAGE_ERROR", message);
    }

    public StorageException(String message, Throwable cause) {
        super("STORAGE_ERROR", message, cause);
    }

    public StorageException(String message, Object context) {
        super("STORAGE_ERROR", message, context);
    }

    public StorageException(String message, Throwable cause, Object context) {
        super("STORAGE_ERROR", message, cause, context);
    }
}
