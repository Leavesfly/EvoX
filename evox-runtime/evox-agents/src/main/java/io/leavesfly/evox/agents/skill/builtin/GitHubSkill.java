package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class GitHubSkill extends BaseSkill {

    public GitHubSkill() {
        setName("github");
        setDescription("Interact with GitHub repositories. "
                + "Query notifications, issues, pull requests, repository info, and perform common GitHub operations.");

        setSystemPrompt(buildGitHubSystemPrompt());

        setRequiredTools(List.of("http", "shell"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> operationParam = new HashMap<>();
        operationParam.put("type", "string");
        operationParam.put("description", "Operation: 'notifications', 'issues', 'prs', 'repo_info', 'search', 'create_issue'");
        inputParams.put("operation", operationParam);

        Map<String, String> repoParam = new HashMap<>();
        repoParam.put("type", "string");
        repoParam.put("description", "Repository in format 'owner/repo' (e.g., 'octocat/Hello-World')");
        inputParams.put("repo", repoParam);

        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "Search query or filter (optional)");
        inputParams.put("query", queryParam);

        Map<String, String> stateParam = new HashMap<>();
        stateParam.put("type", "string");
        stateParam.put("description", "State filter: 'open', 'closed', 'all' (default: 'open')");
        inputParams.put("state", stateParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("operation"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String operation = context.getParameters().getOrDefault("operation", "notifications").toString();
        String repo = context.getParameters().getOrDefault("repo", "").toString();
        String query = context.getParameters().getOrDefault("query", "").toString();
        String state = context.getParameters().getOrDefault("state", "open").toString();

        String prompt = buildPrompt(context.getInput(), context.getAdditionalContext());

        StringBuilder githubPrompt = new StringBuilder(prompt);
        githubPrompt.append("\n\nGitHub Operation: ").append(operation);

        switch (operation) {
            case "notifications" -> {
                githubPrompt.append("\n\nFetch and summarize GitHub notifications.");
                githubPrompt.append("\nUse: curl -s -H 'Authorization: token $GITHUB_TOKEN' https://api.github.com/notifications");
                githubPrompt.append("\nGroup notifications by repository and type (issue, PR, release).");
                githubPrompt.append("\nHighlight unread and important notifications.");
            }
            case "issues" -> {
                githubPrompt.append("\nRepository: ").append(repo);
                githubPrompt.append("\nState: ").append(state);
                githubPrompt.append("\n\nList issues for the repository.");
                githubPrompt.append("\nUse: curl -s -H 'Authorization: token $GITHUB_TOKEN' 'https://api.github.com/repos/").append(repo).append("/issues?state=").append(state).append("'");
                githubPrompt.append("\nShow: number, title, author, labels, created date, and comment count.");
            }
            case "prs" -> {
                githubPrompt.append("\nRepository: ").append(repo);
                githubPrompt.append("\nState: ").append(state);
                githubPrompt.append("\n\nList pull requests for the repository.");
                githubPrompt.append("\nUse: curl -s -H 'Authorization: token $GITHUB_TOKEN' 'https://api.github.com/repos/").append(repo).append("/pulls?state=").append(state).append("'");
                githubPrompt.append("\nShow: number, title, author, branch, review status, and CI status.");
            }
            case "repo_info" -> {
                githubPrompt.append("\nRepository: ").append(repo);
                githubPrompt.append("\n\nGet repository information.");
                githubPrompt.append("\nUse: curl -s -H 'Authorization: token $GITHUB_TOKEN' https://api.github.com/repos/").append(repo);
                githubPrompt.append("\nShow: description, stars, forks, language, last updated, open issues count.");
            }
            case "search" -> {
                githubPrompt.append("\nSearch query: ").append(query);
                githubPrompt.append("\n\nSearch GitHub repositories, issues, or code.");
                githubPrompt.append("\nUse: curl -s 'https://api.github.com/search/repositories?q=").append(query).append("'");
                githubPrompt.append("\nPresent top results with name, description, stars, and language.");
            }
            case "create_issue" -> {
                githubPrompt.append("\nRepository: ").append(repo);
                githubPrompt.append("\nIssue details from input: ").append(context.getInput());
                githubPrompt.append("\n\nCreate a new issue in the repository.");
                githubPrompt.append("\nParse the input to extract title, body, and labels.");
                githubPrompt.append("\nUse POST to https://api.github.com/repos/").append(repo).append("/issues");
            }
            default -> githubPrompt.append("\n\nUnknown operation. Available: notifications, issues, prs, repo_info, search, create_issue.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "github");
        metadata.put("operation", operation);
        if (!repo.isEmpty()) metadata.put("repo", repo);

        return SkillResult.success(githubPrompt.toString(), metadata);
    }

    private String buildGitHubSystemPrompt() {
        return """
                You are a GitHub integration assistant with expertise in GitHub API and workflows.
                
                When interacting with GitHub:
                1. Use the GitHub REST API v3 (api.github.com)
                2. Authentication: Use GITHUB_TOKEN environment variable for API calls
                3. Handle rate limiting gracefully
                4. Present results in a clear, organized format
                5. For notifications, group by repository and highlight important items
                6. For issues/PRs, show relevant metadata (labels, assignees, status)
                7. Use shell tool with curl for API calls
                
                Important:
                - Always check if GITHUB_TOKEN is set before making authenticated requests
                - Handle API errors gracefully and provide helpful error messages
                - Respect GitHub API rate limits (5000 requests/hour for authenticated users)""";
    }
}
