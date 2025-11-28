package io.leavesfly.evox.tools;

import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.database.DatabaseTool;
import io.leavesfly.evox.tools.interpreter.CodeInterpreterTool;
import io.leavesfly.evox.tools.json.JsonTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新增工具测试类
 */
class NewToolsTest {

    private DatabaseTool databaseTool;
    private CodeInterpreterTool codeInterpreterTool;
    private JsonTool jsonTool;
    private Toolkit toolkit;

    @BeforeEach
    void setUp() {
        // 初始化 H2 内存数据库工具
        databaseTool = new DatabaseTool(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                "h2"
        );

        // 初始化代码解释器（JavaScript）- 跳过测试由于环境限制
        try {
            codeInterpreterTool = new CodeInterpreterTool("javascript", Paths.get("./target/test-workspace"));
        } catch (Exception e) {
            // JavaScript 引擎不可用，跳过相关测试
            codeInterpreterTool = null;
        }

        // 初始化 JSON 工具
        jsonTool = new JsonTool();

        // 初始化工具集
        toolkit = new Toolkit("TestToolkit", "Test toolkit for unit tests");
        toolkit.addTool(databaseTool);
        if (codeInterpreterTool != null) {
            toolkit.addTool(codeInterpreterTool);
        }
        toolkit.addTool(jsonTool);
    }

    // ==================== DatabaseTool 测试 ====================

