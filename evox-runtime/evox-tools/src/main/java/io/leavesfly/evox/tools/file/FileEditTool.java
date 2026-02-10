package io.leavesfly.evox.tools.file;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 文件精确编辑工具
 * 基于 old_string → new_string 的精确替换方式编辑文件，
 * 支持创建新文件、插入内容、多处替换和行号定位编辑
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class FileEditTool extends BaseTool {

    private String workingDirectory;

    public FileEditTool() {
        this(System.getProperty("user.dir"));
    }

    public FileEditTool(String workingDirectory) {
        super();
        this.name = "file_edit";
        this.description = "Edit files using precise old_string -> new_string replacement. "
                + "Can also create new files. Supports multiple modes: "
                + "1. Exact replacement with optional replaceAll flag. "
                + "2. Line number range replacement when startLine and endLine are provided.";
        this.workingDirectory = workingDirectory;

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> filePathParam = new HashMap<>();
        filePathParam.put("type", "string");
        filePathParam.put("description", "The file path to edit (relative to project root or absolute)");
        this.inputs.put("filePath", filePathParam);
        this.required.add("filePath");

        Map<String, String> oldStringParam = new HashMap<>();
        oldStringParam.put("type", "string");
        oldStringParam.put("description", "The exact string to find and replace. Empty string means create a new file. Not required if using line-based editing.");
        this.inputs.put("oldString", oldStringParam);
        // Not adding to required as it's optional when using line-based editing

        Map<String, String> newStringParam = new HashMap<>();
        newStringParam.put("type", "string");
        newStringParam.put("description", "The replacement string. Empty string means delete the old_string.");
        this.inputs.put("newString", newStringParam);

        Map<String, String> replaceAllParam = new HashMap<>();
        replaceAllParam.put("type", "boolean");
        replaceAllParam.put("description", "If true, replace all occurrences of old_string instead of just the first one. Default is false.");
        this.inputs.put("replaceAll", replaceAllParam);

        Map<String, String> startLineParam = new HashMap<>();
        startLineParam.put("type", "integer");
        startLineParam.put("description", "The starting line number (1-based) for line-based editing. When provided with endLine, replaces lines from startLine to endLine with newString.");
        this.inputs.put("startLine", startLineParam);

        Map<String, String> endLineParam = new HashMap<>();
        endLineParam.put("type", "integer");
        endLineParam.put("description", "The ending line number (1-based) for line-based editing. Required when startLine is provided.");
        this.inputs.put("endLine", endLineParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String filePath = getParameter(parameters, "filePath", "");
        String oldString = getParameter(parameters, "oldString", "");
        String newString = getParameter(parameters, "newString", "");
        Boolean replaceAll = getParameter(parameters, "replaceAll", false);
        Integer startLine = getParameter(parameters, "startLine", null);
        Integer endLine = getParameter(parameters, "endLine", null);

        if (filePath.isBlank()) {
            return ToolResult.failure("File path cannot be empty");
        }

        try {
            Path path = resolvePath(filePath);

            // Line-based editing mode
            if (startLine != null && endLine != null) {
                if (!Files.exists(path)) {
                    return ToolResult.failure("File not found for line-based editing: " + filePath);
                }
                return editByLineRange(path, startLine, endLine, newString);
            }

            // String-based replacement mode
            // create new file when oldString is empty and file does not exist
            if (oldString.isEmpty() && !Files.exists(path)) {
                return createNewFile(path, newString);
            }

            if (!Files.exists(path)) {
                return ToolResult.failure("File not found: " + filePath);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);

            if (oldString.isEmpty()) {
                // prepend content when oldString is empty but file exists
                String updatedContent = newString + content;
                Files.writeString(path, updatedContent, StandardCharsets.UTF_8);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", "Content prepended to file");
                result.put("filePath", path.toString());
                return ToolResult.success(result);
            }

            // perform the replacement based on replaceAll flag
            String updatedContent;
            if (replaceAll) {
                if (!content.contains(oldString)) {
                    return ToolResult.failure("old_string not found in file. "
                            + "Make sure the string matches exactly, including whitespace and indentation.");
                }
                int occurrences = countOccurrences(content, oldString);
                updatedContent = content.replace(oldString, newString);
                
                Files.writeString(path, updatedContent, StandardCharsets.UTF_8);
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", "Replaced " + occurrences + " occurrence(s) in file");
                result.put("filePath", path.toString());
                return ToolResult.success(result);
            } else {
                // verify old_string exists in the file (single replacement mode)
                int matchIndex = content.indexOf(oldString);
                if (matchIndex < 0) {
                    return ToolResult.failure("old_string not found in file. "
                            + "Make sure the string matches exactly, including whitespace and indentation.");
                }

                // check for multiple occurrences in single replacement mode
                int secondMatch = content.indexOf(oldString, matchIndex + oldString.length());
                if (secondMatch >= 0) {
                    int occurrences = countOccurrences(content, oldString);
                    return ToolResult.failure("old_string found " + occurrences
                            + " times in the file. It must be unique for single replacement mode. "
                            + "Please provide more context to make the match unique, or set replaceAll=true to replace all occurrences.");
                }

                // perform the replacement
                updatedContent = content.substring(0, matchIndex)
                        + newString
                        + content.substring(matchIndex + oldString.length());

                Files.writeString(path, updatedContent, StandardCharsets.UTF_8);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("message", "File edited successfully");
                result.put("filePath", path.toString());
                return ToolResult.success(result);
            }

        } catch (IOException e) {
            log.error("Error editing file: {}", filePath, e);
            return ToolResult.failure("Failed to edit file: " + e.getMessage());
        }
    }

    private ToolResult editByLineRange(Path path, int startLine, int endLine, String newString) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String[] lines = content.split("\n", -1); // -1 to keep trailing empty strings
        
        // Validate line numbers
        if (startLine < 1 || startLine > lines.length) {
            return ToolResult.failure("Invalid startLine: " + startLine + ". File has " + lines.length + " lines.");
        }
        if (endLine < startLine || endLine > lines.length) {
            return ToolResult.failure("Invalid endLine: " + endLine + ". Must be between startLine (" + startLine + ") and file length (" + lines.length + ").");
        }
        
        // Build new content with the line range replaced
        StringBuilder newContent = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1; // Convert to 1-based
            if (lineNum >= startLine && lineNum <= endLine) {
                if (lineNum == startLine) {
                    newContent.append(newString);
                }
                // Skip lines in the range (already replaced)
            } else {
                newContent.append(lines[i]);
            }
            // Add newline after each line (including the last one to preserve original format)
            newContent.append("\n");
        }
        
        // Remove the trailing newline if the original file didn't have one
        if (!content.endsWith("\n") && newContent.length() > 0) {
            newContent.setLength(newContent.length() - 1);
        }
        
        Files.writeString(path, newContent.toString(), StandardCharsets.UTF_8);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Replaced lines " + startLine + " to " + endLine);
        result.put("filePath", path.toString());
        return ToolResult.success(result);
    }

    private ToolResult createNewFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "New file created");
        result.put("filePath", path.toString());
        return ToolResult.success(result);
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }

    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(path).normalize();
        }
        return path;
    }
}