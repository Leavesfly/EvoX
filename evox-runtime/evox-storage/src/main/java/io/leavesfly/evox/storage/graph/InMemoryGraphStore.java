package io.leavesfly.evox.storage.graph;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存图存储实现
 * 
 * 基于内存的图数据存储实现，使用 ConcurrentHashMap 保证线程安全性。
 * 适用于开发、测试和轻量级应用场景。
 * 
 * 核心特性：
 * - 节点和关系的内存存储
 * - 自动 ID 生成机制
 * - 支持节点标签和属性匹配查询
 * - 删除节点时自动清理关联关系
 * - 线程安全的并发访问
 * 
 * @author EvoX Team
 * @see GraphStore
 * @see Node
 * @see Relationship
 */
@Slf4j
public class InMemoryGraphStore implements GraphStore {

    // ==================== 数据存储 ====================
    
    /**
     * 节点存储表（节点 ID -> 节点对象）
     * 使用 ConcurrentHashMap 保证多线程并发访问的线程安全性
     */
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();

    /**
     * 关系存储表（关系 ID -> 关系对象）
     * 使用 ConcurrentHashMap 保证多线程并发访问的线程安全性
     */
    private final Map<String, Relationship> relationships = new ConcurrentHashMap<>();

    // ==================== ID 生成器 ====================
    
    /**
     * 节点 ID 生成器
     * 使用 AtomicLong 保证并发环境下的 ID 唯一性和递增性
     */
    private final AtomicLong nodeIdGenerator = new AtomicLong(0);

    /**
     * 关系 ID 生成器
     * 使用 AtomicLong 保证并发环境下的 ID 唯一性和递增性
     */
    private final AtomicLong relationshipIdGenerator = new AtomicLong(0);

    // ==================== 状态标识 ====================
    
    /**
     * 存储初始化状态标识
     * true 表示已初始化，false 表示未初始化
     */
    private boolean initialized = false;

    // ==================== 生命周期管理 ====================

    /**
     * 初始化图存储
     * 
     * 清空所有节点和关系数据，重置 ID 生成器。
     * 如果已经初始化过，则不会重复初始化。
     */
    @Override
    public void initialize() {
        if (!initialized) {
            clearAllData();
            resetIdGenerators();
            initialized = true;
            log.info("InMemoryGraphStore 已初始化");
        }
    }

    /**
     * 关闭图存储
     * 
     * 清空所有数据并将初始化状态设置为 false。
     * 关闭后可以重新调用 initialize() 进行初始化。
     */
    @Override
    public void close() {
        clearAllData();
        initialized = false;
        log.info("InMemoryGraphStore 已关闭");
    }

    /**
     * 清空图存储中的所有数据
     * 
     * 删除所有节点和关系，并重置 ID 生成器。
     * 与 close() 的区别在于：clear() 后仍保持初始化状态。
     */
    @Override
    public void clear() {
        clearAllData();
        resetIdGenerators();
        log.info("InMemoryGraphStore 已清空");
    }

    /**
     * 检查存储是否已初始化
     * 
     * @return true 表示已初始化，false 表示未初始化
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 清空所有节点和关系数据
     */
    private void clearAllData() {
        nodes.clear();
        relationships.clear();
    }

    /**
     * 重置 ID 生成器为初始状态
     */
    private void resetIdGenerators() {
        nodeIdGenerator.set(0);
        relationshipIdGenerator.set(0);
    }

    // ==================== 节点操作 ====================

    /**
     * 创建新节点
     * 
     * 如果存储未初始化，会自动调用 initialize() 进行初始化。
     * 生成的节点 ID 格式为 "node_序号"，序号从 1 开始递增。
     * 
     * @param labels 节点标签列表（可为空）
     * @param properties 节点属性键值对（可为空）
     * @return 新创建的节点 ID
     */
    @Override
    public String createNode(List<String> labels, Map<String, Object> properties) {
        ensureInitialized();

        String nodeId = generateNodeId();
        Node node = createNewNode(nodeId, labels, properties);
        nodes.put(nodeId, node);
        
        log.debug("创建节点：{}，标签：{}", nodeId, labels);
        return nodeId;
    }

    // ==================== 关系操作 ====================

