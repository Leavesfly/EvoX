package io.leavesfly.evox.tools.database;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * MongoDB数据库工具
 * 提供MongoDB数据库的CRUD操作
 * 
 * @author EvoX Team
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MongoDBTool extends BaseTool {

    /**
     * MongoDB连接字符串
     */
    private String connectionString;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * MongoDB客户端
     */
    private transient MongoClient mongoClient;

    public MongoDBTool(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.name = "mongodb_tool";
        this.description = "MongoDB database operations including query, insert, update, delete";
        initializeSchema();
    }

    @Override
    public void initModule() {
        super.initModule();
        initializeClient();
    }

    private void initializeSchema() {
        Map<String, Map<String, String>> inputs = new HashMap<>();
        
        inputs.put("operation", Map.of(
            "type", "string",
            "description", "Operation to perform: find, insert, update, delete, count",
            "enum", "find,insert,update,delete,count"
        ));
        
        inputs.put("collection", Map.of(
            "type", "string",
            "description", "Collection name"
        ));
        
        inputs.put("filter", Map.of(
            "type", "object",
            "description", "Query filter (for find, update, delete, count operations)"
        ));
        
        inputs.put("document", Map.of(
            "type", "object",
            "description", "Document to insert or update data"
        ));
        
        inputs.put("limit", Map.of(
            "type", "integer",
            "description", "Maximum number of documents to return"
        ));

        this.inputs = inputs;
        this.required = List.of("operation", "collection");
    }

    private void initializeClient() {
        if (connectionString == null || connectionString.isEmpty()) {
            log.warn("MongoDB connection string is not set");
            return;
        }

        try {
            this.mongoClient = MongoClients.create(connectionString);
            log.info("MongoDB client initialized for database: {}", databaseName);
        } catch (Exception e) {
            log.error("Failed to initialize MongoDB client", e);
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String operation = getParameter(parameters, "operation", "");
            String collectionName = getParameter(parameters, "collection", "");
            
            if (mongoClient == null) {
                initializeClient();
            }
            
            if (mongoClient == null) {
                return ToolResult.failure("MongoDB client not initialized");
            }
            
            return switch (operation.toLowerCase()) {
                case "find" -> executeFind(collectionName, parameters);
                case "insert" -> executeInsert(collectionName, parameters);
                case "update" -> executeUpdate(collectionName, parameters);
                case "delete" -> executeDelete(collectionName, parameters);
                case "count" -> executeCount(collectionName, parameters);
                default -> ToolResult.failure("Unknown operation: " + operation);
            };
            
        } catch (Exception e) {
            log.error("MongoDB operation failed", e);
            return ToolResult.failure("MongoDB error: " + e.getMessage());
        }
    }

    /**
     * 查询文档
     */
    private ToolResult executeFind(String collectionName, Map<String, Object> parameters) {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Map<String, Object> filterMap = getParameter(parameters, "filter", new HashMap<>());
            Integer limit = getParameter(parameters, "limit", 100);
            
            Bson filter = createFilter(filterMap);
            
            List<Map<String, Object>> results = new ArrayList<>();
            FindIterable<Document> documents = collection.find(filter).limit(limit);
            
            for (Document doc : documents) {
                results.add(new HashMap<>(doc));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("count", results.size());
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Find operation failed", e);
            return ToolResult.failure("Find failed: " + e.getMessage());
        }
    }

    /**
     * 插入文档
     */
    private ToolResult executeInsert(String collectionName, Map<String, Object> parameters) {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Map<String, Object> document = getParameter(parameters, "document", new HashMap<>());
            
            if (document.isEmpty()) {
                return ToolResult.failure("Document is required for insert operation");
            }
            
            Document doc = new Document(document);
            collection.insertOne(doc);
            
            Map<String, Object> data = new HashMap<>();
            data.put("inserted_id", doc.get("_id").toString());
            data.put("status", "success");
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Insert operation failed", e);
            return ToolResult.failure("Insert failed: " + e.getMessage());
        }
    }

    /**
     * 更新文档
     */
    private ToolResult executeUpdate(String collectionName, Map<String, Object> parameters) {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Map<String, Object> filterMap = getParameter(parameters, "filter", new HashMap<>());
            Map<String, Object> updateData = getParameter(parameters, "document", new HashMap<>());
            
            if (updateData.isEmpty()) {
                return ToolResult.failure("Document is required for update operation");
            }
            
            Bson filter = createFilter(filterMap);
            Bson update = Updates.combine(
                updateData.entrySet().stream()
                    .map(e -> Updates.set(e.getKey(), e.getValue()))
                    .toList()
            );
            
            long modifiedCount = collection.updateMany(filter, update).getModifiedCount();
            
            Map<String, Object> data = new HashMap<>();
            data.put("modified_count", modifiedCount);
            data.put("status", "success");
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Update operation failed", e);
            return ToolResult.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    private ToolResult executeDelete(String collectionName, Map<String, Object> parameters) {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Map<String, Object> filterMap = getParameter(parameters, "filter", new HashMap<>());
            Bson filter = createFilter(filterMap);
            
            long deletedCount = collection.deleteMany(filter).getDeletedCount();
            
            Map<String, Object> data = new HashMap<>();
            data.put("deleted_count", deletedCount);
            data.put("status", "success");
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Delete operation failed", e);
            return ToolResult.failure("Delete failed: " + e.getMessage());
        }
    }

    /**
     * 统计文档数
     */
    private ToolResult executeCount(String collectionName, Map<String, Object> parameters) {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            Map<String, Object> filterMap = getParameter(parameters, "filter", new HashMap<>());
            Bson filter = createFilter(filterMap);
            
            long count = collection.countDocuments(filter);
            
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            
            return ToolResult.success(data);
            
        } catch (Exception e) {
            log.error("Count operation failed", e);
            return ToolResult.failure("Count failed: " + e.getMessage());
        }
    }

    /**
     * 创建过滤器
     */
    private Bson createFilter(Map<String, Object> filterMap) {
        if (filterMap.isEmpty()) {
            return Filters.empty();
        }
        
        List<Bson> filters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
            filters.add(Filters.eq(entry.getKey(), entry.getValue()));
        }
        
        return filters.size() == 1 ? filters.get(0) : Filters.and(filters);
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
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
