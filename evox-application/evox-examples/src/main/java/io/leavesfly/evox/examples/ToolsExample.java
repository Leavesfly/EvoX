package io.leavesfly.evox.examples;

import io.leavesfly.evox.tools.browser.BrowserTool;
import io.leavesfly.evox.tools.calculator.CalculatorTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpClientTool;
import io.leavesfly.evox.tools.search.SearchTool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具使用示例
 * 演示如何使用各种内置工具
 *
 * @author EvoX Team
 */
@Slf4j
public class ToolsExample {

    public static void main(String[] args) {
        log.info("=== EvoX Tools Examples ===\n");

        // 1. 计算器工具
        calculatorExample();

        // 2. HTTP客户端工具
        httpClientExample();

        // 3. 文件系统工具
        fileSystemExample();

        // 4. 搜索工具
        searchToolExample();

        // 5. 浏览器工具
        browserToolExample();
    }

    /**
     * 计算器工具示例
     */
    private static void calculatorExample() {
        log.info("\n--- Calculator Tool Example ---");

        CalculatorTool calculator = new CalculatorTool();

        // 基础运算
        var result1 = calculator.add(10, 5);
        log.info("10 + 5 = {}", result1.get("result"));

        var result2 = calculator.multiply(7, 8);
        log.info("7 * 8 = {}", result2.get("result"));

        var result3 = calculator.divide(100, 4);
        log.info("100 / 4 = {}", result3.get("result"));

        // 高级数学函数
        var result4 = calculator.power(2, 10);
        log.info("2^10 = {}", result4.get("result"));

        var result5 = calculator.sqrt(144);
        log.info("√144 = {}", result5.get("result"));

        var result6 = calculator.sin(30);
        log.info("sin(30°) = {}", result6.get("result"));

        // 使用表达式
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "(10 + 5) * 2 - 8");
        var result7 = calculator.execute(params);
        log.info("Expression result: {}", result7);

        // 统计函数
        double[] values = {10, 20, 30, 40, 50};
        var mean = calculator.mean(values);
        log.info("Mean of [10,20,30,40,50] = {}", mean.get("result"));

