package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.SeedreamClientConfig;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.service.SeedreamClient;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Seedream API 客户端实现类。
 *
 * <p>封装火山引擎 Seedream SDK 的完整调用生命周期，包括 ArkService 客户端初始化、
 * SDK 请求构建、SDK 调用执行、响应解析和错误处理。
 * 本类所有 public 方法保证不抛出异常，错误通过返回结果传达。</p>
 *
 * @author PhotoTransform Team
 * @since 1.0.0
 * @see SeedreamClient
 * @see SeedreamClientConfig
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedreamClientImpl implements SeedreamClient {

    private final SeedreamClientConfig config;
    private final TaskIdGenerator taskIdGenerator;

    private ArkService arkService;

    /**
     * 初始化 ArkService 客户端。
     *
     * <p>使用配置中的 API Key 和 Base URL 初始化火山引擎 SDK 客户端。
     * 配置连接池、调度器和超时时间以优化性能。</p>
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化 Seedream API 客户端...");
        // 1. 验证配置有效性
        validateConfig();

        // 2. 构建 ArkService 客户端
        this.arkService = ArkService.builder()
                .baseUrl(config.getBaseUrl())
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .dispatcher(new Dispatcher())
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .apiKey(config.getApiKey())
                .build();

        log.info("Seedream API 客户端初始化完成，Base URL: {}", config.getBaseUrl());
    }

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request, GenerationCapability capability) {
        String taskId = taskIdGenerator.generate("SD");
        log.info("[{}] 开始执行 Seedream API 调用", taskId);

        try {
            // 1. 构建 SDK 请求
            GenerateImagesRequest sdkRequest = buildSdkRequest(request, capability);

            log.info("[{}] [SEEDREAM_REQUEST] prompt(len: {}): {}, refImages: {}, size: {}, model: {}, capability: {}",
                    taskId,
                    sdkRequest.getPrompt() != null ? sdkRequest.getPrompt().length() : 0,
                    sdkRequest.getPrompt(),
                    sdkRequest.getImage() != null ? sdkRequest.getImage().size() : 0,
                    sdkRequest.getSize(), sdkRequest.getModel(), capability.getDescription());

            // 2. 调用 SDK
            ImagesResponse response = arkService.generateImages(sdkRequest);

            // 3. 转换响应为结果 DTO
            return convertResponseToResult(response, request, capability, taskId);

        } catch (BusinessException e) {
            log.error("[{}] Seedream API 调用失败: {}", taskId, e.getMessage());
            return buildErrorResult(request, taskId, e.getMessage(), String.valueOf(e.getCode()));
        } catch (Exception e) {
            log.error("[{}] Seedream API 调用发生异常", taskId, e);
            return buildErrorResult(request, taskId, "Seedream API 调用过程中发生异常: " + e.getMessage(), "500");
        }
    }

    /**
     * 验证配置有效性。
     *
     * <p>检查必要的配置项是否已正确配置。</p>
     *
     * @throws BusinessException 当配置无效时抛出异常
     */
    private void validateConfig() {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new BusinessException(500, "Seedream API Key 未配置，请检查 seedream.api-key 配置项");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
            throw new BusinessException(500, "Seedream Base URL 未配置，请检查 seedream.base-url 配置项");
        }
    }

    /**
     * 构建 SDK 请求。
     *
     * <p>将应用层的请求对象和生成能力转换为 SDK 请求对象。</p>
     *
     * @param request    应用层请求
     * @param capability 使用的生成能力
     * @return SDK 请求对象
     */
    private GenerateImagesRequest buildSdkRequest(ImageGenerationRequest request,
                                                   GenerationCapability capability) {
        // 1. 构建基础请求参数（模型、提示词、尺寸、格式、水印、生成模式）
        GenerateImagesRequest.Builder builder = GenerateImagesRequest.builder()
                .model(getModel(request))
                .prompt(request.getPrompt())
                .size(getSize(request))
                .responseFormat(getResponseFormat(request))
                .watermark(getWatermark(request))
                .sequentialImageGeneration(capability.getGenerationMode().getSdkValue());

        // 2. 添加参考图
        List<String> referenceImages = request.getReferenceImages();
        if (referenceImages != null && !referenceImages.isEmpty()) {
            if (referenceImages.size() == 1) {
                builder.image(referenceImages.get(0));
            } else {
                for (String image : referenceImages) {
                    builder.image(image);
                }
            }
        }

        // 3. 组图模式下设置生成数量
        if (capability.isSequential() && request.getN() != null && request.getN() > 1) {
            // NOTE: SDK 版本不同，设置生成数量的方法可能不同，目前暂不实现。
        }

        return builder.build();
    }

    /**
     * 转换 SDK 响应为结果 DTO。
     *
     * <p>将 SDK 返回的响应对象转换为应用层的结果 DTO，处理 PARTIAL_SUCCESS 等状态。</p>
     *
     * @param response   SDK 响应
     * @param request    原始请求
     * @param capability 使用的生成能力
     * @param taskId     任务ID
     * @return 结果 DTO
     */
    private ImageGenerationResult convertResponseToResult(ImagesResponse response,
                                                          ImageGenerationRequest request,
                                                          GenerationCapability capability,
                                                          String taskId) {
        // 1. 检查顶层错误
        if (response.getError() != null) {
            String errorMsg = response.getError().getMessage() != null
                    ? response.getError().getMessage() : "API 返回错误";
            log.error("[{}] Seedream API 返回顶层错误: {}", taskId, errorMsg);
            return buildErrorResult(request, taskId, errorMsg, response.getError().getCode());
        }

        List<ImageGenerationResult.GeneratedImage> images = new ArrayList<>();
        boolean hasError = false;
        int successCount = 0;
        int errorCount = 0;

        // 2. 解析 SDK 响应中的图片数据
        if (response.getData() != null) {
            for (int i = 0; i < response.getData().size(); i++) {
                ImagesResponse.Image img = response.getData().get(i);

                ImageGenerationResult.GeneratedImage.GeneratedImageBuilder imageBuilder =
                        ImageGenerationResult.GeneratedImage.builder()
                                .index(i);

                // 2.1 检查单张图片是否生成失败
                boolean imageHasError = img.getUrl() == null && img.getB64Json() == null;

                if (imageHasError) {
                    hasError = true;
                    errorCount++;
                    imageBuilder.error("图片生成失败，未返回有效数据");
                    log.warn("[{}] 图片[{}]生成失败，未返回有效数据", taskId, i);
                } else {
                    successCount++;
                    imageBuilder.url(img.getUrl())
                               .b64Json(img.getB64Json());
                }

                images.add(imageBuilder.build());
            }
        }

        // 3. 确定最终状态
        GenerationStatus status;
        if (hasError) {
            status = images.stream().allMatch(img -> img.getError() != null)
                    ? GenerationStatus.FAILED : GenerationStatus.PARTIAL_SUCCESS;
        } else {
            status = GenerationStatus.SUCCESS;
        }

        // 4. 记录生成结果统计
        log.info("[{}] 生成任务完成，状态: {}, 成功: {}, 失败: {}",
                 taskId, status, successCount, errorCount);

        return ImageGenerationResult.builder()
                .taskId(taskId)
                .status(status)
                .images(images)
                .usedCapability(capability)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .model(getModel(request))
                .prompt(request.getPrompt())
                .build();
    }

    /**
     * 构建错误结果。
     *
     * <p>当生成失败时，构建包含错误信息的结果对象。</p>
     *
     * @param request     原始请求
     * @param taskId      任务ID
     * @param errorMsg    错误信息
     * @param errorCode   错误码
     * @return 错误结果对象
     */
    private ImageGenerationResult buildErrorResult(ImageGenerationRequest request,
                                                   String taskId,
                                                   String errorMsg,
                                                   String errorCode) {
        return ImageGenerationResult.builder()
                .taskId(taskId)
                .status(GenerationStatus.FAILED)
                .images(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .model(getModel(request))
                .prompt(request.getPrompt())
                .errorMessage(errorMsg)
                .errorCode(errorCode)
                .build();
    }

    // ==================== 配置获取方法 ====================

    /**
     * 获取模型名称。
     *
     * <p>优先使用请求中指定的模型，否则使用配置中的默认模型。</p>
     *
     * @param request 生成请求
     * @return 模型名称
     */
    private String getModel(ImageGenerationRequest request) {
        return request.getModel() != null ? request.getModel() : config.getModelName();
    }

    /**
     * 获取图像尺寸。
     *
     * <p>优先使用请求中指定的尺寸，否则使用配置中的默认尺寸。</p>
     *
     * @param request 生成请求
     * @return 图像尺寸
     */
    private String getSize(ImageGenerationRequest request) {
        return request.getSize() != null ? request.getSize() : config.getDefaultSize();
    }

    /**
     * 获取响应格式。
     *
     * <p>优先使用请求中指定的格式，否则使用配置中的默认格式。</p>
     *
     * @param request 生成请求
     * @return 响应格式（"url" 或 "b64_json"）
     */
    private String getResponseFormat(ImageGenerationRequest request) {
        String format = request.getResponseFormat() != null
                ? request.getResponseFormat()
                : config.getDefaultResponseFormat();
        return "b64_json".equalsIgnoreCase(format) ? "b64_json" : "url";
    }

    /**
     * 获取水印设置。
     *
     * <p>优先使用请求中指定的设置，否则使用配置中的默认设置。</p>
     *
     * @param request 生成请求
     * @return 是否开启水印
     */
    private Boolean getWatermark(ImageGenerationRequest request) {
        return request.getWatermark() != null ? request.getWatermark() : config.getDefaultWatermark();
    }
}
