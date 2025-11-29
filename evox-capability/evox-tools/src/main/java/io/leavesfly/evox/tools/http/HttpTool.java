package io.leavesfly.evox.tools.http;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求工具 - 提供 HTTP GET/POST 操作
 * 对应 Python 版本的 request.py 和 request_base.py
 */
@Slf4j
public class HttpTool extends BaseTool {

    private RestTemplate restTemplate;

    public HttpTool() {
        this.name = "http_request";
        this.description = "Make HTTP requests (GET, POST, PUT, DELETE)";
        this.restTemplate = new RestTemplate();
        
        this.inputs = new HashMap<>();
        
        Map<String, String> methodDef = new HashMap<>();
        methodDef.put("type", "string");
        methodDef.put("description", "HTTP method: GET, POST, PUT, DELETE");
        inputs.put("method", methodDef);
        
        Map<String, String> urlDef = new HashMap<>();
        urlDef.put("type", "string");
        urlDef.put("description", "Target URL");
        inputs.put("url", urlDef);
        
        Map<String, String> headersDef = new HashMap<>();
        headersDef.put("type", "object");
        headersDef.put("description", "HTTP headers (optional)");
        inputs.put("headers", headersDef);
        
        Map<String, String> bodyDef = new HashMap<>();
        bodyDef.put("type", "string");
        bodyDef.put("description", "Request body for POST/PUT (optional)");
        inputs.put("body", bodyDef);
        
        this.required = List.of("method", "url");
    }

    public HttpTool(RestTemplate restTemplate) {
        this.name = "http_request";
        this.description = "Make HTTP requests (GET, POST, PUT, DELETE)";
        this.restTemplate = restTemplate;
        
        this.inputs = new HashMap<>();
        
        Map<String, String> methodDef = new HashMap<>();
        methodDef.put("type", "string");
        methodDef.put("description", "HTTP method: GET, POST, PUT, DELETE");
        inputs.put("method", methodDef);
        
        Map<String, String> urlDef = new HashMap<>();
        urlDef.put("type", "string");
        urlDef.put("description", "Target URL");
        inputs.put("url", urlDef);
        
        Map<String, String> headersDef = new HashMap<>();
        headersDef.put("type", "object");
        headersDef.put("description", "HTTP headers (optional)");
        inputs.put("headers", headersDef);
        
        Map<String, String> bodyDef = new HashMap<>();
        bodyDef.put("type", "string");
        bodyDef.put("description", "Request body for POST/PUT (optional)");
        inputs.put("body", bodyDef);
        
        this.required = List.of("method", "url");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String method = getParameter(parameters, "method", "GET");
            String url = getParameter(parameters, "url", "");
            @SuppressWarnings("unchecked")
            Map<String, String> headersMap = getParameter(parameters, "headers", new HashMap<>());
            String body = getParameter(parameters, "body", "");
            
            HttpMethod httpMethod;
            try {
                httpMethod = HttpMethod.valueOf(method.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ToolResult.failure("Invalid HTTP method: " + method);
            }
            
            return executeRequest(httpMethod, url, headersMap, body);
            
        } catch (Exception e) {
            log.error("Error executing HTTP request", e);
            return ToolResult.failure(e.getMessage());
        }
    }

    /**
     * 执行 HTTP 请求
     */
    private ToolResult executeRequest(HttpMethod method, String url, 
                                      Map<String, String> headersMap, String body) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            if (headersMap != null && !headersMap.isEmpty()) {
                headersMap.forEach(headers::add);
            }
            
            // 如果没有指定 Content-Type，默认使用 application/json
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE) && 
                (method == HttpMethod.POST || method == HttpMethod.PUT)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            
            // 构建请求实体
            HttpEntity<String> entity;
            if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                entity = new HttpEntity<>(body, headers);
            } else {
                entity = new HttpEntity<>(headers);
            }
            
            // 执行请求
            ResponseEntity<String> response = restTemplate.exchange(
                url, method, entity, String.class);
            
            // 构建响应元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("status_code", response.getStatusCode().value());
            metadata.put("headers", response.getHeaders().toSingleValueMap());
            metadata.put("url", url);
            metadata.put("method", method.name());
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("body", response.getBody());
            data.put("status_code", response.getStatusCode().value());
            
            return ToolResult.success(data, metadata);
            
        } catch (RestClientException e) {
            log.error("HTTP request failed: {} {}", method, url, e);
            
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("url", url);
            errorMetadata.put("method", method.name());
            errorMetadata.put("error_type", e.getClass().getSimpleName());
            
            return ToolResult.failure("HTTP request failed: " + e.getMessage(), errorMetadata);
        }
    }

    /**
     * 便捷方法：GET 请求
     */
    public ToolResult get(String url) {
        return get(url, new HashMap<>());
    }

    /**
     * 便捷方法：GET 请求（带请求头）
     */
    public ToolResult get(String url, Map<String, String> headers) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("method", "GET");
        parameters.put("url", url);
        parameters.put("headers", headers);
        return execute(parameters);
    }

    /**
     * 便捷方法：POST 请求
     */
    public ToolResult post(String url, String body) {
        return post(url, body, new HashMap<>());
    }

    /**
     * 便捷方法：POST 请求（带请求头）
     */
    public ToolResult post(String url, String body, Map<String, String> headers) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("method", "POST");
        parameters.put("url", url);
        parameters.put("body", body);
        parameters.put("headers", headers);
        return execute(parameters);
    }
}
