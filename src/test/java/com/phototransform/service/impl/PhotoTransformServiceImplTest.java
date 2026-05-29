package com.phototransform.service.impl;

import com.phototransform.common.TaskIdGenerator;
import com.phototransform.config.AppTaskProperties;
import com.phototransform.domain.entity.PhotoTransformTask;
import com.phototransform.dto.ImageGenerationRequest;
import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.dto.PhotoTransformResultResponse;
import com.phototransform.enums.BackgroundColor;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.enums.ModelType;
import com.phototransform.enums.TransformStatus;
import com.phototransform.repository.PhotoTransformTaskRepository;
import com.phototransform.service.ImageFetcher;
import com.phototransform.service.QuotaService;
import com.phototransform.service.SeedreamImageService;
import com.phototransform.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PhotoTransformServiceImpl 单元测试
 */
class PhotoTransformServiceImplTest {

    private PhotoTransformTaskRepository taskRepository;
    private StorageService storageService;
    private SeedreamImageService seedreamImageService;
    private AppTaskProperties taskProperties;
    private ApplicationEventPublisher eventPublisher;
    private IdPhotoPromptBuilder promptBuilder;
    private ImageFetcher imageFetcher;
    private TaskIdGenerator taskIdGenerator;
    private QuotaService quotaService;
    private PhotoTransformServiceImpl service;

    @BeforeEach
    void setUp() {
        taskRepository = mock(PhotoTransformTaskRepository.class);
        storageService = mock(StorageService.class);
        seedreamImageService = mock(SeedreamImageService.class);
        taskProperties = mock(AppTaskProperties.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        imageFetcher = mock(ImageFetcher.class);
        taskIdGenerator = mock(TaskIdGenerator.class);
        quotaService = mock(QuotaService.class);

        promptBuilder = new IdPhotoPromptBuilder();
        IdPhotoPromptBuilder.PromptTemplate idPhoto = new IdPhotoPromptBuilder.PromptTemplate();
        idPhoto.setSystem("Turn this photo into a standard ID photo:\n"
                + "- Background: solid {name} (RGB {rgb})\n"
                + "- Person: centered, facing forward, symmetrical face\n"
                + "- Show full head and shoulders\n"
                + "- Lighting: even and natural\n"
                + "- Clear face, sharp eyes, natural skin texture\n"
                + "- Clothes: replace with black suit, white shirt, and black tie\n"
                + "- Formal style, neat and professional\n"
                + "- Consistent details: ears, shoulders, tie, collar must be aligned and symmetrical\n"
                + "- Final style: official passport/ID photo, high resolution, clean and sharp");
        idPhoto.setNegative("No artistic effects\n"
                + "No filters\n"
                + "No distortions\n"
                + "No asymmetry in face or body\n"
                + "No extra objects or backgrounds\n"
                + "No duplicated or missing body parts\n"
                + "No clothing artifacts (broken tie, missing collar, misaligned suit)\n"
                + "No cropped head or missing shoulders\n"
                + "No unrealistic lighting\n"
                + "No blurriness or low resolution");
        Map<String, IdPhotoPromptBuilder.PromptTemplate> templates = new HashMap<>();
        templates.put("id-photo", idPhoto);
        promptBuilder.setTemplates(templates);

        when(taskIdGenerator.generate("PT")).thenReturn("PT_TEST_12345678");

        service = new PhotoTransformServiceImpl(
                taskRepository, storageService, seedreamImageService,
                taskProperties, eventPublisher, promptBuilder, imageFetcher, taskIdGenerator, quotaService);
    }

    // ==================== 处理任务 — 状态分支 ====================

    /**
     * SUCCESS / PARTIAL_SUCCESS 均标记任务为 SUCCESS，FAILED 标记为 FAILED
     */
    @ParameterizedTest
    @EnumSource(value = GenerationStatus.class, names = {"SUCCESS", "PARTIAL_SUCCESS"})
    void processTransformTask_successOrPartial_setsTaskSuccess(GenerationStatus genStatus) {
        PhotoTransformTask task = buildTask();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));
        mockStorageReadSuccess();
        mockSeedreamResult(genStatus, "http://localhost/result.jpg");
        mockImageFetchAndStore();

        service.processTransformTask(task.getTaskId());

