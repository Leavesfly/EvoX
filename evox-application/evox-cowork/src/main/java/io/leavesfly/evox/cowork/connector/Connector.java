package io.leavesfly.evox.cowork.connector;

import java.util.List;
import java.util.Map;

public interface Connector {
    
    String getId();
    
    String getName();
    
    String getDescription();
    
    ConnectorType getType();
    
    boolean isConnected();
    
    void connect(Map<String, String> credentials);
    
    void disconnect();
    
    Map<String, Object> execute(String action, Map<String, Object> parameters);
    
    List<String> getSupportedActions();
    
    enum ConnectorType {
        CLOUD_STORAGE,
        COMMUNICATION,
        PROJECT_MANAGEMENT,
        CRM,
        BROWSER,
        CUSTOM
    }
}
