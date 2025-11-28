package io.leavesfly.evox.workflow.graph;

import io.leavesfly.evox.workflow.base.WorkflowNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工作流图 - 管理工作流中所有节点及其关系
 * 对应 Python 版本的 WorkFlowGraph
 */
@Slf4j
@Data
@NoArgsConstructor
public class WorkflowGraph {

    /**
     * 工作流目标/描述
     */
    private String goal;

    /**
     * 所有节点的映射 (nodeId -> WorkflowNode)
     */
    private Map<String, WorkflowNode> nodes;

    /**
     * 当前执行的节点 ID
     */
    private String currentNodeId;

    /**
     * 已完成的节点 ID 集合
     */
    private Set<String> completedNodes;

    /**
     * 失败的节点 ID 集合
     */
    private Set<String> failedNodes;

    public WorkflowGraph(String goal) {
        this.goal = goal;
        this.nodes = new ConcurrentHashMap<>();
        this.completedNodes = ConcurrentHashMap.newKeySet();
        this.failedNodes = ConcurrentHashMap.newKeySet();
    }

    /**
     * 添加节点到图中
     */
    public void addNode(WorkflowNode node) {
        if (node == null || node.getNodeId() == null) {
            throw new IllegalArgumentException("Node and nodeId cannot be null");
        }
        nodes.put(node.getNodeId(), node);
        log.debug("Added node: {} ({})", node.getName(), node.getNodeId());
    }

