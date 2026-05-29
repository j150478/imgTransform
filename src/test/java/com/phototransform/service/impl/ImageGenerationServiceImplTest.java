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
import com.phototransform.service.QuotaService;
import com.phototransform.service.SeedreamImageService;
import com.phototransform.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 文生图服务测试
 */
class ImageGenerationServiceImplTest {

    private ImageGenerationTaskRepository taskRepository;
    private StorageService storageService;
    private SeedreamImageService seedreamImageService;
    private AppTaskProperties taskProperties;
    private ApplicationEventPublisher eventPublisher;
    private TaskIdGenerator taskIdGenerator;
    private QuotaService quotaService;
    private ImageFetcher imageFetcher;
    private ImageGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        taskRepository = mock(ImageGenerationTaskRepository.class);
        storageService = mock(StorageService.class);
        seedreamImageService = mock(SeedreamImageService.class);
        taskProperties = mock(AppTaskProperties.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        taskIdGenerator = mock(TaskIdGenerator.class);
        quotaService = mock(QuotaService.class);
        imageFetcher = mock(ImageFetcher.class);

        when(taskIdGenerator.generate("TI")).thenReturn("TI_TEST_12345678");

        service = new ImageGenerationServiceImpl(
                taskRepository, storageService, seedreamImageService,
                taskProperties, eventPublisher, taskIdGenerator, quotaService, imageFetcher);
    }

    /**
     * Tracer bullet: 创建任务 — 扣额度、存实体、发事件、返回 PROCESSING
     */
    @Test
    void createTask_success() {
        TextToImageRequest request = TextToImageRequest.builder()
                .prompt("一只可爱的橘猫")
                .build();

        TextToImageResponse response = service.createTask(request, 1L);

        // 验证扣额度
        verify(quotaService).checkAndDeduct(1L, "TI_TEST_12345678");
        // 验证存实体
        verify(taskRepository).save(any(ImageGenerationTask.class));
        // 验证发事件
        verify(eventPublisher).publishEvent(any(ImageTaskCreatedEvent.class));
        // 验证返回
        assertEquals("TI_TEST_12345678", response.getTaskId());
        assertEquals(ImageTaskStatus.PROCESSING, response.getStatus());
    }

    /**
     * 创建任务 — 额度不足时抛异常、不存实体
     */
    @Test
    void createTask_quotaInsufficient() {
        doThrow(new BusinessException(400, "额度不足"))
                .when(quotaService).checkAndDeduct(anyLong(), anyString());

        TextToImageRequest request = TextToImageRequest.builder()
                .prompt("一只猫")
                .build();

        assertThrows(BusinessException.class, () -> service.createTask(request, 1L));
        verify(taskRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * 处理任务 — Seedream 成功 → 存储图片 → 更新 SUCCESS
     */
    @Test
    void processTask_success() {
        ImageGenerationTask task = buildTask();
        when(taskRepository.findById("TI_TEST_12345678")).thenReturn(Optional.of(task));

        ImageGenerationResult.GeneratedImage generatedImage = ImageGenerationResult.GeneratedImage.builder()
                .index(0)
                .url("http://seedream.result/1.png")
                .build();
        ImageGenerationResult result = ImageGenerationResult.builder()
                .status(GenerationStatus.SUCCESS)
                .images(Collections.singletonList(generatedImage))
                .build();
        when(seedreamImageService.generateSingle(anyString())).thenReturn(result);
        when(imageFetcher.fetch(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.store(any(byte[].class), anyString())).thenReturn("http://local/result.jpg");

        service.processTask("TI_TEST_12345678");

        verify(imageFetcher).fetch("http://seedream.result/1.png");
        verify(storageService).store(any(byte[].class), anyString());
        assertEquals(ImageTaskStatus.SUCCESS, task.getStatus());
        assertEquals("http://local/result.jpg", task.getResultImageUrls());
    }

    /**
     * 处理任务 — Seedream 失败 → 更新 FAILED + error
     */
    @Test
    void processTask_generationFailed() {
        ImageGenerationTask task = buildTask();
        when(taskRepository.findById("TI_TEST_12345678")).thenReturn(Optional.of(task));

        ImageGenerationResult result = ImageGenerationResult.builder()
                .status(GenerationStatus.FAILED)
                .errorMessage("生成失败")
                .images(Collections.emptyList())
                .build();
        when(seedreamImageService.generateSingle(anyString())).thenReturn(result);

        service.processTask("TI_TEST_12345678");

        assertEquals(ImageTaskStatus.FAILED, task.getStatus());
        assertEquals("生成失败", task.getErrorMessage());
    }

    /**
     * 处理任务 — 不存在的 taskId → 抛 BusinessException
     */
    @Test
    void processTask_notFound() {
        when(taskRepository.findById("NONEXIST")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.processTask("NONEXIST"));
    }

    /**
     * 查询结果 — SUCCESS 状态返回 imageUrls
     */
    @Test
    void queryResult_success() {
        ImageGenerationTask task = buildTask();
        task.setStatus(ImageTaskStatus.SUCCESS);
        task.setResultImageUrls("http://img1.jpg,http://img2.jpg");
        when(taskRepository.findById("TI_TEST_12345678")).thenReturn(Optional.of(task));

        TextToImageResponse response = service.queryResult("TI_TEST_12345678");

        assertEquals(ImageTaskStatus.SUCCESS, response.getStatus());
        assertEquals(2, response.getImageUrls().size());
        assertEquals("http://img1.jpg", response.getImageUrls().get(0));
    }

    /**
     * 查询结果 — PROCESSING 状态
     */
    @Test
    void queryResult_processing() {
        ImageGenerationTask task = buildTask();
        when(taskRepository.findById("TI_TEST_12345678")).thenReturn(Optional.of(task));

        TextToImageResponse response = service.queryResult("TI_TEST_12345678");

        assertEquals(ImageTaskStatus.PROCESSING, response.getStatus());
        assertTrue(response.getImageUrls().isEmpty());
    }

    /**
     * 查询结果 — 不存在的 taskId → 抛 BusinessException(404)
     */
    @Test
    void queryResult_notFound() {
        when(taskRepository.findById("NONEXIST")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.queryResult("NONEXIST"));
        assertEquals(404, ex.getCode());
    }

    private ImageGenerationTask buildTask() {
        return ImageGenerationTask.builder()
                .taskId("TI_TEST_12345678")
                .userId(1L)
                .prompt("一只可爱的橘猫")
                .status(ImageTaskStatus.PROCESSING)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
    }
}