    /**
     * 创建两个节点之间的关系
     * 
     * 如果存储未初始化，会自动调用 initialize() 进行初始化。
     * 要求源节点和目标节点都必须已存在。
     * 生成的关系 ID 格式为 "rel_序号"，序号从 1 开始递增。
     * 
     * @param fromNodeId 源节点 ID
     * @param toNodeId 目标节点 ID
     * @param relationshipType 关系类型
     * @param properties 关系属性键值对（可为空）
     * @return 新创建的关系 ID
     * @throws IllegalArgumentException 当源节点或目标节点不存在时抛出
     */
    @Override
    public String createRelationship(String fromNodeId, String toNodeId, 
                                    String relationshipType, Map<String, Object> properties) {
        ensureInitialized();

        validateNodesExist(fromNodeId, toNodeId);

        String relId = generateRelationshipId();
        Relationship relationship = createNewRelationship(relId, fromNodeId, toNodeId, relationshipType, properties);
        relationships.put(relId, relationship);
        
        log.debug("创建关系：{}，从 {} 到 {}，类型：{}", relId, fromNodeId, toNodeId, relationshipType);
        return relId;
    }

    // ==================== 内部创建方法 ====================

    /**
     * 生成唯一的节点 ID
     * 
     * @return 格式为 "node_序号" 的唯一 ID
     */
    private String generateNodeId() {
        return "node_" + nodeIdGenerator.incrementAndGet();
    }

    /**
     * 创建新的节点对象
     * 
     * @param nodeId 节点 ID
     * @param labels 节点标签列表
     * @param properties 节点属性键值对
     * @return 新创建的节点对象
     */
    private Node createNewNode(String nodeId, List<String> labels, Map<String, Object> properties) {
        return new Node(nodeId, new ArrayList<>(labels), new HashMap<>(properties));
    }

    /**
     * 生成唯一的关系 ID
     * 
     * @return 格式为 "rel_序号" 的唯一 ID
     */
    private String generateRelationshipId() {
        return "rel_" + relationshipIdGenerator.incrementAndGet();
    }

    /**
     * 创建新的关系对象
     * 
     * @param relId 关系 ID
     * @param fromNodeId 源节点 ID
     * @param toNodeId 目标节点 ID
     * @param relationshipType 关系类型
     * @param properties 关系属性键值对
     * @return 新创建的关系对象
     */
    private Relationship createNewRelationship(String relId, String fromNodeId, String toNodeId, 
                                               String relationshipType, Map<String, Object> properties) {
        return new Relationship(relId, relationshipType, fromNodeId, toNodeId, new HashMap<>(properties));
    }

