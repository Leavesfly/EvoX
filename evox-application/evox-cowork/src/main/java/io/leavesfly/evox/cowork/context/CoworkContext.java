package io.leavesfly.evox.cowork.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class CoworkContext {
    private String workingDirectory;
    private List<String> accessibleDirectories = new ArrayList<>();
    // 文件类型分布统计
    private final Map<String, Integer> fileTypeDistribution = new ConcurrentHashMap<>();
    private final LinkedHashSet<String> recentFilesSet = new LinkedHashSet<>();
    private static final int MAX_RECENT_FILES = 50;
    private String workspaceRules;

    public CoworkContext(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.accessibleDirectories.add(workingDirectory);
    }

    // 扫描工作区文件结构和类型
    public void scanWorkspace() {
        try {
            Path workspacePath = Paths.get(workingDirectory);
            Files.walk(workspacePath, 3)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        int dotIndex = fileName.lastIndexOf('.');
                        String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "no_extension";
                        fileTypeDistribution.merge(extension, 1, Integer::sum);
                    });
        } catch (IOException e) {
            log.warn("Failed to scan workspace: {}", e.getMessage());
        }
    }

    public void addAccessibleDirectory(String dir) {
        if (!accessibleDirectories.contains(dir)) {
            accessibleDirectories.add(dir);
        }
    }

    // 检查路径是否允许访问
    public boolean isPathAccessible(String path) {
        Path normalizedPath = Paths.get(path).normalize();
        for (String accessibleDir : accessibleDirectories) {
            Path normalizedDir = Paths.get(accessibleDir).normalize();
            if (normalizedPath.startsWith(normalizedDir)) {
                return true;
            }
        }
        return false;
    }

    // 记录最近访问的文件
    public synchronized void recordFileAccess(String filePath) {
        recentFilesSet.remove(filePath);
        recentFilesSet.add(filePath);
        if (recentFilesSet.size() > MAX_RECENT_FILES) {
            recentFilesSet.remove(recentFilesSet.iterator().next());
        }
    }

    public List<String> getRecentFiles() {
        synchronized (this) {
            return new ArrayList<>(recentFilesSet);
        }
    }

    // 生成上下文摘要信息
    public String toContextSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Working Directory: ").append(workingDirectory).append("\n");
        summary.append("Accessible Directories: ").append(accessibleDirectories).append("\n");
        summary.append("File Types: ").append(fileTypeDistribution).append("\n");
        
        List<String> allRecent = getRecentFiles();
        List<String> displayRecentFiles = allRecent.size() > 10
                ? allRecent.subList(allRecent.size() - 10, allRecent.size())
                : allRecent;
        summary.append("Recent Files: ").append(displayRecentFiles).append("\n");
        
        return summary.toString();
    }

    // 加载工作区规则配置
    public void loadWorkspaceRules(String rulesFileName) {
        try {
            Path rulesPath = Paths.get(workingDirectory, rulesFileName);
            if (Files.exists(rulesPath)) {
                workspaceRules = Files.readString(rulesPath);
            }
        } catch (IOException e) {
            log.warn("Failed to load workspace rules: {}", e.getMessage());
        }
    }
}