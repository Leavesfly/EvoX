# EvoX 快速开始指南

## 📋 前置要求

- ✅ JDK 17 或更高版本
- ✅ Maven 3.8 或更高版本
- ✅ OpenAI API Key (或其他 LLM 提供商的 API Key)

## 🚀 5 分钟快速开始

### 步骤 1: 克隆项目

```bash
git clone https://github.com/your-org/evox.git
cd evox
```

### 步骤 2: 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入你的 API Key
nano .env  # 或使用你喜欢的编辑器
```

**最小配置**:
```bash
OPENAI_API_KEY=sk-your-actual-api-key-here
```

### 步骤 3: 编译项目

```bash
# 快速编译(跳过测试)
mvn clean install -DskipTests

# 完整编译(包含测试)
mvn clean install
```

### 步骤 4: 运行示例

```bash
# 运行简单聊天机器人示例
cd evox-application/evox-examples
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
```

## 📝 配置说明

### 方式 1: 使用环境变量 (推荐)

创建 `.env` 文件:
```bash
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL=gpt-4o-mini
LLM_TEMPERATURE=0.7
```

### 方式 2: 使用配置文件

编辑 `evox-application/evox-examples/src/main/resources/application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-api-key-here
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
```

### 方式 3: 使用系统属性

```bash
mvn exec:java -Dexec.mainClass="..." \
  -DOPENAI_API_KEY=sk-xxx \
  -DOPENAI_MODEL=gpt-4o-mini
```

## 💡 示例程序

### 1. 简单聊天机器人

```java
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.models.openai.OpenAILLM;
import io.leavesfly.evox.core.message.Message;

public class SimpleChatBot {
    public static void main(String[] args) {
        // 创建 LLM
        OpenAILLM llm = new OpenAILLM();
        llm.setApiKey(System.getenv("OPENAI_API_KEY"));
        
        // 创建 Agent
        Agent agent = new Agent();
        agent.setName("ChatBot");
        agent.setLlm(llm);
        agent.initModule();
        
        // 发送消息
        Message response = agent.chat("你好，请介绍一下自己");
        System.out.println(response.getContent());
    }
}
```

运行:
```bash
mvn exec:java -Dexec.mainClass="io.leavesfly.evox.examples.SimpleChatBot"
```

### 2. 带记忆的对话

```java
import io.leavesfly.evox.memory.shortterm.ShortTermMemory;

ShortTermMemory memory = new ShortTermMemory();
memory.setCapacity(100);
memory.setWindowSize(10);
memory.initModule();

agent.setMemory(memory);

// 多轮对话
agent.chat("我叫张三");
agent.chat("我最喜欢的颜色是蓝色");
agent.chat("你还记得我叫什么名字吗？");  // Agent 能记住
```

### 3. 使用工具

```java
import io.leavesfly.evox.tools.base.Toolkit;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.calculator.CalculatorTool;

Toolkit toolkit = new Toolkit();
toolkit.addTool(new FileSystemTool());
toolkit.addTool(new CalculatorTool());

agent.setToolkit(toolkit);
agent.chat("帮我计算 123 + 456");
```

## 🔍 故障排查

### 问题 1: API Key 未配置

**错误**: `ConfigurationException: OpenAI API key is not configured`

**解决**:
```bash
# 检查环境变量
echo $OPENAI_API_KEY

# 或在 .env 文件中配置
OPENAI_API_KEY=sk-xxx
```

### 问题 2: 依赖下载失败

**错误**: `Failed to download spring-ai-bom`

**解决**:
```bash
# 清理并重新下载
mvn clean
rm -rf ~/.m2/repository/org/springframework/ai
mvn install
```

### 问题 3: 编译错误

**错误**: `java: error: release version 17 not supported`

**解决**:
```bash
# 检查 Java 版本
java -version

# 应该是 17 或更高
# 如果不是，请安装 JDK 17+
```

### 问题 4: 测试失败

**解决**: 目前测试覆盖不足，可以跳过测试
```bash
mvn clean install -DskipTests
```

## 📚 下一步

1. 查看更多示例: [evox-examples](evox-application/evox-examples/README.md)
2. 了解架构设计: [ARCHITECTURE.md](ARCHITECTURE.md)
3. 查看功能状态: [FEATURE_STATUS.md](FEATURE_STATUS.md)
4. 阅读 API 文档: [Wiki](https://github.com/your-org/evox/wiki)

## 🐛 遇到问题?

- 提交 Issue: https://github.com/your-org/evox/issues
- 查看文档: https://github.com/your-org/evox/wiki
- 加入讨论: https://github.com/your-org/evox/discussions

## 🤝 参与贡献

我们欢迎各种形式的贡献！优先级任务：

1. **P0 级别** (高优先级):
   - 补充单元测试
   - 添加配置验证
   - 修复文档错误

2. **P1 级别** (中优先级):
   - 实现未完成的功能
   - 添加集成测试
   - 完善示例代码

详见: [贡献指南](CONTRIBUTING.md)

---

**祝你使用愉快！** 🎉

如果觉得有帮助，请给我们一个 ⭐️ Star！
