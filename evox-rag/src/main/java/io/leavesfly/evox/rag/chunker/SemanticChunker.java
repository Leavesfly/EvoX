package io.leavesfly.evox.rag.chunker;

import io.leavesfly.evox.rag.schema.Chunk;
import io.leavesfly.evox.rag.schema.Document;
import lombok.extern.slf4j.Slf4j;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 语义分块器
 * 基于句子边界进行智能分块，保持语义完整性
 *
 * @author EvoX Team
 */
@Slf4j
public class SemanticChunker implements Chunker {

    private final int targetChunkSize;
    private final int maxChunkSize;
    private final Integer maxChunks;

    /**
     * @param targetChunkSize 目标分块大小(字符数)
     * @param maxChunkSize 最大分块大小
     * @param maxChunks 最大分块数量
     */
    public SemanticChunker(int targetChunkSize, int maxChunkSize, Integer maxChunks) {
        if (targetChunkSize <= 0) {
            throw new IllegalArgumentException("Target chunk size must be positive");
        }
        if (maxChunkSize < targetChunkSize) {
            throw new IllegalArgumentException("Max chunk size must be >= target chunk size");
        }

        this.targetChunkSize = targetChunkSize;
        this.maxChunkSize = maxChunkSize;
        this.maxChunks = maxChunks;
    }

    @Override
    public List<Chunk> chunk(Document document) {
        if (document == null || document.getText() == null || document.getText().isEmpty()) {
            return new ArrayList<>();
        }

        String text = document.getText();
        List<String> sentences = splitIntoSentences(text);
        List<Chunk> chunks = new ArrayList<>();

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int startPos = 0;

        for (String sentence : sentences) {
            // 检查是否达到最大分块数
            if (maxChunks != null && chunkIndex >= maxChunks) {
                log.debug("Reached max chunks limit: {}", maxChunks);
                break;
            }

            int sentenceLength = sentence.length();
            int currentLength = currentChunk.length();

            // 如果当前块为空，直接添加句子
            if (currentLength == 0) {
                currentChunk.append(sentence);
                continue;
            }

            // 如果添加这个句子会超过最大大小
            if (currentLength + sentenceLength > maxChunkSize) {
                // 保存当前块
                int endPos = Math.min(startPos + currentChunk.length(), text.length());
                Chunk chunk = Chunk.fromDocument(document, chunkIndex, startPos, endPos);
                chunks.add(chunk);
                
                // 开始新块
                currentChunk = new StringBuilder(sentence);
                startPos = endPos;
                chunkIndex++;
                continue;
            }

            // 如果添加这个句子超过目标大小，检查是否应该分块
            if (currentLength + sentenceLength >= targetChunkSize) {
                // 保存当前块
                int endPos = Math.min(startPos + currentChunk.length(), text.length());
                Chunk chunk = Chunk.fromDocument(document, chunkIndex, startPos, endPos);
                chunks.add(chunk);
                
                // 开始新块
                currentChunk = new StringBuilder(sentence);
                startPos = endPos;
                chunkIndex++;
            } else {
                // 添加句子到当前块
                currentChunk.append(sentence);
            }
        }

        // 处理最后一个块
        if (currentChunk.length() > 0) {
            if (maxChunks == null || chunkIndex < maxChunks) {
                int endPos = Math.min(startPos + currentChunk.length(), text.length());
                Chunk chunk = Chunk.fromDocument(document, chunkIndex, startPos, endPos);
                chunks.add(chunk);
            }
        }

        log.debug("Created {} semantic chunks from document {} (length: {})", 
                chunks.size(), document.getId(), text.length());
        
        return chunks;
    }

    /**
     * 将文本拆分为句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 支持中英文的句子分割
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.CHINESE);
        iterator.setText(text);

        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence + " "); // 保留句子间的空格
            }
        }

        return sentences;
    }
}
