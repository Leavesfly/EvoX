package io.leavesfly.evox.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP资源定义
 * 表示可供访问的资源（文件、数据、API等）
 *
 * @author EvoX Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPResource {

    /**
     * 资源URI
     */
    private String uri;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源描述
     */
    private String description;

    /**
     * 资源类型
     */
    private ResourceType type;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 资源元数据
     */
    private Map<String, Object> metadata;

    /**
     * 资源标签
     */
    private List<String> tags;

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        FILE,
        DIRECTORY,
        URL,
        DATABASE,
        API,
        CUSTOM
    }
}
