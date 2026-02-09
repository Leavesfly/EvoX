package io.leavesfly.evox.cowork.connector;

import java.util.List;
import java.util.Map;

/**
 * 连接器接口
 * 定义外部系统集成的标准契约
 */
public interface Connector {
    
    String getId();
    
    String getName();
    
    String getDescription();
    
    ConnectorType getType();
    
    boolean isConnected();
    
    // 建立连接
    void connect(Map<String, String> credentials);
    
    // 断开连接
    void disconnect();
    
    // 执行操作
    Map<String, Object> execute(String action, Map<String, Object> parameters);
    
    // 获取支持的操作列表
    List<String> getSupportedActions();
    
    enum ConnectorType {
        CLOUD_STORAGE,      // 云存储
        COMMUNICATION,      // 通讯工具
        PROJECT_MANAGEMENT, // 项目管理
        CRM,               // 客户关系管理
        BROWSER,           // 浏览器
        CUSTOM             // 自定义
    }
}