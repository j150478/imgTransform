package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.AppStorageProperties;
import com.phototransform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件系统存储服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalStorageServiceImpl implements StorageService {

    private final AppStorageProperties storageProperties;

    private Path storagePath;

    /**
     * 初始化本地存储目录
     *
     * <p>在 Bean 初始化后创建配置指定的存储根目录，确保目录存在。
     */
    @PostConstruct
    public void init() {
        // 1. 解析存储根目录的绝对路径
        storagePath = Paths.get(storageProperties.getLocalPath()).toAbsolutePath();
        // 2. 目录不存在时自动创建
        try {
            Files.createDirectories(storagePath);
            log.info("本地存储目录初始化完成: {}", storagePath);
        } catch (IOException e) {
            throw new BusinessException(500, "创建存储目录失败: " + storagePath, e);
        }
    }

    @Override
    public String store(MultipartFile file, String taskId) {
        // 1. 提取原始文件的扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = StorageUtils.extractExtension(originalFilename);
        // 2. 生成唯一文件名（taskId + _original + 扩展名）
        String fileName = taskId + "_original" + extension;

        // 3. 将上传文件写入本地文件系统
        Path targetPath = storagePath.resolve(fileName);
        try {
            file.transferTo(targetPath.toFile());
            log.info("文件存储成功: {}", targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败: " + fileName, e);
        }

        // 4. 构建并返回访问 URL
        return buildUrl(fileName);
    }

    @Override
    public String store(byte[] data, String fileName) {
        // 1. 构造目标文件路径
        Path targetPath = storagePath.resolve(fileName);
        // 2. 将字节数据写入文件
        try {
            Files.write(targetPath, data);
            log.info("文件存储成功: {}", targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败: " + fileName, e);
        }
        // 3. 构建并返回访问 URL
        return buildUrl(fileName);
    }

    @Override
    public void deleteByUrl(String url) {
        // 1. 从 URL 中提取文件名
        String fileName = extractFileNameFromUrl(url);
        // 2. 删除本地文件（不存在时静默忽略）
        Path filePath = storagePath.resolve(fileName);
        try {
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", filePath);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", filePath, e);
        }
    }

    @Override
    public byte[] readByUrl(String url) {
        // 1. 从 URL 中提取文件名
        String fileName = extractFileNameFromUrl(url);
        // 2. 读取文件字节数据
        Path filePath = storagePath.resolve(fileName);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException(500, "读取文件失败: " + fileName, e);
        }
    }

    private String extractFileNameFromUrl(String url) {
        // 1. 校验 URL 非空且包含路径分隔符
        if (url == null || !url.contains("/")) {
            throw new BusinessException(400, "无效的文件URL: " + url);
        }
        // 2. 提取最后一个斜杠后方的文件名
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String buildUrl(String fileName) {
        // 1. 获取路径前缀并确保以斜杠结尾
        String prefix = storageProperties.getUrlPrefix();
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        // 2. 拼接文件名生成完整访问 URL
        return prefix + fileName;
    }

}
