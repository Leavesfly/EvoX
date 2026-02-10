package io.leavesfly.evox.claudecode.mcp;

import io.leavesfly.evox.mcp.MCPProtocol;
import io.leavesfly.evox.mcp.MCPTool;
import io.leavesfly.evox.mcp.runtime.MCPClient;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 工具桥接器
 * 将 MCP Server 暴露的工具转换为 BaseTool 实例，使其可以被 ToolRegistry 统一管理。
 * 工具调用通过 MCPClient 代理到远程/本地 MCP Server 执行。
 */
@Slf4j
public class MCPToolBridge extends BaseTool {

    private final MCPClient mcpClient;
    private final String serverName;

    public MCPToolBridge(MCPClient mcpClient, MCPTool mcpTool, String serverName) {
        this.mcpClient = mcpClient;
        this.serverName = serverName;

        // prefix tool name with server name to avoid collisions
        this.name = "mcp_" + serverName + "_" + mcpTool.getName();
        this.description = "[MCP:" + serverName + "] " + mcpTool.getDescription();

        // convert MCP parameter schema to BaseTool format
        this.inputs = convertInputSchema(mcpTool.getInputSchema());
        this.required = mcpTool.getInputSchema() != null && mcpTool.getInputSchema().getRequired() != null
                ? mcpTool.getInputSchema().getRequired()
                : List.of();
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);

            // extract the original tool name (without mcp_ prefix)
            String originalToolName = name.substring(("mcp_" + serverName + "_").length());

            log.debug("Calling MCP tool '{}' on server '{}' with params: {}",
                    originalToolName, serverName, parameters);

            MCPProtocol.ToolCallResult mcpResult = mcpClient.callTool(originalToolName, parameters);

            if (mcpResult.isError()) {
                String errorText = extractTextFromContent(mcpResult.getContent());
                return ToolResult.failure("MCP tool error: " + errorText);
            }

            String resultText = extractTextFromContent(mcpResult.getContent());
            return ToolResult.success(resultText);

        } catch (Exception e) {
            log.error("Failed to execute MCP tool '{}' on server '{}'", name, serverName, e);
            return ToolResult.failure("MCP tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 从 MCP Content 列表中提取文本内容
     */
    private String extractTextFromContent(List<MCPProtocol.Content> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return "(no content)";
        }

        return contentList.stream()
                .filter(content -> "text".equals(content.getType()) || content.getText() != null)
                .map(MCPProtocol.Content::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 将 MCP ParameterSchema 转换为 BaseTool 的 inputs 格式
     */
    private Map<String, Map<String, String>> convertInputSchema(MCPTool.ParameterSchema schema) {
        if (schema == null || schema.getProperties() == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Map<String, String>> inputs = new LinkedHashMap<>();
        schema.getProperties().forEach((paramName, propDef) -> {
            Map<String, String> paramInfo = new LinkedHashMap<>();
            paramInfo.put("type", propDef.getType() != null ? propDef.getType() : "string");
            paramInfo.put("description", propDef.getDescription() != null ? propDef.getDescription() : "");
            inputs.put(paramName, paramInfo);
        });

        return inputs;
    }
}