        assertEquals(TransformStatus.SUCCESS, task.getStatus());
        assertNotNull(task.getResultImageUrl());
    }

    @Test
    void processTransformTask_failed_setsTaskFailed() {
        PhotoTransformTask task = buildTask();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));
        mockStorageReadSuccess();
        when(seedreamImageService.generate(any())).thenReturn(
                ImageGenerationResult.builder()
                        .status(GenerationStatus.FAILED)
                        .errorMessage("生成失败")
                        .images(Collections.emptyList())
                        .build());

        service.processTransformTask(task.getTaskId());

        assertEquals(TransformStatus.FAILED, task.getStatus());
        assertEquals("生成失败", task.getErrorMessage());
    }

    // ==================== Prompt 组装 ====================

    /**
     * 传进 Seedream 的 prompt 包含正确的背景色、正负面指令，null photoType fallback 到 id-photo
     */
    @Test
    void processTransformTask_promptToSeedream_containsAllExpectedParts() {
        PhotoTransformTask task = buildTask();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));
        mockStorageReadSuccess();
        mockSeedreamResult(GenerationStatus.SUCCESS, "http://localhost/result.jpg");
        mockImageFetchAndStore();

        service.processTransformTask(task.getTaskId());

        ArgumentCaptor<ImageGenerationRequest> captor = ArgumentCaptor.forClass(ImageGenerationRequest.class);
        verify(seedreamImageService).generate(captor.capture());
        String prompt = captor.getValue().getPrompt();

        assertTrue(prompt.contains("solid blue (RGB 0,112,192)"), "含背景色");
        assertTrue(prompt.contains("standard ID photo"), "含正面指令");
        assertTrue(prompt.contains("No artistic effects"), "含负面约束");
        assertTrue(prompt.contains("\n\n"), "正负面间有空行");
    }

    // ==================== PromptBuilder ====================

    @ParameterizedTest
    @EnumSource(value = BackgroundColor.class, names = {"BLUE", "WHITE"})
    void promptBuilder_containsExpectedColorText(BackgroundColor color) {
        String prompt = promptBuilder.build(color);
        assertTrue(prompt.contains("solid " + color.getName().toLowerCase()));
        assertTrue(prompt.contains("ID photo"));
    }

    // ==================== 查询结果 ====================

    @Test
    void queryTransformResult_expiredCompleted_returnsExpiredWithoutWrite() {
        PhotoTransformTask task = buildTask();
        task.setCreatedTime(LocalDateTime.now().minusHours(25));
        task.setStatus(TransformStatus.SUCCESS);

        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));
        when(taskProperties.getExpiryHours()).thenReturn(24);

        PhotoTransformResultResponse response = service.queryTransformResult(task.getTaskId());

        assertEquals(TransformStatus.FAILED, response.getStatus());
        assertEquals("任务已过期", response.getErrorMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void queryTransformResult_notExpired_orProcessing_returnsCorrectly() {
        PhotoTransformTask task = buildTask();
        task.setCreatedTime(LocalDateTime.now());
        task.setStatus(TransformStatus.PROCESSING);

        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));
        when(taskProperties.getExpiryHours()).thenReturn(24);

        PhotoTransformResultResponse response = service.queryTransformResult(task.getTaskId());

        verify(taskRepository, times(1)).findById(task.getTaskId());
        assertEquals(TransformStatus.PROCESSING, response.getStatus());
    }

    // ==================== 清理逻辑 ====================

    @Test
    void cleanupExpiredTasks_timeoutAndExpiry_bothHandled() {
        PhotoTransformTask timedOut = buildTask();
        timedOut.setCreatedTime(LocalDateTime.now().minusHours(2));
        timedOut.setStatus(TransformStatus.PROCESSING);

        PhotoTransformTask expired = buildTask();
        expired.setTaskId("PT_EXPIRED");
        expired.setStatus(TransformStatus.SUCCESS);
        expired.setCreatedTime(LocalDateTime.now().minusHours(25));

        when(taskProperties.getTimeoutHours()).thenReturn(1.0);
        when(taskProperties.getExpiryHours()).thenReturn(24);
        when(taskRepository.findByStatusAndCreatedTimeBefore(eq(TransformStatus.PROCESSING), any()))
                .thenReturn(Collections.singletonList(timedOut));
        when(taskRepository.findByStatusAndCreatedTimeBefore(eq(TransformStatus.SUCCESS), any()))
                .thenReturn(Collections.singletonList(expired));
        when(taskRepository.findByStatusAndCreatedTimeBefore(eq(TransformStatus.FAILED), any()))
                .thenReturn(Collections.emptyList());
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cleanupExpiredTasks();

        assertEquals(TransformStatus.FAILED, timedOut.getStatus());
        assertTrue(timedOut.getErrorMessage().contains("1.0"));
        verify(taskRepository).save(timedOut);
        verify(taskRepository).deleteAll(Collections.singletonList(expired));
    }

    @Test
    void cleanupStaleTasksOnStartup_staleProcessing_marksFailed() {
        PhotoTransformTask stale = buildTask();
        stale.setCreatedTime(LocalDateTime.now().minusHours(2));
        stale.setStatus(TransformStatus.PROCESSING);

        when(taskProperties.getTimeoutHours()).thenReturn(1.0);
        when(taskRepository.findByStatusAndCreatedTimeBefore(eq(TransformStatus.PROCESSING), any()))
                .thenReturn(Collections.singletonList(stale));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cleanupStaleTasksOnStartup();

        assertEquals(TransformStatus.FAILED, stale.getStatus());
        assertTrue(stale.getErrorMessage().contains("重启"));
        verify(taskRepository).save(stale);
    }

    // ==================== 辅助方法 ====================

    private PhotoTransformTask buildTask() {
        return PhotoTransformTask.builder()
                .taskId("PT_TEST_12345678")
                .originalImageUrl("http://localhost:8080/uploads/test_original.jpg")
                .status(TransformStatus.PROCESSING)
                .modelType(ModelType.SEEDREAM_45)
                .backgroundColor("blue")
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
    }

    private void mockStorageReadSuccess() {
        when(storageService.readByUrl(anyString())).thenReturn(new byte[]{1, 2, 3, 4});
    }

    private void mockSeedreamResult(GenerationStatus status, String url) {
        ImageGenerationResult.GeneratedImage img = ImageGenerationResult.GeneratedImage.builder()
                .index(0).url(url).build();
        when(seedreamImageService.generate(any())).thenReturn(
                ImageGenerationResult.builder()
                        .status(status).images(Collections.singletonList(img)).build());
    }

    private void mockImageFetchAndStore() {
        when(imageFetcher.fetch(anyString())).thenReturn(new byte[]{1, 2, 3, 4});
        when(storageService.store(any(byte[].class), anyString())).thenReturn("http://localhost/saved.jpg");
    }
}
