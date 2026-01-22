package io.leavesfly.evox.workflow.visualization;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 工作流执行追踪器
 * 提供执行监控、状态追踪、性能分析等功能
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class ExecutionTracer {

    /**
     * 追踪ID
     */
    private final String traceId;

    /**
     * 工作流图
     */
    private final WorkflowGraph graph;

    /**
     * 执行记录
     */
    private final List<ExecutionRecord> records = new CopyOnWriteArrayList<>();

    /**
     * 节点执行统计 (nodeId -> stats)
     */
    private final Map<String, NodeExecutionStats> nodeStats = new ConcurrentHashMap<>();

    /**
     * 执行时间线
     */
    private final List<TimelineEvent> timeline = new CopyOnWriteArrayList<>();

    /**
     * 执行开始时间
     */
    private long startTime;

    /**
     * 执行结束时间
     */
    private long endTime;

    /**
     * 当前执行状态
     */
    private ExecutionStatus status = ExecutionStatus.NOT_STARTED;

    /**
     * 事件监听器
     */
    private final List<Consumer<TraceEvent>> eventListeners = new CopyOnWriteArrayList<>();

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public ExecutionTracer(WorkflowGraph graph) {
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
        this.graph = graph;
    }

    // ============= 追踪生命周期 =============

    /**
     * 开始追踪
     */
    public void start() {
        startTime = System.currentTimeMillis();
        status = ExecutionStatus.RUNNING;
        
        addTimelineEvent(TimelineEventType.WORKFLOW_STARTED, "工作流开始执行", null);
        fireEvent(TraceEventType.WORKFLOW_STARTED, null);
        
        log.info("[Trace-{}] 工作流追踪开始", traceId);
    }

    /**
     * 结束追踪
     */
    public void finish(boolean success) {
        endTime = System.currentTimeMillis();
        status = success ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
        
        addTimelineEvent(
            success ? TimelineEventType.WORKFLOW_COMPLETED : TimelineEventType.WORKFLOW_FAILED,
            success ? "工作流执行完成" : "工作流执行失败",
            null
        );
        fireEvent(success ? TraceEventType.WORKFLOW_COMPLETED : TraceEventType.WORKFLOW_FAILED, null);
        
        log.info("[Trace-{}] 工作流追踪结束, 状态: {}, 耗时: {}ms", 
            traceId, status, getTotalDuration());
    }

    // ============= 节点追踪 =============

    /**
     * 记录节点开始执行
     */
    public void recordNodeStart(WorkflowNode node) {
        long timestamp = System.currentTimeMillis();
        
        NodeExecutionStats stats = nodeStats.computeIfAbsent(
            node.getNodeId(), 
            k -> new NodeExecutionStats(node.getNodeId(), node.getName())
        );
        stats.startExecution(timestamp);
        
        addTimelineEvent(TimelineEventType.NODE_STARTED, 
            "节点开始: " + node.getName(), node.getNodeId());
        
        fireEvent(TraceEventType.NODE_STARTED, node);
        
        log.debug("[Trace-{}] 节点开始执行: {}", traceId, node.getName());
    }

    /**
     * 记录节点完成
     */
    public void recordNodeComplete(WorkflowNode node, Object result) {
        long timestamp = System.currentTimeMillis();
        
        NodeExecutionStats stats = nodeStats.get(node.getNodeId());
        if (stats != null) {
            stats.completeExecution(timestamp, true);
        }
        
        ExecutionRecord record = ExecutionRecord.builder()
            .nodeId(node.getNodeId())
            .nodeName(node.getName())
            .nodeType(node.getNodeType())
            .status(ExecutionRecordStatus.COMPLETED)
            .result(result != null ? result.toString() : null)
            .timestamp(timestamp)
            .duration(stats != null ? stats.getLastDuration() : 0)
            .build();
        records.add(record);
        
        addTimelineEvent(TimelineEventType.NODE_COMPLETED, 
            "节点完成: " + node.getName(), node.getNodeId());
        
        fireEvent(TraceEventType.NODE_COMPLETED, node);
        
        log.debug("[Trace-{}] 节点执行完成: {} ({}ms)", 
            traceId, node.getName(), stats != null ? stats.getLastDuration() : 0);
    }

    /**
     * 记录节点失败
     */
    public void recordNodeFailed(WorkflowNode node, String error) {
        long timestamp = System.currentTimeMillis();
        
        NodeExecutionStats stats = nodeStats.get(node.getNodeId());
        if (stats != null) {
            stats.completeExecution(timestamp, false);
        }
        
        ExecutionRecord record = ExecutionRecord.builder()
            .nodeId(node.getNodeId())
            .nodeName(node.getName())
            .nodeType(node.getNodeType())
            .status(ExecutionRecordStatus.FAILED)
            .error(error)
            .timestamp(timestamp)
            .duration(stats != null ? stats.getLastDuration() : 0)
            .build();
        records.add(record);
        
        addTimelineEvent(TimelineEventType.NODE_FAILED, 
            "节点失败: " + node.getName() + " - " + error, node.getNodeId());
        
        fireEvent(TraceEventType.NODE_FAILED, node);
        
        log.warn("[Trace-{}] 节点执行失败: {} - {}", traceId, node.getName(), error);
    }

    /**
     * 记录节点跳过
     */
    public void recordNodeSkipped(WorkflowNode node, String reason) {
        ExecutionRecord record = ExecutionRecord.builder()
            .nodeId(node.getNodeId())
            .nodeName(node.getName())
            .nodeType(node.getNodeType())
            .status(ExecutionRecordStatus.SKIPPED)
            .error(reason)
            .timestamp(System.currentTimeMillis())
            .duration(0)
            .build();
        records.add(record);
        
        addTimelineEvent(TimelineEventType.NODE_SKIPPED, 
            "节点跳过: " + node.getName(), node.getNodeId());
        
        log.debug("[Trace-{}] 节点跳过: {} - {}", traceId, node.getName(), reason);
    }

    // ============= 时间线 =============

    /**
     * 添加时间线事件
     */
    private void addTimelineEvent(TimelineEventType type, String message, String nodeId) {
        TimelineEvent event = TimelineEvent.builder()
            .type(type)
            .message(message)
            .nodeId(nodeId)
            .timestamp(System.currentTimeMillis())
            .relativeTime(System.currentTimeMillis() - startTime)
            .build();
        timeline.add(event);
    }

    // ============= 统计和报告 =============

    /**
     * 获取总执行时间
     */
    public long getTotalDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取执行摘要
     */
    public ExecutionSummary getSummary() {
        int completedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        long totalNodeDuration = 0;
        
        for (ExecutionRecord record : records) {
            switch (record.getStatus()) {
                case COMPLETED -> completedCount++;
                case FAILED -> failedCount++;
                case SKIPPED -> skippedCount++;
            }
            totalNodeDuration += record.getDuration();
        }
        
        return ExecutionSummary.builder()
            .traceId(traceId)
            .status(status)
            .totalDuration(getTotalDuration())
            .totalNodes(graph.getNodes().size())
            .completedNodes(completedCount)
            .failedNodes(failedCount)
            .skippedNodes(skippedCount)
            .averageNodeDuration(records.isEmpty() ? 0 : totalNodeDuration / records.size())
            .build();
    }

    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("                    工作流执行性能报告\n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");
        
        ExecutionSummary summary = getSummary();
        
        sb.append("追踪ID: ").append(traceId).append("\n");
        sb.append("执行状态: ").append(status).append("\n");
        sb.append("总耗时: ").append(summary.getTotalDuration()).append("ms\n\n");
        
        sb.append("节点统计:\n");
        sb.append("  - 总节点数: ").append(summary.getTotalNodes()).append("\n");
        sb.append("  - 完成: ").append(summary.getCompletedNodes()).append("\n");
        sb.append("  - 失败: ").append(summary.getFailedNodes()).append("\n");
        sb.append("  - 跳过: ").append(summary.getSkippedNodes()).append("\n");
        sb.append("  - 平均耗时: ").append(summary.getAverageNodeDuration()).append("ms\n\n");
        
        // 节点详细性能
        sb.append("节点执行详情:\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-20s %-10s %-10s %-10s %-10s\n", 
            "节点名称", "执行次数", "平均耗时", "最大耗时", "成功率"));
        sb.append("───────────────────────────────────────────────────────────────\n");
        
        for (NodeExecutionStats stats : nodeStats.values()) {
            double successRate = stats.getExecutionCount() > 0 ?
                (stats.getSuccessCount() * 100.0 / stats.getExecutionCount()) : 0;
            sb.append(String.format("%-20s %-10d %-10d %-10d %.1f%%\n",
                truncate(stats.getNodeName(), 20),
                stats.getExecutionCount(),
                stats.getAverageDuration(),
                stats.getMaxDuration(),
                successRate));
        }
        
        sb.append("───────────────────────────────────────────────────────────────\n\n");
        
        // 时间线
        sb.append("执行时间线:\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        for (TimelineEvent event : timeline) {
            String time = TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimestamp()));
            sb.append(String.format("[%s] +%6dms  %s\n",
                time, event.getRelativeTime(), event.getMessage()));
        }
        sb.append("───────────────────────────────────────────────────────────────\n");
        
        return sb.toString();
    }

    /**
     * 获取可视化时间线
     */
    public String getVisualTimeline() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("执行时间线 (").append(traceId).append(")\n");
        sb.append("═".repeat(60)).append("\n");
        
        long maxTime = getTotalDuration();
        int barWidth = 40;
        
        for (NodeExecutionStats stats : nodeStats.values()) {
            String name = truncate(stats.getNodeName(), 15);
            long startOffset = stats.getStartTime() - startTime;
            long duration = stats.getLastDuration();
            
            int startPos = (int) (startOffset * barWidth / Math.max(maxTime, 1));
            int width = (int) Math.max(1, duration * barWidth / Math.max(maxTime, 1));
            
            sb.append(String.format("%-15s ", name));
            sb.append("|");
            sb.append(" ".repeat(Math.max(0, startPos)));
            sb.append("█".repeat(Math.max(1, Math.min(width, barWidth - startPos))));
            sb.append(" ".repeat(Math.max(0, barWidth - startPos - width)));
            sb.append("| ").append(duration).append("ms\n");
        }
        
        sb.append("═".repeat(60)).append("\n");
        sb.append("总耗时: ").append(maxTime).append("ms\n");
        
        return sb.toString();
    }

    // ============= 事件系统 =============

    /**
     * 添加事件监听器
     */
    public void addEventListener(Consumer<TraceEvent> listener) {
        eventListeners.add(listener);
    }

    /**
     * 触发事件
     */
    private void fireEvent(TraceEventType type, WorkflowNode node) {
        TraceEvent event = new TraceEvent(type, node, System.currentTimeMillis());
        eventListeners.forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.warn("事件监听器处理失败", e);
            }
        });
    }

    // ============= 工具方法 =============

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }

    // ============= 内部类 =============

    /**
     * 执行状态
     */
    public enum ExecutionStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * 执行记录
     */
    @Data
    @lombok.Builder
    public static class ExecutionRecord {
        private String nodeId;
        private String nodeName;
        private WorkflowNode.NodeType nodeType;
        private ExecutionRecordStatus status;
        private String result;
        private String error;
        private long timestamp;
        private long duration;
    }

    /**
     * 执行记录状态
     */
    public enum ExecutionRecordStatus {
        COMPLETED,
        FAILED,
        SKIPPED
    }

    /**
     * 节点执行统计
     */
    @Data
    public static class NodeExecutionStats {
        private final String nodeId;
        private final String nodeName;
        private int executionCount = 0;
        private int successCount = 0;
        private long totalDuration = 0;
        private long maxDuration = 0;
        private long lastDuration = 0;
        private long startTime = 0;

        public NodeExecutionStats(String nodeId, String nodeName) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
        }

        public void startExecution(long timestamp) {
            this.startTime = timestamp;
        }

        public void completeExecution(long endTimestamp, boolean success) {
            this.lastDuration = endTimestamp - startTime;
            this.totalDuration += lastDuration;
            this.executionCount++;
            if (success) {
                this.successCount++;
            }
            if (lastDuration > maxDuration) {
                maxDuration = lastDuration;
            }
        }

        public long getAverageDuration() {
            return executionCount > 0 ? totalDuration / executionCount : 0;
        }
    }

    /**
     * 时间线事件
     */
    @Data
    @lombok.Builder
    public static class TimelineEvent {
        private TimelineEventType type;
        private String message;
        private String nodeId;
        private long timestamp;
        private long relativeTime;
    }

    /**
     * 时间线事件类型
     */
    public enum TimelineEventType {
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED,
        NODE_STARTED,
        NODE_COMPLETED,
        NODE_FAILED,
        NODE_SKIPPED
    }

    /**
     * 执行摘要
     */
    @Data
    @lombok.Builder
    public static class ExecutionSummary {
        private String traceId;
        private ExecutionStatus status;
        private long totalDuration;
        private int totalNodes;
        private int completedNodes;
        private int failedNodes;
        private int skippedNodes;
        private long averageNodeDuration;
    }

    /**
     * 追踪事件
     */
    @Data
    @lombok.AllArgsConstructor
    public static class TraceEvent {
        private TraceEventType type;
        private WorkflowNode node;
        private long timestamp;
    }

    /**
     * 追踪事件类型
     */
    public enum TraceEventType {
        WORKFLOW_STARTED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED,
        NODE_STARTED,
        NODE_COMPLETED,
        NODE_FAILED
    }
}
