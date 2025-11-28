package io.leavesfly.evox.storage.graph;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存图存储实现
 * 使用内存存储图数据（适用于开发和测试）
 * 
 * @author EvoX Team
 */
@Slf4j
public class InMemoryGraphStore implements GraphStore {

    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, Relationship> relationships = new ConcurrentHashMap<>();
    private final AtomicLong nodeIdGenerator = new AtomicLong(0);
    private final AtomicLong relationshipIdGenerator = new AtomicLong(0);
    private boolean initialized = false;

    @Override
    public void initialize() {
        if (!initialized) {
            nodes.clear();
            relationships.clear();
            nodeIdGenerator.set(0);
            relationshipIdGenerator.set(0);
            initialized = true;
            log.info("InMemoryGraphStore initialized");
        }
    }

    @Override
    public void close() {
        nodes.clear();
        relationships.clear();
        initialized = false;
        log.info("InMemoryGraphStore closed");
    }

    @Override
    public void clear() {
        nodes.clear();
        relationships.clear();
        nodeIdGenerator.set(0);
        relationshipIdGenerator.set(0);
        log.info("InMemoryGraphStore cleared");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String createNode(List<String> labels, Map<String, Object> properties) {
        if (!initialized) {
            initialize();
        }

        String nodeId = "node_" + nodeIdGenerator.incrementAndGet();
        Node node = new Node(nodeId, new ArrayList<>(labels), new HashMap<>(properties));
        nodes.put(nodeId, node);
        
        log.debug("Created node: {} with labels: {}", nodeId, labels);
        return nodeId;
    }

    @Override
    public String createRelationship(String fromNodeId, String toNodeId, 
                                    String relationshipType, Map<String, Object> properties) {
        if (!initialized) {
            initialize();
        }

        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            throw new IllegalArgumentException("Both nodes must exist");
        }

        String relId = "rel_" + relationshipIdGenerator.incrementAndGet();
        Relationship relationship = new Relationship(
            relId, relationshipType, fromNodeId, toNodeId, new HashMap<>(properties)
        );
        relationships.put(relId, relationship);
        
        log.debug("Created relationship: {} from {} to {} of type {}", 
                 relId, fromNodeId, toNodeId, relationshipType);
        return relId;
    }

    @Override
    public List<Node> findNodes(List<String> labels, Map<String, Object> properties) {
        if (!initialized) {
            initialize();
        }

        return nodes.values().stream()
            .filter(node -> matchesLabels(node, labels))
            .filter(node -> matchesProperties(node.getProperties(), properties))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
        // 简化实现：仅支持基本查询
        log.warn("Cypher query execution is not fully implemented in InMemoryGraphStore");
        return new ArrayList<>();
    }

    @Override
    public boolean deleteNode(String nodeId) {
        if (!initialized) {
            return false;
        }

        // 删除相关的关系
        List<String> relToDelete = relationships.values().stream()
            .filter(rel -> rel.getFromNodeId().equals(nodeId) || rel.getToNodeId().equals(nodeId))
            .map(Relationship::getId)
            .collect(Collectors.toList());
        
        relToDelete.forEach(relationships::remove);

        boolean removed = nodes.remove(nodeId) != null;
        if (removed) {
            log.debug("Deleted node: {} and {} related relationships", nodeId, relToDelete.size());
        }
        return removed;
    }

    @Override
    public boolean deleteRelationship(String relationshipId) {
        if (!initialized) {
            return false;
        }

        boolean removed = relationships.remove(relationshipId) != null;
        if (removed) {
            log.debug("Deleted relationship: {}", relationshipId);
        }
        return removed;
    }

    /**
     * 检查节点标签是否匹配
     */
    private boolean matchesLabels(Node node, List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }
        return node.getLabels().containsAll(labels);
    }

    /**
     * 检查属性是否匹配
     */
    private boolean matchesProperties(Map<String, Object> nodeProps, Map<String, Object> queryProps) {
        if (queryProps == null || queryProps.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : queryProps.entrySet()) {
            Object nodeValue = nodeProps.get(entry.getKey());
            if (nodeValue == null || !nodeValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取节点（用于测试和调试）
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取关系（用于测试和调试）
     */
    public Relationship getRelationship(String relationshipId) {
        return relationships.get(relationshipId);
    }

    /**
     * 获取节点总数
     */
    public long getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取关系总数
     */
    public long getRelationshipCount() {
        return relationships.size();
    }
}
