package com.phototransform.controller;

import com.phototransform.dto.ImageGenerationResult;
import com.phototransform.enums.GenerationStatus;
import com.phototransform.service.SeedreamImageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PhotoTransformController 集成测试
 */
class PhotoTransformControllerTest extends BaseControllerTest {

    @MockBean
    private SeedreamImageService seedreamImageService;

    private static final String TEST_IMAGE_PATH = "/Users/feiyu/Downloads/wo1.png";

    /**
     * 提交有效请求 → 返回 taskId + PROCESSING
     */
    @Test
    void transform_validRequest_returnsTaskIdAndProcessing() throws Exception {
        mockSeedreamSuccess();

        byte[] imageBytes = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
        MockMultipartFile file = new MockMultipartFile(
                "file", "wo1.png", "image/png", imageBytes);

        mockMvc.perform(multipart("/api/photo/transform")
                        .file(file)
                        .param("backgroundColor", "1")
                        .param("modelType", "SEEDREAM_45")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    /**
     * 先提交任务，再查询结果 → 返回任务状态
     */
    @Test
    void getResult_existingTask_returnsStatus() throws Exception {
        mockSeedreamSuccess();

        byte[] imageBytes = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
        MockMultipartFile file = new MockMultipartFile(
                "file", "wo1.png", "image/png", imageBytes);

        MvcResult createResult = mockMvc.perform(multipart("/api/photo/transform")
                        .file(file)
                        .param("backgroundColor", "1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String taskId = com.jayway.jsonpath.JsonPath.read(response, "$.data.taskId");

        mockMvc.perform(get("/api/photo/result")
                        .param("taskId", taskId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").isNotEmpty());
    }

    /**
     * 文件为空 → 返回参数错误
     */
    @Test
    void transform_emptyFile_returnsError() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/photo/transform")
                        .file(emptyFile)
                        .param("backgroundColor", "1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * taskId 不存在 → 返回错误
     */
    @Test
    void getResult_nonExistingTask_returnsError() throws Exception {
        mockMvc.perform(get("/api/photo/result")
                        .param("taskId", "NON_EXISTENT_ID")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * taskId 为空 → 返回参数校验错误
     */
    @Test
    void getResult_blankTaskId_returnsError() throws Exception {
        mockMvc.perform(get("/api/photo/result")
                        .param("taskId", "")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void mockSeedreamSuccess() {
        ImageGenerationResult.GeneratedImage generatedImage = ImageGenerationResult.GeneratedImage.builder()
                .index(0)
                .url("http://localhost:8080/uploads/test_result.jpg")
                .build();
        ImageGenerationResult mockResult = ImageGenerationResult.builder()
                .taskId("SD_TEST")
                .status(GenerationStatus.SUCCESS)
                .images(Collections.singletonList(generatedImage))
                .build();
        when(seedreamImageService.generate(any())).thenReturn(mockResult);
    }
}
