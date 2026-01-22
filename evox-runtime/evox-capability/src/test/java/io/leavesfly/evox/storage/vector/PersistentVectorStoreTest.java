package io.leavesfly.evox.storage.vector;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersistentVectorStore 单元测试
 * 
 * @author EvoX Team
 */
@DisplayName("持久化向量存储测试")
class PersistentVectorStoreTest {

    private PersistentVectorStore store;
    private static final int DIMENSION = 128;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        store = new PersistentVectorStore(DIMENSION);
        store.initialize();
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    @DisplayName("测试初始化和关闭")
    void testInitializeAndClose() {
        PersistentVectorStore newStore = new PersistentVectorStore(DIMENSION);
        assertFalse(newStore.isInitialized());
        
        newStore.initialize();
        assertTrue(newStore.isInitialized());
        
        newStore.close();
        assertFalse(newStore.isInitialized());
    }

    @Test
    @DisplayName("测试添加和获取向量")
    void testAddAndGetVector() {
        float[] vector = createRandomVector(DIMENSION);
        Map<String, Object> metadata = Map.of("key", "value");
        
        store.addVector("vec1", vector, metadata);
        
        assertEquals(1, store.getVectorCount());
        assertTrue(store.isModified());
    }

    @Test
    @DisplayName("测试批量添加向量")
    void testAddVectors() {
        List<String> ids = Arrays.asList("v1", "v2", "v3");
        List<float[]> vectors = Arrays.asList(
            createRandomVector(DIMENSION),
            createRandomVector(DIMENSION),
            createRandomVector(DIMENSION)
        );
        List<Map<String, Object>> metadataList = Arrays.asList(
            Map.of("idx", 1),
            Map.of("idx", 2),
            Map.of("idx", 3)
        );
        
        store.addVectors(ids, vectors, metadataList);
        
        assertEquals(3, store.getVectorCount());
    }

    @Test
    @DisplayName("测试维度验证")
    void testDimensionValidation() {
        float[] wrongDimensionVector = createRandomVector(64); // 错误的维度
        
        assertThrows(IllegalArgumentException.class, () -> {
            store.addVector("vec1", wrongDimensionVector, new HashMap<>());
        });
    }

    @Test
    @DisplayName("测试相似度搜索")
    void testSearch() {
        // 添加一些向量
        float[] v1 = createRandomVector(DIMENSION);
        float[] v2 = createRandomVector(DIMENSION);
        float[] v3 = createRandomVector(DIMENSION);
        
        store.addVector("v1", v1, Map.of("type", "A"));
        store.addVector("v2", v2, Map.of("type", "B"));
        store.addVector("v3", v3, Map.of("type", "A"));
        
        // 使用v1作为查询向量
        List<VectorStore.SearchResult> results = store.search(v1, 2);
        
        assertNotNull(results);
        assertTrue(results.size() <= 2);
        // v1应该是最相似的（它本身）
        assertEquals("v1", results.get(0).getId());
        assertEquals(1.0f, results.get(0).getScore(), 0.001f);
    }

