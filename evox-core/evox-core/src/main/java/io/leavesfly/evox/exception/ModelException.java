package io.leavesfly.evox.exception;

/**
 * Model 相关异常
 * 用于模型调用过程中的错误情况
 */
public class ModelException extends EvoXException {

    private static final int MODEL_ERROR_CODE = 6000;

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelException(int errorCode, String message) {
        super(errorCode, message);
    }

    public ModelException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建模型调用异常
     */
    public static ModelException invocationError(String modelName, String reason) {
        return new ModelException(MODEL_ERROR_CODE + 1, 
            String.format("模型 [%s] 调用失败: %s", modelName, reason));
    }

    /**
     * 创建模型调用异常（带原因）
     */
    public static ModelException invocationError(String modelName, String reason, Throwable cause) {
        return new ModelException(MODEL_ERROR_CODE + 1, 
            String.format("模型 [%s] 调用失败: %s", modelName, reason), cause);
    }

    /**
     * 创建模型配置异常
     */
    public static ModelException configurationError(String modelName, String reason) {
        return new ModelException(MODEL_ERROR_CODE + 2, 
            String.format("模型 [%s] 配置错误: %s", modelName, reason));
    }

    /**
     * 创建模型响应解析异常
     */
    public static ModelException responseParseError(String modelName, String reason) {
        return new ModelException(MODEL_ERROR_CODE + 3, 
            String.format("模型 [%s] 响应解析失败: %s", modelName, reason));
    }

    /**
     * 创建模型响应解析异常（带原因）
     */
    public static ModelException responseParseError(String modelName, String reason, Throwable cause) {
        return new ModelException(MODEL_ERROR_CODE + 3, 
            String.format("模型 [%s] 响应解析失败: %s", modelName, reason), cause);
    }

    /**
     * 创建模型超时异常
     */
    public static ModelException timeout(String modelName, long timeoutMs) {
        return new ModelException(MODEL_ERROR_CODE + 4, 
            String.format("模型 [%s] 调用超时: %d ms", modelName, timeoutMs));
    }
}
