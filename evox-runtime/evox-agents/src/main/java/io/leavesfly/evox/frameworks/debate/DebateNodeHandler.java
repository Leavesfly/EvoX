package io.leavesfly.evox.frameworks.debate;

import io.leavesfly.evox.models.base.LLMProvider;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.execution.WorkflowContext;
import io.leavesfly.evox.workflow.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 辩论框架节点处理器集合
 * 
 * 包含两个核心处理器：
 * - DebateRoundHandler：执行单轮辩论，处理智能体响应、观点跟踪和评分
 * - DebateFinalHandler：在辩论结束后生成综合性的最终答案
 * 
 * 支持多种辩论模式：轮流辩论、对抗辩论、圆桌讨论、苏格拉底式辩论
 * 
 * @author EvoX Team
 * @see MultiAgentDebate
 */
@Slf4j
public class DebateNodeHandler {

    // ==================== 单轮辩论处理器 ====================

    /**
     * 单轮辩论处理器
     * 
     * 执行单轮辩论的完整流程：
     * 1. 从 WorkflowContext 读取配置和状态数据
     * 2. 根据辩论模式执行相应的轮次逻辑
     * 3. 处理每个智能体的响应（添加历史、跟踪观点、评分）
     * 4. 检查是否达成共识或达到最大轮次
     * 5. 更新 WorkflowContext 中的状态数据
     * 
     * 支持的辩论模式：
     * - ROUND_ROBIN：轮流发言，每个智能体依次表达观点
     * - ADVERSARIAL：对抗式辩论，正反双方交替发言
     * - PANEL：圆桌讨论，自由发表意见
     * - SOCRATIC：苏格拉底式，通过引导性问题深化讨论
     */
    public static class DebateRoundHandler implements NodeHandler {

        // ==================== 评分常量 ====================
        
        /** 响应长度评分因子：每 100 字符得 1 分 */
        private static final int LENGTH_SCORE_FACTOR = 100;
        
        /** 响应长度评分上限：最多 5 分 */
        private static final int MAX_LENGTH_SCORE = 5;
        
        /** 论据支持分：包含关键论证词时得分 */
        private static final double ARGUMENT_SUPPORT_SCORE = 2.0;
        
        /** 数据引用分：包含数字、百分比或年份时得分 */
        private static final double DATA_REFERENCE_SCORE = 1.5;

        /**
         * 获取处理器名称
         * 
         * @return 处理器标识符 "debate_round_handler"
         */
        @Override
        public String getHandlerName() {
            return "debate_round_handler";
        }
        
        /**
         * 处理单轮辩论节点
         * 
         * 执行完整的单轮辩论流程，包括：
         * - 读取配置和状态数据
         * - 根据辩论模式执行相应逻辑
         * - 处理智能体响应并评分
         * - 检查共识状态
         * - 更新上下文数据
         * 
         * @param context 工作流上下文，包含所有执行数据
         * @param node 当前工作流节点
         * @return 处理结果标识 "round_completed"
         */
        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                log.info("[DebateRoundHandler] 开始执行单轮辩论");
        
                // ==================== 读取配置和状态 ====================
                        
                // 从上下文读取辩论配置
                List<MultiAgentDebate.DebateAgent> agents = 
                    (List<MultiAgentDebate.DebateAgent>) context.getExecutionData("debate_agents");
                LLMProvider moderator = 
                    (LLMProvider) context.getExecutionData("debate_moderator");
                MultiAgentDebate.DebateMode mode = 
                    (MultiAgentDebate.DebateMode) context.getExecutionData("debate_mode");
                MultiAgentDebate.DebateConfig config = 
                    (MultiAgentDebate.DebateConfig) context.getExecutionData("debate_config");
                String question = 
                    (String) context.getExecutionData("debate_question");
                List<MultiAgentDebate.DebateEventListener> eventListeners = 
                    (List<MultiAgentDebate.DebateEventListener>) context.getExecutionData("debate_event_listeners");
                int maxRounds = 
                    (int) context.getExecutionData("debate_max_rounds");
        
                // ==================== 初始化或读取状态 ====================
                        
                // 当前轮次（从 0 开始）
                int currentRound = getOrDefault(context, "current_round", 0);
                        
