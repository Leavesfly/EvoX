package io.leavesfly.evox.tools.file;

import lombok.Data;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 文件系统工具
 * 提供文件读写、目录操作等功能
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class FileSystemTool extends BaseTool {

    private String name;
    private String description;
    private String workingDirectory;
    private boolean allowAbsolutePaths;
    private List<String> allowedExtensions;

    public FileSystemTool() {
        this.name = "FileSystemTool";
        this.description = "A tool for file system operations including read, write, append, delete, and list files";
        this.workingDirectory = System.getProperty("user.dir");
        this.allowAbsolutePaths = false;
        this.allowedExtensions = Arrays.asList(".txt", ".md", ".json", ".xml", ".csv", ".log", ".java", ".py");
        
        // 初始化BaseTool的属性
        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();
        
        // 定义输入参数
        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation to perform: read, write, append, delete, list, exists, mkdir");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");
    }

    /**
     * 执行工具操作
     */
    @Override
    public ToolResult execute(Map<String, Object> params) {
        String operation = (String) params.get("operation");
        
        if (operation == null) {
            return error("Operation parameter is required");
        }

        switch (operation) {
            case "read":
                return readFile((String) params.get("filePath"));
            case "write":
                return writeFile((String) params.get("filePath"), (String) params.get("content"));
            case "append":
                return appendFile((String) params.get("filePath"), (String) params.get("content"));
            case "delete":
                return deleteFile((String) params.get("filePath"));
            case "list":
                return listFiles((String) params.get("directory"));
            case "exists":
                return checkFileExists((String) params.get("filePath"));
            case "mkdir":
                return createDirectory((String) params.get("directory"));
            default:
                return error("Unknown operation: " + operation);
        }
    }

    /**
     * 读取文件内容
     */
    public ToolResult readFile(String filePath) {
        try {
            Path path = resolvePath(filePath);
            
            if (!Files.exists(path)) {
                return error("File not found: " + filePath);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("filePath", path.toString());
            result.put("size", Files.size(path));
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            return error("Error reading file: " + e.getMessage());
        }
    }

    /**
     * 写入文件内容
     */
    public ToolResult writeFile(String filePath, String content) {
        try {
            Path path = resolvePath(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "File written successfully");
            result.put("filePath", path.toString());
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error writing file: {}", filePath, e);
            return error("Error writing file: " + e.getMessage());
        }
    }

    /**
     * 追加文件内容
     */
    public ToolResult appendFile(String filePath, String content) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Content appended");
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error appending: {}", filePath, e);
            return error("Error appending: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public ToolResult deleteFile(String filePath) {
        try {
            Path path = resolvePath(filePath);
            Files.delete(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "File deleted");
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error deleting: {}", filePath, e);
            return error("Error deleting: " + e.getMessage());
        }
    }

    /**
     * 列出目录中的文件
     */
    public ToolResult listFiles(String directory) {
        try {
            Path path = directory != null ? resolvePath(directory) : Paths.get(workingDirectory);
            
            List<Map<String, Object>> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", entry.getFileName().toString());
                    fileInfo.put("isDirectory", Files.isDirectory(entry));
                    files.add(fileInfo);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error listing: {}", directory, e);
            return error("Error listing: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     */
    public ToolResult checkFileExists(String filePath) {
        try {
            Path path = resolvePath(filePath);
            Map<String, Object> result = new HashMap<>();
            result.put("exists", Files.exists(path));
            return ToolResult.success(result);
        } catch (IOException e) {
            return error("Error checking: " + e.getMessage());
        }
    }

    /**
     * 创建目录
     */
    public ToolResult createDirectory(String directory) {
        try {
            Path path = resolvePath(directory);
            Files.createDirectories(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Directory created");
            return ToolResult.success(result);
        } catch (IOException e) {
            return error("Error creating: " + e.getMessage());
        }
    }

    private Path resolvePath(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IOException("File path cannot be null");
        }
        Path path = Paths.get(filePath);
        if (!allowAbsolutePaths && path.isAbsolute()) {
            throw new IOException("Absolute paths not allowed");
        }
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(path).normalize();
        }
        return path;
    }

    private ToolResult error(String message) {
        return ToolResult.failure(message);
    }
}
