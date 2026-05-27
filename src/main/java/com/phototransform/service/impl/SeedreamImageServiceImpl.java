package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.TaskIdGenerator;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationCapability;
import com.phototransform.enums.GenerationMode;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.service.SeedreamClient;
import com.phototransform.service.SeedreamImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seedream 图像生成服务实现类。
 *
 * <p>基于火山引擎 doubao-seedream-4.5 模型的图像生成服务实现类。
 * 支持文生图、图生图、组图生成等多种生成能力。
 * SDK 调用委托给 {@link SeedreamClient} 处理。</p>
 *
 * @author PhotoTransform Team
 * @see SeedreamImageService
 * @see GenerationCapability
 * @see SeedreamClient
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

    private final TaskIdGenerator taskIdGenerator;
    private final SeedreamClient seedreamClient;

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

            // 3. 委托 SeedreamClient 执行 SDK 调用
            return seedreamClient.generate(request, capability);

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
            // 1. 验证请求与指定能力是否匹配
            validateCapabilityMatch(request, capability);

            // 2. 设置能力到请求中
            request.setCapability(capability);

            // 3. 委托 SeedreamClient 执行 SDK 调用
            return seedreamClient.generate(request, capability);

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
        // 1. 验证提示词
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new BusinessException(400, "生成提示词不能为空");
        }
        if (request.getPrompt().length() > 2000) {
            throw new BusinessException(400, "生成提示词长度不能超过2000个字符");
        }

        // 2. 验证参考图数量
        List<String> referenceImages = request.getReferenceImages();
        if (referenceImages != null && referenceImages.size() > MAX_REFERENCE_IMAGES) {
            throw new BusinessException(400,
                    String.format("参考图数量不能超过%d张，当前: %d", MAX_REFERENCE_IMAGES, referenceImages.size()));
        }

        // 3. 验证组图生成数量
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

        // 4. 验证尺寸
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
        // 1. 如果请求中已显式指定能力，直接使用
        if (request.getCapability() != null) {
            return request.getCapability();
        }

        // 2. 获取参考图数量和生成模式
        int refCount = request.getReferenceImages() == null ? 0 : request.getReferenceImages().size();
        GenerationMode mode = request.getMode();

        // 3. 如果未指定模式，根据生成数量判断
        if (mode == null) {
            Integer n = request.getN();
            mode = (n != null && n > 1) ? GenerationMode.SEQUENTIAL : GenerationMode.SINGLE;
        }

        // 4. 根据参考图数量和生成模式确定能力
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
     * 构建错误结果。
     *
     * <p>当参数验证失败或发生未预期异常时，构建包含错误信息的结果对象。</p>
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
                .model(request.getModel())
                .prompt(request.getPrompt())
                .errorMessage(errorMsg)
                .errorCode(errorCode)
                .build();
    }
}
