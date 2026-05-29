package com.phototransform.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端测试 — 使用真实 Seedream API 调用。
 *
 * <p>覆盖真实 API profile（seedream-real），验证完整流程。
 * 日常开发运行全量测试不会触发此测试（需显式指定 seedream-real profile）。</p>
 */
@ActiveProfiles({"test", "seedream-real"})
class PhotoTransformControllerRealApiTest extends BaseControllerTest {

    private static final String TEST_IMAGE_PATH = "/Users/feiyu/Downloads/wo1.png";

    /**
     * 提交照片转化请求，轮询直到任务完成，验证结果为 SUCCESS。
     */
    @Test
    void transformAndPollUntilComplete_returnsSuccess() throws Exception {
        // 1. 提交转化请求
        byte[] imageBytes = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
        MockMultipartFile file = new MockMultipartFile(
                "file", "wo1.png", "image/png", imageBytes);

        MvcResult createResult = mockMvc.perform(multipart("/api/photo/transform")
                        .file(file)
                        .param("backgroundColor", "1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String taskId = JsonPath.read(response, "$.data.taskId");
        System.out.println("=== 任务创建成功, taskId: " + taskId + " ===");

        // 2. 轮询查询结果，最多等待 3 分钟
        String status = null;
        String resultImageUrl = null;
        String errorMessage = null;
        long deadline = System.currentTimeMillis() + 180_000;

        while (System.currentTimeMillis() < deadline) {
            MvcResult queryResult = mockMvc.perform(get("/api/photo/result")
                            .param("taskId", taskId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").isNotEmpty())
                    .andReturn();

            String queryResponse = queryResult.getResponse().getContentAsString();
            status = JsonPath.read(queryResponse, "$.data.status");
            resultImageUrl = JsonPath.read(queryResponse, "$.data.resultImageUrl");
            errorMessage = JsonPath.read(queryResponse, "$.data.errorMessage");

            System.out.printf("=== 轮询 taskId=%s, status=%s, resultUrl=%s, error=%s ===%n",
                    taskId, status, resultImageUrl, errorMessage);

            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                break;
            }

            Thread.sleep(5000);
        }

        // 3. 验证结果
        System.out.println("=== 转化最终状态: " + status + ", 结果图片: " + resultImageUrl + " ===");
        assert "SUCCESS".equals(status) : "期望 SUCCESS，实际: " + status + ", 错误: " + errorMessage;
        assert resultImageUrl != null && !resultImageUrl.isEmpty() : "结果图片 URL 不应为空";
    }
}
