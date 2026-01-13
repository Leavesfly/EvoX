package io.leavesfly.evox.optimizers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * EvoPrompt优化器
 * 使用进化算法优化提示词
 *
 * @author EvoX Team
 */
@Slf4j
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EvoPromptOptimizer extends Optimizer {

    /**
     * 种群大小
     */
    private int populationSize;

    /**
     * 迭代次数
     */
    private int iterations;

    /**
     * 并发限制
     */
    private int concurrencyLimit;

    /**
     * 种群(节点名->提示词列表)
     */
    private Map<String, String[]> nodePopulations;

    /**
     * 分数(节点名->分数列表)
     */
    private Map<String, double[]> nodeScores;

    /**
     * 变异率
     */
    private double mutationRate;

    /**
     * 交叉率
     */
    private double crossoverRate;

    /**
     * 精英保留数量
     */
    private int eliteSize;

    /**
     * 最佳个体
     */
    private Map<String, String> bestIndividuals;

    /**
     * 最佳分数
     */
    private Map<String, Double> bestScores;

    /**
     * 初始化种群
     */
    public void initializePopulation(String nodeName, String initialPrompt) {
        log.info("Initializing population for node: {}", nodeName);
        
        String[] population = new String[populationSize];
        population[0] = initialPrompt; // 第一个是原始提示词
        
        // 生成变异版本
        for (int i = 1; i < populationSize; i++) {
            population[i] = mutatePrompt(initialPrompt);
        }
        
        nodePopulations.put(nodeName, population);
        nodeScores.put(nodeName, new double[populationSize]);
        bestIndividuals.put(nodeName, initialPrompt);
        bestScores.put(nodeName, 0.0);
        
        log.info("Initialized population of size {} for node {}", populationSize, nodeName);
    }

    /**
     * 进化操作
     */
    public void evolve() {
        log.info("Starting evolution process for {} iterations", iterations);
        
        for (int gen = 0; gen < iterations; gen++) {
            log.info("Generation {}/{}", gen + 1, iterations);
            
            for (String nodeName : nodePopulations.keySet()) {
                evolveNode(nodeName);
            }
            
            // 记录当前最佳
            logBestScores(gen);
        }
        
        log.info("Evolution completed");
    }

    /**
     * 对单个节点进行进化
     */
    private void evolveNode(String nodeName) {
        String[] population = nodePopulations.get(nodeName);
        double[] scores = nodeScores.get(nodeName);
        
        // 1. 评估当前种群
        for (int i = 0; i < populationSize; i++) {
            scores[i] = evaluateCandidate(population[i]);
        }
        
        // 2. 选择精英
        List<Integer> eliteIndices = getEliteIndices(scores);
        
        // 3. 生成新种群
        String[] newPopulation = new String[populationSize];
        
        // 保留精英
        for (int i = 0; i < eliteSize && i < eliteIndices.size(); i++) {
            newPopulation[i] = population[eliteIndices.get(i)];
        }
        
        // 通过选择、交叉和变异生成其余个体
        for (int i = eliteSize; i < populationSize; i++) {
            if (Math.random() < crossoverRate) {
                // 交叉
                String parent1 = selectParent(population, scores);
                String parent2 = selectParent(population, scores);
                newPopulation[i] = crossover(parent1, parent2);
            } else {
                // 直接选择
                newPopulation[i] = selectParent(population, scores);
            }
            
            // 变异
            if (Math.random() < mutationRate) {
                newPopulation[i] = mutatePrompt(newPopulation[i]);
            }
        }
        
        // 4. 更新种群
        nodePopulations.put(nodeName, newPopulation);
        
        // 5. 更新最佳个体
        updateBest(nodeName, population, scores);
    }

    /**
     * 评估候选项
     */
    public double evaluateCandidate(String prompt) {
        // TODO: 实际实现需要调用LLM并在数据集上评估
        // 这里返回模拟分数
        return Math.random();
    }

    /**
     * 变异提示词
     */
    private String mutatePrompt(String prompt) {
        // 简单的变异策略
        String[] mutations = {
            prompt + " Be more detailed.",
            prompt + " Be more concise.",
            prompt + " Think step by step.",
            prompt.replace(".", ". Also, explain your reasoning."),
            "Let's approach this differently: " + prompt
        };
        return mutations[new Random().nextInt(mutations.length)];
    }

    /**
     * 交叉操作
     */
    private String crossover(String parent1, String parent2) {
        // 简单的交叉:组合两个提示词
        String[] parts1 = parent1.split("\\. ");
        String[] parts2 = parent2.split("\\. ");
        
        if (parts1.length > 1 && parts2.length > 1) {
            // 取第一个的前半部分和第二个的后半部分
            int split1 = parts1.length / 2;
            int split2 = parts2.length / 2;
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < split1; i++) {
                result.append(parts1[i]).append(". ");
            }
            for (int i = split2; i < parts2.length; i++) {
                result.append(parts2[i]);
                if (i < parts2.length - 1) result.append(". ");
            }
            return result.toString();
        }
        
        return Math.random() < 0.5 ? parent1 : parent2;
    }

    /**
     * 选择父代(轮盘赌选择)
     */
    private String selectParent(String[] population, double[] scores) {
        double totalScore = Arrays.stream(scores).sum();
        double random = Math.random() * totalScore;
        double cumulative = 0.0;
        
        for (int i = 0; i < population.length; i++) {
            cumulative += scores[i];
            if (cumulative >= random) {
                return population[i];
            }
        }
        
        return population[population.length - 1];
    }

    /**
     * 获取精英个体索引
     */
    private List<Integer> getEliteIndices(double[] scores) {
        return IntStream.range(0, scores.length)
                .boxed()
                .sorted((i, j) -> Double.compare(scores[j], scores[i]))
                .limit(eliteSize)
                .collect(Collectors.toList());
    }

    /**
     * 更新最佳个体
     */
    private void updateBest(String nodeName, String[] population, double[] scores) {
        double maxScore = Arrays.stream(scores).max().orElse(0.0);
        int maxIndex = IntStream.range(0, scores.length)
                .reduce((i, j) -> scores[i] > scores[j] ? i : j)
                .orElse(0);
        
        if (maxScore > bestScores.getOrDefault(nodeName, 0.0)) {
            bestScores.put(nodeName, maxScore);
            bestIndividuals.put(nodeName, population[maxIndex]);
        }
    }

    /**
     * 记录最佳分数
     */
    private void logBestScores(int generation) {
        log.info("Generation {} best scores:", generation);
        for (Map.Entry<String, Double> entry : bestScores.entrySet()) {
            log.info("  {}: {}", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public StepResult step(Map<String, Object> kwargs) {
        // 执行一个优化步骤
        return StepResult.builder()
                .step(currentStep++)
                .score(0.0)
                .improved(false)
                .build();
    }

    @Override
    public EvaluationMetrics evaluate(Object dataset, String evalMode, Map<String, Object> kwargs) {
        // 评估预测结果
        return EvaluationMetrics.builder()
                .accuracy(0.0)
                .f1Score(0.0)
                .totalSamples(0)
                .correctSamples(0)
                .build();
    }

    @Override
    public OptimizationResult optimize(Object dataset, Map<String, Object> kwargs) {
        log.info("Starting EvoPrompt optimization");
        
        // 假设dataset是一个Map<节点名, 初始提示词>
        if (dataset instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> nodePrompts = (Map<String, String>) dataset;
            
            // 初始化所有节点的种群
            for (Map.Entry<String, String> entry : nodePrompts.entrySet()) {
                initializePopulation(entry.getKey(), entry.getValue());
            }
            
            // 执行进化
            evolve();
            
            // 返回最佳个体
            log.info("Optimization completed. Best individuals:");
            for (Map.Entry<String, String> entry : bestIndividuals.entrySet()) {
                log.info("{}: {} (score: {})", 
                        entry.getKey(), 
                        entry.getValue().substring(0, Math.min(50, entry.getValue().length())),
                        bestScores.get(entry.getKey()));
            }
            
            return OptimizationResult.builder()
                    .success(true)
                    .finalScore(bestScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                    .totalSteps(iterations)
                    .message("Optimization completed successfully")
                    .build();
        }
        
        log.warn("Invalid target type for optimization");
        return OptimizationResult.builder()
                .success(false)
                .finalScore(0.0)
                .totalSteps(0)
                .message("Invalid target type")
                .build();
    }
}
