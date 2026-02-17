package io.leavesfly.evox.frameworks.auction;

import io.leavesfly.evox.frameworks.base.MultiAgentFramework;
import io.leavesfly.evox.workflow.base.Workflow;
import io.leavesfly.evox.workflow.base.WorkflowNode;
import io.leavesfly.evox.workflow.graph.WorkflowGraph;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拍卖框架
 * 支持多种拍卖机制的通用框架
 * 基于 evox-workflow DAG 引擎实现
 *
 * @param <T> 拍卖物品类型
 * @author EvoX Team
 */
@Slf4j
@Data
public class AuctionFramework<T> extends MultiAgentFramework {

    /**
     * 拍卖物品
     */
    private T item;

    /**
     * 拍卖机制
     */
    private AuctionMechanism auctionMechanism;

    /**
     * 参与竞价的智能体
     */
    private List<Bidder<T>> bidders;

    /**
     * 拍卖配置
     */
    private AuctionConfig config;



    /**
     * 拍卖结果缓存（从 workflow 获取后缓存）
     */
    private AuctionResult<T> cachedResult;

    public AuctionFramework(T item, AuctionMechanism mechanism, List<Bidder<T>> bidders, AuctionConfig config) {
        this.item = item;
        this.auctionMechanism = mechanism;
        this.bidders = bidders;
        this.config = config;
        this.frameworkName = "AuctionFramework";
    }

    public AuctionFramework(T item, AuctionMechanism mechanism, List<Bidder<T>> bidders) {
        this(item, mechanism, bidders, AuctionConfig.builder().build());
    }

    /**
     * 开始拍卖
     *
     * @return 拍卖结果
     */
    public AuctionResult<T> startAuction() {
        log.info("Starting auction for item: {} with mechanism: {}", item, auctionMechanism);

        // 构建工作流输入参数
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("item", item);
        inputs.put("mechanism", auctionMechanism);
        inputs.put("bidders", bidders);
        inputs.put("config", config);
        
        // 初始化拍卖状态
        inputs.put("auction_finished", false);
        inputs.put("start_time", System.currentTimeMillis());
        inputs.put("status", AuctionStatus.PENDING);

        // 执行工作流
        String resultJson = executeWorkflow("Auction: " + item, inputs);

        // 从缓存中获取拍卖结果（AuctionResultHandler 会将结果设置到 handler 中）
        if (cachedResult != null) {
            return cachedResult;
        }

        // 如果无法从缓存中获取结果，返回失败结果
        return AuctionResult.<T>builder()
            .success(false)
            .error("Failed to retrieve auction result from workflow")
            .build();
    }

    @Override
    protected WorkflowGraph buildWorkflowGraph(String task) {
        WorkflowGraph graph = new WorkflowGraph(task);

        // 创建 LOOP 节点：控制拍卖轮次循环
        WorkflowNode loopNode = createLoopNode(
            "auction_loop",
            config != null ? config.getMaxRounds() : 100,
            "auction_finished == false"
        );

        // 创建 COLLECT 节点：执行单轮拍卖
        WorkflowNode roundNode = createCollectNode(
            "auction_round",
            "auction_round",
            new HashMap<>()
        );

        // 创建 COLLECT 节点：构建最终拍卖结果
        WorkflowNode resultNode = createCollectNode(
            "auction_result",
            "auction_result",
            new HashMap<>()
        );

        // 设置 LOOP 节点的循环体
        loopNode.setLoopBodyNodeId(roundNode.getNodeId());

        // 添加节点到图
        graph.addNode(loopNode);
        graph.addNode(roundNode);
        graph.addNode(resultNode);

        // 添加边：LOOP 结束后执行结果节点
        graph.addEdge(loopNode.getNodeId(), resultNode.getNodeId());

        return graph;
    }

    @Override
    protected void registerNodeHandlers(Workflow workflow) {
        // 创建拍卖轮次处理器
        AuctionNodeHandler.AuctionRoundHandler<T> roundHandler = new AuctionNodeHandler.AuctionRoundHandler<>();
        
        // 创建拍卖结果处理器
        AuctionNodeHandler.AuctionResultHandler<T> resultHandler = new AuctionNodeHandler.AuctionResultHandler<>();
        
        // 设置结果回调，当拍卖完成时将结果缓存到 framework 中
        resultHandler.setResultCallback(result -> {
            this.cachedResult = result;
        });

        // 注册处理器到工作流
        workflow.registerHandler("auction_round", roundHandler);
        workflow.registerHandler("auction_result", resultHandler);
    }

    /**
     * 拍卖状态枚举
     */
    public enum AuctionStatus {
        PENDING,    // 等待开始
        RUNNING,    // 进行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }
}