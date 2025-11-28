package io.leavesfly.evox.rag.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索结果类 - RAG检索返回的结果
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /**
     * 检索到的分块列表
     */
    @Builder.Default
    private List<Chunk> chunks = new ArrayList<>();

    /**
     * 查询对象
     */
    private Query query;

    /**
     * 结果总数
     */
    private int totalResults;

    /**
     * 检索耗时(毫秒)
     */
    private long latencyMs;

    /**
     * 结果元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 添加分块
     */
    public void addChunk(Chunk chunk) {
        if (chunks == null) {
            chunks = new ArrayList<>();
        }
        chunks.add(chunk);
        totalResults = chunks.size();
    }

    /**
     * 添加多个分块
     */
    public void addChunks(List<Chunk> chunks) {
        if (this.chunks == null) {
            this.chunks = new ArrayList<>();
        }
        this.chunks.addAll(chunks);
        totalResults = this.chunks.size();
    }

    /**
     * 获取最相关的分块
     */
    public Chunk getTopChunk() {
        if (chunks == null || chunks.isEmpty()) {
            return null;
        }
        return chunks.get(0);
    }

    /**
     * 获取Top N分块
     */
    public List<Chunk> getTopChunks(int n) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }
        int limit = Math.min(n, chunks.size());
        return new ArrayList<>(chunks.subList(0, limit));
    }

    /**
     * 合并分块文本
     */
    public String getCombinedText(String separator) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(chunks.get(i).getText());
        }
        return sb.toString();
    }

    /**
     * 过滤低于阈值的结果
     */
    public void filterByThreshold(float threshold) {
        if (chunks == null) {
            return;
        }
        chunks.removeIf(chunk -> 
            chunk.getSimilarityScore() != null && chunk.getSimilarityScore() < threshold
        );
        totalResults = chunks.size();
    }
}
