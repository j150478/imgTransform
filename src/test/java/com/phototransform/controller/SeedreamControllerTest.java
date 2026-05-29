package com.phototransform.controller;

import com.phototransform.dto.TextToImageResponse;
import com.phototransform.enums.ImageTaskStatus;
import com.phototransform.service.ImageGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SeedreamController 集成测试
 */
class SeedreamControllerTest extends BaseControllerTest {

    @MockBean
    private ImageGenerationService imageGenerationService;

    /**
     * POST /api/seedream/generate — 成功 → 200 PROCESSING + taskId
     */
    @Test
    void generate_success() throws Exception {
        TextToImageResponse response = TextToImageResponse.builder()
                .taskId("TI_TEST_ABCD1234")
                .status(ImageTaskStatus.PROCESSING)
                .build();
        when(imageGenerationService.createTask(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/seedream/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"一只橘猫\"}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("TI_TEST_ABCD1234"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    /**
     * POST /api/seedream/generate — 空 prompt → 200 + code 400
     */
    @Test
    void generate_emptyPrompt() throws Exception {
        mockMvc.perform(post("/api/seedream/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"\"}")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * GET /api/seedream/result — 成功 → 返回 SUCCESS + imageUrls
     */
    @Test
    void getResult_success() throws Exception {
        TextToImageResponse response = TextToImageResponse.builder()
                .taskId("TI_TEST_ABCD1234")
                .status(ImageTaskStatus.SUCCESS)
                .imageUrls(Arrays.asList("http://img1.jpg", "http://img2.jpg"))
                .build();
        when(imageGenerationService.queryResult("TI_TEST_ABCD1234")).thenReturn(response);

        mockMvc.perform(get("/api/seedream/result")
                        .param("taskId", "TI_TEST_ABCD1234")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("http://img1.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("http://img2.jpg"));
    }

    /**
     * GET /api/seedream/result — 不存在 → 200 + code 404
     */
    @Test
    void getResult_notFound() throws Exception {
        when(imageGenerationService.queryResult("NONEXIST"))
                .thenThrow(new com.phototransform.common.BusinessException(404, "任务不存在: NONEXIST"));

        mockMvc.perform(get("/api/seedream/result")
                        .param("taskId", "NONEXIST")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
