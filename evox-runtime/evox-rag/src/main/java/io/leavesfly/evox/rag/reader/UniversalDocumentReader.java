package io.leavesfly.evox.rag.reader;

import io.leavesfly.evox.rag.schema.Document;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 通用文档读取器
 * 根据文件类型自动选择合适的读取器
 *
 * @author EvoX Team
 */
@Slf4j
public class UniversalDocumentReader implements DocumentReader {

    private final Map<String, DocumentReader> readerMap;
    private final boolean excludeHidden;

    public UniversalDocumentReader() {
        this(true);
    }

    public UniversalDocumentReader(boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
        this.readerMap = new HashMap<>();
        registerDefaultReaders();
    }

    /**
     * 注册默认的文档读取器
     */
    private void registerDefaultReaders() {
        // 文本读取器
        TextDocumentReader textReader = new TextDocumentReader(excludeHidden);
        for (String ext : textReader.getSupportedExtensions()) {
            readerMap.put(ext.toLowerCase(), textReader);
        }

        // PDF读取器
        PdfDocumentReader pdfReader = new PdfDocumentReader(excludeHidden, true);
        for (String ext : pdfReader.getSupportedExtensions()) {
            readerMap.put(ext.toLowerCase(), pdfReader);
        }

        log.info("Registered {} document readers for {} extensions", 
                2, readerMap.size());
    }

    /**
     * 注册自定义读取器
     */
    public void registerReader(String extension, DocumentReader reader) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        readerMap.put(extension.toLowerCase(), reader);
        log.debug("Registered custom reader for extension: {}", extension);
    }

    @Override
    public Document loadFromFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        String extension = getFileExtension(filePath).toLowerCase();
        DocumentReader reader = readerMap.get(extension);

        if (reader == null) {
            throw new IOException("Unsupported file type: " + extension);
        }

        return reader.loadFromFile(filePath);
    }

    @Override
    public List<Document> loadFromDirectory(Path dirPath, boolean recursive) throws IOException {
        if (!Files.exists(dirPath)) {
            throw new IOException("Directory not found: " + dirPath);
        }

        if (!Files.isDirectory(dirPath)) {
            throw new IOException("Not a directory: " + dirPath);
        }

        List<Document> allDocuments = new ArrayList<>();

        // 按文件类型分组
        Map<DocumentReader, List<Path>> readerPaths = new HashMap<>();

        try (var paths = recursive ? Files.walk(dirPath) : Files.list(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::shouldIncludeFile)
                    .forEach(filePath -> {
                        String ext = getFileExtension(filePath).toLowerCase();
                        DocumentReader reader = readerMap.get(ext);
                        if (reader != null) {
                            readerPaths.computeIfAbsent(reader, k -> new ArrayList<>())
                                    .add(filePath);
                        } else {
                            log.warn("Skipping unsupported file: {}", filePath);
                        }
                    });
        }

        // 使用相应的读取器加载文档
        for (Map.Entry<DocumentReader, List<Path>> entry : readerPaths.entrySet()) {
            for (Path filePath : entry.getValue()) {
                try {
                    allDocuments.add(entry.getKey().loadFromFile(filePath));
                } catch (IOException e) {
                    log.warn("Failed to load file {}: {}", filePath, e.getMessage());
                }
            }
        }

        log.info("Loaded {} documents from directory: {}", allDocuments.size(), dirPath);
        return allDocuments;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return new ArrayList<>(readerMap.keySet());
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
     * 判断是否应该包含该文件
     */
    private boolean shouldIncludeFile(Path filePath) {
        if (excludeHidden) {
            String fileName = filePath.getFileName().toString();
            return !fileName.startsWith(".");
        }
        return true;
    }

    /**
     * 获取已注册的读取器数量
     */
    public int getRegisteredReaderCount() {
        return new HashSet<>(readerMap.values()).size();
    }
}
