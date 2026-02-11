package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.SeedreamClientConfig;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.service.SeedreamImageService;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 火山引擎 Seedream 图像生成服务实现类
 *
 * 基于 volcengine-java-sdk-ark-runtime SDK 实现
 * 支持文生图、图生图、组图生成等多种图像生成能力
 */
@Slf4j
@Service
public class SeedreamImageServiceImpl implements SeedreamImageService {

    @Autowired
    private SeedreamClientConfig config;

    private ArkService arkService;

    /**
     * 初始化 ArkService
     *
     * 使用配置中的 API Key 和 Base URL 初始化 SDK 服务
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化 Seedream 图像生成服务...");

        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();

        this.arkService = ArkService.builder()
                .baseUrl(config.getBaseUrl())
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(config.getApiKey())
                .build();

        log.info("Seedream 图像生成服务初始化完成，Base URL: {}", config.getBaseUrl());
    }

    @Override
    public ImageGenerationResult generateImage(ImageGenerationRequest request) {
        String taskId = generateTaskId();
        log.info("[{}] 开始文生图任务, prompt: {}", taskId, request.getPrompt());

        try {
            GenerateImagesRequest generateRequest = buildGenerateRequest(request);
            // 使用正确的 SDK 方法名
            ImagesResponse response = arkService.generateImages(generateRequest);

            log.info("[{}] 图像生成成功, 生成图片数量: {}", taskId, 
                    response.getData() != null ? response.getData().size() : 0);
            return convertToResult(response, request, taskId);

        } catch (Exception e) {
            log.error("[{}] 图像生成失败: {}", taskId, e.getMessage(), e);
            throw new BusinessException(500, "图像生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageGenerationResult generateImageWithReference(ImageGenerationRequest request) {
        String taskId = generateTaskId();
        log.info("[{}] 开始图生图任务, 参考图数量: {}", taskId,
                request.getReferenceImages() != null ? request.getReferenceImages().size() : 0);

        if (request.getReferenceImages() == null || request.getReferenceImages().isEmpty()) {
            throw new BusinessException(400, "图生图任务需要提供至少一张参考图像");
        }

        return generateImage(request);
    }

    @Override
    public ImageGenerationResult generateImageSet(ImageGenerationRequest request) {
        String taskId = generateTaskId();
        log.info("[{}] 开始组图生成任务", taskId);

        // 强制设置组图模式
        request.setSequentialImageGeneration("auto");

        if (request.getN() == null || request.getN() < 2) {
            request.setN(4); // 默认生成4张组图
        }

        return generateImage(request);
    }

    @Override
    public List<ImageGenerationResult> batchGenerateImages(List<ImageGenerationRequest> requests) {
        log.info("开始批量图像生成任务, 任务数量: {}", requests.size());

        List<ImageGenerationResult> results = new ArrayList<>();
        for (ImageGenerationRequest request : requests) {
            try {
                results.add(generateImage(request));
            } catch (Exception e) {
                log.error("批量任务中单个任务失败: {}", e.getMessage());
                // 创建失败结果
                results.add(ImageGenerationResult.builder()
                        .taskId(generateTaskId())
                        .status("FAILED")
                        .prompt(request.getPrompt())
                        .errorMessage(e.getMessage())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
        return results;
    }

    /**
     * 构建 SDK 请求对象
     */
    private GenerateImagesRequest buildGenerateRequest(ImageGenerationRequest request) {
        GenerateImagesRequest.Builder builder = GenerateImagesRequest.builder()
                .model(getModel(request))
                .prompt(request.getPrompt())
                .size(getSize(request))
                .responseFormat(getResponseFormat(request))
                .watermark(getWatermark(request));

        // 添加参考图（图生图、多图生图）
        if (request.getReferenceImages() != null && !request.getReferenceImages().isEmpty()) {
            // 根据实际 SDK 版本，可能使用 image 或 images 方法
            // 尝试使用第一张图作为单图参考
            if (request.getReferenceImages().size() == 1) {
                builder.image(request.getReferenceImages().get(0));
            } else {
                // 多图参考 - 根据 SDK 版本可能需要不同的处理方式
                for (String img : request.getReferenceImages()) {
                    // 某些版本支持多次调用 image 方法
                    builder.image(img);
                }
            }
            log.debug("添加参考图，数量: {}", request.getReferenceImages().size());
        }

        // 组图模式
        if (request.getSequentialImageGeneration() != null) {
            builder.sequentialImageGeneration(request.getSequentialImageGeneration());
        }

        return builder.build();
    }

    /**
     * 转换 SDK 响应为项目 DTO
     */
    private ImageGenerationResult convertToResult(ImagesResponse response,
                                                  ImageGenerationRequest request,
                                                  String taskId) {
        List<ImageGenerationResult.GeneratedImage> images = new ArrayList<>();
        
        if (response.getData() != null) {
            int index = 0;
            for (ImagesResponse.Image img : response.getData()) {
                images.add(ImageGenerationResult.GeneratedImage.builder()
                        .index(index++)
                        .url(img.getUrl())
                        .b64Json(img.getB64Json())
                        .build());
            }
        }

        return ImageGenerationResult.builder()
                .taskId(taskId)
                .status("SUCCESS")
                .images(images)
                .model(request.getModel())
                .prompt(request.getPrompt())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取模型名称
     */
    private String getModel(ImageGenerationRequest request) {
        return request.getModel() != null ? request.getModel() : config.getModelName();
    }

    /**
     * 获取图像尺寸
     */
    private String getSize(ImageGenerationRequest request) {
        return request.getSize() != null ? request.getSize() : config.getDefaultSize();
    }

    /**
     * 获取响应格式
     */
    private String getResponseFormat(ImageGenerationRequest request) {
        String format = request.getResponseFormat() != null
                ? request.getResponseFormat()
                : config.getDefaultResponseFormat();
        // 返回字符串格式: "url" 或 "b64_json"
        return "b64_json".equalsIgnoreCase(format) ? "b64_json" : "url";
    }

    /**
     * 获取水印设置
     */
    private Boolean getWatermark(ImageGenerationRequest request) {
        return request.getWatermark() != null ? request.getWatermark() : config.getDefaultWatermark();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "SD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
