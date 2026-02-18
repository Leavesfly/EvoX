package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.models.spi.LLMProvider;
import io.leavesfly.evox.models.provider.openai.OpenAILLM;
import io.leavesfly.evox.models.provider.openai.OpenAILLMConfig;
import io.leavesfly.evox.optimizers.base.EvaluationFeedback;
import io.leavesfly.evox.optimizers.base.OptimizationContext;
import io.leavesfly.evox.optimizers.base.OptimizationType;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 优化器实现的单元测试。
 * 按照 Evolving Layer 三层架构组织：Agent / Workflow / Memory 优化器。
 */
class OptimizerTest {

    private Workflow testWorkflow;
    private LLMProvider testLLM;
    private Object testDataset;

    @BeforeEach
    void setUp() {
        WorkflowGraph graph = new WorkflowGraph();
        testWorkflow = new Workflow();
        testWorkflow.setName("test-workflow");
        testWorkflow.setGraph(graph);

        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .model("gpt-4o-mini")
                .apiKey("test-key")
                .build();
        testLLM = new OpenAILLM(config);

        testDataset = new Object();
    }

    // ========== Agent Optimizer Tests ==========

    @Nested
    @DisplayName("Agent 级优化器测试")
    class AgentOptimizerTests {

        @Test
        @DisplayName("TextGrad优化器基本功能")
        void testTextGradOptimizer() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Analyze this text")
                    .optimizeMode("all")
                    .batchSize(3)
                    .maxSteps(5)
                    .evalEveryNSteps(1)
                    .evalRounds(1)
                    .convergenceThreshold(3)
                    .build();

            assertNotNull(optimizer);
            assertEquals(OptimizationType.AGENT, optimizer.getOptimizationType());
            assertEquals("all", optimizer.getOptimizeMode());
            assertEquals(3, optimizer.getBatchSize());
            assertEquals(5, optimizer.getMaxSteps());

            optimizer.reset();
            assertEquals(0, optimizer.getCurrentStep());
            assertEquals(Double.NEGATIVE_INFINITY, optimizer.getBestScore());
            assertEquals(0, optimizer.getStepsWithoutImprovement());
        }

        @Test
        @DisplayName("TextGrad优化器优化流程")
        void testTextGradOptimization() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Analyze this text")
                    .optimizeMode("system_prompt")
                    .batchSize(2)
                    .maxSteps(3)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(2)
                    .build();

