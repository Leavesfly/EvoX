package io.leavesfly.evox.tools.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端工具
 * 提供基础的HTTP请求功能
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class HttpClientTool {

    /**
     * 默认连接超时时间(毫秒)
     */
    private int connectTimeout;

    /**
     * 默认读取超时时间(毫秒)
     */
    private int readTimeout;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 构造函数
     */
    public HttpClientTool() {
        this.connectTimeout = 10000; // 10秒
        this.readTimeout = 30000; // 30秒
        this.userAgent = "EvoX-HttpClient/1.0";
    }

    /**
     * 执行HTTP请求
     *
     * @param params 请求参数
     * @return 响应结果
     */
    public Map<String, Object> execute(Map<String, Object> params) {
        String method = (String) params.getOrDefault("method", "GET");
        String url = (String) params.get("url");

        if (url == null || url.isEmpty()) {
            return error("URL is required");
        }

        try {
            switch (method.toUpperCase()) {
                case "GET":
                    return get(url, (Map<String, String>) params.get("headers"));
                case "POST":
                    return post(url, 
                              (String) params.get("body"),
                              (Map<String, String>) params.get("headers"));
                case "PUT":
                    return put(url, 
                             (String) params.get("body"),
                             (Map<String, String>) params.get("headers"));
                case "DELETE":
                    return delete(url, (Map<String, String>) params.get("headers"));
                default:
                    return error("Unsupported HTTP method: " + method);
            }
        } catch (Exception e) {
            log.error("HTTP request failed: {}", e.getMessage(), e);
            return error("Request failed: " + e.getMessage());
        }
    }

    /**
     * GET请求
     */
    public Map<String, Object> get(String urlString, Map<String, String> headers) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", userAgent);
            
            // 设置自定义header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            return readResponse(conn);
            
        } catch (Exception e) {
            log.error("GET request failed: {}", e.getMessage(), e);
            return error("GET request failed: " + e.getMessage());
        }
    }

    /**
     * POST请求
     */
    public Map<String, Object> post(String urlString, String body, Map<String, String> headers) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Content-Type", "application/json");
            
            // 设置自定义header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            // 写入请求体
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            return readResponse(conn);
            
        } catch (Exception e) {
            log.error("POST request failed: {}", e.getMessage(), e);
            return error("POST request failed: " + e.getMessage());
        }
    }

    /**
     * PUT请求
     */
    public Map<String, Object> put(String urlString, String body, Map<String, String> headers) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Content-Type", "application/json");
            
            // 设置自定义header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            // 写入请求体
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            return readResponse(conn);
            
        } catch (Exception e) {
            log.error("PUT request failed: {}", e.getMessage(), e);
            return error("PUT request failed: " + e.getMessage());
        }
    }

    /**
     * DELETE请求
     */
    public Map<String, Object> delete(String urlString, Map<String, String> headers) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", userAgent);
            
            // 设置自定义header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            return readResponse(conn);
            
        } catch (Exception e) {
            log.error("DELETE request failed: {}", e.getMessage(), e);
            return error("DELETE request failed: " + e.getMessage());
        }
    }

    /**
     * 读取响应
     */
    private Map<String, Object> readResponse(HttpURLConnection conn) throws Exception {
        int statusCode = conn.getResponseCode();
        String statusMessage = conn.getResponseMessage();
        
        // 读取响应头
        Map<String, String> responseHeaders = new HashMap<>();
        conn.getHeaderFields().forEach((key, values) -> {
            if (key != null && !values.isEmpty()) {
                responseHeaders.put(key, values.get(0));
            }
        });
        
        // 读取响应体
        StringBuilder response = new StringBuilder();
        BufferedReader reader;
        
        if (statusCode >= 200 && statusCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", statusCode >= 200 && statusCode < 300);
        result.put("statusCode", statusCode);
        result.put("statusMessage", statusMessage);
        result.put("headers", responseHeaders);
        result.put("body", response.toString());
        
        log.info("HTTP request completed: {} {}", statusCode, statusMessage);
        
        return result;
    }

    /**
     * 构造错误响应
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }

    /**
     * 下载文件
     */
    public Map<String, Object> downloadFile(String urlString, String savePath) {
        // TODO: 实现文件下载功能
        log.warn("File download not yet implemented");
        return error("File download not implemented");
    }

    /**
     * 上传文件
     */
    public Map<String, Object> uploadFile(String urlString, String filePath, Map<String, String> headers) {
        // TODO: 实现文件上传功能
        log.warn("File upload not yet implemented");
        return error("File upload not implemented");
    }
}
