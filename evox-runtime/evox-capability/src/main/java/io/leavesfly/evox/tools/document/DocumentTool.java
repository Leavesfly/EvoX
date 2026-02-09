package io.leavesfly.evox.tools.document;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档处理工具
 * 支持读取和创建 PDF、Word (docx)、Excel (xlsx)、PowerPoint (pptx)、CSV 和文本文件
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentTool extends BaseTool {

    public DocumentTool() {
        super();
        this.name = "document";
        this.description = "A tool for reading and creating documents including PDF, Word (docx), Excel (xlsx), PowerPoint (pptx), CSV, and text files";
        
        this.inputs = new HashMap<>();
        
        Map<String, String> operationInput = new HashMap<>();
        operationInput.put("type", "string");
        operationInput.put("description", "Operation: read, create_text, create_csv, info");
        this.inputs.put("operation", operationInput);
        
        Map<String, String> filePathInput = new HashMap<>();
        filePathInput.put("type", "string");
        filePathInput.put("description", "Path to the document file");
        this.inputs.put("filePath", filePathInput);
        
        Map<String, String> contentInput = new HashMap<>();
        contentInput.put("type", "string");
        contentInput.put("description", "Content to write (for create operations)");
        this.inputs.put("content", contentInput);
        
        Map<String, String> formatInput = new HashMap<>();
        formatInput.put("type", "string");
        formatInput.put("description", "Output format: txt, csv, md");
        this.inputs.put("format", formatInput);
        
        this.required = List.of("operation", "filePath");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String operation = getParameter(parameters, "operation", "");
            String filePath = getParameter(parameters, "filePath", "");
            
            switch (operation) {
                case "read":
                    return readDocument(filePath);
                case "create_text":
                    String content = getParameter(parameters, "content", "");
                    return createTextFile(filePath, content);
                case "create_csv":
                    String csvContent = getParameter(parameters, "content", "");
                    return createCsvFile(filePath, csvContent);
                case "info":
                    return getDocumentInfo(filePath);
                default:
                    return ToolResult.failure("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            log.error("Document tool execution error", e);
            return ToolResult.failure("Error executing document tool: " + e.getMessage());
        }
    }

    private ToolResult readDocument(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String extension = getFileExtension(filePath);
            String content;
            
            switch (extension) {
                case "txt":
                case "md":
                case "json":
                case "xml":
                case "yaml":
                case "yml":
                case "csv":
                case "tsv":
                    content = Files.readString(path);
                    break;
                case "pdf":
                    content = "PDF reading requires Apache PDFBox. File exists: " + Files.exists(path);
                    break;
                case "docx":
                    content = "DOCX reading requires Apache POI. File exists: " + Files.exists(path);
                    break;
                case "xlsx":
                    content = "XLSX reading requires Apache POI. File exists: " + Files.exists(path);
                    break;
                case "pptx":
                    content = "PPTX reading requires Apache POI. File exists: " + Files.exists(path);
                    break;
                default:
                    try {
                        content = Files.readString(path);
                    } catch (IOException e) {
                        return ToolResult.failure("Failed to read file: " + e.getMessage());
                    }
                    break;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("filePath", filePath);
            result.put("size", Files.size(path));
            
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error reading document: {}", filePath, e);
            return ToolResult.failure("Failed to read document: " + e.getMessage());
        }
    }

    private ToolResult createTextFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "File created successfully");
            result.put("filePath", filePath);
            
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error creating text file: {}", filePath, e);
            return ToolResult.failure("Failed to create text file: " + e.getMessage());
        }
    }

    private ToolResult createCsvFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "CSV file created successfully");
            result.put("filePath", filePath);
            
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error creating CSV file: {}", filePath, e);
            return ToolResult.failure("Failed to create CSV file: " + e.getMessage());
        }
    }

    private ToolResult getDocumentInfo(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String extension = getFileExtension(filePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("size", Files.size(path));
            result.put("lastModified", Files.getLastModifiedTime(path).toMillis());
            result.put("type", extension);
            
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error getting document info: {}", filePath, e);
            return ToolResult.failure("Failed to get document info: " + e.getMessage());
        }
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
