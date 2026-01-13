package io.leavesfly.evox.tools.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JSON 工具 - 提供 JSON 解析、格式化、查询等功能
 * 对应 Python 版本中的 JSON 处理能力
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class JsonTool extends BaseTool {

    private ObjectMapper objectMapper;

    public JsonTool() {
        this.name = "json_tool";
        this.description = "Parse, format, and query JSON data. Supports JSON validation, pretty printing, and JSONPath queries.";
        
        // 初始化输入参数定义
        this.inputs = new HashMap<>();
        
        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation to perform: parse, format, validate, query, extract");
        this.inputs.put("operation", operationParam);
        
        Map<String, String> dataParam = new HashMap<>();
        dataParam.put("type", "string");
        dataParam.put("description", "JSON data as string");
        this.inputs.put("data", dataParam);
        
        Map<String, String> pathParam = new HashMap<>();
        pathParam.put("type", "string");
        pathParam.put("description", "JSONPath expression for query operation (e.g., '$.users[0].name')");
        this.inputs.put("path", pathParam);
        
        Map<String, String> indentParam = new HashMap<>();
        indentParam.put("type", "integer");
        indentParam.put("description", "Indentation spaces for format operation (default: 2)");
        this.inputs.put("indent", indentParam);
        
        this.required = List.of("operation", "data");
        
        // 初始化 ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        log.info("JSON tool initialized");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String operation = getParameter(parameters, "operation", "");
            String data = getParameter(parameters, "data", "");
            String path = getParameter(parameters, "path", "");
            Integer indent = getParameter(parameters, "indent", 2);
            
            if (data.trim().isEmpty()) {
                return ToolResult.failure("Data cannot be empty");
            }
            
            return switch (operation.toLowerCase()) {
                case "parse" -> parseJson(data);
                case "format" -> formatJson(data, indent);
                case "validate" -> validateJson(data);
                case "query" -> queryJson(data, path);
                case "extract" -> extractField(data, path);
                default -> ToolResult.failure("Unknown operation: " + operation);
            };
            
        } catch (Exception e) {
            log.error("JSON tool execution failed: {}", e.getMessage());
            return ToolResult.failure("JSON error: " + e.getMessage());
        }
    }

    /**
     * 解析 JSON 字符串
     */
    private ToolResult parseJson(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            Map<String, Object> result = new HashMap<>();
            result.put("parsed", true);
            result.put("type", getJsonType(jsonNode));
            result.put("size", getJsonSize(jsonNode));
            result.put("data", jsonNode);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "parse");
            
            return ToolResult.success(result, metadata);
            
        } catch (JsonProcessingException e) {
            log.error("JSON parsing failed: {}", e.getMessage());
            return ToolResult.failure("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * 格式化 JSON（美化输出）
     */
    private ToolResult formatJson(String jsonString, int indent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            ObjectMapper prettyMapper = new ObjectMapper();
            prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String formatted = prettyMapper.writeValueAsString(jsonNode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("formatted", formatted);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "format");
            metadata.put("indent", indent);
            
            return ToolResult.success(result, metadata);
            
        } catch (JsonProcessingException e) {
            log.error("JSON formatting failed: {}", e.getMessage());
            return ToolResult.failure("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * 验证 JSON 格式
     */
    private ToolResult validateJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("message", "JSON is valid");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "validate");
            
            return ToolResult.success(result, metadata);
            
        } catch (JsonProcessingException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("error", e.getMessage());
            result.put("location", e.getLocation() != null ? 
                "Line " + e.getLocation().getLineNr() + ", Column " + e.getLocation().getColumnNr() : 
                "Unknown");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "validate");
            
            return ToolResult.success(result, metadata);
        }
    }

    /**
     * 使用简单路径查询 JSON（支持基本的点号和数组索引）
     */
    private ToolResult queryJson(String jsonString, String path) {
        try {
            JsonNode root = objectMapper.readTree(jsonString);
            JsonNode result = navigateJsonPath(root, path);
            
            if (result == null) {
                return ToolResult.failure("Path not found: " + path);
            }
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("path", path);
            resultData.put("result", result);
            resultData.put("type", getJsonType(result));
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "query");
            
            return ToolResult.success(resultData, metadata);
            
        } catch (Exception e) {
            log.error("JSON query failed: {}", e.getMessage());
            return ToolResult.failure("Query error: " + e.getMessage());
        }
    }

    /**
     * 提取指定字段值
     */
    private ToolResult extractField(String jsonString, String path) {
        try {
            JsonNode root = objectMapper.readTree(jsonString);
            JsonNode result = navigateJsonPath(root, path);
            
            if (result == null) {
                return ToolResult.failure("Field not found: " + path);
            }
            
            Object value = convertJsonNodeToJava(result);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("path", path);
            resultData.put("value", value);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "extract");
            
            return ToolResult.success(resultData, metadata);
            
        } catch (Exception e) {
            log.error("Field extraction failed: {}", e.getMessage());
            return ToolResult.failure("Extraction error: " + e.getMessage());
        }
    }

    /**
     * 简单的 JSON 路径导航（支持 $.field, $.field[0], $.nested.field）
     */
    private JsonNode navigateJsonPath(JsonNode root, String path) {
        if (path == null || path.trim().isEmpty() || path.equals("$")) {
            return root;
        }
        
        // 移除开头的 $. 或 $
        String cleanPath = path.replaceFirst("^\\$\\.?", "");
        
        JsonNode current = root;
        String[] parts = cleanPath.split("\\.");
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            // 处理数组索引 field[0]
            if (part.contains("[")) {
                String fieldName = part.substring(0, part.indexOf('['));
                String indexStr = part.substring(part.indexOf('[') + 1, part.indexOf(']'));
                
                if (!fieldName.isEmpty()) {
                    current = current.get(fieldName);
                }
                
                if (current != null && current.isArray()) {
                    int index = Integer.parseInt(indexStr);
                    current = current.get(index);
                }
            } else {
                current = current.get(part);
            }
        }
        
        return current;
    }

    private String getJsonType(JsonNode node) {
        if (node.isObject()) return "object";
        if (node.isArray()) return "array";
        if (node.isTextual()) return "string";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isNull()) return "null";
        return "unknown";
    }

    private int getJsonSize(JsonNode node) {
        if (node.isObject()) return node.size();
        if (node.isArray()) return node.size();
        return 1;
    }

    private Object convertJsonNodeToJava(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) {
            if (node.isInt()) return node.asInt();
            if (node.isLong()) return node.asLong();
            return node.asDouble();
        }
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(item -> list.add(convertJsonNodeToJava(item)));
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            node.fields().forEachRemaining(entry -> 
                map.put(entry.getKey(), convertJsonNodeToJava(entry.getValue()))
            );
            return map;
        }
        return node.toString();
    }

    public void setIndentOutput(boolean enabled) {
        if (enabled) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
    }
}
