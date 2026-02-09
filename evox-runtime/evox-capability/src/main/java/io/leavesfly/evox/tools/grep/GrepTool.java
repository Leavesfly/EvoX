package io.leavesfly.evox.tools.grep;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

/**
 * 文本搜索工具（Grep）
 * 在项目文件中搜索匹配正则表达式或关键词的内容，返回匹配的文件路径和行号
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class GrepTool extends BaseTool {

    private String workingDirectory;
    private int maxResults;
    private int contextLines;
    private Set<String> excludedDirectories;
    private Set<String> binaryExtensions;

    public GrepTool() {
        this(System.getProperty("user.dir"));
    }

    public GrepTool(String workingDirectory) {
        super();
        this.name = "grep";
        this.description = "Search for text patterns (regex or plain text) in project files. "
                + "Returns matching file paths, line numbers, and content with context.";
        this.workingDirectory = workingDirectory;
        this.maxResults = 50;
        this.contextLines = 2;
        this.excludedDirectories = new HashSet<>(Arrays.asList(
                ".git", "node_modules", "target", "build", ".idea", ".vscode",
                "__pycache__", ".gradle", "dist", "out", ".svn"
        ));
        this.binaryExtensions = new HashSet<>(Arrays.asList(
                ".class", ".jar", ".war", ".ear", ".zip", ".tar", ".gz",
                ".png", ".jpg", ".jpeg", ".gif", ".ico", ".bmp", ".svg",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".exe", ".dll", ".so"
        ));

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> patternParam = new HashMap<>();
        patternParam.put("type", "string");
        patternParam.put("description", "The regex pattern or plain text to search for");
        this.inputs.put("pattern", patternParam);
        this.required.add("pattern");

        Map<String, String> pathParam = new HashMap<>();
        pathParam.put("type", "string");
        pathParam.put("description", "Directory or file path to search in (optional, defaults to project root)");
        this.inputs.put("path", pathParam);

        Map<String, String> includeParam = new HashMap<>();
        includeParam.put("type", "string");
        includeParam.put("description", "File glob pattern to include, e.g. '*.java' (optional)");
        this.inputs.put("include", includeParam);

        Map<String, String> caseSensitiveParam = new HashMap<>();
        caseSensitiveParam.put("type", "boolean");
        caseSensitiveParam.put("description", "Whether the search is case-sensitive (optional, defaults to true)");
        this.inputs.put("caseSensitive", caseSensitiveParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String pattern = getParameter(parameters, "pattern", "");
        String searchPath = getParameter(parameters, "path", workingDirectory);
        String includeGlob = getParameter(parameters, "include", null);
        Boolean caseSensitive = getParameter(parameters, "caseSensitive", true);

        if (pattern.isBlank()) {
            return ToolResult.failure("Search pattern cannot be empty");
        }

        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern compiledPattern = Pattern.compile(pattern, flags);
            Path rootPath = Paths.get(searchPath);
            if (!rootPath.isAbsolute()) {
                rootPath = Paths.get(workingDirectory).resolve(rootPath);
            }

            if (!Files.exists(rootPath)) {
                return ToolResult.failure("Search path does not exist: " + searchPath);
            }

            PathMatcher includeMatcher = includeGlob != null
                    ? FileSystems.getDefault().getPathMatcher("glob:" + includeGlob)
                    : null;

            List<Map<String, Object>> matches = new ArrayList<>();
            searchFiles(rootPath, compiledPattern, includeMatcher, matches);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalMatches", matches.size());
            result.put("truncated", matches.size() > maxResults);
            result.put("matches", matches.size() > maxResults ? matches.subList(0, maxResults) : matches);

            return ToolResult.success(result);

        } catch (PatternSyntaxException e) {
            return ToolResult.failure("Invalid regex pattern: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during grep search", e);
            return ToolResult.failure("Search failed: " + e.getMessage());
        }
    }

    private void searchFiles(Path rootPath, Pattern pattern, PathMatcher includeMatcher,
                             List<Map<String, Object>> matches) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (excludedDirectories.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matches.size() >= maxResults * 2) {
                    return FileVisitResult.TERMINATE;
                }

                String fileName = file.getFileName().toString();
                if (isBinaryFile(fileName)) {
                    return FileVisitResult.CONTINUE;
                }
                if (includeMatcher != null && !includeMatcher.matches(file.getFileName())) {
                    return FileVisitResult.CONTINUE;
                }

                searchInFile(file, rootPath, pattern, matches);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void searchInFile(Path file, Path rootPath, Pattern pattern, List<Map<String, Object>> matches) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String relativePath = rootPath.relativize(file).toString();

            for (int i = 0; i < lines.size(); i++) {
                if (pattern.matcher(lines.get(i)).find()) {
                    Map<String, Object> match = new LinkedHashMap<>();
                    match.put("file", relativePath);
                    match.put("line", i + 1);
                    match.put("content", lines.get(i).stripTrailing());

                    List<String> context = new ArrayList<>();
                    int start = Math.max(0, i - contextLines);
                    int end = Math.min(lines.size() - 1, i + contextLines);
                    for (int j = start; j <= end; j++) {
                        context.add((j + 1) + ": " + lines.get(j).stripTrailing());
                    }
                    match.put("context", context);

                    matches.add(match);
                }
            }
        } catch (IOException e) {
            // skip files that cannot be read as text (including MalformedInputException for binary files)
        }
    }

    private boolean isBinaryFile(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String extension = fileName.substring(dotIndex).toLowerCase();
        return binaryExtensions.contains(extension);
    }
}
