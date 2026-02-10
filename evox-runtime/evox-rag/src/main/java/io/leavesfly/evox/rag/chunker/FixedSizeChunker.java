package io.leavesfly.evox.rag.chunker;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Document;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定大小分块器
 *
 * @author EvoX Team
 */
@Slf4j
public class FixedSizeChunker implements Chunker {

    private final int chunkSize;
    private final int chunkOverlap;
    private final Integer maxChunks;

    public FixedSizeChunker(int chunkSize, int chunkOverlap, Integer maxChunks) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }
        
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.maxChunks = maxChunks;
    }

    @Override
    public List<Chunk> chunk(Document document) {
        if (document == null || document.getText() == null || document.getText().isEmpty()) {
            return new ArrayList<>();
        }

        String text = document.getText();
        int textLength = text.length();
        List<Chunk> chunks = new ArrayList<>();
        
        int startPos = 0;
        int chunkIndex = 0;
        int step = chunkSize - chunkOverlap;

        while (startPos < textLength) {
            // 检查是否达到最大分块数
            if (maxChunks != null && chunkIndex >= maxChunks) {
                log.debug("Reached max chunks limit: {}", maxChunks);
                break;
            }

            // 计算当前分块的结束位置
            int endPos = Math.min(startPos + chunkSize, textLength);
            
            // 创建分块
            Chunk chunk = Chunk.fromDocument(document, chunkIndex, startPos, endPos);
            chunks.add(chunk);
            
            // 移动到下一个分块的起始位置
            startPos += step;
            chunkIndex++;
            
            // 如果剩余文本太短，不再分块
            if (textLength - startPos < chunkSize / 2) {
                break;
            }
        }

        log.debug("Created {} chunks from document {} (length: {})", 
                chunks.size(), document.getId(), textLength);
        
        return chunks;
    }
}
