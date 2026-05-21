package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.config.SeedreamClientConfig;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationMode;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.service.SeedreamImageService;
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
 * Seedream 图像生成服务实现类。
 *
 * <p>基于火山引擎 doubao-seedream-4.5 模型的图像生成服务实现类。
 * 支持文生图、图生图、组图生成等多种生成能力。</p>
 *
 * @author PhotoTransform Team
 * @see SeedreamImageService
 * @see GenerationCapability
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedreamImageServiceImpl implements SeedreamImageService {

    /**
     * 最大参考图数量限制（doubao-seedream-4.5 模型限制）
     */
    private static final int MAX_REFERENCE_IMAGES = 14;

    /**
     * 组图最大生成数量限制
     */
    private static final int MAX_SEQUENTIAL_IMAGES = 15;

    /**
     * 组图默认生成数量
     */
    private static final int DEFAULT_SEQUENTIAL_COUNT = 4;

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
        log.info("正在初始化 Seedream 图像生成服务...");
        validateConfig();

        this.arkService = ArkService.builder()
                .baseUrl(config.getBaseUrl())
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .dispatcher(new Dispatcher())
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .apiKey(config.getApiKey())
                .build();

        log.info("Seedream 图像生成服务初始化完成，Base URL: {}", config.getBaseUrl());
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

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request) {
        String taskId = taskIdGenerator.generate("SD");
        log.info("[{}] 开始图像生成任务", taskId);

        try {
            // 1. 验证请求参数
            validateRequest(request);

            // 2. 自动识别或获取生成能力
            GenerationCapability capability = determineCapability(request);
            log.info("[{}] 使用生成能力: {}", taskId, capability.getDescription());

            // 3. 执行生成
            return executeGeneration(request, capability, taskId);

        } catch (BusinessException e) {
            log.error("[{}] 生成任务失败: {}", taskId, e.getMessage());
            return buildErrorResult(request, taskId, e.getMessage(), String.valueOf(e.getCode()));
        } catch (Exception e) {
            log.error("[{}] 生成任务发生异常", taskId, e);
            return buildErrorResult(request, taskId, "生成过程中发生异常: " + e.getMessage(), "500");
        }
    }

    @Override
    public ImageGenerationResult generateWithCapability(ImageGenerationRequest request, GenerationCapability capability) {
        String taskId = taskIdGenerator.generate("SD");
        log.info("[{}] 开始指定能力生成任务: {}", taskId, capability.getDescription());

        try {
            // 验证请求与指定能力是否匹配
            validateCapabilityMatch(request, capability);

            // 设置能力到请求中
            request.setCapability(capability);

            // 执行生成
            return executeGeneration(request, capability, taskId);

        } catch (BusinessException e) {
            log.error("[{}] 生成任务失败: {}", taskId, e.getMessage());
            return buildErrorResult(request, taskId, e.getMessage(), String.valueOf(e.getCode()));
        } catch (Exception e) {
            log.error("[{}] 生成任务发生异常", taskId, e);
            return buildErrorResult(request, taskId, "生成过程中发生异常: " + e.getMessage(), "500");
        }
    }

    /**
     * 验证请求参数。
     *
     * <p>验证请求中的各个参数是否符合要求。</p>
     *
     * @param request 生成请求
     * @throws BusinessException 当参数验证失败时抛出
     */
    private void validateRequest(ImageGenerationRequest request) {
        // 验证提示词
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new BusinessException(400, "生成提示词不能为空");
        }
        if (request.getPrompt().length() > 2000) {
            throw new BusinessException(400, "生成提示词长度不能超过2000个字符");
        }

        // 验证参考图数量
        List<String> referenceImages = request.getReferenceImages();
        if (referenceImages != null && referenceImages.size() > MAX_REFERENCE_IMAGES) {
            throw new BusinessException(400,
                    String.format("参考图数量不能超过%d张，当前: %d", MAX_REFERENCE_IMAGES, referenceImages.size()));
        }

        // 验证组图生成数量
        GenerationMode mode = request.getMode();
        Integer n = request.getN();

        if (mode == GenerationMode.SEQUENTIAL || (mode == null && n != null && n > 1)) {
            // 组图模式
            int refCount = referenceImages == null ? 0 : referenceImages.size();
            int genCount = n == null ? DEFAULT_SEQUENTIAL_COUNT : n;

            // 检查组图总数限制
            int totalCount = refCount + genCount;
            if (totalCount > MAX_SEQUENTIAL_IMAGES) {
                int maxGen = MAX_SEQUENTIAL_IMAGES - refCount;
                throw new BusinessException(400, String.format(
                        "组图总数（参考图+生成图）不能超过%d张。当前参考图%d张，最多可生成%d张",
                        MAX_SEQUENTIAL_IMAGES, refCount, maxGen));
            }
        }

        // 验证尺寸
        if (request.getSize() != null && !request.getSize().matches("^(\\d+x\\d+|1K|2K|4K)$")) {
            throw new BusinessException(400, "图像尺寸格式不正确，应为具体尺寸（如1024x1024）或分辨率标识（1K/2K/4K）");
        }
    }

    /**
     * 确定生成能力。
     *
     * <p>根据请求参数自动识别应使用的生成能力。</p>
     *
     * @param request 生成请求
     * @return 识别到的生成能力
     */
    private GenerationCapability determineCapability(ImageGenerationRequest request) {
        // 如果请求中已显式指定能力，直接使用
        if (request.getCapability() != null) {
            return request.getCapability();
        }

        // 获取参考图数量和生成模式
        int refCount = request.getReferenceImages() == null ? 0 : request.getReferenceImages().size();
        GenerationMode mode = request.getMode();

        // 如果未指定模式，根据生成数量判断
        if (mode == null) {
            Integer n = request.getN();
            mode = (n != null && n > 1) ? GenerationMode.SEQUENTIAL : GenerationMode.SINGLE;
        }

        // 根据参考图数量和生成模式确定能力
        if (refCount == 0) {
            // 文生图
            return mode == GenerationMode.SEQUENTIAL
                    ? GenerationCapability.TEXT_TO_IMAGE_SET
                    : GenerationCapability.TEXT_TO_IMAGE;
        } else if (refCount == 1) {
            // 单图生图
            return mode == GenerationMode.SEQUENTIAL
                    ? GenerationCapability.SINGLE_IMAGE_TO_IMAGE_SET
                    : GenerationCapability.SINGLE_IMAGE_TO_IMAGE;
        } else {
            // 多图生图（2-14张）
            return mode == GenerationMode.SEQUENTIAL
                    ? GenerationCapability.MULTI_IMAGE_TO_IMAGE_SET
                    : GenerationCapability.MULTI_IMAGE_TO_IMAGE;
        }
    }

    /**
     * 验证能力与请求是否匹配。
     *
     * <p>验证显式指定的生成能力与请求参数是否匹配。</p>
     *
     * @param request    生成请求
     * @param capability 指定的生成能力
     * @throws BusinessException 当不匹配时抛出异常
     */
    private void validateCapabilityMatch(ImageGenerationRequest request, GenerationCapability capability) {
        int refCount = request.getReferenceImages() == null ? 0 : request.getReferenceImages().size();

        // 验证参考图数量是否满足能力要求
        if (!capability.validateReferenceImageCount(refCount)) {
            throw new BusinessException(400, String.format(
                    "指定的生成能力 [%s] 需要 %s，但当前提供了 %d 张参考图",
                    capability.getDescription(),
                    capability.getReferenceImageRequirementDesc(),
                    refCount));
        }

        // 验证生成模式是否匹配
        GenerationMode expectedMode = capability.getGenerationMode();
        GenerationMode actualMode = request.getMode();

        if (actualMode != null && actualMode != expectedMode) {
            throw new BusinessException(400, String.format(
                    "指定的生成能力 [%s] 要求生成模式为 [%s]，但请求中指定为 [%s]",
                    capability.getDescription(),
                    expectedMode.getDescription(),
                    actualMode.getDescription()));
        }
    }

    /**
     * 执行图像生成。
     *
     * <p>调用 SDK 执行实际的图像生成操作。</p>
     *
     * @param request    生成请求
     * @param capability 使用的生成能力
     * @param taskId     任务ID
     * @return 生成结果
     */
    private ImageGenerationResult executeGeneration(ImageGenerationRequest request,
                                                      GenerationCapability capability,
                                                      String taskId) {
        // 构建 SDK 请求
        GenerateImagesRequest sdkRequest = buildSdkRequest(request, capability);

        log.info("[{}] 调用 Seedream API, 模型: {}, 能力: {}",
                taskId, getModel(request), capability.getDescription());

        // 调用 SDK
        ImagesResponse response;
        try {
            response = arkService.generateImages(sdkRequest);
        } catch (Exception e) {
            log.error("[{}] Seedream API 调用失败", taskId, e);
            throw new BusinessException(500, "图像生成服务调用失败: " + e.getMessage());
        }

        // 转换响应为结果 DTO
        return convertResponseToResult(response, request, capability, taskId);
    }

    /**
     * 构建 SDK 请求。
     *
     * <p>将应用层的请求对象转换为 SDK 请求对象。</p>
     *
     * @param request    应用层请求
     * @param capability 使用的生成能力
     * @return SDK 请求对象
     */
    private GenerateImagesRequest buildSdkRequest(ImageGenerationRequest request,
                                                  GenerationCapability capability) {
        GenerateImagesRequest.Builder builder = GenerateImagesRequest.builder()
                .model(getModel(request))
                .prompt(request.getPrompt())
                .size(getSize(request))
                .responseFormat(getResponseFormat(request))
                .watermark(getWatermark(request))
                .sequentialImageGeneration(capability.getGenerationMode().getSdkValue());

        // 添加参考图
        List<String> referenceImages = request.getReferenceImages();
        if (referenceImages != null && !referenceImages.isEmpty()) {
            // 单图参考
            if (referenceImages.size() == 1) {
                builder.image(referenceImages.get(0));
            } else {
                // 多图参考：逐个添加
                for (String image : referenceImages) {
                    builder.image(image);
                }
            }
        }

        // 组图模式下设置生成数量
        if (capability.isSequential() && request.getN() != null && request.getN() > 1) {
            // 注意：SDK 版本不同，设置生成数量的方法可能不同
            // 此处假设 SDK 支持通过额外参数传递
            // 如果 SDK 不支持，可以通过 extraParams 传递
        }

        return builder.build();
    }

    /**
     * 转换 SDK 响应为结果 DTO。
     *
     * <p>将 SDK 返回的响应对象转换为应用层的结果 DTO。</p>
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
        // 检查顶层错误
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

        // 解析 SDK 响应中的图片数据
        if (response.getData() != null) {
            for (int i = 0; i < response.getData().size(); i++) {
                ImagesResponse.Image img = response.getData().get(i);

                ImageGenerationResult.GeneratedImage.GeneratedImageBuilder imageBuilder =
                        ImageGenerationResult.GeneratedImage.builder()
                                .index(i);

                // 检查图片是否生成失败
                // 当 url 和 b64Json 均为 null 时，认为图片生成失败
                boolean imageHasError = img.getUrl() == null && img.getB64Json() == null;

                if (imageHasError) {
                    hasError = true;
                    errorCount++;
                    // 记录错误信息
                    imageBuilder.error("图片生成失败，未返回有效数据");
                    // 失败的图片不设置 url 和 b64Json
                    log.warn("[{}] 图片[{}]生成失败，未返回有效数据", taskId, i);
                } else {
                    successCount++;
                    // 成功的图片设置 url 和 b64Json
                    imageBuilder.url(img.getUrl())
                               .b64Json(img.getB64Json());
                }

                images.add(imageBuilder.build());
            }
        }

        // 确定最终状态
        GenerationStatus status;
        if (hasError) {
            status = images.stream().allMatch(img -> img.getError() != null)
                    ? GenerationStatus.FAILED : GenerationStatus.PARTIAL_SUCCESS;
        } else {
            status = GenerationStatus.SUCCESS;
        }

        // 记录生成结果统计
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
