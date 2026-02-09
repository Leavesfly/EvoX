package io.leavesfly.evox.claudecode;

import io.leavesfly.evox.claudecode.agent.ClaudeCodeAgent;
import io.leavesfly.evox.core.message.Message;
import io.leavesfly.evox.core.message.MessageType;
import io.leavesfly.evox.agents.specialized.ToolAwareAgent;
import io.leavesfly.evox.models.base.BaseLLM;
import io.leavesfly.evox.models.config.OpenAILLMConfig;
import io.leavesfly.evox.models.openai.OpenAILLM;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Scanner;

/**
 * ClaudeCode 运行入口
 * 在控制台与 Claude Code 风格编程助手交互：输入自然语言任务，Agent 会使用文件、命令、代码解释器等工具执行。
 *
 * <p>使用前请设置 LLM 环境变量，例如：</p>
 * <ul>
 *   <li>OpenAI: export OPENAI_API_KEY=sk-xxx</li>
 *   <li>或修改本类中的 LLM 配置以使用其他模型</li>
 * </ul>
 *
 * @author EvoX Team
 */
@Slf4j
public class ClaudeCodeRunner {

    public static void main(String[] args) {
        String projectRoot = args.length > 0 ? args[0] : System.getProperty("user.dir");
        log.info("Project root: {}", projectRoot);

        OpenAILLMConfig llmConfig = OpenAILLMConfig.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .model("gpt-4o-mini")
                .temperature(0.2f)
                .build();
        BaseLLM llm = new OpenAILLM(llmConfig);

        ToolAwareAgent agent = ClaudeCodeAgent.create(projectRoot, llmConfig, llm);

        log.info("ClaudeCode ready. Type your request (e.g. 'list files in src', 'run mvn test') or 'exit' to quit.\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine();
                if (line == null || line.isBlank()) continue;
                if ("exit".equalsIgnoreCase(line.trim()) || "quit".equalsIgnoreCase(line.trim())) {
                    log.info("Bye.");
                    break;
                }

                Message userMsg = Message.builder()
                        .messageType(MessageType.INPUT)
                        .content(line)
                        .build();
                Message response = agent.execute(null, List.of(userMsg));
                System.out.println("ClaudeCode: " + (response.getContent() != null ? response.getContent() : "(no response)"));
                System.out.println();
            }
        }
    }
}
