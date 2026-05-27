package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.TaskCreatedEvent;
import com.phototransform.common.TaskIdGenerator;
import com.phototransform.config.AppTaskProperties;
import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.dto.PhotoTransformRequest;
import com.phototransform.dto.PhotoTransformResponse;
import com.phototransform.dto.PhotoTransformResultResponse;
import com.phototransform.enums.BackgroundColor;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.enums.ModelType;
import com.phototransform.enums.TransformStatus;
import com.phototransform.repository.PhotoTransformTaskRepository;
import com.phototransform.service.ImageFetcher;
import com.phototransform.service.PhotoTransformService;
import com.phototransform.service.QuotaService;
import com.phototransform.service.SeedreamImageService;
import com.phototransform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 照片转化服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoTransformServiceImpl implements PhotoTransformService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final PhotoTransformTaskRepository taskRepository;
    private final StorageService storageService;
    private final SeedreamImageService seedreamImageService;
    private final AppTaskProperties taskProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final IdPhotoPromptBuilder promptBuilder;
    private final ImageFetcher imageFetcher;
    private final TaskIdGenerator taskIdGenerator;
    private final QuotaService quotaService;

    /**
     * 创建证件照转化任务
     *
     * 1. 校验请求参数
     * 2. 生成任务ID
     * 3. 检查并扣减用户额度
     * 4. 保存原始图片
     * 5. 创建任务记录（包含 userId）
     * 6. 发布事件，触发异步处理
     * 7. 返回任务响应
     */
    @Override
    public PhotoTransformResponse createTransformTask(PhotoTransformRequest request, Long userId) {
        // 1. 校验参数
        validateRequest(request);

        // 2. 生成任务ID
        String taskId = taskIdGenerator.generate("PT");

        // 3. 检查并扣减用户额度
        quotaService.checkAndDeduct(userId, taskId);

        // 4. 保存原始图片
        String originalImageUrl = storageService.store(request.getFile(), taskId);
        log.info("[{}] 原始图片已保存: {}", taskId, originalImageUrl);

        // 5. 创建任务记录（包含 userId）
        ModelType modelType = request.getModelType() != null ? request.getModelType() : ModelType.SEEDREAM_45;
        String bgColorName = resolveBackgroundColor(request.getBackgroundColor());

        PhotoTransformTask task = PhotoTransformTask.builder()
                .taskId(taskId)
                .originalImageUrl(originalImageUrl)
                .status(TransformStatus.PROCESSING)
                .modelType(modelType)
                .backgroundColor(bgColorName)
                .photoType(request.getPhotoType())
                .userId(userId)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        log.info("[{}] [DB_BEFORE_SAVE] userId: {}, originalImageUrl: {}, status: {}, modelType: {}, backgroundColor: {}, photoType: {}",
                taskId, userId, originalImageUrl, task.getStatus(), modelType, bgColorName, task.getPhotoType());
        taskRepository.save(task);
        log.info("[{}] [DB_AFTER_SAVE] 任务创建成功, 用户: {}, 模型: {}, 背景色: {}", taskId, userId, modelType, bgColorName);

        // 6. 发布事件，触发异步处理（仅传递 taskId，避免实体泄漏）
        eventPublisher.publishEvent(new TaskCreatedEvent(this, taskId));

        // 7. 返回响应
        return PhotoTransformResponse.builder()
                .taskId(taskId)
                .status(TransformStatus.PROCESSING)
                .build();
    }

    /**
     * 执行证件照转化处理
     *
     * 0. 根据 taskId 从数据库加载任务实体
     * 1. 检查模型类型
     * 2. 构建 prompt
     * 3. 将原始图片转为 base64 data URL
     * 4. 调用 Seedream 服务
     * 5. 处理生成结果
     * 6. 更新任务状态为成功（或失败）
     */
    @Override
    public void processTransformTask(String taskId) {
        log.info("[{}] 开始处理证件照转化任务", taskId);

        // 0. 从数据库加载任务实体
        PhotoTransformTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: " + taskId));

        try {
            // 1. 检查模型类型
            if (task.getModelType() == ModelType.NANO_PRO) {
                throw new BusinessException(400, "暂不支持 NANO_PRO 模型");
            }

            // 2. 构建 prompt
            BackgroundColor bgColor;
            try {
                bgColor = BackgroundColor.valueOf(task.getBackgroundColor().toUpperCase());
            } catch (IllegalArgumentException e) {
                bgColor = BackgroundColor.BLUE;
            }
            String prompt = promptBuilder.build(task.getPhotoType(), bgColor);
            log.info("[{}] 生成 prompt, photoType: {}, bgColor: {}", taskId, task.getPhotoType(), bgColor);

            // 3. 将本地图片转为 base64 data URL（Seedream 无法访问 localhost）
            String imageDataUrl;
            try {
                byte[] imageBytes = storageService.readByUrl(task.getOriginalImageUrl());
                String contentType = MediaTypeFactory.getMediaType(task.getOriginalImageUrl())
                        .map(MediaType::toString)
                        .orElse("image/png");
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

            // 5. 处理结果
            if (result.getStatus() == GenerationStatus.SUCCESS
                    || result.getStatus() == GenerationStatus.PARTIAL_SUCCESS) {
                ImageGenerationResult.GeneratedImage generatedImage = result.getImages().stream()
                        .filter(img -> img.getError() == null && img.getUrl() != null)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(500, "图像生成未返回有效结果"));

                String resultUrl = saveResultImage(generatedImage.getUrl(), taskId);
                log.info("[{}] 结果图片已保存: {}", taskId, resultUrl);

                // 6. 更新任务状态为成功
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
     * 2. 检查已完成任务是否过期（只读，不写 DB）
     * 3. 组装响应
     */
    @Override
    public PhotoTransformResultResponse queryTransformResult(String taskId) {
        // 1. 查询任务
        PhotoTransformTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new BusinessException(404, "任务不存在: " + taskId);
        }

        // 2. 已完成任务过期则返回提示（不写 DB，写操作由 cron 负责）
        if (isTaskCompletedAndExpired(task)) {
            return PhotoTransformResultResponse.builder()
                    .status(TransformStatus.FAILED)
                    .errorMessage("任务已过期")
                    .build();
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
     *
     * 1. 清理超时 PROCESSING 任务（标记 FAILED）
     * 2. 删除已过期的 SUCCESS/FAILED 任务记录
     */
    @Scheduled(cron = "${app.task.cleanup-cron:0 0 * * * ?}")
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务...");

        // 1. 清理超时 PROCESSING 任务
        LocalDateTime timeoutThreshold = getTimeoutThreshold();
        List<PhotoTransformTask> timedOutTasks = taskRepository
                .findByStatusAndCreatedTimeBefore(TransformStatus.PROCESSING, timeoutThreshold);
        for (PhotoTransformTask task : timedOutTasks) {
            markTaskFailed(task, "任务超时（超过 " + taskProperties.getTimeoutHours() + " 小时未完成）");
            log.info("[{}] 超时任务已标记 FAILED", task.getTaskId());
        }

        // 2. 删除已过期的 SUCCESS/FAILED 任务（先删存储文件，再删 DB 记录）
        LocalDateTime expiryThreshold = LocalDateTime.now()
                .minusHours(taskProperties.getExpiryHours());
        List<PhotoTransformTask> expiredCompleted = new ArrayList<>(
                taskRepository.findByStatusAndCreatedTimeBefore(TransformStatus.SUCCESS, expiryThreshold));
        expiredCompleted.addAll(
                taskRepository.findByStatusAndCreatedTimeBefore(TransformStatus.FAILED, expiryThreshold));
        if (!expiredCompleted.isEmpty()) {
            for (PhotoTransformTask task : expiredCompleted) {
                storageService.deleteByUrl(task.getOriginalImageUrl());
                storageService.deleteByUrl(task.getResultImageUrl());
            }
            taskRepository.deleteAll(expiredCompleted);
            log.info("已删除 {} 条过期任务记录及关联文件", expiredCompleted.size());
        }

        log.info("过期任务清理完成，超时: {}, 过期删除: {}", timedOutTasks.size(), expiredCompleted.size());
    }

    /**
     * 应用启动时清理残留 PROCESSING 任务
     *
     * 1. 计算超时阈值
     * 2. 查询残留的 PROCESSING 任务
     * 3. 标记超时任务为 FAILED
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupStaleTasksOnStartup() {
        log.info("应用启动，清理残留 PROCESSING 任务...");

        // 1. 计算超时阈值
        LocalDateTime threshold = getTimeoutThreshold();

        // 2. 查询残留的 PROCESSING 任务
        List<PhotoTransformTask> staleTasks = taskRepository
                .findByStatusAndCreatedTimeBefore(TransformStatus.PROCESSING, threshold);

        // 3. 标记超时任务为 FAILED
        for (PhotoTransformTask task : staleTasks) {
            markTaskFailed(task, "应用重启，任务已超时");
            log.info("[{}] 残留任务已标记 FAILED", task.getTaskId());
        }

        log.info("启动清理完成，处理残留任务: {}", staleTasks.size());
    }

    // ==================== 私有方法 ====================

    /**
     * 计算超时阈值，将 timeoutHours 转为分钟避免 (long) 截断小数（如 0.5 小时 = 30 分钟）
     */
    private LocalDateTime getTimeoutThreshold() {
        // 1. 将 timeoutHours 转为分钟，避免 (long) 截断小数
        long timeoutMinutes = (long) (taskProperties.getTimeoutHours() * 60);
        return LocalDateTime.now().minusMinutes(timeoutMinutes);
    }

    /**
     * 校验证件照转化请求参数
     *
     * 1. 检查文件是否为空
     * 2. 检查文件大小是否超过限制
     * 3. 检查文件类型是否为图片
     */
    private void validateRequest(PhotoTransformRequest request) {
        // 1. 检查文件是否为空
        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 2. 检查文件大小是否超过限制
        if (request.getFile().getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }

        // 3. 检查文件类型是否为图片
        String contentType = request.getFile().getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持图片文件上传");
        }
    }

    /**
     * 解析背景色枚举值
     *
     * 1. 若 code 为 null，返回默认蓝色
     * 2. 根据 code 获取对应的背景色名称
     */
    private String resolveBackgroundColor(Integer code) {
        // 1. 未指定背景色时返回默认蓝色
        if (code == null) {
            return BackgroundColor.BLUE.getName();
        }

        // 2. 根据枚举 code 获取背景色名称
        return BackgroundColor.fromCode(code).getName();
    }

    /**
     * 保存生成的结果图片
     *
     * 1. 从结果图片 URL 下载图片数据
     * 2. 存储图片到文件系统并返回 URL
     */
    String saveResultImage(String imageUrl, String taskId) {
        // 1. 从结果图片 URL 下载图片数据
        byte[] imageBytes = imageFetcher.fetch(imageUrl);

        // 2. 保存到存储服务并返回 URL
        String fileName = taskId + "_result.jpg";
        return storageService.store(imageBytes, fileName);
    }

    /**
     * 将任务标记为失败并持久化
     *
     * 1. 设置状态为 FAILED
     * 2. 记录错误信息
     * 3. 更新修改时间
     * 4. 持久化到数据库
     */
    private void markTaskFailed(PhotoTransformTask task, String errorMessage) {
        // 1. 设置状态为 FAILED
        task.setStatus(TransformStatus.FAILED);

        // 2. 记录错误信息
        task.setErrorMessage(errorMessage);

        // 3. 更新修改时间
        task.setUpdatedTime(LocalDateTime.now());

        // 4. 持久化到数据库
        taskRepository.save(task);
    }

    /**
     * 判断已完成任务是否过期（读路径专用，不写 DB）
     *
     * 1. PROCESSING 状态未完成，返回 false
     * 2. 创建时间为空则返回 false
     * 3. 计算已过时间是否超过过期阈值
     */
    private boolean isTaskCompletedAndExpired(PhotoTransformTask task) {
        // 1. PROCESSING 状态未完成，直接返回 false
        if (task.getStatus() == TransformStatus.PROCESSING) {
            return false;
        }

        // 2. 创建时间为空则跳过
        if (task.getCreatedTime() == null) {
            return false;
        }

        // 3. 计算已过时间是否超过过期阈值
        Duration elapsed = Duration.between(task.getCreatedTime(), LocalDateTime.now());
        return elapsed.toHours() >= taskProperties.getExpiryHours();
    }

}
