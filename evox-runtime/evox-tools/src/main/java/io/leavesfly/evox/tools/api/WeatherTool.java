package io.leavesfly.evox.tools.api;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 天气查询工具
 * 支持查询城市天气信息
 * 
 * @author EvoX Team
 */
@Slf4j
public class WeatherTool extends BaseTool {

    private static final String OPENWEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String WTTR_API_URL = "https://wttr.in";
    
    private final HttpClient httpClient;
    private final String apiKey;
    
    /**
     * 创建天气工具（使用免费的wttr.in API，无需API Key）
     */
    public WeatherTool() {
        this(null);
    }
    
    /**
     * 创建天气工具（使用OpenWeatherMap API）
     * 
     * @param apiKey OpenWeatherMap API Key
     */
    public WeatherTool(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        this.name = "weather";
        this.description = "查询指定城市的天气信息，包括温度、湿度、天气状况等";
        
        this.inputs = new HashMap<>();
        
        Map<String, String> cityDef = new HashMap<>();
        cityDef.put("type", "string");
        cityDef.put("description", "城市名称，例如 'Beijing' 或 '北京'");
        inputs.put("city", cityDef);
        
        Map<String, String> unitsDef = new HashMap<>();
        unitsDef.put("type", "string");
        unitsDef.put("description", "温度单位: 'metric'(摄氏度) 或 'imperial'(华氏度)，默认为metric");
        inputs.put("units", unitsDef);
        
        Map<String, String> langDef = new HashMap<>();
        langDef.put("type", "string");
        langDef.put("description", "返回语言: 'zh_cn'(中文) 或 'en'(英文)，默认为zh_cn");
        inputs.put("lang", langDef);
        
        this.required = List.of("city");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String city = getParameter(parameters, "city", "");
            String units = getParameter(parameters, "units", "metric");
            String lang = getParameter(parameters, "lang", "zh_cn");
            
            if (city.isEmpty()) {
                return ToolResult.failure("城市名称不能为空");
            }
            
            Map<String, Object> weatherData;
            
            // 如果有API Key，使用OpenWeatherMap，否则使用wttr.in
            if (apiKey != null && !apiKey.isEmpty()) {
                weatherData = queryOpenWeatherMap(city, units, lang);
            } else {
                weatherData = queryWttrIn(city, lang);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("city", city);
            metadata.put("units", units);
            metadata.put("query_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ToolResult.success(weatherData, metadata);
            
        } catch (Exception e) {
            log.error("天气查询失败", e);
            return ToolResult.failure("天气查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用OpenWeatherMap API查询天气
     */
    private Map<String, Object> queryOpenWeatherMap(String city, String units, String lang) throws IOException, InterruptedException {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&appid=%s&units=%s&lang=%s",
                OPENWEATHER_API_URL, encodedCity, apiKey, units, lang);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("OpenWeatherMap API返回错误: " + response.statusCode());
        }
        
        return parseOpenWeatherResponse(response.body(), units);
    }
    
    /**
     * 使用wttr.in API查询天气（免费，无需API Key）
     */
    private Map<String, Object> queryWttrIn(String city, String lang) throws IOException, InterruptedException {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("%s/%s?format=j1&lang=%s", WTTR_API_URL, encodedCity, lang.replace("_", "-"));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EvoX-WeatherTool/1.0")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("wttr.in API返回错误: " + response.statusCode());
        }
        
        return parseWttrResponse(response.body());
    }
    
    /**
     * 解析OpenWeatherMap响应
     */
    private Map<String, Object> parseOpenWeatherResponse(String responseBody, String units) {
        Map<String, Object> result = new HashMap<>();
        
        // 简化的JSON解析（实际使用时建议用Jackson/Gson）
        try {
            // 提取温度
            String tempMatch = extractJsonValue(responseBody, "\"temp\":");
            if (tempMatch != null) {
                result.put("temperature", Double.parseDouble(tempMatch));
                result.put("temperature_unit", "metric".equals(units) ? "°C" : "°F");
            }
            
            // 提取体感温度
            String feelsLikeMatch = extractJsonValue(responseBody, "\"feels_like\":");
            if (feelsLikeMatch != null) {
                result.put("feels_like", Double.parseDouble(feelsLikeMatch));
            }
            
            // 提取湿度
            String humidityMatch = extractJsonValue(responseBody, "\"humidity\":");
            if (humidityMatch != null) {
                result.put("humidity", Integer.parseInt(humidityMatch) + "%");
            }
            
            // 提取天气描述
            String descMatch = extractJsonString(responseBody, "\"description\":\"");
            if (descMatch != null) {
                result.put("description", descMatch);
            }
            
            // 提取城市名
            String nameMatch = extractJsonString(responseBody, "\"name\":\"");
            if (nameMatch != null) {
                result.put("city_name", nameMatch);
            }
            
            // 提取风速
            String windMatch = extractJsonValue(responseBody, "\"speed\":");
            if (windMatch != null) {
                result.put("wind_speed", Double.parseDouble(windMatch) + " m/s");
            }
            
        } catch (Exception e) {
            log.warn("解析OpenWeatherMap响应时出现警告: {}", e.getMessage());
        }
        
        result.put("source", "OpenWeatherMap");
        return result;
    }
    
    /**
     * 解析wttr.in响应
     */
    private Map<String, Object> parseWttrResponse(String responseBody) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 提取当前天气信息
            String tempC = extractJsonValue(responseBody, "\"temp_C\":\"");
            if (tempC != null) {
                result.put("temperature", Integer.parseInt(tempC.replace("\"", "")));
                result.put("temperature_unit", "°C");
            }
            
            String feelsLike = extractJsonValue(responseBody, "\"FeelsLikeC\":\"");
            if (feelsLike != null) {
                result.put("feels_like", Integer.parseInt(feelsLike.replace("\"", "")));
            }
            
            String humidity = extractJsonValue(responseBody, "\"humidity\":\"");
            if (humidity != null) {
                result.put("humidity", humidity.replace("\"", "") + "%");
            }
            
            String desc = extractJsonString(responseBody, "\"weatherDesc\":[{\"value\":\"");
            if (desc != null) {
                result.put("description", desc);
            }
            
            String windSpeed = extractJsonValue(responseBody, "\"windspeedKmph\":\"");
            if (windSpeed != null) {
                result.put("wind_speed", windSpeed.replace("\"", "") + " km/h");
            }
            
            // 提取城市/地区信息
            String areaName = extractJsonString(responseBody, "\"areaName\":[{\"value\":\"");
            if (areaName != null) {
                result.put("city_name", areaName);
            }
            
            String country = extractJsonString(responseBody, "\"country\":[{\"value\":\"");
            if (country != null) {
                result.put("country", country);
            }
            
        } catch (Exception e) {
            log.warn("解析wttr.in响应时出现警告: {}", e.getMessage());
        }
        
