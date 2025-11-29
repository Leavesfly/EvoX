package io.leavesfly.evox.examples;

import io.leavesfly.evox.benchmark.GSM8K;
import io.leavesfly.evox.benchmark.HumanEval;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Benchmark基准测试示例
 * 演示如何使用不同的Benchmark评估模型性能
 *
 * @author EvoX Team
 */
public class BenchmarkExample {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkExample.class);

    public static void main(String[] args) {
        BenchmarkExample example = new BenchmarkExample();
        example.runGSM8KExample();
        example.runHumanEvalExample();
    }

    /**
     * GSM8K数学问题评测示例
     */
    private void runGSM8KExample() {
        log.info("\n--- GSM8K Math Benchmark ---");

        try {
            // 创建GSM8K benchmark实例
            // 注意: 需要提供实际的数据文件路径
            String dataPath = "data/gsm8k/test.jsonl";
            GSM8K gsm8k = new GSM8K(dataPath);

            // 加载数据
            log.info("Loading GSM8K data from: {}", dataPath);
            // gsm8k.loadData(); // 实际使用时需要取消注释

            log.info("GSM8K benchmark created successfully");
            log.info("Train data size: {} (if available)", 
                    gsm8k.getTrainData() != null ? gsm8k.getTrainData().size() : 0);
            log.info("Test data size: {}", 
                    gsm8k.getTestData() != null ? gsm8k.getTestData().size() : 0);

            // 模拟评估过程
            log.info("\nEvaluating a sample prediction:");
            String question = "Janet's ducks lay 16 eggs per day. She eats three for breakfast every morning " +
                            "and bakes muffins for her friends every day with four. She sells the remainder " +
                            "at the farmers' market daily for $2 per fresh duck egg. How much in dollars does " +
                            "she make every day at the farmers' market?";
            
            String groundTruth = "18";
            String prediction = "Janet lays 16 eggs, eats 3, uses 4, so she has 16-3-4=9 eggs left. " +
                              "She sells them for $2 each, so 9*2=18 dollars. #### 18";

            log.info("Question: {}", question);
            log.info("Ground Truth: {}", groundTruth);
            log.info("Prediction: {}", prediction);

            // 评估单个样本
            var metrics = gsm8k.evaluate(prediction, groundTruth);
            log.info("Evaluation result: {}", metrics);

        } catch (Exception e) {
            log.error("GSM8K example failed: {}", e.getMessage());
            log.info("Note: Make sure GSM8K data file exists at the specified path");
        }
    }

    /**
     * HumanEval代码生成评测示例
     */
    private void runHumanEvalExample() {
        log.info("\n--- HumanEval Code Generation Benchmark ---");

        try {
            // 创建HumanEval benchmark实例
            String dataPath = "data/humaneval/test.jsonl";
            HumanEval humanEval = new HumanEval(dataPath);

            log.info("HumanEval benchmark created successfully");
            log.info("Data path: {}", dataPath);

            // 模拟代码评估
            log.info("\nEvaluating sample code:");
            
            String prompt = "def has_close_elements(numbers, threshold):\n" +
                          "    \"\"\"Check if in given list of numbers, are any two numbers closer " +
                          "to each other than given threshold.\"\"\"\n";
            
            String canonicalSolution = "    for idx, elem in enumerate(numbers):\n" +
                                     "        for idx2, elem2 in enumerate(numbers):\n" +
                                     "            if idx != idx2:\n" +
                                     "                distance = abs(elem - elem2)\n" +
                                     "                if distance < threshold:\n" +
                                     "                    return True\n" +
                                     "    return False\n";
            
            String generatedCode = "    for i in range(len(numbers)):\n" +
                                 "        for j in range(i+1, len(numbers)):\n" +
                                 "            if abs(numbers[i] - numbers[j]) < threshold:\n" +
                                 "                return True\n" +
                                 "    return False\n";

            log.info("Prompt:\n{}", prompt);
            log.info("Canonical solution:\n{}", canonicalSolution);
            log.info("Generated code:\n{}", generatedCode);

            // 评估代码相似度
            var metrics = humanEval.evaluate(generatedCode, canonicalSolution);
            log.info("Code evaluation metrics: {}", metrics);
            log.info("Similarity score: {}", metrics.get("accuracy"));

        } catch (Exception e) {
            log.error("HumanEval example failed: {}", e.getMessage());
            log.info("Note: Make sure HumanEval data file exists at the specified path");
        }
    }

    /**
     * 批量评估示例
     */
    @SuppressWarnings("unused")
    private void runBatchEvaluation() {
        log.info("\n--- Batch Evaluation Example ---");

        // 创建benchmark
        GSM8K gsm8k = new GSM8K("data/gsm8k/test.jsonl");

        // 准备批量预测
        // 这里需要一个模型或agent来生成预测
        log.info("For batch evaluation, you would:");
        log.info("1. Load your model/agent");
        log.info("2. Generate predictions for all test samples");
        log.info("3. Call benchmark.evaluate() with all predictions");
        log.info("4. Analyze the aggregated metrics");

        /*
        // 伪代码示例:
        List<String> predictions = new ArrayList<>();
        for (GSM8K.GSM8KExample example : gsm8k.getTestData()) {
            String prediction = yourModel.predict(example.getQuestion());
            predictions.add(prediction);
        }
        
        Map<String, Double> metrics = gsm8k.evaluate(predictions);
        log.info("Overall accuracy: {}", metrics.get("accuracy"));
        */
    }

    /**
     * 自定义评估指标示例
     */
    @SuppressWarnings("unused")
    private void customMetricsExample() {
        log.info("\n--- Custom Metrics Example ---");

        log.info("You can extend the Benchmark class to add custom metrics:");
        log.info("1. Override evaluateSingle() to compute additional metrics");
        log.info("2. Override evaluate() to aggregate custom metrics");
        log.info("3. Use metrics like exact match, F1, BLEU, etc.");
    }
}
