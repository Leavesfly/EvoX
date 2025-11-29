package io.leavesfly.evox.tools.search;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * arXiv论文搜索工具
 * 支持通过arXiv API搜索学术论文
 *
 * @author EvoX Team
 */
@Slf4j
public class ArxivSearchTool extends BaseTool {

    private static final String ARXIV_API_BASE = "http://export.arxiv.org/api/query";
    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String ARXIV_NAMESPACE = "http://arxiv.org/schemas/atom";
    private static final String OPENSEARCH_NAMESPACE = "http://a9.com/-/spec/opensearch/1.1/";

    private final HttpClient httpClient;

    public ArxivSearchTool() {
        this.name = "arxiv_search";
        this.description = "Search academic papers on arXiv by keywords, categories, or IDs";

        this.inputs = new HashMap<>();

        Map<String, String> queryDef = new HashMap<>();
        queryDef.put("type", "string");
        queryDef.put("description", "Search query (e.g., 'all:electron', 'cat:cs.AI', 'ti:transformer')");
        inputs.put("query", queryDef);

        Map<String, String> idListDef = new HashMap<>();
        idListDef.put("type", "string");
        idListDef.put("description", "Comma-separated list of arXiv IDs (e.g., '1234.5678,2345.6789')");
        inputs.put("id_list", idListDef);

        Map<String, String> maxResultsDef = new HashMap<>();
        maxResultsDef.put("type", "integer");
        maxResultsDef.put("description", "Maximum number of results to return (default: 10)");
        inputs.put("max_results", maxResultsDef);

        Map<String, String> startDef = new HashMap<>();
        startDef.put("type", "integer");
        startDef.put("description", "Starting index for results (default: 0)");
        inputs.put("start", startDef);

        this.required = List.of(); // query或id_list至少一个

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            String query = getParameter(parameters, "query", "");
            String idList = getParameter(parameters, "id_list", "");
            Integer maxResults = getParameter(parameters, "max_results", 10);
            Integer start = getParameter(parameters, "start", 0);

            if (query.isEmpty() && idList.isEmpty()) {
                return ToolResult.failure("Either 'query' or 'id_list' must be provided");
            }

            // 构建请求URL
            String url = buildRequestUrl(query, idList, start, maxResults);
            log.debug("Searching arXiv: {}", url);

            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ToolResult.failure("arXiv API request failed with status: " + response.statusCode());
            }

            // 解析XML响应
            Map<String, Object> result = parseArxivResponse(response.body());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("query", query);
            metadata.put("id_list", idList);
            metadata.put("max_results", maxResults);
            metadata.put("start", start);
            metadata.put("total_results", result.get("total_results"));

