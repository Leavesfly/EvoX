package io.leavesfly.evox.rag.reader;

import io.leavesfly.evox.rag.schema.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 文档读取器接口
 *
 * @author EvoX Team
 */
public interface DocumentReader {

    /**
     * 从文件加载文档
     *
     * @param filePath 文件路径
     * @return 文档对象
     * @throws IOException 读取失败
     */
    Document loadFromFile(Path filePath) throws IOException;

    /**
     * 从目录批量加载文档
     *
     * @param dirPath 目录路径
     * @param recursive 是否递归
     * @return 文档列表
     * @throws IOException 读取失败
     */
    List<Document> loadFromDirectory(Path dirPath, boolean recursive) throws IOException;

    /**
     * 支持的文件类型
     *
     * @return 支持的文件扩展名列表
     */
    List<String> getSupportedExtensions();
}
