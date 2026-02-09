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
 * 支持创建新文件、插入内容和多处替换
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
                + "Can also create new files. The old_string must match exactly (including whitespace and indentation).";
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
        oldStringParam.put("description", "The exact string to find and replace. Empty string means create a new file.");
        this.inputs.put("oldString", oldStringParam);
        this.required.add("oldString");

        Map<String, String> newStringParam = new HashMap<>();
        newStringParam.put("type", "string");
        newStringParam.put("description", "The replacement string. Empty string means delete the old_string.");
        this.inputs.put("newString", newStringParam);
        this.required.add("newString");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String filePath = getParameter(parameters, "filePath", "");
        String oldString = getParameter(parameters, "oldString", "");
        String newString = getParameter(parameters, "newString", "");

        if (filePath.isBlank()) {
            return ToolResult.failure("File path cannot be empty");
        }

        try {
            Path path = resolvePath(filePath);

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

            // verify old_string exists in the file
            int matchIndex = content.indexOf(oldString);
            if (matchIndex < 0) {
                return ToolResult.failure("old_string not found in file. "
                        + "Make sure the string matches exactly, including whitespace and indentation.");
            }

            // check for multiple occurrences
            int secondMatch = content.indexOf(oldString, matchIndex + oldString.length());
            if (secondMatch >= 0) {
                int occurrences = countOccurrences(content, oldString);
                return ToolResult.failure("old_string found " + occurrences
                        + " times in the file. It must be unique. "
                        + "Please provide more context to make the match unique.");
            }

            // perform the replacement
            String updatedContent = content.substring(0, matchIndex)
                    + newString
                    + content.substring(matchIndex + oldString.length());

            Files.writeString(path, updatedContent, StandardCharsets.UTF_8);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "File edited successfully");
            result.put("filePath", path.toString());
            return ToolResult.success(result);

        } catch (IOException e) {
            log.error("Error editing file: {}", filePath, e);
            return ToolResult.failure("Failed to edit file: " + e.getMessage());
        }
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
