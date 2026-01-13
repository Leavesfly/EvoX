package io.leavesfly.evox.rag.reader;

import io.leavesfly.evox.rag.schema.Document;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 纯文本文档读取器
 * 支持 .txt, .md, .json, .csv 等文本格式
 *
 * @author EvoX Team
 */
@Slf4j
public class TextDocumentReader implements DocumentReader {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            ".txt", ".md", ".json", ".csv", ".log", ".xml", ".yaml", ".yml"
    );

    private final boolean excludeHidden;

    public TextDocumentReader() {
        this(true);
    }

    public TextDocumentReader(boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
    }

    @Override
    public Document loadFromFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Not a regular file: " + filePath);
        }

        // 读取文件内容
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        // 检测文档类型
        Document.DocumentType docType = detectDocumentType(filePath);

        // 构建文档
        Document document = Document.builder()
                .text(content)
                .source(filePath.toString())
                .type(docType)
                .build();

        // 添加元数据
        document.setMetadata("file_path", filePath.toString());
        document.setMetadata("file_name", filePath.getFileName().toString());
        document.setMetadata("file_size", Files.size(filePath));
        document.setMetadata("extension", getFileExtension(filePath));

        log.debug("Loaded document from: {}", filePath);
        return document;
    }

    @Override
    public List<Document> loadFromDirectory(Path dirPath, boolean recursive) throws IOException {
        if (!Files.exists(dirPath)) {
            throw new IOException("Directory not found: " + dirPath);
        }

        if (!Files.isDirectory(dirPath)) {
            throw new IOException("Not a directory: " + dirPath);
        }

        List<Document> documents = new ArrayList<>();

        try (Stream<Path> paths = recursive ? 
                Files.walk(dirPath) : 
                Files.list(dirPath)) {
            
            List<Path> filePaths = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .filter(this::shouldIncludeFile)
                    .collect(Collectors.toList());

            for (Path filePath : filePaths) {
                try {
                    documents.add(loadFromFile(filePath));
                } catch (IOException e) {
                    log.warn("Failed to load file {}: {}", filePath, e.getMessage());
                }
            }
        }

        log.info("Loaded {} documents from directory: {}", documents.size(), dirPath);
        return documents;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return new ArrayList<>(SUPPORTED_EXTENSIONS);
    }

    /**
     * 检测文档类型
     */
    private Document.DocumentType detectDocumentType(Path filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        
        return switch (extension) {
            case ".json" -> Document.DocumentType.JSON;
            case ".csv" -> Document.DocumentType.CSV;
            case ".md" -> Document.DocumentType.MARKDOWN;
            case ".html", ".htm" -> Document.DocumentType.HTML;
            default -> Document.DocumentType.TEXT;
        };
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : "";
    }

    /**
     * 判断是否为支持的文件
     */
    private boolean isSupportedFile(Path filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    /**
     * 判断是否应该包含该文件
     */
    private boolean shouldIncludeFile(Path filePath) {
        if (excludeHidden) {
            String fileName = filePath.getFileName().toString();
            return !fileName.startsWith(".");
        }
        return true;
    }
}
