package io.leavesfly.evox.tools.database;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.*;

/**
 * 数据库工具 - 支持 SQL 查询和操作
 * 对应 Python 版本的 DatabaseTool
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatabaseTool extends BaseTool {

    private JdbcTemplate jdbcTemplate;
    private String databaseType; // "postgresql", "mysql", "h2", etc.
    private boolean readOnly = false;

    public DatabaseTool(String connectionUrl, String username, String password, String databaseType) {
        this.name = "database_query";
        this.description = "Execute SQL queries on a database. Supports SELECT, INSERT, UPDATE, DELETE operations.";
        this.databaseType = databaseType;
        
        // 初始化输入参数定义
        this.inputs = new HashMap<>();
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "The SQL query to execute");
        this.inputs.put("query", queryParam);
        
        Map<String, String> paramsParam = new HashMap<>();
        paramsParam.put("type", "object");
        paramsParam.put("description", "Optional parameters for prepared statements");
        this.inputs.put("parameters", paramsParam);
        
        this.required = List.of("query");
        
        // 初始化数据源
        try {
            DataSource dataSource = createDataSource(connectionUrl, username, password, databaseType);
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            log.info("Database tool initialized for type: {}", databaseType);
        } catch (Exception e) {
            log.error("Failed to initialize database tool: {}", e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private DataSource createDataSource(String connectionUrl, String username, String password, String databaseType) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(connectionUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        // 设置驱动类
        switch (databaseType.toLowerCase()) {
            case "postgresql":
                dataSource.setDriverClassName("org.postgresql.Driver");
                break;
            case "mysql":
                dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                break;
            case "h2":
                dataSource.setDriverClassName("org.h2.Driver");
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
        
        return dataSource;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String query = getParameter(parameters, "query", "");
            Map<String, Object> queryParams = getParameter(parameters, "parameters", new HashMap<>());
            
            if (query.trim().isEmpty()) {
                return ToolResult.failure("Query cannot be empty");
            }
            
            // 检查是否为只读模式
            if (readOnly && !isReadOnlyQuery(query)) {
                return ToolResult.failure("Write operations are not allowed in read-only mode");
            }
            
            // 执行查询
            if (isSelectQuery(query)) {
                return executeSelect(query, queryParams);
            } else {
                return executeUpdate(query, queryParams);
            }
            
        } catch (Exception e) {
            log.error("Database query execution failed: {}", e.getMessage());
            return ToolResult.failure("Database error: " + e.getMessage());
        }
    }

    private boolean isSelectQuery(String query) {
        String trimmedQuery = query.trim().toUpperCase();
        return trimmedQuery.startsWith("SELECT") || trimmedQuery.startsWith("SHOW") || trimmedQuery.startsWith("DESCRIBE");
    }

    private boolean isReadOnlyQuery(String query) {
        String trimmedQuery = query.trim().toUpperCase();
        return trimmedQuery.startsWith("SELECT") || 
               trimmedQuery.startsWith("SHOW") || 
               trimmedQuery.startsWith("DESCRIBE") ||
               trimmedQuery.startsWith("EXPLAIN");
    }

    private ToolResult executeSelect(String query, Map<String, Object> params) {
        try {
            List<Map<String, Object>> results;
            
            if (params.isEmpty()) {
                results = jdbcTemplate.queryForList(query);
            } else {
                Object[] paramArray = params.values().toArray();
                results = jdbcTemplate.queryForList(query, paramArray);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("row_count", results.size());
            metadata.put("query_type", "SELECT");
            
            return ToolResult.success(results, metadata);
            
        } catch (Exception e) {
            log.error("SELECT query failed: {}", e.getMessage());
            return ToolResult.failure("Query failed: " + e.getMessage());
        }
    }

    private ToolResult executeUpdate(String query, Map<String, Object> params) {
        try {
            int affectedRows;
            
            if (params.isEmpty()) {
                affectedRows = jdbcTemplate.update(query);
            } else {
                Object[] paramArray = params.values().toArray();
                affectedRows = jdbcTemplate.update(query, paramArray);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("affected_rows", affectedRows);
            result.put("success", true);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("query_type", getQueryType(query));
            
            return ToolResult.success(result, metadata);
            
        } catch (Exception e) {
            log.error("UPDATE query failed: {}", e.getMessage());
            return ToolResult.failure("Query failed: " + e.getMessage());
        }
    }

    private String getQueryType(String query) {
        String trimmedQuery = query.trim().toUpperCase();
        if (trimmedQuery.startsWith("INSERT")) return "INSERT";
        if (trimmedQuery.startsWith("UPDATE")) return "UPDATE";
        if (trimmedQuery.startsWith("DELETE")) return "DELETE";
        if (trimmedQuery.startsWith("CREATE")) return "CREATE";
        if (trimmedQuery.startsWith("DROP")) return "DROP";
        if (trimmedQuery.startsWith("ALTER")) return "ALTER";
        return "OTHER";
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        log.info("Database tool read-only mode set to: {}", readOnly);
    }

    public List<String> listTables() {
        try {
            String query = switch (databaseType.toLowerCase()) {
                case "postgresql" -> "SELECT tablename FROM pg_tables WHERE schemaname = 'public'";
                case "mysql" -> "SHOW TABLES";
                case "h2" -> "SHOW TABLES";
                default -> throw new UnsupportedOperationException("listTables not supported for: " + databaseType);
            };
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            return results.stream()
                    .map(row -> row.values().iterator().next().toString())
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to list tables: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getTableSchema(String tableName) {
        try {
            String query = switch (databaseType.toLowerCase()) {
                case "postgresql" -> String.format(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = '%s'",
                    tableName
                );
                case "mysql" -> String.format("DESCRIBE %s", tableName);
                case "h2" -> String.format("SHOW COLUMNS FROM %s", tableName);
                default -> throw new UnsupportedOperationException("getTableSchema not supported for: " + databaseType);
            };
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(query);
            
            Map<String, Object> schema = new HashMap<>();
            schema.put("table_name", tableName);
            schema.put("columns", columns);
            
            return schema;
            
        } catch (Exception e) {
            log.error("Failed to get table schema: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