        result.put("source", "wttr.in");
        return result;
    }
    
    /**
     * 从JSON中提取数值
     */
    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        
        int start = idx + key.length();
        int end = start;
        
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == '"')) {
            end++;
        }
        
        if (start < end) {
            return json.substring(start, end);
        }
        return null;
    }
    
    /**
     * 从JSON中提取字符串值
     */
    private String extractJsonString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        
        if (end > start) {
            return json.substring(start, end);
        }
        return null;
    }
    
    /**
     * 构建天气摘要
     */
    public String buildWeatherSummary(Map<String, Object> weatherData) {
        StringBuilder sb = new StringBuilder();
        
        Object cityName = weatherData.get("city_name");
        if (cityName != null) {
            sb.append(cityName).append("的天气:\n");
        }
        
        Object temp = weatherData.get("temperature");
        Object unit = weatherData.get("temperature_unit");
        if (temp != null) {
            sb.append("  温度: ").append(temp).append(unit != null ? unit : "").append("\n");
        }
        
        Object feelsLike = weatherData.get("feels_like");
        if (feelsLike != null) {
            sb.append("  体感温度: ").append(feelsLike).append(unit != null ? unit : "").append("\n");
        }
        
        Object desc = weatherData.get("description");
        if (desc != null) {
            sb.append("  天气状况: ").append(desc).append("\n");
        }
        
        Object humidity = weatherData.get("humidity");
        if (humidity != null) {
            sb.append("  湿度: ").append(humidity).append("\n");
        }
        
        Object wind = weatherData.get("wind_speed");
        if (wind != null) {
            sb.append("  风速: ").append(wind).append("\n");
        }
        
        return sb.toString();
    }
}