    /**
     * 根据 ID 获取节点
     */
    public WorkflowNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 根据名称获取节点
     */
    public WorkflowNode getNodeByName(String name) {
        return nodes.values().stream()
                .filter(node -> name.equals(node.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加边（从 sourceId 到 targetId）
     */
    public void addEdge(String sourceId, String targetId) {
        WorkflowNode source = getNode(sourceId);
        WorkflowNode target = getNode(targetId);
        
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source or target node not found");
        }
        
        source.addSuccessor(targetId);
        target.addPredecessor(sourceId);
        log.debug("Added edge: {} -> {}", source.getName(), target.getName());
    }

    /**
     * 查找初始节点（没有前置节点的节点）
     */
    public List<WorkflowNode> findInitialNodes() {
        return nodes.values().stream()
                .filter(node -> node.getPredecessors().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 查找终止节点（没有后继节点的节点）
     */
    public List<WorkflowNode> findTerminalNodes() {
        return nodes.values().stream()
                .filter(node -> node.getSuccessors().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 执行步骤：从源节点移动到目标节点
     */
    public void step(String sourceNodeId, String targetNodeId) {
        if (sourceNodeId != null) {
            WorkflowNode sourceNode = getNode(sourceNodeId);
            if (sourceNode != null && !sourceNode.isCompleted()) {
                log.warn("Stepping to next node but source node {} is not completed", sourceNode.getName());
            }
        }
        
        WorkflowNode targetNode = getNode(targetNodeId);
        if (targetNode != null) {
            targetNode.markRunning();
            this.currentNodeId = targetNodeId;
            log.info("Stepped to node: {} ({})", targetNode.getName(), targetNodeId);
        }
    }

    /**
     * 标记节点为完成状态
     */
    public void completeNode(String nodeId, Object result) {
        WorkflowNode node = getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        
        node.markCompleted(result);
        completedNodes.add(nodeId);
        log.info("Node completed: {} ({})", node.getName(), nodeId);
        
        // 更新后继节点状态
        updateSuccessorStates(nodeId);
    }

    /**
     * 标记节点为失败状态
     */
    public void failNode(String nodeId, String errorMessage) {
        WorkflowNode node = getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        
        node.markFailed(errorMessage);
        failedNodes.add(nodeId);
        log.error("Node failed: {} ({}), error: {}", node.getName(), nodeId, errorMessage);
    }

    /**
     * 更新后继节点的状态（如果所有前置节点都已完成，则标记为 READY）
     */
    private void updateSuccessorStates(String nodeId) {
        WorkflowNode node = getNode(nodeId);
        if (node == null) {
            return;
        }
        
        for (String successorId : node.getSuccessors()) {
            WorkflowNode successor = getNode(successorId);
            if (successor != null && successor.getState() == WorkflowNode.NodeState.PENDING) {
                if (areAllPredecessorsCompleted(successorId)) {
                    successor.markReady();
                    log.debug("Node {} is now ready", successor.getName());
                }
            }
        }
    }

    /**
     * 检查节点的所有前置节点是否都已完成
     */
    public boolean areAllPredecessorsCompleted(String nodeId) {
        WorkflowNode node = getNode(nodeId);
        if (node == null) {
            return false;
        }
        
        return node.getPredecessors().stream()
                .allMatch(predId -> {
                    WorkflowNode pred = getNode(predId);
                    return pred != null && pred.isCompleted();
                });
    }

    /**
     * 获取所有就绪的节点
     */
    public List<WorkflowNode> getReadyNodes() {
        return nodes.values().stream()
                .filter(node -> node.getState() == WorkflowNode.NodeState.READY)
                .collect(Collectors.toList());
    }

    /**
     * 检查工作流是否完成
     */
    public boolean isComplete() {
        // 所有终止节点都已完成
        List<WorkflowNode> terminalNodes = findTerminalNodes();
        return terminalNodes.stream().allMatch(WorkflowNode::isCompleted);
    }

    /**
     * 检查工作流是否失败
     */
    public boolean isFailed() {
        return !failedNodes.isEmpty();
    }

    /**
     * 获取执行进度百分比
     */
    public double getProgress() {
        if (nodes.isEmpty()) {
            return 0.0;
        }
        return (double) completedNodes.size() / nodes.size() * 100;
    }

    /**
     * 重置工作流图状态（用于重新执行）
     */
    public void reset() {
        completedNodes.clear();
        failedNodes.clear();
        currentNodeId = null;
        
        nodes.values().forEach(node -> {
            node.setState(WorkflowNode.NodeState.PENDING);
            node.setResult(null);
            node.setErrorMessage(null);
        });
        
        log.info("Workflow graph reset");
    }

    /**
     * 获取节点总数
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 验证图结构的有效性
     */
    public void validate() {
        // 检查是否有初始节点
        List<WorkflowNode> initialNodes = findInitialNodes();
        if (initialNodes.isEmpty()) {
            throw new IllegalStateException("Workflow graph has no initial nodes");
        }
        
        // 检查是否有终止节点
        List<WorkflowNode> terminalNodes = findTerminalNodes();
        if (terminalNodes.isEmpty()) {
            throw new IllegalStateException("Workflow graph has no terminal nodes");
        }
        
        // 检查是否有孤立节点
        for (WorkflowNode node : nodes.values()) {
            if (node.getPredecessors().isEmpty() && node.getSuccessors().isEmpty() && nodes.size() > 1) {
                throw new IllegalStateException("Workflow graph has isolated node: " + node.getName());
            }
        }
        
        log.info("Workflow graph validation passed");
    }

    /**
     * 获取工作流的拓扑排序
     */
    public List<WorkflowNode> getTopologicalOrder() {
        List<WorkflowNode> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (WorkflowNode node : nodes.values()) {
            if (!visited.contains(node.getNodeId())) {
                topologicalSort(node.getNodeId(), visited, visiting, result);
            }
        }
        
        return result;
    }

    private void topologicalSort(String nodeId, Set<String> visited, Set<String> visiting, List<WorkflowNode> result) {
        if (visiting.contains(nodeId)) {
            throw new IllegalStateException("Workflow graph contains cycle at node: " + nodeId);
        }
        
        if (visited.contains(nodeId)) {
            return;
        }
        
        visiting.add(nodeId);
        WorkflowNode node = getNode(nodeId);
        
        if (node != null) {
            for (String successorId : node.getSuccessors()) {
                topologicalSort(successorId, visited, visiting, result);
            }
        }
        
        visiting.remove(nodeId);
        visited.add(nodeId);
        result.add(0, node);
    }
}
