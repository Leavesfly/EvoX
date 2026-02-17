package io.leavesfly.evox.workflow.execution;

import io.leavesfly.evox.core.agent.IAgent;
import io.leavesfly.evox.core.agent.IAgentManager;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import io.leavesfly.evox.workflow.node.NodeHandler;
import io.leavesfly.evox.workflow.node.NodeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行器 - 负责执行工作流中的节点
 * 对应 Python 版本的 WorkFlowManager 部分功能
 */
@Slf4j
public class WorkflowExecutor {

    private final Workflow workflow;
    private final IAgentManager agentManager;
    private WorkflowContext context;
    private final NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();

    public WorkflowExecutor(Workflow workflow, IAgentManager agentManager) {
        this.workflow = workflow;
        this.agentManager = agentManager;
    }

    /**
     * 注册节点处理器
     *
     * @param handlerName 处理器名称
     * @param handler     处理器实例
     */
    public void registerHandler(String handlerName, NodeHandler handler) {
        handlerRegistry.register(handlerName, handler);
    }

    /**
     * 获取节点处理器注册表
     */
    public NodeHandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    /**
     * 执行工作流
     */
    public Mono<String> execute(WorkflowContext context) {
        this.context = context;
        context.updateState(WorkflowContext.ExecutionState.RUNNING);

        return executeNodes()
                .then(Mono.defer(()-> extractOutput()))
                .doOnError(error -> {
                    context.markFailed(error.getMessage());
                    log.error("Workflow execution failed", error);
                });
    }

    /**
     * 执行所有节点
     */
    private Mono<Void> executeNodes() {
        return Mono.defer(() -> {
            WorkflowGraph graph = workflow.getGraph();
            int stepCount = 0;

            while (!graph.isComplete() && !graph.isFailed() && stepCount < workflow.getMaxExecutionSteps()) {
                try {
                    // 获取下一个要执行的节点
                    WorkflowNode nextNode = getNextNode();
                    if (nextNode == null) {
                        log.warn("No next node to execute, breaking execution loop");
                        break;
                    }

                    log.info("Executing node: {} (step {})", nextNode.getName(), stepCount + 1);

                    // 执行节点
                    executeNode(nextNode).block();

                    stepCount++;

                } catch (Exception e) {
                    log.error("Error executing workflow node", e);
                    return Mono.error(e);
                }
            }

            if (stepCount >= workflow.getMaxExecutionSteps()) {
                String errorMsg = "Workflow execution exceeded maximum steps: " + workflow.getMaxExecutionSteps();
                log.error(errorMsg);
                return Mono.error(new RuntimeException(errorMsg));
            }

            context.markCompleted();
            return Mono.empty();
        });
    }

    /**
     * 获取下一个要执行的节点
     */
    private WorkflowNode getNextNode() {
        WorkflowGraph graph = workflow.getGraph();

        // 如果是第一步，获取初始节点
        if (context.getCurrentStep() == 0) {
            List<WorkflowNode> initialNodes = graph.findInitialNodes();
            if (!initialNodes.isEmpty()) {
                WorkflowNode firstNode = initialNodes.get(0);
                firstNode.markReady();
                return firstNode;
            }
            return null;
        }

        // 获取所有就绪的节点
        List<WorkflowNode> readyNodes = graph.getReadyNodes();
        if (readyNodes.isEmpty()) {
            return null;
        }

        // 智能调度策略：按优先级降序排序，返回优先级最高的节点
        readyNodes.sort(Comparator.comparingInt(WorkflowNode::getPriority).reversed());
        WorkflowNode selectedNode = readyNodes.get(0);
        
        log.debug("Selected node {} with priority {}", selectedNode.getName(), selectedNode.getPriority());
        return selectedNode;
    }

    /**
     * 执行单个节点
     */
    private Mono<Void> executeNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                // 更新图状态
                String lastNodeId = context.getLastExecutedTask();
                workflow.getGraph().step(lastNodeId, node.getNodeId());

                // 记录任务执行
                context.recordTaskExecution(node.getName());

