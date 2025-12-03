package io.leavesfly.evox.hitl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HITL管理器,用于处理人在回路中(Human-in-the-Loop)交互。
 * 管理批准请求、用户输入收集和人类反馈。
 */
@Slf4j
@Data
public class HITLManager {

    /**
     * HITL是否当前激活
     */
    private boolean active = false;

    /**
     * 待处理的批准请求
     */
    private final Map<String, CompletableFuture<HITLResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 用于工作流集成的输入/输出字段映射
     */
    private Map<String, String> hitlInputOutputMapping = new HashMap<>();

    /**
     * 人类响应的默认超时时间(秒)
     */
    private long defaultTimeout = 1800; // 30分钟

    /**
     * 用于CLI输入的扫描器
     */
    private transient Scanner scanner;

    public HITLManager() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * 激活HITL功能
     */
    public void activate() {
        this.active = true;
        log.info("HITL feature activated");
    }

    /**
     * 停用HITL功能
     */
    public void deactivate() {
        this.active = false;
        log.info("HITL feature deactivated");
    }

    /**
     * 检查HITL是否激活
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 请求人类批准某个动作
     */
    public Mono<HITLResponse> requestApproval(
            String taskName,
            String agentName,
            String actionName,
            HITLInteractionType interactionType,
            HITLMode mode,
            Map<String, Object> actionInputsData,
            Object executionResult,
            String workflowGoal,
            Map<String, Object> displayContext
    ) {
        if (!active) {
            // HITL未激活,自动批准
            return Mono.just(HITLResponse.builder()
                    .requestId("auto_approved")
                    .decision(HITLDecision.APPROVE)
                    .feedback("HITL not active - auto approved")
                    .build());
        }

        // 构建上下文
        HITLContext context = HITLContext.builder()
                .taskName(taskName)
                .agentName(agentName)
                .actionName(actionName)
                .workflowGoal(workflowGoal)
                .actionInputs(actionInputsData != null ? actionInputsData : new HashMap<>())
                .executionResult(executionResult)
                .displayContext(displayContext != null ? displayContext : new HashMap<>())
                .build();

        // 生成提示信息
        String promptMessage = generatePromptMessage(interactionType, mode, context);

        // 创建请求
        HITLRequest request = HITLRequest.builder()
                .interactionType(interactionType)
                .mode(mode)
                .context(context)
                .promptMessage(promptMessage)
                .build();

        // 处理交互
        return handleCLIInteraction(request)
                .timeout(Duration.ofSeconds(defaultTimeout))
                .onErrorResume(error -> {
                    log.error("HITL request error: {}", error.getMessage());
                    return Mono.just(HITLResponse.builder()
                            .requestId(request.getRequestId())
                            .decision(HITLDecision.REJECT)
                            .feedback("Error: " + error.getMessage())
                            .build());
                });
    }

    /**
     * 处理与用户的CLI交互
     */
    private Mono<HITLResponse> handleCLIInteraction(HITLRequest request) {
        return Mono.fromCallable(() -> {
            // 显示请求
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔔 Human-in-the-Loop Approval Request");
            System.out.println("=".repeat(80));
            System.out.println(request.getPromptMessage());
            System.out.println("=".repeat(80));

            // 根据交互类型获取用户决策
            if (request.getInteractionType() == HITLInteractionType.APPROVE_REJECT) {
                return handleApproveReject(request);
            } else if (request.getInteractionType() == HITLInteractionType.COLLECT_USER_INPUT) {
                return handleUserInputCollection(request);
            } else {
                log.warn("Unsupported interaction type: {}", request.getInteractionType());
                return HITLResponse.builder()
                        .requestId(request.getRequestId())
                        .decision(HITLDecision.REJECT)
                        .feedback("Unsupported interaction type")
                        .build();
            }
        });
    }

    /**
     * 处理批准/拒绝交互
     */
    private HITLResponse handleApproveReject(HITLRequest request) {
        System.out.print("\nPlease select [a]pprove / [r]eject: ");
        
        String choice = scanner.nextLine().toLowerCase().trim();
        HITLDecision decision;
        
        if ("a".equals(choice) || "approve".equals(choice)) {
            decision = HITLDecision.APPROVE;
        } else if ("r".equals(choice) || "reject".equals(choice)) {
            decision = HITLDecision.REJECT;
        } else {
            System.out.println("Invalid input, defaulting to REJECT");
            decision = HITLDecision.REJECT;
        }

        String feedback = "";
        if (decision == HITLDecision.REJECT) {
            System.out.print("Please provide reason for rejection (optional): ");
            feedback = scanner.nextLine().trim();
        }

        return HITLResponse.builder()
                .requestId(request.getRequestId())
                .decision(decision)
                .feedback(feedback.isEmpty() ? null : feedback)
                .build();
    }

    /**
     * 处理用户输入收集
     */
    private HITLResponse handleUserInputCollection(HITLRequest request) {
        System.out.println("\nPlease provide the requested information:");
        
        Map<String, Object> collectedData = new HashMap<>();
        
        // 简单实现:收集一个输入
        System.out.print("Input data: ");
        String inputData = scanner.nextLine().trim();
        
        if (!inputData.isEmpty()) {
            collectedData.put("user_input", inputData);
            
            return HITLResponse.builder()
                    .requestId(request.getRequestId())
                    .decision(HITLDecision.CONTINUE)
                    .modifiedContent(collectedData)
                    .feedback("User input collected successfully")
                    .build();
        } else {
            return HITLResponse.builder()
                    .requestId(request.getRequestId())
                    .decision(HITLDecision.REJECT)
                    .feedback("No input provided")
                    .build();
        }
    }

    /**
     * 生成用于显示的提示信息
     */
    private String generatePromptMessage(
            HITLInteractionType interactionType,
            HITLMode mode,
            HITLContext context
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Task: ").append(context.getTaskName()).append("\n");
        sb.append("Agent: ").append(context.getAgentName()).append("\n");
        sb.append("Action: ").append(context.getActionName()).append("\n");
        sb.append("Workflow Goal: ").append(context.getWorkflowGoal() != null ? context.getWorkflowGoal() : "N/A").append("\n");
        sb.append("Mode: ").append(mode == HITLMode.PRE_EXECUTION ? "Pre-Execution Approval" : "Post-Execution Review").append("\n");
        
        if (mode == HITLMode.PRE_EXECUTION) {
            sb.append("\nParameters to be executed:\n");
            context.getActionInputs().forEach((key, value) -> 
                sb.append("  ").append(key).append(": ").append(value).append("\n")
            );
        } else {
            sb.append("\nExecution result:\n");
            sb.append("  ").append(context.getExecutionResult() != null ? context.getExecutionResult() : "None").append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
