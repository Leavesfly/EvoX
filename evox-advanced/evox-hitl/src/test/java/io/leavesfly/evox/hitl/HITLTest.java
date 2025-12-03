package io.leavesfly.evox.hitl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HITL模块的单元测试。
 */
class HITLTest {

    private HITLManager hitlManager;

    @BeforeEach
    void setUp() {
        hitlManager = new HITLManager();
    }

    @Test
    @DisplayName("HITL管理器的激活和停用")
    void testHITLManagerActivation() {
        assertFalse(hitlManager.isActive());

        hitlManager.activate();
        assertTrue(hitlManager.isActive());

        hitlManager.deactivate();
        assertFalse(hitlManager.isActive());
    }

    @Test
    @DisplayName("HITL未激活时自动批准")
    void testAutoApproveWhenNotActive() {
        Mono<HITLResponse> responseMono = hitlManager.requestApproval(
                "test_task",
                "test_agent",
                "test_action",
                HITLInteractionType.APPROVE_REJECT,
                HITLMode.PRE_EXECUTION,
                new HashMap<>(),
                null,
                "test_goal",
                new HashMap<>()
        );

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(HITLDecision.APPROVE, response.getDecision());
                    assertTrue(response.getFeedback().contains("not active"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("HITL决策枚举值")
    void testHITLDecisionEnum() {
        assertEquals(4, HITLDecision.values().length);
        assertNotNull(HITLDecision.valueOf("APPROVE"));
        assertNotNull(HITLDecision.valueOf("REJECT"));
        assertNotNull(HITLDecision.valueOf("MODIFY"));
        assertNotNull(HITLDecision.valueOf("CONTINUE"));
    }

    @Test
    @DisplayName("HITL交互类型枚举值")
    void testHITLInteractionTypeEnum() {
        assertEquals(5, HITLInteractionType.values().length);
        assertNotNull(HITLInteractionType.valueOf("APPROVE_REJECT"));
        assertNotNull(HITLInteractionType.valueOf("COLLECT_USER_INPUT"));
        assertNotNull(HITLInteractionType.valueOf("REVIEW_EDIT_STATE"));
        assertNotNull(HITLInteractionType.valueOf("REVIEW_TOOL_CALLS"));
        assertNotNull(HITLInteractionType.valueOf("MULTI_TURN_CONVERSATION"));
    }

    @Test
    @DisplayName("HITL模式枚举值")
    void testHITLModeEnum() {
        assertEquals(2, HITLMode.values().length);
        assertNotNull(HITLMode.valueOf("PRE_EXECUTION"));
        assertNotNull(HITLMode.valueOf("POST_EXECUTION"));
    }

    @Test
    @DisplayName("HITL上下文创建")
    void testHITLContextCreation() {
        Map<String, Object> inputs = Map.of("key1", "value1", "key2", 123);
        
        HITLContext context = HITLContext.builder()
                .taskName("test_task")
                .agentName("test_agent")
                .actionName("test_action")
                .workflowGoal("test_goal")
                .actionInputs(inputs)
                .executionResult("test_result")
                .build();

        assertNotNull(context);
        assertEquals("test_task", context.getTaskName());
        assertEquals("test_agent", context.getAgentName());
        assertEquals("test_action", context.getActionName());
        assertEquals("test_goal", context.getWorkflowGoal());
        assertEquals(inputs, context.getActionInputs());
        assertEquals("test_result", context.getExecutionResult());
        assertNotNull(context.getDisplayContext());
    }

    @Test
    @DisplayName("HITL请求创建")
    void testHITLRequestCreation() {
        HITLContext context = HITLContext.builder()
                .taskName("test")
                .agentName("agent")
                .actionName("action")
                .build();

        HITLRequest request = HITLRequest.builder()
                .interactionType(HITLInteractionType.APPROVE_REJECT)
                .mode(HITLMode.PRE_EXECUTION)
                .context(context)
                .promptMessage("Test prompt")
                .build();

        assertNotNull(request);
        assertNotNull(request.getRequestId());
        assertEquals(HITLInteractionType.APPROVE_REJECT, request.getInteractionType());
        assertEquals(HITLMode.PRE_EXECUTION, request.getMode());
        assertEquals(context, request.getContext());
        assertEquals("Test prompt", request.getPromptMessage());
    }

    @Test
    @DisplayName("HITL响应创建")
    void testHITLResponseCreation() {
        HITLResponse response = HITLResponse.builder()
                .requestId("test-request-id")
                .decision(HITLDecision.APPROVE)
                .feedback("Test feedback")
                .build();

        assertNotNull(response);
        assertEquals("test-request-id", response.getRequestId());
        assertEquals(HITLDecision.APPROVE, response.getDecision());
        assertEquals("Test feedback", response.getFeedback());
        assertNull(response.getModifiedContent());
    }

    @Test
    @DisplayName("HITL响应带修改内容")
    void testHITLResponseWithModifiedContent() {
        Map<String, Object> modifiedContent = Map.of("modified", "data");
        
        HITLResponse response = HITLResponse.builder()
                .requestId("test-id")
                .decision(HITLDecision.MODIFY)
                .modifiedContent(modifiedContent)
                .feedback("Modified by user")
                .build();

        assertNotNull(response);
        assertEquals(HITLDecision.MODIFY, response.getDecision());
        assertEquals(modifiedContent, response.getModifiedContent());
    }

    @Test
    @DisplayName("HITL拦截器智能体创建")
    void testHITLInterceptorAgentCreation() {
        HITLInterceptorAgent agent = HITLInterceptorAgent.builder()
                .name("test_interceptor")
                .targetAgentName("target_agent")
                .targetActionName("target_action")
                .interactionType(HITLInteractionType.APPROVE_REJECT)
                .mode(HITLMode.PRE_EXECUTION)
                .hitlManager(hitlManager)
                .build();

        assertNotNull(agent);
        assertEquals("test_interceptor", agent.getName());
        assertEquals("target_agent", agent.getTargetAgentName());
        assertEquals("target_action", agent.getTargetActionName());
        assertEquals(HITLInteractionType.APPROVE_REJECT, agent.getInteractionType());
        assertEquals(HITLMode.PRE_EXECUTION, agent.getMode());
        assertNotNull(agent.getDescription());
    }

    @Test
    @DisplayName("HITL拦截器无管理器时自动批准")
    void testInterceptorWithoutManager() {
        HITLInterceptorAgent agent = HITLInterceptorAgent.builder()
                .name("test_interceptor")
                .targetAgentName("target_agent")
                .targetActionName("target_action")
                .interactionType(HITLInteractionType.APPROVE_REJECT)
                .mode(HITLMode.PRE_EXECUTION)
                .build();

        Mono<HITLResponse> responseMono = agent.intercept(
                "test_task",
                new HashMap<>(),
                null,
                "test_goal"
        );

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HITLDecision.APPROVE, response.getDecision());
                    assertTrue(response.getFeedback().contains("No"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("HITL用户输入收集器智能体创建")
    void testUserInputCollectorCreation() {
        Map<String, HITLUserInputCollectorAgent.FieldDefinition> fields = new HashMap<>();
        HITLUserInputCollectorAgent.FieldDefinition field = new HITLUserInputCollectorAgent.FieldDefinition();
        field.setName("user_name");
        field.setType("string");
        field.setDescription("Enter your name");
        field.setRequired(true);
        fields.put("user_name", field);

        HITLUserInputCollectorAgent agent = HITLUserInputCollectorAgent.builder()
                .name("input_collector")
                .fieldDefinitions(fields)
                .hitlManager(hitlManager)
                .build();

        assertNotNull(agent);
        assertEquals("input_collector", agent.getName());
        assertEquals(1, agent.getFieldDefinitions().size());
        assertTrue(agent.getFieldDefinitions().containsKey("user_name"));
    }

    @Test
    @DisplayName("HITL管理器输入/输出映射")
    void testHITLManagerMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("human_verified_data", "extracted_data");
        mapping.put("user_input", "workflow_data");

        hitlManager.setHitlInputOutputMapping(mapping);

        assertEquals(2, hitlManager.getHitlInputOutputMapping().size());
        assertEquals("extracted_data", hitlManager.getHitlInputOutputMapping().get("human_verified_data"));
    }

    @Test
    @DisplayName("HITL管理器默认超时")
    void testHITLManagerDefaultTimeout() {
        assertEquals(1800, hitlManager.getDefaultTimeout());

        hitlManager.setDefaultTimeout(600);
        assertEquals(600, hitlManager.getDefaultTimeout());
    }

    @Test
    @DisplayName("字段定义创建")
    void testFieldDefinitionCreation() {
        HITLUserInputCollectorAgent.FieldDefinition field = new HITLUserInputCollectorAgent.FieldDefinition();
        field.setName("email");
        field.setType("string");
        field.setDescription("User email address");
        field.setRequired(true);
        field.setDefaultValue("user@example.com");

        assertEquals("email", field.getName());
        assertEquals("string", field.getType());
        assertEquals("User email address", field.getDescription());
        assertTrue(field.isRequired());
        assertEquals("user@example.com", field.getDefaultValue());
    }

    @Test
    @DisplayName("HITL拦截器动作包装器")
    void testInterceptorActionWrapper() {
        HITLInterceptorAgent agent = HITLInterceptorAgent.builder()
                .name("test_interceptor")
                .targetAgentName("target_agent")
                .targetActionName("target_action")
                .interactionType(HITLInteractionType.APPROVE_REJECT)
                .mode(HITLMode.PRE_EXECUTION)
                .build();

        HITLInterceptorAgent.HITLInterceptorAction action = 
                new HITLInterceptorAgent.HITLInterceptorAction(agent);

        assertNotNull(action);
        assertEquals("HITLInterceptorAction", action.getName());
        assertNotNull(action.getDescription());
    }
}
