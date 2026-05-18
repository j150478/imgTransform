package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.TaskCreatedEvent;
import com.phototransform.config.AppTaskProperties;
import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.dto.PhotoTransformRequest;
import com.phototransform.dto.PhotoTransformResponse;
import com.phototransform.dto.PhotoTransformResultResponse;
import com.phototransform.enums.BackgroundColor;
import com.phototransform.enums.ModelType;
import com.phototransform.enums.TransformStatus;
import com.phototransform.repository.PhotoTransformTaskRepository;
import com.phototransform.service.PhotoTransformService;
import com.phototransform.service.SeedreamImageService;
import com.phototransform.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 照片转化服务实现类
 */
@Slf4j
@Service
public class PhotoTransformServiceImpl implements PhotoTransformService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Autowired
    private PhotoTransformTaskRepository taskRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private SeedreamImageService seedreamImageService;

    @Autowired
    private AppTaskProperties taskProperties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 创建证件照转化任务
     *
     * 1. 校验请求参数
     * 2. 生成任务ID
     * 3. 保存原始图片
     * 4. 创建任务记录
     * 5. 触发异步处理
     * 6. 返回任务响应
     */
    @Override
    public PhotoTransformResponse createTransformTask(PhotoTransformRequest request) {
        // 1. 校验参数
        validateRequest(request);

        // 2. 生成任务ID
        String taskId = "PT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // 3. 保存原始图片
        String originalImageUrl = storageService.store(request.getFile(), taskId);
        log.info("[{}] 原始图片已保存: {}", taskId, originalImageUrl);

        // 4. 创建任务记录
        ModelType modelType = request.getModelType() != null ? request.getModelType() : ModelType.SEEDREAM_45;
        String bgColorName = resolveBackgroundColor(request.getBackgroundColor());

        PhotoTransformTask task = PhotoTransformTask.builder()
                .taskId(taskId)
                .originalImageUrl(originalImageUrl)
                .status(TransformStatus.PROCESSING)
                .modelType(modelType)
                .backgroundColor(bgColorName)
                .photoType(request.getPhotoType())
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        taskRepository.save(task);
        log.info("[{}] 任务创建成功, 模型: {}, 背景色: {}", taskId, modelType, bgColorName);

        // 5. 发布事件，触发异步处理
        eventPublisher.publishEvent(new TaskCreatedEvent(this, task));

        // 6. 返回响应
        return PhotoTransformResponse.builder()
                .taskId(taskId)
                .status(TransformStatus.PROCESSING)
                .build();
    }

    @Override
    public void processTransformTask(PhotoTransformTask task) {
        String taskId = task.getTaskId();
        log.info("[{}] 开始处理证件照转化任务", taskId);

        try {
            // 1. 检查模型类型
            if (task.getModelType() == ModelType.NANO_PRO) {
                throw new BusinessException(400, "暂不支持 NANO_PRO 模型");
            }

            // 2. 构建 prompt
            String prompt = buildPrompt(task.getBackgroundColor());
            log.info("[{}] 生成 prompt: {}", taskId, prompt);

            // 3. 将本地图片转为 base64 data URL（Seedream 无法访问 localhost）
            String imageDataUrl;
            try {
                byte[] imageBytes = storageService.readByUrl(task.getOriginalImageUrl());
                String contentType = resolveContentType(task.getOriginalImageUrl());
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                imageDataUrl = "data:" + contentType + ";base64," + base64;
            } catch (Exception e) {
                throw new BusinessException(500, "读取原始图片失败: " + e.getMessage(), e);
            }
            log.info("[{}] 原始图片已转为 data URL, 大小: {} chars", taskId, imageDataUrl.length());

            // 4. 调用 Seedream 服务
            ImageGenerationRequest genRequest = ImageGenerationRequest.builder()
                    .prompt(prompt)
                    .referenceImages(Collections.singletonList(imageDataUrl))
                    .build();

            ImageGenerationResult result = seedreamImageService.generate(genRequest);
            log.info("[{}] Seedream 生成完成, 状态: {}", taskId, result.getStatus());

            // 4. 处理结果
            if ("SUCCESS".equals(result.getStatus()) || "PARTIAL_SUCCESS".equals(result.getStatus())) {
                // 取第一张成功生成的图片
                ImageGenerationResult.GeneratedImage generatedImage = result.getImages().stream()
                        .filter(img -> img.getError() == null && img.getUrl() != null)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(500, "图像生成未返回有效结果"));

                // 保存结果图片到本地存储
                String resultUrl = saveResultImage(generatedImage.getUrl(), taskId);
                log.info("[{}] 结果图片已保存: {}", taskId, resultUrl);

                // 5. 更新任务状态为成功
                task.setStatus(TransformStatus.SUCCESS);
                task.setResultImageUrl(resultUrl);
                task.setUpdatedTime(LocalDateTime.now());
                taskRepository.save(task);
                log.info("[{}] 任务处理成功", taskId);
            } else {
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "图像生成失败";
                markTaskFailed(task, errorMsg);
            }

        } catch (BusinessException e) {
            log.error("[{}] 任务处理失败: {}", taskId, e.getMessage());
            markTaskFailed(task, e.getMessage());
        } catch (Exception e) {
            log.error("[{}] 任务处理异常", taskId, e);
            markTaskFailed(task, "处理过程中发生异常: " + e.getMessage());
        }
    }

    /**
     * 查询证件照转化结果
     *
     * 1. 查询任务记录
     * 2. 检查任务是否过期
     * 3. 组装响应
     */
    @Override
    public PhotoTransformResultResponse queryTransformResult(String taskId) {
        // 1. 查询任务
        PhotoTransformTask task = taskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在: " + taskId);
        }

        // 2. 检查过期
        if (isTaskExpired(task)) {
            markTaskFailed(task, "任务已过期");
        }

        // 3. 组装响应
        return PhotoTransformResultResponse.builder()
                .status(task.getStatus())
                .resultImageUrl(task.getStatus() == TransformStatus.SUCCESS ? task.getResultImageUrl() : null)
                .errorMessage(task.getStatus() == TransformStatus.FAILED ? task.getErrorMessage() : null)
                .build();
    }

    /**
     * 定时清理过期任务
     */
    @Scheduled(cron = "${app.task.cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务...");
        // InMemoryTaskRepository 暂不支持遍历，后续 MyBatis 实现时补充
        log.info("过期任务清理完成（内存存储暂跳过）");
    }

    // ==================== 私有方法 ====================

    private void validateRequest(PhotoTransformRequest request) {
        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (request.getFile().getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }
        String contentType = request.getFile().getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持图片文件上传");
        }
    }

    private String resolveBackgroundColor(Integer code) {
        if (code == null) {
            return BackgroundColor.BLUE.getName();
        }
        return BackgroundColor.fromCode(code).getName();
    }

    private String buildPrompt(String backgroundColorName) {
        BackgroundColor bgColor;
        try {
            bgColor = BackgroundColor.valueOf(backgroundColorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            bgColor = BackgroundColor.BLUE;
        }

        return "Turn this photo into a standard ID photo:\n"
                + "- Background: " + bgColor.getPromptDescription() + "\n"
                + "- Person: centered, facing forward, symmetrical face\n"
                + "- Show full head and shoulders\n"
                + "- Lighting: even and natural\n"
                + "- Clear face, sharp eyes, natural skin texture\n"
                + "- Clothes: replace with black suit, white shirt, and black tie\n"
                + "- Formal style, neat and professional\n"
                + "- Consistent details: ears, shoulders, tie, collar must be aligned and symmetrical\n"
                + "- Final style: official passport/ID photo, high resolution, clean and sharp\n"
                + "\n"
                + "No artistic effects\n"
                + "No filters\n"
                + "No distortions\n"
                + "No asymmetry in face or body\n"
                + "No extra objects or backgrounds\n"
                + "No duplicated or missing body parts\n"
                + "No clothing artifacts (broken tie, missing collar, misaligned suit)\n"
                + "No cropped head or missing shoulders\n"
                + "No unrealistic lighting\n"
                + "No blurriness or low resolution";
    }

    private String saveResultImage(String imageUrl, String taskId) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream()) {
                byte[] imageBytes = readAllBytes(is);
                String fileName = taskId + "_result.jpg";
                return storageService.store(imageBytes, fileName);
            }
        } catch (Exception e) {
            throw new BusinessException(500, "保存结果图片失败: " + e.getMessage(), e);
        }
    }

    private void markTaskFailed(PhotoTransformTask task, String errorMessage) {
        task.setStatus(TransformStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setUpdatedTime(LocalDateTime.now());
        taskRepository.save(task);
    }

    private boolean isTaskExpired(PhotoTransformTask task) {
        if (task.getCreatedTime() == null) {
            return false;
        }
        long hours = TimeUnit.MILLISECONDS.toHours(
                Duration.between(task.getCreatedTime(), LocalDateTime.now()).toMillis());
        return hours >= taskProperties.getExpireHours();
    }

    private String resolveContentType(String fileUrl) {
        String lower = fileUrl.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/png";
    }

    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