                // 辩论历史记录
                List<MultiAgentDebate.DebateRecord> history = 
                    getOrDefault(context, "history", new ArrayList<>());
        
                // 观点跟踪器
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker = 
                    getOrDefault(context, "viewpoint_tracker", new HashMap<>());
        
                // 智能体评分
                Map<String, Double> scores = initializeScores(context, agents);
        
                // ==================== 执行辩论流程 ====================
                        
                // 触发轮次开始事件
                fireEvent(eventListeners, MultiAgentDebate.DebateEventType.ROUND_STARTED, currentRound);
        
                // 根据辩论模式执行相应的轮次逻辑
                executeDebateMode(mode, agents, moderator, question, history, currentRound, 
                                 config, scores, viewpointTracker, eventListeners);
        
                // ==================== 检查和更新状态 ====================
                        
                // 检查是否达成共识
                boolean consensusReached = checkConsensus(moderator, history);
                boolean debateFinished = consensusReached || (currentRound + 1 >= maxRounds);
        
                if (consensusReached) {
                    log.info("[DebateRoundHandler] 在第 {} 轮达成共识", currentRound + 1);
                    fireEvent(eventListeners, MultiAgentDebate.DebateEventType.CONSENSUS_REACHED, currentRound);
                }
        
                // 触发轮次结束事件
                fireEvent(eventListeners, MultiAgentDebate.DebateEventType.ROUND_ENDED, currentRound);
        
                // 更新上下文状态
                updateContextState(context, currentRound, history, viewpointTracker, scores, debateFinished);
        
                log.info("[DebateRoundHandler] 单轮辩论完成，轮次：{}, 辩论结束：{}", currentRound, debateFinished);
        
