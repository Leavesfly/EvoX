package io.leavesfly.evox.claudecode.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 项目上下文
 * 维护当前项目的元信息，包括项目类型、目录结构、项目规则等
 */
@Slf4j
@Data
public class ProjectContext {

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
            fileTypeDistribution.forEach((ext, count) -> typeEntries.add(ext + "(" + count + ")"));
            summary.append(String.join(", ", typeEntries)).append("\n");
        }

        if (directoryTree != null && !directoryTree.isBlank()) {
            summary.append("\n**Directory Structure**:\n```\n");
            // truncate if too long
            String tree = directoryTree;
            if (tree.length() > 3000) {
                tree = tree.substring(0, 3000) + "\n... (truncated)";
            }
            summary.append(tree).append("\n```\n");
        }

        if (projectRules != null && !projectRules.isBlank()) {
            summary.append("\n## Project Rules\n\n");
            summary.append(projectRules).append("\n");
        }

        return summary.toString();
    }
}
