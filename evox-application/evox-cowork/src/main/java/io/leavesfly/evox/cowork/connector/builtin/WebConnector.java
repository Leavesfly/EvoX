package io.leavesfly.evox.cowork.connector.builtin;

import io.leavesfly.evox.cowork.connector.BaseConnector;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebConnector extends BaseConnector {
    
    private final HttpClient httpClient;
    
    public WebConnector() {
        super("web", "Web Browser", "Browse web pages and extract content", ConnectorType.BROWSER);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Override
    protected void doConnect() throws Exception {
        log.info("WebConnector initialized");
    }
    
    @Override
    protected void doDisconnect() {
        log.info("WebConnector disconnected");
    }
    
    @Override
    public List<String> getSupportedActions() {
        return List.of("fetch", "search");
    }
    
    @Override
    protected Map<String, Object> doExecute(String action, Map<String, Object> parameters) {
        try {
            switch (action) {
                case "fetch":
                    return fetchUrl(parameters);
                case "search":
                    return searchWeb(parameters);
                default:
                    throw new UnsupportedOperationException("Action '" + action + "' is not supported");
            }
        } catch (Exception e) {
            log.error("Error executing action '{}': {}", action, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
    
    private Map<String, Object> fetchUrl(Map<String, Object> parameters) throws IOException, InterruptedException {
        String url = (String) parameters.get("url");
        if (url == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "url parameter is required");
            return result;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("statusCode", response.statusCode());
            result.put("body", response.body());
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Invalid URL: " + e.getMessage());
            return result;
        }
    }
    
    private Map<String, Object> searchWeb(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("message", "Web search requires WebSearchTool integration. This connector only supports fetching specific URLs.");
        return result;
    }
}
