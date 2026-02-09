package io.leavesfly.evox.cowork.connector.builtin;

import io.leavesfly.evox.cowork.connector.BaseConnector;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class LocalFileConnector extends BaseConnector {
    
    public LocalFileConnector() {
        super("local-file", "Local File System", "Access and manage local files and directories", ConnectorType.CLOUD_STORAGE);
    }
    
    @Override
    protected void doConnect() throws Exception {
        log.info("LocalFileConnector connected: no special setup required");
    }
    
    @Override
    protected void doDisconnect() {
        log.info("LocalFileConnector disconnected");
    }
    
    @Override
    public List<String> getSupportedActions() {
        return List.of("list", "read", "write", "search");
    }
    
    @Override
    protected Map<String, Object> doExecute(String action, Map<String, Object> parameters) {
        try {
            switch (action) {
                case "list":
                    return listFiles(parameters);
                case "read":
                    return readFile(parameters);
                case "write":
                    return writeFile(parameters);
                case "search":
                    return searchFiles(parameters);
                default:
                    throw new UnsupportedOperationException("Action '" + action + "' is not supported");
            }
        } catch (IOException e) {
            log.error("Error executing action '{}': {}", action, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
    
    private Map<String, Object> listFiles(Map<String, Object> parameters) throws IOException {
        String directory = (String) parameters.get("directory");
        if (directory == null) {
            directory = ".";
        }
        
        Path path = Paths.get(directory);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Directory does not exist: " + directory);
            return result;
        }
        
        List<String> files = Files.list(path)
            .map(p -> p.getFileName().toString())
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("directory", directory);
        result.put("files", files);
        return result;
    }
    
    private Map<String, Object> readFile(Map<String, Object> parameters) throws IOException {
        String filePath = (String) parameters.get("filePath");
        if (filePath == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "filePath parameter is required");
            return result;
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "File does not exist: " + filePath);
            return result;
        }
        
        String content = Files.readString(path);
        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("content", content);
        return result;
    }
    
    private Map<String, Object> writeFile(Map<String, Object> parameters) throws IOException {
        String filePath = (String) parameters.get("filePath");
        String content = (String) parameters.get("content");
        
        if (filePath == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "filePath parameter is required");
            return result;
        }
        
        if (content == null) {
            content = "";
        }
        
        Path path = Paths.get(filePath);
        Files.writeString(path, content);
        
        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("success", true);
        result.put("message", "File written successfully");
        return result;
    }
    
    private Map<String, Object> searchFiles(Map<String, Object> parameters) throws IOException {
        String directory = (String) parameters.get("directory");
        String pattern = (String) parameters.get("pattern");
        
        if (directory == null) {
            directory = ".";
        }
        if (pattern == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "pattern parameter is required");
            return result;
        }
        
        Path path = Paths.get(directory);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Directory does not exist: " + directory);
            return result;
        }
        
        List<String> matches = Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().contains(pattern))
            .map(p -> p.toString())
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("directory", directory);
        result.put("pattern", pattern);
        result.put("matches", matches);
        return result;
    }
}
