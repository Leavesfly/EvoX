package io.leavesfly.evox.tools;

import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tools 测试
 */
@Slf4j
class ToolsTest {

    private Path testDir;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        testDir = Files.createTempDirectory("evox-tools-test");
        testFile = testDir.resolve("test.txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理测试文件和目录
        if (Files.exists(testDir)) {
            // 递归删除目录中的所有文件
            try (var stream = Files.walk(testDir)) {
                stream.sorted((a, b) -> -a.compareTo(b)) // 反向排序，先删除文件再删除目录
                      .forEach(path -> {
                          try {
                              Files.deleteIfExists(path);
                          } catch (IOException e) {
                              // Ignore
                          }
                      });
            }
        }
    }

    // ========== BaseTool 测试 ==========

    @Test
    void testBaseToolSchema() {
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> schema = tool.getToolSchema();
        
        assertNotNull(schema);
        assertEquals("function", schema.get("type"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) schema.get("function");
        assertNotNull(function);
        assertEquals("file_system", function.get("name"));
        assertNotNull(function.get("description"));
        assertNotNull(function.get("parameters"));
    }

    @Test
    void testToolResultSuccess() {
        BaseTool.ToolResult result = BaseTool.ToolResult.success("test data");
        
        assertTrue(result.isSuccess());
        assertEquals("test data", result.getData());
        assertNull(result.getError());
    }

    @Test
    void testToolResultFailure() {
        BaseTool.ToolResult result = BaseTool.ToolResult.failure("error message");
        
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("error message", result.getError());
    }

    // ========== FileSystemTool 测试 ==========

    @Test
    void testFileSystemToolWrite() {
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "write");
        params.put("path", testFile.toString());
        params.put("content", "Hello World");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        assertTrue(Files.exists(testFile));
    }

    @Test
    void testFileSystemToolRead() throws IOException {
        // 先创建文件
        Files.writeString(testFile, "Test Content");
        
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "read");
        params.put("path", testFile.toString());
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        assertEquals("Test Content", result.getData());
    }

    @Test
    void testFileSystemToolAppend() throws IOException {
        // 先创建文件
        Files.writeString(testFile, "Line 1\n");
        
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "append");
        params.put("path", testFile.toString());
        params.put("content", "Line 2\n");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        String content = Files.readString(testFile);
        assertEquals("Line 1\nLine 2\n", content);
    }

    @Test
    void testFileSystemToolDelete() throws IOException {
        // 先创建文件
        Files.writeString(testFile, "To be deleted");
        assertTrue(Files.exists(testFile));
        
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "delete");
        params.put("path", testFile.toString());
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testFileSystemToolList() throws IOException {
        // 创建几个测试文件
        Files.writeString(testDir.resolve("file1.txt"), "content1");
        Files.writeString(testDir.resolve("file2.txt"), "content2");
        
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "list");
        params.put("path", testDir.toString());
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) result.getData();
        assertTrue(files.size() >= 2);
    }

    @Test
    void testFileSystemToolExists() {
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "exists");
        params.put("path", testFile.toString());
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        assertFalse((Boolean) result.getData());
    }

    @Test
    void testFileSystemToolReadNonExistent() {
        FileSystemTool tool = new FileSystemTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "read");
        params.put("path", "/non/existent/file.txt");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    // ========== HttpTool 测试 ==========

    @Test
    void testHttpToolGet() {
        // Mock RestTemplate
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ResponseEntity<String> mockResponse = new ResponseEntity<>(
            "{\"status\": \"ok\"}", HttpStatus.OK);
        
        when(mockRestTemplate.exchange(
            eq("http://example.com/api"),
            eq(HttpMethod.GET),
            any(),
            eq(String.class)
        )).thenReturn(mockResponse);
        
        HttpTool tool = new HttpTool(mockRestTemplate);
        
        Map<String, Object> params = new HashMap<>();
        params.put("method", "GET");
        params.put("url", "http://example.com/api");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("{\"status\": \"ok\"}", data.get("body"));
        assertEquals(200, data.get("status_code"));
    }

    @Test
    void testHttpToolPost() {
        // Mock RestTemplate
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ResponseEntity<String> mockResponse = new ResponseEntity<>(
            "{\"id\": 123}", HttpStatus.CREATED);
        
        when(mockRestTemplate.exchange(
            eq("http://example.com/api"),
            eq(HttpMethod.POST),
            any(),
            eq(String.class)
        )).thenReturn(mockResponse);
        
        HttpTool tool = new HttpTool(mockRestTemplate);
        
        Map<String, Object> params = new HashMap<>();
        params.put("method", "POST");
        params.put("url", "http://example.com/api");
        params.put("body", "{\"name\": \"test\"}");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("{\"id\": 123}", data.get("body"));
        assertEquals(201, data.get("status_code"));
    }

    @Test
    void testHttpToolInvalidMethod() {
        HttpTool tool = new HttpTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("method", "INVALID");
        params.put("url", "http://example.com");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid HTTP method"));
    }

    // ========== WebSearchTool 测试 ==========

    @Test
    void testWebSearchToolSchema() {
        WebSearchTool tool = new WebSearchTool();
        
        Map<String, Object> schema = tool.getToolSchema();
        
        assertNotNull(schema);
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) schema.get("function");
        assertEquals("web_search", function.get("name"));
    }

    @Test
    void testWebSearchToolEmptyQuery() {
        WebSearchTool tool = new WebSearchTool();
        
        Map<String, Object> params = new HashMap<>();
        params.put("query", "");
        
        BaseTool.ToolResult result = tool.execute(params);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("cannot be empty"));
    }

    @Test
    void testWebSearchToolMissingQuery() {
        WebSearchTool tool = new WebSearchTool();
        
        Map<String, Object> params = new HashMap<>();
        // query 是必需参数，但这里传入空字符串来测试
        params.put("query", "");
        
        BaseTool.ToolResult result = tool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("cannot be empty"));
    }

    // ========== 集成测试 ==========

    @Test
    void testMultipleToolsIntegration() throws IOException {
        // 1. 使用 FileSystemTool 写入文件
        FileSystemTool fileTool = new FileSystemTool();
        Map<String, Object> writeParams = new HashMap<>();
        writeParams.put("operation", "write");
        writeParams.put("path", testFile.toString());
        writeParams.put("content", "Integration Test Content");
        
        BaseTool.ToolResult writeResult = fileTool.execute(writeParams);
        assertTrue(writeResult.isSuccess());
        
        // 2. 读取文件验证
        Map<String, Object> readParams = new HashMap<>();
        readParams.put("operation", "read");
        readParams.put("path", testFile.toString());
        
        BaseTool.ToolResult readResult = fileTool.execute(readParams);
        assertTrue(readResult.isSuccess());
        assertEquals("Integration Test Content", readResult.getData());
        
        // 3. 检查文件存在
        Map<String, Object> existsParams = new HashMap<>();
        existsParams.put("operation", "exists");
        existsParams.put("path", testFile.toString());
        
        BaseTool.ToolResult existsResult = fileTool.execute(existsParams);
        assertTrue(existsResult.isSuccess());
        assertTrue((Boolean) existsResult.getData());
        
        log.info("Multiple tools integration test passed");
    }
}
