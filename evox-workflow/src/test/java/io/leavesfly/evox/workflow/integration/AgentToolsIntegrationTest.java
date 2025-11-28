package io.leavesfly.evox.workflow.integration;

import io.leavesfly.evox.actions.base.Action;
import io.leavesfly.evox.actions.base.ActionInput;
import io.leavesfly.evox.actions.base.ActionOutput;
import io.leavesfly.evox.actions.base.SimpleActionOutput;
import io.leavesfly.evox.agents.base.Agent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.tools.base.BaseTool;
import io.leavesfly.evox.tools.file.FileSystemTool;
import io.leavesfly.evox.tools.http.HttpTool;
import io.leavesfly.evox.tools.search.WebSearchTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent + Tools 集成测试
 * 
 * 测试场景：
 * 1. Agent 使用文件系统工具
 * 2. Agent 使用 HTTP 工具
 * 3. Agent 使用搜索工具
 * 4. Agent 组合多个工具完成复杂任务
 */
@Slf4j
class AgentToolsIntegrationTest {

    @TempDir
    Path tempDir;

    private FileSystemTool fileSystemTool;
    private HttpTool httpTool;
    private WebSearchTool webSearchTool;

    @BeforeEach
    void setUp() {
        fileSystemTool = new FileSystemTool();
        httpTool = new HttpTool();
        webSearchTool = new WebSearchTool();
    }

    /**
     * 测试 1: Agent 使用文件系统工具
     */
    @Test
    void testAgentWithFileSystemTool() {
        log.info("=== 测试 Agent 使用文件系统工具 ===");
        
        // 创建文件处理 Agent
        FileAgent agent = new FileAgent(fileSystemTool, tempDir.toString());
        agent.setName("FileAgent");
        agent.setDescription("文件处理智能体");
        agent.initModule();
        
        // 测试写文件
        Message writeResult = agent.execute("writeFile", Collections.emptyList());
        assertNotNull(writeResult);
        assertEquals(MessageType.RESPONSE, writeResult.getMessageType());
        log.info("写文件结果: {}", writeResult.getContent());
        
        // 测试读文件
        Message readResult = agent.execute("readFile", Collections.emptyList());
        assertNotNull(readResult);
        assertEquals(MessageType.RESPONSE, readResult.getMessageType());
        log.info("读文件结果: {}", readResult.getContent());
    }

    /**
     * 测试 2: Agent 使用 HTTP 工具
     */
    @Test
    void testAgentWithHttpTool() {
        log.info("=== 测试 Agent 使用 HTTP 工具 ===");
        
        // 创建 HTTP Agent
        HttpAgent agent = new HttpAgent(httpTool);
        agent.setName("HttpAgent");
        agent.setDescription("HTTP 请求智能体");
        agent.initModule();
        
        // 测试 HTTP 请求
        Message result = agent.execute("fetchData", Collections.emptyList());
        assertNotNull(result);
        // HTTP 请求可能失败（网络问题），所以只验证返回了消息
        log.info("HTTP 请求结果: {}", result.getContent());
    }

    /**
     * 测试 3: Agent 使用搜索工具
     */
    @Test
    void testAgentWithSearchTool() {
        log.info("=== 测试 Agent 使用搜索工具 ===");
        
        // 创建搜索 Agent
        SearchAgent agent = new SearchAgent(webSearchTool);
        agent.setName("SearchAgent");
        agent.setDescription("搜索智能体");
        agent.initModule();
        
        // 测试搜索
        Message result = agent.execute("search", Collections.emptyList());
        assertNotNull(result);
        log.info("搜索结果: {}", result.getContent());
    }

    /**
     * 测试 4: Agent 组合多个工具
     */
    @Test
    void testAgentWithMultipleTools() {
        log.info("=== 测试 Agent 组合多个工具 ===");
        
        // 创建多工具 Agent
        MultiToolAgent agent = new MultiToolAgent(fileSystemTool, httpTool, tempDir.toString());
        agent.setName("MultiToolAgent");
        agent.setDescription("多工具智能体");
        agent.initModule();
        
        // 验证工具数量
        assertEquals(2, agent.getActions().size());
        
        // 测试文件操作
        Message fileResult = agent.execute("fileOperation", Collections.emptyList());
        assertNotNull(fileResult);
        assertEquals(MessageType.RESPONSE, fileResult.getMessageType());
        log.info("文件操作结果: {}", fileResult.getContent());
        
        // 测试网络操作
        Message httpResult = agent.execute("httpOperation", Collections.emptyList());
        assertNotNull(httpResult);
        log.info("网络操作结果: {}", httpResult.getMessageType());
    }

