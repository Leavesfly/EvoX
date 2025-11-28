package io.leavesfly.evox.storage.db;

import io.leavesfly.evox.storage.base.BaseStorage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * SQLite数据库存储
 * 提供基于SQLite的轻量级数据存储
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
public class SQLiteStore implements BaseStorage {

    private String dbPath;
    private Connection connection;
    private boolean initialized = false;

    public SQLiteStore(String dbPath) {
        this.dbPath = dbPath;
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            initialized = true;
            log.info("SQLite store initialized: {}", dbPath);
        } catch (Exception e) {
            log.error("Failed to initialize SQLite store", e);
            throw new RuntimeException("SQLite initialization failed", e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                initialized = false;
                log.info("SQLite store closed");
            } catch (SQLException e) {
                log.error("Error closing SQLite connection", e);
            }
        }
    }

    @Override
    public void clear() {
        // 由具体表管理器实现
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 创建表
     */
    public void createTable(String tableName, Map<String, String> columns) throws SQLException {
        if (!initialized) {
            initialize();
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (");
        
        int i = 0;
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            if (i > 0) sql.append(", ");
            sql.append(entry.getKey()).append(" ").append(entry.getValue());
            i++;
        }
        sql.append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
            log.debug("Table created: {}", tableName);
        }
    }

    /**
     * 插入数据
     */
    public long insert(String tableName, Map<String, Object> data) throws SQLException {
        if (!initialized) {
            initialize();
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        
        StringBuilder values = new StringBuilder("VALUES (");
        
        int i = 0;
        for (String key : data.keySet()) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(key);
            values.append("?");
            i++;
        }
        sql.append(") ").append(values).append(")");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString(), 
                Statement.RETURN_GENERATED_KEYS)) {
            
            int paramIndex = 1;
            for (Object value : data.values()) {
                pstmt.setObject(paramIndex++, value);
            }
            
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        }
    }

    /**
     * 查询数据
     */
    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        if (!initialized) {
            initialize();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            ResultSet rs = pstmt.executeQuery();
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
        
        return results;
    }

    /**
     * 更新数据
     */
    public int update(String sql, Object... params) throws SQLException {
        if (!initialized) {
            initialize();
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        }
    }

    /**
     * 删除数据
     */
    public int delete(String tableName, String whereClause, Object... params) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        return update(sql, params);
    }

    /**
     * 执行自定义SQL
     */
    public boolean execute(String sql) throws SQLException {
        if (!initialized) {
            initialize();
        }

        try (Statement stmt = connection.createStatement()) {
            return stmt.execute(sql);
        }
    }

    /**
     * 获取连接（用于高级操作）
     */
    public Connection getConnection() {
        if (!initialized) {
            initialize();
        }
        return connection;
    }
}
