package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.SupabaseStorageProperties;
import com.phototransform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Supabase Storage 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
public class SupabaseStorageServiceImpl implements StorageService {

    private final SupabaseStorageProperties supabaseProperties;
    private final RestTemplate restTemplate;

    private static final String ORIGINALS_DIR = "originals";
    private static final String RESULTS_DIR = "results";

    @Override
    public String store(MultipartFile file, String taskId) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String fileName = ORIGINALS_DIR + "/" + taskId + "_original" + extension;

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(500, "读取上传文件失败: " + e.getMessage(), e);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = detectContentType(extension);
        }

        return upload(fileName, data, contentType);
    }

    @Override
    public String store(byte[] data, String fileName) {
        String extension = extractExtension(fileName);
        String contentType = detectContentType(extension);
        String path = RESULTS_DIR + "/" + fileName;
        return upload(path, data, contentType);
    }

    @Override
    public byte[] readByUrl(String url) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(500, "下载文件失败，HTTP " + response.getStatusCodeValue());
            }
            byte[] data = response.getBody();
            if (data == null || data.length == 0) {
                throw new BusinessException(500, "下载文件为空");
            }
            return data;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "下载文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null) {
            return;
        }
        String objectPath = extractObjectPath(url);
        if (objectPath.isEmpty()) {
            log.warn("无法从 URL 解析对象路径，跳过删除: {}", url);
            return;
        }
        try {
            String apiUrl = supabaseProperties.getUrl() + "/storage/v1/object/" + objectPath;
            HttpHeaders headers = authHeaders();
            restTemplate.exchange(apiUrl, HttpMethod.DELETE,
                    new HttpEntity<>(headers), String.class);
            log.info("已删除文件: {}", objectPath);
        } catch (Exception e) {
            log.warn("删除文件失败（可能已过期）: {} - {}", objectPath, e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 上传文件到 Supabase Storage
     */
    private String upload(String objectPath, byte[] data, String contentType) {
        String uploadUrl = supabaseProperties.getUrl() + "/storage/v1/object/" + objectPath;
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(data, headers),
                    String.class);

            HttpStatus status = response.getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new BusinessException(500,
                        "上传文件失败，HTTP " + status.value() + ": " + response.getBody());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "上传文件失败: " + e.getMessage(), e);
        }

        log.info("文件上传成功: {}", objectPath);
        return supabaseProperties.getUrl()
                + "/storage/v1/object/public/" + objectPath;
    }

    /**
     * 构建认证 Header
     */
    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseProperties.getServiceRoleKey());
        return headers;
    }

    /**
     * 从 Supabase 公开 URL 提取 bucket/object 路径
     * https://xxx.supabase.co/storage/v1/object/public/photoX/originals/file.png → photoX/originals/file.png
     */
    private String extractObjectPath(String url) {
        if (url == null || !url.contains("/object/public/")) {
            return "";
        }
        return url.substring(url.indexOf("/object/public/") + "/object/public/".length());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String detectContentType(String extension) {
        switch (extension.toLowerCase()) {
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".jpg":
            case ".jpeg":
            default:
                return "image/jpeg";
        }
    }
}
