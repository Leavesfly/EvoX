package io.leavesfly.evox.cowork.connector;

import io.leavesfly.evox.cowork.connector.builtin.LocalFileConnector;
import io.leavesfly.evox.cowork.connector.builtin.WebConnector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 连接器管理器
 * 负责注册、管理和执行所有连接器
 */
@Slf4j
@Data
public class ConnectorManager {
    private final Map<String, Connector> connectors;
    private final List<String> networkAllowlist;

    public ConnectorManager(List<String> networkAllowlist) {
        this.connectors = new ConcurrentHashMap<>();
        this.networkAllowlist = networkAllowlist != null ? new ArrayList<>(networkAllowlist) : new ArrayList<>();
        registerBuiltinConnectors();
    }

    private void registerBuiltinConnectors() {
        registerConnector(new LocalFileConnector());
        registerConnector(new WebConnector());
        log.info("Registered {} builtin connectors", connectors.size());
    }

    public void registerConnector(Connector connector) {
        connectors.put(connector.getId(), connector);
        log.info("Registered connector: {} ({})", connector.getName(), connector.getId());
    }

    public void unregisterConnector(String connectorId) {
        Connector connector = connectors.get(connectorId);
        if (connector != null) {
            if (connector.isConnected()) {
                connector.disconnect();
            }
            connectors.remove(connectorId);
            log.info("Unregistered connector: {}", connectorId);
        }
    }

    public Connector getConnector(String connectorId) {
        return connectors.get(connectorId);
    }

    public List<Connector> getAllConnectors() {
        return new ArrayList<>(connectors.values());
    }

    public List<Connector> getConnectedConnectors() {
        return connectors.values().stream()
                .filter(Connector::isConnected)
                .collect(Collectors.toList());
    }

    public List<Connector> getConnectorsByType(Connector.ConnectorType type) {
        return connectors.values().stream()
                .filter(connector -> type.equals(connector.getType()))
                .collect(Collectors.toList());
    }

    // 连接指定连接器
    public void connectConnector(String connectorId, Map<String, String> credentials) {
        Connector connector = connectors.get(connectorId);
        if (connector != null) {
            connector.connect(credentials);
            log.info("Connected connector: {}", connectorId);
        } else {
            throw new IllegalArgumentException("Connector not found: " + connectorId);
        }
    }

    // 断开指定连接器
    public void disconnectConnector(String connectorId) {
        Connector connector = connectors.get(connectorId);
        if (connector != null) {
            connector.disconnect();
            log.info("Disconnected connector: {}", connectorId);
        }
    }

    // 执行连接器动作
    public Map<String, Object> executeConnectorAction(String connectorId, String action, Map<String, Object> parameters) {
        Connector connector = connectors.get(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("Connector not found: " + connectorId);
        }

        if (!connector.isConnected()) {
            throw new IllegalStateException("Connector '" + connectorId + "' is not connected");
        }

        if (connector.getType() == Connector.ConnectorType.BROWSER && parameters != null && parameters.containsKey("url")) {
            String url = (String) parameters.get("url");
            if (!isNetworkAllowed(url)) {
                throw new SecurityException("URL not allowed: " + url);
            }
        }

        return connector.execute(action, parameters);
    }

    // 检查网络请求是否允许
    public boolean isNetworkAllowed(String url) {
        if (networkAllowlist == null || networkAllowlist.isEmpty()) {
            return true;
        }

        return networkAllowlist.stream().anyMatch(url::matches);
    }

    // 生成连接器描述（用于 LLM 上下文）
    public String generateConnectorDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Connectors:\n\n");
        
        for (Connector connector : connectors.values()) {
            sb.append(String.format("- %s (%s): %s\n", 
                    connector.getName(), 
                    connector.getId(), 
                    connector.getDescription()));
            sb.append(String.format("  Type: %s\n", connector.getType()));
            sb.append(String.format("  Connected: %s\n", connector.isConnected()));
            sb.append("  Supported Actions:\n");
            for (String action : connector.getSupportedActions()) {
                sb.append(String.format("    - %s\n", action));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}