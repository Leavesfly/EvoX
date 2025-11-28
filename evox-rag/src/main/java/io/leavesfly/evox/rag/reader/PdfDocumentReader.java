package io.leavesfly.evox.rag.reader;

import io.leavesfly.evox.rag.schema.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * PDF文档读取器
 * 基于Apache PDFBox实现
 *
 * @author EvoX Team
 */
@Slf4j
public class PdfDocumentReader implements DocumentReader {

    private static final List<String> SUPPORTED_EXTENSIONS = Collections.singletonList(".pdf");

    private final boolean excludeHidden;
    private final boolean mergePages;

    public PdfDocumentReader() {
        this(true, true);
    }

    public PdfDocumentReader(boolean excludeHidden, boolean mergePages) {
        this.excludeHidden = excludeHidden;
        this.mergePages = mergePages;
    }

    @Override
    public Document loadFromFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        if (!filePath.toString().toLowerCase().endsWith(".pdf")) {
            throw new IOException("Not a PDF file: " + filePath);
        }

        try (PDDocument pdfDocument = org.apache.pdfbox.Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            Document document = Document.builder()
                    .text(text)
                    .source(filePath.toString())
                    .type(Document.DocumentType.PDF)
                    .build();

            // 添加PDF元数据
            document.setMetadata("file_path", filePath.toString());
            document.setMetadata("file_name", filePath.getFileName().toString());
            document.setMetadata("file_size", Files.size(filePath));
            document.setMetadata("extension", ".pdf");
            document.setMetadata("page_count", pdfDocument.getNumberOfPages());

            // PDF文档信息
            if (pdfDocument.getDocumentInformation() != null) {
                var info = pdfDocument.getDocumentInformation();
                if (info.getTitle() != null) {
                    document.setMetadata("title", info.getTitle());
                }
                if (info.getAuthor() != null) {
                    document.setMetadata("author", info.getAuthor());
                }
                if (info.getSubject() != null) {
                    document.setMetadata("subject", info.getSubject());
                }
            }

            log.debug("Loaded PDF document with {} pages from: {}", 
                    pdfDocument.getNumberOfPages(), filePath);
            return document;

        } catch (IOException e) {
            log.error("Failed to load PDF file {}: {}", filePath, e.getMessage());
            throw new IOException("Failed to parse PDF: " + e.getMessage(), e);
        }
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
            
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .filter(this::shouldIncludeFile)
                    .forEach(filePath -> {
                        try {
                            documents.add(loadFromFile(filePath));
                        } catch (IOException e) {
                            log.warn("Failed to load PDF {}: {}", filePath, e.getMessage());
                        }
                    });
        }

        log.info("Loaded {} PDF documents from directory: {}", documents.size(), dirPath);
        return documents;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return new ArrayList<>(SUPPORTED_EXTENSIONS);
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
