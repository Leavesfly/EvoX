package io.leavesfly.evox.storage.db;

import io.leavesfly.evox.storage.base.BaseStorage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL数据库存储实现
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class PostgreSQLStore implements BaseStorage {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Connection connection;
    private boolean initialized = false;

    public PostgreSQLStore(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        try {
            connect();
            initialized = true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize PostgreSQL store", e);
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void clear() {
        // 由具体表管理器实现
        log.warn("clear() not implemented for PostgreSQLStore");
    }

    /**
     * 连接数据库
     */
    public void connect() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        connection = DriverManager.getConnection(url, username, password);
        log.info("Connected to PostgreSQL database: {}", database);
    }

    /**
     * 执行查询
     */
    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        
        return results;
    }

    /**
     * 执行更新
     */
    public int execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    /**
     * 创建表
     */
    public void createTable(String tableName, Map<String, String> columns) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            sql.append(entry.getKey()).append(" ").append(entry.getValue()).append(", ");
        }
        
        sql.setLength(sql.length() - 2);
        sql.append(")");
        
        execute(sql.toString());
        log.info("Created table: {}", tableName);
    }

    /**
     * 插入数据
     */
    public void insert(String tableName, Map<String, Object> data) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            columns.append(entry.getKey()).append(", ");
            values.append("?, ");
            params.add(entry.getValue());
        }
        
        columns.setLength(columns.length() - 2);
        values.setLength(values.length() - 2);
        
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
                                   tableName, columns, values);
        execute(sql, params.toArray());
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Closed PostgreSQL connection");
            } catch (SQLException e) {
                log.error("Error closing connection", e);
            }
        }
    }
}