            Map<String, Object> kwargs = new HashMap<>();
            Optimizer.OptimizationResult result = optimizer.optimize(testDataset, kwargs);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertTrue(result.getTotalSteps() > 0);
            assertNotNull(result.getMessage());
            assertNotNull(result.getMetadata());
        }

        @Test
        @DisplayName("TextGrad优化器历史跟踪")
        void testTextGradHistory() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Analyze this text")
                    .optimizeMode("instruction")
                    .batchSize(1)
                    .maxSteps(2)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(5)
                    .build();

            optimizer.optimize(testDataset, new HashMap<>());

            List<Optimizer.StepResult> history = optimizer.getHistory();
            assertNotNull(history);
            assertEquals(2, history.size());

            for (Optimizer.StepResult step : history) {
                assertNotNull(step.getModification());
                assertNotNull(step.getDetails());
            }
        }

        @Test
        @DisplayName("TextGrad optimizePrompt 方法")
        void testTextGradOptimizePrompt() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Initial prompt")
                    .optimizeMode("all")
                    .batchSize(1)
                    .maxSteps(5)
                    .convergenceThreshold(3)
                    .build();

            EvaluationFeedback feedback = EvaluationFeedback.builder()
                    .primaryScore(0.8)
                    .textualGradient("Be more specific")
                    .build();

            String optimized = optimizer.optimizePrompt("Initial prompt", Map.of(), feedback);
            assertNotNull(optimized);
            assertTrue(optimized.contains("Be more specific"));
        }

        @Test
        @DisplayName("MIPRO优化器基本功能")
        void testMIPROOptimizer() {
            MIPROOptimizer optimizer = MIPROOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .autoMode("medium")
                    .maxBootstrappedDemos(4)
                    .maxLabeledDemos(4)
                    .maxSteps(10)
                    .evalEveryNSteps(2)
                    .convergenceThreshold(3)
                    .build();

            assertNotNull(optimizer);
            assertEquals(OptimizationType.AGENT, optimizer.getOptimizationType());
            assertEquals("medium", optimizer.getAutoMode());
            assertEquals(4, optimizer.getMaxBootstrappedDemos());
            assertEquals(4, optimizer.getMaxLabeledDemos());
        }

        @Test
        @DisplayName("MIPRO优化器优化流程")
        void testMIPROOptimization() {
            MIPROOptimizer optimizer = MIPROOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .autoMode("light")
                    .maxBootstrappedDemos(2)
                    .maxLabeledDemos(2)
                    .maxSteps(3)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(2)
                    .build();

            Map<String, Object> kwargs = new HashMap<>();
            Optimizer.OptimizationResult result = optimizer.optimize(testDataset, kwargs);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNotNull(result.getMetadata());
            assertTrue(result.getMetadata().containsKey("autoMode"));
            assertTrue(result.getMetadata().containsKey("instructionCandidates"));
        }

        @Test
        @DisplayName("MIPRO优化器配置管理")
        void testMIPROConfiguration() {
            MIPROOptimizer optimizer = MIPROOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .autoMode("heavy")
                    .maxBootstrappedDemos(3)
                    .maxLabeledDemos(3)
                    .maxSteps(2)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(5)
                    .build();

            optimizer.optimize(testDataset, new HashMap<>());

            Map<String, Object> config = optimizer.getBestConfiguration();
            assertNotNull(config);

            assertDoesNotThrow(() -> optimizer.restoreBestProgram());
        }
    }

    // ========== Workflow Optimizer Tests ==========

    @Nested
    @DisplayName("Workflow 级优化器测试")
    class WorkflowOptimizerTests {

        @Test
        @DisplayName("AFlow优化器基本功能")
        void testAFlowOptimizer() {
            AFlowOptimizer optimizer = AFlowOptimizer.builder()
                    .workflow(testWorkflow)
                    .optimizerLLM(testLLM)
                    .maxIterations(10)
                    .populationSize(5)
                    .convergenceWindow(3)
                    .maxSteps(10)
                    .evalEveryNSteps(2)
                    .convergenceThreshold(3)
                    .build();

            assertNotNull(optimizer);
            assertEquals(OptimizationType.WORKFLOW, optimizer.getOptimizationType());
            assertEquals(10, optimizer.getMaxIterations());
            assertEquals(5, optimizer.getPopulationSize());
            assertEquals(3, optimizer.getConvergenceWindow());
        }

        @Test
        @DisplayName("AFlow优化器优化流程")
        void testAFlowOptimization() {
            AFlowOptimizer optimizer = AFlowOptimizer.builder()
                    .workflow(testWorkflow)
                    .optimizerLLM(testLLM)
                    .maxIterations(5)
                    .populationSize(3)
                    .convergenceWindow(2)
                    .maxSteps(3)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(2)
                    .build();

            Map<String, Object> kwargs = new HashMap<>();
            Optimizer.OptimizationResult result = optimizer.optimize(testDataset, kwargs);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertNotNull(result.getMetadata());
            assertTrue(result.getMetadata().containsKey("populationSize"));
            assertTrue(result.getMetadata().containsKey("experienceBufferSize"));
        }

        @Test
        @DisplayName("AFlow优化器经验跟踪")
        void testAFlowExperience() {
            AFlowOptimizer optimizer = AFlowOptimizer.builder()
                    .workflow(testWorkflow)
                    .optimizerLLM(testLLM)
                    .maxIterations(5)
                    .populationSize(2)
                    .convergenceWindow(2)
                    .maxSteps(2)
                    .evalEveryNSteps(1)
                    .convergenceThreshold(5)
                    .build();

            optimizer.optimize(testDataset, new HashMap<>());

            List<AFlowOptimizer.ExperienceEntry> experience = optimizer.getExperienceBuffer();
            assertNotNull(experience);
            assertTrue(experience.size() > 0);

            for (AFlowOptimizer.ExperienceEntry entry : experience) {
                assertNotNull(entry.getModification());
                assertTrue(entry.getTimestamp() > 0);
            }
        }
    }

    // ========== 通用测试 ==========

    @Nested
    @DisplayName("通用优化器功能测试")
    class CommonOptimizerTests {

        @Test
        @DisplayName("优化器收敛检查")
        void testConvergenceCheck() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Test prompt")
                    .optimizeMode("all")
                    .batchSize(1)
                    .maxSteps(10)
                    .convergenceThreshold(3)
                    .build();

            optimizer.reset();

            assertFalse(optimizer.checkConvergence(0.5));
            assertFalse(optimizer.checkConvergence(0.4));
            assertFalse(optimizer.checkConvergence(0.3));
            assertTrue(optimizer.checkConvergence(0.3));

            optimizer.reset();
            assertFalse(optimizer.checkConvergence(0.6));
            assertFalse(optimizer.checkConvergence(0.7));
            assertFalse(optimizer.checkConvergence(0.8));
        }

        @Test
        @DisplayName("优化器JSON序列化")
        void testOptimizerJson() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Test prompt")
                    .optimizeMode("all")
                    .batchSize(3)
                    .maxSteps(10)
                    .convergenceThreshold(3)
                    .build();

            optimizer.reset();
            optimizer.checkConvergence(0.75);

            String json = optimizer.toJson();
            assertNotNull(json);
            assertTrue(json.contains("TextGradOptimizer"));
            assertTrue(json.contains("AGENT"));
            assertTrue(json.contains("maxSteps"));
            assertTrue(json.contains("bestScore"));
        }

        @Test
        @DisplayName("评估指标计算")
        void testEvaluationMetrics() {
            Optimizer.EvaluationMetrics metrics = Optimizer.EvaluationMetrics.builder()
                    .accuracy(0.85)
                    .f1Score(0.82)
                    .totalSamples(100)
                    .correctSamples(85)
                    .additionalMetrics(Map.of("precision", 0.88, "recall", 0.77))
                    .build();

            assertNotNull(metrics);
            assertEquals(0.85, metrics.getAccuracy());
            assertEquals(0.82, metrics.getF1Score());
            assertEquals(100, metrics.getTotalSamples());
            assertEquals(85, metrics.getCorrectSamples());
            assertEquals(0.82, metrics.getScore());
            assertNotNull(metrics.getAdditionalMetrics());
            assertEquals(2, metrics.getAdditionalMetrics().size());
        }

        @Test
        @DisplayName("EvaluationFeedback 构建和使用")
        void testEvaluationFeedback() {
            EvaluationFeedback feedback = EvaluationFeedback.builder()
                    .primaryScore(0.85)
                    .evalMode("validation")
                    .sampleCount(100)
                    .textualGradient("Improve specificity")
                    .build();

            assertNotNull(feedback);
            assertEquals(0.85, feedback.getPrimaryScore());
            assertEquals("validation", feedback.getEvalMode());
            assertEquals(100, feedback.getSampleCount());
            assertEquals("Improve specificity", feedback.getTextualGradient());
            assertTrue(feedback.isSuccess());

            feedback.putMetric("accuracy", 0.9);
            assertEquals(0.9, feedback.getMetric("accuracy"));
        }

        @Test
        @DisplayName("OptimizationContext 状态管理")
        void testOptimizationContext() {
            OptimizationContext context = OptimizationContext.builder()
                    .maxSteps(10)
                    .evalEveryNSteps(2)
                    .convergenceThreshold(3)
                    .build();

            assertNotNull(context);
            assertEquals(0, context.getCurrentStep());
            assertFalse(context.isMaxStepsReached());

            assertFalse(context.checkConvergence(0.5));
            assertFalse(context.checkConvergence(0.4));
            assertFalse(context.checkConvergence(0.3));
            assertTrue(context.checkConvergence(0.3));

            context.reset();
            assertEquals(0, context.getCurrentStep());
            assertEquals(Double.NEGATIVE_INFINITY, context.getBestScore());
        }

        @Test
        @DisplayName("OptimizationType 枚举")
        void testOptimizationType() {
            assertEquals(3, OptimizationType.values().length);
            assertNotNull(OptimizationType.AGENT);
            assertNotNull(OptimizationType.WORKFLOW);
            assertNotNull(OptimizationType.MEMORY);
        }

        @Test
        @DisplayName("evaluateWithFeedback 统一评估反馈")
        void testEvaluateWithFeedback() {
            TextGradOptimizer optimizer = TextGradOptimizer.builder()
                    .optimizerLLM(testLLM)
                    .executorLLM(testLLM)
                    .currentPrompt("Test prompt")
                    .optimizeMode("all")
                    .batchSize(1)
                    .maxSteps(5)
                    .convergenceThreshold(3)
                    .build();

            EvaluationFeedback feedback = optimizer.evaluateWithFeedback(testDataset, "validation", new HashMap<>());
            assertNotNull(feedback);
            assertTrue(feedback.isSuccess());
            assertTrue(feedback.getPrimaryScore() > 0);
            assertEquals("validation", feedback.getEvalMode());
        }

        @Test
        @DisplayName("createContext 创建优化上下文")
        void testCreateContext() {
            AFlowOptimizer optimizer = AFlowOptimizer.builder()
                    .workflow(testWorkflow)
                    .optimizerLLM(testLLM)
                    .maxIterations(5)
                    .populationSize(3)
                    .convergenceWindow(2)
                    .maxSteps(10)
                    .evalEveryNSteps(2)
                    .convergenceThreshold(3)
                    .build();

            OptimizationContext context = optimizer.createContext();
            assertNotNull(context);
            assertEquals(10, context.getMaxSteps());
            assertEquals(2, context.getEvalEveryNSteps());
            assertEquals(3, context.getConvergenceThreshold());
        }
    }
}
