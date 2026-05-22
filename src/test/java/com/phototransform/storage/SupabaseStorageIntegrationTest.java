package com.phototransform.storage;

import com.jayway.jsonpath.JsonPath;
import com.phototransform.common.JwtUtil;
import com.phototransform.domain.entity.User;
import com.phototransform.domain.entity.UserQuota;
import com.phototransform.repository.UserQuotaRepository;
import com.phototransform.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Supabase Storage 上传集成测试（使用真实 Supabase Storage API）
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.profiles.active=prod",
        "app.storage.type=supabase"
})
class SupabaseStorageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuotaRepository userQuotaRepository;

    private static final String TEST_IMAGE_PATH = "/Users/feiyu/Downloads/wo2.png";

    private String authToken;

    @BeforeAll
    void setUp() {
        String phone = "13800138" + String.format("%05d", System.currentTimeMillis() % 100000);
        User user = User.builder()
                .phone(phone)
                .status(com.phototransform.enums.UserStatus.ACTIVE)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        UserQuota quota = UserQuota.builder()
                .userId(user.getId())
                .remaining(10)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        userQuotaRepository.save(quota);

        authToken = jwtUtil.generateToken(user.getId());
    }

    /**
     * 模拟浏览器前端上传图片 → 验证 originalImageUrl 是 Supabase 公开 URL
     */
    @Test
    void uploadPhoto_storesToSupabase_returnsPublicUrl() throws Exception {
        // 1. 模拟浏览器前端 multipart/form-data 上传
        byte[] imageBytes = Files.readAllBytes(Paths.get(TEST_IMAGE_PATH));
        MockMultipartFile file = new MockMultipartFile(
                "file", "wo2.png", "image/png", imageBytes);

        MvcResult result = mockMvc.perform(multipart("/api/photo/transform")
                        .file(file)
                        .param("backgroundColor", "1")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andReturn();

        // 2. 提取 taskId
        String response = result.getResponse().getContentAsString();
        String taskId = JsonPath.read(response, "$.data.taskId");
        System.out.println("=== 任务创建成功, taskId: " + taskId + " ===");

        // 3. 等待异步处理完成（最多等 3 分钟）
        String status = null;
        String resultImageUrl = null;
        long deadline = System.currentTimeMillis() + 180_000;

        while (System.currentTimeMillis() < deadline) {
            MvcResult queryResult = mockMvc.perform(get("/api/photo/result")
                            .param("taskId", taskId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String queryResponse = queryResult.getResponse().getContentAsString();
            status = JsonPath.read(queryResponse, "$.data.status");
            resultImageUrl = JsonPath.read(queryResponse, "$.data.resultImageUrl");

            System.out.printf("=== 轮询 %s: status=%s, resultUrl=%s ===%n",
                    taskId, status, resultImageUrl);

            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                break;
            }
            Thread.sleep(5000);
        }

        // 4. 验证：状态为 SUCCESS，结果 URL 是 Supabase 公开 URL
        assert "SUCCESS".equals(status) : "期望 SUCCESS，实际: " + status;
        assert resultImageUrl != null && !resultImageUrl.isEmpty()
                : "结果图片 URL 不应为空";
        assert resultImageUrl.contains("supabase.co/storage/v1/object/public/photoX")
                : "结果 URL 应为 Supabase 公开 URL，实际: " + resultImageUrl;

        System.out.println("=== 验证通过！结果 URL: " + resultImageUrl + " ===");
    }
}