        var max = calculator.max(values);
        log.info("Max of [10,20,30,40,50] = {}", max.get("result"));
    }

    /**
     * HTTP客户端工具示例
     */
    private static void httpClientExample() {
        log.info("\n--- HTTP Client Tool Example ---");

        HttpClientTool httpClient = new HttpClientTool();

        // GET请求示例
        Map<String, Object> params = new HashMap<>();
        params.put("method", "GET");
        params.put("url", "https://api.github.com/repos/octocat/Hello-World");

        log.info("Sending GET request to GitHub API...");
        // var response = httpClient.execute(params);
        // log.info("Response: {}", response);

        // 实际使用中取消注释上面的代码
        log.info("(Example disabled to avoid actual HTTP calls)");

        // POST请求示例
        Map<String, Object> postParams = new HashMap<>();
        postParams.put("method", "POST");
        postParams.put("url", "https://httpbin.org/post");
        postParams.put("body", "{\"key\":\"value\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        postParams.put("headers", headers);

        log.info("POST request configured");
        log.info("URL: {}", postParams.get("url"));
        log.info("Body: {}", postParams.get("body"));
    }

    /**
     * 文件系统工具示例
     */
    private static void fileSystemExample() {
        log.info("\n--- File System Tool Example ---");

        FileSystemTool fileTool = new FileSystemTool();

        // 检查文件是否存在
        Map<String, Object> params1 = new HashMap<>();
        params1.put("operation", "exists");
        params1.put("filePath", "/tmp/test.txt");

        var result1 = fileTool.execute(params1);
        log.info("File exists check: {}", result1);

        // 写入文件
        Map<String, Object> params2 = new HashMap<>();
        params2.put("operation", "write");
        params2.put("filePath", "/tmp/evox_test.txt");
        params2.put("content", "Hello from EvoX!");

        log.info("Writing to file: /tmp/evox_test.txt");
        var result2 = fileTool.execute(params2);
        log.info("Write result: {}", String.valueOf(result2.get("success")));

        // 读取文件
        Map<String, Object> params3 = new HashMap<>();
        params3.put("operation", "read");
        params3.put("filePath", "/tmp/evox_test.txt");

        var result3 = fileTool.execute(params3);
        log.info("Read result: {}", result3);

        // 列出目录
        Map<String, Object> params4 = new HashMap<>();
        params4.put("operation", "list");
        params4.put("directory", "/tmp");

        var result4 = fileTool.execute(params4);
        log.info("List directory result: {} files", 
                result4.get("files") != null ? "multiple" : "none");

        // 删除文件
        Map<String, Object> params5 = new HashMap<>();
        params5.put("operation", "delete");
        params5.put("filePath", "/tmp/evox_test.txt");

        var result5 = fileTool.execute(params5);
        log.info("Delete result: {}", String.valueOf(result5.get("success")));
    }

    /**
     * 搜索工具示例
     */
    private static void searchToolExample() {
        log.info("\n--- Search Tool Example ---");

        // Wikipedia搜索
        SearchTool wikiSearch = new SearchTool("wikipedia", 3);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("query", "artificial intelligence");
        params1.put("numResults", 2);

        log.info("Searching Wikipedia for: artificial intelligence");
        // var result1 = wikiSearch.execute(params1);
        // log.info("Wikipedia results: {}", result1);

        log.info("(Example disabled to avoid actual API calls)");

        // Google搜索配置示例
        SearchTool googleSearch = new SearchTool("google", 5);
        log.info("Google search tool configured");
        log.info("Search engine: {}", googleSearch.getSearchEngine());
        log.info("Max results: {}", googleSearch.getMaxResults());

        // DuckDuckGo搜索配置示例
        SearchTool ddgSearch = new SearchTool("duckduckgo", 10);
        log.info("DuckDuckGo search tool configured");
    }

    /**
     * 浏览器工具示例
     */
    private static void browserToolExample() {
        log.info("\n--- Browser Tool Example ---");

        BrowserTool browser = new BrowserTool();

        // 导航示例
        Map<String, Object> params1 = new HashMap<>();
        params1.put("action", "navigate");
        params1.put("url", "https://www.example.com");

        log.info("Browser navigation configured");
        log.info("Target URL: {}", params1.get("url"));

        // 点击元素示例
        Map<String, Object> params2 = new HashMap<>();
        params2.put("action", "click");
        params2.put("selector", "#submit-button");

        log.info("Click action configured");
        log.info("Selector: {}", params2.get("selector"));

        // 输入文本示例
        Map<String, Object> params3 = new HashMap<>();
        params3.put("action", "type");
        params3.put("selector", "#search-input");
        params3.put("text", "EvoX Framework");

        log.info("Type action configured");
        log.info("Input: {}", params3.get("text"));

        // 截图示例
        Map<String, Object> params4 = new HashMap<>();
        params4.put("action", "screenshot");
        params4.put("path", "/tmp/screenshot.png");

        log.info("Screenshot configured");
        log.info("Save path: {}", params4.get("path"));

        log.info("(Browser examples require Selenium/Playwright setup)");
    }

    /**
     * 工具组合使用示例
     */
    @SuppressWarnings("unused")
    private static void combinedToolsExample() {
        log.info("\n--- Combined Tools Example ---");

        log.info("Real-world scenario: Research Assistant");
        log.info("1. Search for information (SearchTool)");
        log.info("2. Fetch webpage content (BrowserTool)");
        log.info("3. Extract and save data (FileSystemTool)");
        log.info("4. Perform calculations (CalculatorTool)");
        log.info("5. Send results via HTTP (HttpClientTool)");

        /*
        // 伪代码流程:
        SearchTool search = new SearchTool("google", 5);
        BrowserTool browser = new BrowserTool();
        FileSystemTool file = new FileSystemTool();
        CalculatorTool calc = new CalculatorTool();
        HttpClientTool http = new HttpClientTool();

        // 1. 搜索主题
        var searchResults = search.execute(Map.of("query", "AI trends 2024"));
        
        // 2. 访问第一个结果
        String url = extractFirstUrl(searchResults);
        var pageContent = browser.execute(Map.of("action", "navigate", "url", url));
        
        // 3. 保存内容
        file.execute(Map.of("operation", "write", "filePath", "/tmp/research.txt", 
                           "content", pageContent));
        
        // 4. 统计分析
        var stats = calc.mean(extractNumbers(pageContent));
        
        // 5. 发送报告
        http.execute(Map.of("method", "POST", "url", "https://api.example.com/report",
                           "body", createReport(stats)));
        */
    }
}
