package io.leavesfly.evox.exception;

/**
 * Evaluation 相关异常
 * 用于评估过程中的错误情况
 */
public class EvaluationException extends EvoXException {

    private static final int EVALUATION_ERROR_CODE = 3000;

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvaluationException(int errorCode, String message) {
        super(errorCode, message);
    }

    public EvaluationException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建评估执行异常
     */
    public static EvaluationException evaluationError(String evaluatorName, String reason) {
        return new EvaluationException(EVALUATION_ERROR_CODE + 1, 
            String.format("评估器 [%s] 执行失败: %s", evaluatorName, reason));
    }

    /**
     * 创建评估执行异常（带原因）
     */
    public static EvaluationException evaluationError(String evaluatorName, String reason, Throwable cause) {
        return new EvaluationException(EVALUATION_ERROR_CODE + 1, 
            String.format("评估器 [%s] 执行失败: %s", evaluatorName, reason), cause);
    }

    /**
     * 创建评估配置异常
     */
    public static EvaluationException configurationError(String evaluatorName, String reason) {
        return new EvaluationException(EVALUATION_ERROR_CODE + 2, 
            String.format("评估器 [%s] 配置错误: %s", evaluatorName, reason));
    }

    /**
     * 创建评估输入异常
     */
    public static EvaluationException invalidInput(String evaluatorName, String reason) {
        return new EvaluationException(EVALUATION_ERROR_CODE + 3, 
            String.format("评估器 [%s] 输入无效: %s", evaluatorName, reason));
    }
}
