package io.leavesfly.evox.claudecode.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * 项目上下文
 * 自动扫描项目目录，识别项目类型、关键文件、目录结构和文件类型分布
 * 生成上下文摘要注入到 LLM 系统提示词中
 */
@Slf4j
@Data
public class ProjectContext {

    private static final int MAX_DIRECTORY_DEPTH = 3;
    private static final int MAX_TREE_LENGTH = 3000;
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".svn", ".hg", "node_modules", "target", "build", "dist",
            ".idea", ".vscode", ".gradle", "__pycache__", ".tox", "venv", ".venv",
            "out", "bin", ".settings", ".classpath", ".project"
    );

    private String workingDirectory;
    private List<String> projectTypes;
    private String directoryTree;
    private List<String> keyFiles;
    private String projectRules;
    private Map<String, Integer> fileTypeDistribution;
    private boolean gitRepository;

    public ProjectContext(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.projectTypes = new ArrayList<>();
        this.keyFiles = new ArrayList<>();
        this.fileTypeDistribution = new LinkedHashMap<>();
    }

    /**
     * 自动扫描项目目录，识别项目类型、关键文件和目录结构
     */
    public void scanProject() {
        Path rootPath = Paths.get(workingDirectory);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            log.warn("Working directory does not exist or is not a directory: {}", workingDirectory);
            return;
        }

        detectGitRepository(rootPath);
        detectProjectTypes(rootPath);
        detectKeyFiles(rootPath);
        buildDirectoryTree(rootPath);
        countFileTypes(rootPath);

        log.info("Project scan complete: types={}, keyFiles={}, git={}",
                projectTypes, keyFiles.size(), gitRepository);
    }

    /**
     * 加载项目规则文件（如 CLAUDE.md）
     */
    public void loadProjectRules(String rulesFileName) {
        Path rulesPath = Paths.get(workingDirectory, rulesFileName);
        if (Files.exists(rulesPath)) {
            try {
                this.projectRules = Files.readString(rulesPath, StandardCharsets.UTF_8);
                log.info("Loaded project rules from {}", rulesFileName);
            } catch (IOException e) {
                log.warn("Failed to read project rules file: {}", rulesFileName, e);
            }
        }
    }

    /**
     * 生成项目上下文摘要，用于注入到系统提示词中
     */
    public String toContextSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("## Project Context\n\n");
        summary.append("**Working Directory**: ").append(workingDirectory).append("\n");

        if (!projectTypes.isEmpty()) {
            summary.append("**Project Type**: ").append(String.join(", ", projectTypes)).append("\n");
        }

        if (gitRepository) {
            summary.append("**Git Repository**: Yes\n");
        }

        if (!keyFiles.isEmpty()) {
            summary.append("**Key Files**: ").append(String.join(", ", keyFiles)).append("\n");
        }

        if (!fileTypeDistribution.isEmpty()) {
            summary.append("**File Types**: ");
            List<String> typeEntries = new ArrayList<>();
            fileTypeDistribution.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(15)
                    .forEach(entry -> typeEntries.add(entry.getKey() + "(" + entry.getValue() + ")"));
            summary.append(String.join(", ", typeEntries)).append("\n");
        }

        if (directoryTree != null && !directoryTree.isBlank()) {
            summary.append("\n**Directory Structure**:\n```\n");
            String tree = directoryTree;
            if (tree.length() > MAX_TREE_LENGTH) {
                tree = tree.substring(0, MAX_TREE_LENGTH) + "\n... (truncated)";
            }
            summary.append(tree).append("\n```\n");
        }

        if (projectRules != null && !projectRules.isBlank()) {
            summary.append("\n## Project Rules\n\n");
            summary.append(projectRules).append("\n");
        }

        return summary.toString();
    }

    // ==================== Scanning Methods ====================

    private void detectGitRepository(Path rootPath) {
        gitRepository = Files.exists(rootPath.resolve(".git"));
    }

    private void detectProjectTypes(Path rootPath) {
        projectTypes.clear();

        // Java / Maven
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            projectTypes.add("Java/Maven");
        }
        // Java / Gradle
        if (Files.exists(rootPath.resolve("build.gradle")) || Files.exists(rootPath.resolve("build.gradle.kts"))) {
            projectTypes.add("Java/Gradle");
        }
        // Node.js
        if (Files.exists(rootPath.resolve("package.json"))) {
            projectTypes.add("Node.js");
        }
        // Python
        if (Files.exists(rootPath.resolve("setup.py")) || Files.exists(rootPath.resolve("pyproject.toml"))
                || Files.exists(rootPath.resolve("requirements.txt"))) {
            projectTypes.add("Python");
        }
        // Go
        if (Files.exists(rootPath.resolve("go.mod"))) {
            projectTypes.add("Go");
        }
        // Rust
        if (Files.exists(rootPath.resolve("Cargo.toml"))) {
            projectTypes.add("Rust");
        }
        // C/C++
        if (Files.exists(rootPath.resolve("CMakeLists.txt")) || Files.exists(rootPath.resolve("Makefile"))) {
            projectTypes.add("C/C++");
        }

        if (projectTypes.isEmpty()) {
            projectTypes.add("Unknown");
        }
    }

    private void detectKeyFiles(Path rootPath) {
        keyFiles.clear();

        List<String> candidateFiles = List.of(
                "README.md", "README.rst", "README.txt", "README",
                "pom.xml", "build.gradle", "build.gradle.kts",
                "package.json", "tsconfig.json",
                "setup.py", "pyproject.toml", "requirements.txt",
                "go.mod", "Cargo.toml", "CMakeLists.txt", "Makefile",
                "Dockerfile", "docker-compose.yml", "docker-compose.yaml",
                ".gitignore", ".env.example",
                "CLAUDE.md", "AGENTS.md"
        );

        for (String candidate : candidateFiles) {
            if (Files.exists(rootPath.resolve(candidate))) {
                keyFiles.add(candidate);
            }
        }
    }

    private void buildDirectoryTree(Path rootPath) {
        StringBuilder tree = new StringBuilder();
        tree.append(rootPath.getFileName()).append("/\n");
        buildTreeRecursive(rootPath, tree, "", 0);
        directoryTree = tree.toString();
    }

    private void buildTreeRecursive(Path directory, StringBuilder tree, String prefix, int depth) {
        if (depth >= MAX_DIRECTORY_DEPTH) {
            return;
        }

        try (Stream<Path> entries = Files.list(directory)) {
            List<Path> sortedEntries = entries
                    .filter(path -> !IGNORED_DIRECTORIES.contains(path.getFileName().toString()))
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString()))
                    .toList();

            for (int i = 0; i < sortedEntries.size(); i++) {
                Path entry = sortedEntries.get(i);
                boolean isLast = (i == sortedEntries.size() - 1);
                String connector = isLast ? "└── " : "├── ";
                String childPrefix = isLast ? "    " : "│   ";

                tree.append(prefix).append(connector).append(entry.getFileName());

                if (Files.isDirectory(entry)) {
                    tree.append("/\n");
                    buildTreeRecursive(entry, tree, prefix + childPrefix, depth + 1);
                } else {
                    tree.append("\n");
                }

                // prevent tree from getting too large
                if (tree.length() > MAX_TREE_LENGTH + 500) {
                    return;
                }
            }
        } catch (IOException e) {
            log.debug("Failed to list directory: {}", directory, e);
        }
    }

    private void countFileTypes(Path rootPath) {
        fileTypeDistribution.clear();

        try {
            Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), 10,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            String dirName = dir.getFileName().toString();
                            if (IGNORED_DIRECTORIES.contains(dirName) && !dir.equals(rootPath)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            String fileName = file.getFileName().toString();
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                String extension = fileName.substring(dotIndex);
                                fileTypeDistribution.merge(extension, 1, Integer::sum);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            log.debug("Failed to count file types", e);
        }
    }
}
