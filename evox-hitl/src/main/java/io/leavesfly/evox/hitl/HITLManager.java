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
 * HITL Manager for handling human-in-the-loop interactions.
 * Manages approval requests, user input collection, and human feedback.
 */
@Slf4j
@Data
public class HITLManager {

    /**
     * Whether HITL is currently active
     */
    private boolean active = false;

    /**
     * Pending approval requests
     */
    private final Map<String, CompletableFuture<HITLResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Input/output field mapping for workflow integration
     */
    private Map<String, String> hitlInputOutputMapping = new HashMap<>();

    /**
     * Default timeout for human responses (in seconds)
     */
    private long defaultTimeout = 1800; // 30 minutes

    /**
     * Scanner for CLI input
     */
    private transient Scanner scanner;

    public HITLManager() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Activate HITL feature.
     */
    public void activate() {
        this.active = true;
        log.info("HITL feature activated");
    }

    /**
     * Deactivate HITL feature.
     */
    public void deactivate() {
        this.active = false;
        log.info("HITL feature deactivated");
    }

    /**
     * Check if HITL is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Request human approval for an action.
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
            // HITL not active, auto-approve
            return Mono.just(HITLResponse.builder()
                    .requestId("auto_approved")
                    .decision(HITLDecision.APPROVE)
                    .feedback("HITL not active - auto approved")
                    .build());
        }

        // Build context
        HITLContext context = HITLContext.builder()
                .taskName(taskName)
                .agentName(agentName)
                .actionName(actionName)
                .workflowGoal(workflowGoal)
                .actionInputs(actionInputsData != null ? actionInputsData : new HashMap<>())
                .executionResult(executionResult)
                .displayContext(displayContext != null ? displayContext : new HashMap<>())
                .build();

        // Generate prompt message
        String promptMessage = generatePromptMessage(interactionType, mode, context);

        // Create request
        HITLRequest request = HITLRequest.builder()
                .interactionType(interactionType)
                .mode(mode)
                .context(context)
                .promptMessage(promptMessage)
                .build();

        // Handle interaction
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
     * Handle CLI interaction with user.
     */
    private Mono<HITLResponse> handleCLIInteraction(HITLRequest request) {
        return Mono.fromCallable(() -> {
            // Display request
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔔 Human-in-the-Loop Approval Request");
            System.out.println("=".repeat(80));
            System.out.println(request.getPromptMessage());
            System.out.println("=".repeat(80));

            // Get user decision based on interaction type
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
     * Handle approve/reject interaction.
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
     * Handle user input collection.
     */
    private HITLResponse handleUserInputCollection(HITLRequest request) {
        System.out.println("\nPlease provide the requested information:");
        
        Map<String, Object> collectedData = new HashMap<>();
        
        // Simple implementation: collect one input
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
     * Generate prompt message for display.
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
     * Close resources.
     */
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
