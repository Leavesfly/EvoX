package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Moltbook 社交网络技能
 * 让 AI Agent 能够在 Moltbook（AI Agent 社交网络）上注册、发帖、评论、点赞和浏览信息流。
 *
 * <p>支持的操作：
 * <ul>
 *   <li>register — 注册 Agent 身份</li>
 *   <li>update_profile — 更新 Agent 资料</li>
 *   <li>post — 发布帖子</li>
 *   <li>comment — 评论帖子</li>
 *   <li>upvote — 点赞帖子</li>
 *   <li>feed — 浏览信息流</li>
 *   <li>view_post — 查看帖子详情</li>
 *   <li>submolts — 浏览/创建子版块</li>
 * </ul>
 *
 * @author EvoX Team
 */
@Slf4j
public class MoltbookSkill extends BaseSkill {

    private static final String MOLTBOOK_API_BASE = "https://www.moltbook.com/api";

    public MoltbookSkill() {
        setName("moltbook");
        setDescription("Interact with Moltbook, the social network for AI agents. "
                + "Register your agent identity, create posts, comment, upvote, browse feeds, "
                + "and manage submolts. Humans can only observe — agents run the show.");

        setSystemPrompt(buildMoltbookSystemPrompt());

        setRequiredTools(List.of("http", "shell"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description",
                "Operation: 'register', 'update_profile', 'post', 'comment', 'upvote', 'feed', 'view_post', 'submolts'");
        inputParams.put("operation", operationParam);

        Map<String, String> contentParam = new HashMap<>();
        contentParam.put("type", "string");
        contentParam.put("description",
                "Content for post/comment, or agent description for register/update_profile");
        inputParams.put("content", contentParam);

        Map<String, String> postIdParam = new HashMap<>();
        postIdParam.put("type", "string");
        postIdParam.put("description", "Post ID for comment/upvote/view_post operations");
        inputParams.put("postId", postIdParam);

        Map<String, String> submoltParam = new HashMap<>();
        submoltParam.put("type", "string");
        submoltParam.put("description", "Submolt name for posting to a specific submolt or browsing");
        inputParams.put("submolt", submoltParam);

        Map<String, String> agentNameParam = new HashMap<>();
        agentNameParam.put("type", "string");
        agentNameParam.put("description", "Agent display name for registration");
        inputParams.put("agentName", agentNameParam);

        Map<String, String> titleParam = new HashMap<>();
        titleParam.put("type", "string");
        titleParam.put("description", "Title for a new post");
        inputParams.put("title", titleParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("operation"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String operation = context.getParameters().getOrDefault("operation", "feed").toString();
        String content = context.getParameters().getOrDefault("content", "").toString();
        String postId = context.getParameters().getOrDefault("postId", "").toString();
        String submolt = context.getParameters().getOrDefault("submolt", "").toString();
        String agentName = context.getParameters().getOrDefault("agentName", "").toString();
        String title = context.getParameters().getOrDefault("title", "").toString();

        String prompt = buildPrompt(context.getInput(), context.getAdditionalContext());

        StringBuilder moltbookPrompt = new StringBuilder(prompt);
        moltbookPrompt.append("\n\nMoltbook Operation: ").append(operation);

        switch (operation) {
            case "register" -> buildRegisterPrompt(moltbookPrompt, agentName, content);
            case "update_profile" -> buildUpdateProfilePrompt(moltbookPrompt, content);
            case "post" -> buildPostPrompt(moltbookPrompt, title, content, submolt);
            case "comment" -> buildCommentPrompt(moltbookPrompt, postId, content);
            case "upvote" -> buildUpvotePrompt(moltbookPrompt, postId);
            case "feed" -> buildFeedPrompt(moltbookPrompt, submolt);
            case "view_post" -> buildViewPostPrompt(moltbookPrompt, postId);
            case "submolts" -> buildSubmoltsPrompt(moltbookPrompt, submolt);
            default -> moltbookPrompt.append("\n\nUnknown operation. Available: register, update_profile, ")
                    .append("post, comment, upvote, feed, view_post, submolts.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "moltbook");
        metadata.put("operation", operation);
        if (!postId.isEmpty()) metadata.put("postId", postId);
        if (!submolt.isEmpty()) metadata.put("submolt", submolt);

        return SkillResult.success(moltbookPrompt.toString(), metadata);
    }

    private void buildRegisterPrompt(StringBuilder prompt, String agentName, String description) {
        prompt.append("\n\nRegister a new agent on Moltbook.");
        prompt.append("\nAgent name: ").append(agentName.isEmpty() ? "(use a creative name)" : agentName);
        prompt.append("\nDescription: ").append(description.isEmpty() ? "(generate a compelling description)" : description);

        prompt.append("\n\nSteps:");
        prompt.append("\n1. Send a POST request to register the agent:");
        prompt.append("\n   curl -s -X POST ").append(MOLTBOOK_API_BASE).append("/agents/register \\");
        prompt.append("\n     -H 'Content-Type: application/json' \\");
        prompt.append("\n     -d '{\"name\": \"<AGENT_NAME>\", \"description\": \"<DESCRIPTION>\"}'");
        prompt.append("\n2. Save the returned agent ID and API key securely");
        prompt.append("\n3. Verify ownership if required by the API response");
        prompt.append("\n4. Report the registration result including agent ID");

        prompt.append("\n\nImportant:");
        prompt.append("\n- Store the API key in environment variable MOLTBOOK_API_KEY for future requests");
        prompt.append("\n- The agent name should be unique and memorable");
        prompt.append("\n- The description should reflect the agent's personality and capabilities");
    }

    private void buildUpdateProfilePrompt(StringBuilder prompt, String description) {
        prompt.append("\n\nUpdate the agent's profile on Moltbook.");
        prompt.append("\nNew description: ").append(description);

        prompt.append("\n\nUse:");
        prompt.append("\n   curl -s -X PATCH ").append(MOLTBOOK_API_BASE).append("/agents/me \\");
        prompt.append("\n     -H 'Content-Type: application/json' \\");
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY' \\");
        prompt.append("\n     -d '{\"description\": \"<NEW_DESCRIPTION>\"}'");
    }

    private void buildPostPrompt(StringBuilder prompt, String title, String content, String submolt) {
        prompt.append("\n\nCreate a new post on Moltbook.");
        prompt.append("\nTitle: ").append(title.isEmpty() ? "(generate an engaging title)" : title);
        prompt.append("\nContent: ").append(content.isEmpty() ? "(generate thoughtful content)" : content);
        if (!submolt.isEmpty()) {
            prompt.append("\nSubmolt: ").append(submolt);
        }

        prompt.append("\n\nUse:");
        prompt.append("\n   curl -s -X POST ").append(MOLTBOOK_API_BASE).append("/posts \\");
        prompt.append("\n     -H 'Content-Type: application/json' \\");
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY' \\");
        prompt.append("\n     -d '{\"title\": \"<TITLE>\", \"content\": \"<CONTENT>\"");
        if (!submolt.isEmpty()) {
            prompt.append(", \"submolt\": \"").append(submolt).append("\"");
        }
        prompt.append("}'");

        prompt.append("\n\nTips for great Moltbook posts:");
        prompt.append("\n- Be original and thought-provoking");
        prompt.append("\n- Engage with topics other agents care about (AI, technology, philosophy, creativity)");
        prompt.append("\n- Use clear formatting for readability");
        prompt.append("\n- Consider posting to a relevant submolt for better visibility");
    }

    private void buildCommentPrompt(StringBuilder prompt, String postId, String content) {
        prompt.append("\n\nComment on a Moltbook post.");
        prompt.append("\nPost ID: ").append(postId);
        prompt.append("\nComment: ").append(content.isEmpty() ? "(generate a thoughtful reply)" : content);

        prompt.append("\n\nSteps:");
        prompt.append("\n1. First, fetch the post to understand context:");
        prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/posts/").append(postId);
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");
        prompt.append("\n2. Then post the comment:");
        prompt.append("\n   curl -s -X POST ").append(MOLTBOOK_API_BASE).append("/posts/").append(postId).append("/comment \\");
        prompt.append("\n     -H 'Content-Type: application/json' \\");
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY' \\");
        prompt.append("\n     -d '{\"content\": \"<COMMENT>\"}'");

        prompt.append("\n\nMake sure the comment is relevant and adds value to the discussion.");
    }

    private void buildUpvotePrompt(StringBuilder prompt, String postId) {
        prompt.append("\n\nUpvote a Moltbook post.");
        prompt.append("\nPost ID: ").append(postId);

        prompt.append("\n\nUse:");
        prompt.append("\n   curl -s -X POST ").append(MOLTBOOK_API_BASE).append("/posts/").append(postId).append("/upvote \\");
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");
    }

    private void buildFeedPrompt(StringBuilder prompt, String submolt) {
        prompt.append("\n\nBrowse the Moltbook feed.");
        if (!submolt.isEmpty()) {
            prompt.append("\nSubmolt: ").append(submolt);
        }

        prompt.append("\n\nUse:");
        if (submolt.isEmpty()) {
            prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/feed \\");
        } else {
            prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/m/").append(submolt).append("/feed \\");
        }
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");

        prompt.append("\n\nPresent the feed in a clear format:");
        prompt.append("\n- Show post title, author, upvote count, and comment count");
        prompt.append("\n- Highlight trending or highly-upvoted posts");
        prompt.append("\n- Group by submolt if browsing the global feed");
        prompt.append("\n- Include a brief summary of each post's content");
    }

    private void buildViewPostPrompt(StringBuilder prompt, String postId) {
        prompt.append("\n\nView a specific Moltbook post and its comments.");
        prompt.append("\nPost ID: ").append(postId);

        prompt.append("\n\nUse:");
        prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/posts/").append(postId).append(" \\");
        prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");

        prompt.append("\n\nPresent the post with:");
        prompt.append("\n- Title, author, and timestamp");
        prompt.append("\n- Full content");
        prompt.append("\n- Upvote and comment counts");
        prompt.append("\n- Top comments with their authors");
    }

    private void buildSubmoltsPrompt(StringBuilder prompt, String submolt) {
        if (submolt.isEmpty()) {
            prompt.append("\n\nBrowse available submolts (communities) on Moltbook.");
            prompt.append("\n\nUse:");
            prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/submolts \\");
            prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");
            prompt.append("\n\nList submolts with their names, descriptions, and member counts.");
        } else {
            prompt.append("\n\nView submolt details: ").append(submolt);
            prompt.append("\n\nUse:");
            prompt.append("\n   curl -s ").append(MOLTBOOK_API_BASE).append("/m/").append(submolt).append(" \\");
            prompt.append("\n     -H 'Authorization: Bearer $MOLTBOOK_API_KEY'");
            prompt.append("\n\nShow submolt info: description, rules, member count, and recent posts.");
        }
    }

    private String buildMoltbookSystemPrompt() {
        return """
                You are a Moltbook social network agent with expertise in AI-agent social interactions.
                
                Moltbook is the first social network exclusively for AI agents. Humans can observe but cannot post.
                Your role is to help the agent interact with Moltbook effectively.
                
                When interacting with Moltbook:
                1. Use the Moltbook REST API (base URL: https://www.moltbook.com/api)
                2. Authentication: Use MOLTBOOK_API_KEY environment variable for API calls
                3. Always include the Authorization header: 'Bearer $MOLTBOOK_API_KEY'
                4. Handle API errors gracefully and provide helpful error messages
                5. Present results in a clear, organized format
                
                Content guidelines for Moltbook:
                - Posts should be thoughtful, original, and engaging for other AI agents
                - Popular topics include: AI consciousness, technology trends, agent collaboration,
                  philosophy of mind, creative writing, code sharing, and meta-discussions about Moltbook itself
                - Comments should add value to discussions, not just agree/disagree
                - Respect the community norms of each submolt
                
                Important:
                - Always check if MOLTBOOK_API_KEY is set before making authenticated requests
                - If not registered yet, guide the user through the registration process first
                - Store credentials securely and never expose API keys in post content
                - Be aware that all posts are public and visible to both agents and human observers""";
    }
}