            return ToolResult.success(result, metadata);

        } catch (Exception e) {
            log.error("Error searching arXiv", e);
            return ToolResult.failure("Error searching arXiv: " + e.getMessage());
        }
    }

    /**
     * 构建arXiv API请求URL
     */
    private String buildRequestUrl(String query, String idList, int start, int maxResults) {
        StringBuilder url = new StringBuilder(ARXIV_API_BASE);
        url.append("?start=").append(start);
        url.append("&max_results=").append(maxResults);

        if (!query.isEmpty()) {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            url.append("&search_query=").append(encodedQuery);
        }

        if (!idList.isEmpty()) {
            url.append("&id_list=").append(idList);
        }

        return url.toString();
    }

    /**
     * 解析arXiv XML响应
     */
    private Map<String, Object> parseArxivResponse(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            Map<String, Object> result = new HashMap<>();

            // 提取元数据
            int totalResults = getIntegerValue(doc, "totalResults", OPENSEARCH_NAMESPACE);
            int startIndex = getIntegerValue(doc, "startIndex", OPENSEARCH_NAMESPACE);
            int itemsPerPage = getIntegerValue(doc, "itemsPerPage", OPENSEARCH_NAMESPACE);

            result.put("total_results", totalResults);
            result.put("start_index", startIndex);
            result.put("items_per_page", itemsPerPage);

            // 提取论文条目
            List<Map<String, Object>> papers = new ArrayList<>();
            NodeList entries = doc.getElementsByTagNameNS(ATOM_NAMESPACE, "entry");

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                Map<String, Object> paper = parsePaperEntry(entry);
                papers.add(paper);
            }

            result.put("papers", papers);
            return result;

        } catch (Exception e) {
            log.error("Error parsing arXiv XML response", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "XML parsing error: " + e.getMessage());
            errorResult.put("papers", List.of());
            return errorResult;
        }
    }

    /**
     * 解析单篇论文条目
     */
    private Map<String, Object> parsePaperEntry(Element entry) {
        Map<String, Object> paper = new HashMap<>();

        // 基本信息
        String id = getElementText(entry, "id", ATOM_NAMESPACE);
        paper.put("id", id);

        if (id != null && id.contains("/")) {
            paper.put("arxiv_id", id.substring(id.lastIndexOf('/') + 1));
        }

        paper.put("title", cleanText(getElementText(entry, "title", ATOM_NAMESPACE)));
        paper.put("summary", cleanText(getElementText(entry, "summary", ATOM_NAMESPACE)));
        paper.put("published", getElementText(entry, "published", ATOM_NAMESPACE));
        paper.put("updated", getElementText(entry, "updated", ATOM_NAMESPACE));

        // 作者列表
        List<String> authors = new ArrayList<>();
        NodeList authorNodes = entry.getElementsByTagNameNS(ATOM_NAMESPACE, "author");
        for (int i = 0; i < authorNodes.getLength(); i++) {
            Element author = (Element) authorNodes.item(i);
            String name = getElementText(author, "name", ATOM_NAMESPACE);
            if (name != null) {
                authors.add(name);
            }
        }
        paper.put("authors", authors);

        // 分类
        List<String> categories = new ArrayList<>();
        NodeList categoryNodes = entry.getElementsByTagNameNS(ATOM_NAMESPACE, "category");
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            Element category = (Element) categoryNodes.item(i);
            String term = category.getAttribute("term");
            if (term != null && !term.isEmpty()) {
                categories.add(term);
            }
        }
        paper.put("categories", categories);

        // 主要分类
        NodeList primaryCatNodes = entry.getElementsByTagNameNS(ARXIV_NAMESPACE, "primary_category");
        if (primaryCatNodes.getLength() > 0) {
            Element primaryCat = (Element) primaryCatNodes.item(0);
            paper.put("primary_category", primaryCat.getAttribute("term"));
        }

        // 链接
        Map<String, String> links = new HashMap<>();
        NodeList linkNodes = entry.getElementsByTagNameNS(ATOM_NAMESPACE, "link");
        for (int i = 0; i < linkNodes.getLength(); i++) {
            Element link = (Element) linkNodes.item(i);
            String rel = link.getAttribute("rel");
            String href = link.getAttribute("href");
            String title = link.getAttribute("title");

            if ("alternate".equals(rel)) {
                links.put("html", href);
                paper.put("url", href);
            } else if ("pdf".equals(title)) {
                links.put("pdf", href);
            }
        }
        paper.put("links", links);

        // arXiv特定字段
        paper.put("comment", getElementText(entry, "comment", ARXIV_NAMESPACE));
        paper.put("journal_ref", getElementText(entry, "journal_ref", ARXIV_NAMESPACE));
        paper.put("doi", getElementText(entry, "doi", ARXIV_NAMESPACE));

        return paper;
    }

    /**
     * 获取元素文本内容
     */
    private String getElementText(Element parent, String tagName, String namespace) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    /**
     * 获取整数值
     */
    private int getIntegerValue(Document doc, String tagName, String namespace) {
        NodeList nodes = doc.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() > 0) {
            try {
                return Integer.parseInt(nodes.item(0).getTextContent());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 清理文本（移除多余空白）
     */
    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}
