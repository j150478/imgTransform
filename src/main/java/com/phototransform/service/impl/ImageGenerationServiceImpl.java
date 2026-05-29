package com.phototransform.service.impl;

import com.phototransform.common.BusinessException;
import com.phototransform.common.ImageTaskCreatedEvent;
import com.phototransform.common.TaskIdGenerator;
import com.phototransform.config.AppTaskProperties;
import com.phototransform.domain.entity.ImageGenerationTask;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.dto.TextToImageRequest;
import com.phototransform.dto.TextToImageResponse;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.enums.ImageTaskStatus;
import com.phototransform.repository.ImageGenerationTaskRepository;
import com.phototransform.service.ImageFetcher;
import com.phototransform.service.ImageGenerationService;
import com.phototransform.service.QuotaService;
import com.phototransform.service.SeedreamImageService;
import com.phototransform.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文生图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final ImageGenerationTaskRepository taskRepository;
    private final StorageService storageService;
    private final SeedreamImageService seedreamImageService;
    private final AppTaskProperties taskProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskIdGenerator taskIdGenerator;
    private final QuotaService quotaService;
    private final ImageFetcher imageFetcher;

    /**
     * 创建文生图任务
     *
     * 1. 检查并扣减用户额度
     * 2. 生成任务ID
     * 3. 持久化任务实体
     * 4. 发布事件触发异步处理
     * 5. 返回任务响应
     */
    @Override
    public TextToImageResponse createTask(TextToImageRequest request, Long userId) {
        // 1. 检查并扣减用户额度
        String taskId = taskIdGenerator.generate("TI");
        quotaService.checkAndDeduct(userId, taskId);

        // 2. 持久化任务实体
        ImageGenerationTask task = ImageGenerationTask.builder()
                .taskId(taskId)
                .userId(userId)
                .prompt(request.getPrompt())
                .status(ImageTaskStatus.PROCESSING)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        taskRepository.save(task);
        log.info("[{}] 文生图任务创建成功, userId: {}", taskId, userId);

        // 3. 发布事件，触发异步处理
        eventPublisher.publishEvent(new ImageTaskCreatedEvent(this, taskId));

        // 4. 返回响应
        return TextToImageResponse.builder()
                .taskId(taskId)
                .status(ImageTaskStatus.PROCESSING)
                .build();
    }

    /**
     * 执行文生图处理
     *
     * 1. 加载任务实体
     * 2. 调用 Seedream 文生图
     * 3. 下载并存储结果图片
     * 4. 更新任务状态
     */
    @Override
    public void processTask(String taskId) {
        log.info("[{}] 开始执行文生图任务", taskId);

        // 1. 加载任务实体
        ImageGenerationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: " + taskId));

        try {
            // 2. 调用 Seedream 文生图
            ImageGenerationResult result = seedreamImageService.generateSingle(task.getPrompt());
            log.info("[{}] Seedream 生成完成, 状态: {}", taskId, result.getStatus());

            // 3. 处理结果
            if (result.getStatus() == GenerationStatus.SUCCESS
                    || result.getStatus() == GenerationStatus.PARTIAL_SUCCESS) {
                List<String> imageUrls = saveResultImages(result, taskId);
                task.setResultImageUrls(String.join(",", imageUrls));
                task.setStatus(ImageTaskStatus.SUCCESS);
                log.info("[{}] 文生图任务成功, 图片数: {}", taskId, imageUrls.size());
            } else {
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "图像生成失败";
                task.setStatus(ImageTaskStatus.FAILED);
                task.setErrorMessage(errorMsg);
                log.error("[{}] 文生图任务失败: {}", taskId, errorMsg);
            }
        } catch (BusinessException e) {
            log.error("[{}] 文生图任务失败: {}", taskId, e.getMessage());
            task.setStatus(ImageTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("[{}] 文生图任务异常", taskId, e);
            task.setStatus(ImageTaskStatus.FAILED);
            task.setErrorMessage("处理异常: " + e.getMessage());
        }

        // 4. 持久化
        task.setUpdatedTime(LocalDateTime.now());
        taskRepository.save(task);
    }

    /**
     * 查询文生图结果
     *
     * 1. 查询任务
     * 2. 组装响应
     */
    @Override
    public TextToImageResponse queryResult(String taskId) {
        // 1. 查询任务
        ImageGenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new BusinessException(404, "任务不存在: " + taskId);
        }

        // 2. 组装响应
        List<String> urls = task.getResultImageUrls() != null && !task.getResultImageUrls().isEmpty()
                ? Arrays.asList(task.getResultImageUrls().split(","))
                : Collections.emptyList();

        return TextToImageResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .imageUrls(urls)
                .errorMessage(task.getErrorMessage())
                .build();
    }

    /**
     * 保存结果图片（支持 URL 下载和 base64 直接解码）
     */
    private List<String> saveResultImages(ImageGenerationResult result, String taskId) {
        List<String> urls = new ArrayList<>();
        int idx = 0;
        for (ImageGenerationResult.GeneratedImage img : result.getImages()) {
            if (img.getError() != null) {
                continue;
            }
            try {
                // 1. 优先从 base64 解码，其次从 URL 下载
                byte[] imageBytes;
                if (img.getB64Json() != null && !img.getB64Json().isEmpty()) {
                    imageBytes = java.util.Base64.getDecoder().decode(img.getB64Json());
                } else if (img.getUrl() != null && !img.getUrl().isEmpty()) {
                    imageBytes = imageFetcher.fetch(img.getUrl());
                } else {
                    continue;
                }
                String fileName = taskId + "_" + idx + ".jpg";
                String url = storageService.store(imageBytes, fileName);
                urls.add(url);
                idx++;
            } catch (Exception e) {
                log.warn("[{}] 结果图片[{}]存储失败: {}", taskId, idx, e.getMessage());
            }
        }
        if (urls.isEmpty()) {
            throw new BusinessException(500, "图像生成未返回有效结果");
        }
        return urls;
    }
}
