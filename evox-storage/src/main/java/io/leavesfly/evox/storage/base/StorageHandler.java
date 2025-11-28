package io.leavesfly.evox.storage.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 存储处理器接口 - 提供数据持久化的抽象
 * 对应 Python 版本的 StorageHandler
 */
public interface StorageHandler {

    /**
     * 保存数据到指定表
     *
     * @param table 表名
     * @param id 记录ID
     * @param data 数据
     */
    void save(String table, String id, Map<String, Object> data);

    /**
     * 批量保存数据
     *
     * @param table 表名
     * @param records 记录列表（ID到数据的映射）
     */
    void saveAll(String table, Map<String, Map<String, Object>> records);

    /**
     * 从指定表加载数据
     *
     * @param table 表名
     * @param id 记录ID
     * @return 数据，不存在则返回空
     */
    Optional<Map<String, Object>> load(String table, String id);

    /**
     * 加载表中所有数据
     *
     * @param table 表名
     * @return ID到数据的映射
     */
    Map<String, Map<String, Object>> loadAll(String table);

    /**
     * 加载多个表的所有数据
     *
     * @param tables 表名列表
     * @return 表名到数据映射的映射
     */
    Map<String, Map<String, Map<String, Object>>> loadTables(List<String> tables);

    /**
     * 删除指定记录
     *
     * @param table 表名
     * @param id 记录ID
     * @return 是否成功删除
     */
    boolean delete(String table, String id);

    /**
     * 删除表中所有数据
     *
     * @param table 表名
     */
    void deleteAll(String table);

    /**
     * 检查记录是否存在
     *
     * @param table 表名
     * @param id 记录ID
     * @return 是否存在
     */
    boolean exists(String table, String id);

    /**
     * 获取表中记录数量
     *
     * @param table 表名
     * @return 记录数量
     */
    int count(String table);

    /**
     * 清空所有表
     */
    void clearAll();

    /**
     * 获取所有表名
     *
     * @return 表名列表
     */
    List<String> getTables();
}