    /**
     * 验证指定的节点是否存在
     * 
     * @param fromNodeId 源节点 ID
     * @param toNodeId 目标节点 ID
     * @throws IllegalArgumentException 当任一节点不存在时抛出
     */
    private void validateNodesExist(String fromNodeId, String toNodeId) {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            throw new IllegalArgumentException("源节点和目标节点必须已存在");
        }
    }

    // ==================== 查询操作 ====================

    /**
     * 查找符合条件的节点
     * 
     * 如果存储未初始化，会自动调用 initialize() 进行初始化。
     * 返回所有包含指定标签和属性匹配的节点。
     * 
     * @param labels 要匹配的标签列表（可为空或空列表，表示不限制标签）
     * @param properties 要匹配的属性键值对（可为空或空映射，表示不限制属性）
     * @return 符合条件的节点列表
     */
    @Override
    public List<Node> findNodes(List<String> labels, Map<String, Object> properties) {
        ensureInitialized();

        return nodes.values().stream()
            .filter(node -> matchesLabels(node, labels))
            .filter(node -> matchesProperties(node.getProperties(), properties))
            .collect(Collectors.toList());
    }

    /**
     * 执行 Cypher 查询
     * 
     * 当前为简化实现，尚未完全支持 Cypher 查询语法。
     * 
     * @param query Cypher 查询语句
     * @param parameters 查询参数
     * @return 查询结果列表（当前始终返回空列表）
     */
    @Override
    public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
        log.warn("InMemoryGraphStore 尚未完全实现 Cypher 查询功能");
        return new ArrayList<>();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 确保存储已初始化
     * 
     * 如果未初始化，则自动调用 initialize() 方法进行初始化。
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    // ==================== 删除操作 ====================

    /**
     * 删除指定节点及其关联的所有关系
     * 
     * 如果存储未初始化，返回 false。
     * 删除节点时会自动清理所有与该节点相关的关系（包括作为源节点和目标节点的关系）。
     * 
     * @param nodeId 要删除的节点 ID
     * @return true 表示删除成功，false 表示节点不存在或删除失败
     */
    @Override
    public boolean deleteNode(String nodeId) {
        if (!initialized) {
            return false;
        }

        // 查找并删除所有关联关系
        List<String> relatedRelationships = findRelatedRelationships(nodeId);
        removeRelationships(relatedRelationships);

        // 删除节点
        boolean removed = nodes.remove(nodeId) != null;
        if (removed) {
            log.debug("删除节点：{}，同时删除了 {} 个关联关系", nodeId, relatedRelationships.size());
        }
        return removed;
    }

    /**
     * 删除指定的关系
     * 
     * 如果存储未初始化，返回 false。
     * 
     * @param relationshipId 要删除的关系 ID
     * @return true 表示删除成功，false 表示关系不存在或删除失败
     */
    @Override
    public boolean deleteRelationship(String relationshipId) {
        if (!initialized) {
            return false;
        }

        boolean removed = relationships.remove(relationshipId) != null;
        if (removed) {
            log.debug("删除关系：{}", relationshipId);
        }
        return removed;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 查找与指定节点相关的所有关系
     * 
     * @param nodeId 节点 ID
     * @return 相关关系的 ID 列表
     */
    private List<String> findRelatedRelationships(String nodeId) {
        return relationships.values().stream()
            .filter(rel -> rel.getFromNodeId().equals(nodeId) || rel.getToNodeId().equals(nodeId))
            .map(Relationship::getId)
            .collect(Collectors.toList());
    }

    /**
     * 批量删除关系
     * 
     * @param relationshipIds 要删除的关系 ID 列表
     */
    private void removeRelationships(List<String> relationshipIds) {
        relationshipIds.forEach(relationships::remove);
    }

    // ==================== 匹配规则辅助方法 ====================

    /**
     * 检查节点的标签是否与查询条件匹配
     * 
     * 如果查询标签列表为 null 或空，则认为所有节点都匹配。
     * 否则，节点必须包含所有查询标签才算匹配。
     * 
     * @param node 待检查的节点
     * @param labels 查询标签列表
     * @return true 表示标签匹配，false 表示不匹配
     */
    private boolean matchesLabels(Node node, List<String> labels) {
        // 空标签列表表示不限制
        if (labels == null || labels.isEmpty()) {
            return true;
        }
        // 节点必须包含所有查询标签
        return node.getLabels().containsAll(labels);
    }

    /**
     * 检查节点的属性是否与查询条件匹配
     * 
     * 如果查询属性映射为 null 或空，则认为所有节点都匹配。
     * 否则，节点必须在相同键上具有相同的值才算匹配。
     * 
     * @param nodeProps 节点的属性映射
     * @param queryProps 查询的属性映射
     * @return true 表示属性匹配，false 表示不匹配
     */
    private boolean matchesProperties(Map<String, Object> nodeProps, Map<String, Object> queryProps) {
        // 空属性映射表示不限制
        if (queryProps == null || queryProps.isEmpty()) {
            return true;
        }

        // 逐个检查查询属性
        for (Map.Entry<String, Object> entry : queryProps.entrySet()) {
            Object nodeValue = nodeProps.get(entry.getKey());
            // 属性值不存在或不相等则不匹配
            if (nodeValue == null || !nodeValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    // ==================== 调试和统计方法 ====================

    /**
     * 根据节点 ID 获取节点对象
     * 
     * 主要用于测试和调试场景。
     * 
     * @param nodeId 节点 ID
     * @return 节点对象，不存在时返回 null
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 根据关系 ID 获取关系对象
     * 
     * 主要用于测试和调试场景。
     * 
     * @param relationshipId 关系 ID
     * @return 关系对象，不存在时返回 null
     */
    public Relationship getRelationship(String relationshipId) {
        return relationships.get(relationshipId);
    }

    /**
     * 获取图中节点的总数量
     * 
     * @return 节点数量
     */
    public long getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取图中关系的总数量
     * 
     * @return 关系数量
     */
    public long getRelationshipCount() {
        return relationships.size();
    }
}
