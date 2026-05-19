package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.AppStorageProperties;
import com.phototransform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class LocalStorageServiceImpl implements StorageService {

    private final AppStorageProperties storageProperties;

    private Path storagePath;

    @PostConstruct
    public void init() {
        storagePath = Paths.get(storageProperties.getLocalPath()).toAbsolutePath();
        try {
            Files.createDirectories(storagePath);
            log.info("本地存储目录初始化完成: {}", storagePath);
        } catch (IOException e) {
            throw new BusinessException(500, "创建存储目录失败: " + storagePath, e);
        }
    }

    @Override
    public String store(MultipartFile file, String taskId) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String fileName = taskId + "_original" + extension;

        Path targetPath = storagePath.resolve(fileName);
        try {
            file.transferTo(targetPath.toFile());
            log.info("文件存储成功: {}", targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败: " + fileName, e);
        }

        return buildUrl(fileName);
    }

    @Override
    public String store(byte[] data, String fileName) {
        Path targetPath = storagePath.resolve(fileName);
        try {
            Files.write(targetPath, data);
            log.info("文件存储成功: {}", targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败: " + fileName, e);
        }

        return buildUrl(fileName);
    }

    @Override
    public boolean exists(String fileName) {
        return Files.exists(storagePath.resolve(fileName));
    }

    @Override
    public byte[] readByUrl(String url) {
        String fileName = extractFileNameFromUrl(url);
        Path filePath = storagePath.resolve(fileName);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException(500, "读取文件失败: " + fileName, e);
        }
    }

    private String extractFileNameFromUrl(String url) {
        if (url == null || !url.contains("/")) {
            throw new BusinessException(400, "无效的文件URL: " + url);
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String buildUrl(String fileName) {
        String prefix = storageProperties.getUrlPrefix();
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + fileName;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
