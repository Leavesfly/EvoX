package io.leavesfly.evox.cowork.connector;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 连接器基类
 * 提供连接器的通用属性和基础实现
 */
@Slf4j
@Data
public abstract class BaseConnector implements Connector {
    
    private final String id;
    private final String name;
    private final String description;
    private final ConnectorType type;
    
    private boolean connected;
    private Map<String, String> credentials;
    private Map<String, Object> config;
    
    public BaseConnector(String id, String name, String description, ConnectorType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.connected = false;
    }
    
    @Override
    public void connect(Map<String, String> credentials) {
        this.credentials = credentials;
        try {
            doConnect();
            this.connected = true;
            log.info("Connector '{}' connected successfully", id);
        } catch (Exception e) {
            log.error("Failed to connect connector '{}'", id, e);
            throw new RuntimeException("Failed to connect: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void disconnect() {
        try {
            doDisconnect();
            this.connected = false;
            this.credentials = null;
            log.info("Connector '{}' disconnected successfully", id);
        } catch (Exception e) {
            log.error("Failed to disconnect connector '{}'", id, e);
            throw new RuntimeException("Failed to disconnect: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> execute(String action, Map<String, Object> parameters) {
        if (!isConnected()) {
            throw new IllegalStateException("Connector '" + id + "' is not connected");
        }
        
        List<String> supportedActions = getSupportedActions();
        if (!supportedActions.contains(action)) {
            throw new UnsupportedOperationException(
                "Action '" + action + "' is not supported. Supported actions: " + supportedActions
            );
        }
        
        return doExecute(action, parameters);
    }
    
    // 执行具体的连接逻辑
    protected abstract void doConnect() throws Exception;
    
    // 执行具体的断开连接逻辑
    protected abstract void doDisconnect();
    
    // 执行具体的操作
    protected abstract Map<String, Object> doExecute(String action, Map<String, Object> parameters);
}