package io.leavesfly.evox.tools.project;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目上下文工具
 * 扫描项目结构，识别项目类型、依赖管理文件、目录树等信息
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectContextTool extends BaseTool {

    private String workingDirectory;
    private int maxDepth;
    private int maxFiles;
    private Set<String> excludedDirectories;

    public ProjectContextTool() {
        this(System.getProperty("user.dir"));
    }

    public ProjectContextTool(String workingDirectory) {
        super();
        this.name = "project_context";
        this.description = "Scan and analyze project structure, detect project type, "
                + "list directory tree, and read project configuration files.";
        this.workingDirectory = workingDirectory;
        this.maxDepth = 4;
        this.maxFiles = 500;
        this.excludedDirectories = new HashSet<>(Arrays.asList(
                ".git", "node_modules", "target", "build", ".idea", ".vscode",
                "__pycache__", ".gradle", "dist", "out", ".svn", "vendor"
        ));

        this.inputs = new HashMap<>();
        this.required = new ArrayList<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: tree (directory tree), detect (project type detection), "
                + "summary (project summary including type, structure, and key files)");
        this.inputs.put("operation", operationParam);
        this.required.add("operation");

        Map<String, String> pathParam = new HashMap<>();
        pathParam.put("type", "string");
        pathParam.put("description", "Directory path to analyze (optional, defaults to project root)");
        this.inputs.put("path", pathParam);

        Map<String, String> depthParam = new HashMap<>();
        depthParam.put("type", "integer");
        depthParam.put("description", "Maximum depth for directory tree (optional, defaults to 4)");
        this.inputs.put("depth", depthParam);
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        validateParameters(parameters);
        String operation = getParameter(parameters, "operation", "");
        String path = getParameter(parameters, "path", workingDirectory);
        Number depthValue = getParameter(parameters, "depth", null);
        int depth = depthValue != null ? depthValue.intValue() : maxDepth;

        Path rootPath = Paths.get(path);
        if (!rootPath.isAbsolute()) {
            rootPath = Paths.get(workingDirectory).resolve(rootPath);
        }

        if (!Files.isDirectory(rootPath)) {
            return ToolResult.failure("Path is not a directory: " + path);
        }

        return switch (operation) {
            case "tree" -> generateDirectoryTree(rootPath, depth);
            case "detect" -> detectProjectType(rootPath);
            case "summary" -> generateProjectSummary(rootPath, depth);
            default -> ToolResult.failure("Unknown operation: " + operation
                    + ". Supported: tree, detect, summary");
        };
    }

    private ToolResult generateDirectoryTree(Path rootPath, int depth) {
        try {
            StringBuilder tree = new StringBuilder();
            tree.append(rootPath.getFileName().toString()).append("/\n");
            buildTree(rootPath, "", depth, 0, tree);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tree", tree.toString());
            result.put("rootPath", rootPath.toString());
            return ToolResult.success(result);
        } catch (IOException e) {
            log.error("Error generating directory tree", e);
            return ToolResult.failure("Failed to generate directory tree: " + e.getMessage());
        }
    }

    private void buildTree(Path dir, String prefix, int maxDepth, int currentDepth,
                           StringBuilder tree) throws IOException {
        if (currentDepth >= maxDepth) {
            return;
        }

        List<Path> entries;
        try (var stream = Files.list(dir)) {
            entries = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        if (Files.isDirectory(p) && excludedDirectories.contains(name)) {
                            return false;
                        }
                        return !name.startsWith(".");
                    })
                    .sorted((a, b) -> {
                        boolean aIsDir = Files.isDirectory(a);
                        boolean bIsDir = Files.isDirectory(b);
                        if (aIsDir != bIsDir) {
                            return aIsDir ? -1 : 1;
                        }
                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    })
                    .collect(Collectors.toList());
        }

        for (int i = 0; i < entries.size(); i++) {
            Path entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            String connector = isLast ? "└── " : "├── ";
            String childPrefix = isLast ? "    " : "│   ";

            String name = entry.getFileName().toString();
            if (Files.isDirectory(entry)) {
                tree.append(prefix).append(connector).append(name).append("/\n");
                buildTree(entry, prefix + childPrefix, maxDepth, currentDepth + 1, tree);
            } else {
                tree.append(prefix).append(connector).append(name).append("\n");
            }
        }
    }

    private ToolResult detectProjectType(Path rootPath) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> detectedTypes = new ArrayList<>();
        Map<String, String> configFiles = new LinkedHashMap<>();

        // Java / Maven
        Path pomXml = rootPath.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            detectedTypes.add("Java/Maven");
            configFiles.put("pom.xml", readFileHead(pomXml, 50));
        }

        // Java / Gradle
        Path buildGradle = rootPath.resolve("build.gradle");
        Path buildGradleKts = rootPath.resolve("build.gradle.kts");
        if (Files.exists(buildGradle)) {
            detectedTypes.add("Java/Gradle");
            configFiles.put("build.gradle", readFileHead(buildGradle, 30));
        } else if (Files.exists(buildGradleKts)) {
            detectedTypes.add("Kotlin/Gradle");
            configFiles.put("build.gradle.kts", readFileHead(buildGradleKts, 30));
        }

        // Node.js / npm
        Path packageJson = rootPath.resolve("package.json");
        if (Files.exists(packageJson)) {
            detectedTypes.add("Node.js/npm");
            configFiles.put("package.json", readFileHead(packageJson, 30));
        }

        // Python
        Path requirementsTxt = rootPath.resolve("requirements.txt");
        Path pyprojectToml = rootPath.resolve("pyproject.toml");
        Path setupPy = rootPath.resolve("setup.py");
        if (Files.exists(requirementsTxt)) {
            detectedTypes.add("Python/pip");
            configFiles.put("requirements.txt", readFileHead(requirementsTxt, 20));
        }
        if (Files.exists(pyprojectToml)) {
            detectedTypes.add("Python/pyproject");
            configFiles.put("pyproject.toml", readFileHead(pyprojectToml, 30));
        }
        if (Files.exists(setupPy)) {
            detectedTypes.add("Python/setuptools");
        }

        // Go
        Path goMod = rootPath.resolve("go.mod");
        if (Files.exists(goMod)) {
            detectedTypes.add("Go");
            configFiles.put("go.mod", readFileHead(goMod, 20));
        }

        // Rust
        Path cargoToml = rootPath.resolve("Cargo.toml");
        if (Files.exists(cargoToml)) {
            detectedTypes.add("Rust/Cargo");
            configFiles.put("Cargo.toml", readFileHead(cargoToml, 20));
        }

        // Docker
        Path dockerfile = rootPath.resolve("Dockerfile");
        if (Files.exists(dockerfile)) {
            detectedTypes.add("Docker");
        }

        // Git
        Path gitDir = rootPath.resolve(".git");
        if (Files.isDirectory(gitDir)) {
            result.put("gitRepository", true);
        }

        if (detectedTypes.isEmpty()) {
            detectedTypes.add("Unknown");
        }

        result.put("projectTypes", detectedTypes);
        result.put("configFiles", configFiles);
        result.put("rootPath", rootPath.toString());

        return ToolResult.success(result);
    }

    private ToolResult generateProjectSummary(Path rootPath, int depth) {
        Map<String, Object> summary = new LinkedHashMap<>();

        // project type detection
        ToolResult typeResult = detectProjectType(rootPath);
        if (typeResult.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typeData = (Map<String, Object>) typeResult.getData();
            summary.put("projectTypes", typeData.get("projectTypes"));
            summary.put("gitRepository", typeData.getOrDefault("gitRepository", false));
        }

        // directory tree
        ToolResult treeResult = generateDirectoryTree(rootPath, depth);
        if (treeResult.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> treeData = (Map<String, Object>) treeResult.getData();
            summary.put("directoryTree", treeData.get("tree"));
        }

        // key files
        List<String> keyFiles = new ArrayList<>();
        String[] importantFiles = {
                "README.md", "README.rst", "README.txt",
                "pom.xml", "build.gradle", "build.gradle.kts",
                "package.json", "tsconfig.json",
                "requirements.txt", "pyproject.toml", "setup.py",
                "go.mod", "Cargo.toml",
                "Dockerfile", "docker-compose.yml", "docker-compose.yaml",
                ".gitignore", "Makefile",
                "CLAUDE.md", ".cursorrules"
        };
        for (String fileName : importantFiles) {
            if (Files.exists(rootPath.resolve(fileName))) {
                keyFiles.add(fileName);
            }
        }
        summary.put("keyFiles", keyFiles);

        // file statistics
        try {
            Map<String, Integer> extensionCounts = new LinkedHashMap<>();
            int[] totalFiles = {0};
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName().toString();
                    if (excludedDirectories.contains(dirName) || dirName.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalFiles[0]++;
                    String fileName = file.getFileName().toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        String ext = fileName.substring(dotIndex);
                        extensionCounts.merge(ext, 1, Integer::sum);
                    }
                    if (totalFiles[0] >= maxFiles) {
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            summary.put("totalFiles", totalFiles[0]);

            // sort by count descending, take top 10
            List<Map.Entry<String, Integer>> sortedExtensions = extensionCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            Map<String, Integer> topExtensions = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : sortedExtensions) {
                topExtensions.put(entry.getKey(), entry.getValue());
            }
            summary.put("fileTypeDistribution", topExtensions);

        } catch (IOException e) {
            log.warn("Error counting files", e);
        }

        summary.put("rootPath", rootPath.toString());
        return ToolResult.success(summary);
    }

    private String readFileHead(Path file, int maxLines) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int linesToRead = Math.min(lines.size(), maxLines);
            return String.join("\n", lines.subList(0, linesToRead));
        } catch (IOException e) {
            return "(unable to read file)";
        }
    }
}
