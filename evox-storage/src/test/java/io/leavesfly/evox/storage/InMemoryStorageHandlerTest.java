package io.leavesfly.evox.storage;

import io.leavesfly.evox.storage.inmemory.InMemoryStorageHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryStorageHandler 测试
 */
@Slf4j
class InMemoryStorageHandlerTest {

    private InMemoryStorageHandler storage;
    private Map<String, Object> testData1;
    private Map<String, Object> testData2;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorageHandler();

        testData1 = new HashMap<>();
        testData1.put("name", "Test Agent");
        testData1.put("type", "agent");
        testData1.put("version", 1);

        testData2 = new HashMap<>();
        testData2.put("name", "Test Workflow");
        testData2.put("type", "workflow");
        testData2.put("status", "active");
    }

    @Test
    void testSaveAndLoad() {
        storage.save("agents", "agent1", testData1);

        Optional<Map<String, Object>> loaded = storage.load("agents", "agent1");
        assertTrue(loaded.isPresent());
        assertEquals("Test Agent", loaded.get().get("name"));
        assertEquals("agent", loaded.get().get("type"));
        assertEquals(1, loaded.get().get("version"));
    }

    @Test
    void testSaveAll() {
        Map<String, Map<String, Object>> records = new HashMap<>();
        records.put("agent1", testData1);
        records.put("agent2", testData2);

        storage.saveAll("agents", records);

        assertEquals(2, storage.count("agents"));
        assertTrue(storage.exists("agents", "agent1"));
        assertTrue(storage.exists("agents", "agent2"));
    }

    @Test
    void testLoadNonExistent() {
        Optional<Map<String, Object>> loaded = storage.load("agents", "nonexistent");
        assertFalse(loaded.isPresent());
    }

    @Test
    void testLoadAll() {
        storage.save("agents", "agent1", testData1);
        storage.save("agents", "agent2", testData2);

        Map<String, Map<String, Object>> all = storage.loadAll("agents");
        assertEquals(2, all.size());
        assertTrue(all.containsKey("agent1"));
        assertTrue(all.containsKey("agent2"));
    }

    @Test
    void testLoadTables() {
        storage.save("agents", "agent1", testData1);
        storage.save("workflows", "wf1", testData2);

        Map<String, Map<String, Map<String, Object>>> tables = 
                storage.loadTables(Arrays.asList("agents", "workflows"));
        
        assertEquals(2, tables.size());
        assertTrue(tables.containsKey("agents"));
        assertTrue(tables.containsKey("workflows"));
        assertEquals(1, tables.get("agents").size());
        assertEquals(1, tables.get("workflows").size());
    }

    @Test
    void testDelete() {
        storage.save("agents", "agent1", testData1);
        assertTrue(storage.exists("agents", "agent1"));

        boolean deleted = storage.delete("agents", "agent1");
        assertTrue(deleted);
        assertFalse(storage.exists("agents", "agent1"));
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleted = storage.delete("agents", "nonexistent");
        assertFalse(deleted);
    }

    @Test
    void testDeleteAll() {
        storage.save("agents", "agent1", testData1);
        storage.save("agents", "agent2", testData2);
        assertEquals(2, storage.count("agents"));

        storage.deleteAll("agents");
        assertEquals(0, storage.count("agents"));
    }

    @Test
    void testExists() {
        assertFalse(storage.exists("agents", "agent1"));

        storage.save("agents", "agent1", testData1);
        assertTrue(storage.exists("agents", "agent1"));
    }

    @Test
    void testCount() {
        assertEquals(0, storage.count("agents"));

        storage.save("agents", "agent1", testData1);
        assertEquals(1, storage.count("agents"));

        storage.save("agents", "agent2", testData2);
        assertEquals(2, storage.count("agents"));

        storage.delete("agents", "agent1");
        assertEquals(1, storage.count("agents"));
    }

    @Test
    void testGetTables() {
        List<String> tables = storage.getTables();
        assertTrue(tables.isEmpty());

        storage.save("agents", "agent1", testData1);
        storage.save("workflows", "wf1", testData2);

        tables = storage.getTables();
        assertEquals(2, tables.size());
        assertTrue(tables.contains("agents"));
        assertTrue(tables.contains("workflows"));
    }

    @Test
    void testClearAll() {
        storage.save("agents", "agent1", testData1);
        storage.save("workflows", "wf1", testData2);

        storage.clearAll();

        assertEquals(0, storage.getTables().size());
        assertEquals(0, storage.count("agents"));
        assertEquals(0, storage.count("workflows"));
    }

    @Test
    void testUpdate() {
        storage.save("agents", "agent1", testData1);

        Map<String, Object> updates = new HashMap<>();
        updates.put("version", 2);
        updates.put("status", "updated");

        boolean updated = storage.update("agents", "agent1", updates);
        assertTrue(updated);

        Optional<Map<String, Object>> loaded = storage.load("agents", "agent1");
        assertTrue(loaded.isPresent());
        assertEquals(2, loaded.get().get("version"));
        assertEquals("updated", loaded.get().get("status"));
        assertEquals("Test Agent", loaded.get().get("name")); // 原有字段保留
    }

    @Test
    void testUpdateNonExistent() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("version", 2);

        boolean updated = storage.update("agents", "nonexistent", updates);
        assertFalse(updated);
    }

    @Test
    void testQuery() {
        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "Agent A");
        data1.put("type", "agent");
        data1.put("status", "active");

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Agent B");
        data2.put("type", "agent");
        data2.put("status", "inactive");

        Map<String, Object> data3 = new HashMap<>();
        data3.put("name", "Workflow C");
        data3.put("type", "workflow");
        data3.put("status", "active");

        storage.save("entities", "e1", data1);
        storage.save("entities", "e2", data2);
        storage.save("entities", "e3", data3);

        // 查询所有 type=agent
        Map<String, Object> filter1 = new HashMap<>();
        filter1.put("type", "agent");
        Map<String, Map<String, Object>> results1 = storage.query("entities", filter1);
        assertEquals(2, results1.size());

        // 查询 type=agent AND status=active
        Map<String, Object> filter2 = new HashMap<>();
        filter2.put("type", "agent");
        filter2.put("status", "active");
        Map<String, Map<String, Object>> results2 = storage.query("entities", filter2);
        assertEquals(1, results2.size());
        assertTrue(results2.containsKey("e1"));
    }

    @Test
    void testGetStatistics() {
        storage.save("agents", "agent1", testData1);
        storage.save("agents", "agent2", testData2);
        storage.save("workflows", "wf1", testData1);

        InMemoryStorageHandler.StorageStatistics stats = storage.getStatistics();
        assertEquals(2, stats.getTableCount());
        assertEquals(3, stats.getTotalRecords());
        assertEquals(2, stats.getRecordsPerTable().get("agents"));
        assertEquals(1, stats.getRecordsPerTable().get("workflows"));

        log.info("Storage statistics: {}", stats);
    }

    @Test
    void testDataIsolation() {
        // 测试保存和加载的数据是否隔离（深拷贝）
        Map<String, Object> original = new HashMap<>();
        original.put("value", 100);

        storage.save("test", "id1", original);

        // 修改原始数据
        original.put("value", 200);

        // 加载的数据不应受影响
        Optional<Map<String, Object>> loaded = storage.load("test", "id1");
        assertTrue(loaded.isPresent());
        assertEquals(100, loaded.get().get("value"));
    }

    @Test
    void testConcurrentAccess() {
        // 简单的并发测试
        int threadCount = 10;
        int recordsPerThread = 100;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("thread", threadId);
                    data.put("record", j);
                    storage.save("concurrent", "t" + threadId + "_r" + j, data);
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 验证所有记录都被保存
        assertEquals(threadCount * recordsPerThread, storage.count("concurrent"));
    }

    @Test
    void testNullHandling() {
        // 测试 null 参数处理
        assertThrows(IllegalArgumentException.class, () -> storage.save(null, "id", testData1));
        assertThrows(IllegalArgumentException.class, () -> storage.save("table", null, testData1));
        assertThrows(IllegalArgumentException.class, () -> storage.save("table", "id", null));

        assertFalse(storage.load(null, "id").isPresent());
        assertFalse(storage.load("table", null).isPresent());

        assertFalse(storage.delete(null, "id"));
        assertFalse(storage.delete("table", null));

        assertFalse(storage.exists(null, "id"));
        assertFalse(storage.exists("table", null));

        assertEquals(0, storage.count(null));
    }
}
