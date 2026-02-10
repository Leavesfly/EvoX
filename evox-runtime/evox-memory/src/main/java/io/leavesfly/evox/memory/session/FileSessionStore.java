package io.leavesfly.evox.memory.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 基于文件的会话存储实现
 * 每个会话保存为一个 JSON 文件
 */
public class FileSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(FileSessionStore.class);
    private static final String DEFAULT_STORAGE_DIRECTORY = System.getProperty("user.home") + "/.evox/sessions/";

    private final String storageDirectory;
    private final ObjectMapper objectMapper;

    /**
     * 使用默认存储目录构造
     */
    public FileSessionStore() {
        this(DEFAULT_STORAGE_DIRECTORY);
    }

    /**
     * 使用指定的存储目录构造
     *
     * @param storageDirectory 存储目录路径
     */
    public FileSessionStore(String storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.objectMapper = createObjectMapper();
        ensureStorageDirectoryExists();
    }

    /**
     * 创建并配置 ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * 确保存储目录存在
     */
    private void ensureStorageDirectoryExists() {
        try {
            Path path = Paths.get(storageDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created session storage directory: {}", storageDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create storage directory: {}", storageDirectory, e);
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    /**
     * 获取会话文件的完整路径
     */
    private String getSessionFilePath(String sessionId) {
        return storageDirectory + sessionId + ".json";
    }

    @Override
    public String saveSession(String sessionId, SessionData data) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Session ID cannot be null or empty");
            }
            if (data == null) {
                throw new IllegalArgumentException("Session data cannot be null");
            }

            data.setSessionId(sessionId);
            Instant now = Instant.now();
            if (data.getCreatedAt() == null) {
                data.setCreatedAt(now);
            }
            data.setUpdatedAt(now);

            String filePath = getSessionFilePath(sessionId);
            String content = objectMapper.writeValueAsString(data);
            Files.write(Paths.get(filePath), content.getBytes());

            logger.debug("Saved session: {} to {}", sessionId, filePath);
            return sessionId;
        } catch (IOException e) {
            logger.error("Failed to save session: {}", sessionId, e);
            throw new RuntimeException("Failed to save session: " + sessionId, e);
        }
    }

    @Override
    public SessionData loadSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                logger.warn("Attempted to load session with null or empty ID");
                return null;
            }

            String filePath = getSessionFilePath(sessionId);
            File file = new File(filePath);
            if (!file.exists()) {
                logger.debug("Session file not found: {}", filePath);
                return null;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            SessionData data = objectMapper.readValue(content, SessionData.class);

            logger.debug("Loaded session: {} from {}", sessionId, filePath);
            return data;
        } catch (IOException e) {
            logger.error("Failed to load session: {}", sessionId, e);
            throw new RuntimeException("Failed to load session: " + sessionId, e);
        }
    }

    @Override
    public boolean deleteSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                logger.warn("Attempted to delete session with null or empty ID");
                return false;
            }

            String filePath = getSessionFilePath(sessionId);
            File file = new File(filePath);
            if (!file.exists()) {
                logger.debug("Session file not found for deletion: {}", filePath);
                return false;
            }

            boolean deleted = file.delete();
            if (deleted) {
                logger.debug("Deleted session: {}", sessionId);
            } else {
                logger.warn("Failed to delete session file: {}", filePath);
            }
            return deleted;
        } catch (Exception e) {
            logger.error("Error while deleting session: {}", sessionId, e);
            return false;
        }
    }

    @Override
    public List<SessionSummary> listSessions() {
        List<SessionSummary> summaries = new ArrayList<>();
        File storageDir = new File(storageDirectory);

        if (!storageDir.exists() || !storageDir.isDirectory()) {
            logger.debug("Storage directory does not exist: {}", storageDirectory);
            return summaries;
        }

        File[] jsonFiles = storageDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            logger.debug("No session files found in: {}", storageDirectory);
            return summaries;
        }

        for (File jsonFile : jsonFiles) {
            try {
                String content = new String(Files.readAllBytes(jsonFile.toPath()));
                Map<String, Object> sessionMap = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

                String sessionId = extractString(sessionMap, "sessionId");
                String projectDirectory = extractString(sessionMap, "projectDirectory");
                Instant createdAt = extractInstant(sessionMap, "createdAt");
                Instant updatedAt = extractInstant(sessionMap, "updatedAt");

                int messageCount = extractMessageCount(sessionMap);
                String lastUserMessage = extractLastUserMessage(sessionMap);

                SessionSummary summary = SessionSummary.builder()
                        .sessionId(sessionId)
                        .projectDirectory(projectDirectory)
                        .messageCount(messageCount)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .lastUserMessage(lastUserMessage)
                        .build();

                summaries.add(summary);
            } catch (Exception e) {
                logger.error("Failed to parse session summary from file: {}", jsonFile.getName(), e);
            }
        }

        summaries.sort(Comparator.comparing(SessionSummary::getUpdatedAt).reversed());
        logger.debug("Listed {} sessions", summaries.size());
        return summaries;
    }

    @Override
    public boolean sessionExists(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        String filePath = getSessionFilePath(sessionId);
        return new File(filePath).exists();
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Instant extractInstant(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return Instant.parse((String) value);
        } else if (value instanceof Map) {
            Map<String, Object> instantMap = (Map<String, Object>) value;
            Object seconds = instantMap.get("seconds");
            Object nanos = instantMap.get("nanos");
            if (seconds instanceof Number && nanos instanceof Number) {
                return Instant.ofEpochSecond(((Number) seconds).longValue(), ((Number) nanos).intValue());
            }
        }
        return null;
    }

    private int extractMessageCount(Map<String, Object> map) {
        Object messages = map.get("messages");
        if (messages instanceof List) {
            return ((List<?>) messages).size();
        }
        return 0;
    }

    private String extractLastUserMessage(Map<String, Object> map) {
        Object messages = map.get("messages");
        if (messages instanceof List) {
            List<?> messageList = (List<?>) messages;
            for (int i = messageList.size() - 1; i >= 0; i--) {
                Object messageObj = messageList.get(i);
                if (messageObj instanceof Map) {
                    Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                    Object messageType = messageMap.get("messageType");
                    if ("USER".equals(messageType)) {
                        Object content = messageMap.get("content");
                        if (content != null) {
                            String contentStr = content.toString();
                            return contentStr.length() > 100 ? contentStr.substring(0, 100) + "..." : contentStr;
                        }
                    }
                }
            }
        }
        return null;
    }
}
