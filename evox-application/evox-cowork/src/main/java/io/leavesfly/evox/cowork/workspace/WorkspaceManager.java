package io.leavesfly.evox.cowork.workspace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class WorkspaceManager {
    private final Map<String, Workspace> workspaces;
    private volatile String activeWorkspaceId;
    private final String configDirectory;
    private final ObjectMapper objectMapper;

    public WorkspaceManager(String configDirectory) {
        this.workspaces = new ConcurrentHashMap<>();
        this.configDirectory = configDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        loadWorkspaces();
    }

    private void loadWorkspaces() {
        File dir = new File(configDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("Created config directory: {}", configDirectory);
            return;
        }

        File workspacesFile = new File(dir, "workspaces.json");
        if (!workspacesFile.exists()) {
            log.info("No existing workspaces file found, starting fresh");
            return;
        }

        try {
            List<Workspace> loadedWorkspaces = objectMapper.readValue(
                workspacesFile,
                new TypeReference<List<Workspace>>() {}
            );
            
            for (Workspace workspace : loadedWorkspaces) {
                workspaces.put(workspace.getWorkspaceId(), workspace);
            }
            
            log.info("Loaded {} workspaces from {}", workspaces.size(), workspacesFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load workspaces from {}: {}", workspacesFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void saveWorkspaces() {
        File dir = new File(configDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File workspacesFile = new File(dir, "workspaces.json");
        try {
            objectMapper.writeValue(workspacesFile, new ArrayList<>(workspaces.values()));
            log.info("Saved {} workspaces to {}", workspaces.size(), workspacesFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save workspaces to {}: {}", workspacesFile.getAbsolutePath(), e.getMessage());
            throw new RuntimeException("Failed to save workspaces", e);
        }
    }

    public Workspace addWorkspace(String name, String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Directory does not exist: " + directory);
        }

        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setDirectory(directory);
        
        workspaces.put(workspace.getWorkspaceId(), workspace);
        saveWorkspaces();
        
        log.info("Added workspace: {} at {}", name, directory);
        return workspace;
    }

    public void removeWorkspace(String workspaceId) {
        Workspace removed = workspaces.remove(workspaceId);
        if (removed != null) {
            if (workspaceId.equals(activeWorkspaceId)) {
                activeWorkspaceId = null;
            }
            saveWorkspaces();
            log.info("Removed workspace: {}", removed.getName());
        }
    }

    public Workspace getWorkspace(String workspaceId) {
        return workspaces.get(workspaceId);
    }

    public List<Workspace> getAllWorkspaces() {
        return workspaces.values().stream()
                .sorted(Comparator.comparing(Workspace::isPinned).reversed()
                        .thenComparing(Workspace::getLastAccessedAt).reversed())
                .collect(Collectors.toList());
    }

    public Workspace getActiveWorkspace() {
        if (activeWorkspaceId == null) {
            return null;
        }
        return workspaces.get(activeWorkspaceId);
    }

    public Workspace switchWorkspace(String workspaceId) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace not found: " + workspaceId);
        }

        this.activeWorkspaceId = workspaceId;
        workspace.touch();
        saveWorkspaces();
        
        log.info("Switched to workspace: {}", workspace.getName());
        return workspace;
    }

    public List<Workspace> getRecentWorkspaces(int limit) {
        return workspaces.values().stream()
                .sorted(Comparator.comparing(Workspace::getLastAccessedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void pinWorkspace(String workspaceId) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace != null) {
            workspace.setPinned(true);
            saveWorkspaces();
            log.info("Pinned workspace: {}", workspace.getName());
        }
    }

    public void unpinWorkspace(String workspaceId) {
        Workspace workspace = workspaces.get(workspaceId);
        if (workspace != null) {
            workspace.setPinned(false);
            saveWorkspaces();
            log.info("Unpinned workspace: {}", workspace.getName());
        }
    }
}
