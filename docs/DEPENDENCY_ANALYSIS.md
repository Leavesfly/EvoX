# EvoX 项目依赖分析报告

## 一、分析概述

本报告对 EvoX 项目的 Maven 依赖进行全面分析，识别可移除或优化的依赖项。

---

## 二、可移除的依赖

### 1. 根 pom.xml - dependencyManagement 中的死引用

以下模块在 `dependencyManagement` 中声明，但**项目中不存在对应模块**（未在 `<modules>` 中列出）：

| 依赖 | 说明 |
|------|------|
| `evox-channels` | 模块不存在，仅文档中有提及 |
| `evox-scheduler` | 模块不存在 |
| `evox-gateway` | 模块不存在 |

**建议**：从根 `pom.xml` 的 `dependencyManagement` 中移除这三项。

---

### 2. 根 pom.xml - 未被任何模块引用的依赖

以下依赖在 `dependencyManagement` 中定义，但**没有任何子模块声明使用**：

| 依赖 | 说明 |
|------|------|
| `org.hsqldb:hsqldb` | 无模块引用，代码中无使用 |
| `org.codehaus.groovy:groovy-jsr223` | 无模块引用。CodeInterpreterTool 支持 groovy 选项，但 evox-tools 未声明此依赖，且 ScriptEngine 需运行时提供 |
| `org.mapstruct:mapstruct` | 无模块引用，代码中无 @Mapper/@Mapping 注解 |

**说明**：
- **hsqldb**：若未来需要 HSQLDB 支持可保留；否则可移除
- **groovy-jsr223**：CodeInterpreterTool 的 groovy 选项在无此依赖时无法工作，可考虑移除该选项或添加 optional 依赖
- **mapstruct**：仅用于 annotationProcessorPaths，无实际 Mapper 使用，可移除

---

### 3. evox-core - Guava

| 依赖 | 使用情况 |
|------|----------|
| `com.google.guava:guava` | **未使用** - 全项目无 `com.google` 包导入 |

**建议**：从 `evox-core/pom.xml` 移除 Guava 依赖。

---

### 4. evox-core - slf4j-api（冗余）

| 依赖 | 说明 |
|------|------|
| `org.slf4j:slf4j-api` | **冗余** - `spring-boot-starter` 已传递引入 logback-classic → slf4j-api |

**建议**：可移除显式声明的 slf4j-api，使用 Spring Boot 传递依赖即可。

---

### 5. evox-runtime/evox-rag - 未使用的依赖

| 依赖 | 使用情况 |
|------|----------|
| `dev.langchain4j:langchain4j-embeddings` | **未使用** - 嵌入服务使用 `SpringAIEmbeddingService`（基于 evox-models 的 OpenAI 兼容客户端），无 langchain4j 导入 |
| `org.apache.commons:commons-text` | **未使用** - 无 `org.apache.commons` 包导入 |
| `org.apache.poi:poi-ooxml` | **未使用** - `UniversalDocumentReader` 仅注册 Text 和 PDF 读取器，无 Office 文档（docx/xlsx）读取器实现 |

**建议**：从 `evox-rag/pom.xml` 移除上述三项。

---

### 6. evox-application/evox-cowork - 未使用的依赖

| 依赖 | 使用情况 |
|------|----------|
| `org.apache.poi:poi-ooxml` | **未使用** - 无 `org.apache.poi` 包导入 |
| `org.apache.pdfbox:pdfbox` | **未使用** - 无 `org.apache.pdfbox` 包导入。evox-cowork 依赖 evox-rag，但 evox-rag 的 PDF 能力通过 RAG 使用，cowork 自身无直接 PDF 解析代码 |

**建议**：从 `evox-cowork/pom.xml` 移除 poi-ooxml 和 pdfbox。若计划支持 Office/PDF 文档处理，可保留并补充实现。

---

### 7. 应用层 - 冗余的 slf4j / logback

以下模块显式声明了 `slf4j-api` 和 `logback-classic`，但已通过 `spring-boot-starter` 传递引入：

| 模块 | 冗余依赖 |
|------|----------|
| evox-examples | slf4j-api, logback-classic |
| evox-benchmark | slf4j-api, logback-classic |
| evox-claudecode | slf4j-api, logback-classic |
| evox-cowork | slf4j-api, logback-classic |

**说明**：evox-claudecode 和 evox-cowork 未使用 spring-boot-starter，需单独确认。evox-claudecode 无 spring-boot-starter，evox-cowork 有。evox-examples 和 evox-benchmark 有 spring-boot 相关依赖，slf4j/logback 可视为冗余。

**建议**：对使用 spring-boot-starter 的模块，可移除显式 slf4j-api 和 logback-classic。

---

## 三、需保留的依赖（说明）

| 依赖 | 说明 |
|------|------|
| `com.h2database:h2` | DatabaseTool 支持 h2，需用户在使用 h2 时提供驱动。当前无模块直接声明，可保留在 dependencyManagement 供应用层按需引用 |
| `org.apache.pdfbox:pdfbox` (evox-rag) | **已使用** - PdfDocumentReader 使用 |
| `org.mongodb:mongodb-driver-sync` | **已使用** - MongoDBTool、evox-storage 使用 |
| `org.yaml:snakeyaml` (evox-agents) | **已使用** - SKILL.md frontmatter 解析 |
| `io.projectreactor:reactor-core` | **已使用** - 全项目响应式编程 |
| `jackson` 系列 | **已使用** - JSON 序列化 |

---

## 四、依赖优化建议汇总

### 高优先级（建议移除）

1. **evox-core**：移除 Guava
2. **evox-rag**：移除 langchain4j-embeddings、commons-text、poi-ooxml
3. **evox-cowork**：移除 poi-ooxml、pdfbox（若确认无计划使用）
4. **根 pom**：移除 evox-channels、evox-scheduler、evox-gateway 的 dependencyManagement 条目

### 中优先级（可选）

1. **根 pom**：移除 hsqldb、mapstruct、groovy-jsr223（若确认无使用场景）
2. **evox-core**：移除显式 slf4j-api
3. **evox-examples / evox-benchmark**：移除显式 slf4j-api、logback-classic

### 可选依赖（optional）建议

- evox-tools 的 `spring-boot-starter-web`、`spring-boot-starter-jdbc`、`mongodb-driver-sync` 已正确标记为 optional
- evox-storage 的 jdbc、mongodb 同样为 optional，合理

---

## 五、移除依赖后的验证步骤

1. 执行 `mvn clean compile` 确保编译通过
2. 执行 `mvn clean test` 确保测试通过
3. 检查各应用模块（evox-examples、evox-claudecode、evox-cowork）的启动与运行

---

## 六、变更记录

**2025-03-11 已执行优化：**

- 根 pom：移除 evox-channels、evox-scheduler、evox-gateway、mapstruct、guava、hsqldb、groovy-jsr223（及对应 properties）
- evox-core：移除 Guava、slf4j-api
- evox-rag：移除 langchain4j-embeddings、commons-text、poi-ooxml
- evox-cowork：移除 poi-ooxml、pdfbox
- evox-examples、evox-benchmark、evox-claudecode、evox-cowork：移除冗余 slf4j-api、logback-classic

**验证结果**：`mvn clean compile -pl '!evox-application/evox-examples'` 及 `mvn test` 均通过。  
evox-examples 存在既有编译问题（Workflow 类型、SEWOptimizer 枚举），与本次依赖优化无关。

---

*报告生成时间：2025-03-11*
