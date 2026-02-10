package io.leavesfly.evox.core.llm;

/**
 * LLM 完整能力接口
 * 聚合同步、异步、流式三种调用模式。
 * 大多数使用场景只需依赖子接口（如 {@link ILLMSync}），
 * 需要全部能力时才依赖此接口。
 *
 * <p>接口层级：
 * <pre>
 *   ILLMSync          — 同步调用（generate / chat / getModelName）
 *     └─ ILLMAsync    — 异步调用（generateAsync / chatAsync）
 *         └─ ILLM     — 流式调用（generateStream / chatStream）
 * </pre>
 *
 * @author EvoX Team
 */
public interface ILLM extends ILLMAsync, ILLMStream {

}
