package io.leavesfly.evox.core.module;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * BaseModule类测试
 */
class BaseModuleTest {

    static class TestModule extends BaseModule {
        private String name;
        private Integer value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }

    @Test
    void testToJson() {
        TestModule module = new TestModule();
        module.setName("test");
        module.setValue(42);
        
        String json = module.toJson();
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("42"));
    }

    @Test
    void testFromJson() {
        String json = "{\"className\":\"TestModule\",\"version\":0,\"name\":\"test\",\"value\":42}";
        TestModule module = BaseModule.fromJson(json, TestModule.class);
        
        assertNotNull(module);
        assertEquals("test", module.getName());
        assertEquals(42, module.getValue());
    }

    @Test
    void testToDict() {
        TestModule module = new TestModule();
        module.setName("test");
        module.setValue(100);
        
        Map<String, Object> dict = module.toDict();
        assertNotNull(dict);
        assertEquals("test", dict.get("name"));
        assertEquals(100, dict.get("value"));
    }

    @Test
    void testCopy() {
        TestModule original = new TestModule();
        original.setName("original");
        original.setValue(99);
        
        TestModule copy = original.copy();
        assertNotNull(copy);
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getValue(), copy.getValue());
        assertNotSame(original, copy);
    }
}
