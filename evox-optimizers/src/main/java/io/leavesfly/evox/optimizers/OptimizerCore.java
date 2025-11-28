package io.leavesfly.evox.optimizers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 优化器核心
 * 提供可优化字段的注册和管理
 *
 * @author EvoX Team
 */
@Slf4j
@Data
public class OptimizerCore {

    /**
     * 可优化字段注册表
     */
    private Map<String, OptimizableField> fields;

    /**
     * 构造函数
     */
    public OptimizerCore() {
        this.fields = new HashMap<>();
    }

    /**
     * 注册可优化字段
     */
    public void registerField(String name, Function<Void, Object> getter, java.util.function.Consumer<Object> setter) {
        fields.put(name, new OptimizableField(name, getter, setter));
        log.debug("Registered optimizable field: {}", name);
    }

    /**
     * 获取字段值
     */
    public Object get(String name) {
        OptimizableField field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + name);
        }
        return field.get();
    }

    /**
     * 设置字段值
     */
    public void set(String name, Object value) {
        OptimizableField field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + name);
        }
        field.set(value);
    }

    /**
     * 可优化字段类
     */
    @Data
    public static class OptimizableField {
        private String name;
        private Function<Void, Object> getter;
        private java.util.function.Consumer<Object> setter;

        public OptimizableField(String name, Function<Void, Object> getter, java.util.function.Consumer<Object> setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
        }

        public Object get() {
            return getter.apply(null);
        }

        public void set(Object value) {
            setter.accept(value);
        }
    }
}