    @Test
    @DisplayName("测试带过滤条件的搜索")
    void testSearchWithFilter() {
        float[] v1 = createRandomVector(DIMENSION);
        float[] v2 = createRandomVector(DIMENSION);
        float[] v3 = createRandomVector(DIMENSION);
        
        store.addVector("v1", v1, Map.of("type", "A"));
        store.addVector("v2", v2, Map.of("type", "B"));
        store.addVector("v3", v3, Map.of("type", "A"));
        
        // 只搜索type=A的向量
        List<VectorStore.SearchResult> results = store.search(v1, 10, Map.of("type", "A"));
        
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "A".equals(r.getMetadata().get("type"))));
    }

    @Test
    @DisplayName("测试删除向量")
    void testDeleteVector() {
        store.addVector("v1", createRandomVector(DIMENSION), new HashMap<>());
        store.addVector("v2", createRandomVector(DIMENSION), new HashMap<>());
        
        assertEquals(2, store.getVectorCount());
        
        assertTrue(store.deleteVector("v1"));
        assertEquals(1, store.getVectorCount());
        
        assertFalse(store.deleteVector("nonexistent"));
    }

    @Test
    @DisplayName("测试批量删除向量")
    void testDeleteVectors() {
        store.addVector("v1", createRandomVector(DIMENSION), new HashMap<>());
        store.addVector("v2", createRandomVector(DIMENSION), new HashMap<>());
        store.addVector("v3", createRandomVector(DIMENSION), new HashMap<>());
        
        int deleted = store.deleteVectors(Arrays.asList("v1", "v3", "nonexistent"));
        
        assertEquals(2, deleted);
        assertEquals(1, store.getVectorCount());
    }

    @Test
    @DisplayName("测试清空存储")
    void testClear() {
        store.addVector("v1", createRandomVector(DIMENSION), new HashMap<>());
        store.addVector("v2", createRandomVector(DIMENSION), new HashMap<>());
        
        store.clear();
        
        assertEquals(0, store.getVectorCount());
        assertTrue(store.isModified());
    }

    @Test
    @DisplayName("测试保存和加载")
    void testSaveAndLoad() {
        // 添加一些数据
        float[] v1 = createRandomVector(DIMENSION);
        float[] v2 = createRandomVector(DIMENSION);
        store.addVector("v1", v1, Map.of("name", "first"));
        store.addVector("v2", v2, Map.of("name", "second"));
        
        // 保存
        String savePath = tempDir.resolve("vectors.dat").toString();
        store.save(savePath);
        assertFalse(store.isModified());
        
        // 创建新存储并加载
        PersistentVectorStore newStore = new PersistentVectorStore(DIMENSION);
        newStore.initialize();
        newStore.load(savePath);
        
        // 验证数据
        assertEquals(2, newStore.getVectorCount());
        
        // 验证搜索结果
        List<VectorStore.SearchResult> results = newStore.search(v1, 1);
        assertEquals("v1", results.get(0).getId());
        assertEquals("first", results.get(0).getMetadata().get("name"));
        
        newStore.close();
    }

    @Test
    @DisplayName("测试保存路径为空时抛出异常")
    void testSaveWithNullPath() {
        assertThrows(IllegalArgumentException.class, () -> store.save(null));
        assertThrows(IllegalArgumentException.class, () -> store.save(""));
        assertThrows(IllegalArgumentException.class, () -> store.save("   "));
    }

    @Test
    @DisplayName("测试加载路径为空时抛出异常")
    void testLoadWithNullPath() {
        assertThrows(IllegalArgumentException.class, () -> store.load(null));
        assertThrows(IllegalArgumentException.class, () -> store.load(""));
        assertThrows(IllegalArgumentException.class, () -> store.load("   "));
    }

    @Test
    @DisplayName("测试加载不存在的文件")
    void testLoadNonExistentFile() {
        // 不应抛出异常，只是不加载
        store.load(tempDir.resolve("nonexistent.dat").toString());
        assertEquals(0, store.getVectorCount());
    }

    @Test
    @DisplayName("测试维度不匹配时加载失败")
    void testLoadDimensionMismatch() {
        // 创建128维存储并保存
        PersistentVectorStore store128 = new PersistentVectorStore(128);
        store128.initialize();
        store128.addVector("v1", createRandomVector(128), new HashMap<>());
        
        String savePath = tempDir.resolve("dim128.dat").toString();
        store128.save(savePath);
        store128.close();
        
        // 用64维存储尝试加载
        PersistentVectorStore store64 = new PersistentVectorStore(64);
        store64.initialize();
        
        assertThrows(IllegalStateException.class, () -> store64.load(savePath));
        
        store64.close();
    }

    @Test
    @DisplayName("测试支持持久化")
    void testSupportsPersistence() {
        assertTrue(store.supportsPersistence());
    }

    @Test
    @DisplayName("测试获取维度")
    void testGetDimension() {
        assertEquals(DIMENSION, store.getDimension());
    }

    @Test
    @DisplayName("测试自动保存路径")
    void testAutoSavePath() {
        String autoSavePath = tempDir.resolve("auto.dat").toString();
        PersistentVectorStore autoStore = new PersistentVectorStore(DIMENSION, autoSavePath);
        autoStore.initialize();
        
        autoStore.addVector("v1", createRandomVector(DIMENSION), new HashMap<>());
        assertTrue(autoStore.isModified());
        
        // 关闭时应自动保存
        autoStore.close();
        
        // 重新打开应自动加载
        PersistentVectorStore reloadedStore = new PersistentVectorStore(DIMENSION, autoSavePath);
        reloadedStore.initialize();
        
        assertEquals(1, reloadedStore.getVectorCount());
        
        reloadedStore.close();
    }

    /**
     * 创建随机向量用于测试
     */
    private float[] createRandomVector(int dimension) {
        Random random = new Random();
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat() * 2 - 1; // -1 到 1 之间
        }
        // 归一化
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < dimension; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
