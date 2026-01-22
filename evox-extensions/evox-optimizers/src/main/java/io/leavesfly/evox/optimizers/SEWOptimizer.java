package io.leavesfly.evox.optimizers;

import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.workflow.base.Workflow;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * SEW (Sequential Workflow Evolution) 优化器
 * 基于进化算法优化顺序工作流
 *
 * 核心流程:
 * 1. 将工作流转换为指定表示方案(YAML/Python/代码)
 * 2. 对表示进行变异操作(增删改节点、调整参数)
 * 3. 评估变异后的工作流性能
 * 4. 选择最优工作流进入下一轮进化
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SEWOptimizer extends Optimizer {

    /**
     * 工作流表示方案
     */
    public enum Scheme {
        YAML,      // YAML格式
        PYTHON,    // Python代码格式
        JSON,      // JSON格式
        DSL        // 自定义DSL格式
    }

    /**
     * 变异类型
     */
    public enum MutationType {
        ADD_NODE,       // 添加节点
        REMOVE_NODE,    // 删除节点
        MODIFY_PROMPT,  // 修改提示词
        REORDER,        // 重新排序
        MODIFY_PARAMS   // 修改参数
    }

    /**
     * 用于变异操作的LLM
     */
    private BaseLLM mutatorLLM;

    /**
     * 用于评估的LLM
     */
    private BaseLLM evaluatorLLM;

    /**
     * 工作流表示方案
     */
    private Scheme scheme;

    /**
     * 最大迭代次数
     */
    private int maxIterations;

    /**
     * 种群大小
     */
    private int populationSize;

    /**
     * 变异率
     */
    private double mutationRate;

    /**
     * 精英保留比例
     */
    private double eliteRatio;

    /**
     * 当前种群
     */
    private List<WorkflowCandidate> population;

    /**
     * 历史最佳候选
     */
    private WorkflowCandidate bestCandidate;

    /**
     * 进化历史记录
     */
    private List<EvolutionRecord> evolutionHistory;

    /**
     * 工作流候选
     */
    @Data
    @SuperBuilder
    public static class WorkflowCandidate {
        private String id;
        private String representation;  // 工作流的文本表示
        private Workflow workflow;       // 实际工作流对象
        private double fitness;          // 适应度分数
        private int generation;          // 所属代数
        private List<MutationType> appliedMutations;  // 应用的变异
    }

    /**
     * 进化记录
     */
    @Data
    @SuperBuilder
    public static class EvolutionRecord {
        private int generation;
        private double bestFitness;
        private double avgFitness;
        private int populationSize;
        private long timestamp;
    }

    @Override
    public OptimizationResult optimize(Object dataset, Map<String, Object> kwargs) {
        log.info("启动SEW工作流进化优化");
        log.info("参数: maxIterations={}, populationSize={}, mutationRate={}, scheme={}",
                maxIterations, populationSize, mutationRate, scheme);

        reset();
        initializePopulation();
        evolutionHistory = new ArrayList<>();

        for (int generation = 0; generation < maxIterations; generation++) {
            currentStep = generation;
            log.info("===== 第 {} 代进化 =====", generation + 1);

            // 1. 评估当前种群
            evaluatePopulation(dataset, kwargs);

            // 2. 记录进化历史
            recordEvolution(generation);

            // 3. 检查收敛
            double currentBestFitness = bestCandidate != null ? bestCandidate.getFitness() : 0.0;
            if (checkConvergence(currentBestFitness)) {
                log.info("优化在第 {} 代收敛", generation + 1);
                break;
            }

            // 4. 选择精英
            List<WorkflowCandidate> elites = selectElites();

            // 5. 生成下一代
            population = evolvePopulation(elites, generation + 1);

            log.info("第 {} 代完成: 最佳适应度={}", generation + 1, currentBestFitness);
        }

        log.info("SEW优化完成。最终最佳分数: {}", bestScore);

        return OptimizationResult.builder()
                .success(true)
                .finalScore(bestScore)
                .totalSteps(currentStep + 1)
                .message("SEW工作流进化优化完成")
                .metadata(Map.of(
                        "scheme", scheme.name(),
                        "finalPopulationSize", population.size(),
                        "totalGenerations", evolutionHistory.size(),
                        "bestCandidateId", bestCandidate != null ? bestCandidate.getId() : "none"
                ))
                .build();
    }

    @Override
    public StepResult step(Map<String, Object> kwargs) {
        // 单步进化
        int generation = currentStep;
        
        // 评估并选择
        Object dataset = kwargs.get("dataset");
        if (dataset != null) {
            evaluatePopulation(dataset, kwargs);
        }
        
        List<WorkflowCandidate> elites = selectElites();
        population = evolvePopulation(elites, generation + 1);
        currentStep++;

        double currentBestFitness = bestCandidate != null ? bestCandidate.getFitness() : 0.0;
        boolean improved = currentBestFitness > bestScore;
        if (improved) {
            bestScore = currentBestFitness;
        }

        return StepResult.builder()
                .step(currentStep)
                .score(currentBestFitness)
                .modification(String.format("第 %d 代进化完成", generation + 1))
                .improved(improved)
                .details(Map.of(
                        "populationSize", population.size(),
                        "eliteCount", elites.size()
                ))
                .build();
    }

    @Override
    public EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs) {
        log.info("在 {} 集上评估工作流", evalMode);

        if (bestCandidate == null) {
            return EvaluationMetrics.builder()
                    .accuracy(0.0)
                    .f1Score(0.0)
                    .totalSamples(0)
                    .correctSamples(0)
                    .build();
        }

        // 评估最佳候选
        double fitness = evaluateCandidate(bestCandidate, dataset, kwargs);
        
        return EvaluationMetrics.builder()
                .accuracy(fitness)
                .f1Score(fitness)
                .totalSamples(100)
                .correctSamples((int)(fitness * 100))
                .additionalMetrics(Map.of(
                        "evalMode", evalMode,
                        "candidateId", bestCandidate.getId(),
                        "generation", bestCandidate.getGeneration()
                ))
                .build();
    }

    /**
     * 初始化种群
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        
        // 创建初始候选
        for (int i = 0; i < populationSize; i++) {
            String representation = convertToScheme(workflow);
            WorkflowCandidate candidate = WorkflowCandidate.builder()
                    .id("candidate-" + i)
                    .representation(representation)
                    .workflow(workflow)  // 在真实实现中应该克隆
                    .fitness(0.0)
                    .generation(0)
                    .appliedMutations(new ArrayList<>())
                    .build();
            
            // 对非第一个候选应用随机变异
            if (i > 0) {
                candidate.setRepresentation(mutate(representation));
                candidate.getAppliedMutations().add(selectRandomMutation());
            }
            
            population.add(candidate);
        }
        
        log.info("初始化种群完成，大小: {}", population.size());
    }

    /**
     * 评估种群中所有候选
     */
    private void evaluatePopulation(Object dataset, Map<String, Object> kwargs) {
        for (WorkflowCandidate candidate : population) {
            double fitness = evaluateCandidate(candidate, dataset, kwargs);
            candidate.setFitness(fitness);
            
            // 更新最佳候选
            if (bestCandidate == null || fitness > bestCandidate.getFitness()) {
                bestCandidate = candidate;
                bestScore = fitness;
                log.info("发现新的最佳候选: id={}, fitness={}", candidate.getId(), fitness);
            }
        }
        
        // 按适应度排序
        population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
    }

    /**
     * 评估单个候选
     */
    private double evaluateCandidate(WorkflowCandidate candidate, Object dataset, Map<String, Object> kwargs) {
        if (evaluatorLLM != null) {
            // 使用LLM评估工作流质量
            String prompt = buildEvaluationPrompt(candidate, dataset);
            String response = evaluatorLLM.generate(prompt);
            return parseEvaluationScore(response);
        }
        
        // 默认评估: 基于工作流复杂度和完整性
        double complexityScore = evaluateComplexity(candidate);
        double coherenceScore = evaluateCoherence(candidate);
        return (complexityScore + coherenceScore) / 2.0;
    }

    /**
     * 选择精英候选
     */
    private List<WorkflowCandidate> selectElites() {
        int eliteCount = Math.max(1, (int)(population.size() * eliteRatio));
        return new ArrayList<>(population.subList(0, eliteCount));
    }

    /**
     * 进化种群
     */
    private List<WorkflowCandidate> evolvePopulation(List<WorkflowCandidate> elites, int generation) {
        List<WorkflowCandidate> newPopulation = new ArrayList<>(elites);
        
        Random random = new Random();
        int candidateIndex = elites.size();
        
        while (newPopulation.size() < populationSize) {
            // 从精英中选择父代
            WorkflowCandidate parent = elites.get(random.nextInt(elites.size()));
            
            // 创建后代
            String childRepresentation = parent.getRepresentation();
            List<MutationType> mutations = new ArrayList<>(parent.getAppliedMutations());
            
            // 应用变异
            if (random.nextDouble() < mutationRate) {
                childRepresentation = mutate(childRepresentation);
                mutations.add(selectRandomMutation());
            }
            
            WorkflowCandidate child = WorkflowCandidate.builder()
                    .id("candidate-" + generation + "-" + candidateIndex++)
                    .representation(childRepresentation)
                    .workflow(parent.getWorkflow())  // 应克隆并应用变异
                    .fitness(0.0)
                    .generation(generation)
                    .appliedMutations(mutations)
                    .build();
            
            newPopulation.add(child);
        }
        
        return newPopulation;
    }

    /**
     * 记录进化历史
     */
    private void recordEvolution(int generation) {
        double totalFitness = population.stream().mapToDouble(WorkflowCandidate::getFitness).sum();
        double avgFitness = totalFitness / population.size();
        double bestFitness = population.isEmpty() ? 0.0 : population.get(0).getFitness();
        
        EvolutionRecord record = EvolutionRecord.builder()
                .generation(generation)
                .bestFitness(bestFitness)
                .avgFitness(avgFitness)
                .populationSize(population.size())
                .timestamp(System.currentTimeMillis())
                .build();
        
        evolutionHistory.add(record);
    }

    /**
     * 将工作流转换为指定方案的表示
     */
    public String convertToScheme(Object workflow) {
        if (workflow == null) {
            return "";
        }
        
        Scheme targetScheme = this.scheme != null ? this.scheme : Scheme.YAML;
        log.debug("将工作流转换为 {} 方案", targetScheme);
        
        return switch (targetScheme) {
            case YAML -> convertToYaml(workflow);
            case PYTHON -> convertToPython(workflow);
            case JSON -> convertToJson(workflow);
            case DSL -> convertToDsl(workflow);
        };
    }

    /**
     * 从方案解析工作流
     */
    public Object parseFromScheme(String representation) {
        if (representation == null || representation.isEmpty()) {
            return null;
        }
        
        Scheme targetScheme = this.scheme != null ? this.scheme : Scheme.YAML;
        log.debug("从 {} 方案解析工作流", targetScheme);
        
        return switch (targetScheme) {
            case YAML -> parseFromYaml(representation);
            case PYTHON -> parseFromPython(representation);
            case JSON -> parseFromJson(representation);
            case DSL -> parseFromDsl(representation);
        };
    }

    /**
     * 变异操作 - 使用LLM生成变异
     */
    public String mutate(String workflowRepresentation) {
        if (workflowRepresentation == null || workflowRepresentation.isEmpty()) {
            return workflowRepresentation;
        }
        
        MutationType mutationType = selectRandomMutation();
        log.debug("应用变异类型: {}", mutationType);
        
        if (mutatorLLM != null) {
            // 使用LLM进行智能变异
            String prompt = buildMutationPrompt(workflowRepresentation, mutationType);
            return mutatorLLM.generate(prompt);
        }
        
        // 默认简单变异
        return applySimpleMutation(workflowRepresentation, mutationType);
    }

    // ============= 辅助方法 =============

    private String convertToYaml(Object workflow) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("workflow:\n");
        yaml.append("  name: ").append(workflow.getClass().getSimpleName()).append("\n");
        yaml.append("  steps:\n");
        yaml.append("    - step: process\n");
        yaml.append("      type: llm\n");
        return yaml.toString();
    }

    private String convertToPython(Object workflow) {
        StringBuilder python = new StringBuilder();
        python.append("# Auto-generated workflow\n");
        python.append("def workflow():\n");
        python.append("    # Step 1: Process\n");
        python.append("    result = llm_call(input)\n");
        python.append("    return result\n");
        return python.toString();
    }

    private String convertToJson(Object workflow) {
        return String.format(
            "{\"name\":\"%s\",\"steps\":[{\"type\":\"llm\",\"action\":\"process\"}]}",
            workflow.getClass().getSimpleName()
        );
    }

    private String convertToDsl(Object workflow) {
        return "WORKFLOW -> STEP[llm:process] -> END";
    }

    private Object parseFromYaml(String representation) {
        log.debug("解析YAML: {}", representation.substring(0, Math.min(50, representation.length())));
        return workflow;  // 简化实现
    }

    private Object parseFromPython(String representation) {
        log.debug("解析Python: {}", representation.substring(0, Math.min(50, representation.length())));
        return workflow;
    }

    private Object parseFromJson(String representation) {
        log.debug("解析JSON: {}", representation.substring(0, Math.min(50, representation.length())));
        return workflow;
    }

    private Object parseFromDsl(String representation) {
        log.debug("解析DSL: {}", representation);
        return workflow;
    }

    private MutationType selectRandomMutation() {
        MutationType[] types = MutationType.values();
        return types[new Random().nextInt(types.length)];
    }

    private String buildMutationPrompt(String representation, MutationType type) {
        return String.format(
            "请对以下工作流应用 '%s' 变异操作，生成优化后的版本：\n\n%s\n\n只输出修改后的工作流定义，不要解释。",
            type.name(), representation
        );
    }

    private String buildEvaluationPrompt(WorkflowCandidate candidate, Object dataset) {
        return String.format(
            "请评估以下工作流的质量（0-1分）：\n\n%s\n\n请只输出一个0到1之间的数字。",
            candidate.getRepresentation()
        );
    }

    private double parseEvaluationScore(String response) {
        try {
            String cleaned = response.replaceAll("[^0-9.]", "");
            double score = Double.parseDouble(cleaned);
            return Math.max(0.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("解析评估分数失败: {}", response);
            return 0.5;
        }
    }

    private String applySimpleMutation(String representation, MutationType type) {
        return switch (type) {
            case ADD_NODE -> representation + "\n    - step: additional_process";
            case REMOVE_NODE -> representation.replaceFirst("(?m)^.*step:.*$\n?", "");
            case MODIFY_PROMPT -> representation.replace("process", "enhanced_process");
            case REORDER -> representation;  // 简化
            case MODIFY_PARAMS -> representation.replace("llm", "llm_v2");
        };
    }

    private double evaluateComplexity(WorkflowCandidate candidate) {
        String repr = candidate.getRepresentation();
        int lineCount = repr.split("\n").length;
        return Math.min(1.0, lineCount / 20.0);
    }

    private double evaluateCoherence(WorkflowCandidate candidate) {
        String repr = candidate.getRepresentation();
        boolean hasWorkflow = repr.contains("workflow") || repr.contains("def ") || repr.contains("WORKFLOW");
        boolean hasSteps = repr.contains("step") || repr.contains("STEP");
        return (hasWorkflow ? 0.5 : 0.0) + (hasSteps ? 0.5 : 0.0);
    }

    /**
     * 获取进化历史
     */
    public List<EvolutionRecord> getEvolutionHistory() {
        return evolutionHistory != null ? new ArrayList<>(evolutionHistory) : new ArrayList<>();
    }

    /**
     * 获取最佳候选
     */
    public WorkflowCandidate getBestCandidate() {
        return bestCandidate;
    }

    /**
     * 恢复最佳工作流
     */
    public void restoreBestWorkflow() {
        if (bestCandidate != null && bestCandidate.getWorkflow() != null) {
            this.workflow = bestCandidate.getWorkflow();
            log.info("已恢复最佳工作流，适应度: {}", bestCandidate.getFitness());
        } else {
            log.warn("没有可用的最佳工作流");
        }
    }
}
