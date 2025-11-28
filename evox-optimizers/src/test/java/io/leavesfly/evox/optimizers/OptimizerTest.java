package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.models.BaseLLM;
import io.leavesfly.evox.models.OpenAILLM;
import io.leavesfly.evox.models.OpenAILLMConfig;
import io.leavesfly.evox.workflow.Workflow;
import io.leavesfly.evox.workflow.WorkflowGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Optimizer implementations.
 */
class OptimizerTest {

    private Workflow testWorkflow;
    private BaseLLM testLLM;
    private Object testDataset;

    @BeforeEach
    void setUp() {
        // Create test workflow
        WorkflowGraph graph = new WorkflowGraph();
        testWorkflow = Workflow.builder()
                .name("test-workflow")
                .graph(graph)
                .build();

        // Create test LLM
        OpenAILLMConfig config = OpenAILLMConfig.builder()
                .model("gpt-4o-mini")
                .apiKey("test-key")
                .build();
        testLLM = new OpenAILLM(config);

        // Create mock dataset
        testDataset = new Object();
    }

    @Test
    @DisplayName("TextGrad optimizer basic functionality")
    void testTextGradOptimizer() {
        // Create optimizer
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .executorLLM(testLLM)
                .optimizeMode("all")
                .batchSize(3)
                .maxSteps(5)
                .evalEveryNSteps(1)
                .evalRounds(1)
                .convergenceThreshold(3)
                .build();

        assertNotNull(optimizer);
        assertEquals("all", optimizer.getOptimizeMode());
        assertEquals(3, optimizer.getBatchSize());
        assertEquals(5, optimizer.getMaxSteps());

        // Test reset
        optimizer.reset();
        assertEquals(0, optimizer.getCurrentStep());
        assertEquals(Double.NEGATIVE_INFINITY, optimizer.getBestScore());
        assertEquals(0, optimizer.getStepsWithoutImprovement());
    }

    @Test
    @DisplayName("TextGrad optimizer optimization flow")
    void testTextGradOptimization() {
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .executorLLM(testLLM)
                .optimizeMode("system_prompt")
                .batchSize(2)
                .maxSteps(3)
                .evalEveryNSteps(1)
                .convergenceThreshold(2)
                .build();

        // Run optimization
        Map<String, Object> kwargs = new HashMap<>();
        Optimizer.OptimizationResult result = optimizer.optimize(testDataset, kwargs);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getTotalSteps() > 0);
        assertNotNull(result.getMessage());
        assertNotNull(result.getMetadata());
    }

    @Test
    @DisplayName("TextGrad optimizer history tracking")
    void testTextGradHistory() {
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .executorLLM(testLLM)
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
    @DisplayName("MIPRO optimizer basic functionality")
    void testMIPROOptimizer() {
        MIPROOptimizer optimizer = MIPROOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .autoMode("medium")
                .maxBootstrappedDemos(4)
                .maxLabeledDemos(4)
                .maxSteps(10)
                .evalEveryNSteps(2)
                .convergenceThreshold(3)
                .build();

        assertNotNull(optimizer);
        assertEquals("medium", optimizer.getAutoMode());
        assertEquals(4, optimizer.getMaxBootstrappedDemos());
        assertEquals(4, optimizer.getMaxLabeledDemos());
    }

    @Test
    @DisplayName("MIPRO optimizer optimization flow")
    void testMIPROOptimization() {
        MIPROOptimizer optimizer = MIPROOptimizer.builder()
                .workflow(testWorkflow)
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
    @DisplayName("MIPRO optimizer configuration management")
    void testMIPROConfiguration() {
        MIPROOptimizer optimizer = MIPROOptimizer.builder()
                .workflow(testWorkflow)
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
        
        // Restore best program
        assertDoesNotThrow(() -> optimizer.restoreBestProgram());
    }

    @Test
    @DisplayName("AFlow optimizer basic functionality")
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
        assertEquals(10, optimizer.getMaxIterations());
        assertEquals(5, optimizer.getPopulationSize());
        assertEquals(3, optimizer.getConvergenceWindow());
    }

    @Test
    @DisplayName("AFlow optimizer optimization flow")
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
    @DisplayName("AFlow optimizer experience tracking")
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

    @Test
    @DisplayName("Optimizer convergence check")
    void testConvergenceCheck() {
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .executorLLM(testLLM)
                .optimizeMode("all")
                .batchSize(1)
                .maxSteps(10)
                .convergenceThreshold(3)
                .build();

        optimizer.reset();

        // No improvement scenarios
        assertFalse(optimizer.checkConvergence(0.5));
        assertTrue(optimizer.checkConvergence(0.4));
        assertTrue(optimizer.checkConvergence(0.3));
        assertTrue(optimizer.checkConvergence(0.3));

        // Improvement scenario
        optimizer.reset();
        assertFalse(optimizer.checkConvergence(0.6));
        assertFalse(optimizer.checkConvergence(0.7));
        assertFalse(optimizer.checkConvergence(0.8));
    }

    @Test
    @DisplayName("Optimizer JSON serialization")
    void testOptimizerJson() {
        TextGradOptimizer optimizer = TextGradOptimizer.builder()
                .workflow(testWorkflow)
                .optimizerLLM(testLLM)
                .executorLLM(testLLM)
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
        assertTrue(json.contains("maxSteps"));
        assertTrue(json.contains("bestScore"));
    }

    @Test
    @DisplayName("Evaluation metrics calculation")
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
        assertEquals(0.82, metrics.getScore()); // Should return F1 score
        assertNotNull(metrics.getAdditionalMetrics());
        assertEquals(2, metrics.getAdditionalMetrics().size());
    }
}
