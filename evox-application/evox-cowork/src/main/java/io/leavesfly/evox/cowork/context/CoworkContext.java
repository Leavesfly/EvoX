package io.leavesfly.evox.cowork.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class CoworkContext {
    private String workingDirectory;
    private List<String> accessibleDirectories = new ArrayList<>();
    private Map<String, Integer> fileTypeDistribution = new LinkedHashMap<>();
    private List<String> recentFiles = new ArrayList<>();
    private String workspaceRules;

    public CoworkContext(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.accessibleDirectories.add(workingDirectory);
    }

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

    public void recordFileAccess(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        if (recentFiles.size() > 50) {
            recentFiles.remove(recentFiles.size() - 1);
        }
    }

    public String toContextSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Working Directory: ").append(workingDirectory).append("\n");
        summary.append("Accessible Directories: ").append(accessibleDirectories).append("\n");
        summary.append("File Types: ").append(fileTypeDistribution).append("\n");
        
        List<String> displayRecentFiles = recentFiles.size() > 10 
                ? recentFiles.subList(0, 10) 
                : recentFiles;
        summary.append("Recent Files: ").append(displayRecentFiles).append("\n");
        
        return summary.toString();
    }

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
