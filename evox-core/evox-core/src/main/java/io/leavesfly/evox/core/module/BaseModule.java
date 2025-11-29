package io.leavesfly.evox.core.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.leavesfly.evox.core.exception.ModuleException;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块基类
 * 所有模块的基础抽象类,提供序列化/反序列化、持久化等通用能力
 *
 * @author EvoX Team
 */
@Data
@Slf4j
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseModule {

    /**
     * 类名标识
     */
    private String className;

    /**
     * 版本号
     */
    private Integer version = 0;

    /**
     * Jackson ObjectMapper
     */
    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 构造函数,自动设置类名
     */
    public BaseModule() {
        this.className = this.getClass().getSimpleName();
    }

    /**
     * 模块初始化方法
     * 子类可以重写此方法进行自定义初始化
     */
    public void initModule() {
        // 默认实现为空,子类可以覆盖
    }

    /**
     * 转换为Map
     *
     * @return Map表示
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toDict() {
        return OBJECT_MAPPER.convertValue(this, Map.class);
    }

    /**
     * 转换为JSON字符串
     *
     * @return JSON字符串
     * @throws ModuleException 如果序列化失败
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert to JSON", e);
            throw new ModuleException("Failed to serialize module to JSON: " + this.getClass().getSimpleName(), e, this);
        }
    }

    /**
     * 转换为格式化的JSON字符串
     *
     * @return 格式化的JSON字符串
     * @throws ModuleException 如果序列化失败
     */
    public String toPrettyJson() {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert to pretty JSON", e);
            throw new ModuleException("Failed to serialize module to pretty JSON: " + this.getClass().getSimpleName(), e, this);
        }
    }

    /**
     * 从Map创建实例
     *
     * @param data Map数据
     * @param clazz 目标类
     * @param <T> 类型参数
     * @return 实例
     * @throws ModuleException 如果反序列化失败
     */
    public static <T extends BaseModule> T fromDict(Map<String, Object> data, Class<T> clazz) {
        try {
            T instance = OBJECT_MAPPER.convertValue(data, clazz);
            instance.initModule();
            return instance;
        } catch (Exception e) {
            log.error("Failed to create instance from dict", e);
            throw new ModuleException("Failed to create instance from dict for " + clazz.getSimpleName(), e, data);
        }
    }

    /**
     * 从JSON字符串创建实例
     *
     * @param json JSON字符串
     * @param clazz 目标类
     * @param <T> 类型参数
     * @return 实例
     * @throws ModuleException 如果反序列化失败
     */
    public static <T extends BaseModule> T fromJson(String json, Class<T> clazz) {
        try {
            T instance = OBJECT_MAPPER.readValue(json, clazz);
            instance.initModule();
            return instance;
        } catch (JsonProcessingException e) {
            log.error("Failed to create instance from JSON", e);
            throw new ModuleException("Failed to create instance from JSON for " + clazz.getSimpleName(), e, json);
        }
    }

    /**
     * 保存模块到文件
     *
     * @param filePath 文件路径
     * @throws ModuleException 如果保存失败
     */
    public void saveModule(Path filePath) {
        try {
            String json = toPrettyJson();
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, json);
            log.info("Saved module to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save module to file: {}", filePath, e);
            throw new ModuleException("Failed to save module to file: " + filePath, e, this);
        }
    }

    /**
     * 从文件加载模块
     *
     * @param filePath 文件路径
     * @param clazz 目标类
     * @param <T> 类型参数
     * @return 实例
     * @throws ModuleException 如果加载失败
     */
    public static <T extends BaseModule> T loadModule(Path filePath, Class<T> clazz) {
        try {
            String json = Files.readString(filePath);
            T instance = fromJson(json, clazz);
            log.info("Loaded module from: {}", filePath);
            return instance;
        } catch (IOException e) {
            log.error("Failed to load module from file: {}", filePath, e);
            throw new ModuleException("Failed to load module from file: " + filePath, e, filePath.toString());
        }
    }

    /**
     * 复制当前模块
     *
     * @return 新实例
     * @throws ModuleException 如果复制失败
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseModule> T copy() {
        try {
            String json = toJson();
            return (T) fromJson(json, this.getClass());
        } catch (Exception e) {
            log.error("Failed to copy module", e);
            throw new ModuleException("Failed to copy module: " + this.getClass().getSimpleName(), e, this);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}
