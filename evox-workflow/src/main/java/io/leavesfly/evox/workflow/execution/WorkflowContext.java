package io.leavesfly.evox.workflow.execution;

import io.leavesfly.evox.core.message.Message;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行上下文 - 存储工作流执行过程中的状态和数据
 * 对应 Python 版本的 Environment
 */
@Slf4j
@Data
public class WorkflowContext {

    /**
     * 工作流目标
     */
    private String goal;

    /**
     * 初始输入
     */
    private Map<String, Object> initialInputs;

    /**
     * 执行数据（节点输出存储）
     */
    private Map<String, Object> executionData;

    /**
     * 消息历史
     */
    private List<Message> messageHistory;

    /**
     * 任务执行历史（节点执行顺序）
     */
    private List<String> taskExecutionHistory;

    /**
     * 执行状态
     */
    private ExecutionState state;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 最后执行的任务
     */
    private String lastExecutedTask;

    /**
     * 当前步数
     */
    private int currentStep;

    public WorkflowContext(String goal, Map<String, Object> initialInputs) {
        this.goal = goal;
        this.initialInputs = initialInputs != null ? new HashMap<>(initialInputs) : new HashMap<>();
        this.executionData = new ConcurrentHashMap<>();
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
        this.taskExecutionHistory = Collections.synchronizedList(new ArrayList<>());
        this.state = ExecutionState.INITIALIZED;
        this.currentStep = 0;
        
        // 将初始输入添加到执行数据中
        this.executionData.putAll(this.initialInputs);
    }

    /**
     * 添加消息到历史
     */
    public void addMessage(Message message) {
        messageHistory.add(message);
        log.debug("Added message to history: {}", message.getMessageType());
    }

    /**
     * 更新执行数据
     */
    public void updateExecutionData(String key, Object value) {
        executionData.put(key, value);
        log.debug("Updated execution data: {} = {}", key, value);
    }

    /**
     * 更新执行数据（批量）
     */
    public void updateExecutionData(Map<String, Object> data) {
        if (data != null) {
            executionData.putAll(data);
            log.debug("Updated execution data with {} entries", data.size());
        }
    }

    /**
     * 获取执行数据
     */
    public Object getExecutionData(String key) {
        return executionData.get(key);
    }

    /**
     * 获取所有执行数据
     */
    public Map<String, Object> getAllExecutionData() {
        return new HashMap<>(executionData);
    }

    /**
     * 记录任务执行
     */
    public void recordTaskExecution(String taskName) {
        taskExecutionHistory.add(taskName);
        lastExecutedTask = taskName;
        currentStep++;
        log.info("Recorded task execution: {} (step {})", taskName, currentStep);
    }

    /**
     * 获取任务执行轨迹
     */
    public String getTaskExecutionTrajectory() {
        if (taskExecutionHistory.isEmpty()) {
            return "None";
        }
        return String.join(" -> ", taskExecutionHistory);
    }

    /**
     * 获取所有消息
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messageHistory);
    }

    /**
     * 获取最后一条消息
     */
    public Message getLastMessage() {
        if (messageHistory.isEmpty()) {
            return null;
        }
        return messageHistory.get(messageHistory.size() - 1);
    }

    /**
     * 更新状态
     */
    public void updateState(ExecutionState newState) {
        this.state = newState;
        log.debug("Workflow context state changed to: {}", newState);
    }

    /**
     * 标记为完成
     */
    public void markCompleted() {
        this.state = ExecutionState.COMPLETED;
        log.info("Workflow context marked as completed");
    }

    /**
     * 标记为失败
     */
    public void markFailed(String errorMessage) {
        this.state = ExecutionState.FAILED;
        this.errorMessage = errorMessage;
        log.error("Workflow context marked as failed: {}", errorMessage);
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return state == ExecutionState.COMPLETED;
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return state == ExecutionState.FAILED;
    }

    /**
     * 重置上下文
     */
    public void reset() {
        executionData.clear();
        executionData.putAll(initialInputs);
        messageHistory.clear();
        taskExecutionHistory.clear();
        state = ExecutionState.INITIALIZED;
        errorMessage = null;
        lastExecutedTask = null;
        currentStep = 0;
        log.info("Workflow context reset");
    }

    /**
     * 执行状态枚举
     */
    public enum ExecutionState {
        /** 已初始化 */
        INITIALIZED,
        /** 运行中 */
        RUNNING,
        /** 已完成 */
        COMPLETED,
        /** 失败 */
        FAILED,
        /** 已暂停 */
        PAUSED
    }
}