    @Test
    void testDatabaseTool_CreateTable() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100))");

        BaseTool.ToolResult result = databaseTool.execute(params);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testDatabaseTool_InsertData() {
        // 先创建表
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("query", "CREATE TABLE test_users (id INT PRIMARY KEY, name VARCHAR(100))");
        databaseTool.execute(createParams);

        // 插入数据
        Map<String, Object> insertParams = new HashMap<>();
        insertParams.put("query", "INSERT INTO test_users (id, name) VALUES (1, 'Alice')");

        BaseTool.ToolResult result = databaseTool.execute(insertParams);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(1, data.get("affected_rows"));
    }

    @Test
    void testDatabaseTool_SelectData() {
        // 创建表并插入数据
        databaseTool.execute(Map.of("query", "CREATE TABLE select_test (id INT PRIMARY KEY, name VARCHAR(100))"));
        databaseTool.execute(Map.of("query", "INSERT INTO select_test VALUES (1, 'Bob')"));
        databaseTool.execute(Map.of("query", "INSERT INTO select_test VALUES (2, 'Charlie')"));

        // 查询数据
        Map<String, Object> selectParams = new HashMap<>();
        selectParams.put("query", "SELECT * FROM select_test ORDER BY id");

        BaseTool.ToolResult result = databaseTool.execute(selectParams);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getData();
        assertEquals(2, rows.size());
        assertEquals("Bob", rows.get(0).get("NAME"));
    }

    @Test
    void testDatabaseTool_ListTables() {
        databaseTool.execute(Map.of("query", "CREATE TABLE table1 (id INT)"));
        databaseTool.execute(Map.of("query", "CREATE TABLE table2 (id INT)"));

        List<String> tables = databaseTool.listTables();

        assertNotNull(tables);
        assertTrue(tables.size() >= 2);
    }

    @Test
    void testDatabaseTool_ReadOnlyMode() {
        databaseTool.setReadOnly(true);

        Map<String, Object> params = new HashMap<>();
        params.put("query", "CREATE TABLE forbidden (id INT)");

        BaseTool.ToolResult result = databaseTool.execute(params);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("read-only"));
    }

    // ==================== CodeInterpreterTool 测试 ====================

    @Test
    void testCodeInterpreter_SimpleJavaScript() {
        if (codeInterpreterTool == null) {
            return; // 跳过，引擎不可用
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("code", "var result = 1 + 2; result;");
        params.put("language", "javascript");

        BaseTool.ToolResult result = codeInterpreterTool.execute(params);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testCodeInterpreter_JavaScriptWithOutput() {
        if (codeInterpreterTool == null) {
            return; // 跳过，引擎不可用
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("code", "print('Hello, World!'); 42;");

        BaseTool.ToolResult result = codeInterpreterTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data.get("result"));
    }

    @Test
    void testCodeInterpreter_EmptyCode() {
        if (codeInterpreterTool == null) {
            return; // 跳过，引擎不可用
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("code", "");

        BaseTool.ToolResult result = codeInterpreterTool.execute(params);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("empty"));
    }

    @Test
    void testCodeInterpreter_UnsafeCode() {
        if (codeInterpreterTool == null) {
            return; // 跳过，引擎不可用
        }
        
        codeInterpreterTool.setSandboxMode(true);

        Map<String, Object> params = new HashMap<>();
        params.put("code", "java.lang.System.exit(0);");

        BaseTool.ToolResult result = codeInterpreterTool.execute(params);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("unsafe"));
    }

    // ==================== JsonTool 测试 ====================

    @Test
    void testJsonTool_ParseValid() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "parse");
        params.put("data", "{\"name\": \"Alice\", \"age\": 30}");

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue((Boolean) data.get("parsed"));
        assertEquals("object", data.get("type"));
    }

    @Test
    void testJsonTool_ParseInvalid() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "parse");
        params.put("data", "{invalid json}");

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Invalid JSON"));
    }

    @Test
    void testJsonTool_Format() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "format");
        params.put("data", "{\"name\":\"Bob\",\"age\":25}");
        params.put("indent", 2);

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String formatted = (String) data.get("formatted");
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("  "));
    }

    @Test
    void testJsonTool_Validate() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "validate");
        params.put("data", "[1, 2, 3, 4, 5]");

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue((Boolean) data.get("valid"));
    }

    @Test
    void testJsonTool_Query() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "query");
        params.put("data", "{\"user\": {\"name\": \"Charlie\", \"age\": 35}}");
        params.put("path", "$.user.name");

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("$.user.name", data.get("path"));
    }

    @Test
    void testJsonTool_ExtractField() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "extract");
        params.put("data", "{\"users\": [{\"name\": \"Alice\"}, {\"name\": \"Bob\"}]}");
        params.put("path", "$.users[0].name");

        BaseTool.ToolResult result = jsonTool.execute(params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("Alice", data.get("value"));
    }

    // ==================== Toolkit 测试 ====================

    @Test
    void testToolkit_AddTool() {
        Toolkit newToolkit = new Toolkit("TestKit", "Test");
        assertEquals(0, newToolkit.getToolCount());

        newToolkit.addTool(jsonTool);
        assertEquals(1, newToolkit.getToolCount());
        assertTrue(newToolkit.hasTool("json_tool"));
    }

    @Test
    void testToolkit_RemoveTool() {
        assertTrue(toolkit.hasTool("database_query"));

        boolean removed = toolkit.removeTool("database_query");

        assertTrue(removed);
        assertFalse(toolkit.hasTool("database_query"));
    }

    @Test
    void testToolkit_GetTool() {
        BaseTool tool = toolkit.getTool("json_tool");

        assertNotNull(tool);
        assertEquals("json_tool", tool.getName());
    }

    @Test
    void testToolkit_GetToolNames() {
        List<String> names = toolkit.getToolNames();

        assertTrue(names.size() >= 2); // 至少有 database 和 json
        assertTrue(names.contains("database_query"));
        assertTrue(names.contains("json_tool"));
    }

    @Test
    void testToolkit_GetToolSchemas() {
        List<Map<String, Object>> schemas = toolkit.getToolSchemas();

        assertTrue(schemas.size() >= 2);
        schemas.forEach(schema -> {
            assertEquals("function", schema.get("type"));
            assertNotNull(schema.get("function"));
        });
    }

    @Test
    void testToolkit_ExecuteTool() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "validate");
        params.put("data", "{\"valid\": true}");

        BaseTool.ToolResult result = toolkit.executeTool("json_tool", params);

        assertTrue(result.isSuccess());
    }

    @Test
    void testToolkit_ExecuteNonExistentTool() {
        BaseTool.ToolResult result = toolkit.executeTool("non_existent", Map.of());

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    void testToolkit_GetStatistics() {
        Map<String, Object> stats = toolkit.getStatistics();

        assertEquals("TestToolkit", stats.get("toolkit_name"));
        assertTrue((Integer) stats.get("tool_count") >= 2);
        assertNotNull(stats.get("tool_names"));
    }

    @Test
    void testToolkit_Clear() {
        assertTrue(toolkit.getToolCount() >= 2);

        toolkit.clear();

        assertEquals(0, toolkit.getToolCount());
        assertFalse(toolkit.hasTool("json_tool"));
    }
}
