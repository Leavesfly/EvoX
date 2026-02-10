package io.leavesfly.evox.tools.database;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL数据库工具
 * 提供PostgreSQL数据库的查询和操作
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgreSQLTool extends BaseTool {

    /**
     * JDBC连接字符串
     */
    private String jdbcUrl;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 数据库连接
     */
    private transient Connection connection;

    public PostgreSQLTool(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.name = "postgresql_tool";
        this.description = "PostgreSQL database operations including query, insert, update, delete";
        initializeSchema();
    }

    @Override
    public void initModule() {
        super.initModule();
        initializeConnection();
    }

    private void initializeSchema() {
        Map<String, Map<String, String>> inputs = new HashMap<>();
        
        inputs.put("operation", Map.of(
            "type", "string",
            "description", "Operation to perform: query, insert, update, delete, execute",
            "enum", "query,insert,update,delete,execute"
        ));
        
        inputs.put("sql", Map.of(
            "type", "string",
            "description", "SQL query or statement to execute"
        ));
        
        inputs.put("parameters", Map.of(
            "type", "array",
            "description", "Parameters for parameterized query"
        ));

        this.inputs = inputs;
        this.required = List.of("operation", "sql");
    }

    private void initializeConnection() {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            log.warn("PostgreSQL JDBC URL is not set");
            return;
        }

        try {
            this.connection = DriverManager.getConnection(jdbcUrl, username, password);
            log.info("PostgreSQL connection established: {}", jdbcUrl);
        } catch (SQLException e) {
            log.error("Failed to connect to PostgreSQL", e);
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String operation = getParameter(parameters, "operation", "");
            String sql = getParameter(parameters, "sql", "");
            
            if (connection == null || connection.isClosed()) {
                initializeConnection();
            }
            
            if (connection == null) {
                return ToolResult.failure("PostgreSQL connection not initialized");
            }
            
            return switch (operation.toLowerCase()) {
                case "query" -> executeQuery(sql, parameters);
                case "insert", "update", "delete" -> executeUpdate(sql, parameters);
                case "execute" -> executeStatement(sql, parameters);
                default -> ToolResult.failure("Unknown operation: " + operation);
            };
            
        } catch (Exception e) {
            log.error("PostgreSQL operation failed", e);
            return ToolResult.failure("PostgreSQL error: " + e.getMessage());
        }
    }

    /**
     * 执行查询
     */
    private ToolResult executeQuery(String sql, Map<String, Object> parameters) {
        try {
            List<Object> params = getParameter(parameters, "parameters", new ArrayList<>());
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            
            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> results = resultSetToList(rs);
            
            rs.close();
            stmt.close();
            
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("count", results.size());
            
            return ToolResult.success(data);
            
        } catch (SQLException e) {
            log.error("Query execution failed: {}", sql, e);
            return ToolResult.failure("Query failed: " + e.getMessage());
        }
    }

    /**
     * 执行更新（INSERT, UPDATE, DELETE）
     */
    private ToolResult executeUpdate(String sql, Map<String, Object> parameters) {
        try {
            List<Object> params = getParameter(parameters, "parameters", new ArrayList<>());
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            
            int affectedRows = stmt.executeUpdate();
            
            stmt.close();
            
            Map<String, Object> data = new HashMap<>();
            data.put("affected_rows", affectedRows);
            data.put("status", "success");
            
            return ToolResult.success(data);
            
        } catch (SQLException e) {
            log.error("Update execution failed: {}", sql, e);
            return ToolResult.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * 执行通用语句
     */
    private ToolResult executeStatement(String sql, Map<String, Object> parameters) {
        try {
            List<Object> params = getParameter(parameters, "parameters", new ArrayList<>());
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            
            boolean hasResults = stmt.execute();
            
            Map<String, Object> data = new HashMap<>();
            if (hasResults) {
                ResultSet rs = stmt.getResultSet();
                List<Map<String, Object>> results = resultSetToList(rs);
                rs.close();
                data.put("results", results);
                data.put("count", results.size());
            } else {
                int updateCount = stmt.getUpdateCount();
                data.put("update_count", updateCount);
            }
            
            stmt.close();
            data.put("status", "success");
            
            return ToolResult.success(data);
            
        } catch (SQLException e) {
            log.error("Statement execution failed: {}", sql, e);
            return ToolResult.failure("Execution failed: " + e.getMessage());
        }
    }

    /**
     * 设置PreparedStatement参数
     */
    private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            stmt.setObject(i + 1, param);
        }
    }

    /**
     * 将ResultSet转换为List
     */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        return results;
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                log.error("Error closing PostgreSQL connection", e);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
