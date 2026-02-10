package io.leavesfly.evox.tools.grep;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 文件路径搜索工具（Glob）
 * 按文件名模式匹配搜索项目中的文件路径
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class GlobTool extends BaseTool {

    private String workingDirectory;
    private int maxResults;
    private Set<String> excludedDirectories;

    public GlobTool() {
        this(System.getProperty("user.dir"));
    }

    public GlobTool(String workingDirectory) {
        super();
        this.name = "glob";
        this.description = "Search for files by name pattern using glob syntax. "
                + "Returns matching file paths relative to the project root.";
        this.workingDirectory = workingDirectory;
        this.maxResults = 100;
        this.excludedDirectories = new HashSet<>(Arrays.asList(
                ".git", "node_modules", "target", "build", ".idea", ".vscode",
                "__pycache__", ".gradle", "dist", "out", ".svn"
        ));

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> patternParam = new HashMap<>();
        patternParam.put("type", "string");
        patternParam.put("description", "Glob pattern to match file names, e.g. '**/*.java', '**/pom.xml', 'src/**/*.ts'");
        this.inputs.put("pattern", patternParam);
        this.required.add("pattern");

        Map<String, String> pathParam = new HashMap<>();
        pathParam.put("type", "string");
        pathParam.put("description", "Directory to search in (optional, defaults to project root)");
        this.inputs.put("path", pathParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String pattern = getParameter(parameters, "pattern", "");
        String searchPath = getParameter(parameters, "path", workingDirectory);

        if (pattern.isBlank()) {
            return ToolResult.failure("Glob pattern cannot be empty");
        }

        try {
            Path rootPath = Paths.get(searchPath);
            if (!rootPath.isAbsolute()) {
                rootPath = Paths.get(workingDirectory).resolve(rootPath);
            }

            if (!Files.exists(rootPath)) {
                return ToolResult.failure("Search path does not exist: " + searchPath);
            }

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<String> matchedFiles = new ArrayList<>();
            Path finalRootPath = rootPath;

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
                    if (matchedFiles.size() >= maxResults) {
                        return FileVisitResult.TERMINATE;
                    }

                    Path relativePath = finalRootPath.relativize(file);
                    if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                        matchedFiles.add(relativePath.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            Collections.sort(matchedFiles);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalMatches", matchedFiles.size());
            result.put("truncated", matchedFiles.size() >= maxResults);
            result.put("files", matchedFiles);

            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("Error during glob search", e);
            return ToolResult.failure("Glob search failed: " + e.getMessage());
        }
    }
}
