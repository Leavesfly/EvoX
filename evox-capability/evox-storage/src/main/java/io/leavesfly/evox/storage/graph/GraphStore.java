package io.leavesfly.evox.storage.graph;

import io.leavesfly.evox.storage.base.BaseStorage;

import java.util.List;
import java.util.Map;

/**
 * 图存储接口
 * 定义图数据库操作的统一接口
 * 
 * @author EvoX Team
 */
public interface GraphStore extends BaseStorage {

    /**
     * 创建节点
     * 
     * @param labels 节点标签
     * @param properties 节点属性
     * @return 节点ID
     */
    String createNode(List<String> labels, Map<String, Object> properties);

    /**
     * 创建关系
     * 
     * @param fromNodeId 起始节点ID
     * @param toNodeId 目标节点ID
     * @param relationshipType 关系类型
     * @param properties 关系属性
     * @return 关系ID
     */
    String createRelationship(String fromNodeId, String toNodeId, 
                            String relationshipType, Map<String, Object> properties);

    /**
     * 查询节点
     * 
     * @param labels 节点标签
     * @param properties 查询条件
     * @return 节点列表
     */
    List<Node> findNodes(List<String> labels, Map<String, Object> properties);

    /**
     * 执行Cypher查询
     * 
     * @param query Cypher查询语句
     * @param parameters 查询参数
     * @return 查询结果
     */
    List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters);

    /**
     * 删除节点
     * 
     * @param nodeId 节点ID
     * @return 是否删除成功
     */
    boolean deleteNode(String nodeId);

    /**
     * 删除关系
     * 
     * @param relationshipId 关系ID
     * @return 是否删除成功
     */
    boolean deleteRelationship(String relationshipId);

    /**
     * 图节点类
     */
    class Node {
        private String id;
        private List<String> labels;
        private Map<String, Object> properties;

        public Node() {}

        public Node(String id, List<String> labels, Map<String, Object> properties) {
            this.id = id;
            this.labels = labels;
            this.properties = properties;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }

    /**
     * 图关系类
     */
    class Relationship {
        private String id;
        private String type;
        private String fromNodeId;
        private String toNodeId;
        private Map<String, Object> properties;

        public Relationship() {}

        public Relationship(String id, String type, String fromNodeId, 
                          String toNodeId, Map<String, Object> properties) {
            this.id = id;
            this.type = type;
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.properties = properties;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFromNodeId() {
            return fromNodeId;
        }

        public void setFromNodeId(String fromNodeId) {
            this.fromNodeId = fromNodeId;
        }

        public String getToNodeId() {
            return toNodeId;
        }

        public void setToNodeId(String toNodeId) {
            this.toNodeId = toNodeId;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}
