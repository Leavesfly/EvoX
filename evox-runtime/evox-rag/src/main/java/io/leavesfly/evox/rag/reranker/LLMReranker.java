package io.leavesfly.evox.rag.reranker;

import io.leavesfly.evox.core.llm.ILLM;
import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * LLM重排序器 - 使用大语言模型评估query-document相关性
 *
 * @author EvoX Team
 */
@Slf4j
public class LLMReranker implements Reranker {

    private final ILLM llm;
    private final String promptTemplate;
    private final double scoreThreshold;

    private static final String DEFAULT_PROMPT_TEMPLATE = 
            "请评估以下文档片段与查询的相关性，给出0-10的分数。\n\n" +
            "查询: {query}\n\n" +
            "文档片段: {document}\n\n" +
            "相关性分数(0-10):";

    public LLMReranker(ILLM llm, String promptTemplate, double scoreThreshold) {
        this.llm = llm;
        this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
        this.scoreThreshold = scoreThreshold;
        log.info("Initialized LLMReranker with threshold: {}", scoreThreshold);
    }

    public LLMReranker(ILLM llm) {
        this(llm, null, 0.0);
    }

    public LLMReranker(ILLM llm, double scoreThreshold) {
        this(llm, null, scoreThreshold);
    }

    @Override
    public List<Chunk> rerank(Query query, List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("Reranking {} chunks with LLM", chunks.size());

        // 创建副本避免修改原始列表
        List<Chunk> rerankedChunks = new ArrayList<>(chunks);

        // 使用LLM计算每个chunk的相关性分数
        for (Chunk chunk : rerankedChunks) {
            double llmScore = computeLLMScore(query.getQueryText(), chunk.getText());
            chunk.setSimilarityScore((float) llmScore);
        }

        // 按分数降序排序
        rerankedChunks.sort(Comparator.comparing(
                Chunk::getSimilarityScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 过滤低于阈值的结果
        if (scoreThreshold > 0) {
            rerankedChunks.removeIf(chunk ->
                    chunk.getSimilarityScore() == null || chunk.getSimilarityScore() < scoreThreshold
            );
        }

        log.debug("LLM reranking completed. Returned {} chunks", rerankedChunks.size());
        return rerankedChunks;
    }

    /**
     * 使用LLM计算相关性分数
     */
    private double computeLLMScore(String query, String document) {
        try {
            // 构建prompt
            String prompt = promptTemplate
                    .replace("{query}", query)
                    .replace("{document}", document);

            // 调用LLM
            String response = llm.generate(prompt);

            // 解析分数
            return parseScore(response);
        } catch (Exception e) {
            log.error("Failed to compute LLM score: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * 从LLM响应中解析分数
     */
    private double parseScore(String response) {
        if (response == null || response.trim().isEmpty()) {
            return 0.0;
        }

        // 尝试提取数字
        response = response.trim();
        
        // 提取第一个数字（可能是0-10或0.0-1.0格式）
        String[] tokens = response.split("\\s+");
        for (String token : tokens) {
            try {
                double score = Double.parseDouble(token.replaceAll("[^0-9.]", ""));
                // 如果分数大于1，假设是0-10范围，需要归一化到0-1
                if (score > 1.0) {
                    return score / 10.0;
                }
                return score;
            } catch (NumberFormatException ignored) {
                // 继续尝试下一个token
            }
        }

        log.warn("Failed to parse score from LLM response: {}", response);
        return 0.0;
    }

    @Override
    public String getName() {
        return "LLMReranker(" + llm.getClass().getSimpleName() + ")";
    }
}
