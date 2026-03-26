package io.leavesfly.evox.exception;

/**
 * Workflow 相关异常
 * 用于工作流执行过程中的错误情况
 */
public class WorkflowException extends EvoXException {

    private static final int WORKFLOW_ERROR_CODE = 2000;

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkflowException(int errorCode, String message) {
        super(errorCode, message);
    }

    public WorkflowException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建工作流执行异常
     */
    public static WorkflowException executionError(String workflowId, String reason) {
        return new WorkflowException(WORKFLOW_ERROR_CODE + 1, 
            String.format("工作流 [%s] 执行失败: %s", workflowId, reason));
    }

    /**
     * 创建工作流执行异常（带原因）
     */
    public static WorkflowException executionError(String workflowId, String reason, Throwable cause) {
        return new WorkflowException(WORKFLOW_ERROR_CODE + 1, 
            String.format("工作流 [%s] 执行失败: %s", workflowId, reason), cause);
    }

    /**
     * 创建工作流定义异常
     */
    public static WorkflowException definitionError(String workflowId, String reason) {
        return new WorkflowException(WORKFLOW_ERROR_CODE + 2, 
            String.format("工作流 [%s] 定义错误: %s", workflowId, reason));
    }

    /**
     * 创建工作流步骤异常
     */
    public static WorkflowException stepError(String workflowId, String stepName, String reason) {
        return new WorkflowException(WORKFLOW_ERROR_CODE + 3, 
            String.format("工作流 [%s] 步骤 [%s] 执行失败: %s", workflowId, stepName, reason));
    }

    /**
     * 创建工作流步骤异常（带原因）
     */
    public static WorkflowException stepError(String workflowId, String stepName, String reason, Throwable cause) {
        return new WorkflowException(WORKFLOW_ERROR_CODE + 3, 
            String.format("工作流 [%s] 步骤 [%s] 执行失败: %s", workflowId, stepName, reason), cause);
    }
}