                return "round_completed";
            });
        }
        
        /**
         * 从上下文获取数据，如果不存在则返回默认值
         * 
         * @param context 工作流上下文
         * @param key 数据键
         * @param defaultValue 默认值
         * @param <T> 数据类型
         * @return 上下文中的数据或默认值
         */
        @SuppressWarnings("unchecked")
        private <T> T getOrDefault(WorkflowContext context, String key, T defaultValue) {
            Object value = context.getExecutionData(key);
            return value != null ? (T) value : defaultValue;
        }
        
        /**
         * 初始化智能体评分表
         * 
         * @param context 工作流上下文
         * @param agents 智能体列表
         * @return 评分映射表
         */
        @SuppressWarnings("unchecked")
        private Map<String, Double> initializeScores(WorkflowContext context, 
                                                     List<MultiAgentDebate.DebateAgent> agents) {
            Map<String, Double> scores = (Map<String, Double>) context.getExecutionData("scores");
            if (scores == null && agents != null) {
                scores = new HashMap<>();
                for (MultiAgentDebate.DebateAgent agent : agents) {
                    scores.put(agent.getName(), 0.0);
                }
            }
            return scores != null ? scores : new HashMap<>();
        }
        
        /**
         * 根据辩论模式执行相应的轮次逻辑
         * 
         * @param mode 辩论模式
         * @param agents 智能体列表
         * @param moderator 主持人
         * @param question 辩论问题
         * @param history 历史记录
         * @param currentRound 当前轮次
         * @param config 辩论配置
         * @param scores 评分表
         * @param viewpointTracker 观点跟踪器
         * @param eventListeners 事件监听器列表
         */
        private void executeDebateMode(MultiAgentDebate.DebateMode mode,
                                       List<MultiAgentDebate.DebateAgent> agents,
                                       LLMProvider moderator,
                                       String question,
                                       List<MultiAgentDebate.DebateRecord> history,
                                       int currentRound,
                                       MultiAgentDebate.DebateConfig config,
                                       Map<String, Double> scores,
                                       Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                                       List<MultiAgentDebate.DebateEventListener> eventListeners) {
            if (mode == MultiAgentDebate.DebateMode.ROUND_ROBIN) {
                executeRoundRobin(agents, question, history, currentRound, config, scores, viewpointTracker, eventListeners);
            } else if (mode == MultiAgentDebate.DebateMode.ADVERSARIAL) {
                executeAdversarial(agents, history, currentRound, config, scores, viewpointTracker, eventListeners);
            } else if (mode == MultiAgentDebate.DebateMode.PANEL) {
                executePanel(agents, question, history, currentRound, config, scores, viewpointTracker, eventListeners);
            } else if (mode == MultiAgentDebate.DebateMode.SOCRATIC) {
                executeSocratic(agents, moderator, question, history, currentRound, config, scores, viewpointTracker, eventListeners);
            }
        }
        
        /**
         * 更新上下文中的状态数据
         * 
         * @param context 工作流上下文
         * @param currentRound 当前轮次
         * @param history 历史记录
         * @param viewpointTracker 观点跟踪器
         * @param scores 评分表
         * @param debateFinished 辩论是否结束标志
         */
        private void updateContextState(WorkflowContext context, int currentRound,
                                        List<MultiAgentDebate.DebateRecord> history,
                                        Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                                        Map<String, Double> scores, boolean debateFinished) {
            context.updateExecutionData("current_round", currentRound + 1);
            context.updateExecutionData("history", history);
            context.updateExecutionData("viewpoint_tracker", viewpointTracker);
            context.updateExecutionData("scores", scores);
            context.updateExecutionData("debate_finished", debateFinished);
        }

        // ==================== 辩论模式执行方法 ====================

        /**
         * 执行轮流辩论模式
         * 
         * 每个智能体依次发表观点，按顺序完成一轮发言。
         * 
         * @param agents 智能体列表
         * @param question 辩论问题
         * @param history 历史记录
         * @param round 当前轮次
         * @param config 辩论配置
         * @param scores 评分表
         * @param viewpointTracker 观点跟踪器
         * @param eventListeners 事件监听器列表
         */
        private void executeRoundRobin(
                List<MultiAgentDebate.DebateAgent> agents,
                String question,
                List<MultiAgentDebate.DebateRecord> history,
                int round,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            // 按顺序让每个智能体发言
            for (MultiAgentDebate.DebateAgent agent : agents) {
                String response = agent.respond(question, history);
                processResponse(agent.getName(), response, round, history, config, scores, viewpointTracker, eventListeners);
            }
        }

        /**
         * 执行对抗辩论模式
         * 
         * 正反双方交替发言，支持交叉质询环节。
         * 
         * @param agents 智能体列表（至少需要两个智能体）
         * @param history 历史记录
         * @param round 当前轮次
         * @param config 辩论配置
         * @param scores 评分表
         * @param viewpointTracker 观点跟踪器
         * @param eventListeners 事件监听器列表
         */
        private void executeAdversarial(
                List<MultiAgentDebate.DebateAgent> agents,
                List<MultiAgentDebate.DebateRecord> history,
                int round,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            if (agents.size() < 2) {
                log.warn("[DebateRoundHandler] 对抗辩论模式至少需要两个智能体");
                return;
            }

            MultiAgentDebate.DebateAgent proponent = agents.get(0);  // 正方
            MultiAgentDebate.DebateAgent opponent = agents.get(1);   // 反方

            // 正方发言
            String proResponse = proponent.respond("请阐述你的观点", history);
            processResponse(proponent.getName(), proResponse, round, history, config, scores, viewpointTracker, eventListeners);

            // 反方发言
            String oppResponse = opponent.respond("请阐述你的观点", history);
            processResponse(opponent.getName(), oppResponse, round, history, config, scores, viewpointTracker, eventListeners);

            // 交叉质询环节（如果已启用）
            if (config != null && config.isEnableCrossExamination()) {
                executeCrossExamination(proponent, opponent, round, history, config, scores, viewpointTracker, eventListeners);
            }
        }

        /**
         * 执行圆桌讨论模式
         * 
         * 所有智能体自由发表意见，类似圆桌会议形式。
         * 
         * @param agents 智能体列表
         * @param question 辩论问题
         * @param history 历史记录
         * @param round 当前轮次
         * @param config 辩论配置
         * @param scores 评分表
         * @param viewpointTracker 观点跟踪器
         * @param eventListeners 事件监听器列表
         */
        private void executePanel(
                List<MultiAgentDebate.DebateAgent> agents,
                String question,
                List<MultiAgentDebate.DebateRecord> history,
                int round,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            // 每个智能体自由发言
            for (MultiAgentDebate.DebateAgent agent : agents) {
                String response = agent.respond(question, history);
                processResponse(agent.getName(), response, round, history, config, scores, viewpointTracker, eventListeners);
            }
        }

        /**
         * 执行苏格拉底式辩论模式
         * 
         * 通过引导性问题深化讨论，第一轮使用原始问题，后续由主持人生成后续问题。
         * 
         * @param agents 智能体列表
         * @param moderator 主持人（用于生成引导性问题）
         * @param question 辩论问题
         * @param history 历史记录
         * @param round 当前轮次
         * @param config 辩论配置
         * @param scores 评分表
         * @param viewpointTracker 观点跟踪器
         * @param eventListeners 事件监听器列表
         */
        private void executeSocratic(
                List<MultiAgentDebate.DebateAgent> agents,
                LLMProvider moderator,
                String question,
                List<MultiAgentDebate.DebateRecord> history,
                int round,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            // 第一轮使用原始问题，后续轮次由主持人生成引导性问题
            String currentQuestion = (round == 0) ? question : generateFollowUpQuestion(moderator, history);

            // 智能体回答问题
            for (MultiAgentDebate.DebateAgent agent : agents) {
                String response = agent.respond(currentQuestion, history);
                processResponse(agent.getName(), response, round, history, config, scores, viewpointTracker, eventListeners);
            }
        }

        /**
         * 执行交叉质询
         */
        private void executeCrossExamination(
                MultiAgentDebate.DebateAgent proponent,
                MultiAgentDebate.DebateAgent opponent,
                int round,
                List<MultiAgentDebate.DebateRecord> history,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            // 正方质询反方
            String proQuestion = proponent.respond("请向反方提出一个关键问题", history);
            String oppAnswer = opponent.respond("请回答: " + proQuestion, history);
            
            history.add(new MultiAgentDebate.DebateRecord(round, proponent.getName(), "质询: " + proQuestion));
            processResponse(opponent.getName(), "回答: " + oppAnswer, round, history, config, scores, viewpointTracker, eventListeners);

            // 反方质询正方
            String oppQuestion = opponent.respond("请向正方提出一个关键问题", history);
            String proAnswer = proponent.respond("请回答: " + oppQuestion, history);
            
            history.add(new MultiAgentDebate.DebateRecord(round, opponent.getName(), "质询: " + oppQuestion));
            processResponse(proponent.getName(), "回答: " + proAnswer, round, history, config, scores, viewpointTracker, eventListeners);
        }

        /**
         * 生成后续引导性问题
         */
        private String generateFollowUpQuestion(LLMProvider moderator, List<MultiAgentDebate.DebateRecord> history) {
            if (moderator == null) {
                return "请继续讨论";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("基于以下辩论历史，请提出一个能够深化讨论的后续问题：\n\n");

            // 只取最近的几条记录作为上下文
            int startIndex = Math.max(0, history.size() - 5);
            for (int i = startIndex; i < history.size(); i++) {
                MultiAgentDebate.DebateRecord record = history.get(i);
                promptBuilder.append(String.format("%s: %s\n", record.getAgentName(), record.getResponse()));
            }
            
            promptBuilder.append("\n请生成一个能够引导更深入讨论的问题：");

            return moderator.generate(promptBuilder.toString());
        }

        /**
         * 处理智能体的响应
         */
        private void processResponse(
                String agentName,
                String response,
                int round,
                List<MultiAgentDebate.DebateRecord> history,
                MultiAgentDebate.DebateConfig config,
                Map<String, Double> scores,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker,
                List<MultiAgentDebate.DebateEventListener> eventListeners) {
            
            // 添加到历史记录
            MultiAgentDebate.DebateRecord record = new MultiAgentDebate.DebateRecord(round, agentName, response);
            history.add(record);

            // 跟踪观点
            extractAndTrackViewpoints(agentName, response, round, viewpointTracker);

            // 评分（如果启用）
            if (config != null && config.isEnableScoring()) {
                evaluateResponse(agentName, response, scores);
            }

            // 触发事件
            fireEvent(eventListeners, MultiAgentDebate.DebateEventType.AGENT_RESPONDED, record);
            
            log.debug("[{}] {}", agentName, response);
        }

        /**
         * 提取并跟踪智能体的观点
         */
        private void extractAndTrackViewpoints(
                String agentName,
                String response,
                int round,
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker) {
            
            MultiAgentDebate.Viewpoint viewpoint = MultiAgentDebate.Viewpoint.builder()
                    .round(round)
                    .content(response)
                    .timestamp(System.currentTimeMillis())
                    .build();

            viewpointTracker.computeIfAbsent(agentName, k -> new ArrayList<>()).add(viewpoint);
        }

        /**
         * 评估智能体响应质量并评分
         */
        private void evaluateResponse(String agentName, String response, Map<String, Double> scores) {
            double score = 0.0;

            // 基础分：响应长度（每100字符得分，上限 5 分）
            score += Math.min(response.length() / LENGTH_SCORE_FACTOR, MAX_LENGTH_SCORE);

            // 论据支持分：包含关键论证词
            if (containsArgumentKeywords(response)) {
                score += ARGUMENT_SUPPORT_SCORE;
            }

            // 数据引用分：包含数字和百分比或年份
            if (containsDataReferences(response)) {
                score += DATA_REFERENCE_SCORE;
            }

            // 累计评分
            scores.merge(agentName, score, Double::sum);
        }

        /**
         * 检查响应是否包含论据关键词
         */
        private boolean containsArgumentKeywords(String response) {
            return response.contains("因为") 
                || response.contains("根据") 
                || response.contains("研究表明")
                || response.contains("数据显示")
                || response.contains("事实证明");
        }

        /**
         * 检查响应是否包含数据引用
         */
        private boolean containsDataReferences(String response) {
            return response.matches(".*\\d+%.*") 
                || response.matches(".*\\d+年.*")
                || response.matches(".*\\d+\\s*(亿|万|千).*");
        }

        /**
         * 检查是否达成共识
         */
        private boolean checkConsensus(LLMProvider moderator, List<MultiAgentDebate.DebateRecord> history) {
            if (moderator == null || history.isEmpty()) {
                return false;
            }

            log.debug("[DebateRoundHandler] 检查是否达成共识...");

            String prompt = buildConsensusCheckPrompt(history);
            String response = moderator.generate(prompt);
            boolean consensusReached = response.trim().toUpperCase().startsWith("YES");

            if (consensusReached) {
                log.info("[DebateRoundHandler] 主持人检测到共识: {}", response);
            }

            return consensusReached;
        }

        /**
         * 构建共识检查的提示词
         */
        private String buildConsensusCheckPrompt(List<MultiAgentDebate.DebateRecord> history) {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("分析以下辩论历史，并判断所有参与者是否已达成明确的共识或一致意见。\n\n");
            promptBuilder.append("辩论历史：\n");
            
            for (MultiAgentDebate.DebateRecord record : history) {
                promptBuilder.append(String.format("[%d] %s: %s\n", 
                    record.getRound(), 
                    record.getAgentName(), 
                    record.getResponse()));
            }
            
            promptBuilder.append("\n是否已达成共识？请以 'YES' 或 'NO' 开头回答，并简要说明理由。");
            
            return promptBuilder.toString();
        }

        /**
         * 触发辩论事件
         */
        private void fireEvent(
                List<MultiAgentDebate.DebateEventListener> eventListeners,
                MultiAgentDebate.DebateEventType type,
                Object data) {
            
            if (eventListeners == null || eventListeners.isEmpty()) {
                return;
            }
            
            MultiAgentDebate.DebateEvent event = 
                new MultiAgentDebate.DebateEvent(type, data, System.currentTimeMillis());
            
            eventListeners.forEach(listener -> {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.warn("[DebateRoundHandler] 事件监听器处理失败: eventType={}, listener={}", 
                        type, listener.getClass().getSimpleName(), e);
                }
            });
        }
    }

    /**
     * 最终答案处理器
     * 
     * <p>在辩论结束后，基于完整的辩论历史生成综合性的最终答案或总结。</p>
     */
    public static class DebateFinalHandler implements NodeHandler {

        @Override
        public String getHandlerName() {
            return "debate_final_handler";
        }

        @Override
        public Mono<Object> handle(WorkflowContext context, WorkflowNode node) {
            return Mono.fromCallable(() -> {
                log.info("[DebateFinalHandler] 开始生成最终答案");

                // 从 WorkflowContext 读取状态
                LLMProvider moderator = 
                    (LLMProvider) context.getExecutionData("debate_moderator");
                String question = 
                    (String) context.getExecutionData("debate_question");
                List<MultiAgentDebate.DebateRecord> history = 
                    (List<MultiAgentDebate.DebateRecord>) context.getExecutionData("history");
                int currentRound = context.getExecutionData("current_round") != null 
                    ? (int) context.getExecutionData("current_round") : 0;
                Map<String, List<MultiAgentDebate.Viewpoint>> viewpointTracker = 
                    (Map<String, List<MultiAgentDebate.Viewpoint>>) context.getExecutionData("viewpoint_tracker");
                Map<String, Double> scores = 
                    (Map<String, Double>) context.getExecutionData("scores");
                List<MultiAgentDebate.DebateEventListener> eventListeners = 
                    (List<MultiAgentDebate.DebateEventListener>) context.getExecutionData("debate_event_listeners");

                // 生成最终答案
                String finalAnswer = generateFinalAnswer(moderator, history, question);

                // 触发辩论结束事件
                fireEvent(eventListeners, MultiAgentDebate.DebateEventType.DEBATE_ENDED, finalAnswer);

                // 构建结果 Map
                Map<String, Object> result = new HashMap<>();
                result.put("finalAnswer", finalAnswer);
                result.put("history", history);
                result.put("currentRound", currentRound);
                result.put("viewpoints", viewpointTracker);
                result.put("scores", scores);

                log.info("[DebateFinalHandler] 最终答案生成完成，总轮次: {}", currentRound);

                return result;
            });
        }

        /**
         * 生成最终答案
         */
        private String generateFinalAnswer(
                LLMProvider moderator,
                List<MultiAgentDebate.DebateRecord> history,
                String question) {
            
            if (history.isEmpty()) {
                return "没有可用的辩论历史。";
            }

            // 如果未配置主持人，返回最后一条记录
            if (moderator == null) {
                MultiAgentDebate.DebateRecord lastRecord = history.get(history.size() - 1);
                return String.format("辩论结束，未配置主持人。来自 %s 的最后观点：%s",
                        lastRecord.getAgentName(), lastRecord.getResponse());
            }

            log.info("[DebateFinalHandler] 生成最终答案...");

            String prompt = buildFinalAnswerPrompt(history, question);
            return moderator.generate(prompt);
        }

        /**
         * 构建最终答案生成的提示词
         */
        private String buildFinalAnswerPrompt(
                List<MultiAgentDebate.DebateRecord> history,
                String question) {
            
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("基于以下多智能体辩论历史，请提供一个全面的最终答案或总结，");
            promptBuilder.append("综合关键观点及达成的任何共识。\n\n");
            promptBuilder.append("原始问题：").append(question).append("\n\n");
            promptBuilder.append("辩论历史：\n");
            
            for (MultiAgentDebate.DebateRecord record : history) {
                promptBuilder.append(String.format("[%d] %s: %s\n", 
                    record.getRound(), 
                    record.getAgentName(), 
                    record.getResponse()));
            }
            
            promptBuilder.append("\n最终全面回答：");
            
            return promptBuilder.toString();
        }

        /**
         * 触发辩论事件
         */
        private void fireEvent(
                List<MultiAgentDebate.DebateEventListener> eventListeners,
                MultiAgentDebate.DebateEventType type,
                Object data) {
            
            if (eventListeners == null || eventListeners.isEmpty()) {
                return;
            }
            
            MultiAgentDebate.DebateEvent event = 
                new MultiAgentDebate.DebateEvent(type, data, System.currentTimeMillis());
            
            eventListeners.forEach(listener -> {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.warn("[DebateFinalHandler] 事件监听器处理失败: eventType={}, listener={}", 
                        type, listener.getClass().getSimpleName(), e);
                }
            });
        }
    }
}
