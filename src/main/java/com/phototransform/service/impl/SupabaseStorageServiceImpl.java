package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.StorageUtils;
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
        // 1. 提取扩展名并构造对象存储路径
        String originalFilename = file.getOriginalFilename();
        String extension = StorageUtils.extractExtension(originalFilename);
        String fileName = ORIGINALS_DIR + "/" + taskId + "_original" + extension;

        // 2. 读取上传文件的字节数据
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(500, "读取上传文件失败: " + e.getMessage(), e);
        }

        // 3. 确定 Content-Type（从文件元数据或扩展名推断）
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = detectContentType(extension);
        }

        // 4. 上传到 Supabase Storage
        return upload(fileName, data, contentType);
    }

    @Override
    public String store(byte[] data, String fileName) {
        // 1. 检测文件扩展名
        String extension = StorageUtils.extractExtension(fileName);
        // 2. 根据扩展名映射 Content-Type
        String contentType = detectContentType(extension);
        // 3. 构造结果目录路径并上传
        String path = RESULTS_DIR + "/" + fileName;
        return upload(path, data, contentType);
    }

    @Override
    public byte[] readByUrl(String url) {
        // 1. 发起 HTTP GET 请求从 URL 下载文件
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            // 2. 检查 HTTP 状态码
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(500, "下载文件失败，HTTP " + response.getStatusCodeValue());
            }
            // 3. 校验响应体非空
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
        // 1. 检查 URL 非空
        if (url == null) {
            return;
        }
        // 2. 从 URL 解析 Supabase 对象路径
        String objectPath = extractObjectPath(url);
        if (objectPath.isEmpty()) {
            log.warn("无法从 URL 解析对象路径，跳过删除: {}", url);
            return;
        }
        // 3. 发起 DELETE 请求删除远端文件
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
        // 1. 构造带 bucket 的对象路径和上传 URL
        String bucketPath = supabaseProperties.getBucket() + "/" + objectPath;
        String uploadUrl = supabaseProperties.getUrl() + "/storage/v1/object/" + bucketPath;
        // 2. 设置认证头和 Content-Type
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        // 3. 发起 POST 请求上传文件
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(data, headers),
                    String.class);

            // 4. 检查响应状态码
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

        // 5. 返回公开可访问的文件 URL
        log.info("文件上传成功: {}", bucketPath);
        return supabaseProperties.getUrl()
                + "/storage/v1/object/public/" + bucketPath;
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
        // 1. 校验 URL 包含公开访问路径前缀
        if (url == null || !url.contains("/object/public/")) {
            return "";
        }
        // 2. 截取 /object/public/ 之后的路径
        return url.substring(url.indexOf("/object/public/") + "/object/public/".length());
    }

    private String detectContentType(String extension) {
        // 1. 根据文件扩展名映射对应的 MIME 类型
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