                // 根据节点类型执行
                return executeNodeByType(node)
                        .doOnSuccess(result -> {
                            // 标记节点完成
                            workflow.getGraph().completeNode(node.getNodeId(), result);

                            // 更新上下文
                            if (result != null) {
                                context.updateExecutionData(node.getName() + "_output", result);
                            }

                            // 添加响应消息
                            Message message = Message.builder()
                                    .content(result)
                                    .messageType(MessageType.RESPONSE)
                                    .build();
                            context.addMessage(message);

                            log.info("Node {} completed successfully", node.getName());
                        })
                        .doOnError(error -> {
                            // 标记节点失败
                            workflow.getGraph().failNode(node.getNodeId(), error.getMessage());
                            log.error("Node {} failed: {}", node.getName(), error.getMessage());
                        })
                        .then();

            } catch (Exception e) {
                log.error("Failed to execute node: {}", node.getName(), e);
                return Mono.error(e);
            }
        });
    }

    /**
     * 根据节点类型执行
     */
    private Mono<Object> executeNodeByType(WorkflowNode node) {
        return switch (node.getNodeType()) {
            case ACTION -> executeActionNode(node);
            case DECISION -> executeDecisionNode(node);
            case PARALLEL -> executeParallelNode(node);
            case LOOP -> executeLoopNode(node);
            case SUBWORKFLOW -> executeSubWorkflowNode(node);
            case COLLECT -> executeCollectNode(node);
            default -> Mono.error(new UnsupportedOperationException(
                    "Node type not supported: " + node.getNodeType()));
        };
    }

    /**
     * 执行收集节点
     * COLLECT 节点通过注册的 NodeHandler 执行自定义逻辑，
     * 典型场景是向多个 Agent 广播问题并收集所有响应。
     */
    private Mono<Object> executeCollectNode(WorkflowNode node) {
        return Mono.defer(() -> {
            String handlerName = node.getHandlerName();
            if (handlerName == null || handlerName.isEmpty()) {
                return Mono.error(new RuntimeException(
                        "COLLECT node '" + node.getName() + "' has no handlerName configured"));
            }

            NodeHandler handler = handlerRegistry.getHandler(handlerName);
            if (handler == null) {
                return Mono.error(new RuntimeException(
                        "NodeHandler not found: '" + handlerName + "' for node: " + node.getName()));
            }

            log.debug("Executing COLLECT node: {} with handler: {}", node.getName(), handlerName);
            return handler.handle(context, node);
        });
    }

    /**
     * 执行动作节点
     */
    private Mono<Object> executeActionNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                String agentName = node.getAgentName();
                String actionName = node.getActionName();

                if (agentManager == null) {
                    return Mono.error(new RuntimeException(
                            "AgentManager not available, cannot execute action node: " + node.getName()));
                }

                if (agentName == null || agentName.isEmpty()) {
                    return Mono.error(new RuntimeException(
                            "Node '" + node.getName() + "' has no agentName configured"));
                }

                IAgent agent = agentManager.getAgent(agentName);
                if (agent == null) {
                    return Mono.error(new RuntimeException(
                            "Agent not found: '" + agentName + "' for node: " + node.getName()));
                }

                log.debug("Executing action node: {} with agent: {}, action: {}",
                        node.getName(), agentName, actionName);

                List<Message> messages = context.getMessages();

                return agent.executeAsync(actionName, messages)
                        .map(message -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("node", node.getName());
                            result.put("agent", agentName);
                            result.put("action", actionName);
                            result.put("status", "completed");
                            result.put("message", message.getContent());
                            return result;
                        })
                        .onErrorResume(error -> {
                            log.error("Action execution failed for node {}: {}", node.getName(), error.getMessage());
                            return Mono.error(new RuntimeException(
                                    "Action execution failed: " + error.getMessage(), error));
                        });

            } catch (Exception e) {
                log.error("Error preparing action node execution", e);
                return Mono.error(e);
            }
        });
    }

    /**
     * 执行决策节点
     * 决策节点根据条件表达式选择下一个执行的节点
     */
    private Mono<Object> executeDecisionNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                log.debug("Executing decision node: {}", node.getName());
                
                // 获取条件表达式
                String condition = node.getCondition();
                if (condition == null || condition.isEmpty()) {
                    log.warn("Decision node {} has no condition, using first branch", node.getName());
                    return evaluateDefaultBranch(node);
                }
                
                // 评估条件
                Object conditionResult = evaluateCondition(condition);
                log.debug("Condition '{}' evaluated to: {}", condition, conditionResult);
                
                // 根据结果选择分支
                String selectedBranch = selectBranch(node, conditionResult);
                if (selectedBranch == null) {
                    log.warn("No matching branch for condition result: {}", conditionResult);
                    return Mono.just(createDecisionResult(node, conditionResult, null));
                }
                
                log.info("Decision node {} selected branch: {}", node.getName(), selectedBranch);
                
                // 标记其他分支为 SKIPPED
                markUnselectedBranches(node, selectedBranch);
                
                // 返回决策结果
                return Mono.just(createDecisionResult(node, conditionResult, selectedBranch));
                
            } catch (Exception e) {
                log.error("Error executing decision node: {}", node.getName(), e);
                return Mono.error(new RuntimeException("Decision node execution failed: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 评估条件表达式
     * 支持简单的条件判断
     */
    private Object evaluateCondition(String condition) {
        // 简单实现：支持基本的条件表达式
        // 例如："result.success == true", "count > 5", "status == 'completed'"
        
        try {
            // 处理 context.开头的表达式
            if (condition.startsWith("context.")) {
                return evaluateContextCondition(condition);
            }
            
            // 处理布尔值
            if ("true".equalsIgnoreCase(condition)) {
                return true;
            }
            if ("false".equalsIgnoreCase(condition)) {
                return false;
            }
            
            // 处理比较表达式
            if (condition.contains("==")) {
                return evaluateEqualsCondition(condition);
            }
            if (condition.contains("!=")) {
                return evaluateNotEqualsCondition(condition);
            }
            if (condition.contains(">")) {
                return evaluateGreaterThanCondition(condition);
            }
            if (condition.contains("<")) {
                return evaluateLessThanCondition(condition);
            }
            
            // 默认返回 true
            log.warn("Cannot parse condition: {}, defaulting to true", condition);
            return true;
            
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition, e);
            return false;
        }
    }
    
    /**
     * 评估上下文条件
     */
    private Object evaluateContextCondition(String condition) {
        // 例如：context.lastResult.success == true
        String expr = condition.substring("context.".length()).trim();
        
        // 简单实现：检查 executionData 中的值
        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            if (parts.length == 2) {
                String key = parts[0].trim();
                String expectedValue = parts[1].trim().replace("'", "").replace("\"", "");
                Object actualValue = context.getExecutionData(key);
                return actualValue != null && actualValue.toString().equals(expectedValue);
            }
        }
        
        return false;
    }
    
    /**
     * 评估相等条件
     */
    private boolean evaluateEqualsCondition(String condition) {
        String[] parts = condition.split("==");
        if (parts.length != 2) return false;
        
        String left = parts[0].trim();
        String right = parts[1].trim().replace("'", "").replace("\"", "");
        
        // 从上下文获取值
        Object leftValue = context.getExecutionData(left);
        if (leftValue == null) return false;
        
        return leftValue.toString().equals(right);
    }
    
    /**
     * 评估不相等条件
     */
    private boolean evaluateNotEqualsCondition(String condition) {
        return !evaluateEqualsCondition(condition.replace("!=", "=="));
    }
    
    /**
     * 评估大于条件
     */
    private boolean evaluateGreaterThanCondition(String condition) {
        String[] parts = condition.split(">");
        if (parts.length != 2) return false;
        
        try {
            String left = parts[0].trim();
            double rightValue = Double.parseDouble(parts[1].trim());
            
            Object leftObj = context.getExecutionData(left);
            if (leftObj == null) return false;
            
            double leftValue = Double.parseDouble(leftObj.toString());
            return leftValue > rightValue;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 评估小于条件
     */
    private boolean evaluateLessThanCondition(String condition) {
        String[] parts = condition.split("<");
        if (parts.length != 2) return false;
        
        try {
            String left = parts[0].trim();
            double rightValue = Double.parseDouble(parts[1].trim());
            
            Object leftObj = context.getExecutionData(left);
            if (leftObj == null) return false;
            
            double leftValue = Double.parseDouble(leftObj.toString());
            return leftValue < rightValue;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 选择分支
     */
    private String selectBranch(WorkflowNode node, Object conditionResult) {
        Map<String, String> branches = node.getBranches();
        if (branches == null || branches.isEmpty()) {
            // 如果没有分支配置，返回第一个后继节点
            List<String> successors = node.getSuccessors();
            return successors.isEmpty() ? null : successors.get(0);
        }
        
        // 尝试直接匹配
        String resultStr = conditionResult.toString();
        if (branches.containsKey(resultStr)) {
            return branches.get(resultStr);
        }
        
        // 尝试 true/false 匹配
        if (conditionResult instanceof Boolean) {
            boolean boolResult = (Boolean) conditionResult;
            if (branches.containsKey("true") && boolResult) {
                return branches.get("true");
            }
            if (branches.containsKey("false") && !boolResult) {
                return branches.get("false");
            }
        }
        
        // 如果没有匹配，尝试 "default" 分支
        if (branches.containsKey("default")) {
            return branches.get("default");
        }
        
        return null;
    }
    
    /**
     * 评估默认分支
     */
    private Mono<Object> evaluateDefaultBranch(WorkflowNode node) {
        List<String> successors = node.getSuccessors();
        String selectedBranch = successors.isEmpty() ? null : successors.get(0);
        return Mono.just(createDecisionResult(node, true, selectedBranch));
    }
    
    /**
     * 标记未选中的分支为 SKIPPED
     */
    private void markUnselectedBranches(WorkflowNode node, String selectedBranch) {
        if (selectedBranch == null) return;
        
        for (String successorId : node.getSuccessors()) {
            if (!successorId.equals(selectedBranch)) {
                WorkflowNode successorNode = workflow.getGraph().getNode(successorId);
                if (successorNode != null) {
                    successorNode.setState(WorkflowNode.NodeState.SKIPPED);
                    log.debug("Marked node {} as SKIPPED", successorNode.getName());
                }
            }
        }
    }
    
    /**
     * 创建决策结果
     */
    private Map<String, Object> createDecisionResult(WorkflowNode node, Object conditionResult, String selectedBranch) {
        Map<String, Object> result = new HashMap<>();
        result.put("node", node.getName());
        result.put("type", "decision");
        result.put("condition_result", conditionResult);
        result.put("selected_branch", selectedBranch);
        result.put("status", "completed");
        return result;
    }

    /**
     * 执行并行节点
     * 并行节点会同时执行多个子节点，根据策略决定如何等待结果
     */
    private Mono<Object> executeParallelNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                log.debug("Executing parallel node: {}", node.getName());
                
                // 获取并行执行的子节点列表
                List<String> parallelNodeIds = node.getParallelNodes();
                if (parallelNodeIds == null || parallelNodeIds.isEmpty()) {
                    // 如果没有配置并行节点，使用所有后继节点
                    parallelNodeIds = node.getSuccessors();
                }
                
                if (parallelNodeIds.isEmpty()) {
                    log.warn("Parallel node {} has no child nodes", node.getName());
                    return Mono.just(createParallelResult(node, List.of(), "no_children"));
                }
                
                log.info("Parallel node {} executing {} child nodes", node.getName(), parallelNodeIds.size());
                
                // 获取执行策略
                WorkflowNode.ParallelStrategy strategy = node.getParallelStrategy();
                if (strategy == null) {
                    strategy = WorkflowNode.ParallelStrategy.ALL;
                }
                
                // 根据策略执行并行节点
                return switch (strategy) {
                    case ALL -> executeParallelAll(node, parallelNodeIds);
                    case ANY -> executeParallelAny(node, parallelNodeIds);
                    case FIRST -> executeParallelFirst(node, parallelNodeIds);
                };
                
            } catch (Exception e) {
                log.error("Error executing parallel node: {}", node.getName(), e);
                return Mono.error(new RuntimeException("Parallel node execution failed: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 并行执行所有节点，等待全部完成（ALL策略）
     */
    private Mono<Object> executeParallelAll(WorkflowNode parallelNode, List<String> nodeIds) {
        WorkflowGraph graph = workflow.getGraph();
        
        // 创建所有子节点的执行 Mono
        List<Mono<Map<String, Object>>> nodeTasks = nodeIds.stream()
                .map(nodeId -> {
                    WorkflowNode childNode = graph.getNode(nodeId);
                    if (childNode == null) {
                        log.warn("Child node {} not found", nodeId);
                        return Mono.just(createNodeResult(nodeId, null, "not_found"));
                    }
                    
                    // 标记节点为就绪
                    childNode.markReady();
                    
                    // 执行节点
                    return executeNodeByType(childNode)
                            .map(result -> {
                                // 标记节点完成
                                graph.completeNode(nodeId, result);
                                log.debug("Parallel child node {} completed", childNode.getName());
                                return createNodeResult(nodeId, result, "completed");
                            })
                            .onErrorResume(error -> {
                                // 标记节点失败
                                graph.failNode(nodeId, error.getMessage());
                                log.error("Parallel child node {} failed: {}", childNode.getName(), error.getMessage());
                                return Mono.just(createNodeResult(nodeId, null, "failed", error.getMessage()));
                            });
                })
                .toList();
        
        // 使用 Flux.merge 并行执行所有节点，然后收集结果
        return Flux.merge(nodeTasks)
                .collectList()
                .map(results -> createParallelResult(parallelNode, results, "all_completed"));
    }
    
    /**
     * 并行执行，任意一个完成即可（ANY策略）
     */
    private Mono<Object> executeParallelAny(WorkflowNode parallelNode, List<String> nodeIds) {
        WorkflowGraph graph = workflow.getGraph();
        
        // 创建所有子节点的执行 Mono
        List<Mono<Map<String, Object>>> nodeTasks = nodeIds.stream()
                .map(nodeId -> {
                    WorkflowNode childNode = graph.getNode(nodeId);
                    if (childNode == null) {
                        return Mono.<Map<String, Object>>empty();
                    }
                    
                    childNode.markReady();
                    
                    return executeNodeByType(childNode)
                            .map(result -> {
                                graph.completeNode(nodeId, result);
                                return createNodeResult(nodeId, result, "completed");
                            });
                })
                .toList();
        
        // 使用 Mono.firstWithValue 获取第一个完成的结果
        return Mono.firstWithValue(nodeTasks)
                .map(result -> (Object) createParallelResult(parallelNode, List.of(result), "any_completed"))
                .switchIfEmpty(Mono.just(createParallelResult(parallelNode, List.of(), "no_results")));
    }
    
    /**
     * 并行执行，返回第一个完成的节点（FIRST策略）
     */
    private Mono<Object> executeParallelFirst(WorkflowNode parallelNode, List<String> nodeIds) {
        // FIRST 策略与 ANY 类似，但会取消其他任务
        return executeParallelAny(parallelNode, nodeIds);
    }
    
    /**
     * 创建子节点执行结果
     */
    private Map<String, Object> createNodeResult(String nodeId, Object result, String status) {
        return createNodeResult(nodeId, result, status, null);
    }
    
    /**
     * 创建子节点执行结果（带错误信息）
     */
    private Map<String, Object> createNodeResult(String nodeId, Object result, String status, String error) {
        Map<String, Object> nodeResult = new HashMap<>();
        nodeResult.put("node_id", nodeId);
        nodeResult.put("status", status);
        if (result != null) {
            nodeResult.put("result", result);
        }
        if (error != null) {
            nodeResult.put("error", error);
        }
        return nodeResult;
    }
    
    /**
     * 创建并行节点结果
     */
    private Map<String, Object> createParallelResult(WorkflowNode node, List<Map<String, Object>> childResults, String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("node", node.getName());
        result.put("type", "parallel");
        result.put("strategy", node.getParallelStrategy());
        result.put("child_count", childResults.size());
        result.put("child_results", childResults);
        result.put("status", status);
        return result;
    }

    /**
     * 执行循环节点
     * 循环节点会重复执行循环体，直到条件不满足或达到最大迭代次数
     */
    private Mono<Object> executeLoopNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                log.debug("Executing loop node: {}", node.getName());
                
                // 获取循环体节点
                String loopBodyId = node.getLoopBodyNodeId();
                if (loopBodyId == null || loopBodyId.isEmpty()) {
                    // 如果没有指定循环体，使用第一个后继节点
                    List<String> successors = node.getSuccessors();
                    if (successors.isEmpty()) {
                        log.warn("Loop node {} has no loop body", node.getName());
                        return Mono.just(createLoopResult(node, List.of(), 0, "no_body"));
                    }
                    loopBodyId = successors.get(0);
                }
                
                WorkflowNode loopBody = workflow.getGraph().getNode(loopBodyId);
                if (loopBody == null) {
                    log.error("Loop body node {} not found", loopBodyId);
                    return Mono.error(new RuntimeException("Loop body node not found: " + loopBodyId));
                }
                
                // 获取循环条件和最大迭代次数
                String condition = node.getLoopCondition();
                int maxIterations = node.getMaxIterations();
                if (maxIterations <= 0) {
                    maxIterations = 100; // 默认最大100次
                }
                
                log.info("Loop node {} starting, max iterations: {}", node.getName(), maxIterations);
                
                // 执行循环
                return executeLoop(node, loopBody, condition, maxIterations);
                
            } catch (Exception e) {
                log.error("Error executing loop node: {}", node.getName(), e);
                return Mono.error(new RuntimeException("Loop node execution failed: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 执行循环逻辑
     */
    private Mono<Object> executeLoop(WorkflowNode loopNode, WorkflowNode loopBody, 
                                      String condition, int maxIterations) {
        List<Map<String, Object>> iterationResults = new java.util.ArrayList<>();
        
        return Mono.defer(() -> {
            int iteration = 0;
            
            while (iteration < maxIterations) {
                // 检查循环条件
                if (condition != null && !condition.isEmpty()) {
                    Object conditionResult = evaluateCondition(condition);
                    boolean shouldContinue = conditionResult instanceof Boolean && (Boolean) conditionResult;
                    
                    if (!shouldContinue) {
                        log.info("Loop condition '{}' evaluated to false, exiting after {} iterations", 
                                condition, iteration);
                        break;
                    }
                }
                
                // 更新当前迭代次数
                loopNode.setCurrentIteration(iteration);
                context.updateExecutionData("loop_iteration", iteration);
                
                log.debug("Loop iteration {} starting", iteration);
                
                // 标记循环体为就绪
                loopBody.markReady();
                
                try {
                    // 执行循环体
                    Object result = executeNodeByType(loopBody).block();
                    
                    // 记录迭代结果
                    Map<String, Object> iterResult = new HashMap<>();
                    iterResult.put("iteration", iteration);
                    iterResult.put("result", result);
                    iterationResults.add(iterResult);
                    
                    // 标记循环体完成
                    workflow.getGraph().completeNode(loopBody.getNodeId(), result);
                    
                    // 重置循环体状态以便下次迭代
                    loopBody.setState(WorkflowNode.NodeState.READY);
                    
                    log.debug("Loop iteration {} completed", iteration);
                    
                } catch (Exception e) {
                    log.error("Loop iteration {} failed: {}", iteration, e.getMessage());
                    // 循环体执行失败，记录错误并继续或退出
                    Map<String, Object> iterResult = new HashMap<>();
                    iterResult.put("iteration", iteration);
                    iterResult.put("status", "failed");
                    iterResult.put("error", e.getMessage());
                    iterationResults.add(iterResult);
                    
                    // 可以选择在失败时退出循环
                    break;
                }
                
                iteration++;
            }
            
            if (iteration >= maxIterations) {
                log.warn("Loop node {} reached maximum iterations: {}", loopNode.getName(), maxIterations);
            }
            
            loopNode.setCurrentIteration(iteration);
            
            return Mono.just(createLoopResult(loopNode, iterationResults, iteration, "completed"));
        });
    }
    
    /**
     * 创建循环节点结果
     */
    private Map<String, Object> createLoopResult(WorkflowNode node, 
                                                  List<Map<String, Object>> iterationResults,
                                                  int iterations, String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("node", node.getName());
        result.put("type", "loop");
        result.put("iterations", iterations);
        result.put("max_iterations", node.getMaxIterations());
        result.put("condition", node.getLoopCondition());
        result.put("iteration_results", iterationResults);
        result.put("status", status);
        return result;
    }

    /**
     * 执行子工作流节点
     * 子工作流节点会启动一个嵌套的工作流执行
     */
    private Mono<Object> executeSubWorkflowNode(WorkflowNode node) {
        return Mono.defer(() -> {
            try {
                log.debug("Executing subworkflow node: {}", node.getName());
                
                // 获取子工作流
                Workflow subWorkflow = node.getSubWorkflow();
                if (subWorkflow == null) {
                    log.error("Subworkflow node {} has no subworkflow defined", node.getName());
                    return Mono.error(new RuntimeException("No subworkflow defined for node: " + node.getName()));
                }
                
                // 准备子工作流的输入数据
                Map<String, Object> subWorkflowInput = prepareSubWorkflowInput(node);
                
                log.info("Starting subworkflow: {} with input: {}", 
                         subWorkflow.getName(), subWorkflowInput.keySet());
                
                // 执行子工作流
                String subWorkflowResult = subWorkflow.execute(subWorkflowInput);
                
                // 处理子工作流的输出
                Map<String, Object> processedOutput = processSubWorkflowOutput(node, subWorkflowResult);
                
                log.info("Subworkflow {} completed successfully", subWorkflow.getName());
                
                return Mono.just(createSubWorkflowResult(node, subWorkflowResult, processedOutput));
                
            } catch (Exception e) {
                log.error("Error executing subworkflow node: {}", node.getName(), e);
                return Mono.error(new RuntimeException("Subworkflow node execution failed: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * 准备子工作流的输入数据
     * 根据输入映射从父工作流上下文中提取数据
     */
    private Map<String, Object> prepareSubWorkflowInput(WorkflowNode node) {
        Map<String, Object> input = new HashMap<>();
        Map<String, String> inputMapping = node.getSubWorkflowInputMapping();
        
        if (inputMapping == null || inputMapping.isEmpty()) {
            // 如果没有配置映射，传递整个上下文
            log.debug("No input mapping defined, passing entire context");
            return new HashMap<>(context.getExecutionData());
        }
        
        // 根据映射提取数据
        for (Map.Entry<String, String> entry : inputMapping.entrySet()) {
            String subWorkflowParam = entry.getKey();  // 子工作流参数名
            String contextField = entry.getValue();     // 父上下文字段名
            
            Object value = context.getExecutionData().get(contextField);
            if (value != null) {
                input.put(subWorkflowParam, value);
                log.debug("Mapping {} -> {} = {}", contextField, subWorkflowParam, value);
            } else {
                log.warn("Context field '{}' not found for subworkflow input '{}'", 
                        contextField, subWorkflowParam);
            }
        }
        
        return input;
    }
    
    /**
     * 处理子工作流的输出
     * 根据输出映射将子工作流结果映射回父工作流上下文
     */
    private Map<String, Object> processSubWorkflowOutput(WorkflowNode node, String subWorkflowResult) {
        Map<String, Object> output = new HashMap<>();
        Map<String, String> outputMapping = node.getSubWorkflowOutputMapping();
        
        if (outputMapping == null || outputMapping.isEmpty()) {
            // 如果没有配置映射，直接返回结果
            log.debug("No output mapping defined, returning result as is");
            output.put("result", subWorkflowResult);
            // 将结果存储到上下文
            context.updateExecutionData(node.getNodeId() + "_result", subWorkflowResult);
            return output;
        }
        
        // 根据映射处理输出
        // 这里简化处理，实际应用中可能需要从 JSON 结果中提取字段
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            String contextField = entry.getKey();      // 父上下文字段名
            String subWorkflowField = entry.getValue(); // 子工作流结果字段名
            
            // 简化处理：直接使用结果
            output.put(contextField, subWorkflowResult);
            
            // 更新父工作流上下文
            context.updateExecutionData(contextField, subWorkflowResult);
            
            log.debug("Mapping subworkflow.{} -> context.{}", subWorkflowField, contextField);
        }
        
        return output;
    }
    
    /**
     * 创建子工作流节点结果
     */
    private Map<String, Object> createSubWorkflowResult(WorkflowNode node, 
                                                         String subWorkflowResult,
                                                         Map<String, Object> processedOutput) {
        Map<String, Object> result = new HashMap<>();
        result.put("node", node.getName());
        result.put("type", "subworkflow");
        result.put("subworkflow_name", node.getSubWorkflow() != null ? node.getSubWorkflow().getName() : "unknown");
        result.put("subworkflow_result", subWorkflowResult);
        result.put("processed_output", processedOutput);
        result.put("status", "completed");
        return result;
    }

    /**
     * 提取工作流输出
     */
    private Mono<String> extractOutput() {
        log.info("Extracting workflow output...");

        // 简单实现：返回最后一个节点的输出
        String lastTask = context.getLastExecutedTask();
        if (lastTask != null) {
            Object lastOutput = context.getExecutionData(lastTask + "_output");
            if (lastOutput != null) {
                return Mono.just(lastOutput.toString());
            }
        }

        // 返回执行轨迹
        String trajectory = "Workflow completed. Execution trajectory: " + context.getTaskExecutionTrajectory();
        log.info("Workflow result: {}", trajectory);
        return Mono.just(trajectory);
    }

    /**
     * 重置执行器
     */
    public void reset() {
        this.context = null;
        log.debug("Workflow executor reset");
    }
}
