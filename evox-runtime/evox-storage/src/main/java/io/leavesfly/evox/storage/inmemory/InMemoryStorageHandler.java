package io.leavesfly.evox.storage.inmemory;

import io.leavesfly.evox.storage.base.StorageHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存存储处理器 - 基于内存的数据存储实现
 * 对应 Python 版本的 StorageHandler 的内存实现
 */
@Slf4j
public class InMemoryStorageHandler implements StorageHandler {

    /**
     * 存储结构: 表名 -> (记录ID -> 数据)
     */
    private final Map<String, Map<String, Map<String, Object>>> storage;

    /**
     * 构造函数
     */
    public InMemoryStorageHandler() {
        this.storage = new ConcurrentHashMap<>();
        log.info("Initialized InMemoryStorageHandler");
    }

    @Override
    public void save(String table, String id, Map<String, Object> data) {
        if (table == null || id == null || data == null) {
            throw new IllegalArgumentException("Table, id, and data cannot be null");
        }

        storage.computeIfAbsent(table, k -> new ConcurrentHashMap<>())
                .put(id, new HashMap<>(data));
        
        log.debug("Saved record to table '{}' with id '{}'", table, id);
    }

    @Override
    public void saveAll(String table, Map<String, Map<String, Object>> records) {
        if (table == null || records == null) {
            throw new IllegalArgumentException("Table and records cannot be null");
        }

        Map<String, Map<String, Object>> tableData = storage.computeIfAbsent(
                table, k -> new ConcurrentHashMap<>());
        
        records.forEach((id, data) -> tableData.put(id, new HashMap<>(data)));
        
        log.debug("Saved {} records to table '{}'", records.size(), table);
    }

    @Override
    public Optional<Map<String, Object>> load(String table, String id) {
        if (table == null || id == null) {
            return Optional.empty();
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData == null) {
            return Optional.empty();
        }

        Map<String, Object> data = tableData.get(id);
        return Optional.ofNullable(data != null ? new HashMap<>(data) : null);
    }

    @Override
    public Map<String, Map<String, Object>> loadAll(String table) {
        if (table == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData == null) {
            return Collections.emptyMap();
        }

        // 返回深拷贝
        Map<String, Map<String, Object>> result = new HashMap<>();
        tableData.forEach((id, data) -> result.put(id, new HashMap<>(data)));
        return result;
    }

    @Override
    public Map<String, Map<String, Map<String, Object>>> loadTables(List<String> tables) {
        if (tables == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Map<String, Object>>> result = new HashMap<>();
        for (String table : tables) {
            result.put(table, loadAll(table));
        }
        return result;
    }

    @Override
    public boolean delete(String table, String id) {
        if (table == null || id == null) {
            return false;
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData == null) {
            return false;
        }

        boolean removed = tableData.remove(id) != null;
        if (removed) {
            log.debug("Deleted record from table '{}' with id '{}'", table, id);
        }
        return removed;
    }

    @Override
    public void deleteAll(String table) {
        if (table == null) {
            return;
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData != null) {
            int count = tableData.size();
            tableData.clear();
            log.debug("Deleted all {} records from table '{}'", count, table);
        }
    }

    @Override
    public boolean exists(String table, String id) {
        if (table == null || id == null) {
            return false;
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        return tableData != null && tableData.containsKey(id);
    }

    @Override
    public int count(String table) {
        if (table == null) {
            return 0;
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        return tableData != null ? tableData.size() : 0;
    }

    @Override
    public void clearAll() {
        int totalRecords = storage.values().stream()
                .mapToInt(Map::size)
                .sum();
        
        storage.clear();
        log.info("Cleared all storage ({} records across {} tables)", totalRecords, storage.size());
    }

    @Override
    public List<String> getTables() {
        return new ArrayList<>(storage.keySet());
    }

    /**
     * 获取存储统计信息
     */
    public StorageStatistics getStatistics() {
        int tableCount = storage.size();
        int totalRecords = storage.values().stream()
                .mapToInt(Map::size)
                .sum();
        
        Map<String, Integer> recordsPerTable = new HashMap<>();
        storage.forEach((table, data) -> recordsPerTable.put(table, data.size()));

        return new StorageStatistics(tableCount, totalRecords, recordsPerTable);
    }

    /**
     * 查询符合条件的记录
     *
     * @param table 表名
     * @param filter 过滤条件（键值对必须完全匹配）
     * @return 符合条件的记录（ID到数据的映射）
     */
    public Map<String, Map<String, Object>> query(String table, Map<String, Object> filter) {
        if (table == null || filter == null || filter.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Object>> result = new HashMap<>();
        tableData.forEach((id, data) -> {
            boolean matches = filter.entrySet().stream()
                    .allMatch(entry -> Objects.equals(data.get(entry.getKey()), entry.getValue()));
            if (matches) {
                result.put(id, new HashMap<>(data));
            }
        });

        return result;
    }

    /**
     * 更新记录（部分更新）
     *
     * @param table 表名
     * @param id 记录ID
     * @param updates 要更新的字段
     * @return 是否成功更新
     */
    public boolean update(String table, String id, Map<String, Object> updates) {
        if (table == null || id == null || updates == null) {
            return false;
        }

        Map<String, Map<String, Object>> tableData = storage.get(table);
        if (tableData == null) {
            return false;
        }

        Map<String, Object> data = tableData.get(id);
        if (data == null) {
            return false;
        }

        data.putAll(updates);
        log.debug("Updated record in table '{}' with id '{}' ({} fields)", 
                table, id, updates.size());
        return true;
    }

    /**
     * 存储统计信息
     */
    public static class StorageStatistics {
        private final int tableCount;
        private final int totalRecords;
        private final Map<String, Integer> recordsPerTable;

        public StorageStatistics(int tableCount, int totalRecords, Map<String, Integer> recordsPerTable) {
            this.tableCount = tableCount;
            this.totalRecords = totalRecords;
            this.recordsPerTable = recordsPerTable;
        }

        public int getTableCount() {
            return tableCount;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public Map<String, Integer> getRecordsPerTable() {
            return recordsPerTable;
        }

        @Override
        public String toString() {
            return String.format("StorageStatistics(tables=%d, totalRecords=%d, perTable=%s)",
                    tableCount, totalRecords, recordsPerTable);
        }
    }
}