    /**
     * 文件处理 Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    static class FileAgent extends Agent {
        private final FileSystemTool tool;
        private final String baseDir;
        
        public FileAgent(FileSystemTool tool, String baseDir) {
            this.tool = tool;
            this.baseDir = baseDir;
            addAction(new WriteFileAction(tool, baseDir));
            addAction(new ReadFileAction(tool, baseDir));
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                SimpleActionInput input = new SimpleActionInput();
                ActionOutput output = action.execute(input);
                
                return Message.builder()
                        .content(output.getData())
                        .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                        .build();
            } catch (Exception e) {
                return Message.builder()
                        .content("错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    /**
     * HTTP Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    static class HttpAgent extends Agent {
        private final HttpTool tool;
        
        public HttpAgent(HttpTool tool) {
            this.tool = tool;
            addAction(new FetchDataAction(tool));
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                SimpleActionInput input = new SimpleActionInput();
                ActionOutput output = action.execute(input);
                
                return Message.builder()
                        .content(output.getData())
                        .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                        .build();
            } catch (Exception e) {
                return Message.builder()
                        .content("错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    /**
     * 搜索 Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    static class SearchAgent extends Agent {
        private final WebSearchTool tool;
        
        public SearchAgent(WebSearchTool tool) {
            this.tool = tool;
            addAction(new SearchAction(tool));
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                SimpleActionInput input = new SimpleActionInput();
                ActionOutput output = action.execute(input);
                
                return Message.builder()
                        .content(output.getData())
                        .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                        .build();
            } catch (Exception e) {
                return Message.builder()
                        .content("错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    /**
     * 多工具 Agent
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    static class MultiToolAgent extends Agent {
        private final FileSystemTool fileTool;
        private final HttpTool httpTool;
        private final String baseDir;
        
        public MultiToolAgent(FileSystemTool fileTool, HttpTool httpTool, String baseDir) {
            this.fileTool = fileTool;
            this.httpTool = httpTool;
            this.baseDir = baseDir;
            addAction(new FileOperationAction(fileTool, baseDir));
            addAction(new HttpOperationAction(httpTool));
        }
        
        @Override
        public Message execute(String actionName, List<Message> messages) {
            Action action = getAction(actionName);
            if (action == null) {
                return Message.builder()
                        .content("未找到动作: " + actionName)
                        .messageType(MessageType.ERROR)
                        .build();
            }
            
            try {
                SimpleActionInput input = new SimpleActionInput();
                ActionOutput output = action.execute(input);
                
                return Message.builder()
                        .content(output.getData())
                        .messageType(output.isSuccess() ? MessageType.RESPONSE : MessageType.ERROR)
                        .build();
            } catch (Exception e) {
                return Message.builder()
                        .content("错误: " + e.getMessage())
                        .messageType(MessageType.ERROR)
                        .build();
            }
        }
    }

    // ===== 动作实现 =====

    static class WriteFileAction extends Action {
        private final FileSystemTool tool;
        private final String baseDir;
        
        public WriteFileAction(FileSystemTool tool, String baseDir) {
            this.tool = tool;
            this.baseDir = baseDir;
            setName("writeFile");
            setDescription("写入文件");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            String filePath = baseDir + "/test.txt";
            String content = "Hello from EvoX!";
            
            Map<String, Object> params = new HashMap<>();
            params.put("operation", "write");
            params.put("filePath", filePath);
            params.put("content", content);
            
            BaseTool.ToolResult result = tool.execute(params);
            
            if (result.isSuccess()) {
                return SimpleActionOutput.success("文件写入成功: " + filePath);
            } else {
                return SimpleActionOutput.failure("文件写入失败: " + result.getError());
            }
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"result"};
        }
    }

    static class ReadFileAction extends Action {
        private final FileSystemTool tool;
        private final String baseDir;
        
        public ReadFileAction(FileSystemTool tool, String baseDir) {
            this.tool = tool;
            this.baseDir = baseDir;
            setName("readFile");
            setDescription("读取文件");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            String filePath = baseDir + "/test.txt";
            
            Map<String, Object> params = new HashMap<>();
            params.put("operation", "read");
            params.put("filePath", filePath);
            
            BaseTool.ToolResult result = tool.execute(params);
            
            if (result.isSuccess()) {
                return SimpleActionOutput.success("文件内容: " + result.getData());
            } else {
                return SimpleActionOutput.failure("文件读取失败: " + result.getError());
            }
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"content"};
        }
    }

    static class FetchDataAction extends Action {
        private final HttpTool tool;
        
        public FetchDataAction(HttpTool tool) {
            this.tool = tool;
            setName("fetchData");
            setDescription("获取远程数据");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Map<String, Object> params = new HashMap<>();
            params.put("url", "https://httpbin.org/get");
            params.put("method", "GET");
            
            try {
                BaseTool.ToolResult result = tool.execute(params);
                
                if (result.isSuccess()) {
                    return SimpleActionOutput.success("HTTP 请求成功");
                } else {
                    return SimpleActionOutput.failure("HTTP 请求失败: " + result.getError());
                }
            } catch (Exception e) {
                log.warn("HTTP 请求异常（可能是网络问题）: {}", e.getMessage());
                return SimpleActionOutput.failure("网络异常: " + e.getMessage());
            }
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"data"};
        }
    }

    static class SearchAction extends Action {
        private final WebSearchTool tool;
        
        public SearchAction(WebSearchTool tool) {
            this.tool = tool;
            setName("search");
            setDescription("搜索信息");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Map<String, Object> params = new HashMap<>();
            params.put("query", "EvoX framework");
            params.put("maxResults", 3);
            
            try {
                BaseTool.ToolResult result = tool.execute(params);
                
                if (result.isSuccess()) {
                    return SimpleActionOutput.success("搜索完成: " + result.getData());
                } else {
                    return SimpleActionOutput.failure("搜索失败: " + result.getError());
                }
            } catch (Exception e) {
                log.warn("搜索异常（可能是网络问题）: {}", e.getMessage());
                return SimpleActionOutput.failure("搜索异常: " + e.getMessage());
            }
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{"query"};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"results"};
        }
    }

    static class FileOperationAction extends Action {
        private final FileSystemTool tool;
        private final String baseDir;
        
        public FileOperationAction(FileSystemTool tool, String baseDir) {
            this.tool = tool;
            this.baseDir = baseDir;
            setName("fileOperation");
            setDescription("综合文件操作");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            String filePath = baseDir + "/multi-tool-test.txt";
            
            // 写文件
            Map<String, Object> writeParams = new HashMap<>();
            writeParams.put("operation", "write");
            writeParams.put("filePath", filePath);
            writeParams.put("content", "MultiTool Test Content");
            
            BaseTool.ToolResult writeResult = tool.execute(writeParams);
            if (!writeResult.isSuccess()) {
                return SimpleActionOutput.failure("写入失败");
            }
            
            // 读文件
            Map<String, Object> readParams = new HashMap<>();
            readParams.put("operation", "read");
            readParams.put("filePath", filePath);
            
            BaseTool.ToolResult readResult = tool.execute(readParams);
            if (!readResult.isSuccess()) {
                return SimpleActionOutput.failure("读取失败");
            }
            
            return SimpleActionOutput.success("文件操作成功: " + readResult.getData());
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"result"};
        }
    }

    static class HttpOperationAction extends Action {
        private final HttpTool tool;
        
        public HttpOperationAction(HttpTool tool) {
            this.tool = tool;
            setName("httpOperation");
            setDescription("HTTP 操作");
        }
        
        @Override
        public ActionOutput execute(ActionInput input) {
            Map<String, Object> params = new HashMap<>();
            params.put("url", "https://httpbin.org/status/200");
            params.put("method", "GET");
            
            try {
                BaseTool.ToolResult result = tool.execute(params);
                
                if (result.isSuccess()) {
                    return SimpleActionOutput.success("HTTP 操作成功");
                } else {
                    return SimpleActionOutput.failure("HTTP 操作失败");
                }
            } catch (Exception e) {
                log.warn("HTTP 操作异常: {}", e.getMessage());
                return SimpleActionOutput.failure("网络异常");
            }
        }
        
        @Override
        public String[] getInputFields() {
            return new String[]{};
        }
        
        @Override
        public String[] getOutputFields() {
            return new String[]{"result"};
        }
    }

    /**
     * 简单动作输入
     */
    @Data
    static class SimpleActionInput extends ActionInput {
        private Map<String, Object> inputs = new HashMap<>();
        
        @Override
        public boolean validate() {
            return true;
        }
        
        @Override
        public Map<String, Object> toMap() {
            return inputs;
        }
    }
}
